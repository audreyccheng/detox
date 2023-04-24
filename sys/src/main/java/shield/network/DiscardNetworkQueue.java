package shield.network;

import java.net.InetSocketAddress;

public class DiscardNetworkQueue implements NetworkQueue {

  public DiscardNetworkQueue() {

  }

  public void addWork(byte[] m) {
    // do nothing
  }

}
