package shield.benchmarks.smallbank;

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
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.Generator;
import shield.client.ClientBase;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

public class SmallBankLoader {
  public static void main(String[] args) throws IOException, ParseException,
      DatabaseAbortException, InterruptedException {
    String expConfigFile;
    SmallBankExperimentConfiguration sbConfig;

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
    sbConfig= new SmallBankExperimentConfiguration(expConfigFile);

    if (sbConfig.MUST_LOAD_KEYS) {
      System.out.println("Begin loading data");
      loadData(sbConfig, expConfigFile);
    }

    System.out.println("Data loaded");
    System.exit(0);

  }

  // Loading timeout from 500ms to 1500ms
  private static int getRandomTimeout() {
    return (int) (Math.random() * 1000) + 500;
  }

  private static void loadData(SmallBankExperimentConfiguration smallBankConfig, String expConfigFile)
      throws InterruptedException, IOException, ParseException {


    // First load data
    int ranges  = smallBankConfig.NB_ACCOUNTS/ smallBankConfig.NB_LOADER_THREADS;
    int accountsToLoad =  ranges > 0? ranges: 1;
    List<Thread> threads = new LinkedList<Thread>();

    // Pre initialise set of ports to avoid risk of duplicates
    Set<Integer> ports = new HashSet<>();
    while (ports.size() < smallBankConfig.NB_LOADER_THREADS) {
      ports.add(generatePortNumber());
    }

    Iterator<Integer> it = ports.iterator();

    AtomicLong progressCount = new AtomicLong(0);
    long begin = System.nanoTime();
    for (int i = 0 ; i < smallBankConfig.NB_ACCOUNTS; i +=  accountsToLoad) {
      final int j = i;
      int port = it.next();
      Thread t = new Thread() {
        public void run() {
          try {
            int endAccount=
                (j + accountsToLoad > smallBankConfig.NB_ACCOUNTS) ? smallBankConfig.NB_ACCOUNTS:
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


     SmallBankExperimentConfiguration config = new SmallBankExperimentConfiguration(expConfigFile);
     RedisPostgresClient client = (RedisPostgresClient) ClientUtils.createClient(config.CLIENT_TYPE,expConfigFile, port, port);

     assert (end <= config.NB_ACCOUNTS);

     System.out.println("Loading Accounts: " + start + " to " + end);
     int total_to_load = end-start+1;

     client.registerClient();
     SmallBankGenerator generator = new SmallBankGenerator(client, config);


     Table checkingsTable = client.getTable(SmallBankConstants.kCheckingsTable);
     Table accountsTable= client.getTable(SmallBankConstants.kAccountsTable);
     Table savingsTable = client.getTable(SmallBankConstants.kSavingsTable);
     Integer custId;

     boolean success;
     byte[] row;

     int sleep_counter = 0;

     while (start < end) {
       success = false;
       int oldStart = start;
       while (!success) {
         start = oldStart;
//         Thread.sleep((int) (Math.random()*500));
         try {
           client.startTransaction();
           for (int ij = 0; ij < 10; ij++) {
             custId = start + ij;
             if (custId>= end)
               break;
               // Update account table
               row = accountsTable.createNewRow(config.PAD_COLUMNS);
               accountsTable.updateColumn("A_CUST_ID", custId, row);
               accountsTable.updateColumn("A_CUST_NAME",
                   generator.RandString(10, config.NAME_SIZE, false), row);
               accountsTable.updateColumn("A_DATA",
                   generator.RandString(config.VAR_DATA_SIZE / 2, config.VAR_DATA_SIZE, false),
                   row);
               client.writeAndExecute(SmallBankConstants.kAccountsTable, custId.toString(), row);

               // Update savings table
               row = savingsTable.createNewRow(config.PAD_COLUMNS);
               savingsTable.updateColumn("S_CUST_ID", custId, row);
               savingsTable.updateColumn("S_BAL",
                   Generator.getGaussian(config.MIN_BALANCE, config.MAX_BALANCE), row);
               savingsTable.updateColumn("S_DATA",
                   generator.RandString(config.VAR_DATA_SIZE / 2, config.VAR_DATA_SIZE, false),
                   row);
               savingsTable.updateColumn("S_BAL",
                   Generator.getGaussian(config.MIN_BALANCE, config.MAX_BALANCE), row);
               client.writeAndExecute(SmallBankConstants.kSavingsTable, custId.toString(), row);

               // Update checkings table
               row = checkingsTable.createNewRow(config.PAD_COLUMNS);
               checkingsTable.updateColumn("C_CUST_ID", custId, row);
               checkingsTable.updateColumn("C_BAL",
                   Generator.getGaussian(config.MIN_BALANCE, config.MAX_BALANCE), row);
               checkingsTable.updateColumn("C_DATA",
                   generator.RandString(config.VAR_DATA_SIZE / 2, config.VAR_DATA_SIZE, false),
                   row);
               if (row == null) {
                 System.err.println("ERROR Writing null row to customer " + custId.toString());
               }
               client.writeAndExecute(SmallBankConstants.kCheckingsTable, custId.toString(), row);
             }
             System.out.println(count.incrementAndGet() * 10 + "/" + config.NB_ACCOUNTS + " Start: " + start);

           client.commitTransaction();

           start += 10;
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

         sleep_counter += 10;
//         if (sleep_counter > (total_to_load/4)) {
//           sleep_counter = 0;
//           int sleep_time = getRandomTimeout();
//           System.out.println("thread " + Thread.currentThread().getId() + " sleeping for " + sleep_time);
//           Thread.sleep(sleep_time);
//         }
       }
     }

     client.requestExecutor.shutdown();
     while (!client.requestExecutor.isTerminated()) {}
   }
 }

