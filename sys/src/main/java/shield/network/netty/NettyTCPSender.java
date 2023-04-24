package shield.network.netty;

import java.util.concurrent.Executor;
import shield.network.DiscardNetworkQueue;
import shield.network.NetworkSender;
import shield.network.messages.Msg;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;


public class NettyTCPSender implements NetworkSender {

  private ConcurrentHashMap<InetSocketAddress, Channel> openChannels;
  Bootstrap clientBootstrap;

  public NettyTCPSender() {
    this(1);
  }

  public void setTCPNoDelay(boolean nodelay) {
    clientBootstrap.option(ChannelOption.TCP_NODELAY, nodelay);
  }

  public void setKeepAlive(boolean alive) {
    clientBootstrap.option(ChannelOption.SO_KEEPALIVE, alive);
  }

  public NettyTCPSender(int threadCount) {
    this(threadCount, Executors.newCachedThreadPool());
  }

  public NettyTCPSender(int threadCount, Executor threadPool) {
    System.out.println("sender set up threads " + threadCount);
    openChannels = new ConcurrentHashMap<InetSocketAddress, Channel>();
    clientBootstrap = new Bootstrap();
    clientBootstrap.channel(NioSocketChannel.class);
    clientBootstrap.group(
        new NioEventLoopGroup(threadCount, threadPool));
    setTCPNoDelay(true);
    setKeepAlive(true);
    DiscardNetworkQueue dnq = new DiscardNetworkQueue();
    clientBootstrap.handler(new ShieldChannelInitializer(dnq, this));

  }

  public void send(byte[] m, InetSocketAddress recipient) {
    send(m, recipient, true);
  }

  public void send(byte[] m, InetSocketAddress recipient, boolean flush) {

    Channel channel = getChannel(recipient);
    // sending the bytes with a header corresponding to length of
    // actual bytes
    ByteBufAllocator alloc = channel.alloc();
    ByteBuf buf = alloc.buffer(m.length + 4);
    buf.writeInt(m.length);
    buf.writeBytes(m);
    if (flush) {
      channel.writeAndFlush(buf);
    } else {
      channel.write(buf);
    }
  }

  private Channel getChannel(InetSocketAddress recipient) {

    Channel channel = openChannels.computeIfAbsent(recipient, s -> {
      System.out.println("Creating Channel " + recipient);
      ChannelFuture f;
      int retries  = 0;
      boolean success = false;
      Channel c = null;
      while (!success) {
        try {
          f = clientBootstrap.connect(s).sync();
          System.out.println("Channel Created " + recipient);
          c = f.channel();
          success = true;
        } catch (InterruptedException e) {
          success = false;
          c = null;
        } catch (Exception e) {
          System.out.println("Retrying " + retries + " " + e.getMessage() + " " + recipient);
          retries++;
          success = false;
          c = null;
        }
      }
      return c;
    });

    if (channel == null) {
      System.err.println("Failed to connect to " + recipient);
      System.exit(-1);
    }

    return channel;

  }

  @Override
  public void send(Msg.Message msg, InetSocketAddress rcpt, boolean flush) {
    Channel channel = getChannel(rcpt);
    if (flush) {
      channel.writeAndFlush(msg);
    } else {
      channel.write(msg);
    }
  }

  @Override
  public void send(Msg.Message msg, InetSocketAddress rcpt) {
    send(msg, rcpt, true);
  }


  public void onChannelInactive(Channel channel) {
    openChannels.remove(channel.remoteAddress());
  }
}
