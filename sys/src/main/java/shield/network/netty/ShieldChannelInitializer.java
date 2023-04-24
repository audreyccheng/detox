package shield.network.netty;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import shield.network.NetworkQueue;

public class ShieldChannelInitializer
    extends ChannelInitializer<SocketChannel> {

  NetworkQueue NWQ;
  NettyTCPSender sender;

  public ShieldChannelInitializer(NetworkQueue nwq, NettyTCPSender sender) {
    NWQ = nwq;
    this.sender = sender;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    ServerHandler handler = new ServerHandler(NWQ, sender);
    MessageDecoder decoder = new MessageDecoder();
    pipeline.addLast("decoder", decoder);
    pipeline.addLast("handler", handler);
  }

}
