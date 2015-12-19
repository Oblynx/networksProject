import ithakimodem.Modem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.*;
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
		Packet packet= getPacket(code, gpsStart, gpsEnd, );
		System.out.println("GPS RECEIVED!");
		processGPS(packet);
		return packet;
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
		System.out.println("Image saved to file.");
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
	private ArrayList<Byte> echoEnd= new ArrayList<Byte>()   {{ try{ for(byte b: "PSTOP".getBytes("US-ASCII")) add(b);  }
																catch (UnsupportedEncodingException e) {e.printStackTrace();} }};
	private Modem modem;
}
