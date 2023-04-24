package shield.proxy.oram;

import java.util.List;
import java.util.Queue;
import shield.network.messages.Msg;
import shield.proxy.oram.recover.LogEntry;
import shield.proxy.trx.data.Write;

/**
 * A processor for resource-intensive block operations.
 *
 * @author matt
 */
public interface IBlockProcessor {

  /**
   * Reads the value associated with the physical key, decrypts the value, and stores the result in
   * the block.
   *
   * @param b   The client-side block data structure for storing the result.
   */
  void readBlock(Long physicalKey, Long physicalRecoveryKey, Block b, LogEntry recoveryDep);

  /**
   * Reads the value associated with a list of physical keys into the corresponding blocks.
   * Only functional in sync mode
   * @param key
   * @param b
   */
  void readBlockInBatch(List<Long> key, List<Block> b);
  /**
   * Encrypts the value stored in the block and writes the encrypted value.
   *
   * @param b   The block that contains the value to be processed.
   */
  void writeBlock(Long physicalKey, Long physicalRecoveryKey, Block b);

  /**
   * Encrypts the value stored in the block, and returns that value. Does not
   * do the write to the storage
   * @param key
   * @param b
   * @return
   */
  Write writeBlockInBatch(Long key, Block b);

  /**
   * Writes out the different writes in a batch to the storage
   * @param writes
   */
  void flushWrites(Queue<Write> writes);

  void handleRequestResponse(Msg.DataMessageResp msgResp);
}
