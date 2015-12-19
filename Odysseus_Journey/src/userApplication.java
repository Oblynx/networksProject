
public class userApplication {
	public static void main(String[] param){
		VirtualModem vm= new VirtualModem();
		vm.RXsetup(8000, 500000);
		vm.echoPacketRX("E2063\r",2000);
		//vm.imageRX("M8649\rCAM=5\rSIZE=S\r");
		vm.imageRX("M8649\r", 1);
		vm.RXsetup(8000, 500000);
		vm.imageRX("G5741\r", 2);
		vm.close();
	}
}
