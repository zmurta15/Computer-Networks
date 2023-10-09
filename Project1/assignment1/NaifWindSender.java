package assignment1;

import cnss.lib.AbstractApplicationAlgorithm;
import cnss.simulator.ApplicationAlgorithm;
import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import cnss.simulator.Packet;
import cnss.simulator.DataPacket;

public class NaifWindSender extends AbstractApplicationAlgorithm {

	public static int BLOCKSIZE = 10000; // 10000*8 = 80000 bits
	public static int TOTAL_PACKETSIZE = BLOCKSIZE+Packet.HEADERSIZE; // 10000*8 = 80160 bits

	public NaifWindSender() {
		super(true, "naif-window-sender");
	}

	int totSent;
	int totalBlocks;
	int startTime;
	int transferTime;
	int totBytesTransferred;
    int e2eTransferRate;
    int sizeWindow;
    int windowCount;

	boolean mayProceed = false;

	public int initialise(int now, int node_id, Node self, String[] args) {
		super.initialise(now, node_id, self, args);
		if ( args.length != 2 ) {
			System.err.println("files-sender: missing argument time "+now+"\n\n");
			System.exit(-1);
		}
        totalBlocks = Integer.parseInt(args[0]);
        sizeWindow = Integer.parseInt(args[1]);
		log(0, "starting");
		startTime = now;
		totSent = 0;
        mayProceed = true;
        windowCount = 0;
		return 1;	
	}

	public void on_clock_tick(int now) {
		if ( mayProceed && totSent < totalBlocks) {
			totSent++;
			byte[] pl = new byte[BLOCKSIZE];
			pl[0]= (byte) ( totSent & 0xff ); 
			self.send( self.createDataPacket( 1, pl ));
            log(now, "sent packet of size "+TOTAL_PACKETSIZE+" n. "+totSent);
            windowCount++;
            if(windowCount == sizeWindow) {
                mayProceed = false;
            }
		}
	}

	public void on_timeout(int now) {
		log(now, "timeout");
	}


	public void on_receive(int now, DataPacket p) {
        log(now, "ack packet: "+p+" pl: "+new String(p.getPayload()));
        windowCount--;
		mayProceed = true;
		if (totSent == totalBlocks && windowCount == 0) {
			transferTime = now - startTime;
			totBytesTransferred = TOTAL_PACKETSIZE*totalBlocks;
			float transferTimeInSeconds = (float)transferTime / 1000;
			e2eTransferRate = (int)(totBytesTransferred*8 / transferTimeInSeconds);
			log(now, totBytesTransferred+" bytes transferred in "+transferTime+" ms at "+e2eTransferRate+" bps e2e rate");
		}
	}


	public void showState(int now) {
		System.out.println(name + " sent " + totSent + " packets with blocks");
		System.out.println(name+" "+totBytesTransferred+" bytes transferred in "
				+transferTime+" ms at "+e2eTransferRate+" bps e2e rate");
	}

}
