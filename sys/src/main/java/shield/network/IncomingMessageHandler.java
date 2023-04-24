package shield.network;

import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.buffer.ByteBuf;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.util.Logging;

import java.net.InetSocketAddress;

/**
 * Handles a server-side channel.
 */
public class IncomingMessageHandler {

  /**
   * Reference to current processing component
   */
  private final BaseNode block;

  public IncomingMessageHandler(BaseNode block) {
    this.block = block;
  }

  public byte[] getArray(ByteBuf buf) {
    byte[] bytes;
    int length = buf.readableBytes();

    if (buf.hasArray()) {
      bytes = buf.array();
    } else {
      bytes = new byte[length];
      buf.getBytes(buf.readerIndex(), bytes);
    }
    buf.clear();
    return bytes;
  }

  public void handle(byte[] bytes) {
    Msg.Message msg;
    try {
      msg = Msg.Message.parseFrom(bytes);
      block.handleMsg(msg);
    } catch (InvalidProtocolBufferException e) {
      block.logErr(e.toString(), Logging.Level.CRITICAL);
    }
  }

  public void handle(Msg.Message msg) {
    block.handleMsg(msg);
  }


}
