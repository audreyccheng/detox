package shield.tests.unit;

import java.util.concurrent.ConcurrentLinkedQueue;

import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;

import java.util.*;

public class BackingStoreTest {

  public static Long generateKey(Random r) {
    return r.nextLong();
  }

  public static byte[] generateValue(Random r, int length) {
    byte[] b = new byte[length];
    r.nextBytes(b);
    return b;
  }

  public static void checkKeySingle(Long key, byte[] value,
      ISyncBackingStore backingStore) {
    byte[] readValue = backingStore.read(key);
    System.err.printf("Read (%d,%d) as %d\n",
        key, (value == null) ? 0 : Arrays.hashCode(value), (readValue == null) ? 0 : Arrays.hashCode(readValue));
    assert Arrays.equals(readValue, value);
  }

  public static void checkKeysBatched(List<Long> keys,
      List<byte[]> values, ISyncBackingStore backingStore) {
    System.err.printf("=== STARTING READ BATCH ===\n");
    List<byte[]> readValues = backingStore.read(keys);
    for (int i = 0; i < keys.size(); ++i) {
      System.err.printf("Read (%s,%d,%d) as %d\n",
          keys.get(i),
          (values.get(i) == null) ? 0 : Arrays.hashCode(values.get(i)),
          i,
          (readValues.get(i) == null ? 0 : Arrays.hashCode(readValues.get(i))));
      assert Arrays.equals(readValues.get(i), values.get(i));
    }
    System.err.printf("=== DONE READ BATCH ===\n");
  }

  public static void testWriteRead(Random r, ISyncBackingStore backingStore, int valueSize) {
    System.out.println("=== Test WriteRead ===");
    Long key = generateKey(r);
    byte[] value = generateValue(r, valueSize);
    backingStore.write(new Write(key, value.clone(), Type.WRITE));
    checkKeySingle(key, value, backingStore);
    System.err.println("=== Success ===");
  }

  public static void testBatchSequence(int n, int m, Random r, ISyncBackingStore backingStore, int valueSize) {
    System.out.printf("=== Test BatchSequence (n=%d,m=%d) ===\n", n, m);
    List<Long> keys = new ArrayList<>();
    for (int i = 0; i < n; ++i) {
      Long key = generateKey(r);
      keys.add(key);
    }
    for (int i = 0; i < m; ++i) {
      System.err.printf("=== STARTING WRITE BATCH ===\n");
      List<Long> keysShuffled = new ArrayList<>(keys);
      Collections.shuffle(keysShuffled, r);
      Queue<Write> writes = new ConcurrentLinkedQueue<>();
      List<byte[]> values = new ArrayList<>();
      for (int j = 0; j < keysShuffled.size(); ++j) {
        byte[] value = generateValue(r, valueSize);
        writes.add(new Write(keysShuffled.get(j), value.clone(), Type.WRITE));
        values.add(value);
        System.err.printf("Write (%d,%d,%d)\n",
            (keysShuffled.get(j)),
            Arrays.hashCode(value), j);
      }
      backingStore.write(writes);
      System.err.printf("=== DONE WRITE BATCH ===\n");
      checkKeysBatched(keysShuffled, values, backingStore);
    }

    System.err.println("=== Success ===");
  }

  public static void testWriteInStash(int blockSize, ISyncBackingStore backingStore) {
    System.out.printf("=== Test WriteInStash ===\n");
    Random r = new Random(1);
    byte[] value1 = generateValue(r, blockSize);
    byte[] value2 = generateValue(r, blockSize);
    backingStore.write(new Write(1L, value1.clone(), Type.WRITE));
    backingStore.write(new Write(1L, value2.clone(), Type.WRITE));
    checkKeySingle(1L, value2, backingStore);
    System.err.println("=== Success ===");
  }

  public static void testReadFirst(int n, boolean batched, Random r, ISyncBackingStore backingStore) {
    System.err.printf("=== Test ReadFirst (n=%d,batched=%s) ===\n", n,
        batched);
    List<Long> keys = new ArrayList<>();
    List<byte[]> values = new ArrayList<>();
    for (int i = 0; i < n; ++i) {
      Long key = generateKey(r);
      keys.add(key);
      values.add(null);
    }
    for (int j = 0; j < 25; ++j) {
      if (batched) {
        checkKeysBatched(keys, values, backingStore);
      } else {
        for (int i = 0; i < n; ++i) {
          checkKeySingle(keys.get(i), values.get(i), backingStore);
        }
      }
    }
    System.err.println("=== Success ===");
  }

