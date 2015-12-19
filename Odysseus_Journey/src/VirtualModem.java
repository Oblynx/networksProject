import ithakimodem.Modem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public class VirtualModem {
	public void RXsetup(int speed, int timeout){
		modem= new Modem();
		modem.setSpeed(speed);
		modem.setTimeout(timeout);
		modem.write("ATd2310ithaki\r".getBytes());
	}
	public ArrayList<Packet> echoPacketRX(String code, long durationMillis){
		ArrayList<Packet> packets= new ArrayList<Packet>();
		long startTime= System.currentTimeMillis();
		while(System.currentTimeMillis()-startTime < durationMillis){
			Packet packet= getPacket(code, echoStart, echoEnd, 100);
			processEchoPacket(packet);
			packets.add(packet);
		}
		return packets;
	}
	//! Get 1 image from Ithaki
	public Packet imageRX(String code, Integer imgIdx){
		System.out.println("Image transfer begun");
		Packet packet= getPacket(code, jpgStart, jpgEnd, 120*1024);
		System.out.println("Image transfer COMPLETE!");
		processImage(packet, imgIdx);
		return packet;
	}
	public Packet gpsRX(String code){
		System.out.println("GPS receiving");
		Packet packet= getPacket(code, gpsStart, gpsEnd, 100);
		System.out.println("GPS RECEIVED!");
		processGPS(packet);
		ArrayList<Byte> empty= new ArrayList<Byte>();
		System.out.println("GPS addendum...");
		packet= getPacket("",empty,empty,100);
		System.out.println("GPS addendum RECEIVED!");
		for(byte b: packet.data)
			System.out.print((char)b);
		return packet;
	}
	public ArrayList<Integer> arqRX(String ack, String nack, long durationMillis){
		ArrayList<Integer> retries= new ArrayList<Integer>();
		long startTime= System.currentTimeMillis();
		while(System.currentTimeMillis()-startTime < durationMillis){
			int retry= 0;
			Packet packet= getPacket(ack, echoStart, echoEnd, 100);
			//If transmission error, resend
			while (errorARQ(packet)){
				packet= getPacket(nack, echoStart, echoEnd, 100);
				retry++;
			}
			retries.add(retry);
			//Got correct package
			processARQ(packet, retry);
		}
		return retries;
	}
	public void close(){ modem.close(); }
	
	// $$$$$  PRIVATE  $$$$$
	private void processEchoPacket(Packet packet){
		StringBuffer output= new StringBuffer();
		for(byte b: packet.data) output.append((char)b);
		System.out.println(output);
	}
	private void processImage(Packet packet, Integer index){
		Path path= Paths.get("./img/image"+index.toString()+".jpg");
		Byte[] image= new Byte[packet.size()];
		image= packet.data.toArray(image);
		try { Files.write(path, toPrimitives(image), StandardOpenOption.CREATE);
		} catch (IOException e) { e.printStackTrace(); }
		System.out.println("Image saved to file#"+index+". Timestamp: "+Instant.now());
	}
	private void processGPS(Packet packet){
		StringBuffer output= new StringBuffer();
		int j=36;
		for(byte b: packet.data){
			if (j==0) break;
			output.append((char)b);
			if (((char)b) == '\n') j--;
		}
		System.out.print(output);
		System.out.println("\t--> GPS END <--");
	}
	private boolean errorARQ(Packet packet){
		StringBuffer fcsBuf= new StringBuffer();
		byte[] hex= new byte[16];
		if(packet.size() != 58){
			System.out.println("Error: packet size= "+packet.size());
			throw new RuntimeException();
		}
		//<31bytes>HEX FCS<6bytes> 
		for(int i=31; i< 31+16; i++) hex[i-31]= packet.data.get(i);
		for(int i=49; i<52; i++) fcsBuf.append( (char) ((byte)packet.data.get(i)) );	
		//Parse fcs
		int fcs= Integer.parseInt(fcsBuf.toString());
		//Calculate HEX xor
		int fcsCheck= fcs(hex);
			//System.out.println("\nCalculated FCS: "+fcsCheck);
		return fcs != fcsCheck;
	}
	public int fcs(byte[] hex){
		int fcsCheck= (int)hex[0];
		for(int i=1; i<16; i++) fcsCheck^= (int)hex[i];
		return fcsCheck;
	}
	private void processARQ(Packet packet, int retries){
		processEchoPacket(packet);
		System.out.println("\t--> Retries= "+retries);
	}
	
	private Packet getPacket(String code, ArrayList<Byte> start, ArrayList<Byte> end, int capacity){
		ArrayList<Byte> sigStart= new ArrayList<Byte>(), sigEnd= new ArrayList<Byte>();
		ByteFlag mdk= new ByteFlag();
		boolean packetStart= false, packetIn= false, packetEnd= false;
		Packet curPack= new Packet();
		mdk.terminate= false;
		modem.write(code.getBytes());
		while(!mdk.terminate){
			mdk= readByte();
			if (!mdk.terminate){
				if(start.size() == 0) curPack.data.add((byte)mdk.k);
				else{
					// Update packet delimiter buffer
					if(sigStart.size() < start.size()) sigStart.add((byte)mdk.k);
					else {
						sigStart.remove(0);
						sigStart.add((byte)mdk.k);
					}
					if(sigEnd.size() < end.size()) sigEnd.add((byte)mdk.k);
					else {
						sigEnd.remove(0);
						sigEnd.add((byte)mdk.k);
					}
					// Signal accordingly
					boolean oldpacketStart= packetStart, oldpacketEnd= packetEnd;
					packetStart= sigStart.equals(start);
					packetEnd= sigEnd.equals(end);
					// On packet start...
					if (!oldpacketStart && packetStart){
						packetIn= true; 
						curPack.startTime= System.currentTimeMillis();
						curPack.data.ensureCapacity(capacity);
						for(int i=0; i<start.size()-1; i++) curPack.data.add(start.get(i));
					}
					// While in packet transmission
					if (packetIn) curPack.data.add((byte)mdk.k);
					// On packet end
					if (!oldpacketEnd && packetEnd){
						packetIn= false;
						curPack.endTime= System.currentTimeMillis();
						mdk.terminate= true;
					}
				}
			}
		}
		return curPack;
	}
	
	// $$$$$  Utils  $$$$$
	private byte[] toPrimitives(Byte[] oBytes){
	    byte[] bytes = new byte[oBytes.length];
	    for(int i = 0; i < oBytes.length; i++) { bytes[i] = oBytes[i]; }
	    return bytes;
	}
	private ByteFlag readByte(){
		ByteFlag data= new ByteFlag();
		try{
			data.k= modem.read();
			if (data.k==-1) data.terminate= true;
		} catch(Exception e){ data.terminate= true; }
		return data;
	}
	private static class ByteFlag{
		public int k= 0;
		public boolean terminate= false;
	}
	
	// $$$$$ Members $$$$$
	@SuppressWarnings("serial")
	private ArrayList<Byte> jpgStart= new ArrayList<Byte>() {{add((byte)0xFF); add((byte)0xD8);}};
	@SuppressWarnings("serial")
	private ArrayList<Byte> jpgEnd  = new ArrayList<Byte>() {{add((byte)0xFF); add((byte)0xD9);}};
	@SuppressWarnings("serial")
	private ArrayList<Byte> echoStart= new ArrayList<Byte>() {{ try{ for(byte b: "PSTART".getBytes("US-ASCII")) add(b); }
																catch (UnsupportedEncodingException e) {e.printStackTrace();} }};
	@SuppressWarnings("serial")
	private ArrayList<Byte> echoEnd= new ArrayList<Byte>()   {{ try{ for(byte b: "PSTOP".getBytes("US-ASCII")) add(b); }
																catch (UnsupportedEncodingException e) {e.printStackTrace();} }};
	@SuppressWarnings("serial")
	private ArrayList<Byte> gpsStart= new ArrayList<Byte>()
		{{ try{ for(byte b: "START ITHAKI GPS TRACKING\r\n".getBytes("US-ASCII")) add(b); }
		   catch (UnsupportedEncodingException e) {e.printStackTrace();} }};
	@SuppressWarnings("serial")
	private ArrayList<Byte> gpsEnd= new ArrayList<Byte>()
		{{ try{ for(byte b: "STOP ITHAKI GPS TRACKING\r\n".getBytes("US-ASCII")) add(b); }
		   catch (UnsupportedEncodingException e) {e.printStackTrace();} }};
	private Modem modem;
}
