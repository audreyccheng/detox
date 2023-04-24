package shield.proxy.data.async;

import com.google.protobuf.ByteString;

import java.util.*;

import shield.BaseNode;
import shield.network.messages.Msg.DataMessageResp;
import shield.proxy.trx.data.Write;
import shield.util.Pair;
import shield.util.Utility;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Implements most basic local storage using a map interface.
 *
 * @author ncrooks
 */
public class AsyncMapBackingStore implements IAsyncBackingStore {

  private final Map<Long, byte[]> store;

  private final BaseNode node;

  public AsyncMapBackingStore(BaseNode node) {
    this(node, new ConcurrentHashMap<Long, byte[]>(1000000, 0.9f));
  }

  public AsyncMapBackingStore(BaseNode node,
      Map<Long, byte[]> map) {
    assert node != null;
    this.store = map;
    this.node = node;
  }

  @Override
  public void read(Long key, AsyncDataRequest req) {
    if (store.containsKey(key)) {
      byte[] value = store.get(key);
      req.setReadValue(value);
    } else {
      req.setReadValue(null);
    }
    node.executeAsync(() -> req.onDataRequestCompleted(),
        node.getDefaultExpHandler());
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
    node.executeAsync(() -> req.onDataRequestCompleted(),
        node.getDefaultExpHandler());
  }

  @Override
  public void write(Write write, AsyncDataRequest req) {
    byte[] value = write.getValue();
    Long key = write.getKey();
    if (write.isDelete()) {
      store.remove(key);
    } else {
      store.put(key, value);
    }
    node.executeAsync(() -> req.onDataRequestCompleted(),
        node.getDefaultExpHandler());
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
    node.executeAsync(() -> req.onDataRequestCompleted(),
        node.getDefaultExpHandler());
  }

  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {
    throw new RuntimeException("Unimplemented");
  }

}
