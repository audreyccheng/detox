package shield.proxy.oram;

import shield.config.NodeConfiguration;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.oram.AsyncRingOram.OpType;
import shield.proxy.oram.enc.MaskAlgorithm;
import shield.proxy.oram.enc.MaskAlgorithmFactory;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.proxy.oram.recover.*;
import shield.proxy.trx.data.Write;
import shield.util.Logging;
import shield.util.Pair;
import shield.util.Utility;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * RingORAM implementation.
 *
 * @author soumya, matt
 */
public class BaseRingOram {

  /**
   * The length of a partial durability key in bytes.
   */
  private static final int PARTIAL_DURABILITY_KEY_LENGTH = Integer.BYTES + Integer.BYTES + Integer.BYTES;

  /**
   * The length of a durability key in bytes.
   */
  private static final int DURABILITY_KEY_LENGTH = Integer.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES;

  /**
   * Special logical key used for dummy blocks.
   */
  static final Long dummyKey = 0L;

  /**
   * The NodeConfiguration class contains various parameters that configure the behavior of the ORAM.
   */
  protected NodeConfiguration config;

  /**
   * Used in various equations in the Ring ORAM paper. For a tree of height h, L = h - 1.
   */
  private final int L;

  /**
   * The random number generator for the ORAM.
   */
  private final Random rng;

  /**
   * The block processor through which this ORAM will read/decrypt and encrypt/write blocks.
   */
  private final IBlockProcessor blockProcessor;

  /**
   * The maximum number of real blocks that the ORAM will store.
   */
  private final int N;

  /**
   * The number of real blocks in the bucket.
   */
  private int Z;
  /**
   * The number of dummy blocks in the bucket.
   */
  private int S;
  /**
   * How often we are running evictPath.
   */
  private int A;

  /**
   * The height of, or number of levels in, the ORAM tree. For a tree with parameter L, h = L + 1
   */
  private int numLevels;

  /**
   * The number of leaves in the ORAM tree.
   */
  private int numLeaves;

  /**
   * All of the buckets in this ORAM.
   */
  private final Bucket[][] buckets;

  /**
   * Total number of physical blocks used by the ORAM. Calculated as (numberOfBuckets)*(Z+S).
   */
  private int totalServerBlocks;

  /**
   * The access number we are on (always less than A)
   */
  private int round;

  /**
   * The number of individual operations in each ORAM batch. An ORAM batch consists
   * of a fixed number of read stride batches followed by a write batch.
   */
  private final int totalBatchOps;

  /**
   * The client side stash.
   */
  private Stash stash;

  /**
   * The position map for this ORAM, from key to the random leaf the key is assigned to.
   */
  PositionMap positionMap;

  private final BlockLocInfo blockLocInfo;

  private final Map<Long, Block> delayedWrites;

  private final byte[] clientKey;
  private final ThreadLocal<MaskAlgorithm> maskAlgorithm;
  private final ThreadLocal<byte[]> maskByteArray;

  private final byte[] dummyValue;
  private Integer[] intObjectCache;
  private final List<Block> evictBlocks;

  private int batchOpCount;
  private int failAfterBatchOps;
  private boolean remapOnStashAdd;
  private boolean readPathAllReal;

  private int evictPathCount;
  ValidMap validMap;
  StaleMap staleMap;
  PermutationMap permutationMap;
  private EarlyReshuffleMap earlyReshuffleMap;
  final ThreadLocal<RecoveryEncrypter> recoveryEncrypter;
  private WriteAheadLog wal;
  private final byte[] computePartialKeyBytes;
  private final byte[] computeKeyBytes;
  private List<Integer> indices;
  private LogEntry prevLogEntry;

  /**
   * Stat Collection
   */
  private boolean recordStashSize;
  private boolean recordEarlyReshuffles;
  private final Queue<Pair<Long, Long>> statsStashSize;
  private final LinkedList<Pair<Integer, Integer>> statsEarlyReshuffleLocation;
  private long statsStashSizeRecordFrequency;
  private long statsStashSizeAccessCounter;
  private int statsEarlyReshuffleCount;
  AtomicInteger storageReads;
  AtomicInteger storageReadsEvictPath;
  AtomicInteger storageReadsEarlyReshuffle;
  AtomicInteger storageReadsReadPath;
  AtomicInteger storageWrites;
  AtomicInteger storageWritesEvictPath;
  AtomicInteger storageWritesEarlyReshuffle;

