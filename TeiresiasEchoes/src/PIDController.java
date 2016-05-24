import java.io.IOException;
import java.util.ArrayList;

public class PIDController {
	public PIDController(int filtOrder, int initErr, int min_control, int max_control){
		this.filtOrder= filtOrder;
		this.max_control= max_control; this.min_control= min_control;
		errBuf= new CircularArrayList<Integer>(filtOrder);
		// Fill filter taps with starting value
		for(int i=0; i<errBuf.capacity(); i++) errBuf.add(initErr);
		
		//PLOT
    plotUpdater.log(("0 "+initErr+"\n").getBytes());
    String[] command = {"/usr/bin/gnuplot", "src/ithakicopterErrorPlot.gpt"};
		ProcessBuilder pb = new ProcessBuilder(command);	
	  try {
      pb.start();
		} catch (IOException e) { e.printStackTrace(); }
	  //PLOT
	}
	public int next(int y){
		if(errBuf.size() == errBuf.capacity()){
	    errBuf.remove(0);
	  }
	  errBuf.add(setpoint-y);
	  int bufend= errBuf.size()-1;
	  
		float up=0,ui=0,ud=0;
		// proportional is actually a short-time integral
		for(int i=bufend; i>bufend-3; i--) up+= errBuf.get(i);
		up*= params[0];
		for(int i=0; i<=bufend; i++) ui+= errBuf.get(i);
		ui*= params[1];
		ud= (bufend>0)? params[2]*(errBuf.get(bufend) - errBuf.get(bufend-1)): 0;
		System.out.println("[pid]: up="+up+" ui="+ui+" ud="+ud);
		
		double currentTimeSec= (System.nanoTime() - startTime)/1E9;
		plotUpdater.log( (currentTimeSec+" "+(setpoint-y)+"\n").getBytes() );
		int result= (int) (up+ui+ud+params[3]);
		if (result > max_control) result= max_control;
		else if (result < min_control) result= min_control;
		return result;
	}
	
	public void startClock(){
		startTime= System.nanoTime();
	}
	
	public int setpoint;
	public float[] params;
	private CircularArrayList<Integer> errBuf;
	private int filtOrder, max_control, min_control;
	private long startTime;
	Logger plotUpdater= new Logger("logs/copter_error_dataset");
}
