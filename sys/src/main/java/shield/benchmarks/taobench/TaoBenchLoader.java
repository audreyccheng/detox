package shield.benchmarks.taobench;

import static shield.benchmarks.utils.Generator.generatePortNumber;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.json.simple.parser.ParseException;
import org.mapdb.Atomic;
import shield.benchmarks.taobench.TaoBenchConstants;
import shield.benchmarks.taobench.TaoBenchExperimentConfiguration;
import shield.benchmarks.taobench.TaoBenchGenerator;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.Generator;
import shield.client.ClientBase;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

public class TaoBenchLoader {
    public static void main(String[] args) throws IOException, ParseException,
            DatabaseAbortException, InterruptedException {
        String expConfigFile;
        TaoBenchExperimentConfiguration tbConfig;

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
        tbConfig= new TaoBenchExperimentConfiguration(expConfigFile);

        if (tbConfig.MUST_LOAD_KEYS) {
            System.out.println("Begin loading data");
            loadData(tbConfig, expConfigFile);
        }

        System.out.println("Data loaded");
        System.exit(0);

    }

    private static void loadData(TaoBenchExperimentConfiguration taoBenchConfig, String expConfigFile)
            throws InterruptedException, IOException, ParseException {


        // First load data
        int ranges  = taoBenchConfig.NB_OBJECTS/ taoBenchConfig.NB_LOADER_THREADS;
        int accountsToLoad =  ranges > 0? ranges: 1;
        List<Thread> threads = new LinkedList<Thread>();

        // Pre initialise set of ports to avoid risk of duplicates
        Set<Integer> ports = new HashSet<>();
        while (ports.size() < taoBenchConfig.NB_LOADER_THREADS) {
            ports.add(generatePortNumber());
        }

        Iterator<Integer> it = ports.iterator();

        AtomicLong progressCount = new AtomicLong(0);
        long begin = System.nanoTime();
        for (int i = 0 ; i < taoBenchConfig.NB_OBJECTS; i +=  accountsToLoad) {
            final int j = i;
            int port = it.next();
            Thread t = new Thread() {
                public void run() {
                    try {
                        int endAccount=
                                (j + accountsToLoad > taoBenchConfig.NB_OBJECTS) ? taoBenchConfig.NB_OBJECTS:
                                        (j + accountsToLoad);
                        loadAccounts(j, endAccount,port,expConfigFile, progressCount);
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
        long end = System.nanoTime();
        System.out.println("Took " + ((double)end-begin)/1000000000 + " seconds to load data");

    }


    private static void loadAccounts(int start, int end, int port, String expConfigFile, AtomicLong count)
            throws InterruptedException, ParseException, IOException, DatabaseAbortException, SQLException {

        TaoBenchExperimentConfiguration config = new TaoBenchExperimentConfiguration(expConfigFile);
        RedisPostgresClient client = (RedisPostgresClient) ClientUtils.createClient(config.CLIENT_TYPE,expConfigFile, port, port);

        assert (end <= config.NB_OBJECTS);

        System.out.println("Loading Accounts: " + start + " to " + end);

        client.registerClient();
        TaoBenchGenerator generator = new TaoBenchGenerator(client, config);


        Table objectsTable = client.getTable(TaoBenchConstants.kObjectsTable);
        Table edgesTable= client.getTable(TaoBenchConstants.kEdgesTable);
        Integer custId;
        Integer custId2;

        boolean success;
        byte[] row;

        while (start < end) {
            success = false;
            int oldStart = start;
            while (!success) {
                start = oldStart;
                try {
                    client.startTransaction();
                    for (int ij = 0; ij < 10; ij += 2) {
                        custId = start + ij;
                        custId2 = custId + 1;
                        if (custId >= end /* || custId2 >= end */)
                            break;
                        // Update objects table
                        row = objectsTable.createNewRow(config.PAD_COLUMNS);
                        objectsTable.updateColumn("OBJ_ID", custId, row);
                        objectsTable.updateColumn("OBJ_TIME", System.currentTimeMillis(), row);
                        objectsTable.updateColumn("OBJ_DATA",
                                generator.RandDiscreteString(config.DATA_SIZES, config.DATA_WEIGHTS, false),
                                row);
                        client.write(TaoBenchConstants.kObjectsTable, custId.toString(), row);

                        row = objectsTable.createNewRow(config.PAD_COLUMNS);
                        objectsTable.updateColumn("OBJ_ID", custId2, row);
                        objectsTable.updateColumn("OBJ_TIME", System.currentTimeMillis() + 1, row);
                        objectsTable.updateColumn("OBJ_DATA",
                                generator.RandDiscreteString(config.DATA_SIZES, config.DATA_WEIGHTS, false),
                                row);
                        client.write(TaoBenchConstants.kObjectsTable, custId2.toString(), row);

//                        // Update edges table
                        row = edgesTable.createNewRow(config.PAD_COLUMNS);
                        edgesTable.updateColumn("EDGE_ID", custId, row);
                        edgesTable.updateColumn("EDGE_TYPE", 100, row); // Type irrelevant
                        edgesTable.updateColumn("EDGE_ID2", custId2, row);
                        edgesTable.updateColumn("EDGE_TIME", System.currentTimeMillis(), row);
                        edgesTable.updateColumn("EDGE_DATA",
                                generator.RandDiscreteString(config.DATA_SIZES, config.DATA_WEIGHTS, false),
                                row);
                        String edgeStr = custId.toString() + ":" + custId2.toString();
                        client.write(TaoBenchConstants.kEdgesTable, edgeStr, row);
                    }
                    System.out.println(count.incrementAndGet() * 10 + "/" + config.NB_OBJECTS+ " Start: " + start);

                    start += 10;
                    client.commitTransaction();
                    success = true;
                } catch (DatabaseAbortException e) {
                    System.err.println("Trx Aborted ");
                    success = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(e.getMessage());
                    success = false;
                    System.err.println("Retrying");
                }
            }
        }

        client.requestExecutor.shutdown();
        while (!client.requestExecutor.isTerminated()) {}
    }
}

