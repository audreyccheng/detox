package shield.proxy.trx.concurrency;

import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import shield.config.NodeConfiguration;
import shield.proxy.Proxy;
import shield.proxy.trx.concurrency.Transaction.TxState;
import shield.proxy.trx.data.BatchDataHandler;
import shield.proxy.trx.data.DataHandler;
import shield.proxy.trx.data.SimpleDataHandler;

/**
 * Implements a multiversioned TSO algorithm
 *
 * @author ncrooks
 */
public class TSOTransactionManager extends TransactionManager {

  /**
   * Next Id of the transaction to commit
   */
  private final AtomicLong nextTimestamp;

  /**
   * Low-water mark
   */
  private long lowestCommittedTransaction;

  /**
   * Sorted list of aborted/committed transactions. A transaction is inserted here when a reject
   * message is received for its operation. It is removed from this set once nextCommittableTrx is
   * greater than this
   */
  private final SortedSet<Long> abortedOrCommittedTrxs;

  /**
   * Backpointer to configuration
   */
  private final NodeConfiguration config;

  /**
   * TSO MetadataStore
   */
  private final TSOMetadata metadata;

  /**
   * Data Handler: wrapper around the batching mode/non batching mode that the system can operate
   * in. Used to serve the data from the underlying backing-store
   */
  private final DataHandler dataHandler;

  /**
   * True if garbage collection is activated
   */
  private final boolean gcActivated;
  private final Lock gcLock;

  /**
   * True if return commit only when durable
   */
  private final boolean commitOnDurable;

  /**
   * In batch-mode only: batch-change lock
   */
  private final ReentrantReadWriteLock batchChangeLock;
  private final ReadLock batchChangeReadLock;
  private final WriteLock batchChangeWriteLock;

  private AtomicLong totalOperations = new AtomicLong();

  /**
   * List of operations that are pending during the batch change
   */
  private ConcurrentLinkedQueue<Runnable> pendingOperations;
  /**
    Lock to protect pending Ops;
   */
  private final ReentrantReadWriteLock pendingOpLock;
  private final ReadLock pendingOpReadLock;
  private final WriteLock pendingOpWriteLock;

  /**
   * Abort count statistics. Todo: fix
   */
  public int aborts = 0;

  public TSOTransactionManager(Proxy proxy) {
    super(proxy);
    config = proxy.getConfig();
    dataHandler = config.CC_MANAGER_TYPE ==
        TransactionManager.CCManagerType.BATCH ?
        new BatchDataHandler(proxy) :
        new SimpleDataHandler(proxy);
    metadata = new TSOMetadata(this, dataHandler, proxy);
    dataHandler.setTSO(this);
    abortedOrCommittedTrxs = new ConcurrentSkipListSet<Long>();
    nextTimestamp = new AtomicLong(VersionChain.kBaseTimestamp + 1);
    lowestCommittedTransaction = VersionChain.kBaseTimestamp + 1;
    batchChangeLock = new ReentrantReadWriteLock(true);
    batchChangeReadLock = batchChangeLock.readLock();
    batchChangeWriteLock = batchChangeLock.writeLock();
    gcActivated = config.GC_ACTIVATED;
    commitOnDurable = config.COMMIT_ON_DURABLE;
    pendingOperations = new ConcurrentLinkedQueue<>();
    pendingOpLock = new ReentrantReadWriteLock(true);
    pendingOpReadLock =  pendingOpLock.readLock();
    pendingOpWriteLock = pendingOpLock.writeLock();
    gcLock = new ReentrantLock();
    dataHandler.startDataHandler();
  }


  public void startTransaction(long clientId, Transaction t) {

    long timestampId;
    boolean success = false;

     if (!acquireReadLockOrBuffer(()->startTransaction(clientId, t))) {
     } else {
       if (t.isAborting()) {
         assert(false);
          // Transaction was marked as aborted, no need to do anything
         batchChangeReadLock.unlock();
       } else {
         // First assign new timestamp
         timestampId = nextTimestamp.getAndIncrement();
         t.setTrxTimestamp(timestampId);
         success = metadata.startTransaction(t);

         // This is where we will send dummy writes.
         // TODO(send dummy writes)
         // Send reply

         // If GC is activated, need to update the low water mark as have allocated
         // a timestamp
         if (gcActivated && !success) updateLowWaterMark(t);

         batchChangeReadLock.unlock();
         //TODO(natacha): nicer way to express this
         //if (success)
         onTransactionStarted(t, success);
       }
     }
  }

