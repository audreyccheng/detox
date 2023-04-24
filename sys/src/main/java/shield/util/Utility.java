package shield.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Utility {

  private static final HashFunction fastHash = Hashing.goodFastHash(64);
  private static final HashFunction persistentHash = Hashing.murmur3_128();

  public static int mostSignificantBitIndex(int n) {
    int i = -1;
    while (n > 0) {
      n >>= 1;
      i++;
    }
    return i;
  }

  public static void printIntArray(int[] array) {
    for (int i = 0; i < array.length; i++) {
      System.out.print(array[i] + " ");
    }
    System.out.println();
  }

  public static void printByteArray(byte[] array) {
    for (int i = 0; i < array.length; i++) {
      System.out.print(array[i] + " ");
    }
    System.out.println();
  }

  public static long hashPersistent(String key) {
    HashCode hc =
        persistentHash.newHasher(key.length()).putBytes(key.getBytes()).hash();
    return hc.asLong();
  }

  public static long hashPersistent(byte[] b) {
    HashCode hc =
        persistentHash.newHasher(b.length).putBytes(b).hash();
    return hc.asLong();
  }

  public static long hashVersionedKey(Long key, long version) {
    HashCode hc =persistentHash.newHasher().putLong(key).putLong(version).hash();
    return hc.asLong();
  }

  public static long hashTemp(String key) {
    HashCode hc =
        fastHash.newHasher(0).putBytes(key.getBytes()).hash();
    return hc.asLong();
  }


  public static int toBytes(int size) {
    int bytes = size / 8;
    if (size % 8 > 0) {
      bytes++;
    }
    return bytes;
  }

  public static int toInt(byte[] b) {
    assert b.length == 4;
    return b[3] << 24 | b[2] << 16 | b[1] << 8 | b[0];
  }
}
