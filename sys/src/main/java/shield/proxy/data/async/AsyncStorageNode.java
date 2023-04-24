package shield.proxy.data.async;

import shield.BaseNode;
import shield.network.messages.Msg;
import shield.network.messages.Msg.Message;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Logging;

import java.util.List;

public final class AsyncStorageNode extends BaseNode {


  private final IAsyncBackingStore dataManager;

  /**
   * For testing only
   */
  public AsyncStorageNode(String thisAddress, int thisPort,
      String remoteAddress, int remotePort) throws InterruptedException {
    super(thisAddress, thisPort);
    dataManager =
        new AsyncRemoteBackingStoreLib(this, remoteAddress, remotePort);
  }


  @Override
  public void handleMsg(Message msg) {

    Msg.Message.Type ty;

    ty = msg.getMessageType();

    logOut("Received " + ty, Logging.Level.FINE);

    switch (ty) {
      case DataRespMessage:
        dataManager.onHandleRequestResponse(msg.getDataRespMsg());
        break;
      default:
        logErr("Unrecognised message type " + ty, Logging.Level.CRITICAL);
        System.exit(-1);
    }

  }


  public AsyncDataRequest read(Long key) {
    AsyncDataRequest req = new AsyncDataRequest();
    dataManager.read(key, req);
    return req;
  }

  public AsyncDataRequest write(Long key, byte[] value) {
    AsyncDataRequest req = new AsyncDataRequest();
    dataManager.write(new Write(key, value, Type.WRITE), req);
    return req;
  }

  public AsyncDataRequest delete(Long key) {
    AsyncDataRequest req = new AsyncDataRequest();
    dataManager.write(new Write(key, null, Type.DELETE), req);
    return req;
  }

  public AsyncDataRequest read(List<Long> keys) {
    AsyncDataRequest req = new AsyncDataRequest();
    dataManager.read(keys, req);
    return req;
  }


}
