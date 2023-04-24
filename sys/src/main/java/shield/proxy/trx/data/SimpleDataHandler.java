package shield.proxy.trx.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import shield.proxy.Proxy;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.trx.concurrency.Operation;
import shield.proxy.trx.concurrency.Transaction;
import shield.proxy.trx.data.Write.Type;
import shield.util.Utility;

/**
 * Non-batching data handler. Requests are immediately (and individually) sent to the datastore to
 * be processed
 *
 * @author ncrooks
 */
public class SimpleDataHandler extends DataHandler {

  private ConcurrentHashMap<Long,ConcurrentLinkedQueue<Write>> pendingWrites;

  public SimpleDataHandler(Proxy proxy) {
    super(proxy);
    pendingWrites = new ConcurrentHashMap<Long,ConcurrentLinkedQueue<Write>>(100,0.5f,
        proxy.getConfig().N_WORKER_THREADS);
  }



  @Override
  public void handleRequest(Operation op) {
    assert(!op.isDummy());
    // Long versionedKey = Utility.hashVersionedKey(op.getKey(), op.getVersion().getVersionId());
    Long versionedKey = op.getKey();
    if (op.isRead() || op.isReadForUpdate()) {
      dataStore.read(versionedKey, new AsyncDataRequest() {
        public void onDataRequestCompleted() {
          op.setReadValue(readValues.get(0));
          onRequestResponse(op);
        }
      });
    } else {
      ConcurrentLinkedQueue<Write> writes =  pendingWrites.computeIfAbsent(op.getTrx().getTimestamp(),
              w  -> new ConcurrentLinkedQueue<Write>());
      writes.add(new Write(versionedKey,op.getWriteValue(), op.isDelete()? Type.DELETE:Type.WRITE));
      proxy.executeAsync( () -> metadata.onOperationExecuted(op,false), proxy.getDefaultExpHandler());
      /* dataStore.write(new Write(versionedKey, op.getWriteValue(),
          op.isDelete()? Type.DELETE:Type.WRITE), new AsyncDataRequest() {
        public void onDataRequestCompleted() {
          onRequestResponse(op);
        }
      }); */
    }
  }


  @Override
  protected void onRequestResponse(Operation op) {
    metadata.onOperationExecuted(op, false);
  }

  @Override
  protected void onTransactionCommitted(Transaction t) {
    trxManager.onTransactionCommitted(t);

  }

  @Override
  public void commitTransaction(Transaction t) {
    ConcurrentLinkedQueue writes = pendingWrites.remove(t.getTimestamp());
    if (writes!=null) {
      dataStore.write(writes,
          new AsyncDataRequest() {
            public void onDataRequestCompleted() {
            onTransactionCommitted(t);
            }
          });
    } else proxy.executeAsync(()->trxManager.onTransactionCommitted(t), proxy.getDefaultExpHandler());
  }

  @Override
  public boolean startTransaction(Transaction t) {
    return true;
  }

  @Override
  public void cancelWrite(Operation op) {

  }

}
