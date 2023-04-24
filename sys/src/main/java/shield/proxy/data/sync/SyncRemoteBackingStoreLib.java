package shield.proxy.data.sync;

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
public class SyncRemoteBackingStoreLib implements ISyncBackingStore {

  /**
   * Map of ongoing (pending) requests. Each request should be assigned a unique id. An id can
   * correspond to a batch of requests (in which case all operations in a request message will have
   * the same op id)
   */
  public ConcurrentHashMap<Long, Object> pendingIORequests;

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

  public SyncRemoteBackingStoreLib(BaseNode proxy) {
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
    this.pendingIORequests = new ConcurrentHashMap<Long, Object>(100,
        (float) 0.8, proxy.getConfig().N_WORKER_THREADS);
    this.useProxy = proxy.getConfig().USE_PROXY;
  }


  /**
   * Testing constructor only
   */
  public SyncRemoteBackingStoreLib(BaseNode node, String host, int port) {
    this.proxy = node;
    this.remoteStoreAddress = new InetSocketAddress(host, port);
    this.nextOpId = new AtomicLong(0);
    // TODO(configure threads)
    this.receivedMessages = new ConcurrentHashMap<Long, DataMessageResp>(100,
        (float) 0.8, proxy.getConfig().N_WORKER_THREADS);
    this.pendingIORequests = new ConcurrentHashMap<Long, Object>(100,
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
    proxy.sendMsg(msg.build(), remoteStoreAddress);
  }

  /**
   * Takes a constructed request message and sends it. This function is BLOCKING as it waits for the
   * reply to arrive, and consequently returns a DataMessageResp object
   *
   * TODO(natacha): currently waits an unbounded amount of time. May want to handle network
   * partition scenarios more gracefully
   *
   * @param reqMsg - the request message
   * @param opId - the unique operation id for this request(s)
   */
  public DataMessageResp sendMsgAndWait(DataMessageReq.Builder reqMsg,
      long opId) throws DuplicateOperationException, InterruptedException {

    DataMessageResp resp;
    Lock lock = new ReentrantLock();
    Object msgArrived = new Object();
    synchronized (msgArrived) {
      Object success = pendingIORequests.putIfAbsent(opId, msgArrived);
      if (success != null) {
        // There was a conflict in the operation id (maybe because of
        // the long counter wrapping around. Retry
        throw new DuplicateOperationException();
      }
      send(reqMsg);
      // TODO(natacha) add loop here
      msgArrived.wait();
    }
    resp = receivedMessages.get(opId);
    pendingIORequests.remove(opId);
    assert (resp != null);
    return resp;
  }

  public long getNextOperationId() {
    return nextOpId.incrementAndGet();
  }

  @Override
  public byte[] read(Long key) {

    long opId;
    DataMessageReq.Builder reqMsg;
    DataMessageResp respMsg;
    boolean success = false;
    byte[] retValue = null;

    while (!success) {
      try {
        opId = getNextOperationId();
        reqMsg = DataMessageReq.newBuilder();

        reqMsg.addRequestsBuilder().setKey(key).setOpId(opId)
            .setReqType(ReqType.READ);

        respMsg = sendMsgAndWait(reqMsg, opId);
        retValue = respMsg.getRequestsList().get(0).getValue().toByteArray();
        success = true;
      } catch (DuplicateOperationException e) {
        success = false;
      } catch (InterruptedException e) {
        System.exit(-1);
      }
    }

    return retValue;
  }


  @Override
  public List<byte[]> read(List<Long> keys) {
    long batchId = getNextOperationId();
    DataMessageReq.Builder reqMsg;
    DataMessageResp respMsg;
    boolean success = false;
    List<byte[]> retValues = null;


    while (!success) {
      try {
        batchId = getNextOperationId();
        reqMsg = DataMessageReq.newBuilder();

        for (Long key : keys) {
          reqMsg.addRequestsBuilder().setKey(key).setOpId(batchId)
              .setReqType(ReqType.READ);
        }

        respMsg = sendMsgAndWait(reqMsg, batchId);
        retValues = respMsg.getRequestsList().stream()
            .filter(x -> x.getReqType() == ReqType.READ).map(x -> {
              ByteString b = x.getValue();
              if (b == null) {
                return null;
              } else {
                return b.toByteArray();
              }
            }).collect(Collectors.toList());
        success = true;
      } catch (DuplicateOperationException e) {
        success = false;
      } catch (InterruptedException e) {
        System.exit(-1);
      }
    }

    return retValues;

  }


  public void write(Write write) {
    long batchId = getNextOperationId();
    DataMessageReq.Builder reqMsg;
    @SuppressWarnings("unused")
    DataMessageResp respMsg;
    boolean success = false;


    while (!success) {
      try {
        batchId = getNextOperationId();
        reqMsg = DataMessageReq.newBuilder();

        if (write.isDelete()) {
          reqMsg.addRequestsBuilder().setKey(write.getKey())
              .setOpId(batchId).setReqType(ReqType.DELETE);
        } else {
          reqMsg.addRequestsBuilder().setKey(write.getKey())
              .setValue(ByteString.copyFrom(write.getValue())).setOpId(batchId)
              .setReqType(ReqType.WRITE);
        }
        respMsg = sendMsgAndWait(reqMsg, batchId);
        success = true;
      } catch (DuplicateOperationException e) {
        success = false;
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }


  @SuppressWarnings("unused")
  @Override
  public void write(Queue<Write> writes) {
    long batchId = getNextOperationId();
    DataMessageReq.Builder reqMsg;
    DataMessageResp respMsg;
    boolean success = false;
    Long currentKey;
    ByteString currentValue;


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
        respMsg = sendMsgAndWait(reqMsg, batchId);
        success = true;
      } catch (DuplicateOperationException e) {
        success = false;
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }



  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {

    long opId = msgResp.getRequestsList().get(0).getOpId();
    DataMessageResp previousMessage = receivedMessages.put(opId, msgResp);

    // There should never be a previous message present here, due to the
    // order in which we update the pendingIO/receivedMessage tables
    // in the sendMsgandWait function, and deal with duplicates/wrap-around
    // there
    Object awaitMsg = pendingIORequests.get(opId);

    assert (opId != 0);
    assert (awaitMsg != null);
    assert (previousMessage == null);

    // Notify the waiting thread that a message has arrived
    synchronized (awaitMsg) {
      awaitMsg.notifyAll();
    }
  }


}
