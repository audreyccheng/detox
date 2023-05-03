package shield.benchmarks.ycsb;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.json.simple.parser.ParseException;

import shield.BaseNode;
import shield.benchmarks.utils.StatisticsCollector;
import shield.benchmarks.ycsb.utils.YCSBSimulator;
import shield.client.DatabaseAbortException;
import shield.client.StorageClient;
import shield.config.NodeConfiguration;
import shield.proxy.data.async.*;
import shield.proxy.data.sync.SyncDynamoBackingStore;
import shield.proxy.data.sync.SyncMapBackingStore;
import shield.proxy.data.sync.SyncMapDBBackingStore;
import shield.proxy.data.sync.SyncRemoteBackingStoreLib;
import shield.proxy.oram.AsyncRingOram;
import shield.proxy.oram.SyncRingOram;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.AsyncMapBackingStore;
import shield.proxy.data.async.AsyncMapDBBackingStore;
import shield.proxy.data.async.AsyncRemoteBackingStoreLib;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Pair;
import shield.util.Utility;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO(natacha): add comments
 *
 * Simulates a client that runs a YCSB workload, the parameters of the YCSB workload are configured
 * in the json file passed in as argument
 *
 * @author ncrooks
 */
public class StartYCSBClient {

  private final StatisticsCollector stats;
  private final YCSBExperimentConfiguration ycsbConfig;
  private final NodeConfiguration nodeConfig;
  private final YCSBSimulator ycsbSimulator;
  private final StorageClient client;
  private final Lock lock;
  private final Condition condition;
  private final AtomicInteger activeClients;
  private int executed = 0;

  boolean isWritePhase = false;
  long beginTime =0;
  long requestTime =0;
  byte[] value;

  private volatile int nbStrides =  0;

  private Queue<Set<String>> operationsReads = new ConcurrentLinkedQueue<>();
  private Queue<Set<String>> operationsWrites= new ConcurrentLinkedQueue<>();


  StartYCSBClient(String expConfigFile) throws IOException, ParseException,
      DatabaseAbortException, InterruptedException, NoSuchAlgorithmException {
    activeClients = new AtomicInteger(0);
    lock = new ReentrantLock();
    condition = lock.newCondition();
    ycsbConfig = new YCSBExperimentConfiguration(expConfigFile);
    nodeConfig = new NodeConfiguration(expConfigFile);
    ycsbSimulator = new YCSBSimulator(ycsbConfig);
    value = new byte[ycsbConfig.VALUE_SIZE];
    stats = new StatisticsCollector(ycsbConfig.RUN_NAME);

    if (ycsbConfig.KEY_FILE_NAME.equals("") || ycsbConfig.MUST_GENERATE_KEYS) {
      ycsbSimulator.setupExperiment();
    } else {
      ycsbSimulator.setupExperimentFromFile(ycsbConfig.KEY_FILE_NAME);

    }
    client = new StorageClient(expConfigFile);
  }

  private void executeRequest() {

    System.out.println("executeRequest");

    Set<String> nextOperations;
    // Find out whether should execute batch of reads or batch of writes
    int nbExecuted = nbStrides++;
    isWritePhase = nbExecuted == nodeConfig.MAX_NB_STRIDE? true: false;
    nextOperations= isWritePhase?
        operationsWrites.poll():
        //ycsbSimulator.generateOperations(nodeConfig.WRITES_SIZE):
        //ycsbSimulator.generateOperations(nodeConfig.STRIDE_SIZE);
        operationsReads.poll();

    if (isWritePhase) operationsWrites.add(nextOperations);
    else operationsReads.add(nextOperations);

    long elapsedTime = System.currentTimeMillis() - beginTime;

    int k = elapsedTime < (ycsbConfig.RAMP_UP * 1000)||
            elapsedTime > ((ycsbConfig.RAMP_UP + ycsbConfig.EXP_LENGTH) * 1000)?
        -1 : executed;

    requestTime = System.currentTimeMillis();
    if (isWritePhase) {
        nbStrides= 0;
        Queue<Write> ops = nextOperations.stream()
            .map(s -> new Write(Utility.hashPersistent(s),
              ycsbSimulator.getValue(), Type.WRITE))
            .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
        client.write(ops,
        new AsyncDataRequest() {
            public void onDataRequestCompleted() {
              onRequestExecuted(k, ops.size());
            }
          });
      } else {
        List<Long> ops = nextOperations.stream()
            .map(s -> Utility.hashPersistent(s))
            .collect(Collectors.toCollection(ArrayList::new));
        assert(ops.size() == nodeConfig.STRIDE_SIZE);
        client.read(ops,
        new AsyncDataRequest() {
            public void onDataRequestCompleted() {
              onRequestExecuted(k, ops.size());
            }
          });
      }
  }

