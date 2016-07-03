import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.Stack;

public class Measurer {
	private float[] copterControlParams= new float[]{0.20f, 0.022f, 0.020f, 150f};
	
	public Measurer(IthakiSocket s, String echoc, String imgc, String soundc, String copterc, String echof,
			String echof_nodelay, String imgf1, String imgf2, String tempf, String tonef, String musicf,
			String copterf1, String copterf2){
		this.s=s; this.echoc= echoc; this.imgc= imgc; this.soundc= soundc; this.copterc= copterc; this.echof= echof;
		this.echof_nodelay= echof_nodelay; this.imgf1= imgf1; this.imgf2= imgf2;
		this.tempf= tempf; this.tonef= tonef; this.musicf= musicf;
		this.copterf1= copterf1; this.copterf2= copterf2;
	}
	
	public void take_measurements(int echoDelayMillis, int echototalMeasurementTimeMillis,
			int toneDuration){
		echoMeasurements(echoDelayMillis, echototalMeasurementTimeMillis);
		tempMeasurements();
		imgMeasurements();
		soundMeasurements(toneDuration);

		// Wait for any running tasks to finish
		while(!tasks.empty()) try{
			tasks.pop().get();
		} catch(Exception e) { System.err.println(e.getMessage()); }
		pool.shutdown();
	}
	public void take_copter_measurements(int flightlevel1, int flightlevel2, int timeSec, String logdir){
		copterMeasurements(flightlevel1, flightlevel2, timeSec, logdir);
	}

	// output: timestamp<milli>;echodelay<milli>
	private void echoMeasurements(int delayMillis, int totalMeasurementTimeMillis){
    Logger logger_delay= new Logger(echof), logger_nodelay= new Logger(echof_nodelay); 
    java.util.function.BiConsumer<Logger, String> measureAction= (logger, code) -> {
      try{
        long startTime= System.currentTimeMillis(), curTime= 0;
        while( curTime < totalMeasurementTimeMillis){
          long echoTimeMilli= System.nanoTime()/1000000;
          s.send(code.getBytes());
          try{ s.receive(100); } catch(SocketTimeoutException e) { break; }
          echoTimeMilli= System.nanoTime()/1000000-echoTimeMilli;
          logger.log( (new Long(curTime).toString()+";"+new Long(echoTimeMilli).toString()+"\n").getBytes() );
          Thread.sleep(delayMillis);
          curTime= System.currentTimeMillis() - startTime;
        }
      } catch (InterruptedException e) { e.printStackTrace(); }
    };
    
    // measure delay
    measureAction.accept(logger_delay, echoc);
    // measure no delay
    measureAction.accept(logger_nodelay, "E0000");
	}
	// Get 2 images
	private void imgMeasurements(){
		try{
      ByteArrayOutputStream image1= getImage(imgc), image2= getImage(imgc+"CAM=PTZ");
      Logger l1= new Logger(imgf1), l2= new Logger(imgf2);
      l1.log(image1.toByteArray());
      l2.log(image2.toByteArray());
		} catch(SocketTimeoutException e) { e.printStackTrace(); }
	}
	// output: name;temperature
	private void tempMeasurements(){
		Logger logger= new Logger(tempf);
    String echomsg;
    for(int i=0;i<8;i++){
    	String code= echoc+"T0"+Integer.toString(i);
      s.send( code.getBytes() );
      try{ echomsg= new String(s.receive(100)); } catch(SocketTimeoutException e) {
      	e.printStackTrace();
      	break;
      }
      System.out.println(Integer.toString(i)+":"+code+" -> "+echomsg/*.substring(27, 46)*/);
      String name=echomsg.substring(27,30); 
      if (name.startsWith("T")) {
        String temp=echomsg.substring(43,46);
        logger.log( (name+";"+temp+"\n").getBytes() );
      }
    }
	}
	private void soundMeasurements(int durationSec){
		streamer= new AudioStreamer(pool, s, tonef, musicf);
		boolean adaptive= false;
		// stream tone
		streamer.stream(soundc+"T", durationSec, (adaptive)? 16: 8, adaptive, true, "tone");
		streamer.waitToFinish();
		// stream music
		adaptive= true;
		streamer.stream(soundc+"AQF", durationSec, (adaptive)? 16: 8, adaptive, true, "music");
		streamer.waitToFinish();
		streamer.close();
	}
	private void copterMeasurements(int fl1, int fl2, int timeSec, String logdir){
		CopterController ctrl= new CopterController(s);
		ctrl.setSessionTimeout(timeSec);
		ctrl.setControlGains(copterControlParams, false);
		// session1
		ctrl.log(true, copterf1);
		ctrl.setFlightLevel(fl1);
		ctrl.start();
		// session2
		ctrl.log(true,copterf2);
		ctrl.setFlightLevel(fl2);
		ctrl.start();
		
		// Copy error dataset to current logging directory
    String[] command = {"cp", "logs/copter_error_dataset", logdir+"copter_error_dataset"};
		ProcessBuilder pb = new ProcessBuilder(command);	
	  try {
      pb.start();
		} catch (java.io.IOException e) { e.printStackTrace(); }
	}
	
	private ByteArrayOutputStream getImage(String code) throws SocketTimeoutException {
		ByteArrayOutputStream image= new ByteArrayOutputStream();
		boolean imageStart= false, imageEnd= false;
		
		s.send(code.getBytes());
    //init
		while(!imageStart){
			byte[] msg= s.receive(1024);
			int start=0;
			for (start=0; start<msg.length-1 && !imageStart; start++)
        if ( msg[start]==(byte)0xFF && msg[start+1]==(byte)0xD8 )
        	imageStart= true;
			start--;
			if(imageStart) image.write(msg, start, msg.length-start);
		}
		//get the rest
		while(!imageEnd){
			byte[] msg= s.receive(1024);
			int l= msg.length;
			image.write(msg,0,l);
			if ( msg[l-2]==(byte)0xFF && msg[l-1]==(byte)0xD9 ) imageEnd= true;
		}
		return image;
	}
	
	
	private String echoc, imgc, soundc, copterc;
	private String echof, echof_nodelay, imgf1, imgf2, tempf,	tonef, musicf, copterf1, copterf2;
	private IthakiSocket s;
	
	private ExecutorService pool= Executors.newCachedThreadPool();
	private Stack<Future<Void>> tasks= new Stack<Future<Void>>();
	private AudioStreamer streamer;
}
