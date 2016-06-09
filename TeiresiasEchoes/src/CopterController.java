
public class CopterController {
	public CopterController(IthakiSocket s) { this.s= s; }
	public void log(boolean enable, String fname){
		logEnabled= enable; logger= new Logger(fname);
	}
	public void setSessionTimeout(int timeoutSec) { this.timeoutSec= timeoutSec; }
	public void setFlightLevel(int fl) { this.flightlevel= fl; pid.setpoint= fl; }
	public void setControlGains(float[] gains, boolean gainScheduling){
		pid.gains= gains;
		if (gainScheduling){
      // Schedule gains to implement nonlinear control
      float[] decisionPoints= {-100,-60,-15,65,90}, paramScales= {0.65f,0.8f,0.9f, 1f,0.65f,0.45f};
      pid.createGainSchedule(decisionPoints, paramScales, 3, 0);
		}
	}
	public void start(){
		assert(timeoutSec < 200);
		if (logEnabled) logger.log((flightlevel+"\n").getBytes());
		long startTime= System.currentTimeMillis();
		s.initAutopilot();
		pid.startClock();
		s.sendAutopilot(flightlevel, 100, 100);
		while(System.currentTimeMillis() - startTime < timeoutSec*1000){
			IthakiSocket.CopterResponse resp= s.rcvAutopilot();
			System.out.print("[controller]: status="+new String(resp.bytes()));
			int u= pid.next(resp.altit);
			System.out.println("[controller]: u="+u+" err="+(flightlevel-resp.altit)+"\n");
			s.sendAutopilot(flightlevel, u, u);
			if (logEnabled) logger.log(resp.bytes());
		}
	}
	
	private PIDController pid= new PIDController(70,0, 100,200);
	private int timeoutSec= 0, flightlevel= 0;
	private boolean logEnabled= true;
	private Logger logger;
	private IthakiSocket s;
}
