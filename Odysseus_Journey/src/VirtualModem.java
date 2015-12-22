import ithakimodem.Modem;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public class VirtualModem {
	//! Dial Ithaki
	public void RXsetup(int speed, int timeout){
		modem= new Modem();
		modem.setSpeed(speed);
		modem.setTimeout(timeout);
		modem.write("ATd2310ithaki\r".getBytes());
	}
	//! Echoes what the modem says to the console
	public void echoModem(String code){
		getPacket(code, new ArrayList<Byte>(), new ArrayList<Byte>(), 100);
	}
	//! Request echo packages continuously, until durationMillis time has passed.
	public ArrayList<Packet> echoPacketRX(String code, long durationMillis, int serial){
		ArrayList<Packet> packets= new ArrayList<Packet>();
		long startTime= System.currentTimeMillis();
		while(System.currentTimeMillis()-startTime < durationMillis){
			Packet packet= getPacket(code, echoStart, echoEnd, 100);
			processEchoPacket(packet, serial);
			packets.add(packet);
		}
		return packets;
	}
	//! Request 1 image from Ithaki and store it to file
	public Packet imageRX(String code, int serial){
		System.out.println("Image transfer begun");
		Packet packet= getPacket(code, jpgStart, jpgEnd, 120*1024);
		String imgName= (code.charAt(0) == 'M')? "image":(code.charAt(0)=='G')? "noise": "map";
		if (!packet.incomplete){
			System.out.println("Image transfer COMPLETE!");
			processImage(packet, imgName, serial);
		} else {
			System.out.println("TIMEOUT! Image transfer FAILED!");
		}
		return packet;
	}
	//! Request a GPS packet, calculate coordinates and request annotated map
	public Packet gpsMapRX(String code, Integer imgIdx, int secBetweenPos){
		System.out.println("GPS receiving");
		Packet packet= getPacket(code, gpsStart, gpsEnd, 100);
		if (packet.incomplete) {
			System.out.println("Error! Packet transfer TIMEDOUT!");
			throw new RuntimeException();
		}
		System.out.println("GPS RECEIVED!");
		String posCode= positionFromGPS(packet, code, secBetweenPos);
		System.out.println("Generated code:  --> "+posCode+"\nGetting map...");
		return imageRX(posCode, imgIdx);
	}
	//! Implement ARQ mechanism to countermeasure transmission errors 
	public ArrayList<Packet> arqRX(String ack, String nack, long durationMillis, int serial){
		ArrayList<Packet> packets= new ArrayList<Packet>();
		long startTime= System.currentTimeMillis();
		while(System.currentTimeMillis()-startTime < durationMillis){
			int retry= 0;
			long start,end;
			Packet packet= getPacket(ack, echoStart, echoEnd, 100);
			start= packet.startTime; end= packet.endTime;
			//If transmission error, request again...
			while (errorARQ(packet)){
				packet= getPacket(nack, echoStart, echoEnd, 100);
				end= packet.endTime;
				retry++;
			}
			packet.retries= retry;
			packet.startTime= start; packet.endTime= end;
			//Got correct package
			processARQ(packet, retry, serial);
			packets.add(packet);
		}
		return packets;
	}
	//! Cleanup resources
	public void close(){ modem.close(); }
	
	// $$$$$  PRIVATE  $$$$$
	private void processEchoPacket(Packet packet, Integer serial){
		if(!packet.incomplete){
			StringBuffer output= new StringBuffer();
			for(byte b: packet.data) output.append((char)b);
			Path path= Paths.get("./log/echoes"+serial.toString()+".log");
			packet.log(path);
			//System.out.println("Echo packet received");
		}
	}
	private void processImage(Packet packet, String imgName, Integer serial){
		if(!packet.incomplete){
			Path path= Paths.get("./img/"+imgName+serial.toString()+".jpg");
			Byte[] log= new Byte[packet.data.size()];
			log= packet.data.toArray(log);
			try { Files.write(path, toPrimitives(log), StandardOpenOption.CREATE);
			} catch (IOException e) { e.printStackTrace(); }
			System.out.println("Image saved to file#"+serial+". Timestamp: "+Instant.now());
		}
	}
	//! Parse GPS packet and get string of position codes with positions > 4secs apart
	private String positionFromGPS(Packet packet, String code, int secBetweenPos){
		StringBuffer sigcode= new StringBuffer(), curr= new StringBuffer();
		ArrayList<StringBuffer> positionBufs= new ArrayList<StringBuffer>();
		boolean gettingPos= false;
		//Get all the GPGGA lines in positionBufs
		for(byte b: packet.data){
			if(sigcode.length() < 6) sigcode.append((char)b);
			else{
				sigcode.deleteCharAt(0);
				sigcode.append((char)b);
				if(sigcode.toString().equals(gpsPosHeader)){
					gettingPos= true;
					positionBufs.add(new StringBuffer());
					curr= positionBufs.get(positionBufs.size()-1);
				}
				if(gettingPos) curr.append((char)b);
				if(sigcode.substring(5).equals("\r")){
					gettingPos= false;
				}
			}
		}
	System.out.println("Relevant lines: "+positionBufs.size());
		//Extract positions from positionBuf lines
		String[] positions= new String[9];
		int posIdx=0;
		boolean firstTime= true;
		//Timestamp of each GPS signal in seconds
		int[] time= new int[2];
		for(StringBuffer buf: positionBufs){
			if(posIdx >= 9) break;
			String[] parts= buf.toString().split(",");
			time[1]= (int)Float.parseFloat(parts[1]);
			if (firstTime || time[1]-time[0] > secBetweenPos){
				firstTime= false;
				time[0]= time[1];
				String latitude= parts[2], longitude= parts[4];
				/*float latFrac= Float.parseFloat(latitude), longFrac= Float.parseFloat(longitude);
			System.out.println(String.format("%.2f", latFrac)+" "+String.format("%.2f", longFrac));
				latFrac/= 100; longFrac/= 100;.
				int latDeg= (int)latFrac, longDeg= (int)longFrac;
				latFrac-= latDeg; longFrac-= longDeg;
				latFrac*= 60; longFrac*= 60;
				int latMin= (int)(latFrac), longMin= (int)(longFrac);
				latFrac*= 60; longFrac*= 60;
				int latSec= Math.round(latFrac%60), longSec= Math.round(longFrac%60);*/
				
				int latDeg= Integer.parseInt(latitude.substring(0,2)), longDeg= Integer.parseInt(longitude.substring(1,3));
				int latMin= Integer.parseInt(latitude.substring(2,4)), longMin= Integer.parseInt(longitude.substring(3,5));
				int latSec= (int)(Integer.parseInt(latitude.split("\\.")[1].substring(0,2))*0.6);
				int longSec= (int)(Integer.parseInt(longitude.split("\\.")[1].substring(0,2))*0.6);
				//Create a position-ful gps_request_code
				positions[posIdx++]= "T="+String.format("%02d", longDeg)+String.format("%02d", longMin)+
						String.format("%02d", longSec)+String.format("%02d", latDeg)+String.format("%02d", latMin)+
						String.format("%02d", latSec);//+"\r";
			}
		}
	System.out.println("T-codes: "+posIdx);
		//Buffer to concatenate all the position codes together: <code> T=... T=... ...\r
		StringBuffer concatPos= new StringBuffer();
		concatPos.append(code)/*.deleteCharAt(concatPos.length()-1);*/.delete(5, concatPos.length());
		for(int i=0; i<posIdx; i++) concatPos.append(positions[i]);
		concatPos.append("\r");
		return concatPos.toString();
	}
	private boolean errorARQ(Packet packet){
		StringBuffer fcsBuf= new StringBuffer();
		byte[] hex= new byte[16];
		if(packet.data.size() != 58){
			System.out.println("[errorARQ]: Error! Packet size= "+packet.data.size());
			return true;
		} else {
			//<31bytes>HEX FCS<6bytes> 
			for(int i=31; i< 31+16; i++) hex[i-31]= packet.data.get(i);
			for(int i=49; i<52; i++) fcsBuf.append( (char) ((byte)packet.data.get(i)) );	
			//Parse fcs
			int fcs= Integer.parseInt(fcsBuf.toString());
			//Calculate HEX xor
			int fcsCheck= fcs(hex);
			return fcs != fcsCheck;
		}
	}
	public int fcs(byte[] hex){
		int fcsCheck= (int)hex[0];
		for(int i=1; i<16; i++) fcsCheck^= (int)hex[i];
		return fcsCheck;
	}
	private void processARQ(Packet packet, int retries, Integer serial){
		if(!packet.incomplete){
			StringBuffer output= new StringBuffer();
			for(byte b: packet.data) output.append((char)b);
			Path path= Paths.get("./log/arques"+serial.toString()+".log");
			packet.log(path);
		}
	}
	
	//! Write and read byte streams from the modem. Calculate response time and package RX time.
	//! 	Special use if (start||end) isEmpty: show all data received to console
	private Packet getPacket(String code, ArrayList<Byte> start, ArrayList<Byte> end, int capacity){
		ArrayList<Byte> sigStart= new ArrayList<Byte>(), sigEnd= new ArrayList<Byte>();
		ByteFlag mdk= new ByteFlag();
		boolean packetStart= false, packetIn= false, packetEnd= false;
		Packet packet= new Packet();
		mdk.terminate= false;
		long sendTime= System.currentTimeMillis();
		modem.write(code.getBytes());
		//Loop until the *end delimiter* has been received
		while(!mdk.terminate){
			mdk= readByte();
			if (packet.responseTimeMillis <= 0) packet.responseTimeMillis= System.currentTimeMillis()-sendTime;
			if (!mdk.terminate && !(start.isEmpty() || end.isEmpty())){
				if(start.size() == 0) packet.data.add((byte)mdk.k);
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
						//Only record the first time this packet reaches here (ARQ)
						if (packet.startTime <= 0) packet.startTime= System.currentTimeMillis();
						packet.data.ensureCapacity(capacity);
						for(int i=0; i<start.size()-1; i++) packet.data.add(start.get(i));
					}
					// While packet is being transmitted...
					if (packetIn) packet.data.add((byte)mdk.k);
					// On packet end
					if (!oldpacketEnd && packetEnd){
						packetIn= false;
						packet.endTime= System.currentTimeMillis();
						mdk.terminate= true;
					}
				}
			} else if (!mdk.terminate) System.out.print((char)mdk.k);		// start/end isEmpty
			else packet.incomplete= true;
		}
		return packet;
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
	private String gpsPosHeader= "$GPGGA";
	private Modem modem;
}
