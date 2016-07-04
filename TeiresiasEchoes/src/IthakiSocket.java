import java.net.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @class Provides a high-level communication channel with the Ithaki server
 */
public class IthakiSocket {
	public IthakiSocket(int localPort, int ithakiPort, int timeout){
		this.localPort= localPort; this.ithakiPort= ithakiPort;
		try {
			ithakiAddress= InetAddress.getByName("ithaki.eng.auth.gr");
		} catch (UnknownHostException e1) { e1.printStackTrace(); }
		try {
			s= new DatagramSocket();
			r= new DatagramSocket(localPort);
			r.setSoTimeout(timeout);
			
			sTCP= new Socket(ithakiAddress, 38048);
			inTCP= sTCP.getInputStream();
			outTCP= sTCP.getOutputStream();
		} catch (SocketException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}
	public void send(byte[] data){
		DatagramPacket packet= new DatagramPacket(data, data.length, ithakiAddress, ithakiPort);
		try { s.send(packet);
		} catch (IOException e) { e.printStackTrace(); }
	}
	public byte[] receive(int size) throws SocketTimeoutException{
		byte[] data= new byte[size];
		DatagramPacket packet= new DatagramPacket(data, data.length);
		try { r.receive(packet);
		} catch(IOException e) {
			if(e.getClass() == SocketTimeoutException.class) {
				throw new SocketTimeoutException();
			}
		}
		byte[] datarcv= new byte[packet.getLength()];
		for(int i=0; i<datarcv.length; i++) datarcv[i]= data[i];
		return datarcv;
	}
	
	public void close(){
		s.close(); r.close();
	}
	
	public void sendAutopilot(int flightlevel, int lmotor, int rmotor){
		try{
  		outTCP.write(String.format(copterReqFormat,
  				flightlevel, lmotor, rmotor).getBytes());
		} catch(IOException e) { e.printStackTrace(); }
	}
	public CopterResponse rcvAutopilot(){
    byte[] buf= new byte[copterRespLength];
    int bytesRead= 0;
		try{
      while(bytesRead == 0) bytesRead= inTCP.read(buf);
		} catch(IOException e) { e.printStackTrace(); }
		String response= new String(buf);
		//System.out.println("[rcv]: length="+bytesRead);
		return parseCopterResponse(response);
	}
	public void initAutopilot(){
		byte[] buf= new byte[512];
		try {
			do{
        sendAutopilot(100,100,100);
        do{
          inTCP.read(buf);
        } while(inTCP.available() > 0);
			}while( parseCopterResponse(new String(buf)).isZero() );
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	private CopterResponse parseCopterResponse(String response){
		int l=0,r=0,a=0;
		float t=0,p=0;
		if (response.startsWith("ITHAKICOPTER")){
      //System.out.print("[parser]: "+response);
      Scanner scanner= new Scanner(response);
      scanner.useDelimiter("[=\\s]");
      try{
        while(!scanner.hasNextInt()) scanner.next();
        l= scanner.nextInt();
        while(!scanner.hasNextInt()) scanner.next();
        r= scanner.nextInt();
        while(!scanner.hasNextInt()) scanner.next();
        a= scanner.nextInt();
        while(!scanner.hasNextFloat()) scanner.next();
        t= scanner.nextFloat();
        while(!scanner.hasNextFloat()) scanner.next();
        p= scanner.nextFloat();
      } catch(java.util.InputMismatchException e) { System.out.println("[parser]: error!"); }
      catch(java.util.NoSuchElementException e) { System.out.println("[parser]: no more tokens!"); }
      scanner.close();
		}
		return new CopterResponse(l,r,a,t,p);
	}

	private InetAddress ithakiAddress;
	private int localPort;
	private int ithakiPort;
	private DatagramSocket s,r;
	private Socket sTCP;
	private InputStream inTCP;
	private OutputStream outTCP;
	
	private String copterReqFormat="AUTO FLIGHTLEVEL=%03d LMOTOR=%03d RMOTOR=%03d PILOT \r\n",
								 copterResFormat="ITHAKICOPTER LMOTOR=%03d RMOTOR=%03d ALTITUDE=%03d "
								 								 + "TEMPERATURE=+%2.2f PRESSURE=%4.2fTELEMETRY \r\n";
  private int copterRespLength= String.format(copterResFormat, 111,111,111,11.11,1111.11).length()+1;
	
	public class CopterResponse{
		public CopterResponse(int l, int r, int a, float t, float p){
			lmotor= l; rmotor= r; altit= a; temper= t; pressure= p;
		}
		/*public CopterResponse(int[] v){
			assert(v.length == 5);
			lmotor= v[0]; rmotor= v[1]; altit= v[2]; temper= v[3]; pressure= v[4];
		}*/
		/** Log format: "lmotor rmotor alt temp press\n"
		 */
		public byte[] bytes(){
			return (new String(Integer.toString(lmotor)+" "+ Integer.toString(rmotor)+" "+
							Integer.toString(altit)+" "+ Float.toString(temper)+" "+ Float.toString(pressure)+"\n")
						 ).getBytes();
		}
		public boolean isZero() { return (lmotor|rmotor|altit)==0 && temper==0 && pressure==0; } 
		public int lmotor, rmotor, altit;
		public float temper, pressure;
	}
}
