package shield.benchmarks.micro;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;

import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.oram.ORAMConfig;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.proxy.trx.data.Write;

public class EarlyReshuffleTest {


  public static SyncRingOram runTest(long time, int max_blocks, int z,
      int s, int a) {

    int nbQueries = 0;

    int keySize = 4;
    int valueSize = 5;

    NodeConfiguration c  = new NodeConfiguration();
    c.ORAM_MAX_BLOCKS = max_blocks;
    c.ORAM_Z = z;
    c.ORAM_S = s;
    c.ORAM_A = a;
    SyncRingOram store = SyncRingOram.create(new SecureRandom(ORAMConfig.clientKey), new SyncMapBackingStore(new HashMap<>((2 * max_blocks + 1) * (z + s), 0.9f)), c);

    store.enableRecordEarlyReshuffles();

    long start = System.currentTimeMillis();
    long end = start;
    Random ran = new Random();
    int nextKey = ran.nextInt(max_blocks);
    while (end - start < time) {
      Write write = new Write(new Long(nextKey), "hello".getBytes(), Write.Type.WRITE);
      store.write(write);
      nextKey = ran.nextInt(max_blocks);
      nbQueries++;
      // if (nbQueries % 1000 == 0) System.out.println("Executed " + nbQueries +
      // " Elapsed " + (end - start));
      end = System.currentTimeMillis();
    }
    System.out.println("Total Queries " + nbQueries);
    return store;
  }

  public static void printStats(SyncRingOram store, int max_blocks, int z,
      int s, int a) {
    int nbLevels = (int) (Math.log(max_blocks * 2 / a) / Math.log(2));
    System.out
        .println("Z " + z + " S " + s + " A " + a + " Levels " + nbLevels);
    System.out.println("Total ER Count: " + store.getEarlyReshuffleCount());
    System.out.println(
        "Count ER with tree-top Caching 0: " + store.getEarlyReshuffleLocation()
            .stream().filter(x -> x.getLeft() == 0).count());
    System.out.println(
        "Count ER with tree-top Caching 1: " + store.getEarlyReshuffleLocation()
            .stream().filter(x -> x.getLeft() == 1).count());
    System.out.println("Balance ER "
        + store.getEarlyReshuffleLocation().stream()
        .filter(x -> x.getLeft() == 1).filter(x -> x.getRight() == 0)
        .count()
        + " "
        + store.getEarlyReshuffleLocation().stream()
        .filter(x -> x.getLeft() == 1).filter(x -> x.getRight() == 1)
        .count());
    System.out.println(
        "Count ER with tree-top Caching 2: " + store.getEarlyReshuffleLocation()
            .stream().filter(x -> x.getLeft() == 2).count());
    System.out.println("Count ER with tree-top Caching >1: "
        + store.getEarlyReshuffleLocation().stream()
        .filter(x -> x.getLeft() > 1).count());
    System.out.println("Count ER with tree-top Caching >2: "
        + store.getEarlyReshuffleLocation().stream()
        .filter(x -> x.getLeft() > 2).count());
    System.out.println("Count ER with tree-top Caching >4: "
        + store.getEarlyReshuffleLocation().stream()
        .filter(x -> x.getLeft() > 4).count());
    System.out.println("Count ER with tree-top Caching >10: "
        + store.getEarlyReshuffleLocation().stream()
        .filter(x -> x.getLeft() > 10).count());
    System.out.println("Count ER with tree-top Caching >"
        + ((int) (nbLevels / 2)) + ": " + store.getEarlyReshuffleLocation()
        .stream().filter(x -> x.getLeft() > (nbLevels / 2)).count());
    System.out.println("Count ER with tree-top Caching >" + ((int) (nbLevels))
        + ": " + store.getEarlyReshuffleLocation().stream()
        .filter(x -> x.getLeft() >= (nbLevels)).count());
    System.out.println("*********************");
  }

  public static void main(String[] args) {

    int max_blocks = 66536;
    int z = 50;
    int s = 97;
    int a = 78;
    long time = 60 * 1000;
    SyncRingOram store = null;

    z = 100; // 200;
    s = 186; // 398;
    a = 168; // 354;
    store = runTest(time, max_blocks, z, s, a);
    printStats(store, max_blocks, z, s, a);
    store = null;

    System.gc();
    z = 196; // 200;
    s = 196; // 398;
    a = 168; // 354;
    store = runTest(time, max_blocks, z, s, a);
    printStats(store, max_blocks, z, s, a);
    store = null;

    z = 200;
    s = 398;
    a = 354;
    store = runTest(time, max_blocks, z, s, a);
    printStats(store, max_blocks, z, s, a);
    store = null;

    System.gc();
    z = 4; // 200;
    s = 3; // 398;
    a = 3; // 354;
    store = runTest(time, max_blocks, z, s, a);
    printStats(store, max_blocks, z, s, a);
    store = null;


  }

}
