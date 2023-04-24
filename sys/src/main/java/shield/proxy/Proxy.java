package shield.proxy;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.network.messages.Msg.Message;
import shield.proxy.client.ClientManager;
import shield.proxy.data.async.*;
import shield.proxy.data.async.IAsyncBackingStore.BackingStoreType;
import shield.proxy.data.sync.*;
import shield.proxy.oram.AsyncRingOram;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.trx.concurrency.TransactionManager;
import shield.proxy.trx.concurrency.TSOTransactionManager;
import shield.util.Logging;

import java.io.IOException;

public final class Proxy extends BaseNode {


  /**
   * Manages registered clients. Handles incoming/outgoing requests
   */
  private final ClientManager clientManager;

  /**
   * Handles transaction processing (scheduling, batching, concurrency control and request
   * execution)
   */
  private final TransactionManager trxManager;

  private IAsyncBackingStore dataStore;

  /**
   * For testing only
   */
  public Proxy() throws InterruptedException, NoSuchAlgorithmException {
    super();
    clientManager = new ClientManager(this);
    dataStore =
        config.BACKING_STORE_TYPE == IAsyncBackingStore.BackingStoreType.ORAM_PAR_HASHMAP
            ? AsyncRingOram.create(SecureRandom.getInstance("SHA1PRNG"), new AsyncMapBackingStore(this), this.workpool, config)
            : config.BACKING_STORE_TYPE == IAsyncBackingStore.BackingStoreType.NORAM_MAPDB
                ? new AsyncMapDBBackingStore(this)
                : new AsyncMapBackingStore(this);
    trxManager = new TSOTransactionManager(this);
    trxManager.startTrxManager();
  }

  public Proxy(String configFileName)
      throws InterruptedException, IOException, ParseException, NoSuchAlgorithmException {
    super(configFileName);
    clientManager = new ClientManager(this);
    dataStore = createDataManager(this);
    trxManager = new TSOTransactionManager(this);
    // trxManager.startTrxManager();
  }


  /**
   * For testing only
   */
  public Proxy(String addr, int port) throws InterruptedException, NoSuchAlgorithmException {
    super(addr, port);
    clientManager = new ClientManager(this);
    dataStore = createDataManager(this);
    trxManager = new TSOTransactionManager(this);
    // trxManager.startTrxManager();
  }

  public Proxy(String addr, int port, BackingStoreType ty)
      throws InterruptedException, NoSuchAlgorithmException {
    super(addr, port);
    getConfig().BACKING_STORE_TYPE = ty;
    clientManager = new ClientManager(this);
    dataStore = createDataManager(this);
    trxManager = new TSOTransactionManager(this);
    // trxManager.startTrxManager();
  }

  /**
   * Initialises the data manager for the appropriate type
   */
  IAsyncBackingStore createDataManager(Proxy proxy) throws NoSuchAlgorithmException {

    IAsyncBackingStore dataManager;
    SecureRandom rng = SecureRandom.getInstance("SHA1PRNG");

    switch (proxy.getConfig().BACKING_STORE_TYPE) {
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
        ISyncBackingStore sserverLib = new SyncRemoteBackingStoreLib(this);
        this.dataStore= new AsyncSyncBackingStore(sserverLib);
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
        IAsyncBackingStore serverLib = new AsyncRemoteBackingStoreLib(this);
        this.dataStore= serverLib;
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
      case NORAM_DUMMY:
        dataManager = new AsyncDummyBackingStore(this);
        break;
      default:
        throw new RuntimeException("Unsupported storage backend");
    }

    return dataManager;
  }

  public ClientManager getClientManager() {
    return clientManager;
  }


  public IAsyncBackingStore getDataManager() {
    return dataStore;
  }

  public TransactionManager getTrxManager() {
    return trxManager;
  }

  @Override
  public void handleMsg(Message msg) {

    Msg.Message.Type ty;

    ty = msg.getMessageType();

    switch (ty) {
      case ClientReqMessage:
        clientManager.handleMsg(msg.getClientReqMsg());
        break;
      case DataRespMessage:
        dataStore.onHandleRequestResponse(msg.getDataRespMsg());
        break;
      default:
        logErr("Unrecognised message type " + ty, Logging.Level.CRITICAL);
        System.exit(-1);
    }

  }

  public void startProxy() {
    trxManager.startTrxManager();
   }
  }



