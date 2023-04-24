package shield.tests.unit.oram;

import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.tests.unit.BackingStoreTest;

import java.util.Random;

public class SyncRingOramFunctionalityTest {

  private static final int SEED = 0;

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
    CONFIG.ORAM_BUFFER_SYNCOPS = true;
  }

  public static void main(String[] args) {
    for (int i = 0; i < N.length; ++i) {
      Random r = new Random(SEED);
      SyncMapBackingStore backingStore = new SyncMapBackingStore();
      CONFIG.ORAM_MAX_BLOCKS = N[i];
      CONFIG.ORAM_Z = Z[i];
      CONFIG.ORAM_S = S[i];
      CONFIG.ORAM_A = A[i];
      CONFIG.ORAM_VALUE_SIZE = B[i];
      SyncRingOram syncBackingStore = SyncRingOram.create(r, backingStore, CONFIG);
      BackingStoreTest.testWriteRead(r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
      BackingStoreTest.testWriteInStash(CONFIG.ORAM_VALUE_SIZE, syncBackingStore);
      BackingStoreTest.testReadFirst(1000, true, r, syncBackingStore);
      BackingStoreTest.testBatchSequence(1000, 25, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
      BackingStoreTest.testBatchSequence(10002, 50, r, syncBackingStore, CONFIG.ORAM_VALUE_SIZE);
    }
  }
}
