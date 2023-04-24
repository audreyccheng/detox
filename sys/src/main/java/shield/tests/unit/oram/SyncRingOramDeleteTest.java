package shield.tests.unit.oram;

import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.tests.unit.BackingStoreTest;

import java.util.Random;

public class SyncRingOramDeleteTest {

  private static final int SEED = 7759;

  private static final NodeConfiguration CONFIG = new NodeConfiguration();
  static {
    CONFIG.ORAM_MAX_BLOCKS = 1024 * 100;
    CONFIG.ORAM_Z = 16;
    CONFIG.ORAM_S = 20;
    CONFIG.ORAM_A = 20;
    CONFIG.ORAM_VALUE_SIZE = 1024;
    CONFIG.ORAM_NONCE_LEN = 1;
    CONFIG.ORAM_ENCRYPT_BLOCKS = true;
    CONFIG.ORAM_MASK_ALGORITHM = MaskAlgorithmType.BC_HMAC;
    CONFIG.ORAM_WRITE_END_BATCH = false;
    CONFIG.ORAM_PAD_BATCHES = false;
    CONFIG.ORAM_WRITE_WITHOUT_READ = false;
    CONFIG.ORAM_DURABLE = false;
    CONFIG.ORAM_BUFFER_SYNCOPS = false;
    CONFIG.STRIDE_SIZE = 0;
    CONFIG.WRITES_SIZE = 0;
    CONFIG.MAX_NB_STRIDE = 0;
    CONFIG.CLIENT_KEY = new byte[1];
  }

  public static void main(String[] args) {
    Random r = new Random(SEED);
    SyncMapBackingStore backingStore = new SyncMapBackingStore();
    SyncRingOram syncBackingStore = SyncRingOram.create(r, backingStore, CONFIG);
    BackingStoreTest.testWriteDeleteRead(0, 0, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    BackingStoreTest.testWriteDeleteRead(20, 0, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    BackingStoreTest.testWriteDeleteRead(20, 20, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    BackingStoreTest.testWriteDeleteRead(0, 20, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
  }
}
