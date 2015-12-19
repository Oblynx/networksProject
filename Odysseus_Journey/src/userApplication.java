
public class userApplication {
	public static void main(String[] param){
		VirtualModem vm= new VirtualModem();
		vm.RXsetup(8000, 500000);
		vm.echoPacketRX("TEST\r",100);
		//vm.imageRX("M8854 CAM=PTZ\r", 7);
		//vm.imageRX("M8649\r", 1);
		//vm.imageRX("G5741\r", 2);
		//vm.gpsRX("P3613 T=403725225726\r");
		vm.arqRX("Q6268\r", "R5031\r", 1000);
		vm.close();
	}
}
