package shield.benchmarks.taobench;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.CacheStats;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.StatisticsCollector;
import shield.benchmarks.utils.TrxStats;
import shield.client.ClientTransaction;
import shield.client.DatabaseAbortException;
import shield.client.ClientBase;
import shield.client.RedisPostgresClient;

/**
 * Simulates a client that runs a YCSB workload, the parameters of the YCSB workload are configured
 * in the json file passed in as argument
 *
 * @author ncrooks
 */
public class StartTaoBenchTrxClient {

    public static class BenchmarkRunnable implements Runnable {

        public int threadNumber;
        public TaoBenchExperimentConfiguration tpcConfig;
        public String expConfigFile;
        public HashMap<TaoBenchConstants.Transactions, TrxStats> trxStats;
        public Map<Long, ReadWriteLock> keyLocks;

        BenchmarkRunnable(int threadNumber, TaoBenchExperimentConfiguration tpcConfig, String expConfigFile, Map<Long, ReadWriteLock> keyLocks) {
            this.threadNumber = threadNumber;
            this.tpcConfig = tpcConfig;
            this.expConfigFile = expConfigFile;
            this.keyLocks = keyLocks;
        }

        @Override
        public void run() {
            StatisticsCollector stats;
            TaoBenchGenerator taoBenchGenerator;
            long beginTime;
            long expiredTime;
            int nbExecuted = 0;
            int nbAbort = 0;
            boolean success = false;
            ClientTransaction ongoingTrx;
            int measurementKey = 0;
            boolean warmUp;
            boolean warmDown;

            stats = new StatisticsCollector(tpcConfig.RUN_NAME + "_thread" + this.threadNumber);

            try {
                RedisPostgresClient client = (RedisPostgresClient) ClientUtils.createClient(tpcConfig.CLIENT_TYPE, expConfigFile, keyLocks, 7000 + this.threadNumber, this.threadNumber);
                client.setThreadNumber(this.threadNumber);
                client.registerClient();

                System.out.println("Client registered");

                taoBenchGenerator = new TaoBenchGenerator(client, tpcConfig);

                System.out.println("Begin Client " + client.getBlockId() + System.currentTimeMillis());

                beginTime = System.currentTimeMillis();
                expiredTime = 0;
                warmUp = true;
                warmDown = false;

                int totalRunningTime = (tpcConfig.EXP_LENGTH + tpcConfig.RAMP_UP + tpcConfig.RAMP_DOWN) * 1000;

                while (expiredTime < totalRunningTime) {
                    if (!warmUp && !warmDown) {
                        measurementKey = StatisticsCollector.addBegin(stats);
                    }
                    taoBenchGenerator.runNextTransaction();
                    if (!warmUp && !warmDown) {
                        StatisticsCollector.addEnd(stats, measurementKey);
                    }
                    nbExecuted++;
                    expiredTime = System.currentTimeMillis() - beginTime;
                    if (expiredTime > tpcConfig.RAMP_UP * 1000)
                        warmUp = false;
                    if ((totalRunningTime - expiredTime) < tpcConfig.RAMP_DOWN * 1000)
                        warmDown = true;

//                    System.out.println("[Executed] " + nbExecuted + " " + expiredTime + " ");
                }

                client.requestExecutor.shutdown();
                while (!client.requestExecutor.isTerminated()) {}

                taoBenchGenerator.printStats();
                trxStats = taoBenchGenerator.getTrxStats();
            } catch (Exception e) {}
        }
    }

    public static void main(String[] args) throws InterruptedException,
            IOException, ParseException, DatabaseAbortException, SQLException {

        String expConfigFile;
        TaoBenchExperimentConfiguration tpcConfig;

        if (args.length != 1) {
            System.err.println(
                    "Incorrect number of arguments: expected <expConfigFile.json>");
        }
        // Contains the experiment paramaters
        expConfigFile = args[0];
        tpcConfig = new TaoBenchExperimentConfiguration(expConfigFile);
        Map<Long, ReadWriteLock> keyLocks = new ConcurrentHashMap<>(); // only make one lock map for all clients

        Thread[] threads = new Thread[tpcConfig.THREADS];
        BenchmarkRunnable[] runnables = new BenchmarkRunnable[tpcConfig.THREADS];
        for (int i = 0; i < tpcConfig.THREADS; i++) {
            runnables[i] = new BenchmarkRunnable(i, tpcConfig, expConfigFile, keyLocks);
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }

        HashMap<TaoBenchConstants.Transactions, TrxStats> combinedStats = new HashMap<>();

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();

            HashMap<TaoBenchConstants.Transactions, TrxStats> threadStats = runnables[i].trxStats;
            threadStats.forEach((txn, stat) -> {
                if (!combinedStats.containsKey(txn)) combinedStats.put(txn, new TrxStats());
                combinedStats.get(txn).mergeTxnStats(stat);
            });
        }

        System.out.println("THREADS COMBINED STATS");

        // Over how long a period the statistics were taken from
        System.out.println("Benchmark duration: " + tpcConfig.EXP_LENGTH);
        combinedStats.forEach((tType,stat) -> System.out.println("[STAT] " + tType + " " +  stat.getStats()));

        System.out.println();
        long txnsExecuted = combinedStats.values().stream().map(TrxStats::getExecuteCount).reduce(0L, Long::sum);
        System.out.println("Average throughput: " + txnsExecuted / tpcConfig.EXP_LENGTH + " txn/s");
        System.out.println("Average latency: " + ((float) combinedStats.values().stream().map(TrxStats::getTimeExecuted).reduce(0L, Long::sum)) / txnsExecuted + "ms");

        System.out.println();
        CacheStats.printReport();
        System.exit(0);
    }
}