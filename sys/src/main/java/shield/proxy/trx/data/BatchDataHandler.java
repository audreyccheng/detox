package shield.proxy.trx.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import shield.proxy.Proxy;
import shield.proxy.trx.concurrency.Operation;
import shield.proxy.trx.concurrency.Transaction;
import shield.proxy.trx.concurrency.Transaction.TxState;
import shield.util.Pair;

/**
 * Class handles the relationship between the batches (and strides) and the TSO concurrency control
 * component.
 *
 * Constructs the read/write batches when asked to serve a request (deduplicating requests as
 * appropriate, creating new batches as appropriate)
 *
 * Is responsible for batch changes: 1) blocking new transactions from executing in the TSO Store 2)
 * aborting ongoing transactions (and their dependents) 3) flushing out all writes
 *
 * @author ncrooks
 */
public class BatchDataHandler extends DataHandler {


  private Batch currentBatch;
  /**
   * Mapping from transaction to batch
   */
//  private final ConcurrentHashMap<Transaction, Batch> trxToBatch;

  /**
   * Number of pending operations that are ongoing
   */
  private final AtomicInteger pendingUndos;

  /**
   * Lock for waiting until all undo operations have executed
   */
  private final Object undoLock;

  /**
   * Number of batches execute so far
   */
  private int nbBatches;

  /**
   * Lock to protect "switching out" the current batch
   */
  private final Lock batchSwitchLock;

  /**
   * Lock to protect "pending" transactions on batch change
   */
  private final ReadWriteLock pendingBatchLock;

  /**
   * Set of operations currently waiting for the next batch to finish
   */
  private final ConcurrentLinkedQueue<Runnable> pendingStartTrxsRequests;


  /**
   * Number of transactions that were aborted on batch change
   */
  private int statsAbortedOnBatchChange = 0;

  /**
   * Number of operations that we failed to schedule
   */
  private int statsFailedScheduleAbortOps = 0;

  public AtomicLong duplicateReads = new AtomicLong(0);



  public BatchDataHandler(Proxy proxy) {
    super(proxy);
//    trxToBatch = new ConcurrentHashMap<Transaction, Batch>();
    pendingUndos = new AtomicInteger();
    undoLock = new Object();
    currentBatch = null;
    batchSwitchLock = new ReentrantLock();
    pendingBatchLock = new ReentrantReadWriteLock();
    pendingStartTrxsRequests = new ConcurrentLinkedQueue<>();
    System.out.println("Creating new batch handler");
  }

  @Override
  public void handleRequest(Operation op) {
    Transaction trx;
    Batch b;

    trx = op.getTrx();
    b = trx.getBatch();
    assert (b!=null);
    scheduleRequest(op, b);
  }


  /**
   * Schedules an operation into the next available stride in the next available batch (or in the
   * pre-selected batch).
   *
   * @param op - the operation to be scheduled
   * @param b - the batch, if any, with which the associated trx is associated
   */
  private void scheduleRequest(Operation op, Batch b) {

    boolean wasScheduledOk = false;
    Batch currentBatch = b;

    assert (b!=null);

      // Execute transaction in the batch it was already scheduled in
      wasScheduledOk = currentBatch.scheduleRequest(op);

    if (wasScheduledOk) {
      // successfully scheduled. We will receive a callback once the operation
      // has been executed if the operation is a read. Otherwise,
      // notify that the write has been executed (writes are just added
      // to a queue, hence they can execute synchronously
      if (!(op.isRead()||op.isReadForUpdate())) {
        proxy.executeAsync(() -> onRequestResponse(op),
            proxy.getDefaultExpHandler());
      }
    } else {
      // Failed to schedule this operation, must abort the
      // transaction as transactions cannot span multiple batches. Notify the
      // client immediately
      System.out.println("Failed to schedule op " + op.isWrite() + " " + op.isDelete());
     if (config.LOG_ABORT_TYPES) {
       config.statsBatchFullAbort.addPoint(statsFailedScheduleAbortOps++);
     }
      op.markError();
      onRequestResponse(op);
    }

  }

  @Override
  protected void onRequestResponse(Operation op) {
    metadata.onOperationExecuted(op, true);
  }

  @Override
  public void commitTransaction(Transaction t) {
      throw new RuntimeException("Unimplemented");
  }


  @Override
  protected void onTransactionCommitted(Transaction t) {
    if (config.COMMIT_ON_DURABLE) {
      trxManager.onTransactionCommitted(t);
    } // Else : don't send a notification as transaction
    // has already been marked as committed and returned
    // to the client
  }


  @Override
  public void startDataHandler() {
    proxy.executeAsync(() -> startNextBatch(), proxy.getDefaultExpHandler());
  }

  @Override
  public boolean startTransaction(Transaction t) {

     Batch b = null;
     boolean wasScheduledOk;
     b = getOrCreateBatch();
     assert(b!=null);
     wasScheduledOk = b.scheduleTransaction(t);
    if (wasScheduledOk) t.setBatch(currentBatch);
     else {
        //TODO(natacha): might be better to "queue" transactions and wake them up
       // when the next batch happens
      // addToPendingTrx(t);
     }
     return wasScheduledOk;

     }

     public void addToPendingTrx(Transaction t) {
       pendingBatchLock.readLock().lock();
       pendingStartTrxsRequests.add(() -> startTransaction(t));
       pendingBatchLock.readLock().unlock();
     }

