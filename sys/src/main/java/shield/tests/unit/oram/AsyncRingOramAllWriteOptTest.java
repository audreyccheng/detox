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

public class AsyncRingOramAllWriteOptTest {

  private static final int SEED = 59;

  private static final NodeConfiguration CONFIG = new NodeConfiguration();
  static {
    CONFIG.ORAM_MAX_BLOCKS = 1024 * 100;
    CONFIG.ORAM_Z = 16;
    CONFIG.ORAM_S = 20;
    CONFIG.ORAM_A = 20;
    CONFIG.ORAM_ENCRYPT_BLOCKS = false;
    CONFIG.ORAM_VALUE_SIZE = 1024;
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
  }

  public static void main(String[] args) {
    Random r = new Random(SEED);
    IAsyncBackingStore backingStore = new AsyncSyncBackingStore(new SyncMapBackingStore(new ConcurrentHashMap<>()));
    AsyncRingOram oram = AsyncRingOram.create(r, backingStore, 8, CONFIG);
    ISyncBackingStore syncBackingStore = new SyncAsyncBackingStore(oram);
    BackingStoreTest.testBatchSequence(1103, 50, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    oram.shutdown();
  }
}
