package shield.proxy.oram;

/**
 * A listener interface for receiving notifications of the completion of a block read and
 * decryption. Notifications are sent immediately after a block has been read and decrypted, before
 * other procedures are invoked.
 *
 * @author matt
 */
public interface IBlockReadListener {

  /**
   * Called by the thread that just completed the block read and decryption.
   *
   * @param b            The client-side block data structure that contains the read and decrypted value.
   */
  void onBlockRead(Block b);

}
