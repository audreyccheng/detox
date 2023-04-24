package shield.network.netty;


import com.google.protobuf.MessageLite;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import shield.network.NetworkQueue;

public class ShieldProtobufChannelInitializer
    extends ChannelInitializer<SocketChannel> {

  private final NetworkQueue nwq;
  private final MessageLite lite;
  private final NettyTCPSender sender;

  public ShieldProtobufChannelInitializer(NetworkQueue nwq, MessageLite lite, NettyTCPSender sender) {
    this.nwq = nwq;
    this.lite = lite;
    this.sender = sender;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    ServerHandler handler = new ServerHandler(nwq, sender);
    MessageDecoder decoder = new MessageDecoder();
    pipeline.addLast(new ProtobufVarint32FrameDecoder());
    pipeline.addLast(new ProtobufEncoder());
    pipeline.addLast(new ProtobufDecoder(lite));
    pipeline.addLast("decoder", decoder);
    pipeline.addLast("handler", handler);
  }

}
