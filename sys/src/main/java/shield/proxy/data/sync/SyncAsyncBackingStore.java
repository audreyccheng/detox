package shield.proxy.data.sync;

import com.google.protobuf.ByteString;
import java.util.Queue;
import shield.network.messages.Msg;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.oram.AsyncRingOram;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.util.List;

public class SyncAsyncBackingStore implements ISyncBackingStore {

  private final IAsyncBackingStore store;

  public SyncAsyncBackingStore(IAsyncBackingStore store) {
    this.store = store;
  }

  public IAsyncBackingStore getStore() {
    return store;
  }

  @Override
  public byte[] read(Long key) {
    AsyncDataRequest req = new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        markDone();
        synchronized (this) {
          this.notifyAll();
        }
      }
    };
    store.read(key, req);
    while (!req.isDone()) {
      synchronized (req) {
        try {
          req.wait(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    return req.getValues().get(0);
  }

  @Override
  public List<byte[]> read(List<Long> keys) {
    AsyncDataRequest req = new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        markDone();
        synchronized (this) {
          this.notifyAll();
        }
      }
    };
    store.read(keys, req);
    while (!req.isDone()) {
      synchronized (req) {
        try {
          req.wait(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    return req.getValues();
  }

  @Override
  public void write(Write write) {
    AsyncDataRequest req = new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        markDone();
        synchronized (this) {
          this.notifyAll();
        }
      }
    };
    store.write(write, req);
    while (!req.isDone()) {
      synchronized (req) {
        try {
          req.wait(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void write(Queue<Write> writes) {
    AsyncDataRequest req = new AsyncDataRequest() {
      @Override
      public void onDataRequestCompleted() {
        markDone();
        synchronized (this) {
          this.notifyAll();
        }
      }
    };
    store.write(writes, req);
    while (!req.isDone()) {
      synchronized (req) {
        try {
          req.wait(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void onHandleRequestResponse(Msg.DataMessageResp msgResp) {
    store.onHandleRequestResponse(msgResp);
  }

}
