import java.util.ArrayList;

public class Packet {
	Packet() { data= new ArrayList<Byte>(); }
	Packet(ArrayList<Byte> d) { data= new ArrayList<Byte>(d); }
	public ArrayList<Byte> data;
	public long startTime, endTime;
	public int size() { return data.size(); }
	public long rxTime() { return startTime-endTime; }
}
