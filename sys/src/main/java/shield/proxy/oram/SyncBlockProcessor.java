package shield.proxy.oram;

import java.util.*;

import shield.network.messages.Msg;
import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.oram.recover.LogEntry;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;

/**
 * A simple synchronous block processor.
 *
 * @author matt
 */
public class SyncBlockProcessor implements IBlockProcessor {

  /**
   * The storage to which this processor will send block read and write requests.
   */
  private final ISyncBackingStore store;

  /**
   * The random number generator this processor will use to generate new nonces.
   */
  private final Random rng;

  /**
   * Constructs a new SyncBlockProcessor instance.
   *
   * @param store The storage to which this processor will send block read and write requests.
   * @param rng   The random number generator this processor will use to generate new nonces.
   */
  public SyncBlockProcessor(ISyncBackingStore store, Random rng) {
    this.store = store;
    this.rng = rng;
  }

  /**
   * Synchronously reads and decrypts the block.
   *
   * @param b   The client-side block data structure for storing the result.
   */
  @Override
  public void readBlock(Long physicalKey, Long physicalRecoveryKey, Block b, LogEntry recoveryDep) {
    byte[] value = store.read(physicalRecoveryKey);
    if (!b.isDummy) {
      b.decryptAndSetValue((value == null) ? null : value.clone());
    }
  }

  @Override
  public void readBlockInBatch(List<Long> readRequests, List<Block> blocks) {

    byte[] value;
    Iterator<byte[]> val;
    List<byte[]> values;

    values = store.read(readRequests);
    assert(values.size() == blocks.size());
    val = values.iterator();
    for (Block b: blocks) {
      value = val.next();
      if (!b.isDummy) {
        b.decryptAndSetValue(value.clone());
      }
    }
  }

  /**
   * Synchronously encrypts and writes the block.
   *
   * @param b   The block that contains the value to be processed.
   */
  @Override
  public void writeBlock(Long physicalKey, Long physicalRecoveryKey, Block b) {
    byte[] value = b.encryptAndClearValue(rng);
    store.write(new Write(physicalRecoveryKey, value.clone(), Type.WRITE));
  }

  @Override
  public Write writeBlockInBatch(Long key, Block b) {
    byte[] value = b.encryptAndClearValue(rng);
    return new Write(key, value.clone(), Type.WRITE);
  }

  @Override
  public void flushWrites(Queue<Write> writes) {
    store.write(writes);
  }

  @Override
  public void handleRequestResponse(Msg.DataMessageResp msgResp) {
    store.onHandleRequestResponse(msgResp);
  }
}
