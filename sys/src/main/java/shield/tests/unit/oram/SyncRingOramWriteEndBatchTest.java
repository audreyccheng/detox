package shield.tests.unit.oram;

import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.tests.unit.BackingStoreTest;

import java.util.Random;

public class SyncRingOramWriteEndBatchTest {

  private static final int SEED = 56;

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
    CONFIG.ORAM_WRITE_WITHOUT_READ = false;
    CONFIG.ORAM_NONCE_LEN = 1;
    CONFIG.ORAM_WRITE_END_BATCH = true;

    CONFIG.STRIDE_SIZE = 100;
    CONFIG.WRITES_SIZE = 500;
    CONFIG.MAX_NB_STRIDE = 5;
    CONFIG.ORAM_PAD_BATCHES = false;
    CONFIG.ORAM_DURABLE = false;
  }

  public static void main(String[] args) {
    Random r = new Random(SEED);
    SyncMapBackingStore backingStore = new SyncMapBackingStore();
    SyncRingOram syncBackingStore = SyncRingOram.create(r, backingStore, CONFIG);
    BackingStoreTest.testBatchSequence(1313, 25, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
  }
}
