package shield.proxy.oram.recover;

import java.io.*;
import java.util.*;

public class StaleMap {

  private final int bucketSlots;
  public final int[] staleBits;
  private final int maxStaleChanged;
  private final Set<Integer> changed;

  public StaleMap(int levels, int bucketSlots, int maxStaleChanged) {
    this.bucketSlots = bucketSlots;
    this.staleBits = new int[((((1 << levels) - 1) * bucketSlots) + Integer.SIZE - 1) / Integer.SIZE];
    this.maxStaleChanged = maxStaleChanged;
    this.changed = new HashSet<>(maxStaleChanged);
  }

  private StaleMap(int[] validBits, int bucketSlots, int maxStaleChanged) {
    this.staleBits = validBits;
    this.bucketSlots = bucketSlots;
    this.maxStaleChanged = maxStaleChanged;
    this.changed = new HashSet<>(maxStaleChanged);
  }

  public void clearChanged() {
    this.changed.clear();
  }

  public static StaleMap deserialize(byte[] b, int bucketSlots, int maxStaleChanged) {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    try {
      int longs = dis.readInt();
      int[] validBits = new int[longs];
      for (int i = 0; i < longs; ++i) {
        validBits[i] = dis.readInt();
      }
      return new StaleMap(validBits, bucketSlots, maxStaleChanged);
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return null;
  }

  public byte[] serialize() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(Integer.BYTES + staleBits.length * Integer.BYTES);
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(staleBits.length);
      for (int i = 0; i < staleBits.length; ++i) {
        dos.writeInt(staleBits[i]);
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return bos.toByteArray();
  }

  private int getTrueIdx(int bucketLevel, int bucketIndex, int blockIndex) {
    return (((1 << bucketLevel) - 1) + bucketIndex) * bucketSlots + blockIndex;
  }

  // if bit is set, then block is stale
  public void staleify(int bucketLevel, int bucketIndex, int blockIndex) {
    int trueIdx = getTrueIdx(bucketLevel, bucketIndex, blockIndex);
    staleBits[trueIdx / Integer.SIZE] |= (1L << (trueIdx % Integer.SIZE));
    changed.add(trueIdx / Integer.SIZE);
  }

  // if bit is not set, then block is fresh
  public void refresh(int bucketLevel, int bucketIndex, int blockIndex) {
    int trueIdx = getTrueIdx(bucketLevel, bucketIndex, blockIndex);
    staleBits[trueIdx / Integer.SIZE] &= ~(1L << (trueIdx % Integer.SIZE));
    changed.add(trueIdx / Integer.SIZE);
  }

  public boolean isStale(int bucketLevel, int bucketIndex, int blockIndex) {
    int trueIdx = getTrueIdx(bucketLevel, bucketIndex, blockIndex);
    return (staleBits[trueIdx / Integer.SIZE] & (1L << (trueIdx % Integer.SIZE))) != 0;
  }

  public byte[] diff() {
    Map<Integer, Integer> diff = new HashMap<>();
    for (Integer i : changed) {
      diff.put(i, staleBits[i]);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    try {
      dos.writeInt(diff.size());
      int written = 0;
      for (Map.Entry<Integer, Integer> entry : diff.entrySet()) {
        dos.writeByte(1);
        dos.writeInt(entry.getKey());
        dos.writeInt(entry.getValue());
        ++written;
      }
      for (int i = written; i < maxStaleChanged; ++i) {
        dos.write(0);
        dos.writeInt(0);
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
      int entries = dis.readInt();
      for (int i = 0; i < entries; ++i) {
        boolean done = dis.readByte() == 0;
        if (done) {
          break;
        }
        int offset = dis.readInt();
        int value = dis.readInt();
        staleBits[offset] = value;
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  @Override
  public StaleMap clone() {
    return new StaleMap(staleBits.clone(), bucketSlots, maxStaleChanged);
  }

}
