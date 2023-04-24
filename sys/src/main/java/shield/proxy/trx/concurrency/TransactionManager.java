package shield.proxy.trx.concurrency;

import shield.proxy.Proxy;
import shield.proxy.client.ClientManager;

/**
 * Abstract away the details of transaction execution. The methods in this class are intentionally
 * very generic and assume full asynchrony. Every transaction
 *
 * @author ncrooks
 */
public abstract class TransactionManager {


  public enum CCManagerType {
    BATCH, NOBATCH
  }

  /**
   * Back-pointer to containing proxy
   */
  protected final Proxy proxy;

  /**
   * Back-pointer to client manager for callbacks
   */
  protected final ClientManager clientManager;

  public TransactionManager(Proxy proxy) {
    this.proxy = proxy;
    this.clientManager = proxy.getClientManager();
    assert (clientManager != null);
    assert (proxy != null);
  }

  /**
   * Starts the transaction. This operation must result in the callback onTransactionStarted()
   * eventually being called
   */
  public abstract void startTransaction(long clientId, Transaction t);

  /**
   * Callback when the transaction has successfully been executed. This operation must result in the
   * callback. If the transction did not successfully start, the returned transaction wil be null
   * TODO(natacha): add support of actual error messages?
   */
  public abstract void onTransactionStarted(Transaction trx, boolean success);

  /**
   * Executes a read or write operation. This method must result to a call top the
   * onOperationExecuted callback regardless of whether it is successful
   */
  public abstract void executeOperation(Operation op);

  /**
   * Notified when the operation has successfully executed. The return value of the operation (op
   * and ko) is stored in the operation, along with the return value if the operation was a read
   */
  public abstract void onOperationExecuted(Operation op);

  /**
   * Attempts to commit an operation. This method can lead either to a onTransactionCommitted
   * callback, or an onTransactionAborted callback if the commit failed
   */
  public abstract void commitTransaction(Transaction trx);

  /**
   * Notified when the transaction successfully committed. This callback should be called only if
   * the transaction successfully committed. It should also be called once all the state associated
   * with the committed transaction has been cleaned up.
   */
  public abstract void onTransactionCommitted(Transaction trx);

  /**
   * Aborts a transaction (this operation cannot fail)
   */
  public abstract void abortTransaction(Transaction trx);

  /**
   * Notified when the transaction has successfully aborted. This callback can be called following a
   * commit/abortTransaction. This callback should only be called once all the state associated with
   * the committed transaction has been cleaned up
   */
  public abstract void onTransactionAborted(Transaction trx);

  public abstract long getCurrentTs();

  /**
   * Begin executing transactions
   */
  public abstract void startTrxManager();
}
