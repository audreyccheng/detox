package shield.proxy.data.sync;

import java.util.Queue;
import shield.network.messages.Msg;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.util.List;


/**
 * Common interfaces to all key-value stores that this system can use. All new kv stores should be
 * registered as an entry in the BackingStoreType, and should update the function in the NodeConfig
 * file.
 *
 * This backing store uses a *synchronous* interface. Threads will wait for pending requests to
 * complete. If not careful, it can lead to starvation.
 *
 * @author ncrooks
 */
public interface ISyncBackingStore {

  enum BackingStoreType {
    ORAM_SEQ_MAPDB, ORAM_SEQ_HASHMAP, ORAM_SEQ_DYNAMO, ORAM_SEQ_SERVER, ORAM_PAR_MAPDB, ORAM_PAR_HASHMAP, ORAM_PAR_DYNAMO, ORAM_PAR_SERVER, NORAM_MAPDB, NORAM_HASHMAP, NORAM_DYNAMO, NORAM_SERVER
  }

  byte[] read(Long key);

  List<byte[]> read(List<Long> key);

  void write(Write write);

  void write(Queue<Write> writes);

  void onHandleRequestResponse(Msg.DataMessageResp msgResp);

}
