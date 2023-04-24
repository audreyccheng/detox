package shield.tests;

import org.mapdb.*;

import com.google.protobuf.ByteString;

import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncMapDBBackingStore;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Utility;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.AsyncMapDBBackingStore;


public class MapDBStorageTest {


  public static void main(String[] args) {

    NodeConfiguration config = new NodeConfiguration();

    AsyncMapDBBackingStore node = new AsyncMapDBBackingStore(config);
    AsyncDataRequest req;
    req = new AsyncDataRequest();
    node.read(Utility.hashPersistent("hello"), req);
    while (!req.isDone()) {
    }
    byte[] value = req.getValues().get(0);
    assert (new String(value).equals(""));
    req = new AsyncDataRequest();
    node.write(new Write(Utility.hashPersistent("hello"), "world".getBytes(),
        Type.WRITE), req);
    while (!req.isDone()) {
    }
    req = new AsyncDataRequest();
    node.read(Utility.hashPersistent("hello"), req);
    while (!req.isDone()) {
    }
    value = req.getValues().get(0);
    assert (new String(value).equals("world"));
    req = new AsyncDataRequest();
    node.read(Utility.hashPersistent("hello"), req);
    while (!req.isDone()) {
    }
    value = req.getValues().get(0);
    req = new AsyncDataRequest();
    node.write(new Write(Utility.hashPersistent("hello"), "world1".getBytes(),
        Type.WRITE), req);
    while (!req.isDone()) {
    }
    req = new AsyncDataRequest();
    node.write(new Write(Utility.hashPersistent("hello"), "world2".getBytes(),
        Type.WRITE), req);
    while (!req.isDone()) {
    }
    req = new AsyncDataRequest();
    node.read(Utility.hashPersistent("hello"), req);
    while (!req.isDone()) {
    }
    value = req.getValues().get(0);
    assert (new String(value).equals("world2"));
    node.deleteDB();

    SyncMapDBBackingStore node2 = new SyncMapDBBackingStore(config);
    node2.read(Utility.hashPersistent("hello"));
    value = req.getValues().get(0);
    assert (new String(value).equals(""));
    node2.write(new Write(Utility.hashPersistent("hello"),
        "world".getBytes(), Type.WRITE));
    value = node2.read(Utility.hashPersistent("hello"));
    assert (new String(value).equals("world"));
    value = node2.read(Utility.hashPersistent("hello"));
    node2.write(new Write(Utility.hashPersistent("hello"), "world1".getBytes(),
        Type.WRITE));
    node2.write(new Write(Utility.hashPersistent("hello"), "world2".getBytes(),
        Type.WRITE));
    value = node2.read(Utility.hashPersistent("hello"));
    assert (new String(value).equals("world2"));
    node.deleteDB();

    System.out.println("Test Successful");
  }
}
