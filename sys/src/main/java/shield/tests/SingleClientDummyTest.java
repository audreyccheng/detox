package shield.tests;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.json.simple.parser.ParseException;
import shield.client.Client;
import shield.client.DatabaseAbortException;
import shield.client.DummyClient;
import shield.proxy.Proxy;
import shield.proxy.data.async.IAsyncBackingStore.BackingStoreType;
import shield.proxy.trx.concurrency.TransactionManager.CCManagerType;

/**
 * Tests basic operations of a single client. There is a single stride per batch, and the stride
 * corresponds to exactly the number of operations in the trx
 *
 * @author ncrooks
 */
public class SingleClientDummyTest {

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException {

    DummyClient c = new DummyClient();
    System.out.println("Client Created");

   c.registerClient();

    System.out.println("Client Registered");

    String result;
    List<byte[]> res;
    try {
      c.startTransaction();
      c.read("", "2");
      res = c.readAndExecute("", "1");
      System.out.println("Expected (1,null):" + new String(res.get(0)));
      result = (new String(res.get(0)));
      assert (result.equals(""));
      c.write("", "1", "1".getBytes());
      c.commitTransaction();
    } catch (DatabaseAbortException e) {
      System.err.println("Aborted " + e.getMessage());
      System.exit(-1);
    }

    System.out.println("FINISHED 1");


    try {
      c.startTransaction();
      c.read("", "2");
      res = c.readAndExecute("", "1");
      result = (new String(res.get(0)));
      assert (result.equals(""));
      System.out.println(" Expected (2,null):" + " " + new String(res.get(0)));
      result = (new String(res.get(1)));
      System.out.println(" Expected (1,1):" + " " + new String(res.get(1)));
      assert (result.equals("1"));
      c.commitTransaction();
    } catch (DatabaseAbortException e) {
      System.err.println("Aborted");
      System.exit(-1);
    }

    System.out.println("FINISHED 2");

    try {
      c.startTransaction();

      res = c.deleteAndExecute("", "1");
      c.commitTransaction();
    } catch (DatabaseAbortException e) {
      System.err.println("Aborted");
      System.exit(-1);
    }

    System.out.println("FINISHED 3");

    try {
      c.startTransaction();
      res = c.readAndExecute("", "1");
      result = (new String(res.get(0)));
      System.out.println(result);
      assert (result.equals(""));
      result = (new String(res.get(0)));
      System.out.println(" Expected (1,null):" + " " + new String(res.get(0)));
      c.commitTransaction();
    } catch (DatabaseAbortException e) {
      System.err.println("Aborted");
      System.exit(-1);
    }

    System.out.println("FINISHED 4");

    System.out.println("Test successful");

  }

}
