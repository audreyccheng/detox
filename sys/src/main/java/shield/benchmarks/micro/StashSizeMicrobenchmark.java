package shield.benchmarks.micro;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.proxy.trx.data.Write;
import shield.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by matt on 1/11/18.
 */
public class StashSizeMicrobenchmark {

  public static int SEED = 13;

  public static int N = 66656;
  public static int Z = 4; // 100 3;
  public static int S = 3; // 196 ; 3;
  public static int A = 4; // 168 ; 4;

  public static int KEY_SIZE = 8;
  public static int VALUE_SIZE = 1;

  public static int BENCHMARK_TIME = 2 * 60 * 1000;

  public static SyncRingOram[] orams = new SyncRingOram[2];
  public static long[] average_stashsize = {0, 0};
  public static long[] max_stashsize = {0, 0};
  public static long nbQueries = 0;

  public static Random rand = new Random(SEED);

  public static void main(String[] args) {

    // Generating keys
    Queue<Write> initialWrites = new ConcurrentLinkedQueue<>();
    List<Long> keys = new ArrayList<>();

    N = 250000;
    Z = 100;
    S = 196;
    A = 168;
    generateKeys(N, keys, initialWrites);
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);

    N = 10000;
    Z = 4;
    S = 3;
    A = 4;
    generateKeys(N, keys, initialWrites);
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 38;
    S = 71;
    A = 51;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 100;
    S = 196;
    A = 168;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 200;
    S = 398;
    A = 354;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 398;
    S = 398;
    A = 354;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);

    N = 66656;
    generateKeys(N, keys, initialWrites);
    Z = 4;
    S = 3;
    A = 4;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 38;
    S = 71;
    A = 57;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 100;
    S = 196;
    A = 168;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 200;
    S = 398;
    A = 354;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 398;
    S = 398;
    A = 354;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);

    N = 1000000;
    generateKeys(N, keys, initialWrites);
    Z = 4;
    S = 3;
    A = 4;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 38;
    S = 71;
    A = 57;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 100;
    S = 196;
    A = 168;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 200;
    S = 398;
    A = 354;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);
    Z = 398;
    S = 398;
    A = 354;
    runExperiment(N, Z, S, A, keys, initialWrites);
    printStats(N, Z, S, A);

  }

  public static void generateKeys(int N, List<Long> keys,
      Queue<Write> initialWrites) {
    keys.clear();
    initialWrites.clear();
    for (int i = 0; i < N; ++i) {
      byte[] keyBytes = new byte[KEY_SIZE];
      rand.nextBytes(keyBytes);
      Long key = Utility.hashPersistent(new String(keyBytes));
      // initially keys will have value 0
      byte[] initialValue = new byte[VALUE_SIZE];
      keys.add(key);
      initialWrites.add(new Write(key, initialValue, Write.Type.WRITE));
    }

  }

  public static void runExperiment(int N, int Z, int S, int A,
      List<Long> keys, Queue<Write> initialWrites) {

    System.out.println("[Experiment] " + N + " " + Z + " " + S + " " + A);
    orams = new SyncRingOram[2];
    average_stashsize[0] = 0;
    average_stashsize[1] = 0;
    max_stashsize[0] = 0;
    max_stashsize[1] = 0;
    nbQueries = 0;

    for (int i = 0; i < 10; i++) {
      // Trick to remove all the stale blocks before we restart experiment
      System.gc();
    }

    NodeConfiguration config = new NodeConfiguration();
    config.ORAM_MAX_BLOCKS = N;
    config.ORAM_Z = Z;
    config.ORAM_S = S;
    config.ORAM_A = A;
    config.ORAM_ENCRYPT_BLOCKS = true;

    orams[0] = SyncRingOram.create(rand, new SyncMapBackingStore(new HashMap<>()), config);
    orams[1] = SyncRingOram.create(rand, new SyncMapBackingStore(new HashMap<>()), config);
    orams[1].enableRemapOnStashAdd();
    orams[0].write(initialWrites);
    // orams[1].write(initialWrites);

    long start = System.currentTimeMillis();
    int i = 0;
    while (System.currentTimeMillis() - start < BENCHMARK_TIME) {
      boolean write = rand.nextBoolean();
      Long key = keys.get(i++ % keys.size());
      // ByteString key = keys.get(rand.nextInt(keys.size()));
      if (write) {
        byte[] value = new byte[VALUE_SIZE];
        rand.nextBytes(value);
        orams[0].write(new Write(key, value, Write.Type.WRITE));
        //    orams[1].write(key, value);
      } else {
        orams[0].read(key);
        //   orams[1].read(key);
      }
      nbQueries++;
      average_stashsize[0] += orams[0].getStashSize();
      //  average_stashsize[1] += orams[1].getStashSize();
      max_stashsize[0] = orams[0].getStashSize() > max_stashsize[0]
          ? orams[0].getStashSize() : max_stashsize[0];
      //  max_stashsize[1] = orams[1].getStashSize() > max_stashsize[1]
      //     ? orams[1].getStashSize() : max_stashsize[1];
    }

  }

  public static void printStats(int N, int Z, int S, int A) {
    double stashNoRemap = (double) average_stashsize[0] / (double) nbQueries;
    double stashRemap = (double) average_stashsize[1] / (double) nbQueries;
    System.out.println("[FINAL] N:" + N + " Z:" + Z + " S:" + S + " A:" + A
        + " NRAvg:" + stashNoRemap /*+ " RAvg:" + stashRemap*/ + " NRMax"
        + max_stashsize[0] /*+ " RMax" + max_stashsize[1]*/);
  }


}
