package shield.benchmarks.taobench;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import shield.benchmarks.taobench.*;
import shield.benchmarks.taobench.TaoBenchConstants.Transactions;

import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.benchmarks.utils.TrxStats;
import shield.client.RedisPostgresClient;
import shield.client.schema.ColumnInfo;
import shield.benchmarks.ycsb.utils.ZipfianIntGenerator;

public class TaoBenchGenerator {
    private final TaoBenchExperimentConfiguration config;

    private final RedisPostgresClient client;
    private final HashMap<Transactions, TrxStats> trxStats;
    private final char[] ALPHANUM =  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private final int nbAccounts;
    private final double a_1 = 2.0;
    private final double a_2= 1.3;
    private ZipfianIntGenerator zipf_1;
    private ZipfianIntGenerator zipf_2;
    private int group_1;
    private int group_2;
    private int group_3;
    private static long txn_id = 0;

    public TaoBenchGenerator(RedisPostgresClient client, TaoBenchExperimentConfiguration config) {
        this.config = config;
        this.client = client;
        this.trxStats = new HashMap<>();
        for (TaoBenchConstants.Transactions tType: TaoBenchConstants.Transactions.values()) {
            trxStats.put(tType, new TrxStats());
        }
        createTaoBenchTables(client);
        this.nbAccounts = config.HOTSPOT_USE_FIXED_SIZE
                ? config.HOTSPOT_FIXED_SIZE:
                (int) (config.NB_ACCOUNTS/config.HOTSPOT_PERCENTAGE);
        group_1 = config.BASE_SIZE;
        group_2 = config.BASE_SIZE * 2;
        group_3 = config.BASE_SIZE * 3;
        zipf_1 = new ZipfianIntGenerator(config.NB_OBJECTS, a_1); // should these be group_1?
        zipf_2 = new ZipfianIntGenerator(config.NB_OBJECTS, a_2);
    }

    public RedisPostgresClient getClient() {
        return client;
    }

    private void createTaoBenchTables(RedisPostgresClient client) {

        client.createTable(TaoBenchConstants.kObjectsTable,
                new ColumnInfo("OBJ_ID", Integer.class),
                new ColumnInfo("OBJ_TIME", Long.class),
                new ColumnInfo("OBJ_DATA", String.class, config.VAR_DATA_SIZE));
        client.createTable(TaoBenchConstants.kEdgesTable,
                new ColumnInfo("EDGE_ID", Integer.class),
                new ColumnInfo("EDGE_TYPE", Integer.class),
                new ColumnInfo("EDGE_ID2", Integer.class),
                new ColumnInfo("EDGE_TIME", Long.class),
                new ColumnInfo("EDGE_DATA", String.class, config.VAR_DATA_SIZE));
    }

    public void runNextTransaction() {

        // TBU
        int x = Generator.generateInt(0,100);
        int nbAborts;
        long begin = System.currentTimeMillis();
        long end = 0;
        BenchmarkTransaction trx;
        if (x < config.PROB_TRX_READ) {
            trx = GenerateReadInput();
//            System.out.println("[" + TaoBenchConstants.Transactions.READ + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
            trxStats.get(TaoBenchConstants.Transactions.READ).addTransaction(nbAborts, end-begin);
//            System.out.println("[" + TaoBenchConstants.Transactions.READ + "] End");
        } else if (x < config.PROB_TRX_READ_TXN) {
            trx = GenerateReadTxnInput(config.TXN_SIZES_5, config.TXN_WEIGHTS_5);
//            System.out.println("[" + TaoBenchConstants.Transactions.READTRANSACTION + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
            trxStats.get(TaoBenchConstants.Transactions.READTRANSACTION).addTransaction(nbAborts, end - begin);
//            System.out.println("[" + TaoBenchConstants.Transactions.READTRANSACTION + "] End");
        } else if (x < config.PROB_TRX_READ_SCAN) {
            trx = GenerateReadScanInput(config.TXN_SIZES_6, config.TXN_WEIGHTS_6);
//            System.out.println("[" + TaoBenchConstants.Transactions.SCAN + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
            trxStats.get(TaoBenchConstants.Transactions.SCAN).addTransaction(nbAborts, end - begin);
//            System.out.println("[" + TaoBenchConstants.Transactions.SCAN + "] End");
        } else if (x < config.PROB_TRX_WRITE) {
            trx = GenerateWriteInput();
//            System.out.println("[" + TaoBenchConstants.Transactions.UPDATE + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
            trxStats.get(TaoBenchConstants.Transactions.WRITE).addTransaction(nbAborts, end-begin);
//            System.out.println("[" + TaoBenchConstants.Transactions.UPDATE + "] End");
        } else {
            trx = GenerateWriteTxnInput(config.TXN_SIZES_7, config.TXN_WEIGHTS_7); // TBU
//            System.out.println("[" + TaoBenchConstants.Transactions.WRITETRANSACTION + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
            trxStats.get(TaoBenchConstants.Transactions.WRITETRANSACTION).addTransaction(nbAborts,
                    end - begin);
//            System.out.println("[" + TaoBenchConstants.Transactions.WRITETRANSACTION + "] End");
        }
    }

