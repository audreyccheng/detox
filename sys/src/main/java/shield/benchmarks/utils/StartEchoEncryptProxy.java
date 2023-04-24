package shield.benchmarks.utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.json.simple.parser.ParseException;
import shield.client.DatabaseAbortException;
import shield.proxy.EchoEncryptProxy;
import shield.proxy.EchoProxy;

/**
 * Class starts a proxy, based on the current configuration file
 *
 * @author ncrooks
 */
public class StartEchoEncryptProxy {

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException, NoSuchAlgorithmException {

    String expConfigFile;

    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
    }
    // Contains the experiment paramaters
    expConfigFile = args[0];

    EchoEncryptProxy proxy = new EchoEncryptProxy(expConfigFile);
    proxy.startProxy();


    // Ugly hack to keep alive
    while(true) {Thread.sleep(100000);}

  }

}
