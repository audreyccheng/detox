package shield.proxy.data.sync;

import java.util.Queue;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.proxy.trx.data.Write;
import shield.util.Pair;
import shield.proxy.data.*;
import shield.proxy.data.async.*;
import shield.proxy.data.sync.*;

import java.util.List;
import java.util.Map;

/**
 * Simulates round-trip communication latency by sleeping for a configurable amount of time before
 * and after operations.
 */
public class LatencyMapBackingStore extends SyncMapBackingStore {

  private final int latencyMillis;

  /**
   * Constructs a new instance of a LatencyMapBackingStore.
   *
   * @param node The base node of this backing store.
   * @param map The underlying map implementation.
   * @param latencyMillis The number of milliseconds to sleep, in total, during each operation.
   */
  public LatencyMapBackingStore(Map<Long, byte[]> map,
      int latencyMillis) {
    super(map);
    this.latencyMillis = latencyMillis;
  }

  @Override
  public byte[] read(Long key) {
    sleep(latencyMillis / 2);
    byte[] value = super.read(key);
    sleep(latencyMillis / 2);
    return value;
  }

  @Override
  public List<byte[]> read(List<Long> keys) {
    sleep(latencyMillis / 2);
    List<byte[]> values = super.read(keys);
    sleep(latencyMillis / 2);
    return values;
  }

  @Override
  public void write(Write write){
    sleep(latencyMillis / 2);
    super.write(write);
    sleep(latencyMillis / 2);
  }

  @Override
  public void write(Queue<Write> writes) {
    sleep(latencyMillis / 2);
    super.write(writes);
    sleep(latencyMillis / 2);
  }

  @Override
  public void onHandleRequestResponse(Msg.DataMessageResp msgResp) {
    super.onHandleRequestResponse(msgResp);
  }


  /**
   * Sleeps for the specified amount of time if positive.
   *
   * @param millis The number of milliseconds to sleep.
   */
  private static void sleep(int millis) {
    if (millis > 1) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        // ignore?
      }
    }
  }

}