  public static void testMultipleWriteRead(int n, boolean batched, Random r, ISyncBackingStore backingStore, int valueSize) {
    System.err.printf("=== Test MultipleWriteRead (n=%d,batched=%s) ===\n", n,
        batched);
    List<Long> keys = new ArrayList<>();
    List<byte[]> values = new ArrayList<>();
    Queue<Write> pairs = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < n; ++i) {
      Long key = generateKey(r);
      byte[] value = generateValue(r, valueSize);
      pairs.add(new Write(key, value.clone(), Type.WRITE));
      keys.add(key);
      values.add(value);
      System.err.printf("Write (%d,%d)\n",
          (keys.get(i)),
          Arrays.hashCode(values.get(i)));
    }
    if (batched) {
      backingStore.write(pairs);
      checkKeysBatched(keys, values, backingStore);
    } else {
      for (int i = 0; i < n; ++i) {
        backingStore.write(new Write(keys.get(i), values.get(i).clone(), Type.WRITE));
        checkKeySingle(keys.get(i), values.get(i), backingStore);
      }
    }
  }

  public static void testWriteDeleteRead(int bufferWriteDelete, int bufferDeleteRead, Random r, ISyncBackingStore backingStore, int valueSize) {
    System.err.printf("=== Test WriteDeleteRead (b1=%d,b2=%d) ===\n", bufferWriteDelete, bufferDeleteRead);
    backingStore.write(new Write(1L, generateValue(r, valueSize), Type.WRITE));
    for (int i = 0; i < bufferWriteDelete; ++i) {
      backingStore.write(new Write(generateKey(r), generateValue(r, valueSize), Type.WRITE));
    }
    backingStore.write(new Write(1L, null, Type.DELETE));
    for (int i = 0; i < bufferDeleteRead; ++i) {
      backingStore.write(new Write(generateKey(r), generateValue(r, valueSize), Type.WRITE));
    }
    byte[] value = backingStore.read(1L);
    assert value == null;
    System.err.println("=== Success ===");
  }

  public static List<Long> testOramBatchSequence(int numStrides, int readsPerStride, int writesPerBatch, int m, Random r, ISyncBackingStore syncBackingStore, int valueSize, List<Long> keys) {
    for (int i = 0; i < m; ++i) {
      List<Long> readCopy = new ArrayList<>(keys);
      for (int j = 0; j < numStrides; ++j) {
        int s = Math.min(readsPerStride, readsPerStride / 2  + r.nextInt(readsPerStride));
        List<Long> readStride = new ArrayList<>(s);
        for (int k = 0; k < s; ++k) {
          readStride.add(readCopy.remove(r.nextInt(readCopy.size())));
        }
        System.err.printf("=== STARTING READ STRIDE %d ===\n", j);
        System.err.printf("--- Stride has %d/%d keys ---\n", s, readsPerStride);
        syncBackingStore.read(readStride);
        System.err.printf("=== DONE READ STRIDE %d ===\n", j);
      }
      List<Long> writeCopy = new ArrayList<>(keys);
      int s = Math.min(writesPerBatch, writesPerBatch / 2 + r.nextInt(writesPerBatch));
      Queue<Write> writeBatch = new ArrayDeque<>();
      for (int j = 0; j < s; ++j) {
        byte[] value = new byte[valueSize];
        r.nextBytes(value);
        writeBatch.add(new Write(writeCopy.remove(r.nextInt(writeCopy.size())), value, Type.WRITE));
      }

      System.err.printf("=== STARTING WRITE BATCH ===\n");
      System.err.printf("--- Batch has %d/%d keys ---\n", s, writesPerBatch);
      syncBackingStore.write(writeBatch);
      System.err.printf("=== DONE WRITE BATCH ===\n");
    }
    System.err.println("=== Success ===");
    return keys;
  }

  public static List<Long> testOramBatchSequence(int numStrides, int readsPerStride, int writesPerBatch, int n, int m, Random r, ISyncBackingStore syncBackingStore, int valueSize) {
    System.out.printf("=== Test OramBatchSequence (n=%d,m=%d) ===\n", n, m);
    List<Long> keys = new ArrayList<>();
    for (int i = 0; i < n; ++i) {
      Long key = generateKey(r);
      keys.add(key);
    }
    return testOramBatchSequence(numStrides, readsPerStride, writesPerBatch, m, r, syncBackingStore, valueSize, keys);
  }

  public static List<byte[]> writeOramBatch(int numStrides, int readsPerStride, int writesPerBatch, Random r, ISyncBackingStore syncBackingStore, int valueSize, List<Long> keys) {
    List<Long> readStride = new ArrayList<>();
    for (int j = 0; j < numStrides; ++j) {
      syncBackingStore.read(readStride);
    }
    Queue<Write> writeBatch = new ArrayDeque<>();
    List<byte[]> values = new ArrayList<>();
    for (int j = 0; j < writesPerBatch; ++j) {
      byte[] value = new byte[valueSize];
      r.nextBytes(value);
      writeBatch.add(new Write(keys.get(j), value.clone(), Type.WRITE));
      values.add(value);
    }
    syncBackingStore.write(writeBatch);

    return values;
  }

  public static void checkPreviousOramBatch(int numStrides, int readsPerStride, int writesPerBatch, Random r, ISyncBackingStore syncBackingStore, int valueSize, List<Long> keys, List<byte[]> values) {
    // check writes from previous epoch
    int idx = 0;
    System.err.println("=== Checking Values Written in Previous ORAM Batch ===");
    for (int i = 0; i < numStrides; ++i) {
      System.err.printf("=== Stride %d ===\n", i);
      List<byte[]> strideValues = new ArrayList<>();
      List<Long> strideKeys = new ArrayList<>();
      for (int j = 0; j < readsPerStride; ++j) {
        strideKeys.add(keys.get(idx));
        strideValues.add(values.get(idx++));
      }
      checkKeysBatched(strideKeys, strideValues, syncBackingStore);
      System.err.printf("=== Done Stride %d ===\n", i);
    }

    // finish ORAM epoch
    System.err.println("=== Finishing ORAM Epoch ===");
    Queue<Write> writeBatch = new ArrayDeque<>();
    syncBackingStore.write(writeBatch);
    System.err.println("=== Success ===");
  }

}
