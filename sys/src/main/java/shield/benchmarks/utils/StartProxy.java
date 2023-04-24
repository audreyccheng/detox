package shield.benchmarks.utils;

import java.security.NoSuchAlgorithmException;
import org.json.simple.parser.ParseException;
import shield.client.DatabaseAbortException;
import shield.proxy.Proxy;

import java.io.IOException;

/**
 * Class starts a proxy, based on the current configuration file
 *
 * @author ncrooks
 */
public class StartProxy {

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException, NoSuchAlgorithmException {

    String expConfigFile;

    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
    }
    // Contains the experiment paramaters
    expConfigFile = args[0];

    Proxy proxy = new Proxy(expConfigFile);
    proxy.startProxy();
  }

}
