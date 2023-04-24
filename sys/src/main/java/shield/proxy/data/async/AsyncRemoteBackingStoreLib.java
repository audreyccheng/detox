package shield.proxy.data.async;

import com.google.protobuf.ByteString;
import java.util.Queue;
import shield.BaseNode;
import shield.network.messages.Msg.DataMessageReq;
import shield.network.messages.Msg.DataMessageResp;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Message.Type;
import shield.network.messages.Msg.Request.ReqType;
import shield.proxy.Proxy;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Library to contact remote storage.
 *
 * This library allows for multiple requests (either single requests or batched requests) to be
 * pending at once. While waiting for requests to finish, threads are blocked. If not used
 * carefully, it may cause starvation.
 *
 * The remote backing store does not provide any ordering guarantees if requests are submitted
 * concurrently (ak: with a pending requests, or within the same batch).
 *
 * TODO(natacha): only supports sending a single batch of read or a single batch of write requests.
 * Would make sense to be able to batches of reads/and writes
 *
 * TODO(natacha): currently does not support any way for the remote storage to convey that an error
 * has occurred
 *
 * @author ncrooks
 */
public class AsyncRemoteBackingStoreLib implements IAsyncBackingStore {

  /**
   * Map of ongoing (pending) requests. Each request should be assigned a unique id. An id can
   * correspond to a batch of requests (in which case all operations in a request message will have
   * the same op id)
   */
  public ConcurrentHashMap<Long, AsyncDataRequest> pendingIORequests;

  /**
   * Data Response Messages are placed in this data structure when received. They will be processed
   * and removed by the waiting processing threads
   */
  public ConcurrentHashMap<Long, DataMessageResp> receivedMessages;

  /**
   * Backpointer to proxy
   */
  public final BaseNode proxy;

  /**
   * Remote datastore address
   */
  public final InetSocketAddress remoteStoreAddress;

  /**
   * Uniquely identifies a pending request
   */
  public AtomicLong nextOpId;

  private final boolean useProxy;

  @SuppressWarnings("serial")
  private class DuplicateOperationException extends Exception {

  }

  public AsyncRemoteBackingStoreLib(BaseNode proxy) {
    this.proxy = proxy;
    System.out.println("Connecting to remote store address "
        + proxy.getConfig().REMOTE_STORE_IP_ADDRESS + " "
        + proxy.getConfig().REMOTE_STORE_LISTENING_PORT);
    this.remoteStoreAddress =
        new InetSocketAddress(proxy.getConfig().REMOTE_STORE_IP_ADDRESS,
            proxy.getConfig().REMOTE_STORE_LISTENING_PORT);
    this.nextOpId = new AtomicLong(0);
    // TODO(configure threads)
    this.receivedMessages = new ConcurrentHashMap<Long, DataMessageResp>(100,
        (float) 0.8, proxy.getConfig().N_WORKER_THREADS);
    this.pendingIORequests = new ConcurrentHashMap<Long, AsyncDataRequest>(100,
        (float) 0.8, proxy.getConfig().N_WORKER_THREADS);
    this.useProxy = proxy.getConfig().USE_PROXY;
  }


  /**
   * Testing constructor only
   */
  public AsyncRemoteBackingStoreLib(BaseNode node, String host, int port) {
    this.proxy = node;
    this.remoteStoreAddress = new InetSocketAddress(host, port);
    this.nextOpId = new AtomicLong(0);
    // TODO(configure threads)
    this.receivedMessages = new ConcurrentHashMap<Long, DataMessageResp>(100,
        (float) 0.8, proxy.getConfig().N_WORKER_THREADS);
    this.pendingIORequests = new ConcurrentHashMap<Long, AsyncDataRequest>(100,
        (float) 0.8, proxy.getConfig().N_WORKER_THREADS);
    this.useProxy = proxy.getConfig().USE_PROXY;
  }


  public void send(DataMessageReq.Builder req) {
    if (!useProxy) {
      req.setClientId(proxy.getBlockId());
    }
    Message.Builder msg = Message.newBuilder();
    msg.setDataReqMsg(req);
    msg.setMessageType(Type.DataReqMessage);
    proxy.sendMsg(msg.build(), remoteStoreAddress, true);
  }

  /**
   * Takes a constructed request message and sends it. This function is NON-BLOCKING and does not
   * wait for the returned message to arrive. The {@link OnHandleRequestResponse} is responsible for
   * invoking the appropriate callback
   *
   * @param reqMsg - the request message
   * @param opId - the unique operation id for this request(s)
   */
  public void sendMsg(DataMessageReq.Builder reqMsg, long opId,
      AsyncDataRequest req) throws DuplicateOperationException {

    AsyncDataRequest success = pendingIORequests.putIfAbsent(opId, req);
    // System.out.println("[Op Id] Begin " + opId + " " + System.currentTimeMillis());
    if (success != null) {
      // There was a conflict in the operation id (maybe because of
      // the long counter wrapping around. Retry
      System.exit(-1);
      // throw new DuplicateOperationException();
    }
    send(reqMsg);
  }

