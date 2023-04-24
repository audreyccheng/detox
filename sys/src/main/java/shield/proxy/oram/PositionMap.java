package shield.proxy.oram;

import shield.proxy.oram.recover.PermutationMap;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PositionMap extends HashMap<Long, Integer> {

  private final int maxKeys;
  private final int maxPositionChanged;

  private final Set<Long> changedKeys;

  public PositionMap(int maxKeys, int maxPositionChanged) {
    this.maxKeys = maxKeys;
    this.maxPositionChanged = maxPositionChanged;
    this.changedKeys = new HashSet<>(maxPositionChanged);
  }

  @Override
  public Integer put(Long key, Integer value) {
    Integer prev = super.put(key, value);
    if (!value.equals(prev)) {
      changedKeys.add(key);
    }
    return prev;
  }

  public void clearChanged() {
    this.changedKeys.clear();
  }

  public byte[] serialize() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(maxKeys * (Integer.BYTES + Long.BYTES) + Integer.BYTES);
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(maxKeys);
      int written = 0;
      for (Map.Entry<Long, Integer> entry : entrySet()) {
        dos.writeByte(1);
        dos.writeLong(entry.getKey());
        dos.writeInt(entry.getValue());
        ++written;
      }
      for (int i = written; i < maxKeys; ++i) {
        dos.write(0);
        dos.writeLong(0L);
        dos.writeInt(0);
      }
      System.err.printf("Wrote %d entries for position map.\n", maxKeys);
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return bos.toByteArray();
  }

  public static PositionMap deserialize(byte[] b, int maxKeys, int maxPositionChanged) {
    PositionMap map = new PositionMap(maxKeys, maxPositionChanged);
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    try {
      int keys = dis.readInt();
      for (int i = 0; i < keys; ++i) {
        boolean dummy = dis.readByte() == 0;
        if (dummy) {
          System.err.printf("Read %d keys for position map.\n", keys);
          break;
        }
        Long key = dis.readLong();
        Integer value = dis.readInt();
        map.put(key, value);
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    map.clearChanged();
    return map;
  }

  public byte[] diff() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    try {
      int written = 0;
      for (Long entry : changedKeys) {
        Integer value = get(entry);
        dos.writeByte(1);
        dos.writeLong(entry);
        dos.writeInt(value);
        ++written;
      }
      System.err.printf("Wrote %d real keys for position map diff.\n", written);
      assert changedKeys.size() <= maxPositionChanged;
      for (int i = written; i < maxPositionChanged; ++i) {
        dos.writeByte(0);
        dos.writeLong(0L);
        dos.writeInt(0);
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return baos.toByteArray();
  }

  public void applyDiff(byte[] diff) {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(diff));
    try {
      for (int i = 0; i < maxPositionChanged; ++i) {
        boolean done = dis.readByte() == 0;
        if (done) {
          System.err.printf("Read %d real keys for position map diff.\n", i);
          break;
        }
        Long key = dis.readLong();
        Integer value = dis.readInt();
        put(key, value);
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    clearChanged();
  }

  @Override
  public PositionMap clone() {
    PositionMap pm = new PositionMap(maxKeys, maxPositionChanged);
    for (Map.Entry<Long, Integer> entry : entrySet()) {
      pm.put(entry.getKey(), entry.getValue());
    }
    return pm;
  }
}
