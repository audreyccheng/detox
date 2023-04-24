package shield.benchmarks.ycsb;

import org.json.simple.parser.ParseException;

import shield.BaseNode;
import shield.benchmarks.utils.StatisticsCollector;
import shield.benchmarks.ycsb.utils.YCSBSimulator;
import shield.client.DatabaseAbortException;
import shield.config.NodeConfiguration;
import shield.proxy.data.sync.ISyncBackingStore;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.data.sync.SyncMapDBBackingStore;
import shield.proxy.data.sync.SyncRemoteBackingStoreLib;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.tests.StorageNode;
import shield.util.Pair;
import shield.util.Utility;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TODO(natacha): add comments
 *
 * Simulates a client that runs a YCSB workload, the parameters of the YCSB workload are configured
 * in the json file passed in as argument
 *
 * @author ncrooks
 */
public class StartYCSBSyncDataStore {

  private final StatisticsCollector stats;
  private final YCSBExperimentConfiguration ycsbConfig;
  private final NodeConfiguration nodeConfig;
  private final YCSBSimulator ycsbSimulator;
  private final StorageNode node;
  private final ISyncBackingStore dataStoreLib;

  long beginTime;
  AtomicLong nbExecuted = new AtomicLong(0);
  byte[] value;

  StartYCSBSyncDataStore(String expConfigFile) throws IOException, ParseException,
      DatabaseAbortException, InterruptedException {

    ycsbConfig = new YCSBExperimentConfiguration(expConfigFile);
    ycsbSimulator = new YCSBSimulator(ycsbConfig);
    nodeConfig = new NodeConfiguration(expConfigFile);
    stats = new StatisticsCollector(ycsbConfig.RUN_NAME);

    if (ycsbConfig.KEY_FILE_NAME.equals("") || ycsbConfig.MUST_GENERATE_KEYS) {
      ycsbSimulator.setupExperiment();
    } else {
      ycsbSimulator.setupExperimentFromFile(ycsbConfig.KEY_FILE_NAME);

    }
    node = new StorageNode(nodeConfig.NODE_IP_ADDRESS,
        nodeConfig.NODE_LISTENING_PORT, nodeConfig.REMOTE_STORE_IP_ADDRESS,
        nodeConfig.REMOTE_STORE_LISTENING_PORT);

    dataStoreLib = createDataManager(node);

    beginTime = System.currentTimeMillis();


  }

  private boolean executeRequest() {
    long elapsedTime = System.currentTimeMillis() - beginTime;
    Pair<String, Boolean> nextOperation = ycsbSimulator.generateOperation();
    int k = elapsedTime < ycsbConfig.RAMP_UP ? -1 : stats.addBegin();
    if (nextOperation.getRight()) {
      dataStoreLib.read(Utility.hashPersistent(nextOperation.getLeft()));
    } else {
      dataStoreLib.write(new
          Write(Utility.hashPersistent(nextOperation.getLeft()),
          value, Type.WRITE));
    }
    if (k > 0) {
      // Recording
      stats.addEnd(k);
    }
    elapsedTime = System.currentTimeMillis() - beginTime;
    boolean finished =
        elapsedTime < ycsbConfig.EXP_LENGTH * 1000 ? false : true;
    return finished;

  }


  public void runExperiment() throws InterruptedException {

    List<Thread> threads = new LinkedList<Thread>();

    if (ycsbConfig.MUST_LOAD_KEYS) {
      loadData();
    }

    for (int i = 0; i < ycsbConfig.NB_CLIENT_THREADS; i++) {
      Thread t = new Thread() {
        public void run() {
          boolean finished = false;
          while (!finished) {
            finished = executeRequest();
          }
          System.out.println("Execution finished");
        }
      };
      threads.add(t);
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }
    System.out.println("All Finished");
  }


  private void loadData() throws InterruptedException {
    List<String> keys = ycsbSimulator.getKeys();
    final int ranges = keys.size() / ycsbConfig.NB_LOADER_THREADS;
    List<Thread> threads = new LinkedList<Thread>();

    for (int i = 0; i < keys.size(); i += ranges) {
      final int j = i;
      Thread t = new Thread() {
        public void run() {
              int end = j + ranges < keys.size()? j+ranges:keys.size();
              loadSubData(j, end);
        }
      };
      threads.add(t);
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }
  }

  private void loadSubData(int start, int end) {
    List<String> keys = ycsbSimulator.getKeys();
    for (int i = start; i < end; i++) {
      String key = keys.get(i);
      dataStoreLib.write(new Write(Utility.hashPersistent(key), value, Type.WRITE));
    }
    System.out.println("Loaded from " + start + " to " + end);
  }

  /**
   * Initialises the data manager for the appropriate type
   */
  ISyncBackingStore createDataManager(BaseNode proxy) {

    ISyncBackingStore dataManager;

    switch (proxy.getConfig().BACKING_STORE_TYPE) {

      case ORAM_SEQ_MAPDB: // falling through
      case ORAM_SEQ_HASHMAP:
      case ORAM_SEQ_SERVER:
      case ORAM_PAR_MAPDB: // falling through
      case ORAM_PAR_HASHMAP:
      case ORAM_PAR_SERVER:
      case NORAM_MAPDB:
        dataManager = new SyncMapDBBackingStore(proxy);
        break;
      case NORAM_HASHMAP:
        dataManager = new SyncMapBackingStore();
        break;
      case NORAM_SERVER:
        dataManager = new SyncRemoteBackingStoreLib(proxy);
        break;
      default:
        throw new RuntimeException("Unsupported storage backend");
    }

    return dataManager;
  }

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException {
    // Contains the experiment paramaters
    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
    }
    String expConfigFile = args[0];
    StartYCSBSyncDataStore store = new StartYCSBSyncDataStore(expConfigFile);

    store.runExperiment();
  }

}