     public void startPendingTransactions() {
        pendingBatchLock.writeLock().lock();
        ConcurrentLinkedQueue<Runnable>
            queue = new ConcurrentLinkedQueue<>(pendingStartTrxsRequests);
        pendingStartTrxsRequests.clear();
        pendingBatchLock.writeLock().unlock();

       System.out.println("[Batch Change] "
           + queue.size() + " transactions waiting");
       for (Runnable r: queue) {
         proxy.executeAsync(r, proxy.getDefaultExpHandler());
       }
     }



  public void onBatchFinalised(Batch b) {
    SortedSet<Transaction> transactions = b.getTrxsInBatch();
    System.out.println("[Batch Change] Batch Finalised " + transactions.size());
    for (Transaction t : transactions) {
      // Notify the transactions that the batch has finished executing
      if (config.COMMIT_ON_DURABLE) {
        assert (t.getTrxState() == TxState.COMMITTED);
        proxy.executeAsync(() -> onTransactionCommitted(t),
            proxy.getDefaultExpHandler());
      } // Else: transaction has already committed to the client
    }
    // Start processing next batch
    consumeBatch();
    proxy.executeAsync(() -> startNextBatch(), proxy.getDefaultExpHandler());
  }

  private void startNextBatch() {
    System.out.println("[Batch Change] Starting Next Batch: Nb " + nbBatches);
    Batch batch = getOrCreateBatch();
    // startPendingTransactions();
    batch.startNextStride();
    nbBatches++;
    }


  private Batch getOrCreateBatch() {
    Batch retBatch;
    batchSwitchLock.lock();
    if (currentBatch == null) {
      currentBatch = new Batch(proxy, this, nbBatches);
    } else {
    }
    retBatch = currentBatch;
    batchSwitchLock.unlock();
    return retBatch;
  }

  private Batch consumeBatch() {
   Batch retBatch = null;
  batchSwitchLock.lock();
  retBatch = currentBatch;
  currentBatch = null;
  batchSwitchLock.unlock();
  return retBatch;
  }


  /**
   * Check if a transaction as committed, and if not, aborts it and aborts dependent transactions
   */
  public void checkIfCommitted(Transaction t, Batch b) {


    t.lock();
    if (t.getTrxState() == TxState.COMMITTED) {
      // Trxs has committed, everything ok
      // waiting for the others to finish committing
      t.unlock();
    } else {
      b.removeTrxFromBatch(t);

      if (t.getTrxState() == TxState.ABORTED) {
        // was already cleaned, do nothing
        t.unlock();
      } else {
        System.out.println("Transaction was not finished");
        if (config.LOG_ABORT_TYPES) {
          config.statsLateOpAbort.addPoint(statsAbortedOnBatchChange++);
        }
        t.setTrxState(TxState.ABORTED);
        ConcurrentSkipListSet<Transaction> dependingTrxs = t.getDependingTransactions();
        // Have to release the lock here because cleanupOnAbort acquires lock on version chain
        List<Operation> operations = t.getOperations();
        t.unlock();
        for (Operation o : operations) {
          if (o.hasExecuted()) {
            metadata.cleanupOnAbort(o);
          }
        }


        proxy.executeAsync(() -> trxManager.onTransactionAborted(t),
            proxy.getDefaultExpHandler());
        /* for (Transaction depT : dependingTrxs) {
          depT.markRollbacked();
          pendingUndos.incrementAndGet();
          proxy.executeAsync(() -> checkIfCommitted(depT, b),
              proxy.getDefaultExpHandler());
        } */
      }
    }


    synchronized (undoLock) {
      pendingUndos.decrementAndGet();
      undoLock.notifyAll();
    }

  }

  /**
   * Forces batch change by 1) marking all ongoing transactions as aborted 2) cleaning up the
   * metadata associated with this batch 3) giving the storage the writes to flush
   */
  public Queue<Write> forceBatchChange(Batch b) {

    //System.out.println("[Batch Change] Begin Batch Change " + b.batchId);

    long begin = 0;
    long end = 0;

    SortedSet<Transaction> trxsInBatch;
    Queue<Write> versionsToWriteBack;

    // Acquire exclusive ownership of batch write lock.
    // No concurrent transaction can execute
    trxManager.batchWriteLock();
    b.writeLock();
    b.markFinished();
    begin = System.nanoTime();
    trxsInBatch = b.getTrxsInBatch();
    // trxToBatch.remove(trxsInBatch);
    System.out.println("[Batch Change] Number of trxs in batch" + trxsInBatch.size());
    // Check if transaction should be undone, for every transaction
    // in the batch
    pendingUndos.set(trxsInBatch.size());
    for (Transaction t : trxsInBatch) {
      proxy.executeAsync(() -> checkIfCommitted(t, b),
          proxy.getDefaultExpHandler());
    }

    // Wait until all undo requests have executed
    synchronized (undoLock) {
      while (pendingUndos.get() > 0) {
        try {
          undoLock.wait();
        } catch (InterruptedException e) {
        }
      }
    }

//    System.out.println("Getting latest write versions");

    // Now acquire all the versions to write back
    versionsToWriteBack = metadata.getLatestVersions(b.getWriteKeys());

    assert(versionsToWriteBack.size() <= config.WRITES_SIZE);

    System.out.println("[Batch Change] Number of versions to write " + versionsToWriteBack.size());
     // Clean metadata
    trxManager.flushBatch();

    end = System.nanoTime();

    //System.out.println("[Batch Change] ForceBatchChange " + ((double)end-begin)/1000000);
    trxManager.batchWriteUnlock();

    // Execute operations that were buffered
    trxManager.executeBufferedOperations();

    b.writeUnlock();

    //System.out.println("[Batch Change] Finish batch change");
    return versionsToWriteBack;

  }

  @Override
  public void cancelWrite(Operation op) {
    // Don't do anythings.
  }


}
