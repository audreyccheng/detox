package shield.proxy.oram;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import shield.proxy.oram.recover.ReadPath;
import shield.proxy.trx.data.Write;

import static shield.proxy.oram.Block.BlockOrigin.EARLY_RESHUFFLE;
import static shield.proxy.oram.Block.BlockOrigin.EVICT_PATH;
import static shield.proxy.oram.Block.BlockOrigin.READ_PATH;

/**
 * A bucket in the ORAM tree.
 *
 * @author soumya
 */
public class Bucket {

  /**
   * The level in the ORAM tree that this bucket is in.
   */
  private final int level;

  /**
   * The offset in the level that this bucket is in. Ranges from 0 to 2^level - 1
   */
  private final int index;
  /**
   * The list of blocks that are in this bucket.
   */
  final List<Block> blocks;
  /**
   * Contains a string representation of "[height]-[offset]-[index]".
   */
  private final Long[] keys;
  /**
   * The keys in this bucket to the offset that this key is in the bucket.
   */
  private final Map<Long, Integer> keyToOffset;
  private final Map<Long, Integer> permutation;
  /**
   * The ORAM instance for this bucket.
   */
  private final BaseRingOram oram;
  /**
   * The list of dummy indices in this bucket that haven't been accessed yet.
   */
  private final List<Integer> unreadDummyIndices;
  private final List<Integer> dummyIndices;
  private final int[] indicesToRead;
  private final List<Integer> unreadRealIndices;
  /**
   * Number of times this bucket was accessed. Must be <= S.
   */
  private int numAccessed;
  /**
   * Stat Collection
   */
  private int skipped;

  private int written;

  private boolean recovered;

  public Bucket(BaseRingOram oram, int height, int offset) {
    this.oram = oram;
    this.numAccessed = 0;
    this.skipped = 0;
    this.written = 0;
    this.level = height;
    this.index = offset;
    this.blocks = new ArrayList<>(oram.getZ() + oram.getS());
    this.keys = new Long[oram.getZ() + oram.getS()];
    long l = ((long) index) << 16;
    l |= ((long) level & 0xFFFF) << 48;
    for (int i = 0; i < keys.length; ++i) {
      l &= 0xFFFFFFFFFFFF0000L;
      l |= i & 0xFFFF;
      keys[i] = Long.valueOf(l);
    }
    this.keyToOffset = new HashMap<>(oram.getZ());
    this.permutation = new HashMap<>(oram.getZ());
    this.unreadDummyIndices = new ArrayList<>(oram.getZ() + oram.getS());
    this.unreadRealIndices = new ArrayList<>(oram.getZ());
    this.dummyIndices = new ArrayList<>(oram.getZ() + oram.getS());
    for (int i = 0; i < oram.getZ() + oram.getS(); ++i) {
      Block b = Block
          .createDummyBlock(oram.getDummyKey(), oram.getDummyValue(), oram.isEncryptingBlocks(),
              oram.getNonceLength(), oram.getMaskByteArray(), oram.getMaskAlgorithm(), oram.getClientKey());
      blocks.add(b);
      Integer ii = oram.getIntObject(i);
      dummyIndices.add(ii);
      unreadDummyIndices.add(ii);
    }
    this.indicesToRead = new int[oram.getZ()];
    this.recovered = !oram.isDurable();
  }

  /**
   * Gets the index of this level that this bucket is at.
   *
   * @return The index of this bucket.
   */
  public int getIndex() {
    return this.index;
  }

  /**
   * Gets the level of the ORAM tree this bucket is in.
   *
   * @return The level of this bucket.
   */
  public int getLevel() {
    return this.level;
  }

  /**
   * The number of times this bucket has been accessed. Must be <= S.
   *
   * @return The number of times that this block has been accessed.
   */
  public int getNumAccessed() {
    return numAccessed;
  }

