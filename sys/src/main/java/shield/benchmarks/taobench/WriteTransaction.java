package shield.benchmarks.taobench;

import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WriteTransaction extends BenchmarkTransaction {
    private TaoBenchGenerator generator;
    private int txnSize;
    private int group3;
    private int totalSize;
    private List<Integer> sizes;
    private List<Double> weights;
    private long txn_id;

    public WriteTransaction(TaoBenchGenerator generator, int txnSize, int group3, int totalSize, List<Integer> sizes,
                            List<Double> weights, long txn_id) {
        this.txnSize= txnSize;
        this.client = generator.getClient();
        this.group3 = group3;
        this.totalSize = totalSize;
        this.generator = generator;
        this.sizes = sizes;
        this.weights = weights;
        this.txn_id = txn_id;
    }

    @Override
    public boolean tryRun() {
        try {

//            System.out.println("Write Transaction");

            Set<Integer> keys = new HashSet<Integer>();
            List<byte[]> results;

            client.startTransaction();

            for (int i = 0; i < this.txnSize; i++) {
                Integer objIdRand = -1;
                while (objIdRand < 0) {
                    Integer key;
                    if (i % 2 == 0) {
                        key = generator.generateZipf(1) + group3;
                    } else {
                        key = Generator.generateInt(0, totalSize);
                    }
                    if (!keys.contains(key)) {
                        objIdRand = key;
                        keys.add(key);
                    }
                }
                String val = generator.RandDiscreteString(sizes,weights,false);

                int x = Generator.generateInt(0, 100);
                ((RedisPostgresClient) client).write(TaoBenchConstants.kObjectsTable, objIdRand.toString(), val.getBytes(),
                        TaoBenchConstants.Transactions.WRITETRANSACTION.ordinal(), this.txn_id);
            }

            results = client.execute();

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
