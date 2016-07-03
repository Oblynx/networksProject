import java.text.*;
import java.io.File;
import java.net.SocketTimeoutException;
import java.util.*;

public class TeiresiasEchoes {
	// Parameters
	private static int localPort=48010, ithakiPort=38010;
	private static String echoc="E1860", imgc="M8057", soundc="V1588", copterc="Q5015";
	
	public static void main(String[] args) {
		prepareLoggingDir();
    //Executes the prescribed measurements
	  Measurer measurer= new Measurer(s, echoc,imgc,soundc,copterc, echof,echof_nodelay,
			imgf1,imgf2,tempf,tonef,musicf,copterf1,copterf2);
		measurer.take_copter_measurements(160,230, 40, logdir);
//		measurer.take_measurements(0,4*60*1000,30);
		s.close();
	}
	private static void prepareLoggingDir(){
		DateFormat df = new SimpleDateFormat("dd-MM HH:mm:ss");
		Date today = Calendar.getInstance().getTime();
		logdir= "logs/"+df.format(today)+"/";
		
		new File(logdir).mkdirs();
		echof= logdir+echof; echof_nodelay= logdir+echof_nodelay; imgf1= logdir+imgf1;
		imgf2= logdir+imgf2; tempf= logdir+tempf; tonef= logdir+tonef; musicf= logdir+musicf;
		copterf1= logdir+copterf1; copterf2= logdir+copterf2;
	}
	
	private static String echof="echo"+echoc+".log", echof_nodelay="echo"+echoc+"_nodelay.log",
					imgf1="img1"+imgc+".jpg", imgf2="img2"+imgc+".jpg", tempf="temp"+echoc+".log",
					tonef="tone"+soundc+".log", musicf="music"+soundc+".log",
					copterf1="copter_fl1"+copterc+".log",	copterf2="copter_fl2"+copterc+".log", logdir;
	private static IthakiSocket s= new IthakiSocket(localPort, ithakiPort, 5000);
}
