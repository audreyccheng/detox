package shield.tests;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncDynamoBackingStore;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Utility;

public class SyncDynamoBatchTest {

  public static void main(String[] args)
      throws InterruptedException, IOException {

    NodeConfiguration config = new NodeConfiguration();
    SyncDynamoBackingStore node = new SyncDynamoBackingStore(config);

    Queue<Write> writes = new ConcurrentLinkedDeque<Write>();
    Write write1 = new Write(Utility.hashPersistent("hello"), "world".getBytes(),
        Type.WRITE);
    Write write2 = new Write(Utility.hashPersistent("hello2"), "world2".getBytes(),
      Type.WRITE);
    writes.add(write1);
    writes.add(write2);
    node.write(writes);

    Thread.sleep(10000);
    Long key1 = Utility.hashPersistent("hello");
    Long key2 = Utility.hashPersistent("hello2");
    List<Long> keys = new LinkedList<>();
    keys.add(key1);
    keys.add(key2);

    List<byte[]> value = node.read(keys);
    System.out.println(new String(value.get(0)));
    System.out.println(new String(value.get(1)));

    assert(new String(value.get(0)).equals("world"));
    assert(new String(value.get(1)).equals("world2"));

    node.write(new Write(Utility.hashPersistent("hello"), "world2".getBytes(),
                Type.WRITE));
    node.write(new Write(Utility.hashPersistent("hello"), "world3".getBytes(),
        Type.WRITE));

    byte[] val = node.read(Utility.hashPersistent("hello"));
    System.out.println(new String(val) + "\n");

    assert(new String(val).equals("world3"));

    key1 = Utility.hashPersistent("hello3");
    key2 = Utility.hashPersistent("hello2");
    keys = new LinkedList<>();
    keys.add(key1);
    keys.add(key2);

    value = node.read(keys);
    System.out.println(new String(value.get(0)));
    System.out.println(new String(value.get(1)));
    System.out.println("Done ");
    Thread.sleep(1000);

    assert(new String(value.get(0)).equals(""));
    assert(new String(value.get(1)).equals("world2"));



    System.out.println("Test Successful");
  }

}


