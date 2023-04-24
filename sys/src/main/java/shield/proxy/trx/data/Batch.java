package shield.proxy.trx.data;

import com.google.common.collect.ConcurrentHashMultiset;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import shield.proxy.Proxy;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.trx.concurrency.Operation;
import shield.proxy.trx.concurrency.Transaction;
import shield.util.Pair;

/**
 * A batch executes multiple transactions and is the unit of durability in the system: once a batch
 * has finished executing, transactions are guaranteed to be durable. Transactions cannot span
 * multiple batches. A batch contains one-or-more strides, and a transaction can span multiple
 * strides. Strides are the unit of "request uniqueness" for the ORAM
 *
 * @author ncrooks
 */

public class Batch {

  /**
   * Back-pointer to proxy
   */
  private final Proxy proxy;

  /**
   * Queued Strides. These are strides that have not yet started executing
   */
  private final ConcurrentLinkedQueue<Stride> batchStrides;

  /**
   * Number of strides that have been executed
   */
  private AtomicInteger nbBatches;

  /**
   * Stride that is currently executing
   */
  private Stride executingStride;

  /**
   * Number of strides that have already finished executing
   */
  private int nbExecutedStride;

  /**
   * True if this all the strides in this batch have executed, and we are in the
   * concurrency-control/write-back phase
   */
  private boolean finishing = false;

  /**
   * Shared mode access to the batch
   */
  private final ReadLock rLock;

  /**
   * Exclusive mode access to the batch
   */
  private final WriteLock wLock;

  /**
   * Underlying storage
   */
  private final IAsyncBackingStore dataManager;

  /**
   * Time since the last stride was executed. All strides are executed at a fixed time interval,
   * regardless of how many operations are in the set
   */
  private long timeSinceLastStride;

  /**
   * Reference to the batch data handler that manages the execution of batches
   */
  private final BatchDataHandler dataHandler;

  /**
   * List of keys that will be written back in this batch (NB: currently do not remove keys from
   * this set when a transaction aborts TODO(natacha): add) TODO(natacha): change to set when move
   * to long
   */
  private final ConcurrentSkipListSet<Long> writeKeys;
  private final ConcurrentLinkedQueue<Long> writeKeysWithDuplicates;

  /**
   * Lock for adding write operations
   */
  private final Lock addOpLock;

  private final ConcurrentSkipListSet<Transaction> trxsInBatch;

  long begin = 0;
  long end = 0;

  public int batchId = 0;

  public Batch(Proxy proxy, BatchDataHandler dataHandler, int batchId) {
    this.proxy = proxy;
    this.dataHandler = dataHandler;
    this.batchStrides = new ConcurrentLinkedQueue<Stride>();
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    this.rLock = lock.readLock();
    this.wLock = lock.writeLock();
    this.addOpLock = new ReentrantLock();
    this.executingStride = null;
    this.nbExecutedStride = 0;
    this.finishing = false;
    this.dataManager = proxy.getDataManager();
    this.timeSinceLastStride = System.currentTimeMillis();
    this.nbBatches = new AtomicInteger();
    this.writeKeys = new ConcurrentSkipListSet<>();
    this.writeKeysWithDuplicates = new ConcurrentLinkedQueue<>();
    this.trxsInBatch = new ConcurrentSkipListSet<Transaction>();
    this.batchId = batchId;
    for (int i = 0 ; i < proxy.getConfig().MAX_NB_STRIDE ; i++) {
        batchStrides.add(new Stride(proxy, this));
    }
  }

  public BatchDataHandler getDataHandler() {
    return dataHandler;
  }

  public boolean scheduleRequest(Operation op) {
    boolean success;
    if (op.isRead() || op.isReadForUpdate()) {
      success = scheduleRead(op);
    } else {
      success = scheduleWrite(op);
    }
    if (success) {
      trxsInBatch.add(op.getTrx());
    }
    return success;
  }

  public ConcurrentSkipListSet<Long> getWriteKeys() {
    return writeKeys;
  }

  public boolean scheduleWrite(Operation op) {
    boolean success = false;
    boolean addedOp = false;

    if (rLock.tryLock() == false) {
      // Fail to acquire read lock, means that
      // stride is about to be executed
      System.out.println("Batch finished");
    } else {
      if (finishing == false) {

        if (!proxy.getConfig().ALLOW_DUPLICATES) {
          // Add the operation to the set of operations that
          // should be executed
          if (willAddWrite(op)) {
            // If this operation *might* create a new read,
            // need to acquire an exclusive lock to make sure that
            // no concurrent operation also adds a new read (thereby
            // increasing the size of the stride to > Stride_SIZE)
            addOpLock.lock();
            if (writeKeys.size() >= proxy.getConfig().WRITES_SIZE) {
              System.out.println("Batch full");
              success = false;
            } else {
              addedOp = writeKeys.add(op.getKey());
              success = true;
            }
            addOpLock.unlock();
          } else {
            addedOp = writeKeys.add(op.getKey());
            success = true;
          }
        } else {
          addOpLock.lock();
          if (writeKeys.size() >= proxy.getConfig().WRITES_SIZE) {
              success = false;
          } else {
              writeKeys.add(op.getKey());
              success = true;
          }
          addOpLock.unlock();
        }
      } else {
        // This batch already finished
        System.out.println("Batch finished");
      }
      rLock.unlock();
    }

    return success;
  }