  int durabilityWriteTimeKey;

  /**
   * Constructs a new BaseRingOram instance.
   * @param config           The configuration for this ORAM.
   * @param rng              The random number generator for this ORAM.
   * @param blockProcessor   The block processor through which this ORAM will read/decrypt and
   *                         encrypt/write blocks.
   * @param backingStore     The backing store used for storing recovery information if durability is enabled.
   */
  public BaseRingOram(NodeConfiguration config, int L, Random rng, IBlockProcessor blockProcessor,
                      IAsyncBackingStore backingStore) {
    assert !config.ORAM_DURABLE || config.ORAM_WRITE_END_BATCH;
    assert !config.ORAM_DURABLE || (this instanceof AsyncRingOram);
    assert !config.ORAM_BUFFER_SYNCOPS || (this instanceof SyncRingOram);
    assert !config.ORAM_DURABLE || config.ORAM_NONCE_LEN == 8;
    this.config = config;
    this.L = L;
    this.rng = rng;
    this.blockProcessor = blockProcessor;

    /** Base ORAM functionality **/
    this.N = config.ORAM_MAX_BLOCKS;
    this.Z = config.ORAM_Z;
    this.S = config.ORAM_S;
    this.A = config.ORAM_A;
    this.numLevels = L + 1; // levels are numbered from 0 to L (inclusive)
    this.numLeaves = 1 << numLevels;
    this.buckets = new Bucket[numLevels][];
    this.totalServerBlocks = 0;
    this.round = 0;
    this.totalBatchOps = config.MAX_NB_STRIDE * config.STRIDE_SIZE + config.WRITES_SIZE;
    this.stash = new Stash(this, config.ORAM_DURABLE_MAX_STASH_SIZE);
    this.positionMap = new PositionMap(config.ORAM_MAX_BLOCKS, this.totalBatchOps);
    this.blockLocInfo = new BlockLocInfo();
    this.delayedWrites = new HashMap<>();

    /* Shared Encryption Objects */
    this.clientKey = config.CLIENT_KEY.clone();
    this.maskAlgorithm = ThreadLocal.withInitial(() -> {
      try {
        return MaskAlgorithmFactory.getAlgorithm(config.ORAM_MASK_ALGORITHM, config.CLIENT_KEY);
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        System.exit(-1);
      }
      return null;
    });
    this.maskByteArray = ThreadLocal.withInitial(() -> new byte[config.ORAM_VALUE_SIZE]);

    /* Shared Objects (for reducing memory usage */
    this.dummyValue = new byte[config.ORAM_VALUE_SIZE];
    this.intObjectCache = new Integer[config.ORAM_Z + config.ORAM_S];
    this.evictBlocks = new ArrayList<>();

    /* Testing State */
    this.batchOpCount = 0;
    this.failAfterBatchOps = -1;
    this.remapOnStashAdd = false;
    this.readPathAllReal = false;

    /* Recovery Objects */
    this.evictPathCount = 0;
    this.validMap = new ValidMap(numLevels, config.ORAM_Z + config.ORAM_S);
    this.staleMap = new StaleMap(numLevels, config.ORAM_Z + config.ORAM_S, config.WRITES_SIZE);
    this.permutationMap = new PermutationMap(this);
    this.earlyReshuffleMap = new EarlyReshuffleMap();
    this.recoveryEncrypter = ThreadLocal.withInitial(() -> new RecoveryEncrypter(this.clientKey));
    if (config.ORAM_DURABLE) {
      this.wal = new WriteAheadLog(backingStore, recoveryEncrypter, numLevels, totalBatchOps, config, ((AsyncBlockProcessor) blockProcessor).getPool());
    }
    this.computePartialKeyBytes = new byte[PARTIAL_DURABILITY_KEY_LENGTH];
    this.computeKeyBytes = new byte[DURABILITY_KEY_LENGTH];
    this.indices = new ArrayList<>();
    this.prevLogEntry = null;

    /* Stat Recording */
    this.recordStashSize = false;
    this.recordEarlyReshuffles = false;
    this.statsStashSize = new LinkedList<>();
    this.statsEarlyReshuffleLocation = new LinkedList<>();
    this.statsStashSizeRecordFrequency = config.ORAM_A * 10 - 1;
    this.statsStashSizeAccessCounter = 0;
    this.statsEarlyReshuffleCount = 0;
    this.storageReads = new AtomicInteger(0);
    this.storageWrites = new AtomicInteger(0);
    this.storageReadsEarlyReshuffle = new AtomicInteger(0);
    this.storageReadsEvictPath = new AtomicInteger(0);
    this.storageReadsReadPath = new AtomicInteger(0);
    this.storageWritesEarlyReshuffle = new AtomicInteger(0);
    this.storageWritesEvictPath = new AtomicInteger(0);

    for (int i = 0; i <= L; i++) {
      assert (Math.pow(2, i) < Integer.MAX_VALUE);
      int num_buckets = (int) Math.pow(2, i);
      totalServerBlocks += num_buckets * (config.ORAM_Z + config.ORAM_S);
      buckets[i] = new Bucket[num_buckets];
      for (int j = 0; j < num_buckets; j++) {
        buckets[i][j] = new Bucket(this, i, j);
      }
    }
    System.out.println("Number of levels " + numLevels);
    System.err.printf("N=%d,Z=%d,S=%d,A=%d,L=%d\n", config.ORAM_MAX_BLOCKS, config.ORAM_Z, config.ORAM_S, config.ORAM_A, L);
    System.err.printf("Total physical addresses: %d\n", totalServerBlocks);
    System.err.printf("%.3f%% of physical addresses are unused\n", ((double) (totalServerBlocks - config.ORAM_MAX_BLOCKS) / totalServerBlocks) * 100);
  }