  private void onRequestExecuted(int k, int nbRequests) {
    long elapsedTime = System.currentTimeMillis() - beginTime;
    long request = System.currentTimeMillis() - requestTime;
    System.out.println(nbRequests + " " + request);
    boolean finished =
        elapsedTime < (ycsbConfig.EXP_LENGTH  + ycsbConfig.RAMP_DOWN + ycsbConfig.RAMP_UP) * 1000 ? false : true;
    if (k >= 0 && nbRequests > 0) {
      // Recording
      for (int i = 0 ; i < nbRequests ; i++) {
        stats.addPoint(  (executed + i) + " "  + ((float)request));
      }
      executed+=nbRequests;
    }

    /*
    if (isWritePhase) {
      System.out
          .println("Request Executed WRITE PHASE Time: " + request + " Elapsed " + elapsedTime + " " + finished + " " + k + " " + nbRequests);
    } else {
      System.out.println(
          "Request Executed READ PHASE Time: " + request + " Elapsed " + elapsedTime + " "
              + finished + " " + k + " " + nbRequests);
    }
    isWritePhase = false;

    */

    if (!finished) {
      client.executeAsync(() -> executeRequest(), client.getDefaultExpHandler());
    } else {
      // Notify others that client has finished
      lock.lock();
      activeClients.decrementAndGet();
      System.out.println("Client finishing");
      condition.signalAll();
      lock.unlock();
    }
  }


  public void runExperiment() throws InterruptedException {
    System.out.printf("Beginning to load data...");
    if (ycsbConfig.MUST_LOAD_KEYS) {
      loadData();
    }
    System.out.printf("Finshed loading data.\n");

    for (int i = 0 ; i < 10 ; i++) {
        System.out.println("Generating workloads");
        operationsReads.add(ycsbSimulator.generateOperations(nodeConfig.STRIDE_SIZE));
        operationsReads.add(ycsbSimulator.generateOperations(nodeConfig.STRIDE_SIZE));
        operationsWrites.add(ycsbSimulator.generateOperations(nodeConfig.WRITES_SIZE));
    }

    // Currently not tested for greater than 1
    assert (ycsbConfig.NB_CLIENT_THREADS == 1);

    beginTime = System.currentTimeMillis();

    System.out.println(beginTime);

    for (int i = 0; i < ycsbConfig.NB_CLIENT_THREADS; i++) {
      activeClients.incrementAndGet();
      client.executeAsync(() -> executeRequest(), client.getDefaultExpHandler());
    }

    awaitTermination();
    System.out.printf("Done with experiment.\n");
    client.shutdown();
  }

  private void awaitTermination() throws InterruptedException {
    lock.lock();
    while (activeClients.get() > 0) {
      condition.await();
    }
    lock.unlock();
  }

  private void loadData() throws InterruptedException {
    List<String> keys = ycsbSimulator.getKeys();
    int ranges = keys.size() / ycsbConfig.NB_LOADER_THREADS;
    for (int i = 0; i < keys.size(); i += ranges) {
      activeClients.incrementAndGet();
      int end = i + ranges < keys.size()? i+ranges:keys.size();
      loadSubData(i, 0, end);
    }
    awaitTermination();
  }

  private void loadSubData(int start, int current, int end) {
    if (current >= end) {
      // Finish
      lock.lock();
      activeClients.decrementAndGet();
      condition.signalAll();
      System.out.printf("\n");
      lock.unlock();
    } else {
      System.out.println("LOAD SUB DATA " + start + " " + current + " " + end + " Stride " + nbStrides);
      List<String> keys = ycsbSimulator.getKeys();
      int nbExecuted = nbStrides++;
      boolean  writePhase = nbExecuted == nodeConfig.MAX_NB_STRIDE? true: false;
      if (writePhase) {
        nbStrides = 0;
        int operations =  writePhase?nodeConfig.WRITES_SIZE:nodeConfig.STRIDE_SIZE;
        final int next = current + operations;
        Queue<Write> ops = new ConcurrentLinkedQueue<>();
        for (int i = 0 ; i < operations; i++ ) {
          if ((current + i) >= keys.size()) break;
          String key = keys.get(current + i);
          ops.add(new Write(Utility.hashPersistent(key), value, Type.WRITE));
        }
        client.write(ops,
            new AsyncDataRequest() {
              public void onDataRequestCompleted() {
                loadSubData(start, next, end);
              }
            });
      } else {
        client.read(new ArrayList<>(),
            new AsyncDataRequest() {
              public void onDataRequestCompleted() {
                loadSubData(start, current, end);
              }
            });
      }

    }
  }

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException, NoSuchAlgorithmException {
    // Contains the experiment paramaters
    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
    }
    String expConfigFile = args[0];
    StartYCSBClient store = new StartYCSBClient(expConfigFile);

    store.runExperiment();
    // do this to end all networking/thread pool threads that would prevent us from exiting gracefully
    System.exit(0);
  }

}
