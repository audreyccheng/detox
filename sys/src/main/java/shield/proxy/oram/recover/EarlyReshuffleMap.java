package shield.proxy.oram.recover;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EarlyReshuffleMap extends HashMap<Long, Integer> {

  private final Set<Long> changed;

  public EarlyReshuffleMap() {
    this.changed = new HashSet<>();
  }

  public void clearChanged() {
    this.changed.clear();
  }

  public void increment(int level, int index) {
    Long k = getMapKey(level, index);
    compute(k, (key, count) -> count == null ? 1 : count + 1);
    changed.add(k);
  }

  public void reset(int level, int index) {
    Long k = getMapKey(level, index);
    remove(k);
    changed.remove(k);
  }

  public int getCount(int bucketLevel, int bucketIndex) {
    Integer c = get(getMapKey(bucketLevel, bucketIndex));
    return c == null ? 0 : c;
  }

  private long getMapKey(int bucketLevel, int bucketIndex) {
    return ((long) bucketLevel) << 32 | bucketIndex;
  }

  public static EarlyReshuffleMap deserialize(byte[] b) {
    EarlyReshuffleMap erm = new EarlyReshuffleMap();
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    try {
      int entries = dis.readInt();
      for (int i = 0; i < entries; ++i) {
        erm.put(dis.readLong(), dis.readInt());
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    erm.clearChanged();
    return erm;
  }

  public byte[] serialize() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(Integer.BYTES + size() * (Long.BYTES + Integer.BYTES));
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(size());
      for (Map.Entry<Long, Integer> entry : entrySet()) {
        dos.writeLong(entry.getKey());
        dos.writeInt(entry.getValue());
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return bos.toByteArray();
  }

  public byte[] diff() {
    EarlyReshuffleMap diff = new EarlyReshuffleMap();
    for (Long entry : changed) {
      diff.put(entry, get(entry));
    }
    return diff.serialize();
  }

  public void applyDiff(byte[] diff) {
    EarlyReshuffleMap diffMap = EarlyReshuffleMap.deserialize(diff);
    putAll(diffMap);
    clearChanged();
  }

  @Override
  public EarlyReshuffleMap clone() {
    return (EarlyReshuffleMap) super.clone();
  }

}