  /**
   * Empties the stash back to the backing store and resets the number of accesses to 0. If there
   * aren't enough real blocks in the stash, then writeBucket creates enough dummy blocks to fill
   * out the bucket.
   * <p>
   * This is the same as the writeBucket primitive in the RingORAM paper, except that this also
   * resets the number of accesses to 0.
   *
   * @param s   The stash that is going to be emptied into this bucket.
   * @param rng The random number generator that is used to generate new random
   */
  public void writeBucket(Stash s, Random rng, Map<Long, Integer> positionMap, boolean evictPath) {
    assert recovered;
    //System.err.printf("Writing bucket %d %d\n", level, index);

    Queue<Write> bufferedWrites;

    bufferedWrites = oram.getBufferOps()? new LinkedList<Write>() : null;

    List<Block> evictBlocks = oram.getEvictBlocksList();
    evictBlocks.clear();

    // Grab at most Z real blocks from the stash
    // beginInt = System.nanoTime();
    s.popBlocks(this.level, this.index, oram.getZ(), positionMap, evictBlocks);
    // System.out.println("Pop" + (System.nanoTime() - beginInt));

    while (evictBlocks.size() < oram.getZ() + oram.getS()) {
      evictBlocks.add(null);
    }

    Collections.shuffle(evictBlocks, rng);

    this.unreadDummyIndices.clear();
    this.dummyIndices.clear();

    this.keyToOffset.clear();
    this.permutation.clear();
    this.unreadRealIndices.clear();

    this.blocks.clear();
    this.blocks.addAll(evictBlocks);

    Block b;
    for (int i = 0; i < blocks.size(); i++) {
      b = blocks.get(i);

      Integer ii = oram.getIntObject(i);
      if (b == null) { // we need to place a dummy block at this index
        b = Block.createDummyBlock(oram.getDummyKey(), oram.getDummyValue(), oram.isEncryptingBlocks(),
            oram.getNonceLength(), oram.getMaskByteArray(), oram.getMaskAlgorithm(), oram.getClientKey());
        blocks.set(i, b);
      }
      b.setCurrBucket(this);
      b.setCurrIndex(i);

      if (b.isDummy) {
        unreadDummyIndices.add(ii);
        dummyIndices.add(ii);
      } else {
        permutation.put(b.getKey(), ii);
        keyToOffset.put(b.getKey(), ii);
        unreadRealIndices.add(ii);
      }

      if (oram.isWriteEndBatchEnabled()) {
        assert !b.isStale();
        oram.addDelayedWrite(getPartialKeyForIndex(i), b);
        b.setCached(true);
      } else {
        if (!oram.getBufferOps()) {
          oram.getBlockProcessor().writeBlock(getPartialKeyForIndex(i), getKeyForIndex(i), b);
          if (oram.config.LOG_STORAGE_ACCESSES_PER_BATCH) {
            oram.storageWrites.incrementAndGet();
            if (evictPath) {
              oram.storageWritesEvictPath.incrementAndGet();
            } else {
              oram.storageWritesEarlyReshuffle.incrementAndGet();
            }
          }
        } else {
          Write write = oram.getBlockProcessor().writeBlockInBatch(getKeyForIndex(i), b);
          bufferedWrites.add(write);
        }
      }


      b.validate();
      if (oram.isDurable()) {
        oram.validMap.validate(level, index, i);
        oram.staleMap.refresh(level, index, i);
      }
    }
    numAccessed = 0;
    ++written;
    assert unreadDummyIndices.size() + unreadRealIndices.size() == oram.getZ() + oram.getS();

    if (oram.getBufferOps()) {
      oram.getBlockProcessor().flushWrites(bufferedWrites);
    }

    if (oram.isDurable()) {
      oram.permutationMap.updatePermutation(level, index, permutation);
    }
  }

