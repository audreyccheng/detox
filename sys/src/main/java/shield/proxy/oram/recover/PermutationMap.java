package shield.proxy.oram.recover;

import shield.proxy.oram.BaseRingOram;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PermutationMap extends HashMap<Long, Map<Long, Integer>> {

  private final BaseRingOram oram;

  private final Set<Long> changed;

  public PermutationMap(BaseRingOram oram) {
    this.oram = oram;
    this.changed = new HashSet<>();
  }

  public void updatePermutation(int bucketLevel, int bucketIndex, Map<Long, Integer> permutation) {
    Long key = getMapKey(bucketLevel, bucketIndex);
    Map<Long, Integer> old = put(key, permutation);
    changed.add(key);
  }

  public void clearChanged() {
    this.changed.clear();
  }

  public Map<Long, Integer> getPermutation(int bucketLevel, int bucketIndex) {
    Map<Long, Integer> perm = get(getMapKey(bucketLevel, bucketIndex));
    if (perm == null) {
      return new HashMap<>();
    } else {
      return perm;
    }
  }

  private long getMapKey(int bucketLevel, int bucketIndex) {
    return ((long) bucketLevel) << 32 | bucketIndex;
  }

  public static PermutationMap deserialize(byte[] b, BaseRingOram oram) {
    PermutationMap pm = new PermutationMap(oram);
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    try {
      int entries = dis.readInt();
      for (int i = 0; i < entries; ++i) {
        long key = dis.readLong();
        Map<Long, Integer> perm = new HashMap<>();
        for (int j = 0; j < oram.getZ(); ++j) {
          boolean dummy = dis.readByte() == 0;
          if (dummy) {
            dis.readLong();
            dis.readInt();
          } else {
            perm.put(dis.readLong(), oram.getIntObject(dis.readInt()));
          }
        }
        pm.put(key, perm);
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    pm.clearChanged();
    return pm;
  }

  public byte[] serialize() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      System.err.printf("PermutationMap checkpoint size: %d permutations.\n", size());
      dos.writeInt(size());
      int k = 0;
      for (Map.Entry<Long, Map<Long, Integer>> entry : entrySet()) {
        dos.writeLong(entry.getKey());
        int written = 0;
        for (Map.Entry<Long, Integer> entry1 : entry.getValue().entrySet()) {
          dos.write(1);
          dos.writeLong(entry1.getKey());
          dos.writeInt(entry1.getValue());
          ++written;
        }
        for (int i = written; i < oram.getZ(); ++i) {
          dos.write(0);
          dos.writeLong(0L);
          dos.writeInt(0);
        }
      }
    } catch (Exception e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return bos.toByteArray();
  }

  public void applyDiff(byte[] diff) {
    PermutationMap diffMap = PermutationMap.deserialize(diff, oram);
    putAll(diffMap);
    clearChanged();
  }

  private static boolean permutationsEqual(Map<Long, Integer> p1, Map<Long, Integer> p2) {
    for (Map.Entry<Long, Integer> entry : p1.entrySet()) {
      Integer otherValue = p2.get(entry.getKey());
      if (otherValue == null || !otherValue.equals(entry.getValue())) {
        return false;
      }
    }
    for (Map.Entry<Long, Integer> entry : p2.entrySet()) {
      Integer otherValue = p1.get(entry.getKey());
      if (otherValue == null || !otherValue.equals(entry.getValue())) {
        return false;
      }
    }
    return true;
  }

  public byte[] diff() {
    PermutationMap diff2 = new PermutationMap(oram);
    for (Long entry : changed) {
      diff2.put(entry, get(entry));
    }
    return diff2.serialize();
  }


  @Override
  public PermutationMap clone() {
    PermutationMap cloned = new PermutationMap(oram);
    for (Map.Entry<Long, Map<Long, Integer>> entry : entrySet()) {
      Map<Long, Integer> clonedMap = new HashMap<>(entry.getValue());
      cloned.put(entry.getKey(), clonedMap);
    }
    return cloned;
  }
}
