package shield.network.netty;

import io.netty.channel.ChannelFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import shield.network.NetworkQueue;
import shield.network.NetworkReceiver;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyTCPReceiver implements NetworkReceiver {

  // The channel on which we'll accept connections
  private ServerBootstrap bootstrap;

  public NettyTCPReceiver(InetSocketAddress me, NetworkQueue NQ) {
    this(me, NQ, 1);
  }

  public NettyTCPReceiver(InetSocketAddress me, NetworkQueue NQ,
      int threadCount) {
    this(me,NQ,threadCount, Executors.newCachedThreadPool());
  }
  public NettyTCPReceiver(InetSocketAddress me, NetworkQueue NQ,
      int threadCount, Executor executor) {
    bootstrap = new ServerBootstrap();
    bootstrap.channel(NioServerSocketChannel.class);
    bootstrap.group(
        // new NioEventLoopGroup(threadCount, ForkJoinPool.Executors.newCachedThreadPool()));
        new NioEventLoopGroup(threadCount, executor));
    bootstrap.childHandler(new ShieldChannelInitializer(NQ, null));
    setTCPNoDelay(true);
    setKeepAlive(true);
    ChannelFuture f = bootstrap.bind(me);
    while (!f.isDone()) {
      try {
        f.await();
      } catch (InterruptedException e) {
      }
      System.out.println("Waiting to Bound");
    }
    if (!f.isSuccess()) {
      System.err.println(f.cause().getStackTrace());
      System.err.println(f.cause().getMessage());
      System.exit(-1);
    }
    else {
      System.out.println("Bound " + me);
    }
  }

  public void setTCPNoDelay(boolean nodelay) {
    bootstrap.option(ChannelOption.TCP_NODELAY, nodelay);
  }

  public void setKeepAlive(boolean alive) {
    bootstrap.option(ChannelOption.SO_KEEPALIVE, alive);
  }

  @Override
  public void start() {
    // Not implemented yet
  }

  @Override
  public void stop() {
    // Not implemented yet
  }


}
