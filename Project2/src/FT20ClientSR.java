
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
import java.util.*;

public class FT20ClientSR extends FT20AbstractApplication implements FT20_PacketHandler {

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
    private int first;

	

	private SortedMap<Integer, Integer> window = new TreeMap<>();

	private State state;

	public FT20ClientSR() {
		super(true, "FT20-ClientSR");
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


		sendNextPacket(now);
		return 1;
	}

	private void sendNextPacket(int now) {
			if(nextPacketSeqN == 0) {
				state = State.BEGINNING;
			}
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
			window.put(nextPacketSeqN,now);
			nextPacketSeqN++;
			counter++;
		
	}

	public void sendSinglePacket(int now, int packetNumber) {
		if(nextPacketSeqN == 0) {
			state = State.BEGINNING;
		}
        if(state == State.BEGINNING) {
			super.sendPacket(now, SERVER, new FT20_UploadPacket(file.getName(), now));
            state = State.UPLOADING;
        }
        else if(state == State.UPLOADING) {
            super.sendPacket(now, SERVER, readDataPacket(file, packetNumber, now));
        }
        else if(state == State.FINISHING) {
            super.sendPacket(now, SERVER, new FT20_FinPacket(packetNumber, now));
		}
		window.replace(packetNumber,now);
		counter++;
	}


	public void on_clock_tick(int now) {
		if (counter < windowSize && nextPacketSeqN<= lastPacketSeqN)  {
			sendNextPacket(now);
		}
		for (int i = first; i < nextPacketSeqN ; i++) {
			if ((now - window.get(i) >= DEFAULT_TIMEOUT) && window.get(i)!=-2) {
				super.on_timeout(now);
				counter--;
				sendSinglePacket(now, i);
				window.replace(i, now);
			}
		}
		if(first>lastPacketSeqN) {
			state= State.FINISHING;
		}
	} 

	@Override
	public void on_receive_ack(int now, int client, FT20_AckPacket ack) {
		if(ack.sSeqN == first) {
			first = ack.sSeqN + 1;
			window.replace(ack.sSeqN, -2);
			counter--;
			for (int i = first; i < nextPacketSeqN; i++) {
				if (window.get(i) == -2) {
					first++;
					counter--;
				}
				if (window.get(i) != -2) {
					break;
				}
			}
		}
		else if(ack.sSeqN > first){
			window.replace(ack.sSeqN, -2);
			for(int j = first; j<=ack.cSeqN; j++) {
				window.replace(j, -2);
				counter--;
			}
			first = ack.cSeqN+1;
		}
		else if(ack.sSeqN == -1) {
			for(int j = first; j<ack.cSeqN; j++) {
				window.replace(j, -2);
				counter--;
			}
			first = ack.cSeqN+1;
		}
    	if(state == State.UPLOADING) {
			if (first-1 >= lastPacketSeqN) {
				state = State.FINISHING;
				sendNextPacket(now);
			}
		}
        else if(state == State.FINISHING) {
			super.log(now, "All Done. Transfer complete...");
			super.printReport( now );
			return;
    	}
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
