package shield.proxy.data.async;

import java.util.LinkedList;
import java.util.List;


/**
 * Object stub passed to method calls of {@link IAsyncBackingStore} on which notifications that the
 * requests have completed will be callback. This interface can be extended according to the
 * application logic
 *
 * @author ncrooks
 */
public class AsyncDataRequest {

  /**
   * The result of the read requests if any
   */
  protected List<byte[]> readValues;

  /**
   * True if this request has finished executing
   */
  protected boolean requestServed;

  /**
   * Callback method is invoked once data operation has finished executing
   */
  public void onDataRequestCompleted() {
    markDone();
  }

  /**
   * Sets the return values (if any exist) that were read as part of this data request
   */
  public synchronized void setReadValues(List<byte[]> retValues) {
    readValues = retValues;
  }

  public synchronized void setReadValue(byte[] value) {
    if (readValues == null) {
      readValues = new LinkedList<byte[]>();
    }
    readValues.add(value);
  }

  public synchronized boolean isDone() {
    return requestServed == true;
  }

  public synchronized void markDone() {
    requestServed = true;
  }

  public List<byte[]> getValues() {
    if (readValues == null) {
      return null;
    } else {
      return new LinkedList<byte[]>(readValues);
    }
  }

}