    public PointRead GenerateReadInput() {
        int x = Generator.generateInt(0,100);
        int object;
        if (x < config.PROB_TRX_READ_1) {
            object = generateZipf(1);
        } else {
            object = generateUnif(0, group_1);
        }
        return new PointRead(this,object, txn_id++);
    }

    public ReadTransaction GenerateReadTxnInput(List<Integer> values, List<Double> weights) {
        int txnSize = RandDiscreteInt(values, weights);
        return new ReadTransaction(this,txnSize,group_1,group_2, txn_id++);
    }

    public ReadScan GenerateReadScanInput(List<Integer> values, List<Double> weights) {
        int txnSize = RandDiscreteInt(values, weights);
        return new ReadScan(this,txnSize,group_1,group_2,group_3,config.NB_OBJECTS, txn_id++);
    }

    public PointWrite GenerateWriteInput() {
        int x = Generator.generateInt(0,100);
        int object;
        if (x < config.PROB_TRX_READ_1) {
            object = generateZipf(1) + group_3;
        } else {
            object = generateUnif(0, config.NB_OBJECTS) + group_3;
        }
        String val = RandDiscreteString(config.DATA_SIZES, config.DATA_WEIGHTS, false);
        return new PointWrite(this,object,val, txn_id++);
    }

    public WriteTransaction GenerateWriteTxnInput(List<Integer> values, List<Double> weights) {
        int txnSize = RandDiscreteInt(values, weights);
        return new WriteTransaction(this,txnSize,group_3,config.NB_OBJECTS,
                config.DATA_SIZES, config.DATA_WEIGHTS, txn_id++);
    }

    public int generateUnif(int min, int max) {
        return Generator.generateInt(min, max);
    }

    public int generateZipf(int a) {
        if (a == 1) {
            return zipf_1.nextValue();
        }
        return zipf_2.nextValue();
    }
//
//    public BalanceTransaction GenerateBalanceInput() {
//        int custId1 = generateCustomerAccount();
//        return new BalanceTransaction(this, custId1);
//    }
//
//    public DepositCheckingTransaction GenerateDepositCheckingInput() {
//        int custId = generateCustomerAccount();
//        return new DepositCheckingTransaction(this, custId, config.PARAM_DEPOSIT_CHECKING_AMOUNT);
//    }
//
//    public SendPaymentTransaction GenerateSendPaymentInput() {
//        int custId1 = generateCustomerAccount();
//        int custId2 = custId1;
//        while (custId2==custId1) {
//            custId2 = generateCustomerAccount();
//        }
//        return new SendPaymentTransaction(this,custId1, custId2, config.PARAM_SEND_PAYMENT_AMOUNT);
//    }
//
//    public TransactSavingsTransaction GenerateTransactSavingsInput() {
//        int custId = generateCustomerAccount();
//        return new TransactSavingsTransaction(this, custId, config.PARAM_TRANSACT_SAVINGS_AMOUNT);
//    }
//
//    public WriteCheckTransaction GenerateWriteCheckTransaction() {
//        int custId = generateCustomerAccount();
//        return new WriteCheckTransaction(this,custId, config.PARAM_WRITE_CHECK_AMOUNT);
//    }

    public int RandDiscreteInt(List<Integer> values, List<Double> weights) {
        return Generator.getDiscrete(values, weights);
    }

    public String RandDiscreteString(List<Integer> values, List<Double> weights, boolean num_only) {
        int size = Generator.getDiscrete(values, weights);
        return RandString(size, size+1, num_only);
    }

    public char RandCharNum(boolean numOnly) {
        int x = Generator.generateInt(0,numOnly?10:26);
        return ALPHANUM[x];
    }

    /**
     * Generates a random string of size between min and max, and optionally consisting
     * of numbers only
     *
     * @param num_only
     * @return
     */
    public String RandString(int min, int max, boolean num_only) {
        StringBuffer bf = new StringBuffer();
        int len = Generator.generateInt(min, max);
        for (int i = 0; i < len; ++i) {
            bf.append(RandCharNum(num_only));
        }
        return bf.toString();
    }

    public HashMap<Transactions, TrxStats> getTrxStats() {
        return trxStats;
    }

    public void printStats() {
        trxStats.forEach((tType,stat) -> System.out.println("[STAT] " + tType + " " +  stat.getStats()));
    }


}
