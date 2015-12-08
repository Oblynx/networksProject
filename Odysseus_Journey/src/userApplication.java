
public class userApplication {
	public static void main(String[] param){
		VirtualModem vm= new VirtualModem();
		vm.RXsetup(2000, 2000);
		vm.echoPacketRX("TEST\r");
		//vm.imageRX("");
		
		vm.close();
	}
}
