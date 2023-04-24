package shield.proxy.oram;

import shield.proxy.oram.enc.MaskAlgorithm;
import shield.proxy.oram.recover.LogEntry;
import shield.util.Utility;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A block in the ORAM data structure. This block could be in the stash or may just be a reference
 * to a block in the BackingStore.
 *
 * @author soumya
 */
public class Block {

  enum BlockOrigin {
    READ_PATH,
    EVICT_PATH,
    EARLY_RESHUFFLE,
  };

  /**
   * Whether or not this block is a dummy block.
   */
  public final boolean isDummy;
  private final BlockMask mask;
  /**
   * The value for this block- may be null if it is not in the stash.
   */
  private byte[] values;
  /**
   * Whether or not this block is valid (i.e. whether or not I'm allowed to access this from remote
   * storage)
   */
  private boolean valid;
  /**
   * The key of this block.
   */
  private Long key;
  private boolean stale;
  private boolean cached;
  Integer version;
  private boolean deterministicNonce;
  private Bucket currBucket;
  private LogEntry currLogEntryDep;
  private BlockOrigin origin;
  private Long physicalKey;
  private boolean computedNonce;

  /**
   * Constructs a new Block
   *
   * @param key   The key for this block
   * @param value The value of this block- cannot be null.
   * @param mask  The BlockMask that will be used to encrypty/decrypt values.
   */
  public Block(Long key, byte[] value, BlockMask mask, boolean isDummy, Integer version, boolean deterministicNonce, Long physicalKey) {
    this.key = key;
    this.values = value;
    this.valid = false;
    this.mask = mask;
    this.isDummy = isDummy;
    this.stale = false;
    this.cached = false;
    this.version = version;
    this.deterministicNonce = deterministicNonce;
    this.origin = null;
    this.physicalKey = physicalKey;
    this.computedNonce = false;
  }

  /**
   * Creates a dummy block.
   *
   * @return A Block instance of a dummy block.
   */
  public static Block createDummyBlock(Long dummyKey, byte[] dummyValue, boolean encrypt, int nonceLength, ThreadLocal<byte[]> maskByteArray, ThreadLocal<MaskAlgorithm> maskAlgorithm, byte[] clientKey) {
    Block b = new Block(dummyKey, dummyValue, new BlockMask(encrypt, nonceLength, maskByteArray, maskAlgorithm, clientKey), true, 0, false, 0L);
    b.valid = true;
    return b;
  }

  public static Block copyToNewVersion(Block b) {
    assert !b.isDummy;
    Block newB = new Block(b.getKey(), null, b.mask.copy(), false, b.version + 1, b.deterministicNonce, b.physicalKey);
    newB.currBucket = b.currBucket;
    return newB;
  }

  /**
   * @return The key of this Block
   */
  public Long getKey() {
    return key;
  }

  /**
   * @return true if value is null, false otherwise.
   */
  public boolean isValid() {
    return valid;
  }

  public void invalidate() {
    this.valid = false;
  }

  public void validate() {
    this.valid = true;
  }

  public boolean isCached() {
    return cached;
  }

  public void setCached(boolean cached) {
    this.cached = cached;
  }

  public void setCurrBucket(Bucket bucket) { this.currBucket = bucket; }

  private int currIdx;
  public void setCurrIndex(int idx) { this.currIdx = idx; }

  public Long getCurrPhysicalKey() { return currBucket.getKeyForIndex(currIdx); }

  public void setPhysicalKey(Long key) { this.physicalKey = key; }

  public void setOrigin(BlockOrigin origin) {
    this.origin = origin;
  }

  public BlockOrigin getOrigin() {
    return origin;
  }

  /**
   * @return The value associated with this Block, null if it's not in the stash.
   */
  public byte[] getValue() {
    return values;
  }

  /**
   * Sets the value for this block to something new.
   *
   * @param value: The value to set this block to.
   */
  public void setValue(byte[] value) {
    values = value;
  }

  public void staleify() {
    this.stale = true;
  }

  public boolean isStale() {
    return stale;
  }

  /**
   * Encrypts the value contained in this block with the given nonce and returns it. Additionally,
   * it sets the value contained in this block to null.
   *
   * @return The encrypted value.
   */
  public byte[] encryptAndClearValue(Random rng) {
    assert values != null;

    byte[] oldValue;
    if (isDummy) {
      oldValue = values.clone();
      // we keep the reference to the dummy value on the client because all dummy blocks refer to the
      // same byte[] object
    } else {
      oldValue = values;
      values = null;
    }
    if (deterministicNonce) {
      computeDeterministicNonce(this.mask.getNonce());
    } else {
      this.mask.generateNonce(rng);
    }
    mask.mask(oldValue);
    return oldValue;
  }

  /**
   * Resets the value for this block.
   * <p>
   * Precondition is that encryptAndClearValue was called before this function, and it's output was
   * passed in as the value argument.
   *
   * @param value: The encrypted value for this block, must be the return value of
   *               encryptAndClearValue()
   */
  public void decryptAndSetValue(byte[] value) {
    assert value != null;
    assert !isDummy;

    if (!isDummy && values == null) {
      if (!computedNonce) {
        computeDeterministicNonce(this.mask.getNonce());
      }

      this.mask.unmask(value);
      values = value;
    }
    // otherwise, this block's value has already been overwritten by a subsequent write and we don't want to lose
    // the write's newer value for the block / or this is a dummy block and it's value is unchanged
  }

  private void computeDeterministicNonce(byte[] nonce) {
    computedNonce = true;
    assert nonce.length >= 8;
    for (int i = 0; i < 8; ++i) {
      nonce[i] = (byte) ((physicalKey >> (8 * i)) & 0xFF);
    }
  }

  public void setCurrLogEntryDep(LogEntry dep) {
    this.currLogEntryDep = dep;
  }

  public LogEntry getCurrLogEntryDep() {
    return currLogEntryDep;
  }
}
