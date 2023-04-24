package shield.tests;

import shield.BaseNode;
import shield.network.messages.Msg;
import shield.network.messages.Msg.Message;
import shield.proxy.data.sync.*;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Logging;

import java.util.List;

public final class StorageNode extends BaseNode {


  private final ISyncBackingStore dataManager;

  /**
   * For testing only
   */
  public StorageNode(String thisAddress, int thisPort, String remoteAddress,
      int remotePort) throws InterruptedException {
    super(thisAddress, thisPort);
    dataManager =
        new SyncRemoteBackingStoreLib(this, remoteAddress, remotePort);
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


  public byte[] read(Long key) {
    return dataManager.read(key);
  }

  public void write(Long key, byte[] value) {
    dataManager.write(new Write(key, value, Type.WRITE));
  }

  public List<byte[]> read(List<Long> keys) {
    return dataManager.read(keys);
  }


}
