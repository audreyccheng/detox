package shield.proxy.oram.recover;

import java.io.*;
import java.util.*;

public class ValidMap {

  private final int bucketSlots;

  public final int[] validBits;

  private final Set<Integer> changed;

  public ValidMap(int levels, int bucketSlots) {
    this.bucketSlots = bucketSlots;
    this.validBits = new int[((((1 << levels) - 1) * bucketSlots) + Integer.SIZE - 1) / Integer.SIZE];
    this.changed = new HashSet<>();
  }

  private ValidMap(int[] validBits, int bucketSlots) {
    this.validBits = validBits;
    this.bucketSlots = bucketSlots;
    this.changed = new HashSet<>();
  }

  public void clearChanged() {
    this.changed.clear();
  }

  public static ValidMap deserialize(byte[] b, int bucketSlots) {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    try {
      int longs = dis.readInt();
      int[] validBits = new int[longs];
      for (int i = 0; i < longs; ++i) {
        validBits[i] = dis.readInt();
      }
      return new ValidMap(validBits, bucketSlots);
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return null;
  }

  public byte[] serialize() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(Integer.BYTES + validBits.length * Long.BYTES);
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(validBits.length);
      for (int i = 0; i < validBits.length; ++i) {
        dos.writeInt(validBits[i]);
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

  // if bit is set, then block is invalid
  public void invalidate(int bucketLevel, int bucketIndex, int blockIndex) {
    int trueIdx = getTrueIdx(bucketLevel, bucketIndex, blockIndex);
    validBits[trueIdx / Integer.SIZE] |= (1L << (trueIdx % Integer.SIZE));
    changed.add(trueIdx / Integer.SIZE);
  }

  // if bit is not set, then block is valid
  public void validate(int bucketLevel, int bucketIndex, int blockIndex) {
    int trueIdx = getTrueIdx(bucketLevel, bucketIndex, blockIndex);
    validBits[trueIdx / Integer.SIZE] &= ~(1L << (trueIdx % Integer.SIZE));
    changed.add(trueIdx / Integer.SIZE);
  }

  public boolean isValid(int bucketLevel, int bucketIndex, int blockIndex) {
    int trueIdx = getTrueIdx(bucketLevel, bucketIndex, blockIndex);
    return (validBits[trueIdx / Integer.SIZE] & (1L << (trueIdx % Integer.SIZE))) == 0;
  }

  public byte[] diff() {
    Map<Integer, Integer> diff = new HashMap<>();
    for (Integer i : changed) {
      diff.put(i, validBits[i]);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    try {
      dos.writeInt(diff.size());
      for (Map.Entry<Integer, Integer> entry : diff.entrySet()) {
        dos.writeInt(entry.getKey());
        dos.writeInt(entry.getValue());
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
        int offset = dis.readInt();
        int value = dis.readInt();
        validBits[offset] = value;
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  @Override
  public ValidMap clone() {
    return new ValidMap(validBits.clone(), bucketSlots);
  }
}