  /**
   * Read all real blocks from this bucket and puts it into the stash.
   * <p>
   * All reads are done in order of the offset in this bucket.
   *  @param s   the stash for this ORAM
   * @param rng the random number generator used for this ORAM.
   */
  public void readBucket(Stash s, Random rng, boolean evictPath) {
    if (oram.isDurable() && !recovered) {
      recover();
    }
    Block b;
    int reals = 0;
    List<Long> keysToRead = null;
    List<Block> blocksToRead = null;

    if (oram.getBufferOps()) {
      keysToRead = new LinkedList<>();
      blocksToRead = new LinkedList<>();
    }

    // Get all of the real blocks in this bucket.
    for (Map.Entry<Long, Integer> e : keyToOffset.entrySet()) {
      b = blocks.get(e.getValue());

      if (b.isValid()) {
        indicesToRead[reals] = e.getValue();
        reals++;
      }
    }

    assert reals <= oram.getZ();

    int dummies = 0;
    if (!oram.isReadPathAllRealEnabled()) {
      // Fill out indices with the dummy blocks.
      while (reals + dummies < oram.getZ()) {
        int i = rng.nextInt(unreadDummyIndices.size());
        indicesToRead[reals + dummies] = unreadDummyIndices.remove(i);
        dummies++;
      }
    }
    assert reals + dummies == oram.getZ();

    int k = reals + dummies;

    // Sort the indices to hide the real blocks.
    Arrays.sort(indicesToRead, 0, k);

    for (int i = 0; i < k; ++i) {
      b = blocks.get(indicesToRead[i]);

      b.invalidate();

      if (oram.isDurable()) {
        oram.validMap.invalidate(level, index, i);
      }

      Block newBlock = b;
      if (!b.isDummy && !b.isCached()) {
        newBlock = Block.copyToNewVersion(b);
        if (b.isStale()) {
          newBlock.staleify();
        }
      }

      if (!oram.isWriteEndBatchEnabled() || !newBlock.isCached()) {
        if (!oram.getBufferOps()) {
          //System.err.printf("r: ");
          oram.getBlockProcessor().readBlock(getPartialKeyForIndex(indicesToRead[i]), getKeyForIndex(indicesToRead[i]), newBlock, null);
          if (oram.config.LOG_STORAGE_ACCESSES_PER_BATCH) {
            oram.storageReads.incrementAndGet();
            if (evictPath) {
              oram.storageReadsEvictPath.incrementAndGet();
            } else {
              oram.storageReadsEarlyReshuffle.incrementAndGet();
            }
          }
        } else {
          keysToRead.add(getKeyForIndex(indicesToRead[i]));
          blocksToRead.add(newBlock);
        }
      }

      if (oram.isWriteEndBatchEnabled()) {
        oram.removeDelayedWrite(getPartialKeyForIndex(indicesToRead[i]));
      }

      oram.beforeAddStashReadBucket(newBlock);
      s.addBlock(newBlock);
    }

    if (oram.getBufferOps()) {
      oram.getBlockProcessor().readBlockInBatch(keysToRead,blocksToRead);
    }
  }

  /**
   * Gets the block offset that this key has in this bucket. If the key doesn't exist in this bucket
   * and this bucket is tree-top cached, returns -1. If the key doesn't exist in this bucket and
   * this bucket is not tree-top cached, returns a random valid dummy index.
   *
   * @param key The key to look for in this bucket
   * @param rng The random number generator to use for the dummy block
   * @return The block offset for the real key, or the offset for a dummy block.
   */
  public Integer getBlockOffset(Long key, Random rng) {
    if (oram.isDurable() && !recovered) {
      recover();
    }
    Integer realBlockOffset = keyToOffset.get(key);
    if (realBlockOffset != null && blocks.get(realBlockOffset).isValid() && !blocks.get(realBlockOffset).isStale()) {
      this.unreadRealIndices.remove(realBlockOffset);
      keyToOffset.remove(key);
      return realBlockOffset;
    } else if (oram.isReadPathAllRealEnabled()) {
      if (unreadRealIndices.size() == 0) {
        skipped++;
        return -1;
      } else {
        int randomRealIndex = rng.nextInt(this.unreadRealIndices.size());
        Integer realIndex = this.unreadRealIndices.get(randomRealIndex);
        this.unreadRealIndices.remove(randomRealIndex);
        keyToOffset.remove(blocks.get(realIndex).getKey());
        return realIndex;
      }
    } else {
      int randomDummyIndex = rng.nextInt(this.unreadDummyIndices.size());
      Integer dummyIndex = this.unreadDummyIndices.get(randomDummyIndex);
      this.unreadDummyIndices.remove(randomDummyIndex);
      return dummyIndex;
    }
  }

