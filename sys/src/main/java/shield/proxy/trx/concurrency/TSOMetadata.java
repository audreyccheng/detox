package shield.proxy.trx.concurrency;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import org.mapdb.Atomic;
import shield.proxy.Proxy;
import shield.proxy.trx.concurrency.TransactionManager.CCManagerType;
import shield.proxy.trx.data.DataHandler;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Pair;

/**
 * Version store for TSO
 *
 * @author ncrooks
 */
public class TSOMetadata {

  private static HashFunction hf = Hashing.goodFastHash(64);

  /**
   * Data-structure contains the metadata that is necessary to run TSO. It does not contain any of
   * the cached data
   */
  private final ConcurrentHashMap<Long, VersionChain> versionInfo;

  /**
   * Data-structure contains only the cached data. Cached data is inserted with key hash(
   */
  private final ConcurrentMap<Long, byte[]> dataCache;

  /**
   * Reference to the transaction manager
   */
  private final TSOTransactionManager trxManager;

  /**
   * Reference to the data handler responsible for executing requests
   */
  private final DataHandler handler;

  private AtomicLong cacheHits = new AtomicLong();



  public TSOMetadata(TSOTransactionManager trxManager, DataHandler handler,
      Proxy proxy) {
    this.trxManager = trxManager;
    this.versionInfo = new ConcurrentHashMap<Long, VersionChain>(
        trxManager.getConfig().STRIDE_SIZE, 0.9f,
        trxManager.getConfig().N_WORKER_THREADS);
    this.dataCache = new ConcurrentHashMap<Long, byte[]>();
    this.handler = handler;
    this.cacheHits = new AtomicLong();
  }

  private long hash(Long key, long versionId) {
    // TODO(natacha): check if making copy of array can be removed
    HashCode hc =
        hf.newHasher().putLong(key).putLong(versionId).hash();
    return hc.asLong();
  }

  /**
   * Returns the data if it is currently stored at the proxy and null otherwise
   */
  private byte[] getCachedData(Long key, long versionId) {
    long hash = hash(key, versionId);
    byte[] retValue = dataCache.get(hash);
    return retValue;
  }

  /**
   * Adds data to the cache. NB: as we only allow transactions to write once, the data should never
   * already be present in the cache (key,id) pairs should be unique. Hence, if we find data that is
   * already in, it must be a hash collision
   */
  private void addDataToCache(Long key, long versionId, byte[] data) {
    long hash = hash(key, versionId);
    byte[] oldData = dataCache.put(hash, data);
    if (oldData != null) {
      // TODO(natacha) handle properly
      throw new RuntimeException("Collision " + key + " " + " " + hash);
    }
  }

  void removeDataFromCache(Long key, long versionId) {
   long hash = hash(key, versionId);
    byte[] removed = dataCache.remove(hash);
  }

  public void doRead(Long key, Operation op) {

    Version readVersion;

    // Find version chain or create a new one if none exist

    VersionChain chain =
        versionInfo.computeIfAbsent(key, k -> new VersionChain());

    // Identify the correct version to read. Also updates
    // the dependency list
    boolean isDummy = chain.read(op, false);

    completeRead(key, op, isDummy);
  }


  /**
   * Callback method for operations
   */
  public void onOperationExecuted(Operation op, boolean servedFromCache) {
    byte[] data = op.getReadValue();
    if (trxManager.getConfig().CC_MANAGER_TYPE == CCManagerType.BATCH &&
        !trxManager.getConfig().ALLOW_DUPLICATES &&
        !servedFromCache &&
        data != null &&
        op.wasSuccessful() &&
        !op.isWrite()
        ) {
      assert(false);
      // Add Data Cache with timestamp 0, as this must be the first version of the timestamp
      addDataToCache(op.getKey(),0, data);
    }
    trxManager.onOperationExecuted(op);
  }

  public void doWrite(Long key, Operation op) {
    SortedSet<Operation> blockedOperations;
    // Add data to the cache first to ensure that, if any
    // operation reads the data, it will always find it in the cache
    addDataToCache(key, op.getTrx().getTimestamp(), op.getWriteValue());
    VersionChain chain =
        versionInfo.computeIfAbsent(key, k -> new VersionChain());
    // Write operations never block, so no need to implement it as callbacks
    chain.write(op);
    if (op.wasSuccessful()) {
      blockedOperations = op.getVersion().getBlockedOperations();
      for (Operation blockedOp: blockedOperations) {
          blockedOp.setVersion(op.getVersion());
          System.out.println("Unblocking " + blockedOp);
          trxManager.getProxy()
              .executeAsync(() -> completeRead(key, blockedOp, false),
                    trxManager.getProxy().getDefaultExpHandler());
      }
      handler.handleRequest(op);
    } else {
      System.out.println("Failed to schedule op conflict " + "Trx " + op.getTrx().getTimestamp() + " " + op.getKey());
      if (trxManager.getConfig().LOG_ABORT_TYPES) {
        trxManager.getConfig().statsTSOAborts.addPoint(trxManager.aborts++);
      }
      assert(op.getVersion().getBlockedOperations().size() == 0);
      removeDataFromCache(key, op.getTrx().getTimestamp());
      trxManager.onOperationExecuted(op);
    }
  }

