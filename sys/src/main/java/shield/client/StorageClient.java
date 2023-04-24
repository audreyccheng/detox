package shield.client;

import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.Random;
import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.proxy.data.async.*;
import shield.proxy.data.sync.*;
import shield.proxy.oram.AsyncRingOram;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.trx.data.Write;
import shield.util.Logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.List;

import static shield.network.messages.Msg.Message.Type.ClientReqMessage;
import static shield.util.Logging.Level.CRITICAL;
import static shield.util.Logging.Level.INFO;

public class StorageClient extends BaseNode implements IAsyncBackingStore {

  private IAsyncBackingStore dataManager;

  private final InetSocketAddress currentAddress;

  private final InetSocketAddress remoteStoreAddress;

  private boolean isRegistered;

  private String errorMsg;

  public StorageClient(String expConfigFile)
      throws InterruptedException, ParseException, IOException, NoSuchAlgorithmException {
    super(expConfigFile);
    this.currentAddress = new InetSocketAddress(config.NODE_IP_ADDRESS, config.NODE_LISTENING_PORT);
    this.remoteStoreAddress = new InetSocketAddress(config.REMOTE_STORE_IP_ADDRESS, config.REMOTE_STORE_LISTENING_PORT);
    this.dataManager = createDataManager();
    this.isRegistered = false;
  }

  IAsyncBackingStore createDataManager() throws NoSuchAlgorithmException {
    IAsyncBackingStore dataManager;
    SecureRandom rng = SecureRandom.getInstance("SHA1PRNG");
    System.out.println("Backing Store Type");
    switch (config.BACKING_STORE_TYPE) {
      case ORAM_SEQ_DUMMY:
            System.out.println("Creating ORAM Seq Dummy");
            dataManager = new AsyncSyncBackingStore(SyncRingOram.create(rng, new SyncDummyBackingStore(config.ORAM_VALUE_SIZE), config));
        break;
      case ORAM_SEQ_MAPDB:
        dataManager = new AsyncSyncBackingStore(SyncRingOram.create(rng, new SyncMapDBBackingStore(this), config));
        break;
      case ORAM_SEQ_HASHMAP:
        dataManager = new AsyncSyncBackingStore(SyncRingOram.create(rng, new SyncMapBackingStore(), config));
        break;
      case ORAM_SEQ_SERVER:
        // TODO: for now we must hackily register with remote server before initializing ORAM
        ISyncBackingStore sserverLib = new SyncRemoteBackingStoreLib(this);
        this.dataManager = new AsyncSyncBackingStore(sserverLib);
        registerClient();
        dataManager = new AsyncSyncBackingStore(SyncRingOram.create(rng, sserverLib, config));
        break;
      case ORAM_PAR_DUMMY:
        dataManager = AsyncRingOram.create(rng, new AsyncDummyBackingStore(this), this.workpool, config);
        break;
       case ORAM_PAR_MAPDB:
        dataManager = AsyncRingOram.create(rng, new AsyncMapDBBackingStore(this), this.workpool, config);
        break;
      case ORAM_PAR_HASHMAP:
        dataManager = AsyncRingOram.create(rng, new AsyncMapBackingStore(this), this.workpool, config);
        break;
      case ORAM_PAR_SERVER:
        // TODO: for now we must hackily register with remote server before initializing ORAM
        IAsyncBackingStore serverLib = new AsyncRemoteBackingStoreLib(this);
        this.dataManager = serverLib;
        registerClient();
        dataManager = AsyncRingOram.create(rng, serverLib, this.workpool, config);
        break;
      case NORAM_MAPDB:
        dataManager = new AsyncMapDBBackingStore(this);
        break;
      case NORAM_HASHMAP:
        dataManager = new AsyncMapBackingStore(this);
        break;
      case NORAM_SERVER:
        dataManager = new AsyncRemoteBackingStoreLib(this);
        break;
      default:
        throw new RuntimeException("Unsupported storage backend");
    }

    return dataManager;
  }

  public void shutdown() {
    getWorkpool().shutdown();
  }
  
  @Override
  public void handleMsg(Msg.Message msg) {
    switch (msg.getMessageType()) {
      case DataRespMessage:
        onHandleRequestResponse(msg.getDataRespMsg());
        break;
      case ClientRespMessage:
        handleRegisterClientReply(msg.getClientRespMsg());
        break;
      default:
        System.err.printf("Unknown message type: %s", msg.getMessageType().toString());
        System.exit(1);
        break;
    }
  }

  @Override
  public void read(Long key, AsyncDataRequest req) {
    dataManager.read(key, req);
  }

  @Override
  public void read(List<Long> key, AsyncDataRequest req) {
    dataManager.read(key, req);
  }

  @Override
  public void write(Write write, AsyncDataRequest req) {
    dataManager.write(write, req);
  }

  @Override
  public void write(Queue<Write> writes, AsyncDataRequest req) {
    dataManager.write(writes, req);
  }

  @Override
  public void onHandleRequestResponse(Msg.DataMessageResp msgResp) {
    dataManager.onHandleRequestResponse(msgResp);
  }

  public synchronized void registerClient() {
    try {
      logOut("Register Client " + this.getBlockId() + " " + currentAddress, Logging.Level.INFO);

      if (config.BACKING_STORE_TYPE == BackingStoreType.ORAM_SEQ_SERVER
          || config.BACKING_STORE_TYPE == BackingStoreType.ORAM_PAR_SERVER
          || config.BACKING_STORE_TYPE == BackingStoreType.NORAM_SERVER) {
        // First send message
        Msg.ClientMessageReq.Builder clientMsgB = Msg.ClientMessageReq.newBuilder();
        clientMsgB.setRegister(true);
        clientMsgB.setClientHost(currentAddress.getHostName());
        clientMsgB.setClientPort(currentAddress.getPort());
        clientMsgB.setClientId(this.getBlockId());
        Msg.Message.Builder msgB = Msg.Message.newBuilder();
        msgB.setClientReqMsg(clientMsgB);
        msgB.setMessageType(ClientReqMessage);
        sendMsg(msgB.build(), remoteStoreAddress, true);

        // Wait for replyproxyAddress
        while (!isRegistered) {
          wait();
        }

        // Check that reply was successful
        if (errorMsg != null) {
          logErr(errorMsg, CRITICAL);
          System.exit(-1);
        } else {
          logOut("Client registered", INFO);
        }
       }
      } catch(InterruptedException e){
        logErr("Client Interrupted", CRITICAL);
      }
  }

  private synchronized void handleRegisterClientReply(Msg.ClientMessageResp rsp) {
    logOut("Received Reg Client Reply", Logging.Level.FINE);
    if (rsp.getIsError()) {
      errorMsg = rsp.getErrorMsg();
      isRegistered = true;
    } else {
      isRegistered = true;
    }

    notifyAll();
  }
}