  public BaseRingOram(Random rng, IBlockProcessor blockProcessor, IAsyncBackingStore backingStore,
                      NodeConfiguration config) {
    this(config, (int) Math.ceil(Math.log(2.0 * config.ORAM_MAX_BLOCKS / config.ORAM_A) / Math.log(2)), // from Ring ORAM paper section 5/lemma 3)
        rng, blockProcessor, backingStore
    );
  }

  static Long getDummyKey() {
    return dummyKey;
  }

  int getNonceLength() {
    return config.ORAM_NONCE_LEN;
  }

  public int getZ() {
    return this.Z;
  }

  public int getS() {
    return this.S;
  }

  public int getA() {
    return this.A;
  }

  int getValueSize() {
    return config.ORAM_VALUE_SIZE;
  }

  byte[] getDummyValue() {
    return dummyValue;
  }

  public byte[] getClientKey() {
    return clientKey;
  }

  public MaskAlgorithmType getMaskAlgorithmType() {
    return config.ORAM_MASK_ALGORITHM;
  }

  ThreadLocal<MaskAlgorithm> getMaskAlgorithm() {
    return maskAlgorithm;
  }

  ThreadLocal<byte[]> getMaskByteArray() {
    return maskByteArray;
  }

  boolean isEncryptingBlocks() {
    return config.ORAM_ENCRYPT_BLOCKS;
  }

  boolean isReadPathAllRealEnabled() {
    return readPathAllReal;
  }

  boolean isWriteEndBatchEnabled() {
    return config.ORAM_WRITE_END_BATCH;
  }

  public void enableRemapOnStashAdd() {
    remapOnStashAdd = true;
  }

  public void enableReadPathAllReal() {
    readPathAllReal = true;
  }

  public boolean isDurable() {
    return config.ORAM_DURABLE;
  }

  public long getStashSize() {
    return stash.getStashSize();
  }

  boolean getBufferOps() { return config.ORAM_BUFFER_SYNCOPS; }

