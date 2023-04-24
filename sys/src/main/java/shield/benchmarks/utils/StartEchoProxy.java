package shield.benchmarks.utils;

import java.io.IOException;
import org.json.simple.parser.ParseException;
import shield.client.DatabaseAbortException;
import shield.proxy.EchoProxy;

/**
 * Class starts a proxy, based on the current configuration file
 *
 * @author ncrooks
 */
public class StartEchoProxy {

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException {

    String expConfigFile;

    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
    }
    // Contains the experiment paramaters
    expConfigFile = args[0];

    EchoProxy proxy = new EchoProxy(expConfigFile);
    proxy.startProxy();


    // Ugly hack to keep alive
    while(true) {Thread.sleep(100000);}

  }

}
