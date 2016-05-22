import java.io.ByteArrayOutputStream;
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.Stack;

public class Measurer {

	public Measurer(IthakiSocket s, String echoc, String imgc, String soundc, String copterc, String echof,
			String echof_nodelay, String imgf1, String imgf2, String tempf, String tonef, String musicf,
			String copterf1, String copterf2){
		this.s=s; this.echoc= echoc; this.imgc= imgc; this.soundc= soundc; this.copterc= copterc; this.echof= echof;
		this.echof_nodelay= echof_nodelay; this.imgf1= imgf1; this.imgf2= imgf2;
		this.tempf= tempf; this.tonef= tonef; this.musicf= musicf;
		this.copterf1= copterf1; this.copterf2= copterf2;
		streamer= new AudioStreamer(pool, s, tonef, musicf);
	}
	
	public void take_measurements(int echoDelayMillis, int echototalMeasurementTimeMillis,
			int toneDuration, int flightlevel1, int flightlevel2){
		//echoMeasurements(echoDelayMillis, echototalMeasurementTimeMillis);
		//imgMeasurements();
		//tempMeasurements();
		soundMeasurements(toneDuration);
		copterMeasurements(flightlevel1, flightlevel2);

		// Wait for any running tasks to finish
		while(!tasks.empty()) try{
			tasks.pop().get();
		} catch(Exception e) { System.err.println(e.getMessage()); }
		pool.shutdown();
	}

	// output: timestamp<milli>;echodelay<milli>
	private void echoMeasurements(int delayMillis, int totalMeasurementTimeMillis){
		Callable<Void> task= () -> {
			Logger logger_delay= new Logger(echof), logger_nodelay= new Logger(echof_nodelay); 
			java.util.function.BiConsumer<Logger, String> measureAction= (logger, code) -> {
				try{
    			long startTime= System.currentTimeMillis(), curTime= 0;
    			while( curTime < totalMeasurementTimeMillis){
    				long echoTimeMilli= System.nanoTime()/1000000;
    				s.send(code.getBytes());
    				s.receive(100);
    				echoTimeMilli= System.nanoTime()/1000000-echoTimeMilli;
    				logger.log( (new Long(curTime).toString()+";"+new Long(echoTimeMilli).toString()+"\n").getBytes() );
    				Thread.sleep(delayMillis);
    				curTime= System.currentTimeMillis() - startTime;
    			}
				} catch (InterruptedException e) {}
			};
			
			// measure delay
			measureAction.accept(logger_delay, echoc);
			// measure no delay
			measureAction.accept(logger_nodelay, "E0000");
			return null;
		};
		tasks.push(pool.submit(task));
	}
	// Get 2 images
	private void imgMeasurements(){
		ByteArrayOutputStream image1= getImage(imgc), image2= getImage(imgc+"CAM=PTZ");
		Logger l1= new Logger(imgf1), l2= new Logger(imgf2);
    l1.log(image1.toByteArray());
    l2.log(image2.toByteArray());
	}
	// output: name;temperature
	private void tempMeasurements(){
		Logger logger= new Logger(tempf);
    String echomsg;
    for(int i=0;i<8;i++){
      s.send( (echoc+"T0"+new Integer(i).toString()).getBytes() );
      echomsg= new String(s.receive(100));
      //System.out.println(new Integer(i).toString()+":"+echomsg.substring(27, 46)+"|");
      String name=echomsg.substring(27,30); 
      if (name.startsWith("T")) {
        String temp=echomsg.substring(43,46);
        logger.log( (name+";"+temp+"\n").getBytes() );
      }
    }
	}
	private void soundMeasurements(int durationSec){
		boolean adaptive= false;
		// stream tone
		streamer.stream(soundc+"T", durationSec, (adaptive)? 16: 8, adaptive, true, "tone");
		streamer.waitToFinish();
		// stream music
		adaptive= true;
		streamer.stream(soundc+"F", durationSec, (adaptive)? 16: 8, adaptive, true, "music");
		streamer.waitToFinish();
		streamer.close();
	}
	private void copterMeasurements(int fl1, int fl2){
		CopterController ctrl= new CopterController(s);
		ctrl.setSessionTimeout(30);
		ctrl.setControlParams(new int[]{1,1,1});
		// session1
		ctrl.log(true, copterf1);
		ctrl.setFlightLevel(fl1);
		ctrl.start();
		//ctrl.waitTimeout();
		// session2
		ctrl.log(true,copterf2);
		ctrl.setFlightLevel(fl2);
		ctrl.start();
		//ctrl.waitTimeout();
	}
	
	private ByteArrayOutputStream getImage(String code){
		ByteArrayOutputStream image= new ByteArrayOutputStream();
		boolean imageEnd= false;
		s.send(code.getBytes());
		
		s.receive(0);
		while(!imageEnd){
			byte[] msg= s.receive(1*1024);
			int l= msg.length;
			image.write(msg,0,l);
			if ( msg[l-2]==(byte)0xFF && msg[l-1]==(byte)0xD9 ) imageEnd= true;
			//System.out.println(new Byte(msg[l-2]).toString()+new Byte(msg[l-1]).toString());
		}
		return image;
	}
	/*
	private ByteArrayOutputStream getSoundclip(String code, int durationSec, boolean adaptiveMode){
		assert(durationSec<32);
		int msgSize= (adaptiveMode)? 128+4: 128;
		ByteArrayOutputStream clip= new ByteArrayOutputStream();
		s.send((code+String.format("%03d", durationSec*32)).getBytes());
		for(int i=0; i<durationSec*32; i++)
			clip.write(s.receive(msgSize), 0, msgSize);
		return clip;
	}
	*/
	
	
	private String echoc, imgc, soundc, copterc;
	private String echof, echof_nodelay, imgf1, imgf2, tempf,	tonef, musicf, copterf1, copterf2;
	private IthakiSocket s;
	
	private ExecutorService pool= Executors.newCachedThreadPool();
	private Stack<Future<Void>> tasks= new Stack<Future<Void>>();
	private AudioStreamer streamer;
}
