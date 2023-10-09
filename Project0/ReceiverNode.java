import java.util.Arrays;
import cnss.simulator.*;
import cnss.lib.*;

public class ReceiverNode extends AbstractApplicationAlgorithm {

  public ReceiverNode() {
      super(true, "receiver-node");
  }

  public int initialise(int now, int node_id, Node self, String[] args) {
    super.initialise(now, node_id, self, args);
		return 0;
	}

  public void on_receive( int now, DataPacket p ) {
    log( now, "got: " + p); 
    self.send (self.createDataPacket(p.getSource(),p.getPayload()));
  }
}