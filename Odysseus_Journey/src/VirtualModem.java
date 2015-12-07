import ithakimodem.Modem;

public class VirtualModem {
	public static void main(String[] param){
		(new VirtualModem()).rx();
	}
	
	public void rx(){
		Modem modem= new Modem();
		modem.setSpeed(1000);
		modem.setTimeout(2000);
		int k;
		
		modem.write("ATd2310ithaki\r".getBytes());
		//modem.write("ATH0\r".getBytes());
		while(true){
			try{
				k= modem.read();
				if (k==-1) break;
				System.out.print((char)k);
			} catch(Exception e){
				break;
			}
		}
		modem.close();
	}
}
