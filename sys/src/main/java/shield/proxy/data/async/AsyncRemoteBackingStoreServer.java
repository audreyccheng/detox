package shield.proxy.data.async;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.network.messages.Msg.DataMessageReq;
import shield.network.messages.Msg.DataMessageResp;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Request;
import shield.network.messages.Msg.Request.ReqType;
import shield.proxy.data.sync.ISyncBackingStore.BackingStoreType;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Logging;

/**
 * Data server for ORAM node: handles requests and serves requests from the chosen backing store
 *
 * @author ncrooks
 */
public class AsyncRemoteBackingStoreServer extends BaseNode {

  private IAsyncBackingStore store;

  private final InetSocketAddress proxyAddress;

  private final ConcurrentHashMap<Long, InetSocketAddress> clientIdToAddress;

  private final boolean useProxy;

  public AsyncRemoteBackingStoreServer(String configFileName)
      throws InterruptedException, IOException, ParseException {
    super(configFileName);
    this.clientIdToAddress = new ConcurrentHashMap<>();
    switch (config.REMOTE_BACKING_STORE_TYPE) {
      case NORAM_HASHMAP:
        store = new AsyncMapBackingStore(this);
        break;
      case NORAM_MAPDB:
        store = new AsyncMapDBBackingStore(this);
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
  public AsyncRemoteBackingStoreServer(String localHost, int localPort,
      String remoteHost, int remotePort, BackingStoreType ty)
      throws InterruptedException, IOException {
    super(localHost, localPort);
    this.clientIdToAddress = new ConcurrentHashMap<>();
    switch (ty) {
      case NORAM_HASHMAP:
        store = new AsyncMapBackingStore(this);
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
    // Currently support only reads or only writest


    DataMessageReq req;
    DataMessageResp.Builder resp;
    List<Long> readRequests;
    Queue<Write> writeRequests;
    boolean isRead = false;

    req = msg.getDataReqMsg();
    readRequests = new LinkedList<Long>();
    writeRequests = new ConcurrentLinkedQueue<>();

    for (Request op : req.getRequestsList()) {
      if (op.getReqType() == Request.ReqType.READ) {
        isRead = true;
        readRequests.add(op.getKey());
      } else {
        if (isRead == true) throw new RuntimeException();
        writeRequests.add(new Write(op.getKey(), op.getValue().toByteArray(),
            op.getReqType() == ReqType.WRITE? Type.WRITE: Type.DELETE));
      }
    }

    // Execute Read Operations
      if (isRead) {
      store.read(readRequests, new AsyncDataRequest() {
          @Override
          public void onDataRequestCompleted() {
            onRequestResponse(msg, readValues);
          }
        });
      }
      else {
        store.write(writeRequests, new AsyncDataRequest() {
          public void onDataRequestCompleted() {
            onRequestResponse(msg, readValues);
          }
        });
      }

     }

  public void onRequestResponse(Message msg, List<byte[]> returnValues) {
    DataMessageResp.Builder resp;
    DataMessageReq req = msg.getDataReqMsg();
    resp = DataMessageResp.newBuilder();
    Iterator<byte[]> it = returnValues.iterator();
    for (Request op : req.getRequestsList()) {
      Request.Builder respReq;
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
        respReq.setReqType(Request.ReqType.WRITE);
        respReq.setOpId(op.getOpId());
      }
      resp.addRequests(respReq);
    }
    Message.Builder response = Message.newBuilder();
    response.setDataRespMsg(resp);
    response.setMessageType(Message.Type.DataRespMessage);
    if (useProxy) {
      sendMsg(response.build(), proxyAddress);
    } else {
      InetSocketAddress clientAddress = clientIdToAddress.get(req.getClientId());
      sendMsg(response.build(), clientAddress);
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
