package shield.tests.unit.oram;

import shield.config.NodeConfiguration;
import shield.proxy.data.async.AsyncSyncBackingStore;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.data.sync.SyncAsyncBackingStore;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.AsyncRingOram;
import shield.proxy.oram.SimulatedFailureException;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.tests.unit.BackingStoreTest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncRingOramRecoveryTest {

  private static final int N = 2048;

  private static final int SEED = 522;
  
  private static final int NUM_STRIDES = 5;

  private static final int READS_PER_STRIDE = 100;

  private static final int WRITES_PER_BATCH = 500;

  private static final NodeConfiguration CONFIG = new NodeConfiguration();
  static {
    CONFIG.ORAM_MAX_BLOCKS = N;
    CONFIG.ORAM_Z = 16;
    CONFIG.ORAM_S = 20;
    CONFIG.ORAM_A = 20;
    CONFIG.ORAM_ENCRYPT_BLOCKS = true;
    CONFIG.ORAM_VALUE_SIZE = 1024;
    CONFIG.ORAM_MASK_ALGORITHM = MaskAlgorithmType.BC_HMAC;
    CONFIG.CLIENT_KEY = new byte[1];
    CONFIG.ORAM_WRITE_WITHOUT_READ = true;
    CONFIG.ORAM_NONCE_LEN = 8;
    CONFIG.ORAM_WRITE_END_BATCH = true;

    CONFIG.STRIDE_SIZE = READS_PER_STRIDE;
    CONFIG.WRITES_SIZE = WRITES_PER_BATCH;
    CONFIG.MAX_NB_STRIDE = NUM_STRIDES;
    CONFIG.ORAM_PAD_BATCHES = true;
    CONFIG.ORAM_DURABLE = true;
    CONFIG.ORAM_BUFFER_SYNCOPS = false;
  }

  public static void longTest() {
    System.err.printf("$$$=== Testing Recover After Long Time ===$$$\n");
    Random r = new Random(SEED);
    IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
    AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
    ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
    List<Long> keys = BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, N, 500, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    List<Long> importantKeys = keys.subList(0, N / 2);
    List<Long> otherKeys = keys.subList(N / 2, keys.size());
    List<byte[]> values = BackingStoreTest.writeOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, importantKeys);
    oram.injectFailureAfterBatchOps((NUM_STRIDES - 2) * READS_PER_STRIDE + 13);
    try {
      BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, 1, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, otherKeys);
    } catch (SimulatedFailureException e) {
      oram.shutdown();
      oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
      syncBackingStore = new SyncAsyncBackingStore(oram);
      BackingStoreTest.checkPreviousOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, importantKeys, values);
      BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, 10, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys);
    }
    System.err.printf("$$$=== DONE ===$$$\n");
  }

  public static void failDuringEpochTest() {
    System.err.printf("$$$=== Testing Recover After Fail During Epoch ===$$$\n");
    Random r = new Random(SEED);
    IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
    AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
    ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
    List<Long> keys = BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, N, 6, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    List<Long> importantKeys = keys.subList(0, N / 2);
    List<Long> otherKeys = keys.subList(N / 2, keys.size());
    List<byte[]> values = BackingStoreTest.writeOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, importantKeys);
    oram.injectFailureAfterBatchOps(READS_PER_STRIDE - 3);
    try {
      BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, 1, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, otherKeys);
    } catch (SimulatedFailureException e) {
      oram.shutdown();
      oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
      syncBackingStore = new SyncAsyncBackingStore(oram);
      BackingStoreTest.checkPreviousOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, importantKeys, values);
      // continue testing as normal
      BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, 3, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys);
      values = BackingStoreTest.writeOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, importantKeys);
      oram.injectFailureAfterBatchOps(NUM_STRIDES * READS_PER_STRIDE + WRITES_PER_BATCH - 37);
      try {
        BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, 1, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, otherKeys);
      } catch (SimulatedFailureException e1) {
        oram.shutdown();
        oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
        syncBackingStore = new SyncAsyncBackingStore(oram);
        BackingStoreTest.checkPreviousOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, importantKeys, values);
        // continue testing as normal
        BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, 3, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys);
      }
    }
    System.err.printf("$$$=== DONE ===$$$\n");
  }

  public static void failBetweenEpochsTest() {
    System.err.printf("$$$=== Testing Recover After Fail Between Epochs ===$$$\n");
    Random r = new Random(SEED);
    IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
    AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
    ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
    // complete 3 epochs
    List<Long> keys = BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, N, 3, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    List<byte[]> values = BackingStoreTest.writeOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys);
    // simulate failure by reinitializing
    oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
    syncBackingStore = new SyncAsyncBackingStore(oram);
    // continue testing as normal
    BackingStoreTest.checkPreviousOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys, values);
    BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, 25, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys);
    System.err.printf("$$$=== DONE ===$$$\n");
  }

  public static void initTest() {
    System.err.printf("$$$=== Testing Init Durable ORAM with No Stored Recovery ===$$$\n");
    Random r = new Random(SEED);
    IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
    AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
    ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
    List<Long> keys = BackingStoreTest.testOramBatchSequence(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, N, 25, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    List<byte[]> values = BackingStoreTest.writeOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys);
    BackingStoreTest.checkPreviousOramBatch(NUM_STRIDES, READS_PER_STRIDE, WRITES_PER_BATCH, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE, keys, values);
    System.err.printf("$$$=== DONE ===$$$\n");
  }

  public static void main(String[] args) {
    initTest();
    failBetweenEpochsTest();
    failDuringEpochTest();
    longTest();
  }
}
