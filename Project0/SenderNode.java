import java.util.Arrays;
import cnss.simulator.*;
import cnss.lib.*;
import java.nio.ByteBuffer;

public class SenderNode extends AbstractApplicationAlgorithm {

  public SenderNode() {
      super(true, "sender-node");
  }

  public int initialise(int now, int node_id, Node self, String[] args) {
    super.initialise(now, node_id, self, args);
		return 5000;
	}

  public void on_clock_tick(int now) {
      ByteBuffer b = ByteBuffer.allocate(4);
      b.putInt(now);
      byte [] bytes = b.array();
      self.send(self.createDataPacket(1, bytes));
  }

  public void on_receive ( int now, DataPacket p ) {
    byte[] bytes = p.getPayload();
    int time = ByteBuffer.wrap(bytes).getInt();
    int rtt = now - time;
    log (now, "\nRound Trip Time = " + rtt);
  }
}