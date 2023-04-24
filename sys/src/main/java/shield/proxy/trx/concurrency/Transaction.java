package shield.proxy.trx.concurrency;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import shield.network.messages.Msg;
import shield.proxy.trx.data.Batch;

//TODO(natacha): way in which acquire locks is not very clean

public class Transaction implements Comparable<Transaction> {


  public enum TxState {
    NOT_STARTED,
    /**
     * Transaction is running and has not yet sent a commit or abort request
     */
    ONGOING,
    /**
     * This transaction has finished executing and is waiting for dependent transactions to either
     * commit or abort. If dependent transactions abort, the transaction will be aborted
     */
    FINISHED,
    /**
     * This trx has been marked for abort and will be cleaned-up
     * This occurs when a transaction triggers a rollback of dependent
     * transactions
     */
    WILL_ABORT,
    /**
     * This transaction has aborted and it's state has been cleaned up
     */
    ABORTED,
    /**
     * This transaction has been committed (either durably or not, depending on the mode in which
     * we're executing in)
     */
    COMMITTED,
  }

  /**
   * Unique transaction id: timestamp
   */
  private long trxTimestamp;

  /**
   * Client Id that this transaction is associated with
   */
  private final long clientId;

  /**
   * List of transactions that depend on this transaction (read a value that this transaction
   * wrote)
   *
   * This is used to notify transactions that this trx has committed
   */
  private final ConcurrentSkipListSet<Transaction> outgoingDependencies;

  /**
   * List of transactions that this transaction depends on. This is used to determine when a
   * transaction can safely commit
   */

  private final ConcurrentSkipListSet<Transaction> incomingDependencies;


  /**
   * Operations that this transaction contains. This is a place-holder for storing the actual value
   */
  private final LinkedList<Operation> operations;

  /**
   * Current state of the transaction
   *
   * Valid state transitions are:
   */
  private volatile TxState txState;

  /**
   * Transaction exclusive lock
   */
  private ReentrantLock lock;

  private Batch batch;



  public Transaction(long clientId) {
    this.operations = new LinkedList<Operation>();
    this.clientId = clientId;
    this.trxTimestamp = 0;
    this.txState = TxState.NOT_STARTED;
    this.incomingDependencies = new ConcurrentSkipListSet<Transaction>();
    this.outgoingDependencies = new ConcurrentSkipListSet<Transaction>();
    this.lock = new ReentrantLock();

  }

  public int getDependentTransactionsCount() {
    return incomingDependencies.size();
  }


  public void setTrxTimestamp(long timestamp) {
    // assert (trxTimestamp == 0);
    lock.lock();
    trxTimestamp = timestamp;
    this.txState = TxState.ONGOING;
    lock.unlock();
  }

  /**
   * Add transaction trx that is dependent on T. Must wait until T finishes to begin committing
   */
  public void addDependantTransaction(Transaction trx) {
    lock.lock();
    assert (trx.getTimestamp() > trxTimestamp);
    assert (txState != TxState.ABORTED);
    outgoingDependencies.add(trx);
    lock.unlock();
  }

  /**
   * Add transaction trx that T is dependent on (T must wait until that transaction finishes before
   * committing) trx --wr--> T
   */
  public void addDependingTransaction(Transaction trx) {
    lock.lock();
     assert (trx.getTimestamp() < trxTimestamp);
    assert (!trx.isAborting());
    incomingDependencies.add(trx);
    lock.unlock();
  }

  public Operation addOperation(Msg.Statement stat) {
    Operation op;
    lock();
    op = new Operation(this, stat, operations.size());
    operations.add(op);
    unlock();
    return op;
  }



  public void setBatch(Batch b) {
    this.batch = b;
  }

  public Batch getBatch() {
    return batch;
  }

  @Override
  public int compareTo(Transaction arg0) {
     if (trxTimestamp < arg0.getTimestamp()) {
      return -1;
    } else if (trxTimestamp > arg0.getTimestamp()) {
      return 1;
    }
    return 0;
  }

  /**
   * Returns true if any of the operations has been marked has having failed.
   */
  public boolean containsFailedOp() {
    try {
      lock.lock();
      return operations.stream().anyMatch(op -> !op.wasSuccessful());
    } finally {
      lock.unlock();
    }
  }


  public long getClientId() {
    return clientId;
  }

  public ConcurrentSkipListSet<Transaction> getDependingTransactions() {
     try {
       lock.lock();
       return new ConcurrentSkipListSet<Transaction>(outgoingDependencies);
     } finally {
       lock.unlock();
     }
  }

  public int getOpCount() {
     try {
       lock.lock();
       return operations.size();
     } finally {
       lock.unlock();
     }
  }

  public LinkedList<Operation> getOperations() {
     try {
       lock.lock();
       return new LinkedList<Operation>(operations);
     } finally {
        lock.unlock();
     }
  }



  public long getTimestamp() {
    return trxTimestamp;
  }


  public boolean isAborting() {
    try {
      lock.lock();
      return (txState == TxState.ABORTED
          || txState == TxState.WILL_ABORT);
    } finally {
      lock.unlock();
    }
  }

  public TxState getTrxState() {
    try {
      lock.lock();
      return txState;
    } finally {
      lock.unlock();
    }
  }


  /**
   * Returns true if all the transactions on which this transaction depends have already committed.
   *
   * This function must own the transaction lock when called for thread-safety
   */
  public boolean isCommittable() {
     try {
       lock.lock();
       return incomingDependencies.size() == 0;
     } finally {
       lock.unlock();
     }
  }

  /**
   * Acquires an exclusive lock for this transaction. To avoid deadlocks, transaction locks should
   * always be acquired in increased timestamp order.
   *
   * This function is blocking and will wait until the lock has been successfully acquired before
   * returning.
   */
  public void lock() {
    lock.lock();
   }

  /**
   * Updates the state of a transaction appropriately once that transaction is being rollback by a
   * dependent transaction aborting. Marking the transaction has aborted means that the calling
   * thread must queue an abortTransaction request. Otherwise transaction will never get a callback
   */
  public void markRollbacked() {
    try {
      lock.lock();
      if (txState != TxState.ABORTED) {
        txState = TxState.WILL_ABORT;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Notifies the transaction that a transaction on which it depends has committed. Returns true if
   * the transaction is now committable: its dependency set is empty and it is marked as ready to
   * commit.
   *
   * This function must own the transaction lock when called for thread-safety
   *
   * @return true if transaction is now ready to commit
   */
  public boolean notifyCommit(Transaction trx) {
    try {
      lock.lock();
      incomingDependencies.remove(trx);
      if (incomingDependencies.size() == 0 && txState == TxState.FINISHED) {
        return true;
      } else {
        return false;
      }
    } finally {
        lock.unlock();
    }
  }

  public void setTrxState(TxState state) {
    try {
      lock.lock();
      this.txState = state;
    } finally {
      lock.unlock();
    }
  }

  public void unlock() {
    lock.unlock();
  }




}
