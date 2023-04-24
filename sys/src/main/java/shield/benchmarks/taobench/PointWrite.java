package shield.benchmarks.taobench;

import java.util.List;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

public class PointWrite extends BenchmarkTransaction {
    private Integer objId;
    private Integer objId2;
    private Boolean edge = false;
    private String val;
    private long txn_id;

    public PointWrite(TaoBenchGenerator generator, int objId, String val, long txn_id) {
        this.objId = objId;
        if (objId % 2 == 0) {
            this.objId = objId;
            this.objId2 = objId + 1;
        } else {
            this.objId = objId - 1;
            this.objId2 = objId;
        }
        int x = Generator.generateInt(0, 100);
        if (x < 0.5) {
            this.edge = true;
        }
        this.val = val;
        this.client = generator.getClient();
        this.txn_id = txn_id;
    }

    @Override
    public boolean tryRun() {
        try {

//            System.out.println("Point Write");

            List<byte[]> results;

            client.startTransaction();
            ((RedisPostgresClient) client).writeAndExecute(TaoBenchConstants.kObjectsTable, objId.toString(), val.getBytes(),
                    TaoBenchConstants.Transactions.WRITE.ordinal(), this.txn_id);
            if (!this.edge) {
                results = ((RedisPostgresClient) client).writeAndExecute(TaoBenchConstants.kObjectsTable, objId.toString(), val.getBytes(),
                        TaoBenchConstants.Transactions.UPDATE.ordinal(), this.txn_id);
            } else {
                String e = objId.toString() + ":" + objId2.toString();
                results = ((RedisPostgresClient) client).writeAndExecute(TaoBenchConstants.kEdgesTable, e, val.getBytes(),
                        TaoBenchConstants.Transactions.UPDATE.ordinal(), this.txn_id);
            }

            client.commitTransaction();
            return true;
        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
