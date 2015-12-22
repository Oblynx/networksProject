import java.util.*;

public class userApplication {
	public static void main(String[] param){
		int serial= 10, echoMsgTime=6*60000, testsSucceeded=0, i, speed= 8000, timeout= 30*60000;
		VirtualModem vm= new VirtualModem();
		ArrayList<Packet> echoes;
		vm.RXsetup(speed, timeout);
		
		//Get packets for 4 minutes
		echoes= vm.echoPacketRX("E6996\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size() && echoes.size() > 0) { testsSucceeded++; System.out.println("Test1 finish"); }
		else System.out.println("Test1 INCOMPLETE!");
		//vm.close(); vm.RXsetup(speed, timeout);
		if(!vm.imageRX("M4660\r", serial).incomplete) { testsSucceeded++; System.out.println("Test2 finish"); }
		else System.out.println("Test2 INCOMPLETE!");
		vm.close(); vm.RXsetup(speed, timeout);
		if(!vm.imageRX("G9539\r", serial).incomplete) { testsSucceeded++; System.out.println("Test3 finish"); }
		else System.out.println("Test3 INCOMPLETE!");
		vm.close(); vm.RXsetup(speed, timeout);
		if(!vm.gpsMapRX("P5987R=1004040\r", serial, 10).incomplete) { testsSucceeded++; System.out.println("Test4 finish"); }
		else System.out.println("Test4 INCOMPLETE!");
		vm.close(); vm.RXsetup(speed, timeout);
		echoes= vm.arqRX("Q4761\r", "R7581\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size() && echoes.size() > 0) { testsSucceeded++; System.out.println("Test5 finish"); }
		else System.out.println("Test5 INCOMPLETE!");
		
		System.out.println("\t--->  Tests succedeed: "+testsSucceeded+"/5  <---");
		vm.close();
	}
}
