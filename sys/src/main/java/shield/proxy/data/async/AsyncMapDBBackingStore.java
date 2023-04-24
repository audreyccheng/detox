package shield.proxy.data.async;

import java.util.Queue;
import shield.BaseNode;
import shield.config.NodeConfiguration;
import shield.network.messages.Msg.DataMessageResp;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;


/**
 * Implements most basic local storage using a map interface.
 *
 *
 * TODO(natacha): unnecessary copy of Long. Write custom serializer
 *
 * @author ncrooks
 */
public class AsyncMapDBBackingStore implements IAsyncBackingStore {

  private final HTreeMap<Long, byte[]> store;
  private final DB db;

  protected final BaseNode node;

  public AsyncMapDBBackingStore(NodeConfiguration config) {
    node = null;
    db = DBMaker.memoryDirectDB()
        .concurrencyScale(config.N_WORKER_THREADS).make();
    store = db.hashMap(config.KV_NAME)
        .keySerializer(Serializer.LONG)
        .valueSerializer(Serializer.BYTE_ARRAY).create();
  }

  public AsyncMapDBBackingStore(BaseNode node) {
    this.node = node;
    if (node.getConfig().DB_IN_MEMORY) {
      // Option 1
      db = DBMaker.memoryDirectDB()
          .concurrencyScale(node.getConfig().N_WORKER_THREADS).make();
      store = db.hashMap(node.getConfig().KV_NAME)
          .keySerializer(Serializer.LONG)
          .valueSerializer(Serializer.BYTE_ARRAY).create();
    } else {
      db = DBMaker
          .fileDB(node.getConfig().DB_FILE_NAME)
          .checksumHeaderBypass()
           // .fileMmapEnableIfSupported()
          .concurrencyScale(node.getConfig().N_WORKER_THREADS)
          .closeOnJvmShutdown()
          .make();
      store =
          db.hashMap(node.getConfig().KV_NAME)
              .keySerializer(Serializer.LONG)
              .valueSerializer(Serializer.BYTE_ARRAY).createOrOpen();
      }
  }


  @Override
  public void read(Long key, AsyncDataRequest req) {
    if (store.containsKey(key)) {
      byte[] value = store.get(key);
      req.setReadValue(value);
    } else {
      req.setReadValue(null);
    }
    if (node != null) {
      node.executeAsync(() -> req.onDataRequestCompleted(),
          node.getDefaultExpHandler());
    }
  }

  @Override
  public void read(List<Long> keys, AsyncDataRequest req) {
    List<byte[]> toReturn = new ArrayList<byte[]>();

    for (Long key : keys) {
      if (store.containsKey(key)) {
        toReturn.add(store.get(key));
      } else {
        toReturn.add(null);
      }
    }
    req.setReadValues(toReturn);
    if (node != null) {
      node.executeAsync(() -> req.onDataRequestCompleted(),
          node.getDefaultExpHandler());
    }
  }

  @Override
  public void write(Write write, AsyncDataRequest req) {
    if (write.isDelete()) {
      store.remove(write.getKey());
    }
     else {
      store.put(write.getKey(), write.getValue());
    }
    if (node != null) {
      node.executeAsync(() -> req.onDataRequestCompleted(),
          node.getDefaultExpHandler());
    }
  }

  public void deleteDB() {
    db.close();
  }

  @Override
  public void write(Queue<Write> writes,
      AsyncDataRequest req) {
    for (Write w : writes) {
      if (w.isDelete()) {
        store.remove(w.getKey());
      } else {
        store.put(w.getKey(), w.getValue());
      }
    }
    if (node != null) {
      node.executeAsync(() -> req.onDataRequestCompleted(),
          node.getDefaultExpHandler());
    }
  }

  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {
    throw new RuntimeException("Unimplemented");
  }


}
