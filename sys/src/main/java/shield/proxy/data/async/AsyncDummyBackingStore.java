package shield.proxy.data.async;

import com.google.protobuf.ByteString;
import java.util.Queue;
import shield.BaseNode;
import shield.network.messages.Msg.DataMessageResp;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;


/**
 * Implements fake storage. Directly returns value
 *
 * @author ncrooks
 */
public class AsyncDummyBackingStore implements IAsyncBackingStore {

  protected final BaseNode node;
  protected final byte[] value;

  public AsyncDummyBackingStore(BaseNode node) {
    this.node = node;
    System.out.println("Async Dummy Backing Store");
    this.value = new byte[node.getConfig().ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
  }

  public AsyncDummyBackingStore() {
    node = null;
    this.value = new byte[node.getConfig().ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
  }

  @Override
  public void read(Long key, AsyncDataRequest req) {
    node.executeAsync(() ->
        {
          req.setReadValue(value);
          req.onDataRequestCompleted();
        },
        node.getDefaultExpHandler());
  }

  @Override
  public void read(List<Long> keys, AsyncDataRequest req) {
    List<byte[]> toReturn = new ArrayList<byte[]>();

    for (Long key : keys) {
      toReturn.add(value);
    }

    req.setReadValues(toReturn);
    node.executeAsync(() -> req.onDataRequestCompleted(),
        node.getDefaultExpHandler());
  }

  @Override
  public void write(Write write, AsyncDataRequest req) {
    node.executeAsync(() -> req.onDataRequestCompleted(),
        node.getDefaultExpHandler());
  }

  public void deleteDB() {
  }

  @Override
  public void write(Queue<Write> writes,
      AsyncDataRequest req) {
    node.executeAsync(() -> req.onDataRequestCompleted(),
        node.getDefaultExpHandler());
  }

  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {
    throw new RuntimeException("Unimplemented");
  }


}
