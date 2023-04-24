package shield.benchmarks.freehealth;

import java.sql.SQLException;
import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.StatisticsCollector;
import shield.client.ClientBase;
import shield.client.ClientTransaction;
import shield.client.DatabaseAbortException;

import java.io.IOException;

public class FreeHealthTransactionTester {



    public static void main(String args[]) throws InterruptedException,
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

        // Contains the experiment paramaters
        expConfigFile = "none";
        config = new FreeHealthExperimentConfiguration();

        ClientBase client = ClientUtils.createClient(ClientUtils.ClientType.DUMMY, expConfigFile);
        client.registerClient();

        generator = new FreeHealthGenerator(client, config);

        stats = new StatisticsCollector(config.RUN_NAME);


        System.out.println("Begin Client " + client.getBlockId() + System.currentTimeMillis());

        // Trying to minimise timing differences
        Thread.sleep(client.getBlockId() * 500);

        beginTime = System.currentTimeMillis();
        expiredTime = 0;
        warmUp = true;
        warmDown = false;


        while (expiredTime < config.EXP_LENGTH * 1000) {
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
//        freeHealthGenerator.runNextTransaction();


    }
}
