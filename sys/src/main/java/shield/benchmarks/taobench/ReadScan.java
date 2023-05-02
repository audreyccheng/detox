package shield.benchmarks.taobench;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

public class ReadScan extends BenchmarkTransaction {
    private TaoBenchGenerator generator;
    private int txnSize;
    private int group1;
    private int group2;
    private int group3;
    private int totalSize;
    private long txn_id;

    public ReadScan(TaoBenchGenerator generator, int txnSize, int group1, int group2, int group3, int totalSize, long txn_id) {
        this.txnSize= txnSize;
        this.client = generator.getClient();
        this.group1 = group1;
        this.group2 = group2;
        this.group3 = group3;
        this.generator = generator;
        this.totalSize = totalSize;
        this.txn_id = txn_id;
    }

    @Override
    public boolean tryRun() {
        try {

//            System.out.println("Read Scan");

            Set<Integer> keys = new HashSet<Integer>();
            List<byte[]> results;

            client.startTransaction();

            for (int i = 0; i < this.txnSize; i++) {
                Integer objIdRand = -1;
                while (objIdRand < 0) {
                    Integer key;
                    if (i % 5 == 0){ // i % 2 == 0) { //
                        key = generator.generateZipf(0); // null-2 // + group2; // + group2
                    } else if (i < this.txnSize - 1) { //else { //
                        key = Generator.generateInt(0, group1); // null-2 // + group3; // + group3
                    } else {
                            key = Generator.generateInt(0, totalSize - group3) + group2; // // + group3; //
    //                        System.out.printf("totalSize %d key %d\n", totalSize, key);
                        }
                        if (!keys.contains(key)) {
                            objIdRand = key;
                            keys.add(key);
                        }
                    }

                int x = Generator.generateInt(0,100);
                ((RedisPostgresClient) client).read(TaoBenchConstants.kObjectsTable, objIdRand.toString(),
                        TaoBenchConstants.Transactions.SCAN.ordinal(), this.txn_id);
            }
//            System.out.printf("READ SCAN keys %s\n", keys.toString());

            results = client.execute();

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
