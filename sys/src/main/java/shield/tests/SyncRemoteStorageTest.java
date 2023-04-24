package shield.tests;

import java.io.IOException;

import com.google.protobuf.ByteString;

import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.data.sync.SyncRemoteBackingStoreLib;
import shield.proxy.data.sync.SyncRemoteBackingStoreServer;
import shield.util.Utility;

public class SyncRemoteStorageTest {

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

    StorageNode node =
        new StorageNode(localHost, localPort, remoteHost, remotePort);

    System.out.println("Reached here");
    byte[] value = node.read(Utility.hashPersistent("hello"));
    assert (new String(value).equals(""));
    node.write(Utility.hashPersistent("hello"), "world".getBytes());
    value = node.read(Utility.hashPersistent("hello"));
    assert (new String(value).equals("world"));
    value = node.read(Utility.hashPersistent("hello"));
    node.write(Utility.hashPersistent("hello"), "world1".getBytes());
    node.write(Utility.hashPersistent("hello"), "world2".getBytes());
    value = node.read(Utility.hashPersistent("hello"));
    assert (new String(value).equals("world2"));

    System.out.println("Test Successful");
  }

}


