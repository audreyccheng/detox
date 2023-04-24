package shield.tests.unit.oram;

import shield.benchmarks.utils.StatisticsCollector;
import shield.config.NodeConfiguration;
import shield.proxy.data.async.AsyncSyncBackingStore;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.data.sync.SyncAsyncBackingStore;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.AsyncRingOram;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.tests.unit.BackingStoreTest;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncRingOramStressTest {

  private static final int SEED = 12; // for Tom Brady

  private static final NodeConfiguration CONFIG = new NodeConfiguration();
  static {
    CONFIG.ORAM_MAX_BLOCKS = 100000;
    CONFIG.ORAM_Z = 100;
    CONFIG.ORAM_S = 196;
    CONFIG.ORAM_A = 168;
    CONFIG.ORAM_ENCRYPT_BLOCKS = true;
    CONFIG.ORAM_VALUE_SIZE = 32;
    CONFIG.ORAM_MASK_ALGORITHM = MaskAlgorithmType.BC_HMAC;
    CONFIG.CLIENT_KEY = new byte[1];
    CONFIG.ORAM_WRITE_WITHOUT_READ = false;
    CONFIG.ORAM_NONCE_LEN = 1;
    CONFIG.ORAM_WRITE_END_BATCH = false;

    CONFIG.STRIDE_SIZE = 500;
    CONFIG.WRITES_SIZE = 500;
    CONFIG.MAX_NB_STRIDE = 5;
    CONFIG.ORAM_PAD_BATCHES = true;
    CONFIG.ORAM_DURABLE = false;
    CONFIG.ORAM_BUFFER_SYNCOPS = false;

    CONFIG.N_WORKER_THREADS = 8;

    CONFIG.LOG_STORAGE_ACCESSES_PER_BATCH = true;
    CONFIG.statsStorageReadsPerBatch = new StatisticsCollector("log_storage_reads_per_batch.txt");
    CONFIG.statsStorageWritesPerBatch = new StatisticsCollector("log_storage_reads_per_batch.txt");
  }

  public static void main(String[] args) {
    Random r = new Random(SEED);
    IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
    AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
    ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
    BackingStoreTest.testOramBatchSequence(CONFIG.MAX_NB_STRIDE, CONFIG.STRIDE_SIZE, CONFIG.WRITES_SIZE, 10000, 10000, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    oram.shutdown();
  }

}
