import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;

public class testConnection_standalone {

	public static void main(String[] args) throws IOException {
		System.out.println("Connecting...");
		while(true){
			byte[] buf= "E0000".getBytes(), receiveBuf= new byte[1024*4];
			packet= new DatagramPacket(buf, buf.length, ithakiAddress, ithakiPort);
			System.out.println("Sending...");
			s.send(packet);
			
			packet= new DatagramPacket(receiveBuf, receiveBuf.length);
			
			r.receive(packet);
			String received = new String(packet.getData(), 0, packet.getLength());
			System.out.println("Quote of the Moment: " + received);
		}
	}


	private static InetAddress ithakiAddress;
	private static int localPort= 48027;
	private static int ithakiPort= 38027;
	private static DatagramSocket s,r;
	private static DatagramPacket packet;
	static{
		try {
			ithakiAddress= InetAddress.getByName("ithaki.eng.auth.gr");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		try {
			s= new DatagramSocket();
			r= new DatagramSocket(localPort);
			r.setSoTimeout(3000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
	}
}
