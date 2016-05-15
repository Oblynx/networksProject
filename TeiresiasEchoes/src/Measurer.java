import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.*;
import java.util.Stack;

public class Measurer {

	public Measurer(IthakiSocket s, String echoc, String imgc, String soundc, String copterc, String echof,
			String echof_nodelay, String imgf1, String imgf2, String tempf, String pitchf, String audiof, String copterf){
		this.s=s; this.echoc= echoc; this.imgc= imgc; this.soundc= soundc; this.copterc= copterc; this.echof= echof;
		this.echof_nodelay= echof_nodelay; this.imgf1= imgf1; this.imgf2= imgf2;
		this.tempf= tempf; this.pitchf= pitchf; this.audiof= audiof; this.copterf= copterf;
	}
	
	public void take_measurements(int echoDelayMillis, int echototalMeasurementTimeMillis){
		echoMeasurements(echoDelayMillis, echototalMeasurementTimeMillis);
		imgMeasurements();
		tempMeasurements();
		//pitchMeasurements();
		//audioMeasurements();
		//copterMeasurements();
		// Wait for any running tasks to finish
		while(!tasks.empty()) try{
			tasks.pop().get();
		} catch(Exception e) { System.err.println(e.getMessage()); }
	}

	// output: timestamp<milli>;echodelay<milli>
	private void echoMeasurements(int delayMillis, int totalMeasurementTimeMillis){
		Callable<Void> task= () -> {
			File f= new File(echof), fnd= new File(echof_nodelay);
			java.util.function.BiConsumer<File, String> measureAction= (file, code) -> {
				try{
  				FileOutputStream fout= new FileOutputStream(file);
    			long startTime= System.currentTimeMillis(), curTime= 0;
    			while( curTime < totalMeasurementTimeMillis){
    				long echoTimeMilli= System.nanoTime()/1000000;
    				s.send(code.getBytes());
    				s.receive(100);
    				echoTimeMilli= System.nanoTime()/1000000-echoTimeMilli;
    				fout.write( (new Long(curTime).toString()+";"+new Long(echoTimeMilli).toString()+"\n").getBytes() );
    				Thread.sleep(delayMillis);
    				curTime= System.currentTimeMillis() - startTime;
    			}
    			fout.close();
				} catch(FileNotFoundException e){ System.err.println("Error opening file: "+f.getAbsolutePath()); }
				catch(IOException e){ System.err.println("Error writing to file: "+f.getAbsolutePath()); }
				catch (InterruptedException e) {}
			};
			
			// measure delay
			measureAction.accept(f, echoc);
			// measure no delay
			measureAction.accept(fnd, "E0000");
			return null;
		};
		tasks.push(pool.submit(task));
	}
	// Get 2 images
	private void imgMeasurements(){
		ByteArrayOutputStream image1= getImage(imgc), image2= getImage(imgc+"CAM=PTZ");
		File f1= new File(imgf1), f2= new File(imgf2);
		FileOutputStream fout;
		try{
			fout= new FileOutputStream(f1);
			fout.write(image1.toByteArray());
			fout= new FileOutputStream(f2);
			fout.write(image2.toByteArray());
		} catch(FileNotFoundException e){ System.err.println("Error opening file: "+f1.getAbsolutePath()); }
		catch(IOException e){ System.err.println("Error writing to file: "+f1.getAbsolutePath()); }
	}
	// output: name;temperature
	private void tempMeasurements(){
		File f= new File(tempf);
		FileOutputStream fout;
		try{
			fout= new FileOutputStream(f); 
			String echomsg;
			for(int i=0;i<8;i++){
				s.send( (echoc+"T0"+new Integer(i).toString()).getBytes() );
				echomsg= new String(s.receive(100));
				//System.out.println(new Integer(i).toString()+":"+echomsg.substring(27, 46)+"|");
				String name=echomsg.substring(27,30), temp=echomsg.substring(43,46);
				if (name.startsWith("T")) fout.write( (name+";"+temp+"\n").getBytes() );
			}
		} catch(FileNotFoundException e){ System.err.println("Error opening file: "+f.getAbsolutePath()); }
		catch(IOException e){ System.err.println("Error writing to file: "+f.getAbsolutePath()); }
	}
	private void pitchMeasurements(){}
	private void audioMeasurements(){}
	private void copterMeasurements(){}
	
	private ByteArrayOutputStream getImage(String code){
		ByteArrayOutputStream image= new ByteArrayOutputStream();
		boolean imageEnd= false;
		
		s.send(code.getBytes());
		while(!imageEnd){
			byte[] msg;
			msg= s.receive(3*1024);
			int l= msg.length;
			image.write(msg,image.size(),l);
			if ( msg[l-2]==0xFF && msg[l-1]==0xD9 ) imageEnd= true;
		}
		return image;
	}
	
	
	private String echoc, imgc, soundc, copterc;
	private String echof, echof_nodelay, imgf1, imgf2, tempf,	pitchf, audiof, copterf;
	private IthakiSocket s;
	
	private ExecutorService pool= Executors.newCachedThreadPool();
	private Stack<Future<Void>> tasks= new Stack<Future<Void>>();
}
