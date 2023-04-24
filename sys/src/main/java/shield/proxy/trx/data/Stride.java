package shield.proxy.trx.data;

import shield.network.messages.Msg.Statement.Type;
import shield.proxy.Proxy;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.IAsyncBackingStore;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import shield.proxy.trx.concurrency.Operation;

/**
 * A stride is contained in a batch and contains a set of read operations which are guaranteed to be
 * unique
 *
 * (TODO): add commends
 *
 * @author ncrooks
 */
public class Stride {

  /**
   * Backpointer to proxy
   */
  private final Proxy proxy;
  /**
   * Backpointer to enclosing batch
   */
  private final Batch batch;
  /**
   * Datastore that will be used to serve the requests
   */
  private final IAsyncBackingStore dataStore;
  /**
   * Number of distinct keys that will be read in this stride (this is not equal to the number of
   * distinct operations)
   */
  private final AtomicInteger readOpsToExecuteCount;
  /**
   * Datastructure that keeps track of operations associated with this batch
   */
  private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Operation>> readOpsToExecute;
  /**
   * Keys to send: actual keys that will be executed in the stride
   */
  private final LinkedList<Long> keysToSend;

  /**
   * List of all the operations in this stride
   */
  private final Queue<Operation> operationsInStride;
  /**
   * Acquired by all operations that are added to the stride
   */
  private final ReadLock rLock;
  /**
   * Acquired by the stride when it is finished, and about to execute
   */
  private final WriteLock wLock;
  /**
   * True if the stride is no longer accepting requests and instead is executing read requests with
   * the ORAM
   */
  private boolean finishing;

  /**
   * Lock that regulates the size of the stride and how we add operations
   */
  private Lock addOpLock;

  public Stride(Proxy proxy, Batch batch) {
    this.proxy = proxy;
    this.batch = batch;
    this.dataStore = proxy.getDataManager();
    this.readOpsToExecuteCount = new AtomicInteger(0);
    this.readOpsToExecute =
        new ConcurrentHashMap<Long, ConcurrentLinkedQueue<Operation>>(
            proxy.getConfig().STRIDE_SIZE, 0.9f,
            proxy.getConfig().N_WORKER_THREADS);
    this.keysToSend = new LinkedList<Long>();
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    this.rLock = lock.readLock();
    this.wLock = lock.writeLock();
    this.addOpLock = new ReentrantLock();
    this.finishing = false;
    this.operationsInStride = new ConcurrentLinkedQueue<Operation>();
  }

  /**
   * Check whether this operation will add a key to the set of keys to read. This method will never
   * return false if it in fact adds an operation but may return true when in fact the operation
   * won't
   */
  private boolean willAddRead(Operation op) {
    return !readOpsToExecute.containsKey(op.getKey());
  }

  /**
   * Adds an operation to the set of operations that will be executed, creating a new key entry if
   * necessary
   *
   * @param op - the operation to add
   * @return true - if a new key entry has been added
   */
  private boolean addOperation(Operation op) {
    // TODO(natacha): there should be an easier way to
    // try and do this
    final AtomicBoolean addedOp = new AtomicBoolean(false);
    ConcurrentLinkedQueue<Operation> opQueue =
        readOpsToExecute.computeIfAbsent(op.getKey(), key -> {
          addedOp.set(true);
          return new ConcurrentLinkedQueue<Operation>();
        });
    opQueue.add(op);
    return addedOp.get();
  }

  /**
   * Determines whether an operation can be scheduled in this stride. If the stride is full or
   * already executing, then the operation will not be scheduled
   */
  public boolean scheduleRead(Operation op) {

    boolean success = false;
    boolean addedOp = false;

    if (rLock.tryLock() == false) {
      // Fail to acquire read lock, means that
      // stride is about to be executed
    } else {
      if (finishing == false) {

        assert (op.getStatement().getOpType() == Type.READ
                || op.getStatement().getOpType() == Type.READ_FOR_UPDATE);

          if (proxy.getConfig().ALLOW_DUPLICATES) {
            // Only used for benchmarking
            addOpLock.lock();
            if (readOpsToExecuteCount.get() >= proxy.getConfig().STRIDE_SIZE) {
              success = false;
            } else {
              addOperation(op);
              readOpsToExecuteCount.incrementAndGet();
              keysToSend.add(op.getKey());
              success = true;
            }
            addOpLock.unlock();
          } else {
            // Add the operation to the set of operations that
            // should be executed
            if (willAddRead(op)) {
              // If this operation *might* create a new read,
              // need to acquire an exclusive lock to make sure that
              // no concurrent operation also adds a new read (thereby
              // increasing the size of the stride to > Stride_SIZE)
              addOpLock.lock();
              if (readOpsToExecuteCount.get() >= proxy.getConfig().STRIDE_SIZE) {
                success = false;
              } else {
                addedOp = addOperation(op);
                if (addedOp) {
                  readOpsToExecuteCount.incrementAndGet();
                  keysToSend.add(op.getKey());
                }
                success = true;
              }
              addOpLock.unlock();
            } else {
              addedOp = addOperation(op);
              assert (addedOp == false);
              success = true;
            }
            if (proxy.getConfig().LOG_DUPLICATES_SAVED &&!addedOp) {
              proxy.getConfig().statsDuplicatesSaved
                  .addPoint(batch.getDataHandler().duplicateReads.incrementAndGet());
            }
          }
      } else {
        // This stride has already finished, try another
        // stride
      }
      rLock.unlock();
    }
    assert (readOpsToExecuteCount.get() <= proxy.getConfig().STRIDE_SIZE);

    if (success) {
      operationsInStride.add(op);
    }

    return success;
  }

  /**
   * Upon received this callback, the operation has been executed by the datastore. If it is a read,
   * the op value should contain a non-null readValue byte array (assuming the key existed)
   */
  public void onDataStoreOpsExecuted(List<Long> keys,
      List<byte[]> values) {

    assert (keys.size() == values.size());

    Iterator<byte[]> it = values.iterator();

    for (Long key : keys) {
      ConcurrentLinkedQueue<Operation> pendingOps = readOpsToExecute.get(key);
      final byte[] currentValue = it.next();
      if (currentValue != null) {
        pendingOps.stream().forEach(op -> op.setReadValue(currentValue));
      }

    }
    // next notify batch that stride has finished executing,
    batch.onStrideExecuted(this);
  }

  /**
   * Executes all the operations in a batch, ensuring that only one operation is submitted to the
   * datastore per batch (to guarantee correctness)
   */
  public void executeReads() {

    wLock.lock();
    finishing = true;

    System.out.println("[Stride] Number of Operations " + readOpsToExecuteCount.get());
    assert (readOpsToExecuteCount.get() <= proxy.getConfig().STRIDE_SIZE);
    assert (keysToSend.size() == readOpsToExecuteCount.get());

    if (readOpsToExecute.size() > 0) {
      // Execute batch of requests
      dataStore.read(keysToSend, new BatchedAsyncDataRequest(keysToSend));
    } else {
      // This stride was empty. Return immediately
      proxy.executeAsync(() -> batch.onStrideExecuted(this),
          proxy.getDefaultExpHandler());
    }

    wLock.unlock();
  }

  public Queue<Operation> getOperationsInStride() {
    return operationsInStride;
  }

  private class BatchedAsyncDataRequest extends AsyncDataRequest {

    private List<Long> keys;

    BatchedAsyncDataRequest(List<Long> keys) {
      this.keys = keys;
    }

    @Override
    public void onDataRequestCompleted() {
      onDataStoreOpsExecuted(keys, readValues);
    }
  }


}
