import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
		} catch (SocketException e1) { e1.printStackTrace(); }
	}
	public void send(byte[] data){
		packet= new DatagramPacket(data, data.length, ithakiAddress, ithakiPort);
		try { s.send(packet);
		} catch (IOException e) { e.printStackTrace(); }
	}
	public byte[] receive(int size){// throws java.net.SocketTimeoutException{
		byte[] data= new byte[size];
		packet= new DatagramPacket(data, data.length);
		try { r.receive(packet);
		} catch (IOException e) {	e.printStackTrace(); }
		byte[] datarcv= new byte[packet.getLength()];
		for(int i=0; i<datarcv.length; i++) datarcv[i]= data[i];
		return datarcv;
	}
	
	public void close(){
		s.close(); r.close();
	}

	private InetAddress ithakiAddress;
	private int localPort;
	private int ithakiPort;
	private DatagramSocket s,r;
	private DatagramPacket packet;
}
