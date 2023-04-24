package shield.proxy.oram;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import shield.config.NodeConfiguration;
import shield.network.messages.Msg;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.oram.recover.ReadPath;
import shield.proxy.trx.data.Write;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRingOram extends BaseRingOram implements IAsyncBackingStore, IBlockReadListener {


  private static final byte[] nullValue = new byte[0];

  private final Map<Block, ReadRequest> blocksToRequests;

  private ReadRequest currentReadRequest;
  private ForkJoinPool pool;
  /**
   * Async request associated with the ongoing batch
   */
  private AsyncDataRequest currentBatchedRequest = null;
  /**
   * Atomic Lock acquired during batch changes
   */
  private ReentrantLock taskTerminationLock;

  /**
   * Number of operations that have been processed in this batch;
   */
  private AtomicInteger currentProcessedTasks;
  /**
   * Number of tasks that *must* be processed in this batch
   */
  private int nbTasksInBatch;

  /**
   * Marked to true when all the metadata for the thread has been
   * procssed. At that point, @nbTasksInBatch represent the total
   * number of tasks that will ever be executed for this batch
   */
  private boolean metadataForBatchReady;

  private AsyncRingOram(Random rng, IAsyncBackingStore store, ForkJoinPool pool, NodeConfiguration config) {
    super(rng, new AsyncBlockProcessor(store, rng, pool), store, config);
    getBlockProcessor().addBlockReadListener(this);
    getBlockProcessor().setRingOram(this);
    this.pool = getBlockProcessor().getPool();
    this.taskTerminationLock = new ReentrantLock();
    this.blocksToRequests = new ConcurrentHashMap<>();
    this.currentProcessedTasks = new AtomicInteger();
    this.metadataForBatchReady = false;
    this.nbTasksInBatch = 0;
  }

  private AsyncRingOram(int L, Random rng, IAsyncBackingStore store, ForkJoinPool pool, NodeConfiguration config) {
    super(config, L, rng, new AsyncBlockProcessor(store, rng, pool), store);
    getBlockProcessor().addBlockReadListener(this);
    getBlockProcessor().setRingOram(this);
    this.pool = pool;
    this.pool = getBlockProcessor().getPool();
    this.taskTerminationLock = new ReentrantLock();
    this.blocksToRequests = new ConcurrentHashMap<>();
    this.currentProcessedTasks = new AtomicInteger();
    this.metadataForBatchReady = false;
    this.nbTasksInBatch = 0;
  }

  private AsyncRingOram(Random rng, IAsyncBackingStore store, int numThreads, NodeConfiguration config) {
    super(rng, new AsyncBlockProcessor(store, rng, new ForkJoinPool(numThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, exceptionHandler(), true)), store, config);
    getBlockProcessor().addBlockReadListener(this);
    getBlockProcessor().setRingOram(this);
    this.pool = getBlockProcessor().getPool();
    this.taskTerminationLock = new ReentrantLock();
    this.blocksToRequests = new ConcurrentHashMap<>();
    this.currentProcessedTasks = new AtomicInteger();
    this.metadataForBatchReady = false;
    this.nbTasksInBatch = 0;
  }

  private AsyncRingOram(int L, Random rng, IAsyncBackingStore store, int numThreads, NodeConfiguration config) {
    super(config, L, rng, new AsyncBlockProcessor(store, rng, new ForkJoinPool(numThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, exceptionHandler(), true)), store);
    getBlockProcessor().setRingOram(this);
    getBlockProcessor().addBlockReadListener(this);
    this.pool = getBlockProcessor().getPool();
    this.taskTerminationLock = new ReentrantLock();
    this.blocksToRequests = new ConcurrentHashMap<>();
    this.currentProcessedTasks = new AtomicInteger();
    this.metadataForBatchReady = false;
    this.nbTasksInBatch = 0;
  }

  public static AsyncRingOram create(Random rng, IAsyncBackingStore store, ForkJoinPool pool, NodeConfiguration config) {
    AsyncRingOram oram = new AsyncRingOram(rng, store, pool, config);
    oram.init();
    return oram;
  }

  public static AsyncRingOram create(int L, Random rng, IAsyncBackingStore store, ForkJoinPool pool, NodeConfiguration config) {
    AsyncRingOram oram = new AsyncRingOram(L, rng, store, pool, config);
    oram.init();
    return oram;
  }

  public static AsyncRingOram create(Random rng, IAsyncBackingStore store, int numThreads, NodeConfiguration config) {
    AsyncRingOram oram = new AsyncRingOram(rng, store, numThreads, config);
    oram.init();
    return oram;
  }

  public static AsyncRingOram create(int L, Random rng, IAsyncBackingStore store, int numThreads, NodeConfiguration config) {
    AsyncRingOram oram = new AsyncRingOram(L, rng, store, numThreads, config);
    oram.init();
    return oram;
  }

  public static Thread.UncaughtExceptionHandler exceptionHandler() {
    return (t, e) -> {
      System.err.printf("Uncaught exception in Thread %s\n", t.toString());
      e.printStackTrace(System.err);
      System.exit(1);
    };
  }

  public void shutdown() {
    if (pool != null) {
      pool.shutdown();
    }
  }

  @Override
  public void read(Long key, AsyncDataRequest req) {
    read(Arrays.asList(key), req);
  }

  @Override
  public void read(List<Long> keys, AsyncDataRequest req) {
    currentReadRequest = new ReadRequest(keys, req);
    currentBatchedRequest = req;
    doReadBatch(keys, null);
    // Mark Metadata Thread as finished
    taskTerminationLock.lock();
    metadataForBatchReady = true;
    int tasks = currentProcessedTasks.get();
    checkFinishedBatch(tasks);
    taskTerminationLock.unlock();
  }

  @Override
  protected void doRecoveryReadBatch(List<ReadPath> readPaths) throws InterruptedException {
    Object cond = new Object();
    AsyncDataRequest req = new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        super.onDataRequestCompleted();
        synchronized (cond) {
          cond.notifyAll();
        }
      }
    };
    currentBatchedRequest = req;
    for (ReadPath readPath : readPaths) {
      if (readPath.isDummy()) {
        access(dummyKey, null, OpType.DUMMY_READ, true, readPath.getOldPath(), false);
      } else {
        access(readPath.getKey(), null, OpType.READ, true, readPath.getOldPath(), false);
      }

    }
    for (int i = readPaths.size(); i < config.STRIDE_SIZE; ++i) {
      access(dummyKey, null, OpType.DUMMY_READ, false, 0, true);
    }
    taskTerminationLock.lock();
    metadataForBatchReady = true;
    int tasks = currentProcessedTasks.get();
    checkFinishedBatch(tasks);
    taskTerminationLock.unlock();
    while (!req.isDone()) {
      synchronized (cond) {
        cond.wait();
      }
    }
  }

  protected void doRecoveryWriteBatch() throws InterruptedException {
    Object cond = new Object();
    AsyncDataRequest req = new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        super.onDataRequestCompleted();
        synchronized (cond) {
          cond.notifyAll();
        }
      }
    };
    currentBatchedRequest = req;
    for (int i = 0; i < config.WRITES_SIZE; ++i) {
      access(dummyKey, null, OpType.DUMMY_WRITE, false, 0, true);
    }
    taskTerminationLock.lock();
    metadataForBatchReady = true;
    int tasks = currentProcessedTasks.get();
    checkFinishedBatch(tasks);
    taskTerminationLock.unlock();
    while (!req.isDone()) {
      synchronized (cond) {
        cond.wait();
      }
    }
  }


  @Override
  public void write(Write write, AsyncDataRequest req) {
    write(new ArrayDeque<>(Arrays.asList(write)), req);
  }

  @Override
  public void write(Queue<Write> writes, AsyncDataRequest req) {
    assert(getBufferOps()==false);
    currentReadRequest = null;
    currentBatchedRequest = req;
    doWriteBatch(writes);

    // Mark Metadata Thread as finished
    taskTerminationLock.lock();
    metadataForBatchReady = true;
    int tasks = currentProcessedTasks.get();
    checkFinishedBatch(tasks);
    taskTerminationLock.unlock();
  }

  @Override
  public void onHandleRequestResponse(Msg.DataMessageResp msgResp) {
    getBlockProcessor().handleRequestResponse(msgResp);
  }


  @Override
  public void onBlockRead(Block b) {
    if (!b.isDummy) {
      // this block may have been read for an evict path or early reshuffle
      // in the case of reading a real block from every bucket, it also could have been
      ReadRequest request = blocksToRequests.get(b);
      if (request != null) {
        request.addReadValue(b.getKey(), b.getValue(), b);
      }
    }
  }

  @Override
  protected void onReadPathComplete(Long key, byte[] value, OpType opType, Integer oldPos, Block b,
                                    boolean wasInTree, boolean wasCached) {
    if (opType == OpType.READ) {
      assert !isDurable() || !recovered || currentReadRequest != null;
      if (!isDurable() ||recovered) {
        if (b == null) {
          currentReadRequest.addReadValue(key, null, null);
        } else if (!wasInTree || wasCached) { // block was in stash
          // this is safe because we are calling b.getValue() before blockProcessor.writeBlock is called on b
          synchronized (b) {
            if (b.getValue() == null) {
              blocksToRequests.put(b, currentReadRequest);
            } else {
              currentReadRequest.addReadValue(key, b.getValue(), null);
            }
          }
        }
      }
    }
  }

  @Override
  protected void beforeReadRealBlock(Block b, Long key, byte[] value, OpType opType) {
    super.beforeReadRealBlock(b, key, value, opType);
    if (!isDurable() || recovered) {
      if (opType == OpType.READ && !b.isDummy && !b.isCached() && b.getKey().equals(key)) { // java is the worst
        blocksToRequests.put(b, currentReadRequest);
      }
    }
  }

  @Override
  protected AsyncBlockProcessor getBlockProcessor() {
    return (AsyncBlockProcessor) super.getBlockProcessor();
  }


  private void checkFinishedBatch(int processedTasks) {
   AsyncDataRequest finishedReq;

   if (metadataForBatchReady) {
      // this means that the metadata thread has finished executing
      if (processedTasks == nbTasksInBatch) {
       // all pending requests have finished executing

        finishedReq = currentBatchedRequest;

        final boolean wasWriteBatch = (currentReadRequest == null);
        System.out.println("[BatchFinishedExecuting] " + nbTasksInBatch);

        // Reinitialise data
        currentReadRequest = null;
        currentBatchedRequest = null;
        metadataForBatchReady = false;
        currentProcessedTasks.set(0);
        nbTasksInBatch = 0;

        CompletableFuture.runAsync(() -> {
          // check if this batch was the end-of-epoch write batch
          if (wasWriteBatch && isDurable()) {
            onEpochFinished(() -> {
              if (config.LOG_DURABILITY_WRITE_TIME) {
                config.statsDurabilityWriteTime.addEnd(durabilityWriteTimeKey);
              }
              finishedReq.onDataRequestCompleted();
            });
          } else {
            if (config.LOG_STORAGE_ACCESSES_PER_BATCH) {
              int storageWritesCount = storageWrites.getAndSet(0);
              int storageReadsCount= storageReads.getAndSet(0);
              String logString = String.format("  Reads=%d\n    ReadPath=%d\n    EarlyReshuffle=%d\n    EvictPath=%d\n  Writes=%d\n    EarlyReshuffle=%d\n    EvictPath=%d\n",
                  storageReadsCount, storageReadsReadPath.getAndSet(0), storageReadsEarlyReshuffle.getAndSet(0), storageReadsEvictPath.getAndSet(0),
                  storageWritesCount, storageWritesEarlyReshuffle.getAndSet(0), storageWritesEvictPath.getAndSet(0));
              if (wasWriteBatch) {
                System.out.printf("STORAGE ACCESS PER BATCH WRITE:\n%s", logString);
                config.statsStorageWritesPerBatch.addPoint(storageReadsCount + " " + storageWritesCount);
              } else {
                System.out.printf("STORAGE ACCESS PER BATCH READ:\n%s", logString);
                config.statsStorageReadsPerBatch.addPoint(storageReadsCount + " " + storageWritesCount);
              }
            }
            finishedReq.onDataRequestCompleted();
          }
        }, pool);
      }
    } else {
      // Not done yet
    }
  }
  /**
   * Method is called when a task in AsyncBlockProcessor finishes.
   * Checks whether all the write tasks for this batch have finished
   */
  public void finishedTask() {
    taskTerminationLock.lock();
    // Increment number of tasks that have finished processing
    int tasks = currentProcessedTasks.incrementAndGet();
    checkFinishedBatch(tasks);
    taskTerminationLock.unlock();
  }
  /**
   * Increments the number of IO tasks that this
   * batch creates
   */
  public void addTask() {
    // taskTerminationLock.lock();
    nbTasksInBatch++;
    // taskTerminationLock.unlock();
  }

  enum OpType {
    READ,
    WRITE,
    DUMMY_READ, DUMMY_WRITE, DELETE
  }

  private class ReadRequest {

    private final List<Long> keys;

    private final AsyncDataRequest req;

    private final Map<Long, byte[]> readValues;

    private final AtomicInteger count;

    ReadRequest(Long key, AsyncDataRequest req) {
      this(Arrays.asList(key), req);
    }

    ReadRequest(List<Long> keys, AsyncDataRequest req) {
      this.keys = new ArrayList<>(keys);
      this.req = req;
      this.readValues = new ConcurrentHashMap<>(keys.size());
      this.count = new AtomicInteger(0);
    }

    void addReadValue(Long key, byte[] value, Block fromBlock) {
      readValues.put(key, (value == null) ? nullValue : value.clone());
      if (fromBlock != null) {
        blocksToRequests.remove(fromBlock);
      }
      if (count.incrementAndGet() == keys.size()) {
        List<byte[]> retValues = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); ++i) {
          byte[] val = readValues.get(keys.get(i));
          assert val != null;
          retValues.add(val == nullValue ? null : val);
        }
        req.setReadValues(retValues);
       // req.onDataRequestCompleted();
      }
    }

  }

}