  /**
   * Reads a block at an offset, decrypts it and places it in the stash.
   *
   * @param s      The stash for this ORAM
   * @param offset The offset to read this Bucket at.
   */
  public void readBlockAtOffset(Stash s, int offset, Long key, BlockLocInfo blockLocInfo, ReadPath readPath,
                                Function<Block, Void> beforeReadBlock) {
    assert recovered;

    Block b = blocks.get(offset);

    assert b.isValid();
    assert !b.isStale();

    b.invalidate();

    if (oram.isDurable()) {
      oram.validMap.invalidate(level, index, offset);
    }

    boolean readDesiredBlock = false;
    Block newBlock = b;
    if (!b.isDummy) {
      if (!b.isCached()) {
        newBlock = Block.copyToNewVersion(b);
      }
      if (newBlock.getKey().equals(key)) {
        readDesiredBlock = true;
      }
      beforeReadBlock.apply(newBlock);
    }

    if (!oram.isWriteEndBatchEnabled() || !newBlock.isCached()) {
      oram.getBlockProcessor().readBlock(getPartialKeyForIndex(offset), getKeyForIndex(offset), newBlock, readPath);
      if (oram.config.LOG_STORAGE_ACCESSES_PER_BATCH) {
        oram.storageReads.incrementAndGet();
        oram.storageReadsReadPath.incrementAndGet();
      }
    }
    if (readDesiredBlock) {
      blockLocInfo.wasCached = newBlock.isCached();
      blockLocInfo.wasInTree = true;
    }

    if (oram.isWriteEndBatchEnabled()) {
      oram.removeDelayedWrite(getPartialKeyForIndex(offset));
    }

    s.addBlock(newBlock);

    numAccessed += 1;
  }

  public boolean staleifyBlock(Long key, Stash s) {
    Integer realBlockOffset = keyToOffset.get(key);
    if (realBlockOffset != null) {
      Block b = blocks.get(realBlockOffset);
      if (b.isValid()) {
        b.staleify();

        if (oram.isDurable()) {
          oram.staleMap.staleify(level, index, realBlockOffset);
        }

        Block newBlock = Block.copyToNewVersion(b);
        newBlock.setValue(null);
        s.addBlock(newBlock);

      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Stat Collection
   */
  public int getNumSkipped() {
    return skipped;
  }

  Long getKeyForIndex(int idx) {
    if (oram.isDurable()) {
      return oram.computePhysicalKeyForBucketIndex(level, index, idx);
    } else {
      return keys[idx];
    }
  }

  private Long getPartialKeyForIndex(int idx) {
    if (oram.isDurable()) {
      return oram.computePartialPhysicalKeyForBucketIndex(level, index, idx);
    } else {
      return keys[idx];
    }
  }

  void recover() {
    keyToOffset.clear();
    keyToOffset.putAll(oram.permutationMap.getPermutation(level, index));
    Map<Integer, Long> invertedPerm = keyToOffset.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    for (int i = 0; i < blocks.size(); ++i) {
      Long key = invertedPerm.get(i);
      if (oram.validMap.isValid(level, index, i)) {
        if (key != null) {
          Block b = new Block(key, null, new BlockMask(oram.isEncryptingBlocks(),
              oram.getNonceLength(), oram.getMaskByteArray(), oram.getMaskAlgorithm(), oram.getClientKey()),
              false, 0, true, 0L);
          Long pkey = getKeyForIndex(i);
          b.setPhysicalKey(pkey);
          b.setCurrBucket(this);
          blocks.set(i, b);
          unreadDummyIndices.remove(unreadDummyIndices.indexOf(oram.getIntObject(i)));
          unreadRealIndices.add(oram.getIntObject(i));
        }
        blocks.get(i).validate();
      } else {
        unreadDummyIndices.remove(unreadDummyIndices.indexOf(oram.getIntObject(i)));
        ++numAccessed;
      }
      if (oram.staleMap.isStale(level, index, i)) {
        blocks.get(i).staleify();
      }
    }
    recovered = true;
  }
}
