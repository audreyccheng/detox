package shield.benchmarks.ycsb;

import java.sql.SQLException;
import java.util.Random;
import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.StatisticsCollector;
import shield.benchmarks.ycsb.utils.YCSBSimulator;
import shield.client.ClientBase;
import shield.client.ClientTransaction;
import shield.client.DatabaseAbortException;

import java.io.IOException;
import java.util.List;

/**
 * Simulates a client that runs a YCSB workload, the parameters of the YCSB workload are configured
 * in the json file passed in as argument
 *
 * @author ncrooks
 */
public class StartYCSBTrxClient {

  public static void main(String[] args) throws InterruptedException,
      IOException, ParseException, DatabaseAbortException, SQLException {

    StatisticsCollector stats;
    String expConfigFile;
    YCSBExperimentConfiguration ycsbConfig;
    YCSBSimulator ycsbSimulator;
    long beginTime;
    long expiredTime;
    int nbExecuted = 0;
    int nbAbort = 0;
    boolean success = false;
    ClientTransaction ongoingTrx;
    int measurementKey = 0;
    boolean warmUp;
    boolean warmDown;

    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
    }
    // Contains the experiment paramaterOMMIT_DURBALE
    expConfigFile = args[0];

    ycsbConfig = new YCSBExperimentConfiguration(expConfigFile);
    ycsbSimulator = new YCSBSimulator(ycsbConfig);

    if (ycsbConfig.KEY_FILE_NAME.equals("")) {
      throw new RuntimeException("Incorrect File Name");
    } else {
      ycsbSimulator.setupExperimentFromFile(ycsbConfig.KEY_FILE_NAME);
    }

    stats = new StatisticsCollector(ycsbConfig.RUN_NAME);

    ClientBase client = ClientUtils.createClient(ycsbConfig.CLIENT_TYPE, expConfigFile);

    client.registerClient();

    System.out.println("Begin Client " + client.getBlockId() + System.currentTimeMillis());

    // Trying to minimise timing differences
    //Thread.sleep(client.getBlockId() * 100);

    beginTime = System.currentTimeMillis();
    expiredTime = 0;
    warmUp = true;
    warmDown = false;

    int totalRunningTime = (ycsbConfig.EXP_LENGTH + ycsbConfig.RAMP_UP + ycsbConfig.RAMP_DOWN) * 1000;

    while (expiredTime < totalRunningTime) {
      success = false;
      ongoingTrx = ycsbSimulator.generateTransaction();

      if (!warmUp && !warmDown) {
        measurementKey = StatisticsCollector.addBegin(stats);
      } else {
      }

      int localAbort = 0;
      while (!success) {
        try {
          ongoingTrx.markStart(ycsbConfig.TRX_SIZE);
          List<byte[]> b = client.executeTransaction(ongoingTrx);
          success = true;
          assert (b.stream().filter(x -> x.equals("")).count() == 0);

//          if (localAbort > 10) Thread.sleep(client.getBlockId() * 300);
        } catch (DatabaseAbortException e) {
          success = false;
          ongoingTrx.resetExecution();
          localAbort++;
          System.out.println("Abort " + ++nbAbort);
        }
      }

      if (!warmUp && !warmDown) {
        StatisticsCollector.addEnd(stats, measurementKey);
      } else {
      }

      nbExecuted++;
      expiredTime = System.currentTimeMillis() - beginTime;
      if (expiredTime > ycsbConfig.RAMP_UP * 1000) {
        warmUp = false;
      }
      if ((totalRunningTime - expiredTime) < (ycsbConfig.RAMP_DOWN * 1000)) {
        warmDown = true;
      }

      String noAbort = localAbort == 0? "No Abort":"";
      System.out.println("[Executed] " + nbExecuted + " " + expiredTime + " " + localAbort + " " + noAbort);
    }

    System.out.println("Finished ... ");

    System.exit(0);
  }

}