  public long getNextOperationId() {
    return nextOpId.incrementAndGet();
  }

  @Override
  public void read(Long key, AsyncDataRequest req) {


    long opId;
    DataMessageReq.Builder reqMsg;
    boolean success = false;
    byte[] retValue = null;

    while (!success) {
      try {
        opId = getNextOperationId();
        reqMsg = DataMessageReq.newBuilder();

        reqMsg.addRequestsBuilder().setKey(key).setOpId(opId)
            .setReqType(ReqType.READ);

        sendMsg(reqMsg, opId, req);
        success = true;
      } catch (DuplicateOperationException e) {
        success = false;
        // TODO(handle this properly)
        System.exit(-1);
      }
    }
  }


  @Override
  public void read(List<Long> keys, AsyncDataRequest req) {
    long batchId;
    DataMessageReq.Builder reqMsg;
    boolean success = false;

    while (!success) {
      try {
        batchId = getNextOperationId();
        reqMsg = DataMessageReq.newBuilder();

        for (Long key : keys) {
          reqMsg.addRequestsBuilder().setKey(key).setOpId(batchId)
              .setReqType(ReqType.READ);
        }

        sendMsg(reqMsg, batchId, req);
        success = true;
      } catch (DuplicateOperationException e) {
        success = false;
        // TODO(handle this properly)
        System.exit(-1);
      }
    }
  }


  @Override
  public void write(Write write, AsyncDataRequest req) {
    long batchId;
    DataMessageReq.Builder reqMsg;
    boolean success = false;


    while (!success) {
      try {
        batchId = getNextOperationId();

        reqMsg = DataMessageReq.newBuilder();

        if (write.isDelete()) {
          reqMsg.addRequestsBuilder().setKey(write.getKey()).setOpId(batchId)
              .setReqType(ReqType.DELETE);
        } else {
          reqMsg.addRequestsBuilder().setKey(write.getKey())
              .setValue(ByteString.copyFrom(write.getValue())).setOpId(batchId)
              .setReqType(ReqType.WRITE);
        }
        sendMsg(reqMsg, batchId, req);
        success = true;
      } catch (DuplicateOperationException e) {
        success = false;
        // TODO(handle this properly)
        System.exit(-1);
      }
    }
  }


  @SuppressWarnings("unused")
  @Override
  public void write(Queue<Write> writes,
      AsyncDataRequest req) {
    long batchId;
    DataMessageReq.Builder reqMsg;
    boolean success = false;
    Long currentKey;
    ByteString currentValue;

    if (writes.isEmpty()) {
      proxy.executeAsync(() -> req.onDataRequestCompleted(),
          proxy.getDefaultExpHandler());

    } else {
      while (!success) {
        try {
          batchId = getNextOperationId();
          reqMsg = DataMessageReq.newBuilder();

          for (Write write : writes) {
            currentKey = write.getKey();
            if (write.isDelete()) {
              reqMsg.addRequestsBuilder().setKey(currentKey)
                  .setOpId(batchId).setReqType(ReqType.DELETE);
            } else {
              currentValue = ByteString.copyFrom(write.getValue());
              reqMsg.addRequestsBuilder().setKey(currentKey).setValue(currentValue)
                  .setOpId(batchId).setReqType(ReqType.WRITE);
            }
          }
          sendMsg(reqMsg, batchId, req);
          success = true;
        } catch (DuplicateOperationException e) {
          success = false;
          // TODO(handle this properly)
          System.exit(-1);
        }
      }
    }
  }

  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {



    long opId = msgResp.getRequestsList().get(0).getOpId();

    // System.out.println(receivedMessages);
    // DataMessageResp previousMessage = receivedMessages.put(opId, msgResp);
    List<byte[]> retValues;

    // There should never be a previous message present here, due to the
    // order in which we update the pendingIO/receivedMessage tables
    // in the sendMsgandWait function, and deal with duplicates/wrap-around
    // there
    AsyncDataRequest awaitMsg = pendingIORequests.remove(opId);
    // System.out.println("[Op Id] End " + opId + " " + System.currentTimeMillis());

    assert (opId != 0);
    assert (awaitMsg != null);

    retValues = msgResp.getRequestsList().stream()
        .filter(x -> x.getReqType() == ReqType.READ).map(x -> {
          ByteString b = x.getValue();
          if (b == null) {
            //return "".getBytes();
            return null;
          } else {
            return b.toByteArray();
          }
        }).collect(Collectors.toList());
    awaitMsg.setReadValues(retValues);
    proxy.executeAsync(() -> awaitMsg.onDataRequestCompleted(),
        proxy.getDefaultExpHandler());

  }


}