  /**
   * Generalized read/write interface. Copied from the RingORAM paper.
   *
   * @param key    The key to access
   * @param value  The value to write to this key, null if isRead is true
   * @param opType The type of operation
   */
  public void access(Long key, byte[] value, OpType opType, boolean usePreDeterminedPath, Integer preDeterminedPath, boolean needsToLog) {
    int newPos = 0;
    Integer oldPos = null;
    if (opType == OpType.READ || opType == OpType.WRITE) {
      newPos = generatePath();
      oldPos = positionMap.put(key, newPos);
      assert !config.ORAM_DURABLE || positionMap.size() <= N;
   }

   if (usePreDeterminedPath) {
      oldPos = preDeterminedPath;
   }

    blockLocInfo.wasInTree = false;
    blockLocInfo.wasCached = false;
    if (opType == OpType.READ || opType == OpType.DUMMY_READ || !config.ORAM_WRITE_WITHOUT_READ) {
      if (oldPos == null) {
        // first access to this block
        oldPos = generatePath();
      }
      readPath(oldPos, newPos, key, value, opType, blockLocInfo, needsToLog);
    } else {
      if (opType == OpType.DELETE) {
        oldPos = positionMap.get(key);
      }
      if (oldPos != null) {
        staleifyBlockInTree(key, oldPos);
      }
    }

    if (opType != OpType.DUMMY_WRITE) {
      Block b = stash.getBlock(key);
      onReadPathComplete(key, value, opType, oldPos, b, blockLocInfo.wasInTree, blockLocInfo.wasCached);

      if (b == null) {
        // block is guaranteed to be found in the stash after read path unless it has not yet
        // been written. in that case, if this is the first write, we create a new real block
        // and put it in the stash
        if (opType == OpType.WRITE) {
          b = new Block(key, value, new BlockMask(config.ORAM_ENCRYPT_BLOCKS, config.ORAM_NONCE_LEN, getMaskByteArray(), getMaskAlgorithm(), clientKey), false, 0, isDurable(), 0L);
          stash.addBlock(b);
        }
      } else {
        if (opType == OpType.WRITE && !blockLocInfo.wasInTree) {
          synchronized (b) {
            b.setValue(value);
          }
        } else if (opType == OpType.DELETE) {
          stash.deleteBlock(key);
        }
      }
    }

    round = (round + 1) % A;
    if (round == 0) {
      evictPath();
    }

    if (opType == OpType.READ || opType == OpType.DUMMY_READ || (!config.ORAM_WRITE_WITHOUT_READ && oldPos != null)) {
      earlyReshuffle(oldPos);
    }

    if (config.ORAM_WRITE_END_BATCH) {
      batchOpCount = (batchOpCount + 1) % totalBatchOps;
      if (batchOpCount == 0) {
        doDelayedWrites();
      }
      if (failAfterBatchOps >= 0 && batchOpCount == failAfterBatchOps) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        throw new SimulatedFailureException(String.format("Failed after %d ops in batch.", failAfterBatchOps));
      }
    }

