package shield.proxy.data.sync;

import static shield.network.messages.Msg.Request.ReqType.WRITE;

import com.google.protobuf.ByteString;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.network.messages.Msg.DataMessageReq;
import shield.network.messages.Msg.DataMessageResp;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Request;
import shield.proxy.data.sync.ISyncBackingStore.BackingStoreType;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data server for ORAM node: handles requests and serves requests from the chosen backing store
 *
 * @author ncrooks
 */
public class SyncRemoteBackingStoreServer extends BaseNode {

  private ISyncBackingStore store;

  private final InetSocketAddress proxyAddress;

  private final ConcurrentHashMap<Long, InetSocketAddress> clientIdToAddress;

  private final boolean useProxy;

  public SyncRemoteBackingStoreServer(String configFileName)
      throws InterruptedException, IOException, ParseException {
    super(configFileName);
    this.clientIdToAddress = new ConcurrentHashMap<>();
    switch (config.REMOTE_BACKING_STORE_TYPE) {
      case NORAM_HASHMAP:
        store = new SyncMapBackingStore(config);
        break;
      case NORAM_MAPDB:
        store = new SyncMapDBBackingStore(config);
        break;
      case NORAM_DUMMY:
        store = new SyncDummyBackingStore(config.ORAM_VALUE_SIZE);
        break;
      default:
        store = null;
        logErr("Incompatible HashStore" + config.REMOTE_BACKING_STORE_TYPE,
            Logging.Level.CRITICAL);
        System.exit(-1);
    }
    proxyAddress = new InetSocketAddress(getConfig().PROXY_IP_ADDRESS,
        getConfig().PROXY_LISTENING_PORT);
    this.useProxy = getConfig().USE_PROXY;
  }

  /**
   * Testing Constructor only
   */
  public SyncRemoteBackingStoreServer(String localHost, int localPort,
      String remoteHost, int remotePort, BackingStoreType ty)
      throws InterruptedException, IOException {
    super(localHost, localPort);
    this.clientIdToAddress = new ConcurrentHashMap<>();
    switch (ty) {
      case NORAM_HASHMAP:
        store = new SyncMapBackingStore();
        break;
      case NORAM_MAPDB:
        throw new RuntimeException("Unimplemented");
      default:
        store = null;
        logErr("Incompatible HashStore" + ty, Logging.Level.CRITICAL);
        System.exit(-1);
    }
    proxyAddress = new InetSocketAddress(remoteHost, remotePort);
    this.useProxy = getConfig().USE_PROXY;
  }

  @Override
  public void handleMsg(Message msg) {
    switch (msg.getMessageType()) {
      case DataReqMessage:
        handleDataReqMessage(msg);
        break;
      case ClientReqMessage:
        handleClientReqMessage(msg);
        break;
      default:
        logErr("Unrecognized message type " + msg.getMessageType());
        System.exit(-1);
    }
  }

  public void handleDataReqMessage(Message msg) {

    DataMessageReq req;
    DataMessageResp.Builder resp;
    List<Long> readRequests;
    ConcurrentLinkedQueue<Write> writeRequests;
    List<byte[]> returnValues;
    Iterator<byte[]> it;
    Request.Builder respReq;

    req = msg.getDataReqMsg();
    readRequests = new LinkedList<Long>();
    writeRequests = new ConcurrentLinkedQueue<>();


    for (Request op : req.getRequestsList()) {
      if (op.getReqType() == Request.ReqType.READ) {
        readRequests.add(op.getKey());
      } else {
        writeRequests.add(new Write(op.getKey(),
            op.getValue().toByteArray(), op.getReqType() == WRITE?
            Type.WRITE: Type.DELETE));
      }
    }

    // Execute Read Operations
    returnValues = store.read(readRequests);
    // Execute Write Operations
    store.write(writeRequests);

    resp = DataMessageResp.newBuilder();
    it = returnValues.iterator();
    for (Request op : req.getRequestsList()) {
      if (op.getReqType() == Request.ReqType.READ) {
        byte[] val = it.next();
        if (val != null) {
          respReq = Request.newBuilder().setValue(ByteString.copyFrom(val));
        } else {
          respReq = Request.newBuilder().setValue(ByteString.EMPTY);
        }
        respReq.setReqType(Request.ReqType.READ);
        respReq.setOpId(op.getOpId());
      } else {
        respReq = Request.newBuilder();
        respReq.setReqType(WRITE);
        respReq.setOpId(op.getOpId());
      }
      resp.addRequests(respReq);
    }
    Message.Builder response = Message.newBuilder();
    response.setDataRespMsg(resp);
    response.setMessageType(Msg.Message.Type.DataRespMessage);
    if (useProxy) {
      sendMsg(response.build(), proxyAddress);
    } else {
      InetSocketAddress clientAddress = clientIdToAddress.get(req.getClientId());
      sendMsg(response.build(), clientAddress, true);
    }
  }

  private void handleClientReqMessage(Message msg) {
      Msg.ClientMessageReq req = msg.getClientReqMsg();
      InetSocketAddress clientAddress = new InetSocketAddress(req.getClientHost(), req.getClientPort());
      clientIdToAddress.put(req.getClientId(), clientAddress);

      Msg.ClientMessageResp.Builder resp = Msg.ClientMessageResp.newBuilder();
      resp.setIsError(false);
      resp.setRespType(Msg.ClientMessageResp.RespType.REGISTER);
      Msg.Message.Builder respMsg = Message.newBuilder();
      respMsg.setClientRespMsg(resp);
      respMsg.setMessageType(Msg.Message.Type.ClientRespMessage);
      sendMsg(respMsg.build(), clientAddress, true);
    }


}
