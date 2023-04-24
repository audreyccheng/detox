package shield.proxy.trx.data;

import java.util.concurrent.atomic.AtomicLong;
import shield.config.NodeConfiguration;
import shield.proxy.Proxy;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.trx.concurrency.Operation;
import shield.proxy.trx.concurrency.TSOMetadata;
import shield.proxy.trx.concurrency.TSOTransactionManager;
import shield.proxy.trx.concurrency.Transaction;

/**
 * Handles data requests from the TSO Store.
 *
 * This class is primarily used to abstract away the difference between batching and traditional
 * execution
 *
 * @author ncrooks
 */
public abstract class DataHandler {

  /**
   * Shortcut to the proxy
   */
  protected final Proxy proxy;

  /**
   * Shortcut to the current configuration parameters
   */
  protected final NodeConfiguration config;
  /**
   * Reference to the underlying datastore (MapDB, ORAM, etc.)
   */
  protected final IAsyncBackingStore dataStore;

  protected final AtomicLong operationsProcessed;


  protected TSOMetadata metadata;

  protected TSOTransactionManager trxManager;

  public DataHandler(Proxy proxy) {
    this.proxy = proxy;
    this.config = proxy.getConfig();
    this.dataStore = proxy.getDataManager();
    this.operationsProcessed = new AtomicLong(0);
    System.out.println("Creating Data Handler");
  }

  abstract public void handleRequest(Operation op);

  abstract public void cancelWrite(Operation op);

  abstract protected void onRequestResponse(Operation op);

  abstract public void commitTransaction(Transaction t);

  abstract protected void onTransactionCommitted(Transaction t);

  public void setTSO(TSOTransactionManager trxManager) {
    this.trxManager = trxManager;
    this.metadata = trxManager.getMetadata();
  }

  public void startDataHandler() {

  }

  public long getProcessedOperations() {
    return operationsProcessed.get();
  }

  public abstract boolean startTransaction(Transaction t);
}
