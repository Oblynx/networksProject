
public class userApplication {
	public static void main(String[] param){
		VirtualModem vm= new VirtualModem();
		vm.RXsetup(8000, 5000);
		//vm.echoPacketRX("TEST\r");
		vm.imageRX("M0398\rCAM=5\rSIZE=S\r");
		vm.close();
	}
}
