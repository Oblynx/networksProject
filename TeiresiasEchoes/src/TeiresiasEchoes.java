public class TeiresiasEchoes {

	public static void main(String[] args) {
		s.send(echoc.getBytes());
		//System.out.println(new String(s.receive(100)));
		
		measurer.take_measurements(0,4000);
	}
	
	
	private static int localPort=48006, ithakiPort=38006;
	private static String echoc="E0000", imgc="I", soundc="S", copterc="C";
	
	private static String echof="logs/echo"+echoc+".log", echof_nodelay="logs/echo"+echoc+"_nodelay.log",
					imgf1="logs/img1"+imgc+".jpg", imgf2="logs/img2"+imgc+".jpg", tempf="logs/temp"+echoc+".log",
					pitchf="logs/pitch"+soundc+".log",
					audiof="logs/audio"+soundc+".log", copterf="logs/copter"+copterc+".log";
	private static IthakiSocket s= new IthakiSocket(localPort, ithakiPort, 2000);
	
	//Executes the prescribed measurements
	private static Measurer measurer= new Measurer(s, echoc,imgc,soundc,copterc, echof,echof_nodelay,imgf1,imgf2,tempf,pitchf,audiof,copterf);
}
