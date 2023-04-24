package shield.network;

import com.google.protobuf.Message;

import io.netty.buffer.ByteBuf;
import shield.BaseNode;
import shield.config.NodeConfiguration;
import shield.network.netty.NettyTCPReceiver;
import shield.network.netty.NettyTCPSender;

import java.net.InetSocketAddress;

public class NetworkManager {

  /**
   * TODO(natacha): currently does a copy of every protobuf. Is it possible to implement it such
   * that 0 copy?
   */

  private final BaseNode block;
  private final InetSocketAddress currentAddress;
  private final NettyTCPReceiver receiver;
  private final NettyTCPSender sender;
  private final NodeConfiguration config;

  public static NetworkManager createNetworkManager(BaseNode block,
      String listeningHost, int listeningPort) throws InterruptedException {
    System.out.println("Creating NM " + listeningHost + " " + listeningPort);
    NetworkManager nm = new NetworkManager(block, listeningHost, listeningPort);
    return nm;
  }

  private NetworkManager(BaseNode block, String listeningHost,
      int listeningPort) throws InterruptedException {

    assert (block.getConfig().isInitialised());

    this.config = block.getConfig();
    this.block = block;
    this.currentAddress = new InetSocketAddress(listeningHost, listeningPort);
    sender = new NettyTCPSender(config.N_SENDER_NET_THREADS);
    receiver =
        new NettyTCPReceiver(currentAddress,
            new ParallelPassThroughNetworkQueue(
                new IncomingMessageHandler(block), block.getWorkpool()),
            config.N_RECEIVER_NET_THREADS);
    receiver.setTCPNoDelay(true);
    sender.setTCPNoDelay(true);

  }

  public void sendMsg(Message msg, InetSocketAddress addr) {
    byte[] bytes = msg.toByteArray();
    sender.send(bytes, addr);
  }

  public void sendMsg(Message msg, InetSocketAddress addr, boolean flush) {
    byte[] bytes = msg.toByteArray();
    sender.send(bytes, addr, flush);
  }

  public String getNodeHost() {
    return currentAddress.getHostName();
  }

  public int getNodePort() {
    return currentAddress.getPort();
  }

}
