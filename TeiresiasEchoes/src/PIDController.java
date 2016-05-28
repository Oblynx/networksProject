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
	  // Log
		double currentTimeSec= (System.nanoTime() - startTime)/1E9;
		plotUpdater.log( (currentTimeSec+" "+(setpoint-y)+"\n").getBytes() );
		
		
		int lookback= 5;
		float meanErr= 0;
		for(int i=bufend; i> bufend-lookback; i--) meanErr+= errBuf.get(i);
		meanErr/= lookback;
		float scale= selectScale(meanErr);

	  // Calc responses
		float up=0,ui=0,ud=0;
		up= errBuf.get(bufend);
		for(int i=0; i<=bufend; i++) ui+= errBuf.get(i);
		ui/= filtOrder;
		ud= (bufend>0)? (errBuf.get(bufend) - errBuf.get(bufend-1)): 0;
		// Add nonlinearity
		up*= params[0]*scale; ui*= params[1]*scale; ud*= params[2]*scale;
		int result= (int) (up+ui+ud+params[3]);
		if (result > max_control) result= max_control;
		else if (result < min_control) result= min_control;
		// Output result
		System.out.println("[pid]: up="+up+" ui="+ui+" ud="+ud);
		return result;
	}
	
	public void startClock(){
		startTime= System.nanoTime();
	}
	
	private float selectScale(float meanErr){
		float selectedScale= 1;
		boolean foundInterval;
		// For proportional && integral gains
    foundInterval= false;
    for(int dp= decisionPoints.length-1; dp>-1; dp--){
      if (meanErr < -decisionPoints[dp]){
        selectedScale= paramScales[decisionPoints.length-1 - dp];
        foundInterval= true;
        break;
      }
    }
    if (!foundInterval)
      for(int dp= 1; dp<decisionPoints.length; dp++){
        if (meanErr < decisionPoints[dp]){
          selectedScale= paramScales[decisionPoints.length-1 + dp];
          foundInterval= true;
          break;
        }
      }
    if (!foundInterval)
      selectedScale= paramScales[2*decisionPoints.length-1];
		return selectedScale;
	}
	
	public int setpoint;
	public float[] params, decisionPoints, paramScales;
	private CircularArrayList<Integer> errBuf;
	private int filtOrder, max_control, min_control;
	private long startTime;
	Logger plotUpdater= new Logger("logs/copter_error_dataset");
}
