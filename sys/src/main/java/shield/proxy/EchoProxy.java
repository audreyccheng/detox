package shield.proxy;

import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.ClientMessageResp.RespType;
import shield.network.messages.Msg.Message;
import shield.proxy.data.async.IAsyncBackingStore.BackingStoreType;
import shield.util.Logging;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy class used for benchmarking: does nothing and simply replies "ok" to the appropriate
 * message type.
 *
 * It is currently used to measure the max throughput of our networking layer
 *
 * @author ncrooks
 */
public final class EchoProxy extends BaseNode {

  byte[] value;

  ConcurrentHashMap<Long, InetSocketAddress> clientMap =
      new ConcurrentHashMap<Long, InetSocketAddress>(100, 0.9f, 32);

  /**
   * For testing only
   */
  public EchoProxy() throws InterruptedException {
    super();
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
  }

  public EchoProxy(String configFileName)
      throws InterruptedException, IOException, ParseException {
    super(configFileName);
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
  }

/**
   * For testing only
   */
  public EchoProxy(String addr, int port) throws InterruptedException {
    super(addr, port);
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
  }

  public EchoProxy(String addr, int port, BackingStoreType ty)
      throws InterruptedException {
    super(addr, port);
    getConfig().BACKING_STORE_TYPE = ty;
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
  }

  @Override
  public void handleMsg(Message msg) {

    Msg.Message.Type ty;

    ty = msg.getMessageType();

    switch (ty) {
      case ClientReqMessage:
        sendEchoResponse(msg.getClientReqMsg());
        break;
      default:
        logErr("Unrecognised message type " + ty, Logging.Level.CRITICAL);
        System.exit(-1);
    }

  }

  private void sendEchoResponse(ClientMessageReq clientReqMsg) {
    if (clientReqMsg.hasRegister()) {
      ClientMessageResp.Builder clientMsg;
      Msg.Message.Builder respMsg;
      InetSocketAddress addr =
          new InetSocketAddress(clientReqMsg.getClientHost(),
              clientReqMsg.getClientPort());
      clientMap.put(clientReqMsg.getClientId(), addr);
      clientMsg = ClientMessageResp.newBuilder();
      clientMsg.setRespType(RespType.REGISTER);
      clientMsg.setIsError(false);
      respMsg = Message.newBuilder();
      respMsg.setClientRespMsg(clientMsg);
      respMsg.setMessageType(Msg.Message.Type.ClientRespMessage);
      sendMsg(respMsg.build(), addr, true);
      } else {
      ClientMessageResp resp
          = ClientMessageResp.newBuilder().setIsError(false).addReadValues(ByteString.copyFrom(value)).setRespType(RespType.OPERATION).build();
      Message rsp = Message.newBuilder().setClientRespMsg(resp).setMessageType(
          Message.Type.ClientRespMessage).build();
      sendMsg(rsp, clientMap.get(clientReqMsg.getClientId()));
    }
  }

  public void startProxy() {
  }


}
