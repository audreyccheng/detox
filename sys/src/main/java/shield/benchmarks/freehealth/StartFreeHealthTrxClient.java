package shield.benchmarks.freehealth;

import java.sql.SQLException;
import java.util.Random;
import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.StatisticsCollector;
import shield.client.ClientBase;
import shield.client.ClientTransaction;
import shield.client.DatabaseAbortException;

import java.io.IOException;

/**
 * Simulates a client that runs a workload on a freehealth-based database,
 * the parameters of the workload are configured in the json file passed in as argument
 *
 * @author ncrooks
 */
public class StartFreeHealthTrxClient {

    public static void main(String[] args) throws InterruptedException,
        IOException, ParseException, DatabaseAbortException, SQLException {

        StatisticsCollector stats;
        String expConfigFile;
        FreeHealthExperimentConfiguration config;
        FreeHealthGenerator generator;
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
        // Contains the experiment paramaters
        expConfigFile = args[0];
        config = new FreeHealthExperimentConfiguration(expConfigFile);

        stats = new StatisticsCollector(config.RUN_NAME);

        ClientBase client = ClientUtils.createClient(config.CLIENT_TYPE, expConfigFile);
        client.registerClient();

        generator = new FreeHealthGenerator(client, config);

        System.out.println("Begin Client " + client.getBlockId() + System.currentTimeMillis());

        // Trying to minimise timing differences
//        Thread.sleep(client.getBlockId() * 500);

        beginTime = System.currentTimeMillis();
        expiredTime = 0;
        warmUp = true;
        warmDown = false;

        Random ran = new Random();

        while (expiredTime < config.EXP_LENGTH * 1000) {
            if (config.USE_THINK_TIME) Thread.sleep(ran.nextInt(config.THINK_TIME));
            if (!warmUp && !warmDown) {
                measurementKey = StatisticsCollector.addBegin(stats);
            }
            System.out.println(warmUp + " " + warmDown + " " + expiredTime);
            generator.runNextTransaction();
            if (!warmUp && !warmDown) {
                StatisticsCollector.addEnd(stats, measurementKey);
            }
            nbExecuted++;
            expiredTime = System.currentTimeMillis() - beginTime;
            if (expiredTime > config.RAMP_UP * 1000)
                warmUp = false;
            if ((config.EXP_LENGTH * 1000 - expiredTime) < config.RAMP_DOWN * 1000)
                warmDown = true;

            System.out.println("[Executed] " + nbExecuted + " " + expiredTime + " ");
        }

        generator.printStats();

        System.exit(0);
    }

}