    /*
     * Stat Collection
     */
    if (recordStashSize) {
      statsStashSizeAccessCounter++;
      if (statsStashSizeAccessCounter % statsStashSizeRecordFrequency == 0) {
        statsStashSize.add(new Pair<>(statsStashSizeAccessCounter, getStashSize()));
      }
    }

  }

  protected void onReadPathComplete(Long key, byte[] value, OpType opType, Integer oldPos, Block b, boolean wasInTree, boolean wasCached) {
  }

  /**
   * Early reshuffle operation on the ORAM.
   *
   * @param path The path to do the early reshuffle operation on.
   */
  private void earlyReshuffle(int path) {
    for (int i = 0; i < numLevels; i++) {
      Bucket b = buckets[i][path >> (numLevels - i)];
      if (b.getNumAccessed() >= S) {
        b.readBucket(stash, rng, false);
        if (isDurable()) {
          earlyReshuffleMap.increment(i, path >> (numLevels - i));
        }
        b.writeBucket(stash, rng, positionMap, false);
      }
    }
  }

  /**
   * Evicts the path defined by G in this ORAM and increments G by 1.
   */
  private void evictPath() {
    assert numLevels <= Integer.SIZE;
    int path = Integer.reverse(evictPathCount);
    path >>>= Integer.SIZE - numLevels; // levels are numbered 0 through numLevels - 1

    for (int i = 0; i < numLevels; i++) {
      buckets[i][path >> (numLevels - i)].readBucket(stash, rng, true);
    }
    evictPathCount += 1;
    for (int i = numLevels - 1; i >= 0; i--) {
      buckets[i][path >> (numLevels - i)].writeBucket(stash, rng, positionMap, true);
      if (isDurable()) {
        earlyReshuffleMap.reset(i, path >> (numLevels - i));
      }
    }

  }

  /**
   * Reads the key from this path and puts the block into the stash.
   *
   * @param path The path to read from this ORAM
   * @param key  The key that we are accessing from this path.
   */
  private void readPath(int path, int newPath, final Long key, byte[] value, OpType opType, BlockLocInfo blockLocInfo, boolean needsToLog) {
    Bucket b;
    Integer blockOffset;

    ReadPath readPath = null;
    if (isDurable()) {
      readPath = new ReadPath(key, path, newPath, numLevels, opType == OpType.DUMMY_READ || opType == OpType.DUMMY_WRITE);
    }
    for (int i = 0; i < numLevels; i++) {
      b = buckets[i][path >> (numLevels - i)];
      blockOffset = b.getBlockOffset(key, rng);
      indices.add(blockOffset);
    }

    if (isDurable() && needsToLog) {
      readPath.addIndicesAccessed(indices);
      assert blockProcessor instanceof AsyncBlockProcessor;
      ((AsyncBlockProcessor) blockProcessor).writeLogEntry(readPath, wal.getKeyForNextReadPath(), prevLogEntry);
      prevLogEntry = readPath;
    }

    for (int i = 0; i < numLevels; ++i) {
      b = buckets[i][path >> (numLevels - i)];
      blockOffset = indices.get(i);
      if (blockOffset != -1) {
        // this will add key to stash if contained in this bucket
        b.readBlockAtOffset(stash, blockOffset, key, blockLocInfo, readPath, (Block blk) -> {
          beforeReadRealBlock(blk, key, value, opType);
          return null;
        });
      }
    }

    indices.clear();
  }

  private void staleifyBlockInTree(Long key, int path) {
    Bucket b;
    for (int i = 0; i < numLevels; i++) {
      b = buckets[i][path >> (numLevels - i)];
      if (b.staleifyBlock(key, stash)) {
        break;
      }
    }
  }

  private void doDelayedWrites() {
    for (Map.Entry<Long, Block> write : delayedWrites.entrySet()) {
      write.getValue().setPhysicalKey(write.getValue().getCurrPhysicalKey());
      blockProcessor.writeBlock(write.getKey(), write.getValue().getCurrPhysicalKey(), write.getValue());
      write.getValue().setCached(false);
    }
    delayedWrites.clear();
  }

  /**
   * BEGIN RECOVERY DATA AND PROCEDURES
   */

  private void recoverFromCrash() throws InterruptedException {
    Object cond = new Object();
    final Boolean[] init = {false};
    wal.initializeRecovery(this, () -> {
      synchronized (cond) {
        init[0] = true;
        cond.notifyAll();
      }
    });
    while (!init[0]) {
      synchronized (cond) {
        try {
          cond.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    System.err.printf("Finished reading recovery data.\n");

    if (wal.getLastCompletedEpoch() >= 0) {
      System.err.printf("Reinitializing ORAM with data recovered from epoch %d.\n", wal.getLastCompletedEpoch());
      positionMap = wal.getPositionMap();
      validMap = wal.getValidMap();
      staleMap = wal.getStaleMap();
      earlyReshuffleMap = wal.getEarlyReshuffleMap();
      permutationMap = wal.getPermutationMap();
      stash = wal.getStash();
      evictPathCount = wal.getEvictPathCount();
      for (int i = 0; i < buckets.length; ++i) {
        for (int j = 0; j < buckets[i].length; ++j) {
          buckets[i][j].recover();
        }
      }
      System.err.printf("Recovered from epoch %d.\n", wal.getLastCompletedEpoch());
      List<ReadPath> readPaths = wal.getLogicalAccesses();
      long start = System.nanoTime();
      int numStrides = (totalBatchOps - config.WRITES_SIZE) / config.STRIDE_SIZE;
      int i = 0;
      for (int j = 0; j < numStrides; ++j) {
        List<ReadPath> readBatch = new ArrayList<>(config.STRIDE_SIZE);
        while (readBatch.size() < config.STRIDE_SIZE && i < readPaths.size()) {
          readBatch.add(readPaths.get(i++));
        }
        doRecoveryReadBatch(readBatch);
      }
      doRecoveryWriteBatch();
      long end = System.nanoTime();
      System.err.printf("Replayed logged paths in %dms.\n", (long) ((end - start) / 1e6));
      System.err.printf("Clearing all data structures of changed entries.\n");
      positionMap.clearChanged();
      validMap.clearChanged();
      staleMap.clearChanged();
      earlyReshuffleMap.clearChanged();
      permutationMap.clearChanged();
    }
    System.err.printf("ORAM has recovered to a consistent state.\n");
    // we should now be in a consistent and secure state to start a NEW BATCH
  }

  private void logEpochFinished(Runnable onLogged) {
    Object cond = new Object();
    final AtomicInteger doneCount = new AtomicInteger();
    Function<String, Void> done = (str) -> {
      synchronized (cond) {
        if (config.LOG_DURABILITY_SERIALIZATION_TIME) {
          config.statsDurabilitySerializationTime.addPoint(wal.serializeTime.get());
        }
        if (config.LOG_DURABILITY_CLONE_TIME) {
          config.statsDurabilityCloneTime.addPoint(wal.cloneTime.get());
        }
        if (config.LOG_DURABILITY_MASK_TIME) {
          config.statsDurabilityMaskTime.addPoint(wal.maskTime.get());
        }
        if (config.LOG_DURABILITY_NETWORKING_TIME) {
          config.statsDurabilityNetworkingTime.addPoint(wal.networkTime.get());
        }
        int d = doneCount.incrementAndGet();
        System.err.printf("Logged recovered data %s (%d).\n", str, d);
        if (d == 7) {
          wal.writeEpochComplete(onLogged);
        }
      }
      return null;
    };
    if (config.LOG_DURABILITY_WRITE_TIME) {
      durabilityWriteTimeKey = config.statsDurabilityWriteTime.addBegin();
    }
    if (config.LOG_DURABILITY_SERIALIZATION_TIME) {
      wal.serializeTime.set(0);
    }
    if (config.LOG_DURABILITY_CLONE_TIME) {
      wal.cloneTime.set(0);
    }
    if (config.LOG_DURABILITY_MASK_TIME) {
      wal.maskTime.set(0);
    }
    if (config.LOG_DURABILITY_NETWORKING_TIME) {
      wal.networkTime.set(0);
    }

    wal.writePositionMap(positionMap, done);
    wal.writeValidMap(validMap, done);
    wal.writeStaleMap(staleMap, done);
    wal.writeEarlyReshuffleMap(earlyReshuffleMap, done);
    wal.writePermutationMap(permutationMap, done);
    wal.writeStash(stash, done);
    System.err.printf("Writing evict path count %d.\n", evictPathCount);
    wal.writeEvictedPathCount(evictPathCount, done);
  }

  Long computePartialPhysicalKeyForBucketIndex(int bucketLevel, int bucketIndex, int blockIndex) {
    putInt(bucketLevel, computePartialKeyBytes, 0);
    putInt(bucketIndex, computePartialKeyBytes, Integer.BYTES);
    putInt(blockIndex, computePartialKeyBytes, Integer.BYTES + Integer.BYTES);
    return Utility.hashPersistent(computePartialKeyBytes);
  }

  Long computePhysicalKeyForBucketIndex(int bucketLevel, int bucketIndex, int blockIndex) {
    putInt(bucketLevel, computeKeyBytes, 0);
    putInt(bucketIndex, computeKeyBytes, 4);
    putInt(blockIndex, computeKeyBytes, 8);
    int evictPathVersion = evictPathCount >> bucketLevel;
    if (evictPathCount % (1 << bucketLevel) > Integer.reverse(bucketIndex << (Integer.SIZE - bucketLevel))) {
      evictPathVersion += 1;
    }
    int earlyReshuffles = earlyReshuffleMap.getCount(bucketLevel, bucketIndex);
    int evictPathWrite = config.ORAM_WRITE_END_BATCH ? evictPathVersion % 3 : evictPathVersion;
    int earlyReshuffleWrite = config.ORAM_WRITE_END_BATCH ? earlyReshuffles % 3 : earlyReshuffles;
    putInt(evictPathWrite% 2, computeKeyBytes, 12); // evict path count / 2^bucketLevel
    putInt(earlyReshuffleWrite % 2, computeKeyBytes, 16);
    return Utility.hashPersistent(computeKeyBytes);
  }

  private static void putInt(int n, byte[] b, int off) {
    b[off] = (byte) ((n >> 24) & 0xFF);
    b[off + 1] = (byte) ((n >> 16) & 0xFF);
    b[off + 2] = (byte) ((n >> 8) & 0xFF);
    b[off + 3] = (byte) (n & 0xFF);
  }

  /**
   * END RECOVERY DATA AND PROCEDURES
   */

  protected boolean recovered = false;
  protected void init() {
    if (isDurable()) {
      try {
        int durabilityRecoverTimeKey = 0;
        if (config.LOG_DURABILITY_RECOVER_TIME) {
          durabilityRecoverTimeKey = config.statsDurabilityRecoverTime.addBegin();
        }
        recoverFromCrash();
        if (config.LOG_DURABILITY_RECOVER_TIME) {
          config.statsDurabilityRecoverTime.addEnd(durabilityRecoverTimeKey);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      recovered = true;
    }
  }

  protected void beforeReadRealBlock(Block blk, Long key, byte[] value, OpType opType) {
    if (opType == OpType.WRITE && !blk.isDummy && blk.getKey().equals(key)) {
      // the Block class will prevent a block processor from overwriting this new value
      synchronized (blk) {
        blk.setValue(value);
      }
    }
  }

  void onEpochFinished(Runnable onLogged) {
    logEpochFinished(onLogged);
  }

  void doReadBatch(List<Long> keys, Runnable afterAccess) {
    for (Long key : keys) {
      access(key, null, OpType.READ, false, 0, true);
      if (afterAccess != null) {
        afterAccess.run();
      }
    }
    if (config.ORAM_PAD_BATCHES) {
      for (int i = keys.size(); i < config.STRIDE_SIZE; ++i) {
        access(dummyKey, null, OpType.DUMMY_READ, false, 0, true);
      }
    }
  }

  protected void doRecoveryReadBatch(List<ReadPath> readPaths) throws InterruptedException {
  }

  protected void doRecoveryWriteBatch() throws InterruptedException {
  }

  void doWriteBatch(Queue<Write> writes) {
    for (Write write : writes) {
       access(write.getKey(), write.getValue(), write.isWrite() ? OpType.WRITE : OpType.DELETE, false, 0, true);
    }
    if (config.ORAM_PAD_BATCHES) {
      for (int i = writes.size(); i < config.WRITES_SIZE; ++i) {
        access(dummyKey, null, OpType.DUMMY_WRITE, false, 0, true);
      }
    }
   }

  void beforeAddStashReadBucket(Block b) {
    if (remapOnStashAdd) {
      positionMap.put(b.getKey(), generatePath());
    }
  }

  List<Block> getEvictBlocksList() {
    return evictBlocks;
  }

  private int generatePath() {
    return rng.nextInt(numLeaves);
  }

  public Integer getIntObject(int i) {
    if (i >= intObjectCache.length) {
      Integer[] biggerArray = new Integer[intObjectCache.length * 2];
      System.arraycopy(intObjectCache, 0, biggerArray, 0, intObjectCache.length);
      intObjectCache = biggerArray;
    }
    if (intObjectCache[i] == null) {
      intObjectCache[i] = Integer.valueOf(i);
    }
    return intObjectCache[i];
  }

  void addDelayedWrite(Long key, Block b) {
    delayedWrites.put(key, b);
  }

  void removeDelayedWrite(Long key) {
    delayedWrites.remove(key);
  }

  boolean hasDelayedWrite(Long key) {
    return delayedWrites.containsKey(key);
  }

  protected Logging getLogger() {
    return Logging.getLogger(Logging.Level.NONE);
  }

  protected IBlockProcessor getBlockProcessor() {
    return blockProcessor;
  }

  /**
   * Stat Collection
   */

  public void enableRecordEarlyReshuffles() {
    recordEarlyReshuffles = true;
  }

  public void enableRecordStashSize() {
    recordStashSize = true;
  }

  public int getEarlyReshuffleCount() {
    return statsEarlyReshuffleCount;
  }

  public LinkedList<Pair<Integer, Integer>> getEarlyReshuffleLocation() {
    return statsEarlyReshuffleLocation;
  }


  public Pair<Long, Long> getStashSizeRecording() {
    return statsStashSize.poll();
  }

  public int getTotalBucketsSkipped() {
    int total = 0;
    for (Bucket[] bucket : buckets) {
      for (Bucket aBucket : bucket) {
        total += aBucket.getNumSkipped();
      }
    }
    return total;
  }

  public void injectFailureAfterBatchOps(int ops) {
    failAfterBatchOps = ops;
  }

  int getNumLevels() {
    return numLevels;
  }
}
