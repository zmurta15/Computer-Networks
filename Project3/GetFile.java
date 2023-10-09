import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/** A really simple HTTP Client
 * 
 * @author Jose Murta 55226 && Diogo Rodrigues 56153
 *
 */

public class GetFile {
	private static final int BUF_SIZE = 512*16;
	
	private static Stats statistics;
	
	public static void main(String[] args) throws Exception {
		if ( args.length != 4 ) {
			System.out.println("Usage: java GetFile url_to_access0 url_to_access1 url_to_access2 url_to_access3 ");
			System.exit(0);
		}
		int[] portas = new int[args.length];
		statistics = new Stats();
		URL url = new URL(args[0]);
		for(int i = 0 ; i < portas.length; i++){
			URL u = new URL(args[i]);
			portas[i] = u.getPort() == -1 ? 80 : u.getPort();
		}
		// Assuming URL of the form http://server-name:port/path ....

		String path = url.getPath() == "" ? "/" : url.getPath();
		downloadFile(url.getHost(), portas, path);

	}
		
        // Implement here to download the file requested in the URL
        // and write the file in the client side.
        // In the end print te requires detatistics
    
	private static void downloadFile(String host, int[] portas, String path) 
			throws UnknownHostException, IOException, InterruptedException {
		
		Socket socket = new Socket(host ,portas[0]);
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		String request = String.format(
			"GET %s HTTP/1.0\r\n"+
			"Host: %s\r\n"+
			"User-Agent: X-RC2020 GetFile\r\n"+
			"Range: bytes=0-0\r\n\r\n", path, host);

		out.write(request.getBytes());

		System.out.println("\nSent Request:\n-------------\n"+request);		
		System.out.println("Got Reply:");
		System.out.println("\nReply Header:\n--------------");

		String answerLine = Http.readLine(in);
		System.out.println(answerLine);
		long[] pHeader = null;
		int fileSize = 0;

		answerLine = Http.readLine(in);
		while ( !answerLine.equals("") ) {
			System.out.println(answerLine);
			String[] head = Http.parseHttpHeader(answerLine);
			answerLine = Http.readLine(in);
			String cr = head[0];
			String value = head[1];
			if(cr.equals("Content-Range")){
				pHeader = Http.parseRangeValuesSentByServer(value);
				fileSize = (int)pHeader[2];
			}
		}

		socket.close();
		
		int limitPerThread = (int)fileSize/portas.length;
		
		File[] auxFiles = createFiles();
		FileOutputStream[] streams = createfileStreams(auxFiles);
		Thread[] t = new Thread[portas.length];
		
		for(int i = 0; i < portas.length; i++){
			Thread th = new Thread( new HelperClass(statistics, host, path, portas, limitPerThread, i, fileSize, BUF_SIZE, streams));
			th.start();
			t[i] = th;
		}

		waitForThreads(t);
		endDownload(auxFiles, streams, statistics);
	}


	private static File[] createFiles() {
		File[] files = new File[4];
		for(int i = 0; i<4; i++) {
			File f = new File("f"+i+".out");
			files[i] = f;
		}
		return files;
	}

	private static FileOutputStream[] createfileStreams(File[] files) throws FileNotFoundException{
		FileOutputStream[] streams = new FileOutputStream[4];
		for(int i = 0; i<4; i++) {
			FileOutputStream f = new FileOutputStream(files[i].getName());
			streams[i] = f;
		}
		return streams;
	}

	private static void waitForThreads(Thread[] t) throws InterruptedException{
		for(int i =0; i<t.length; i++) {
			t[i].join();
		}
	}

	private static void endDownload(File[] auxFiles, FileOutputStream[] streams, Stats statistics) throws IOException{
		FileOutputStream actual = new FileOutputStream("finished.out");
		for(int i = 0; i<auxFiles.length; i++){
			InputStream input = new FileInputStream(auxFiles[i].getName());
			byte[] buf = new byte[(int)auxFiles[i].length()];
			int n= 0;
			while((n= input.read(buf)) >=0){
				actual.write(buf, 0, n);
			}
			input.close();
		}
		actual.close();
		for(int i = 0; i<streams.length; i++){
			streams[i].close();
		}
		for(int i = 0; i<auxFiles.length; i++){
			auxFiles[i].delete();
		}
		statistics.printReport();
	}
}

