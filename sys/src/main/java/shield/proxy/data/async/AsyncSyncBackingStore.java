package shield.proxy.data.async;

import shield.network.messages.Msg;
import shield.proxy.data.sync.*;
import shield.proxy.trx.data.Write;

import java.util.List;
import java.util.Queue;

public class AsyncSyncBackingStore implements IAsyncBackingStore {

  private final ISyncBackingStore store;

  public AsyncSyncBackingStore(ISyncBackingStore store) {
    this.store = store;
  }

  @Override
  public void read(Long key, AsyncDataRequest req) {
    byte[] value = store.read(key);
    req.setReadValue(value);
    req.onDataRequestCompleted();
  }

  @Override
  public void read(List<Long> keys, AsyncDataRequest req) {
    List<byte[]> values = store.read(keys);
    req.setReadValues(values);
    req.onDataRequestCompleted();
  }

  @Override
  public void write(Write write, AsyncDataRequest req) {
    store.write(write);
    req.onDataRequestCompleted();
  }

  @Override
  public void write(Queue<Write> writes, AsyncDataRequest req) {
    store.write(writes);
    req.onDataRequestCompleted();
  }

  @Override
  public void onHandleRequestResponse(Msg.DataMessageResp msgResp) {
    store.onHandleRequestResponse(msgResp);
  }
}
