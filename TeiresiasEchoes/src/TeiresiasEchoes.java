import java.util.Arrays;
import java.io.*;

public class TeiresiasEchoes {

	public static void main(String[] args) {
		s.send(echoc.getBytes());
		System.out.println(new String(s.receive(100)));
		
		measurer.tempMeasurements();
		//measurer.take_measurements();
	}
	
	
	private static int localPort=48006, ithakiPort=38006;
	private static String echoc="E0000", imgc="I", soundc="S", copterc="C";
	
	private static String echof="logs/echo"+echoc+".log", imgf="logs/img"+imgc+".log", tempf="logs/temp"+echoc+".log",
					pitchf="logs/pitch"+soundc+".log", audiof="logs/audio"+soundc+".log", copterf="logs/copter"+copterc+".log";
	private static IthakiSocket s= new IthakiSocket(localPort, ithakiPort, 2000);
	
	//Executes the prescribed measurements
	private static Measurer measurer= new Measurer(s, echoc,imgc,soundc,copterc, echof,imgf,tempf,pitchf,audiof,copterf);
}
