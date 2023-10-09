import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import java.net.UnknownHostException;


import java.io.FileOutputStream;
import java.io.IOException;


public class HelperClass extends Thread{

	Stats statistics;
	String host;
	String path;
	int[] portas;
	int limitPerThread;
	int threadNumber;
	int fileSize;
	int range;
	FileOutputStream[] streams;

	public HelperClass(Stats statistics, String host, String path, int[] portas, int limitPerThread, int threadNumber, int fileSize, 
								int range, FileOutputStream[] streams) throws IOException,UnknownHostException{
		this.statistics = statistics;
		this.host = host;
		this.path = path;
		this.portas = portas;
		this.limitPerThread = limitPerThread;
		this.threadNumber = threadNumber;
		this.fileSize = fileSize;
		this.range = range;
		this.streams = streams;
		
	}

	public void run(){
		try{
			int limit = limitPerThread * (threadNumber+1);

			if(threadNumber == portas.length-1){
				limit = (limit + (fileSize%portas.length));
			}
			int initialByte = threadNumber*limitPerThread;
			while(initialByte < limit){
				Socket socket = new Socket(host, portas[threadNumber]);
				
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();

				int finalByte = 0;
				if(initialByte+range-1 > limit) {
					finalByte = limit-1;
					statistics.newRequest(finalByte - initialByte+1);
				}
				else {
					finalByte = initialByte+range-1;
					statistics.newRequest(finalByte - initialByte+1);
				}
				String r= String.format(
					"GET %s HTTP/1.0\r\n"+
					"Host: %s\r\n"+
					"User-Agent: X-RC2020 GetFile\r\n"+
					"Range: bytes=%d-%d\r\n\r\n", path, host , initialByte, finalByte);
				
				out.write(r.getBytes());
									
				String answer = Http.readLine(in);
				String[] reply = Http.parseHttpReply(answer);
				
				while ( !answer.equals("") ) {
					answer = Http.readLine(in);
				}
				
				if ( reply[1].equals("206")) {					
					int n;
					byte[] buffer = null;
					
					buffer = new byte[range];
					while( (n = in.read(buffer)) >= 0 ) {
						streams[threadNumber].write(buffer, 0, n);
					}
				}else {
					System.out.println("Ooops, received status:" + reply[1]);
				}
				
				initialByte += range;					
				socket.close();
			}
		}
			catch(IOException i){}
	}

}