  public void onTransactionStarted(Transaction trx, boolean success) {
    if (!acquireReadLockOrBuffer(() -> onTransactionStarted(trx,success))) {

    }
    else {
      if (trx.getTrxState() != TxState.ABORTED) {
        batchChangeReadLock.unlock();
        proxy.executeAsync( () -> clientManager.onTransactionStarted(trx, success),
            proxy.getDefaultExpHandler());
      } else {
        batchChangeReadLock.unlock();
      }
    }
  }

  public boolean acquireReadLockOrBuffer(Runnable fn) {
    pendingOpReadLock.lock();
   boolean acquiredLock = batchChangeReadLock.tryLock();
    if (!acquiredLock) pendingOperations.add(fn);
    pendingOpReadLock.unlock();
    return acquiredLock;
  }

  public void executeOperation(Operation op) {
     if (!acquireReadLockOrBuffer(()->executeOperation(op))) {
      // Couldn't acquire a lock as ongoing a batch change. Requeue operation
    } else {
      Transaction trx = op.getTrx();
      if (trx.getTrxState() == TxState.ABORTED) {
        // This transaction has already been aborted
      } else {
        if (config.LOG_TOTAL_OPERATIONS) {
          if (totalOperations.incrementAndGet()%500==0)
            config.statsTotalOperations.addPoint(totalOperations);
        }
        switch (op.getType()) {

          case READ:
            metadata.doRead(op.getKey(), op);
            break;
          case READ_FOR_UPDATE:
            // System.out.println("[ReadFor] Read for update!!!");
            metadata.doReadForUpdate(op.getKey(), op);
            break;
          case WRITE:
            metadata.doWrite(op.getKey(), op);
            break;
          case DELETE:
            metadata.doDelete(op.getKey(), op);
            break;
          case DUMMY:
            metadata.doDummyWrite(op.getKey(), op);
            break;

        }
      }
      batchChangeReadLock.unlock();
    }
  }

  public void onOperationExecuted(Operation op) {
    clientManager.onOperationExecuted(op);
  }

  /**
   * Marks a transaction has ready to be committed (FINISHED). If all transactions that this
   * transaction depends on have already committed, then commits the transaction and notifies all
   * subsequent transactions that this transaction has committed. Otherwise, waits (a subsequent
   * transaction will eventually call this method, once the transaction is ready to commit)
   */
  public void commitTransaction(Transaction trx) {
    assert (trx != null);
    if (!acquireReadLockOrBuffer(()->commitTransaction(trx))) {
    } else {
      SortedSet<Transaction> depTransactions;
      boolean canNowCommit = false;
      trx.lock();
      if (trx.isAborting()) {
        // This transaction has already been aborted
      } else {
        if (trx.isCommittable()) {
          trx.setTrxState(TxState.COMMITTED);
          // Notify other transactions that they have committed
          depTransactions = trx.getDependingTransactions();
          for (Transaction depTrx : depTransactions) {
            depTrx.lock();
            canNowCommit = depTrx.notifyCommit(trx);
            if (canNowCommit) {
              proxy.executeAsync(() -> commitTransaction(depTrx),
                  proxy.getDefaultExpHandler());
            }
            depTrx.unlock();
          }
          if (!commitOnDurable ) {
            proxy.executeAsync(() -> onTransactionCommitted(trx),
                proxy.getDefaultExpHandler());
          } else if (config.CC_MANAGER_TYPE == CCManagerType.NOBATCH) {
              dataHandler.commitTransaction(trx);
          } else {
            // Wait until get notification
          }
        } else {
          System.out.println("Waiting. Number of dependencies " + trx.getTimestamp() + " " + trx.getDependentTransactionsCount());
          // Mark the transaction has finished. The commit function
          // will be triggered by the notifyCommit method of other transactions
          trx.setTrxState(TxState.FINISHED);
        }
      }
      trx.unlock();
      batchChangeReadLock.unlock();
    }
  }

  public void onTransactionCommitted(Transaction trx) {
    if (!acquireReadLockOrBuffer(() -> onTransactionCommitted(trx))) {
      // Couldn't acquire a lock as ongoing a batch change. Requeue operation
    } else {
      if (gcActivated) {
        updateLowWaterMark(trx);
      }
      batchChangeReadLock.unlock();
      clientManager.onTransactionCommitted(trx);
  }
  }



