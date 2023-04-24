package shield.proxy.data.sync;

import com.google.protobuf.ByteString;
import java.util.Queue;
import shield.BaseNode;
import shield.network.messages.Msg.DataMessageResp;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Implements dummy storage.
 *
 * @author ncrooks
 */
public class SyncDummyBackingStore implements ISyncBackingStore {

  private final byte[] value;

  public SyncDummyBackingStore(int keySize) {
    System.out.println("Creating Sync Dummy Backing Store ");
    this.value = new byte[keySize];
    new Random().nextBytes(value);
  }


  @Override
  public byte[] read(Long key) {
    return value;
  }

  public List<byte[]> read(List<Long> keys) {
    List<byte[]> toReturn = new ArrayList<byte[]>();

    for (Long key : keys) {
      toReturn.add(value);
    }

    return toReturn;
  }

  @Override
  public void write(Write write) {
  }

  public void write(Queue<Write> writes) {
  }

  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {
    throw new RuntimeException("Unimplemented");
  }

}
