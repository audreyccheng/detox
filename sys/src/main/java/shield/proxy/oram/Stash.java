package shield.proxy.oram;

import shield.proxy.oram.recover.ValidMap;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The client-side stash for the ORAM.
 *
 * @author soumya
 */
public class Stash {

  /**
   * The key to Block object mapping for blocks that are in the stash.
   */
  private final Map<Long, Block> keyToBlock;
  /**
   * The num_levels of the ORAM tree (where 2^num_levels ~= blockList.size())
   */
  private int num_levels;

  private final int valueSize;

  private final BaseRingOram oram;

  private final int maxStashSize;

  /**
   * Creates a new Stash for the ORAM.
   */
  public Stash(BaseRingOram oram, int maxStashSize) {
    this.oram = oram;
    this.maxStashSize = maxStashSize;
    this.num_levels = oram.getNumLevels();
    this.keyToBlock = new HashMap<>();
    this.valueSize = oram.getValueSize();
  }

  /**
   * Pops as many as Z blocks from the stash that can go into the bucket at a particular height and
   * offset.
   *
   * @param height The height of the bucket to pop blocks off of.
   * @param index  The index of this bucket
   * @param Z      The maximum number of blocks to pop from this stash.
   * @return A list of blocks that have been removed from the stash.
   */
  public void popBlocks(int height, int index, int Z, Map<Long, Integer> positionMap,
                        List<Block> popBlocksList) {
    for (Block b : keyToBlock.values()) {
      int path = 0;
      try {
        path = positionMap.get(b.getKey());
      } catch (NullPointerException e) {
        System.err.printf("%d\n", b.getKey());
        e.printStackTrace();
        System.exit(1);
      }
      if (path >> (num_levels - height) == index) {
        popBlocksList.add(b);
        if (popBlocksList.size() == Z) {
          break;
        }
      }
    }

    for (Block b : popBlocksList) {
      keyToBlock.remove(b.getKey());
    }
  }

  /**
   * Adds a block to the stash.
   *
   * @param b The block to add to the stash.
   */
  public void addBlock(Block b) {
    if (!b.isDummy && !b.isStale()) {
      Block existing = keyToBlock.put(b.getKey(), b);
      assert existing == null;
    }
  }

  public void deleteBlock(Long key) {
    assert keyToBlock.remove(key) != null;
  }

  /**
   * Gets the block corresponding to this key, null if it doesn't exist.
   *
   * @param key The key for the block that you want to get.
   * @return The block corresponding to this key.
   */
  public Block getBlock(Long key) {
    if (keyToBlock.containsKey(key)) {
      return keyToBlock.get(key);
    }
    return null;
  }

  public long getStashSize() {
    return keyToBlock.size();
  }

  public byte[] serialize() {
    System.err.printf("Stash size: %d (max: %d).\n", keyToBlock.size(), maxStashSize);
    assert keyToBlock.size() <= maxStashSize;
    ByteArrayOutputStream bos = new ByteArrayOutputStream(keyToBlock.size() * (1 + Long.BYTES + valueSize));
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(maxStashSize);
      int written = 0;
      for (Map.Entry<Long, Block> entry : keyToBlock.entrySet()) {
        dos.write(1);
        dos.writeLong(entry.getKey());
        dos.write(entry.getValue().getValue());
        ++written;
      }
      byte[] dummy = new byte[valueSize];
      for (int i = written; i < maxStashSize; ++i) {
        dos.write(0);
        dos.writeLong(0L);
        dos.write(dummy);
      }
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return bos.toByteArray();
  }

  public static Stash deserialize(byte[] b, BaseRingOram oram, int maxStashSize) {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    Stash stash = new Stash(oram, maxStashSize);
    try {
      int entries = dis.readInt();
      byte[] value = new byte[oram.getValueSize()];
      for (int i = 0; i < entries; ++i) {
        boolean done = dis.readByte() == 0;
        if (done) {
          System.err.printf("Read %d entries from checkpointed stash\n", i);
          break;
        }
        long key = dis.readLong();
        dis.read(value);
        Block block = new Block(key, value, new BlockMask(oram.isEncryptingBlocks(),
            oram.getNonceLength(), oram.getMaskByteArray(), oram.getMaskAlgorithm(), oram.getClientKey()),
            false, 0, true, 0L);
        stash.addBlock(block);
      }
      return stash;
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return null;
  }

}