  public void abortTransaction(Transaction trx) {
    if (!acquireReadLockOrBuffer(()->abortTransaction(trx))) {
      // Batch change is ongoing, operation is buffered
    } else {
      trx.lock();
      if (trx.getTrxState() == TxState.ABORTED) {
        // This transaction was already aborted. Do nothing
        trx.unlock();
      } else {
        // Mark the transaction has aborted
        trx.setTrxState(TxState.ABORTED);
        // If it safe to release lock here because no operation
        // should try to read a version written by trx once
        // it is marked as aborted
        // Undo all the operations and rollback any affected transactions
        List<Operation> ops = trx.getOperations();
        ConcurrentSkipListSet<Transaction> dependingTransactions = trx.getDependingTransactions();
        trx.unlock();
        for (Operation o : ops) {
          System.out.println("Cleaning up " + o.getTrx().getTimestamp() + " " + o.getKey() + " " + o
              .hasExecuted());
          if (o.hasExecuted()) {
            metadata.cleanupOnAbort(o);
          }
        }
        for (Transaction rollBackTrx : dependingTransactions) {
          rollBackTrx.markRollbacked();
          // rollBackTrx.lock();
          System.out.println("Notifying " + rollBackTrx.getTimestamp() + " to rollback");
          if (config.LOG_ABORT_TYPES) {
            config.statsTSOAborts.addPoint(aborts++);
          }
          proxy.executeAsync(() -> onTransactionRolledback(rollBackTrx),
              proxy.getDefaultExpHandler());
          // rollBackTrx.unlock();
        }
        proxy.executeAsync(() -> onTransactionAborted(trx),
            proxy.getDefaultExpHandler());
      }
      batchChangeReadLock.unlock();
    }
  }

  public void onTransactionRolledback(Transaction trx) {
    if (!acquireReadLockOrBuffer(() -> clientManager.onRollback(trx))) {
      // Batch change is ongoing, operation is buffered
    } else {
      if (trx.getTrxState() != TxState.ABORTED) {
        batchChangeReadLock.unlock();
        clientManager.onRollback(trx);
      } else {
        batchChangeReadLock.unlock();
      }
    }
  }

  public void onTransactionAborted(Transaction trx) {
    if (!acquireReadLockOrBuffer(() -> onTransactionAborted(trx))) {
      // Batch change is ongoing, operation is buffered
    } else {
      // Note that in this case we do not need to check if the
      // transaction has been aborted, as there will only ever be a single
      // callback of onTransactionAborted
      if (gcActivated) {
        updateLowWaterMark(trx);
      }
      batchChangeReadLock.unlock();
      clientManager.onTransactionAborted(trx);
    }
  }

  public long getCurrentTs() {
    return nextTimestamp.get();
  }

  /**
   * Updates the low water mark for this instance of TSO. This is the timestamp below which all
   * transactions have committed and aborted. Any version below that timestamp can be safely
   * removed
   */
  public void updateLowWaterMark(Transaction trx) {
    gcLock.lock();
    long timestamp = trx.getTimestamp();
    abortedOrCommittedTrxs.add(trx.getTimestamp());

    while (timestamp == lowestCommittedTransaction) {
      abortedOrCommittedTrxs.remove(timestamp);
      lowestCommittedTransaction++;
      if (abortedOrCommittedTrxs.size() > 0) {
        timestamp = abortedOrCommittedTrxs.first();
      }
    }
    gcLock.unlock();

  }


  public void startTrxManager() {
    if (gcActivated) {
      doGarbageCollection();
    }
  }

  public Proxy getProxy() {
    return proxy;
  }

  public void doGarbageCollection() {
    System.out.println("[Garbage Collection] " + lowestCommittedTransaction);
    for (int i = 0; i < config.GC_THREADS; i++) {
      final int index = i;
      proxy.executeAsync(() -> metadata.garbageCollect(index), proxy.getDefaultExpHandler());
    }
  }

  long getLowestCommittedTransaction() {
      return lowestCommittedTransaction;
  }


  public NodeConfiguration getConfig() {
    return config;
  }

  public void batchWriteLock() {
    batchChangeWriteLock.lock();
  }

  public void batchWriteUnlock()
  {
    batchChangeWriteLock.unlock();
  }


  public TSOMetadata getMetadata() {
    return metadata;
  }


  public void flushBatch() {
     // nextTimestamp.set(VersionChain.kBaseTimestamp + 1);
     // lowestCommittedTransaction = VersionChain.kBaseTimestamp + 1;
     // abortedOrCommittedTrxs.clear();
     // metadata.clear();
  }


  /**
   * Execute operations that were buffered during the
   * batch change
   */
  public void executeBufferedOperations() {
    //System.out.println("[Batch change] Pending Operations: " + pendingOperations.size());
    pendingOpWriteLock.lock();
    for (Runnable fn: pendingOperations) {
      proxy.executeAsync(fn, proxy.getDefaultExpHandler());
    }
    pendingOperations.clear();
    pendingOpWriteLock.unlock();
  }
}
