package shield.tests;

import java.security.NoSuchAlgorithmException;
import org.json.simple.parser.ParseException;
import shield.client.Client;
import shield.client.DatabaseAbortException;
import shield.proxy.EchoProxy;
import shield.proxy.Proxy;
import shield.proxy.data.async.IAsyncBackingStore.BackingStoreType;

import java.io.IOException;
import java.util.List;
import shield.proxy.trx.concurrency.TransactionManager.CCManagerType;

/**
 * Tests basic operations of a single client. There is a single stride per batch, and the stride
 * corresponds to exactly the number of operations in the trx
 *
 * @author ncrooks
 */
public class SingleClientTest {

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException {

    Client c = new Client();
    System.out.println("Client Created");

    // Create server in separate thread
    Thread t = new Thread() {
      public void run() {
        try {
         Proxy proxy = new Proxy(c.getConfig().PROXY_IP_ADDRESS,
              c.getConfig().PROXY_LISTENING_PORT,
              BackingStoreType.NORAM_MAPDB);
         proxy.getConfig().CC_MANAGER_TYPE = CCManagerType.NOBATCH;
          proxy.getConfig().MAX_NB_STRIDE = 2;
          proxy.getConfig().STRIDE_SIZE = 3;
          proxy.getConfig().FINALISE_BATCH_BUFFER_T = 1000;
          proxy.getConfig().TIME_BETWEEN_STRIDE = 3000;
          while(true) {
              Thread.sleep(10000);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
          e.printStackTrace();
        }
      }
    };
    t.start();
    System.out.println("Server Created");

    Thread.sleep(10000);
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
      res = c.readAndExecute("", "1");
      System.out.println("Expected (1,1) " + " " + new String(res.get(0)));
      System.out.println(res.get(0).equals("1"));
      result = (new String(res.get(0)));
      assert (result.equals("1"));
      c.writeAndExecute("", "1", "2".getBytes());
      c.abortTransaction();
    } catch (DatabaseAbortException e) {
      System.err.println("Aborted (Expected)");
    }

    System.out.println("FINISHED 2");

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

    System.out.println("FINISHED 3");

    try {
      c.startTransaction();

      res = c.deleteAndExecute("", "1");
      c.commitTransaction();
    } catch (DatabaseAbortException e) {
      System.err.println("Aborted");
      System.exit(-1);
    }

    System.out.println("FINISHED 4");

    try {
      c.startTransaction();
      res = c.readAndExecute("", "1");
      result = (new String(res.get(0)));
      assert (result.equals(""));
      result = (new String(res.get(0)));
      System.out.println(" Expected (1,null):" + " " + new String(res.get(0)));
      c.commitTransaction();
    } catch (DatabaseAbortException e) {
      System.err.println("Aborted");
      System.exit(-1);
    }

    System.out.println("FINISHED 5");

    System.out.println("Test successful");

  }

}
