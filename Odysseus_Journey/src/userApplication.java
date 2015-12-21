import java.util.*;

public class userApplication {
	public static void main(String[] param){
		int serial= 6, echoMsgTime=10*60000, testsSucceeded=0, i;
		VirtualModem vm= new VirtualModem();
		ArrayList<Packet> echoes;
		vm.RXsetup(8000, 50000);
		
		//Get packets for 4 minutes
		echoes= vm.echoPacketRX("E8533\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size() && echoes.size() > 0) { testsSucceeded++; System.out.println("Test1 finish"); }
		else System.out.println("Test1 INCOMPLETE!");
		vm.close(); vm.RXsetup(8000, 50000);
		if(!vm.imageRX("M3603\r", serial).incomplete) { testsSucceeded++; System.out.println("Test2 finish"); }
		else System.out.println("Test2 INCOMPLETE!");
		vm.close(); vm.RXsetup(8000, 50000);
		if(!vm.imageRX("G1689\r", serial).incomplete) { testsSucceeded++; System.out.println("Test3 finish"); }
		else System.out.println("Test3 INCOMPLETE!");
		vm.close(); vm.RXsetup(8000, 50000);
		if(!vm.gpsMapRX("P8568R=1004040\r", serial, 10).incomplete) { testsSucceeded++; System.out.println("Test4 finish"); }
		else System.out.println("Test4 INCOMPLETE!");
		/*vm.close(); vm.RXsetup(8000, 50000);
		echoes= vm.arqRX("Q9183\r", "R0270\r", echoMsgTime, serial);
		for (i=0; i< echoes.size(); i++) if (echoes.get(i).incomplete) break;
		if (i == echoes.size() && echoes.size() > 0) { testsSucceeded++; System.out.println("Test5 finish"); }
		else System.out.println("Test5 INCOMPLETE!");*/
		
		System.out.println("\t--->  Tests succedeed: "+testsSucceeded+"/5  <---");
		vm.close();
	}
}
