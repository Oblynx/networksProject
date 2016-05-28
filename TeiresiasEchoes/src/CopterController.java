
public class CopterController {
	public CopterController(IthakiSocket s) { this.s= s; }
	public void log(boolean enable, String fname){
		logEnabled= enable; logger= new Logger(fname);
	}
	public void setSessionTimeout(int timeoutSec) { this.timeoutSec= timeoutSec; }
	public void setFlightLevel(int fl) { this.flightlevel= fl; pid.setpoint= fl; }
	public void setControlParams(float[] params){
		// Schedule gains to implement nonlinear control
		float[] decisionPoints= {0,60,100}, paramScales= {1/2f,1/1.5f,1f, 1f,1/2f,1/3f};

		pid.params= params;
		pid.decisionPoints= decisionPoints.clone();
		pid.paramScales= paramScales.clone();
	}
	public void start(){
		assert(timeoutSec < 200);
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
	
	private PIDController pid= new PIDController(30,70, 100,190);
	private int timeoutSec= 0, flightlevel= 0;
	private boolean logEnabled= true;
	private Logger logger;
	private IthakiSocket s;
}
