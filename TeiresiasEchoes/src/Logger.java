import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

// Logs to particular file
public class Logger {
	public Logger(String fname){
		f= new File(fname);
		try{ fout= new FileOutputStream(f); }
		catch(FileNotFoundException e){ System.err.println("Error opening file: "+f.getAbsolutePath()); }
	}
	
	public void log(byte[] msg){
		try{ fout.write(msg); } 
		catch(IOException e){ System.err.println("Error writing to file: "+f.getAbsolutePath()); }
	}
	
	private File f;
	private FileOutputStream fout;
}
