package shield.tests;

import java.io.IOException;

import com.google.protobuf.ByteString;

import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.data.sync.SyncRemoteBackingStoreLib;
import shield.proxy.data.sync.SyncRemoteBackingStoreServer;
import shield.util.Utility;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.AsyncStorageNode;

public class AsyncRemoteStorageTest {

  public static void main(String[] args)
      throws InterruptedException, IOException {

    int remotePort = 10000;
    String remoteHost = "127.0.0.1";
    int localPort = 10001;
    String localHost = "127.0.0.1";

    /// Create the Store
    SyncRemoteBackingStoreServer remoteServer = null;

    remoteServer = new SyncRemoteBackingStoreServer(remoteHost, remotePort,
        localHost, localPort, ISyncBackingStore.BackingStoreType.NORAM_HASHMAP);

    AsyncStorageNode node =
        new AsyncStorageNode(localHost, localPort, remoteHost, remotePort);

    // NB: this is not how AsyncDataRequests should be used
    // This is only for testing purposes. Normally should
    // rely on the onRequestCompleted callback
    AsyncDataRequest req;
    req = node.read(Utility.hashPersistent("hello"));
    while (!req.isDone()) {
    }
    byte[] value = req.getValues().get(0);
    assert (new String(value).equals(""));
    req = node.write(Utility.hashPersistent("hello"), "world".getBytes());
    while (!req.isDone()) {
    }
    req = node.read(Utility.hashPersistent("hello"));
    while (!req.isDone()) {
    }
    value = req.getValues().get(0);
    assert (new String(value).equals("world"));
    req = node.read(Utility.hashPersistent("hello"));
    while (!req.isDone()) {
    }
    value = req.getValues().get(0);
    req = node.write(Utility.hashPersistent("hello"), "world1".getBytes());
    while (!req.isDone()) {
    }
    req = node.write(Utility.hashPersistent("hello"), "world2".getBytes());
    while (!req.isDone()) {
    }
    req = node.read(Utility.hashPersistent("hello"));
    while (!req.isDone()) {
    }
    value = req.getValues().get(0);
    assert (new String(value).equals("world2"));

    System.out.println("Test Successful");
  }

}


