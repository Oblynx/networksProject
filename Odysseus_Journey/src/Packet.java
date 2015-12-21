import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//! The main communication data object
public class Packet {
	Packet() { data= new ArrayList<Byte>(); }
	Packet(ArrayList<Byte> d) { data= new ArrayList<Byte>(d); }
	
	//! Time between getting the first and last byte of the package 
	public long rxTime() { return startTime-endTime; }
	//! Log this packet's metadata to file
	public void log(Path path){
		List<String> log= Arrays.asList(String.format("%d;%d;%d;%d;%b", startTime,endTime,responseTimeMillis,retries,incomplete));
		StandardOpenOption option= (new File(path.toString()).exists())? StandardOpenOption.APPEND: StandardOpenOption.CREATE;
		try { Files.write(path, log,StandardOpenOption.WRITE, option);
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public ArrayList<Byte> data;
	public long startTime=0, endTime=0;
	//! Time between: before writing code to modem and after receiving first byte back (whether that byte belongs to package or not)
	public long responseTimeMillis=0;
	//! How many times the FCS check failed
	public int retries=0;
	//! Whether the package has been fully received
	public boolean incomplete= false;
}