  /**
   * Check whether this operation will add a key to the set of keys to read. This method will never
   * return false if it in fact adds an operation but may return true when in fact the operation
   * won't
   */
  private boolean willAddWrite(Operation op) {
    return !writeKeys.contains(op.getKey());
  }

  /**
   * Schedules a read operation into an existing stride, or a new stride if all strides are
   * currently full
   */
  public boolean scheduleRead(Operation op) {

    boolean wasScheduledOk = false;
    Iterator<Stride> it;
    Stride currentStride = null;
    Stride newStride = null;
    int currentNbStrides = 0;

    assert(false);

    if (rLock.tryLock() == false) {
      // If fail to acquire a lock this means that the batch
      // is about to finish executing, so also consider it a failure
      System.out.println("Batch was executing");
    } else {
      // Lock was acquired
      if (finishing == true) {
        // this batch already finished executing, so cannot
        // schedule an operation here.
        System.out.println("Batch was executing");
        } else {
        it = batchStrides.iterator();
        // Find minibatch to schedule operation on
        while (it.hasNext()) {
          System.out.println("Finding next stride");
          currentStride = it.next();
          wasScheduledOk = currentStride.scheduleRead(op);
          if (wasScheduledOk) {
            break;
          }
        }

        if (!wasScheduledOk) System.out.println("Batch was full " + op.isWrite());
      }
      rLock.unlock();
    }
    return wasScheduledOk;
  }


  /**
   * Starts next stride within a batch. NB: currently using a busy-wait loop as the probability that
   * there will not be a next stride to execute is extremely low in the steady-state
   */
  public void startNextStride() {

    long elapsed;
    long now;
    long toSleep;

    while (executingStride == null) {
      try {
        // Calculate minimum time between two strides
        now = System.currentTimeMillis();
        elapsed = now - timeSinceLastStride;
        toSleep = proxy.getConfig().TIME_BETWEEN_STRIDE - elapsed;
        if (toSleep > 0) {
          Thread.sleep(toSleep);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      executingStride =  batchStrides.poll();
      // Even if there is no stride to execute, we should always
      // execute strides at a fixed time interval
      assert (executingStride != null);
    }

    executingStride.executeReads();
  }


  /**
   * Operation successfully executed inside a stride, notify the transaction that scheduled the
   * operation
   */
  public void onOperationExecuted(Operation op) {
    dataHandler.onRequestResponse(op);
  }


  /**
   * Once stride has executed, notifies this batch, and notifies all requests that were involved in
   * this stride. Do this on in parallel on multiple threads
   */
  public void onStrideExecuted(Stride stride) {
    // Notify all the operations in the stride that executed.
    Queue<Operation> operations = stride.getOperationsInStride();
    for (Operation op : operations) {
      proxy.executeAsync(() -> onOperationExecuted(op),
          proxy.getDefaultExpHandler());
    }
    timeSinceLastStride = System.currentTimeMillis();
    // If there are no more free strides in this batch, terminate batch
    if (++nbExecutedStride == proxy.getConfig().MAX_NB_STRIDE) {
      finaliseBatch();
    } else {
      executingStride = null;
      proxy.executeAsync(() -> startNextStride(), proxy.getDefaultExpHandler());
    }
  }


   public SortedSet<Transaction> getTrxsInBatch() {
    return trxsInBatch;
  }

  /**
   * This method is called when all strides in a batch have been executed. First, it aborts all
   * transaction which have not yet finished executing
   */
  public void finaliseBatch() {

    try {
      Thread.sleep(proxy.getConfig().FINALISE_BATCH_BUFFER_T);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }


    Queue<Write> versionsToWrite =
        dataHandler.forceBatchChange(this);
    begin = System.nanoTime();
    doWritesForBatch(versionsToWrite);
  }

  public void writeLock() {
    wLock.lock();
  }

  public void writeUnlock() {
    wLock.unlock();
  }

  public void markFinished() {
    finishing = true;
  }

  public void doWritesForBatch(Queue<Write> versionsToWrite) {
    // TODO(natacha): should be possible to optimise this
    // Directly return as a pair of ByteString/byte[]?

    dataManager.write(versionsToWrite, new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        onWritesFlushed();
      }
    });
  }

  public void onWritesFlushed() {
    end = System.nanoTime();
    System.out.println("Flushing Writes Took " + ((double)end-begin)/1000000);
    dataHandler.onBatchFinalised(this);
  }

  public void removeTrxFromBatch(Transaction trx) {
    trxsInBatch.remove(trx);

  }


  //TODO(Natacha): add a way to not schedule transactions if the batch is about to finish
  public boolean scheduleTransaction(Transaction t) {
    boolean added = false;
    if (rLock.tryLock() == false) {
      // If fail to acquire a lock this means that the batch
      // is about to finish executing, so also consider it a failure
    } else {
      // Lock was acquired
      if (finishing == true) {
        // this batch already finished executing, so cannot
        // schedule an operation here.
      } else {
        added = true;
        trxsInBatch.add(t);
      }
      rLock.unlock();
    }
    return added;
   }
}
