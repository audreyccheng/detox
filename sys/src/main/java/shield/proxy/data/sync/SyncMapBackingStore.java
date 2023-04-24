package shield.proxy.data.sync;

import com.google.protobuf.ByteString;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import shield.BaseNode;
import shield.config.NodeConfiguration;
import shield.network.messages.Msg.DataMessageResp;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Implements most basic local storage using a map interface.
 *
 * @author ncrooks
 */
public class SyncMapBackingStore implements ISyncBackingStore {

  private final ConcurrentHashMap<Long, byte[]> store;

  private final NodeConfiguration config;

  private int accessCount = 0;

  public SyncMapBackingStore(NodeConfiguration config) {
    store = new ConcurrentHashMap<>(80000000, 0.7f, config.N_WORKER_THREADS);
    this.config = config;
  }

  public SyncMapBackingStore() {
    config = null;
    this.store = new ConcurrentHashMap<>(800000, 0.7f, 1);
  }

  public SyncMapBackingStore(ConcurrentHashMap<Long, byte[]> map) {
    this.config = null;
    this.store = map;
  }

  public SyncMapBackingStore(Map<Long,byte[]> map) {
      this.config = null;
      this.store  = null;
      // TODO(natacha): Fix
  }


  @Override
  public byte[] read(Long key) {
    byte[] result = null;
    long begin = 0;
    if (config != null && config.LOG_STORAGE_TIME) {
      begin = System.nanoTime();
    }
    if (store.containsKey(key)) {
      result =  store.get(key);
    }

    if (config != null && config.LOG_STORAGE_TIME) {
      long end = System.nanoTime();
      config.statsStorageTime.addPoint("READ "  + (end-begin));
    }

    if (config != null && config.LOG_STORAGE_SIZE && accessCount++ % 500 ==0) {
      config.statsStorageSize.addPoint(store.size());
    }
    return result;
  }

  public List<byte[]> read(List<Long> keys) {
    List<byte[]> toReturn = new ArrayList<byte[]>();

    for (Long key : keys) {
      if (store.containsKey(key)) {
        toReturn.add(store.get(key));
      } else {
        toReturn.add(null);
      }
    }

    return toReturn;
  }

  @Override
  public void write(Write write) {
    Long key;
    byte[] value;
    long begin = 0;
    if (config != null && config.LOG_STORAGE_TIME) {
      begin = System.nanoTime();
      config.statsStorageTime.addBegin();
    }
    key = write.getKey();
    if (write.isDelete()) {
      store.remove(key);
      if (config != null && config.LOG_STORAGE_TIME) {
        long end = System.nanoTime();
        config.statsStorageTime.addPoint("DELETE "  + (end-begin) + " " + write.getValue().length);
      }
    }
    else {
      value = write.getValue();
      store.put(key, value);
      if (config != null && config.LOG_STORAGE_TIME) {
        long end = System.nanoTime();
        config.statsStorageTime.addPoint("WRITE "  + (end-begin) + " " + write.getValue().length);
      }
    }

    if (config != null && config.LOG_STORAGE_SIZE && accessCount++ % 500 ==0) {
      config.statsStorageSize.addPoint(store.size());
    }
  }

  public void write(Queue<Write> writes) {
    for (Write w : writes) {
      write(w);
    }
  }

  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {
    throw new RuntimeException("Unimplemented");
  }

}