  public void completeRead(Long key, Operation op, boolean isDummy) {
    Version readVersion = op.getVersion();
    if (!isDummy) {
      switch (readVersion.getType()) {
        case NORMAL:
          byte[] readData = getCachedData(key, op.getVersion().getVersionId());

          if (readData != null) {
            // System.err.println("Version found " + key);
            if (trxManager.getConfig().LOG_CACHED_SAVED) {
              cacheHits.incrementAndGet();
              trxManager.getConfig().statsCachedSaved.addPoint(cacheHits);
            }
            op.setReadValue(readData);
            onOperationExecuted(op, true);
          } else {
            System.err.println("Version not found " + key + " " + op.getVersion().getVersionId());
            // System.exit(-1);
            // Need to access main datastore
            //handler.handleRequest(op);
            onOperationExecuted(op, true);
          }
          break;
        case TOMBSTONE:
          onOperationExecuted(op, true);
          break;
        default:
          assert(false);
          break;
      }
    }  else {
        System.out.println("Read a dummy Blocked " + op.getKey()+ " " + op + " by " + op.getVersion().getVersionId());
     /*   if (!op.wasSuccessful()) {
          System.out.println("Terminating failed dummies " + op.getKey());
          onOperationExecuted(op,true);
        } */
    }
  }


  public void doReadForUpdate(Long key, Operation op) {
    //System.out.println("[ReadFor] doReadForUpdate");
   VersionChain chain =
        versionInfo.computeIfAbsent(key, k -> new VersionChain());
   boolean dummy = chain.read(op, true);
   if (op.wasSuccessful()) {
      completeRead(key, op, dummy);
   } else {
     System.out.println("[ReadFor] Read for update failed " + op);
     // NB: This only arises in the case where
     trxManager.onOperationExecuted(op);
   }
}

  public void doDelete(Long key, Operation op) {

    // Find version chain or create a new one if none exist

    VersionChain chain =
        versionInfo.computeIfAbsent(key, k -> new VersionChain());

    chain.delete(op);
    if (op.wasSuccessful()) {
      handler.handleRequest(op);
    } else {
      trxManager.onOperationExecuted(op);
    }
  }

  public void doDummyWrite(Long key, Operation op) {
    throw new RuntimeException("Unimplemented");
  }

  /**
   * Undoes an operation: if it is a read, removes read from set of transactions that observed this
   * version. if it is a write, removes version
   *
   * @param op - the operation to undo
   */
  public void cleanupOnAbort(Operation op) {
    VersionChain chain = versionInfo.get(op.getKey());
    SortedSet<Operation> blockedOps = chain.undoOperation(op);
    System.out.println("Cleaning up " + op.getTrx().getTimestamp() + " " + op.getKey() + " " + blockedOps);
      for (Operation blockedOp:  blockedOps) {
        System.out.println("Terminating blocked operations " + op.getKey());
        blockedOp.markError();
        trxManager.getProxy()
                .executeAsync(() -> onOperationExecuted(blockedOp,false), trxManager.getProxy().getDefaultExpHandler());
      }
  }


  /**
   * Removes all versions that have a smaller timestamp than this as they will no longer be read
   */
  public void garbageCollect(int i) {
    int totalNumberVersions = 0;
    int count = 0;
    // TODO(natacha): java 8 options might be more efficient here
    for (Entry<Long, VersionChain> chain : versionInfo.entrySet()) {
      if (count++ % trxManager.getConfig().GC_THREADS == i) {
        // Remove logical versions
        int versions = chain.getValue()
            .truncateChain(chain.getKey(), trxManager.getLowestCommittedTransaction(), this);
        totalNumberVersions += versions;
      }
    }
    trxManager.getProxy().executeAsync(() -> garbageCollect(i), trxManager.getProxy().getDefaultExpHandler());
  }


  /**
   * Returns the latest versions of every object (iterates over the store sequentially)
   */
  public Queue<Write> getLatestVersions(ConcurrentSkipListSet<Long> keys) {

    ConcurrentLinkedQueue<Write> latestVersions =
        new ConcurrentLinkedQueue<>();

    if (!trxManager.getConfig().ALLOW_DUPLICATES) {
      //versionInfo.forEach((key, v) -> {
      for (Long key: keys) {
        VersionChain v = versionInfo.get(key);
        Version head = v.getLast();
        byte[] data;
        if (head.getVersionId() > 0) {
          if (!head.isTombstone()) {
            data = getCachedData(key, head.getVersionId());
            latestVersions.add(new Write(key, data.clone(), Type.WRITE));
          } else {
            latestVersions.add(new Write(key, null, Type.DELETE));
          }
          if (trxManager.getConfig().LOG_WRITES_SAVED) {
            trxManager.getConfig().statsWritesSaved.addPoint(v.getVersionCount());
          }
        }
        // });
      }
    } else {
      for (Long key: keys) {
          VersionChain v = versionInfo.get(key);
          Version head = v.getLast();
          byte[] data;
          while (head.getVersionId() > 0) {
            if (!head.isTombstone()) {
              data = getCachedData(key, head.getVersionId());
              latestVersions.add(new Write(key, data, Type.WRITE));
            } else {
              latestVersions.add(new Write(key, null, Type.DELETE));
            }
            head = head.getPrevious();
          }
      }
    }

    return latestVersions;
  }


  /**
   * Clears all metadata (used during batch change only).
   * Assumes that appropriate locks are owned.
   */
  public void clear() {
    versionInfo.clear();
    dataCache.clear();
  }

  /**
   * Starts transaction
   *
   * @param t
   * @return
   */
  public boolean startTransaction(Transaction t) {
    return handler.startTransaction(t);
  }


}
