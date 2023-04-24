package com.oltpbenchmark.benchmarks.taobench.procedures;

import com.oltpbenchmark.api.RestQuery;
import com.oltpbenchmark.benchmarks.taobench.TaobenchConstants;
import com.oltpbenchmark.benchmarks.taobench.TaobenchWorker;
import com.oltpbenchmark.benchmarks.taobench.utils.Generator;
import com.oltpbenchmark.benchmarks.taobench.utils.Hash;
import com.sun.jersey.api.client.WebResource;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

public class ReadTxn extends TaobenchProcedure {
    @Override
    public boolean run(WebResource wr, TaobenchWorker w) throws SQLException {
        boolean txnHit = true;
        int txnSize = Generator.getDiscrete(TaobenchConstants.TXN_SIZES_5, TaobenchConstants.TXN_WEIGHTS_5);
        Set<Integer> keys = new HashSet<>();

        for (int i = 0; i < txnSize; i++) {
            int objIdRand = -1;
            while (objIdRand < 0) {
                int key;
                if (true) { // i % 2 == 0) { // //
                    key = w.zipf_1.nextValue(); // + group2; // ;
                } else {
                    key =  Generator.generateInt(0, TaobenchConstants.GROUP_1) + TaobenchConstants.GROUP_2; // ; //
                }

                if (!keys.contains(key)) {
                    objIdRand = key;
                    keys.add(key);
                }
            }
            String row = TaobenchConstants.OBJECTS_TABLE + objIdRand;
            Long id = Hash.hashPersistent(row);
            String stmt = "SELECT data FROM " + TaobenchConstants.OBJECTS_TABLE + " WHERE id = " + id;
            List<Map<String, Object>> r0 = RestQuery.restReadQuery(wr, stmt, w.getId());
            if (r0.isEmpty() || r0.get(0).get("CACHE_HIT") == null) {
                txnHit = false;
            }
        }
        return txnHit;
    }
}
