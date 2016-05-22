import java.net.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
	public byte[] receive(int size){// throws java.net.SocketTimeoutException{
		byte[] data= new byte[size];
		DatagramPacket packet= new DatagramPacket(data, data.length);
		try { r.receive(packet);
		} catch (IOException e) {	e.printStackTrace(); }
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
	public CopterResponse recAutopilot(){
    int respLength= String.format(copterResFormat, 111,111,111,11.11,1111.11).length();
    byte[] buf= new byte[respLength];
		try{
      int bytesRead=0;
      do{
        bytesRead= bytesRead + inTCP.read(buf, bytesRead, respLength);
      } while(bytesRead < respLength);
		} catch(IOException e) { e.printStackTrace(); }
		String response= new String(buf);
		return parseCopterResponse(response);
	}
	
	private CopterResponse parseCopterResponse(String response){
		int l,r,a;
		float t,p;
		Scanner scanner= new Scanner(response), partScanner;
		scanner.useDelimiter("[=\\s]");
		l= scanner.nextInt();
		r= scanner.nextInt();
		a= scanner.nextInt();
		t= scanner.nextFloat();
		p= scanner.nextFloat();
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
								 copterResFormat="ITHAKICOPTER LMOTOR=%03d RMOTOR=%3d ALTITUDE=%3d "
								 								 + "TEMPERATURE=+%2.2f PRESSURE=%4.2fTELEMETRY \r\n";
	
	public class CopterResponse{
		public CopterResponse(int l, int r, int a, float t, float p){
			lmotor= l; rmotor= r; altit= a; temper= t; pressure= p;
		}
		public CopterResponse(int[] v){
			assert(v.length == 5);
			lmotor= v[0]; rmotor= v[1]; altit= v[2]; temper= v[3]; pressure= v[4];
		}
		public byte[] bytes(){
			return (new String(Integer.toString(lmotor)+" "+ Integer.toString(rmotor)+" "+
							Integer.toString(altit)+" "+ Float.toString(temper)+" "+ Float.toString(pressure)+"\n")
						 ).getBytes();
		}
		public int lmotor, rmotor, altit;
		public float temper, pressure;
	}
}
