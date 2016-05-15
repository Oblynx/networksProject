import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.Stack;

public class Measurer {

	public Measurer(IthakiSocket s, String echoc, String imgc, String soundc, String copterc, String echof,
			String imgf, String tempf, String pitchf, String audiof, String copterf){
		this.s=s; this.echoc= echoc; this.imgc= imgc; this.soundc= soundc; this.copterc= copterc; this.echof= echof;
		this.imgf= imgf; this.tempf= tempf; this.pitchf= pitchf; this.audiof= audiof; this.copterf= copterf;
	}
	
	public void take_measurements(){
		echoMeasurements(0);
		imgMeasurements();
		tempMeasurements();
		pitchMeasurements();
		audioMeasurements();
		copterMeasurements();
	}
	public void echoMeasurements(int delayMillis){
		Callable<Void> task= () -> {
			File f= new File(echof);
			FileOutputStream fout;
			try{
				fout= new FileOutputStream(f);
  			long startTime= System.currentTimeMillis()/1000;
  			while( System.currentTimeMillis()/1000 - startTime < 4){
  				long echoTimeMilli= System.nanoTime()/1000000;
  				s.send(echoc.getBytes());
  				s.receive(1000);
  				echoTimeMilli= System.nanoTime()/1000000-echoTimeMilli;
  				fout.write( (new Long(echoTimeMilli).toString()+"\n").getBytes() );
  				Thread.sleep(delayMillis);
  			}
			} catch(FileNotFoundException e){ System.err.println("Error opening file: "+f.getAbsolutePath()); }
			catch(IOException e){ System.err.println("Error writing to file: "+f.getAbsolutePath()); }
			return null;
		};
		tasks.push(pool.submit(task));
	}
	public void imgMeasurements(){}
	public void tempMeasurements(){
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
	public void pitchMeasurements(){}
	public void audioMeasurements(){}
	public void copterMeasurements(){}
	
	
	private String echoc, imgc, soundc, copterc;
	private String echof, imgf, tempf,	pitchf, audiof, copterf;
	private IthakiSocket s;
	
	private ExecutorService pool= Executors.newCachedThreadPool();
	private Stack<Future<Void>> tasks= new Stack();
}
