
import java.io.File;
import java.io.RandomAccessFile;

import cnss.simulator.Node;
import cnss.simulator.Simulator;
import ft20.FT20AbstractApplication;
import ft20.FT20_AckPacket;
import ft20.FT20_DataPacket;
import ft20.FT20_FinPacket;
import ft20.FT20_PacketHandler;
import ft20.FT20_UploadPacket;

public class FT20ClientGBN extends FT20AbstractApplication implements FT20_PacketHandler {

	static int SERVER = 1;

	enum State {
		BEGINNING, UPLOADING, FINISHING
	};

	static int DEFAULT_TIMEOUT = 1000;

	private File file;
	private RandomAccessFile raf;
	private int BlockSize;
    private int nextPacketSeqN, lastPacketSeqN;
    private int windowSize;
    private int counter;
    //private int first;
	//private int last;
	private boolean skip;
	private int timestamp;

	private State state;

	public FT20ClientGBN() {
		super(true, "FT20-ClientGBN");
	}

	public int initialise(int now, int node_id, Node nodeObj, String[] args) {
		super.initialise(now, node_id, nodeObj, args, this);

		raf = null;
		file = new File(args[0]);
        BlockSize = Integer.parseInt(args[1]);
        windowSize = Integer.parseInt(args[2]);

        state = State.BEGINNING;
        nextPacketSeqN = 0;
        lastPacketSeqN = (int) Math.ceil(file.length() / (double)BlockSize);
        counter = 0;
        first = 0;
		skip = true;
		timestamp = 0;

		sendNextPacket(now);
		return 0;
	}

	private void sendNextPacket(int now) {
        //if(nextPacketSeqN==0) {
            //state = State.BEGINNING;
        //}
        //while(counter<windowSize) {
            if(state == State.BEGINNING) {
                super.sendPacket(now, SERVER, new FT20_UploadPacket(file.getName(), now));
                state = State.UPLOADING;
            }
            else if(state == State.UPLOADING) {
                super.sendPacket(now, SERVER, readDataPacket(file, nextPacketSeqN, now));
            }
            else if(state == State.FINISHING) {
                super.sendPacket(now, SERVER, new FT20_FinPacket(nextPacketSeqN, now));
            }
            nextPacketSeqN++;
            counter++;
		//}
		if (skip == true) {
			//self.set_timeout(DEFAULT_TIMEOUT);
			skip = false;
			timestamp = now;
		}
		
	}

	public void on_timeout(int now) {
        super.on_timeout(now);
		nextPacketSeqN = first;
		skip = true;
		sendNextPacket(now);
	}

	public void on_clock_tick(int now) {
		super.on_clock_tick(now);
		if (counter < window.size) {
			sendNextPacket(now);
		}
		if (now-timestamp > DEFAULT_TIMEOUT) {
			on_timeout(now);
		}
	} 

	@Override
	public void on_receive_ack(int now, int client, FT20_AckPacket ack) {
        if (first == 0) {
			first += ack.cSeqN + 1;
		} else {
			first += ack.cSeqN;
		}
        if(state == State.UPLOADING) {
			if (nextPacketSeqN > lastPacketSeqN)
				state = State.FINISHING;
        }
        else if(state == State.FINISHING) {
            super.log(now, "All Done. Transfer complete...");
			super.printReport( now );
			return;
        }

        counter-= ack.cSeqN;
		sendNextPacket(now);
		skip = true;
	}
	
	private FT20_DataPacket readDataPacket(File file, int seqN, int timestamp) {
		try {
			if (raf == null)
				raf = new RandomAccessFile(file, "r");

			raf.seek(BlockSize * (seqN - 1));
			byte[] data = new byte[BlockSize];
			int nbytes = raf.read(data);
			return new FT20_DataPacket(seqN, timestamp, data, nbytes);
		} catch (Exception x) {
			throw new Error("Fatal Error: " + x.getMessage());
		}
	}
}