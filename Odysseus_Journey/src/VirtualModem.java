import ithakimodem.Modem;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class VirtualModem {
	public void RXsetup(int speed, int timeout){
		modem.setSpeed(speed);
		modem.setTimeout(timeout);
		modem.write("ATd2310ithaki\r".getBytes());
	}
	public void echoPacketRX(String code){
		StringBuffer sigStart= new StringBuffer(), sigEnd= new StringBuffer(), packet= new StringBuffer();
		ByteFlag mdk= new ByteFlag();
		boolean packetStart= false, packetIn= false, packetEnd= false;
		mdk.terminate= false;
		
		modem.write(code.getBytes());
		while(!mdk.terminate){
			mdk= readByte();
			if (!mdk.terminate){
				// Update packet delimiter buffers
				if(sigStart.length() < 6) sigStart.append((char)mdk.k);
				else {
					sigStart.deleteCharAt(0);
					sigStart.append((char)mdk.k);
				}
				if(sigEnd.length() < 5) sigEnd.append((char)mdk.k);
				else {
					sigEnd.deleteCharAt(0);
					sigEnd.append((char)mdk.k);
				}
				// Signal accordingly
				boolean oldpacketStart= packetStart, oldpacketEnd= packetEnd;
				packetStart= sigStart.toString().equals("PSTART");
				packetEnd= sigEnd.toString().equals("PSTOP");
				// On packet start...
				if (!oldpacketStart && packetStart){
					packetIn= true;
					packet.setLength(0);
					packet.append(sigStart.deleteCharAt(6-1).toString());
				}
				// While in packet transmission
				if (packetIn) packet.append((char)mdk.k);
				// On packet end
				if (!oldpacketEnd && packetEnd){
					packetIn= false;
					processEchoPacket(packet);
					modem.write(code.getBytes());
					mdk.terminate= true;
				}
			}
		}
	}
	public void imageRX(String code, int imgIdx){
		ArrayList<Byte> sigStartEnd= new ArrayList<Byte>(), packet= new ArrayList<Byte>();
		packet.ensureCapacity(120*1024);
		ByteFlag mdk= new ByteFlag();
		boolean packetStart= false, packetIn= false, packetEnd= false;
		mdk.terminate= false;
		
		modem.write(code.getBytes());
		while(!mdk.terminate){
			mdk= readByte();
			if (!mdk.terminate){
				// Update packet delimiter buffer
				if(sigStartEnd.size() < 2) sigStartEnd.add(new Integer(mdk.k).byteValue());
				else {
					sigStartEnd.remove(0);
					sigStartEnd.add(new Integer(mdk.k).byteValue());
				}
				// Signal accordingly
				boolean oldpacketStart= packetStart, oldpacketEnd= packetEnd;
				packetStart= sigStartEnd.equals(jpgStart);
				packetEnd= sigStartEnd.equals(jpgEnd);
				// On packet start...
				if (!oldpacketStart && packetStart){
					packetIn= true;
					packet.clear();
					packet.add((byte)0xFF);
					System.out.println("Image transfer begun");
				}
				// While in packet transmission
				if (packetIn) packet.add(new Integer(mdk.k).byteValue());
				// On packet end
				if (!oldpacketEnd && packetEnd){
					System.out.println("Image transfer COMPLETE!");
					packetIn= false;
					processImage(packet, imgIdx);
					modem.write(code.getBytes());
					System.out.println("Image saved to file.");
					mdk.terminate= true;
				}
			}
		}
	}
	
	public void close(){ modem.close(); }
	
	private void processEchoPacket(StringBuffer packet){
		System.out.println(packet);
	}
	private void processImage(ArrayList<Byte> packet, Integer index){
		Path path= Paths.get("./image"+index.toString()+".jpg");
		Byte[] image= new Byte[packet.size()];
		image= packet.toArray(image);
		try {
			Files.write(path, toPrimitives(image), StandardOpenOption.CREATE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// $$$$$  Utils  $$$$$
	private byte[] toPrimitives(Byte[] oBytes){
	    byte[] bytes = new byte[oBytes.length];
	    for(int i = 0; i < oBytes.length; i++) {
	        bytes[i] = oBytes[i];
	    }
	    return bytes;
	}
	private ByteFlag readByte(){
		ByteFlag data= new ByteFlag();
		try{
			data.k= modem.read();
			if (data.k==-1) data.terminate= true;
		} catch(Exception e){
			data.terminate= true;
		}
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
	private Modem modem= new Modem();
}
