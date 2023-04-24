package shield.proxy.oram;

import shield.config.NodeConfiguration;
import shield.network.messages.Msg;
import shield.proxy.data.async.AsyncSyncBackingStore;
import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.oram.AsyncRingOram.OpType;
import shield.proxy.trx.data.Write;

import java.util.*;

public class SyncRingOram extends BaseRingOram implements ISyncBackingStore {

  private byte[] currentReadValue;

  private SyncRingOram(Random rng, ISyncBackingStore store, NodeConfiguration config) {
    super(rng, new SyncBlockProcessor(store, rng), new AsyncSyncBackingStore(store), config);
  }

  private SyncRingOram(int L, Random rng, ISyncBackingStore store, NodeConfiguration config) {
    super(config, L, rng, new SyncBlockProcessor(store, rng), new AsyncSyncBackingStore(store));
  }

  public static SyncRingOram create(Random rng, ISyncBackingStore store,NodeConfiguration config) {
    SyncRingOram oram = new SyncRingOram(rng, store, config);
    oram.init();
    return oram;
  }

  public static SyncRingOram create(int L, Random rng, ISyncBackingStore store, NodeConfiguration config) {
    SyncRingOram oram = new SyncRingOram(L, rng, store, config);
    oram.init();
    return oram;
  }


  @Override
  public byte[] read(Long key) {
    return read(Arrays.asList(key)).get(0);
  }

  @Override
  public List<byte[]> read(List<Long> keys) {
    List<byte[]> toReturn = new ArrayList<>(keys.size());
    doReadBatch(keys, () -> {
      toReturn.add(currentReadValue);
    });
    return toReturn;
  }

  @Override
  public void write(Write write) {
    write(new ArrayDeque<>(Arrays.asList(write)));
  }

  @Override
  public void write(Queue<Write> writes) {
    doWriteBatch(writes);
  }

  @Override
  public void onHandleRequestResponse(Msg.DataMessageResp msgResp) {
    getBlockProcessor().handleRequestResponse(msgResp);
  }

  @Override
  public void onReadPathComplete(Long key, byte[] value, OpType opType, Integer oldPos, Block b,
                                 boolean wasInTree, boolean wasCached) {
    if (opType == OpType.READ) {
      // because our backing store and block processor are synchronous, the block corresponding to this key
      // that exists in the stash is guaranteed to have the value that we are currently reading (as long as
      // the block has been written to at least once)
      if (b == null) {
        currentReadValue = null;
      } else {
        currentReadValue = b.getValue().clone();
      }
    }
  }

}

