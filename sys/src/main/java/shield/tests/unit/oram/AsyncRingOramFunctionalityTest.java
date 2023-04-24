package shield.tests.unit.oram;

import shield.config.NodeConfiguration;
import shield.proxy.data.async.AsyncSyncBackingStore;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.data.sync.SyncAsyncBackingStore;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.AsyncRingOram;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.tests.unit.BackingStoreTest;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncRingOramFunctionalityTest {

  private static final int SEED = 1313;

  private static final int[] N = {1024, 1024 * 10, 1024 * 100};
  private static final int[] B = {1024, 1024, 1024};
  private static final int[] Z = {4, 8, 16};
  private static final int[] S = {3, 8, 20};
  private static final int[] A = {3, 8, 20};

  private static final NodeConfiguration CONFIG = new NodeConfiguration();
  static {
    CONFIG.ORAM_ENCRYPT_BLOCKS = true;
    CONFIG.ORAM_MASK_ALGORITHM = MaskAlgorithmType.BC_HMAC;
    CONFIG.CLIENT_KEY = new byte[1];
    CONFIG.ORAM_WRITE_WITHOUT_READ = true;
    CONFIG.ORAM_NONCE_LEN = 1;
    CONFIG.ORAM_WRITE_END_BATCH = true;

    CONFIG.STRIDE_SIZE = 100;
    CONFIG.WRITES_SIZE = 500;
    CONFIG.MAX_NB_STRIDE = 5;
    CONFIG.ORAM_PAD_BATCHES = false;
    CONFIG.ORAM_DURABLE = false;
    CONFIG.ORAM_BUFFER_SYNCOPS = false;

    CONFIG.LOG_STORAGE_TIME = false;
  }

  public static void main(String[] args) {
    CONFIG.ORAM_ENCRYPT_BLOCKS = false;
    for (int i = 0; i < N.length; ++i) {
      Random r = new Random(SEED);
      IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
      CONFIG.ORAM_MAX_BLOCKS = N[i];
      CONFIG.ORAM_Z = Z[i];
      CONFIG.ORAM_S = S[i];
      CONFIG.ORAM_A = A[i];
      CONFIG.ORAM_VALUE_SIZE = B[i];
      AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
      ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
      BackingStoreTest.testReadFirst(1000, true, r, syncBackingStore);
      BackingStoreTest.testWriteRead(r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
      BackingStoreTest.testWriteInStash(CONFIG.ORAM_VALUE_SIZE, syncBackingStore);
      BackingStoreTest.testBatchSequence(1000, 25, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
      BackingStoreTest.testBatchSequence(10002, 50, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
      oram.shutdown();
    }
    CONFIG.ORAM_WRITE_WITHOUT_READ = true;
    CONFIG.ORAM_WRITE_END_BATCH = true;
    for (int i = 0; i < N.length; ++i) {
      int opsInBatch = 1000;
      Random r = new Random(SEED);
      IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
      CONFIG.ORAM_MAX_BLOCKS = N[i];
      CONFIG.ORAM_Z = Z[i];
      CONFIG.ORAM_S = S[i];
      CONFIG.ORAM_A = A[i];
      CONFIG.ORAM_VALUE_SIZE = B[i];
      AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
      ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
      BackingStoreTest.testReadFirst(opsInBatch, true, r, syncBackingStore);
      BackingStoreTest.testWriteRead(r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
      BackingStoreTest.testWriteInStash(CONFIG.ORAM_VALUE_SIZE, syncBackingStore);
      BackingStoreTest.testBatchSequence(opsInBatch, 50, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
      oram.shutdown();
    }
    CONFIG.ORAM_ENCRYPT_BLOCKS = true;
    CONFIG.ORAM_WRITE_WITHOUT_READ = false;
    CONFIG.ORAM_WRITE_END_BATCH = false;
    for (int i = 0; i < N.length; ++i) {
      Random r = new Random(SEED);
      IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
      CONFIG.ORAM_MAX_BLOCKS = N[i];
      CONFIG.ORAM_Z = Z[i];
      CONFIG.ORAM_S = S[i];
      CONFIG.ORAM_A = A[i];
      CONFIG.ORAM_VALUE_SIZE = B[i];
      AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
      ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
      BackingStoreTest.testReadFirst(1000, true, r, syncBackingStore);
      BackingStoreTest.testWriteRead(r, syncBackingStore, B[i]);
      BackingStoreTest.testWriteInStash(CONFIG.ORAM_VALUE_SIZE, syncBackingStore);
      BackingStoreTest.testBatchSequence(1000, 25, r, syncBackingStore, B[i]);
      BackingStoreTest.testBatchSequence(10002, 50, r, syncBackingStore, B[i]);
      oram.shutdown();
    }
  }
}
