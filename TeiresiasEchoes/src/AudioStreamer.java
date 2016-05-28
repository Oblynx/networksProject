import java.util.concurrent.*;
import java.util.Arrays;
import java.util.Stack;
import java.io.ByteArrayOutputStream;
import java.nio.*;
import java.lang.IllegalStateException;
import java.lang.IllegalArgumentException;
import javax.sound.sampled.*;

// Streams audio. Uses external executor 
public class AudioStreamer {
	public AudioStreamer(ExecutorService pool, IthakiSocket s, String tonef, String musicf){
		this.pool= pool; this.s= s;
		toneDiffLogger= new Logger(tonef+"_diff");
		toneSampLogger= new Logger(tonef+"_samp");
		musicDiffLogger= new Logger(musicf+"_diff");
		musicSampLogger= new Logger(musicf+"_samp");
	}
	
	public void close(){
		toneDiffLogger.close();
		toneSampLogger.close();
		musicDiffLogger.close();
		musicSampLogger.close();
	}
	
	public void waitToFinish(){
		while(!tasks.empty()) try{
			tasks.pop().get();
		} catch(Exception e) { System.err.println(e.getMessage()); }
	}
	
	// Asynchronous: Source elasticMemory -- tasks must be empty 
	public void stream(String code, int durationSec, int Q, boolean adaptiveMode,
										 boolean logEnable, String logSource) throws IllegalStateException
	{
		assert(durationSec < 32);
		if (!tasks.isEmpty()) throw new IllegalStateException("Streaming already in progress!");
		
		Callable<Void> sourceMem= () -> {
      int msgSize= (adaptiveMode)? packetLength+packetOverhead: packetLength;
      s.send((code+String.format("%03d", durationSec*32)).getBytes());
      for(int i=0; i<durationSec*packetsPerSec; i++){
      	try{
          byte[] msg= s.receive(msgSize);
          if (logEnable) log(logSource, "diff", msg);
          short[] decodedMsg= (adaptiveMode)? decodeAdaptive(msg): decode(msg,(short)0,beta_def);
          elasticMemory.add(decodedMsg);
      	} catch(java.net.SocketTimeoutException e){
      		streamTimeout= true;
      		System.out.println("[streamer]: Streaming timed out");
      		break;
      	}
      }
      System.out.println(">>> Streaming complete! <<<");
      return null;
		};
		Callable<Void> playback= () -> {
      AudioFormat audioFormat= new AudioFormat(8000,Q,1,true,false);
      SourceDataLine line;
      try{
      	// Open audio line
        line= AudioSystem.getSourceDataLine(audioFormat);
        line.open(audioFormat, 5*packetsPerSec*packetLength*Q/8);
        line.start();
        // Wait to buffer memory before starting playback
        int delayPlayback= durationSec/3;
        Thread.sleep(delayPlayback*1000);
        for (int i=0; i<durationSec*packetsPerSec; i++){
          while(elasticMemory.isEmpty()){
          	line.stop();
          	if (elasticMemory.isEmpty() && streamTimeout) break;
            if (streamingTimeout-- < 0) throw new TimeoutException("Streaming stalled!"); 
            System.err.println("[playback]: Buffer empty");
            Thread.sleep(1000);
            line.start();
          }
          if (elasticMemory.isEmpty() && streamTimeout) break;
          short[] packet= elasticMemory.poll();
          byte[] bytePacket= new byte[packet.length*2];
          ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(packet);
          line.write(bytePacket, 0, bytePacket.length);
        }
        System.out.println(">>> Playback complete! <<<");
        line.close();
      } catch(LineUnavailableException e) { System.err.println(e.getMessage()); }
			return null;
		};
		
		tasks.push(pool.submit(sourceMem));
		tasks.push(pool.submit(playback));
	}
	
	private static short[] decode(byte[] msg, short mu, short beta){
		short[] decodedMsg= new short[2*msg.length];
		// Initialize
		byte d1= (byte)(msg[0]>>4 & 0x0F);
		byte d2= (byte)(msg[0]    & 0x0F);
		decodedMsg[0]= (short) ((d1-8)*beta);
		decodedMsg[1]= (short) ((d2-8)*beta+decodedMsg[0]);
		// Iterate
		for(int i=1; i<msg.length; i++){
			d1= (byte)(msg[i]>>4 & 0x0F);
			d2= (byte)(msg[i]    & 0x0F);
			decodedMsg[2*i]=   (short) ((d1-8)*beta+decodedMsg[2*i-1]);
			decodedMsg[2*i+1]= (short) ((d2-8)*beta+decodedMsg[2*i]  );
		}
		return decodedMsg;
	}
	private static short[] decodeAdaptive(byte[] msg){
		short mu=   (short) (msg[1]<<8 | (msg[0] & 0xFF));
		short beta= (short) (msg[3]<<8 | (msg[2] & 0xFF));
		return decode(ByteBuffer.wrap(msg, 4, msg.length-4).slice().array(), mu, beta);
	}
	// Disambiguates between loggers and then logs msg. Example: log("music","diff",msg)
	private void log(String source, String mode, byte[] msg){
		int pickLogger= 0;
		if (source.equals("tone")) pickLogger|= 1;
		else if (source.equals("music")) pickLogger|= 2;
		if (mode.equals("diff")) pickLogger|= 4;
		else if (source.equals("samp")) pickLogger|= 8;
		Logger logger;
		switch (pickLogger){
		case 1|4:
			logger= toneDiffLogger; break;
		case 1|8:
			logger= toneSampLogger; break;
		case 2|4:
			logger= musicDiffLogger; break;
		case 2|8:
			logger= musicSampLogger; break;
		default:
			throw new IllegalArgumentException();
		}
		logger.log(msg);
	}

	private ConcurrentLinkedQueue<short[]> elasticMemory= new ConcurrentLinkedQueue<short[]>();
	private volatile boolean streamTimeout= false;
	private ExecutorService pool;
	private Stack<Future<Void>> tasks= new Stack<Future<Void>>();
	private IthakiSocket s;
	private Logger toneDiffLogger, toneSampLogger, musicDiffLogger, musicSampLogger;
	
	private int packetLength= 128, packetOverhead= 4, packetsPerSec= 32, streamingTimeout= 5;

	private static byte beta_def= 1;
}
