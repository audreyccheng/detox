package shield.benchmarks.utils;

import org.json.simple.parser.ParseException;
import shield.client.DatabaseAbortException;
import shield.config.NodeConfiguration;
import shield.proxy.data.sync.SyncRemoteBackingStoreServer;

import java.io.IOException;

/**
 * Class starts a remote backing store server, based on the current configuration file
 *
 * @author ncrooks
 */
public class StartDataStore {

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException {

    String expConfigFile;
    NodeConfiguration config;
    SyncRemoteBackingStoreServer store;

    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
      System.exit(1);
    }
    // Contains the experiment paramaters
    expConfigFile = args[0];
    store = new SyncRemoteBackingStoreServer(expConfigFile);
  }

}
