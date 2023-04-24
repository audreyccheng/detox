package shield.proxy.data.async;

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
public interface IAsyncBackingStore {

  enum BackingStoreType {
    ORAM_SEQ_MAPDB,
    ORAM_SEQ_HASHMAP,
    ORAM_SEQ_SERVER,
    ORAM_SEQ_DUMMY,
    ORAM_PAR_MAPDB,
    ORAM_PAR_HASHMAP,
    ORAM_PAR_SERVER,
    ORAM_PAR_DUMMY,
    NORAM_MAPDB,
    NORAM_HASHMAP,
    NORAM_SERVER,
    NORAM_DUMMY

  }


  void read(Long key, AsyncDataRequest req);

  void read(List<Long> key, AsyncDataRequest req);

  void write(Write write,
      AsyncDataRequest req);

  void write(Queue<Write> writes,
      AsyncDataRequest req);

  void onHandleRequestResponse(Msg.DataMessageResp msgResp);

}
