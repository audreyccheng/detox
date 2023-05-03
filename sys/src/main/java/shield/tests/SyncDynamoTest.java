package shield.tests;

import java.io.IOException;

import com.google.protobuf.ByteString;

import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncDynamoBackingStore;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Utility;

public class SyncDynamoTest {

  public static void main(String[] args)
      throws InterruptedException, IOException {

    NodeConfiguration config = new NodeConfiguration();
    SyncDynamoBackingStore node = new SyncDynamoBackingStore(config);

    node.write(new Write(Utility.hashPersistent("hello"), "world".getBytes(),
        Type.WRITE));
    Thread.sleep(10000);
    byte[] value = node.read(Utility.hashPersistent("hello"));
    assert(new String(value).equals("world"));
    value = node.read(Utility.hashPersistent("hello"));
    node.write(new Write(Utility.hashPersistent("hello"), "world1".getBytes(),
                Type.WRITE));
    node.write(new Write(Utility.hashPersistent("hello"), "world2".getBytes(),
        Type.WRITE));
    value = node.read(Utility.hashPersistent("hello"));
    assert(new String(value).equals("world2"));

    System.out.println("Test Successful");
  }

}


