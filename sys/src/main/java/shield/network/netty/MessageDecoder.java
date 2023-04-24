package shield.network.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

  public MessageDecoder() {
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in,
      List<Object> out) {
    // System.out.println(in.readerIndex() + " " + in.readableBytes());
    if (in.readableBytes() < 4) {
    } else {
      in.markReaderIndex();
      int len = in.readInt();
      if (in.readableBytes() < len) {
        in.resetReaderIndex();
      } else {
        byte[] msg = new byte[len];
        in.readBytes(msg);
        out.add(msg);
        // System.out.println("Left over " + len + " " + in.readableBytes());
      }
    }
  }

}
