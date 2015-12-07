import ithakimodem.Modem;

public class VirtualModem {
	public static void main(String[] param){
		(new VirtualModem()).rx();
	}
	
	public void rx(){
		Modem modem= new Modem();
		char[] buffer= new char[5];
		for(int i=0; i<5; i++) buffer[i]=' ';
		int i=0, k;
		modem.setSpeed(8000);
		modem.setTimeout(2000);
		
		modem.write("ATd2310ithaki\r".getBytes());
		Boolean terminate= false;
		while(!terminate){
			try{
				modem.write("E6009\r".getBytes());
				k= modem.read();
				if (k==-1) terminate= true;
				else {
					System.out.print((char)k);
					if (i < 5)
						buffer[i++]= (char)k;
					else{
						//Shift buffer
						for(int b=0; b<4; b++) buffer[b]= buffer[b+1];
						buffer[4]= (char)k;
					}
				}
			} catch(Exception e){
				terminate= true;
			}
			if (!terminate){
				//System.out.println("\nBUFFER: "+new String(buffer));
				if (new String(buffer).equals("PSTOP")){
					System.out.print("\n");
				}
			}
		}
		modem.close();
	}
}
