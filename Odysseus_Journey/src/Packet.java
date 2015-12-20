import java.util.ArrayList;

//! The main communication data object
public class Packet {
	Packet() { data= new ArrayList<Byte>(); }
	Packet(ArrayList<Byte> d) { data= new ArrayList<Byte>(d); }
	
	public int size() { return data.size(); }
	//! Time between getting the first and last byte of the package 
	public long rxTime() { return startTime-endTime; }
	
	public ArrayList<Byte> data;
	public long startTime=0, endTime=0;
	//! Time between: before writing code to modem and after receiving first byte back (whether that byte belongs to package or not)
	public long responseTimeMillis=0;
	//! How many times the FCS check failed
	public int retries=0;
	//! Whether the package has been fully received
	public boolean incomplete= false;
}
