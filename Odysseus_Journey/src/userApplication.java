import java.util.*;

public class userApplication {
	public static void main(String[] param){
		int serial= 8, echoMsgTime=5*60000, testsSucceeded=0, i, speed= 80000, timeout= 30*60000;
		VirtualModem vm= new VirtualModem();
		ArrayList<Packet> echoes;
		vm.RXsetup(speed, timeout);
		
		//Get packets for 4 minutes
		echoes= vm.echoPacketRX("E5296\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size() && echoes.size() > 0) { testsSucceeded++; System.out.println("Test1 finish"); }
		else System.out.println("Test1 INCOMPLETE!");
		vm.close(); vm.RXsetup(speed, timeout);
		if(!vm.imageRX("M0491\r", serial).incomplete) { testsSucceeded++; System.out.println("Test2 finish"); }
		else System.out.println("Test2 INCOMPLETE!");
		vm.close(); vm.RXsetup(speed, timeout);
		if(!vm.imageRX("G8399\r", serial).incomplete) { testsSucceeded++; System.out.println("Test3 finish"); }
		else System.out.println("Test3 INCOMPLETE!");
		vm.close(); vm.RXsetup(speed, timeout);
		if(!vm.gpsMapRX("P2379R=1004040\r", serial, 10).incomplete) { testsSucceeded++; System.out.println("Test4 finish"); }
		else System.out.println("Test4 INCOMPLETE!");
		vm.close(); vm.RXsetup(speed, timeout);
		echoes= vm.arqRX("Q7717\r", "R3840\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size() && echoes.size() > 0) { testsSucceeded++; System.out.println("Test5 finish"); }
		else System.out.println("Test5 INCOMPLETE!");
		
		System.out.println("\t--->  Tests succedeed: "+testsSucceeded+"/5  <---");
		vm.close();
	}
}
