import java.util.*;

public class userApplication {
	public static void main(String[] param){
		int serial= 3, echoMsgTime=10*60000, testsSucceeded=0, i;
		VirtualModem vm= new VirtualModem();
		ArrayList<Packet> echoes;
		vm.RXsetup(8000, 500);
		
		//Get packets for 4 minutes
		/*echoes= vm.echoPacketRX("TEST\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size()) { testsSucceeded++; System.out.println("Test1 finish"); }
		else System.out.println("Test1 INCOMPLETE!");
		
		if(!vm.imageRX("M3727\r", serial).incomplete) { testsSucceeded++; System.out.println("Test2 finish"); }
		else System.out.println("Test2 INCOMPLETE!");
		
		if(!vm.imageRX("G5384\r", serial).incomplete) { testsSucceeded++; System.out.println("Test3 finish"); }
		else System.out.println("Test3 INCOMPLETE!");*/
		
		if(!vm.gpsMapRX("P0906R=1004030\r", serial, 20).incomplete) { testsSucceeded++; System.out.println("Test4 finish"); }
		else System.out.println("Test4 INCOMPLETE!");
		
		echoes= vm.arqRX("Q8761\r", "R4356\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size()) { testsSucceeded++; System.out.println("Test5 finish"); }
		else System.out.println("Test5 INCOMPLETE!");
		
		System.out.println("\t--->  Tests succedeed: "+testsSucceeded+"/5  <---");
		vm.close();
	}
}
