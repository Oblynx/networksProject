import java.io.IOException;
import java.util.ArrayList;

public class PIDController {
	public PIDController(int filtOrder, int initErr, int min_control, int max_control){
		this.filtOrder= filtOrder;
		this.max_control= max_control; this.min_control= min_control;
		errBuf= new CircularArrayList<Integer>(filtOrder);
		// Fill filter taps with starting value
		for(int i=0; i<errBuf.capacity(); i++) errBuf.add(initErr);
		errSum= initErr*filtOrder; err2Sum= initErr*initErr*filtOrder;
		
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
			errSum-= errBuf.get(0);
			err2Sum-= errBuf.get(0)*errBuf.get(0);
	    errBuf.remove(0);
	  }
	  errBuf.add(setpoint-y);
	  int bufend= errBuf.size()-1;
	  errSum+= errBuf.get(bufend);
	  err2Sum+= errBuf.get(bufend)*errBuf.get(bufend);
	  // Log
		double currentTimeSec= (System.nanoTime() - startTime)/1E9;
		plotUpdater.log( (currentTimeSec+" "+(setpoint-y)+"\n").getBytes() );

	  // Calc responses
		float errVar= (filtOrder*err2Sum - errSum*errSum)/(filtOrder*(filtOrder-1));
		float up=0,ui=0,ud=0; 
		up= errBuf.get(bufend);
		ui= (float) (errSum/(0.002*errVar+1));
		ud= (bufend>0)? (errBuf.get(bufend) - errBuf.get(bufend-1)): 0;
		
		// Calc mean error to select appropriate gain model from the schedule
		// Gain model will be applied to proportional and integral response (derivative not affected)
		float[] gainModel_p= gainSchedule.getModel(up), gainModel_i= gainSchedule.getModel(ui/filtOrder);
		// Apply gains
		up= gains[0]*(gainModel_p[0]*up+gainModel_p[1]);
		ui= gains[1]*(gainModel_i[0]*ui+gainModel_i[1]);
		ud*= gains[2];
		int result= (int) (up+ui+ud+gains[3]);
		if (result > max_control) result= max_control;
		else if (result < min_control) result= min_control;
		// Output result
		System.out.println("[pid]: up="+up+" ui="+ui+" ud="+ud);
		return result;
	}
	
	public void startClock(){
		startTime= System.nanoTime();
	}
	
	// Don't implement gain scheduling. Default behaviour
	public void nullGainSchedule(){
		gainSchedule= new GainSchedule();
	}
	public void createGainSchedule(float[] d, float[] a, int k, int c_k){
		gainSchedule= new GainSchedule(d, a, k, c_k);
	}
	
	public int setpoint;
	public float[] gains;
	public GainSchedule gainSchedule= new GainSchedule();
	
	private CircularArrayList<Integer> errBuf;
	private float errSum=0, err2Sum=0;
	private int filtOrder, max_control, min_control;
	private long startTime;
	Logger plotUpdater= new Logger("logs/copter_error_dataset");
	
	/** Implements a piecewise linear gain scheduling. Decision points MUST be in ascending order!
	 */
	public class GainSchedule{
		// Generate proper model
		public GainSchedule(float[] d, float[] a, int k, int c_k){
			decisionPoints= d.clone();
			scales= a.clone();
			contConst= findContinuityConsts(d,a,k,c_k);
		}
		// Generate null model
		public GainSchedule(){
			nullSchedule= true;
		}
		
		// Returns scaling and constant parameter
		public float[] getModel(float e){
			if (nullSchedule) return new float[] {1, 0};

			int idx= 0;
			boolean foundIdx= false;
			for(int i=0; i<decisionPoints.length && !foundIdx; i++){
				if (e < decisionPoints[i]){
					idx= i;
					foundIdx= true;
				}
			}
			if(!foundIdx) idx= decisionPoints.length;
			return new float[]{scales[idx], contConst[idx]};
		}
		
    /** Calculate continuity constants for piecewise linear function {y(x)= a_i*x for (d_i-1 < x <= d_i)}
     * @param d Decision points (length=l)
     * @param a Scales (length=l+1)
     * @param k Index of known constant
     * @param c_k Value of known constant 
     * @return Continuity constants (length=l+1)
     */
    private float[] findContinuityConsts(float[] d, float[] a, int k, int c_k){
      if(a.length != d.length+1) throw new IllegalArgumentException("Wrong array size");
      if(k < 0 || k > d.length) throw new IllegalArgumentException("Illegal k value");
      
      float[] c= new float[a.length];
      c[k]= c_k;
      for(int i=k-1; i>=0; i--){
        c[i]= a[i+1]*d[i] - a[i]*d[i] + c[i+1]; 
      }
      for(int i=k+1; i<=d.length; i++){
        c[i]= c[i-1] - a[i]*d[i-1] + a[i-1]*d[i-1];
      }
      return c;
    }
	
		private float[] decisionPoints, scales, contConst;
		private boolean nullSchedule= false;
	}
}
