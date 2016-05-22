
public class CopterController {
	public CopterController(IthakiSocket s) { this.s= s; }
	public void log(boolean enable, String fname){
		logEnabled= enable; logger= new Logger(fname);
	}
	public void setSessionTimeout(int timeoutSec) { this.timeoutSec= timeoutSec; }
	public void setFlightLevel(int fl) { this.flightlevel= fl; pid.setpoint= fl; }
	public void setControlParams(int[] params){ pid.params= params; }
	public void start(){
		assert(timeoutSec < 300);
		long startTime= System.currentTimeMillis();
		s.sendAutopilot(flightlevel, 100, 100);
		s.recAutopilot();
		s.sendAutopilot(flightlevel, 100, 100);
		while(System.currentTimeMillis() - startTime < timeoutSec*1000){
			IthakiSocket.CopterResponse resp= s.recAutopilot();
			int motor= pid.next(resp.altit);
			s.sendAutopilot(flightlevel, motor, motor);
			if (logEnabled) logger.log(resp.bytes());
		}
	}
	//public void interrupt();
	//public void waitTimeout();
	
	private PIDController pid= new PIDController();
	private int timeoutSec= 0, flightlevel= 0;
	private boolean logEnabled= true;
	private Logger logger;
	private IthakiSocket s;
}
