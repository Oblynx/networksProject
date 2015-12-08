import ithakimodem.Modem;

public class VirtualModem {
	public void RXsetup(int speed, int timeout){
		modem.setSpeed(speed);
		modem.setTimeout(timeout);
		modem.write("ATd2310ithaki\r".getBytes());
	}
	public void echoPacketRX(String code){
		StringBuffer sigStart= new StringBuffer(), sigEnd= new StringBuffer(), packet= new StringBuffer();
		ByteFlag mdk= new ByteFlag();
		Boolean packetStart= false, packetIn= false, packetEnd= false;
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
				Boolean oldpacketStart= packetStart, oldpacketEnd= packetEnd;
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
				}
			}
		}
	}

	public void imageRX(String code){
		
	}
	
	public void close(){ modem.close(); }
	
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
	private void processEchoPacket(StringBuffer packet){
		System.out.println(packet);
	}
	private static class ByteFlag{
		public int k= 0;
		public Boolean terminate= false;
	}
	private Modem modem= new Modem();
}
