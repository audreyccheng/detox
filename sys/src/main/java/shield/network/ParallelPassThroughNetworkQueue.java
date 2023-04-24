package shield.network;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

import shield.network.messages.Msg;

public class ParallelPassThroughNetworkQueue implements NetworkQueue {

  private final IncomingMessageHandler bn;
  private final ExecutorService pool;

  public ParallelPassThroughNetworkQueue(IncomingMessageHandler bn,
      ExecutorService p) {
    this.bn = bn;
    pool = p;
  }

  public void addWork(byte[] m) {
    pool.execute(() -> bn.handle(m));
  }

  public void addWork(Msg.Message m) {
    pool.execute(() -> bn.handle(m));
  }


}
