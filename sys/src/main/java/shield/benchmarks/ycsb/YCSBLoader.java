package shield.benchmarks.ycsb;

import static shield.benchmarks.utils.Generator.generatePortNumber;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.ycsb.utils.YCSBSimulator;
import shield.client.DatabaseAbortException;
import shield.client.ClientBase;
import shield.config.NodeConfiguration;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import shield.util.Utility;

/**
 * Generates, based on the config parameters the appropriate set of keys
 *
 * @author ncrooks
 */
public class YCSBLoader {

  public static void main(String[] args) throws IOException, ParseException,
      DatabaseAbortException, InterruptedException {
    String expConfigFile;
    YCSBExperimentConfiguration ycsbConfig;
    YCSBSimulator ycsbSimulator;

    if (args.length != 1) {
      System.out.println(args.length);
      System.out.println(args[0]);
      System.out.println(args[1]);
      System.err.println(
          "Incorrect number of arguments: expected <clientConfigFile.json expConfigFile.json>");
    }
    // Contains the experiment parameters
    expConfigFile = args[0];
    System.err.println(expConfigFile);
    ycsbConfig = new YCSBExperimentConfiguration(expConfigFile);

    ycsbSimulator = new YCSBSimulator(ycsbConfig);

    System.out.println("Begin key generation ... ");
    if (ycsbConfig.MUST_GENERATE_KEYS || ycsbConfig.KEY_FILE_NAME.equals("")) {
      System.out.println("Generate keys");
      ycsbSimulator.setupExperiment();
      ycsbSimulator.dumpKeysToFile(ycsbConfig.KEY_FILE_NAME);
    } else {
      System.out.println("Generate Keys From File");
      ycsbSimulator.setupExperimentFromFile(ycsbConfig.KEY_FILE_NAME);
    }

    if (ycsbConfig.MUST_LOAD_KEYS) {
      System.out.println("Begin loading data");
      loadData(ycsbSimulator, ycsbConfig, expConfigFile);
    }

    System.out.println("Data loaded");
    System.exit(0);

  }

  private static void loadData(YCSBSimulator ycsbSimulator,
      YCSBExperimentConfiguration ycsbConfig, String expConfigFile)
      throws InterruptedException, IOException, ParseException {
    List<String> keys = ycsbSimulator.getKeys();
    NodeConfiguration nodeConfig = new NodeConfiguration(expConfigFile);
     final int ranges = keys.size() / ycsbConfig.NB_LOADER_THREADS;
    List<Thread> threads = new LinkedList<Thread>();
    byte[] value = new byte[ycsbConfig.VALUE_SIZE];
    new Random().nextBytes(value);
    assert (ycsbConfig.VALUE_SIZE == nodeConfig.ORAM_VALUE_SIZE);
    assert (ycsbConfig.KEY_SIZE == nodeConfig.ORAM_KEY_SIZE);

    // Pre initialise set of ports to avoid risk of duplicates
    Set<Integer> ports = new HashSet<>();
    while (ports.size() < ycsbConfig.NB_LOADER_THREADS) {
      ports.add(generatePortNumber());
    }

    Iterator<Integer> it = ports.iterator();
    for (int i = 0; i < keys.size(); i += ranges) {
      System.out.println("Begin Loading" + i);
      final int j = i;
      int id = it.next();
      Thread t = new Thread() {
        public void run() {
          try {
            int maxData = j + ranges > keys.size() ? keys.size() : j + ranges;
            loadSubData(j, maxData, keys, value, id,expConfigFile);
          } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.err.println("Loading failed");
            System.exit(-1);
          }
        }
      };
      threads.add(t);
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }
  }


  private static void loadSubData(int start, int end, List<String> keys,
      byte[] value, int port, String expConfigFile) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException, SQLException {

    System.out.println("Loading Sub data " + start + " " + end);
    int trxSize = 5;

    YCSBExperimentConfiguration ycsbConfig = new YCSBExperimentConfiguration(expConfigFile);
    ClientBase client = ClientUtils.createClient(ycsbConfig.CLIENT_TYPE, expConfigFile, port, port);
    client.registerClient();

    System.out.println("Client registered " + client.getConfig().NODE_LISTENING_PORT);

    int nbTrxs = 0;

    int i = start;
    while (i < end) {
      System.out.println("I " + i + " " + end);
      boolean success = false;
      System.out.println("Start Transaction " + client.getBlockId() +  " " + nbTrxs);
      while (!success) {
        int ii = 0;
        try {
          client.startTransaction();
          while ((ii < trxSize) && ((i + ii) < end)) {
            String key = keys.get(i+ii);
            client.write("", key, value);
            ii++;
          }
          client.commitTransaction();
          success = true;
        } catch (DatabaseAbortException e) {
          System.out.println("Transaction aborted. Retrying " + client.getBlockId());
          System.err.println("Transaction aborted. Retrying " + client.getBlockId());
          success = false;
        }
      }
      nbTrxs++;
      i += trxSize;
    }

    System.out.println("Finished Loading Sub data " + client.getBlockId() + " " + start + " " + end);

  }
}
