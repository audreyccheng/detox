
package shield.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import shield.network.NetworkQueue;
import shield.util.Pair;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerHandler extends ChannelInboundHandlerAdapter {

  private final NetworkQueue NWQ;
  private final NettyTCPSender sender;

  public ServerHandler(NetworkQueue _NWQ, NettyTCPSender sender) {
    NWQ = _NWQ;
    this.sender = sender;
  }

  private static final Logger logger =
      Logger.getLogger(ServerHandler.class.getName());

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    NWQ.addWork((byte[]) msg);
  }

  @Override
  synchronized public void exceptionCaught(ChannelHandlerContext ctx,
      Throwable e) {
    // Close the connection when an exception is raised.
    logger.log(Level.WARNING, "Unexpected exception from downstream.",
        e.getCause());

    System.out.println("Exception caught");
    Channel c = ctx.channel();
    System.out.println(c.localAddress());
    System.out.println(c.remoteAddress());

    System.out.println(c.localAddress());
    System.out.println(c.remoteAddress());
    c.close();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    System.out.println("Channel connected from " + ctx.channel().remoteAddress()
        + " to " + ctx.channel().localAddress());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    System.out
        .println("Channel disconnected from " + ctx.channel().remoteAddress());
    if (sender != null) {
      sender.onChannelInactive(ctx.channel());
    }
  }

}
