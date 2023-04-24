package com.oltpbenchmark.benchmarks.taobench.procedures;

import com.oltpbenchmark.api.RestQuery;
import com.oltpbenchmark.benchmarks.taobench.TaobenchConstants;
import com.oltpbenchmark.benchmarks.taobench.TaobenchWorker;
import com.oltpbenchmark.benchmarks.taobench.utils.Generator;
import com.oltpbenchmark.benchmarks.taobench.utils.Hash;
import com.oltpbenchmark.benchmarks.taobench.utils.TaobenchGenerator;
import com.sun.jersey.api.client.WebResource;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

public class WriteTxn extends TaobenchProcedure {
    @Override
    public boolean run(WebResource wr, TaobenchWorker w) throws SQLException {
        boolean txnHit = false; // write is never txn hit
        int txnSize = Generator.getDiscrete(TaobenchConstants.TXN_SIZES_7, TaobenchConstants.TXN_WEIGHTS_7);
        Set<Integer> keys = new HashSet<>();
        for (int i = 0; i < txnSize; i++) {
            int objIdRand = -1;
            while (objIdRand < 0) {
                int key;
                if (i % 2 == 0) {
                    key = w.zipf_1.nextValue() + TaobenchConstants.GROUP_3;
                } else {
                    key = Generator.generateInt(0, TaobenchConstants.NB_OBJECTS);
                }
                if (!keys.contains(key)) {
                    objIdRand = key;
                    keys.add(key);
                }
            }
            String row = TaobenchConstants.OBJECTS_TABLE + objIdRand;
            Long id = Hash.hashPersistent(row);
            String data = TaobenchGenerator.RandDiscreteString(TaobenchConstants.DATA_SIZES, TaobenchConstants.DATA_WEIGHTS, false);
            String stmt = "INSERT INTO " + TaobenchConstants.OBJECTS_TABLE +
                    "(id, data) VALUES (" + id + ", '" + data + "')";
            RestQuery.restOtherQuery(wr, stmt, w.getId());
        }
        return txnHit;
    }
}
