package shield.proxy.oram.recover;

import com.google.common.primitives.Ints;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import shield.config.NodeConfiguration;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.oram.BaseRingOram;
import shield.proxy.oram.PositionMap;
import shield.proxy.oram.Stash;
import shield.proxy.trx.data.Write;
import shield.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class WriteAheadLog {

  private static final String LAST_COMPLETED_EPOCH_KEY = "last_completed_epoch";

  private NodeConfiguration config;
  private final IAsyncBackingStore store;
  private final int maxKeys;
  private final int treeLevels;
  private final int maxLogicalAccessesInBatch;

  private int lastCompletedEpoch;
  private int logicalAccessIndex;

  private List<ReadPath> logicalAccesses;
  private PositionMap positionMap;
  private ValidMap validMap;
  private StaleMap staleMap;
  private EarlyReshuffleMap earlyReshuffleMap;
  private PermutationMap permutationMap;
  private Stash stash;
  private final int bucketSlots;
  private final int maxPositionChanged;
  private int evictPathCount;

  private final int positionMapCheckpoint;
  private final int validMapCheckpoint;
  private final int staleMapCheckpoint;
  private final int permutationMapCheckpoint;
  private final int earlyReshuffleMapCheckpoint;
  private final int checkpointFrequency;
  private final int strideSize;
  private final int maxStaleChanged;
  private final int maxStashSize;

  private final ThreadLocal<RecoveryEncrypter> rc;

  public AtomicLong serializeTime;
  public AtomicLong cloneTime;
  public AtomicLong maskTime;
  public AtomicLong networkTime;

  private final ForkJoinPool executor;

  public WriteAheadLog(IAsyncBackingStore store, ThreadLocal<RecoveryEncrypter> rc, int treeLevels, int batchSize, NodeConfiguration config, ForkJoinPool executor) {
    this.config = config;
    this.store = store;
    this.rc = rc;
    this.maxKeys = config.ORAM_MAX_BLOCKS;
    this.treeLevels = treeLevels;
    this.maxLogicalAccessesInBatch = batchSize;
    this.bucketSlots = config.ORAM_Z + config.ORAM_S;
    this.maxPositionChanged = batchSize;
    this.maxStaleChanged = config.WRITES_SIZE;
    this.lastCompletedEpoch = 0;
    this.logicalAccessIndex = 0;
    this.evictPathCount = 0;
    this.checkpointFrequency = config.ORAM_DURABLE_CHECKPOINT_FREQ;
    this.positionMapCheckpoint = config.ORAM_DURABLE_CHECKPOINT_POSITION_MAP;
    this.validMapCheckpoint = config.ORAM_DURABLE_CHECKPOINT_VALID_MAP;
    this.staleMapCheckpoint = config.ORAM_DURABLE_CHECKPOINT_STALE_MAP;
    this.permutationMapCheckpoint = config.ORAM_DURABLE_CHECKPOINT_PERMUTATION_MAP;
    this.earlyReshuffleMapCheckpoint = config.ORAM_DURABLE_CHECKPOINT_EARLY_RESHUFFLE_MAP;
    this.strideSize = config.ORAM_DURABLE_MAX_DATA_SIZE;
    this.maxStashSize = config.ORAM_DURABLE_MAX_STASH_SIZE;
    this.serializeTime = new AtomicLong();
    this.cloneTime = new AtomicLong();
    this.maskTime = new AtomicLong();
    this.networkTime = new AtomicLong();
    this.executor = executor;
  }

  public void initializeRecovery(BaseRingOram oram, Runnable onInitialized) {
    System.err.printf("Initializing recovery....\n");
    readLastCompletedEpoch(() -> readLogEntriesForLastCompletedEpoch(oram, onInitialized::run));
  }

  public int getLastCompletedEpoch() { return lastCompletedEpoch; }

  public PositionMap getPositionMap() {
    return positionMap;
  }

  public ValidMap getValidMap() {
    return validMap;
  }

  public StaleMap getStaleMap() { return staleMap; }

  public PermutationMap getPermutationMap() {
    return permutationMap;
  }

  public EarlyReshuffleMap getEarlyReshuffleMap() {
    return earlyReshuffleMap;
  }

  public List<ReadPath> getLogicalAccesses() {
    return logicalAccesses;
  }

  public Stash getStash() { return stash; }

  public void writePositionMap(PositionMap positionMap, Function<String, Void> onLogged) {
    executor.submit(new PrepareRecoveryDataTask(true, true) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(String.format("%s_%d", POSITION_MAP_BASE_KEY, lastCompletedEpoch + 1));
      }

      @Override
      byte[] getData() {
        byte[] pmBytes;
        long start = System.nanoTime();
        if (lastCompletedEpoch == -1 || (lastCompletedEpoch + 1) % checkpointFrequency == positionMapCheckpoint) {
          pmBytes = positionMap.serialize();
        } else {
          pmBytes = positionMap.diff();
        }
        long end = System.nanoTime();
        serializeTime.addAndGet(end - start);
        start = end;
        positionMap.clearChanged();
        end = System.nanoTime();
        cloneTime.addAndGet(end - start);
        if (config.LOG_DURABILITY_POSITION_MAP_SIZE) {
          config.statsDurabilityPositionMapSize.addPoint(pmBytes.length);
        }
        return pmBytes;
      }

      @Override
      Runnable getDebugRunnable() {
        return getNamedDebugRunnable(System.nanoTime(), "position_map", onLogged);
      }
    });
  }

  public void writeStaleMap(StaleMap staleMap, Function<String, Void> onLogged) {
    executor.submit(new PrepareRecoveryDataTask(true, true) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(String.format("%s_%d", STALE_MAP_BASE_KEY, lastCompletedEpoch + 1));
      }

      @Override
      byte[] getData() {
        byte[] staleBytes;
        long start = System.nanoTime();
        if (lastCompletedEpoch == -1 || (lastCompletedEpoch + 1) % checkpointFrequency == staleMapCheckpoint) {
          staleBytes = staleMap.serialize();
        } else {
          staleBytes = staleMap.diff();
        }
        long end = System.nanoTime();
        serializeTime.addAndGet(end - start);
        start = end;
        staleMap.clearChanged();
        end = System.nanoTime();
        cloneTime.addAndGet(end - start);
        if (config.LOG_DURABILITY_STALE_MAP_SIZE) {
          config.statsDurabilityStaleMapSize.addPoint(staleBytes.length);
        }
        return staleBytes;
      }

      @Override
      Runnable getDebugRunnable() {
        return getNamedDebugRunnable(System.nanoTime(), "stale_map", onLogged);
      }
    });
  }

  public void writeStash(Stash stash, Function<String, Void> onLogged) {
    executor.submit(new PrepareRecoveryDataTask(true, true) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(String.format("%s_%d", STASH_BASE_KEY, lastCompletedEpoch + 1));
      }

      @Override
      byte[] getData() {
        byte[] stashBytes;
        long start = System.nanoTime();
        stashBytes = stash.serialize();
        long end = System.nanoTime();
        serializeTime.addAndGet(end - start);
        if (config.LOG_DURABILITY_STASH_SIZE) {
          config.statsDurabilityStashSize.addPoint(stashBytes.length);
        }
        return stashBytes;
      }

      @Override
      Runnable getDebugRunnable() {
        return getNamedDebugRunnable(System.nanoTime(), "stash", onLogged);
      }
    });
  }

  public void writePermutationMap(PermutationMap permutationMap, Function<String, Void> onLogged) {
    executor.submit(new PrepareRecoveryDataTask(true, true) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(String.format("%s_%d", PERMUTATION_MAP_BASE_KEY, lastCompletedEpoch + 1));
      }

      @Override
      byte[] getData() {
        byte[] pmBytes;
        long start = System.nanoTime();
        if (lastCompletedEpoch == -1 || (lastCompletedEpoch + 1) % checkpointFrequency == permutationMapCheckpoint) {
          pmBytes = permutationMap.serialize();
        } else {
          pmBytes = permutationMap.diff();
        }
        long end = System.nanoTime();
        serializeTime.addAndGet(end - start);
        start = end;
        permutationMap.clearChanged();
        end = System.nanoTime();
        cloneTime.addAndGet(end - start);
        if (config.LOG_DURABILITY_PERMUTATION_MAP_SIZE) {
          config.statsDurabilityPermutationMapSize.addPoint(pmBytes.length);
        }
        return pmBytes;
      }

      @Override
      Runnable getDebugRunnable() {
        return getNamedDebugRunnable(System.nanoTime(), "permutation_map", onLogged);
      }
    });
  }


  public void writeValidMap(ValidMap validMap, Function<String, Void> onLogged) {
    executor.submit(new PrepareRecoveryDataTask(true, false) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(String.format("%s_%d", VALID_MAP_BASE_KEY, lastCompletedEpoch + 1));
      }

      @Override
      byte[] getData() {
        byte[] data;
        long start = System.nanoTime();
        if (lastCompletedEpoch == -1 || (lastCompletedEpoch + 1) % checkpointFrequency == validMapCheckpoint) {
          data = validMap.serialize();
        } else {
          data = validMap.diff();
        }
        long end = System.nanoTime();
        serializeTime.addAndGet(end - start);
        start = end;
        validMap.clearChanged();
        end = System.nanoTime();
        cloneTime.addAndGet(end - start);
        if (config.LOG_DURABILITY_VALID_MAP_SIZE) {
          config.statsDurabilityValidMapSize.addPoint(data.length);
        }
        return data;
      }

      @Override
      Runnable getDebugRunnable() {
        return getNamedDebugRunnable(System.nanoTime(), "valid_map", onLogged);
      }
    });
  }

  public void writeEarlyReshuffleMap(EarlyReshuffleMap earlyReshuffleMap, Function<String, Void> onLogged) {
    executor.submit(new PrepareRecoveryDataTask(true, false) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(String.format("%s_%d", EARLY_RESHUFFLE_MAP_BASE_KEY, lastCompletedEpoch + 1));
      }

      @Override
      byte[] getData() {
        byte[] data;
        long start = System.nanoTime();
        if (lastCompletedEpoch == -1 || (lastCompletedEpoch + 1) % checkpointFrequency == earlyReshuffleMapCheckpoint) {
          data = earlyReshuffleMap.serialize();
        } else {
          data = earlyReshuffleMap.diff();
        }
        long end = System.nanoTime();
        serializeTime.addAndGet(end - start);
        start = end;
        earlyReshuffleMap.clearChanged();
        end = System.nanoTime();
        cloneTime.addAndGet(end - start);
        if (config.LOG_DURABILITY_EARLY_RESHUFFLE_MAP_SIZE) {
          config.statsDurabilityEarlyReshuffleMapSize.addPoint(data.length);
        }
        return data;
      }

      @Override
      Runnable getDebugRunnable() {
        return getNamedDebugRunnable(System.nanoTime(), "early_reshuffle_map", onLogged);
      }
    });
  }

  private Runnable getNamedDebugRunnable(Long start, String s, Function<String, Void> f) {
    return () -> {
      if (start > 0) {
        networkTime.addAndGet(System.nanoTime() - start);
      }
      f.apply(s);
    };
  }

  public void writeEvictedPathCount(int count, Function<String, Void> onLogged) {
    executor.submit(new PrepareRecoveryDataTask(false, true) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(String.format("%s_%d", EVICTED_PATH_COUNT_BASE_KEY, lastCompletedEpoch + 1));
      }

      @Override
      byte[] getData() {
        return Ints.toByteArray(count);
      }

      @Override
      Runnable getDebugRunnable() {
        return getNamedDebugRunnable(System.nanoTime(), "evicted_path_count", onLogged);
      }
    });
  }

  private int getLastPositionMapCheckpoint(int lastCompletedEpoch) {
    int lastPositionMapCheckpoint;
    if (lastCompletedEpoch % checkpointFrequency < positionMapCheckpoint) {
      lastPositionMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + positionMapCheckpoint);
    } else {
      lastPositionMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + positionMapCheckpoint;
    }
    return lastPositionMapCheckpoint;
  }

  private int getLastValidMapCheckpoint(int lastCompletedEpoch) {
    int lastValidMapCheckpoint;
    if (lastCompletedEpoch % checkpointFrequency < validMapCheckpoint) {
      lastValidMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + validMapCheckpoint);
    } else {
      lastValidMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + validMapCheckpoint;
    }
    return lastValidMapCheckpoint;
  }

  private int getLastEarlyReshuffleMapCheckpoint(int lastCompletedEpoch) {
    int lastEarlyReshuffleMapCheckpoint;
    if (lastCompletedEpoch % checkpointFrequency < earlyReshuffleMapCheckpoint) {
      lastEarlyReshuffleMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + earlyReshuffleMapCheckpoint);
    } else {
      lastEarlyReshuffleMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + earlyReshuffleMapCheckpoint;
    }
    return lastEarlyReshuffleMapCheckpoint;
  }

  private int getLastStaleMapCheckpoint(int lastCompletedEpoch) {
    int lastStaleMapCheckpoint;
    if (lastCompletedEpoch % checkpointFrequency < staleMapCheckpoint) {
      lastStaleMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + staleMapCheckpoint);
    } else {
      lastStaleMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + staleMapCheckpoint;
    }
    return lastStaleMapCheckpoint;
  }

  private int getLastPermutationMapCheckpoint(int lastCompletedEpoch) {
    int lastPermutationMapCheckpoint;
    if (lastCompletedEpoch % checkpointFrequency < permutationMapCheckpoint) {
      lastPermutationMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + permutationMapCheckpoint);
    } else {
      lastPermutationMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + permutationMapCheckpoint;
    }
    return lastPermutationMapCheckpoint;
  }

  private void deletePreviousEpochData() {
    Queue<Write> deletes = new ArrayDeque<>();

    for (int i = 0; i < maxLogicalAccessesInBatch; ++i) {
      deletes.add(new Write(Utility.hashPersistent(String.format("%s_%d_%d", LOGICAL_ACCESS_BASE_KEY, lastCompletedEpoch, i)), null, Write.Type.DELETE));
    }
    deletes.add(new Write(Utility.hashPersistent(String.format("%s_%d", STASH_BASE_KEY, lastCompletedEpoch - 1)), null, Write.Type.DELETE));

    int lastPositionMapCheckpoint = getLastPositionMapCheckpoint(lastCompletedEpoch);
    if (lastPositionMapCheckpoint == lastCompletedEpoch) {
      lastPositionMapCheckpoint = getLastPositionMapCheckpoint(lastCompletedEpoch - 1);
      for (int j = lastPositionMapCheckpoint; j < lastCompletedEpoch; ++j) {
        deletes.add(new Write(Utility.hashPersistent(String.format("%s_%d", POSITION_MAP_BASE_KEY, j)), null, Write.Type.DELETE));
      }
    }

    int lastValidMapCheckpoint = getLastValidMapCheckpoint(lastCompletedEpoch);
    if (lastValidMapCheckpoint == lastCompletedEpoch) {
      lastValidMapCheckpoint = getLastValidMapCheckpoint(lastCompletedEpoch - 1);
      for (int j = lastValidMapCheckpoint; j < lastCompletedEpoch; ++j) {
        deletes.add(new Write(Utility.hashPersistent(String.format("%s_%d", VALID_MAP_BASE_KEY, j)), null, Write.Type.DELETE));
      }
    }

    int lastStaleMapCheckpoint = getLastStaleMapCheckpoint(lastCompletedEpoch);
    if (lastStaleMapCheckpoint == lastCompletedEpoch) {
      lastStaleMapCheckpoint = getLastStaleMapCheckpoint(lastCompletedEpoch - 1);
      for (int j = lastStaleMapCheckpoint; j < lastCompletedEpoch; ++j) {
        deletes.add(new Write(Utility.hashPersistent(String.format("%s_%d", STALE_MAP_BASE_KEY, j)), null, Write.Type.DELETE));
      }
    }

    int lastEarlyReshuffleMapCheckpoint = getLastEarlyReshuffleMapCheckpoint(lastCompletedEpoch);
    if (lastEarlyReshuffleMapCheckpoint == lastCompletedEpoch) {
      lastEarlyReshuffleMapCheckpoint = getLastEarlyReshuffleMapCheckpoint(lastCompletedEpoch - 1);
      for (int j = lastEarlyReshuffleMapCheckpoint; j < lastCompletedEpoch; ++j) {
        deletes.add(new Write(Utility.hashPersistent(String.format("%s_%d", EARLY_RESHUFFLE_MAP_BASE_KEY, j)), null, Write.Type.DELETE));
      }
    }

    int lastPermutationMapCheckpoint = getLastPermutationMapCheckpoint(lastCompletedEpoch);
    if (lastPermutationMapCheckpoint == lastCompletedEpoch) {
      lastPermutationMapCheckpoint = getLastPermutationMapCheckpoint(lastCompletedEpoch - 1);
      for (int j = lastPermutationMapCheckpoint; j < lastCompletedEpoch; ++j) {
        deletes.add(new Write(Utility.hashPersistent(String.format("%s_%d", PERMUTATION_MAP_BASE_KEY, j)), null, Write.Type.DELETE));
      }
    }

    store.write(deletes, new AsyncDataRequest());
  }

  public void writeEpochComplete(Runnable onLogged) {
    executor.submit(new PrepareRecoveryDataTask(false, true) {
      @Override
      Long getKey() {
        return Utility.hashPersistent(LAST_COMPLETED_EPOCH_KEY);
      }

      @Override
      byte[] getData() {
        return Ints.toByteArray(lastCompletedEpoch + 1);
      }

      @Override
      Runnable getDebugRunnable() {
        return () -> {
          ++lastCompletedEpoch;
          System.err.printf("Finished epoch %d.\n", lastCompletedEpoch);
          logicalAccessIndex = 0;
          deletePreviousEpochData();
          onLogged.run();
        };
      }
    });
  }

  public Long getKeyForNextReadPath() {
    return Utility.hashPersistent(String.format("%s_%d_%d", LOGICAL_ACCESS_BASE_KEY, lastCompletedEpoch + 1, logicalAccessIndex++));
  }

  private class MaskAndWriteRecoveryDataTask extends ForkJoinTask<Void> {

    private final Long key;
    private final Long maskKey;
    private final boolean mask;
    private final byte[] data;
    private final Runnable onDataRequestCompleted;

    public MaskAndWriteRecoveryDataTask(Long key, Long maskKey, boolean mask, byte[] data, Runnable onDataRequestCompleted) {
      this.key = key;
      this.maskKey = maskKey;
      this.mask = mask;
      this.data = data;
      this.onDataRequestCompleted = onDataRequestCompleted;
    }

    @Override
    public Void getRawResult() {
      return null;
    }

    @Override
    protected void setRawResult(Void value) {
    }

    @Override
    protected boolean exec() {
      if (mask) {
        rc.get().mask(data, maskKey);
      }
      store.write(new Write(key, data, Write.Type.WRITE), new AsyncDataRequest() {
        @Override
        public void onDataRequestCompleted() {
          onDataRequestCompleted.run();
        }
      });
      return true;
    }
  }

  private abstract class PrepareRecoveryDataTask extends ForkJoinTask<Void> {

    private final boolean stride;
    private final boolean mask;

    PrepareRecoveryDataTask(boolean stride, boolean mask) {
      this.stride = stride;
      this.mask = mask;
    }

    abstract Long getKey();

    abstract byte[] getData();

    abstract Runnable getDebugRunnable();

    @Override
    public Void getRawResult() {
      return null;
    }

    @Override
    protected void setRawResult(Void value) {
    }

    @Override
    protected boolean exec() {
      Long key = getKey();
      byte[] data = getData();
      Runnable onDataRequestCompleted = getDebugRunnable();
      if (stride) {
        // add (strideSize - 1) to round up to next whole number with integer division
        // add 1 for the header write
        int total = (data.length + strideSize - 1) / strideSize + 1;
        AtomicInteger completedCount = new AtomicInteger();
        Runnable onFinished = () -> {
          if (completedCount.incrementAndGet() == total) {
            onDataRequestCompleted.run();
          }
        };
        byte[] headerData = Ints.toByteArray(data.length / strideSize + 1);
        store.write(new Write(key, headerData, Write.Type.WRITE), new AsyncDataRequest() {
          @Override
          public void onDataRequestCompleted() {
            onFinished.run();
          }
        });
        for (int i = 0; i < data.length; i += strideSize) {
          byte[] b = new byte[Math.min(strideSize, data.length - i)];
          System.arraycopy(data, i, b, 0, b.length);
          String keyS = String.format("%d_%d", key, i / strideSize);
          Long k = Utility.hashPersistent(keyS);
          new MaskAndWriteRecoveryDataTask(k, key, mask, b, onFinished).fork();
        }
      } else {
        new MaskAndWriteRecoveryDataTask(key, key, mask, data, onDataRequestCompleted).fork();
      }

      return true;
    }
  }

  private void readLastCompletedEpoch(Runnable onDataRequestCompleted) {
    store.read(Utility.hashPersistent(LAST_COMPLETED_EPOCH_KEY), new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        if (readValues.get(0) != null && readValues.get(0).length != 0) {
          assert readValues.get(0).length == 4;
          lastCompletedEpoch = Ints.fromByteArray(readValues.get(0));
        } else {
          lastCompletedEpoch = -1;
        }
        onDataRequestCompleted.run();
      }
    });
  }

  private static final String LOGICAL_ACCESS_BASE_KEY = "logical_access";

  private static final String POSITION_MAP_BASE_KEY = "position_map";

  private static final String VALID_MAP_BASE_KEY = "valid_map";

  private static final String STALE_MAP_BASE_KEY = "stale_map";

  private static final String EARLY_RESHUFFLE_MAP_BASE_KEY = "early_reshuffle_map";

  private static final String PERMUTATION_MAP_BASE_KEY = "permutation_map";

  private static final String STASH_BASE_KEY = "stash";

  private static final String EVICTED_PATH_COUNT_BASE_KEY = "evicted_path_count";

  private void readLogEntriesForLastCompletedEpoch(BaseRingOram oram, Runnable onDataRequestCompleted) {
    System.err.printf("Read last completed epoch of %d.\n", lastCompletedEpoch);
    if (lastCompletedEpoch >= 0) {
      System.err.printf("Recovering from epoch %d.\n", lastCompletedEpoch);
      List<Long> keys = new ArrayList<>();
      /**
       * not diffed, not strided
       */

      keys.add(Utility.hashPersistent(String.format("%s_%d", EVICTED_PATH_COUNT_BASE_KEY, lastCompletedEpoch)));
      for (int i = 0; i < maxLogicalAccessesInBatch; ++i) {
        keys.add(Utility.hashPersistent(String.format("%s_%d_%d", LOGICAL_ACCESS_BASE_KEY, lastCompletedEpoch + 1, i)));
      }

      /**
       * not diffed, strided
       */
      keys.add(Utility.hashPersistent(String.format("%s_%d", STASH_BASE_KEY, lastCompletedEpoch)));

      /**
       * diffed, strided
       */
      int lastPositionMapCheckpoint;
      if (lastCompletedEpoch % checkpointFrequency < positionMapCheckpoint) {
        lastPositionMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + positionMapCheckpoint);
      } else {
        lastPositionMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + positionMapCheckpoint;
      }
      for (int j = lastPositionMapCheckpoint; j <= lastCompletedEpoch; ++j) {
        System.err.printf("Reading position map %s from epoch %d.\n", j == lastPositionMapCheckpoint ? "checkpoint" : "diff", j);
        keys.add(Utility.hashPersistent(String.format("%s_%d", POSITION_MAP_BASE_KEY, j)));
      }

      int lastValidMapCheckpoint;
      if (lastCompletedEpoch % checkpointFrequency < validMapCheckpoint) {
        lastValidMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + validMapCheckpoint);
      } else {
        lastValidMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + validMapCheckpoint;
      }
      for (int j = lastValidMapCheckpoint; j <= lastCompletedEpoch; ++j) {
        keys.add(Utility.hashPersistent(String.format("%s_%d", VALID_MAP_BASE_KEY, j)));
      }

      int lastStaleMapCheckpoint;
      if (lastCompletedEpoch % checkpointFrequency < staleMapCheckpoint) {
        lastStaleMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + staleMapCheckpoint);
      } else {
        lastStaleMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + staleMapCheckpoint;
      }
      for (int j = lastStaleMapCheckpoint; j <= lastCompletedEpoch; ++j) {
        keys.add(Utility.hashPersistent(String.format("%s_%d", STALE_MAP_BASE_KEY, j)));
      }

      int lastEarlyReshuffleMapCheckpoint;
      if (lastCompletedEpoch % checkpointFrequency < earlyReshuffleMapCheckpoint) {
        lastEarlyReshuffleMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + earlyReshuffleMapCheckpoint);
      } else {
        lastEarlyReshuffleMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + earlyReshuffleMapCheckpoint;
      }
      for (int j = lastEarlyReshuffleMapCheckpoint; j <= lastCompletedEpoch; ++j) {
        keys.add(Utility.hashPersistent(String.format("%s_%d", EARLY_RESHUFFLE_MAP_BASE_KEY, j)));
      }
      
      int lastPermutationMapCheckpoint;
      if (lastCompletedEpoch % checkpointFrequency < permutationMapCheckpoint) {
        lastPermutationMapCheckpoint = Math.max(0, (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) - checkpointFrequency + permutationMapCheckpoint);
      } else {
        lastPermutationMapCheckpoint = (lastCompletedEpoch - lastCompletedEpoch % checkpointFrequency) + permutationMapCheckpoint;
      }
      for (int j = lastPermutationMapCheckpoint; j <= lastCompletedEpoch; ++j) {
        keys.add(Utility.hashPersistent(String.format("%s_%d", PERMUTATION_MAP_BASE_KEY, j)));
      }

      int networkingReadTimeKey = -1;
      if (config.LOG_DURABILITY_NETWORKING_READ_TIME) {
        networkingReadTimeKey = config.statsDurabilityNetworkingReadTime.addBegin();
      }
      final int networkingReadTimeKeyFinal = networkingReadTimeKey;
      store.read(keys, new AsyncDataRequest() {
        @Override
        public void onDataRequestCompleted() {
          int evictedPathCountIdx = 0;
          byte[] evictedPathCountData = readValues.get(evictedPathCountIdx);
          rc.get().mask(evictedPathCountData, keys.get(evictedPathCountIdx));
          evictPathCount = Ints.fromByteArray(evictedPathCountData);
          System.err.printf("Read evict path count %d.\n", evictPathCount);

          logicalAccesses = new ArrayList<>(maxLogicalAccessesInBatch);
          for (int i = 1; i <= maxLogicalAccessesInBatch; ++i) {
            int logicalAccessIdx = evictedPathCountIdx + i;
            if (readValues.get(logicalAccessIdx) == null || readValues.get(logicalAccessIdx).length == 0) {
              System.err.printf("Read %d logged read paths from previous epoch\n", i - 1);
              break;
            } else {
              byte[] laData = readValues.get(logicalAccessIdx);
              rc.get().mask(laData, keys.get(logicalAccessIdx));
              logicalAccesses.add(ReadPath.deserialize(laData, treeLevels));
            }
          }

          List<Long> stridedKeys = new ArrayList<>();

          int stashIdx = evictedPathCountIdx + maxLogicalAccessesInBatch + 1;

          byte[] stashHeaderData = readValues.get(stashIdx);
          int stashStrides = Ints.fromByteArray(stashHeaderData);
          for (int i = 0; i < stashStrides; ++i) {
            stridedKeys.add(Utility.hashPersistent(String.format("%d_%d", keys.get(stashIdx), i)));
          }

          int positionMapIdx = stashIdx + 1;
          int numPositionMapDiffs = 1 + Math.max(0, lastCompletedEpoch - lastPositionMapCheckpoint);
          int[] positionMapStrides = new int[numPositionMapDiffs];
          for (int j = 0; j < numPositionMapDiffs; ++j) {
            byte[] positionMapHeaderData = readValues.get(positionMapIdx + j);
            positionMapStrides[j] = Ints.fromByteArray(positionMapHeaderData);
            for (int i = 0; i < positionMapStrides[j]; ++i) {
              stridedKeys.add(Utility.hashPersistent(String.format("%d_%d", keys.get(positionMapIdx + j), i)));
            }
          }

          int validMapIdx = positionMapIdx + numPositionMapDiffs;
          int numValidMapDiffs = 1 + Math.max(0, lastCompletedEpoch - lastValidMapCheckpoint);
          int[] validMapStrides = new int[numValidMapDiffs];
          for (int j = 0; j < numValidMapDiffs; ++j) {
            byte[] validMapHeaderData = readValues.get(validMapIdx + j);
            validMapStrides[j] = Ints.fromByteArray(validMapHeaderData);
            for (int i = 0; i < validMapStrides[j]; ++i) {
              stridedKeys.add(Utility.hashPersistent(String.format("%d_%d", keys.get(validMapIdx + j), i)));
            }
          }


          int staleMapIdx = validMapIdx + numValidMapDiffs;
          int numStaleMapDiffs = 1 + Math.max(0, lastCompletedEpoch - lastStaleMapCheckpoint);
          int[] staleMapStrides = new int[numStaleMapDiffs];
          for (int j = 0; j < numStaleMapDiffs; ++j) {
            byte[] staleMapHeaderData = readValues.get(staleMapIdx + j);
            staleMapStrides[j] = Ints.fromByteArray(staleMapHeaderData);
            for (int i = 0; i < staleMapStrides[j]; ++i) {
              stridedKeys.add(Utility.hashPersistent(String.format("%d_%d", keys.get(staleMapIdx + j), i)));
            }
          }

          int earlyReshuffleMapIdx = staleMapIdx + numStaleMapDiffs;
          int numEarlyReshuffleMapDiffs = 1 + Math.max(0, lastCompletedEpoch - lastEarlyReshuffleMapCheckpoint);
          int[] earlyReshuffleMapStrides = new int[numEarlyReshuffleMapDiffs];
          for (int j = 0; j < numEarlyReshuffleMapDiffs; ++j) {
            byte[] earlyReshuffleMapHeaderData = readValues.get(earlyReshuffleMapIdx + j);
            earlyReshuffleMapStrides[j] = Ints.fromByteArray(earlyReshuffleMapHeaderData);
            for (int i = 0; i < earlyReshuffleMapStrides[j]; ++i) {
              stridedKeys.add(Utility.hashPersistent(String.format("%d_%d", keys.get(earlyReshuffleMapIdx + j), i)));
            }
          }

          int permutationMapIdx = earlyReshuffleMapIdx + numEarlyReshuffleMapDiffs;
          int numPermutationMapDiffs = 1 + Math.max(0, lastCompletedEpoch - lastPermutationMapCheckpoint);
          int[] permutationMapStrides = new int[numPermutationMapDiffs];
          for (int j = 0; j < numPermutationMapDiffs; ++j) {
            byte[] permutationMapHeaderData = readValues.get(permutationMapIdx + j);
            permutationMapStrides[j] = Ints.fromByteArray(permutationMapHeaderData);
            for (int i = 0; i < permutationMapStrides[j]; ++i) {
              stridedKeys.add(Utility.hashPersistent(String.format("%d_%d", keys.get(permutationMapIdx + j), i)));
            }
          }

          final AtomicLong deserializeTime  = new AtomicLong();
          final AtomicLong unmaskTime = new AtomicLong();
          store.read(stridedKeys, new AsyncDataRequest() {
            @Override
            public void onDataRequestCompleted() {
              if (config.LOG_DURABILITY_NETWORKING_READ_TIME) {
                config.statsDurabilityNetworkingReadTime.addEnd(networkingReadTimeKeyFinal);
              }
              byte[] stashData = concatenate(keys.get(stashIdx), true, readValues, 0, stashStrides);

              long start = System.nanoTime();
              stash = Stash.deserialize(stashData, oram, maxStashSize);
              long end = System.nanoTime();
              deserializeTime.addAndGet(end - start);
              start = end;

              int positionMapIdxStrides = stashStrides;
              long positionMapDes = 0;
              long positionMapDec = 0;
              long positionMapSize = 0;
              for (int j = 0; j < numPositionMapDiffs; ++j) {
                byte[] positionMapData = concatenate(keys.get(positionMapIdx + j), true, readValues, positionMapIdxStrides, positionMapIdxStrides + positionMapStrides[j]);
                end = System.nanoTime();
                unmaskTime.addAndGet(end - start);
                if (j == 0) {
                  positionMapDec += end - start;
                  positionMapSize = positionMapData.length;
                }
                start = end;
                if (j == 0) {
                  positionMap = PositionMap.deserialize(positionMapData, maxKeys, maxPositionChanged);
                } else {
                  positionMap.applyDiff(positionMapData);
                }
                end = System.nanoTime();
                deserializeTime.addAndGet(end - start);
                if (j == 0) {
                  positionMapDes += end - start;
                }
                start = end;
                positionMapIdxStrides += positionMapStrides[j];
              }
              System.err.printf("PositionMap deserialize took: %dms.\n", (long) (positionMapDes / 1e6));
              System.err.printf("PositionMap decrypt took: %dms.\n", (long) (positionMapDec / 1e6));
              System.err.printf("PositionMap size: %d bytes.\n", (long) (positionMapSize));

              int validMapIdxStrides = positionMapIdxStrides;
              long validMapDes = 0;
              long validMapDec = 0;
              long validMapSize = 0;
              for (int j = 0; j < numValidMapDiffs; ++j) {
                byte[] validMapData = concatenate(0L, false, readValues, validMapIdxStrides, validMapIdxStrides + validMapStrides[j]);
                start = System.nanoTime();
                if (j == 0) {
                  validMap = ValidMap.deserialize(validMapData, bucketSlots);
                } else {
                  validMap.applyDiff(validMapData);
                }
                end = System.nanoTime();
                deserializeTime.addAndGet(end - start);
                if (j == 0) {
                  validMapDes += end - start;
                  validMapSize = validMapData.length;
                }
                validMapIdxStrides += validMapStrides[j];
              }
              System.err.printf("ValidMap deserialize took: %dms.\n", (long) (validMapDes / 1e6));
              System.err.printf("ValidMap size: %d bytes.\n", (long) (validMapSize));

              long staleMapDes = 0;
              long staleMapDec = 0;
              long staleMapSize = 0;
              int staleMapIdxStrides = validMapIdxStrides;
              for (int j = 0; j < numStaleMapDiffs; ++j) {
                byte[] staleMapData = concatenate(keys.get(staleMapIdx + j), true, readValues, staleMapIdxStrides, staleMapIdxStrides + staleMapStrides[j]);
                end = System.nanoTime();
                unmaskTime.addAndGet(end - start);
                if (j == 0) {
                  staleMapDec += end - start;
                  staleMapSize = staleMapData.length;
                }
                start = end;
                if (j == 0) {
                  staleMap = StaleMap.deserialize(staleMapData, bucketSlots, maxStaleChanged);
                } else {
                  staleMap.applyDiff(staleMapData);
                }
                end = System.nanoTime();
                deserializeTime.addAndGet(end - start);
                if (j == 0) {
                  staleMapDes += end - start;
                }
                start = end;
                staleMapIdxStrides += staleMapStrides[j];
              }
              System.err.printf("StaleMap deserialize took: %dms.\n", (long) (staleMapDes / 1e6));
              System.err.printf("StaleMap decrypt took: %dms.\n", (long) (staleMapDec / 1e6));
              System.err.printf("StaleMap size: %d bytes.\n", (long) (staleMapSize));

              long earlyReshuffleMapDes = 0;
              long earlyReshuffleMapDec = 0;
              long earlyReshuffleMapSize = 0;
              int earlyReshuffleMapIdxStrides = staleMapIdxStrides;
              for (int j = 0; j < numEarlyReshuffleMapDiffs; ++j) {
                byte[] earlyReshuffleMapData = concatenate(0L, false, readValues, earlyReshuffleMapIdxStrides, earlyReshuffleMapIdxStrides + earlyReshuffleMapStrides[j]);
                start = System.nanoTime();
                if (j == 0) {
                  earlyReshuffleMap = EarlyReshuffleMap.deserialize(earlyReshuffleMapData);
                } else {
                  earlyReshuffleMap.applyDiff(earlyReshuffleMapData);
                }
                end = System.nanoTime();
                deserializeTime.addAndGet(end - start);
                if (j == 0) {
                  earlyReshuffleMapDes += end - start;
                  earlyReshuffleMapSize = earlyReshuffleMapData.length;
                }
                earlyReshuffleMapIdxStrides += earlyReshuffleMapStrides[j];
              }
              System.err.printf("EarlyReshuffleMap deserialize took: %dms.\n", (long) (earlyReshuffleMapDes / 1e6));
              System.err.printf("EarlyReshuffleMap decrypt took: %dms.\n", (long) (earlyReshuffleMapDec / 1e6));
              System.err.printf("EarlyReshuffleMap size: %d bytes.\n", (long) (earlyReshuffleMapSize));

              long permutationMapDes = 0;
              long permutationMapDec = 0;
              long permutationMapSize = 0;
              int permutationMapIdxStrides = earlyReshuffleMapIdxStrides;
              for (int j = 0; j < numPermutationMapDiffs; ++j) {
                byte[] permutationMapData = concatenate(keys.get(permutationMapIdx + j), true, readValues, permutationMapIdxStrides, permutationMapIdxStrides + permutationMapStrides[j]);
                end = System.nanoTime();
                unmaskTime.addAndGet(end - start);
                if (j == 0) {
                  permutationMapDec += end - start;
                  permutationMapSize = permutationMapData.length;
                }
                start = end;
                if (j == 0) {
                  permutationMap = PermutationMap.deserialize(permutationMapData, oram);
                } else {
                  permutationMap.applyDiff(permutationMapData);
                }
                end = System.nanoTime();
                deserializeTime.addAndGet(end - start);
                if (j == 0) {
                  permutationMapDes += end - start;
                }
                start = end;
                permutationMapIdxStrides += permutationMapStrides[j];
              }
              System.err.printf("PermutationMap deserialize took: %dms.\n", (long) (permutationMapDes / 1e6));
              System.err.printf("PermutationMap decrypt took: %dms.\n", (long) (permutationMapDec / 1e6));
              System.err.printf("PermutationMap size: %d bytes.\n", (long) (permutationMapSize));

              if (config.LOG_DURABILITY_DESERIALIZATION_TIME) {
                config.statsDurabilityDeserializationTime.addPoint(deserializeTime.get());
              }
              if (config.LOG_DURABILITY_UNMASK_TIME) {
                config.statsDurabilityUnmaskTime.addPoint(unmaskTime.get());
              }
              onDataRequestCompleted.run();
            }
          });
        }
      });
    } else {
      onDataRequestCompleted.run();
    }
  }

  private byte[] concatenate(Long maskKey, boolean mask, List<byte[]> data, int startIdx, int endIdx) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      for (int i = startIdx; i < endIdx; ++i) {
        byte[] da = data.get(i);
        if (mask) {
          rc.get().mask(da, maskKey);
        }
        baos.write(da);
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return baos.toByteArray();
  }

  public int getEvictPathCount() {
    return evictPathCount;
  }


}

