package com.oltpbenchmark.benchmarks.taobench.procedures;

import com.oltpbenchmark.api.RestQuery;
import com.oltpbenchmark.benchmarks.taobench.TaobenchConstants;
import com.oltpbenchmark.benchmarks.taobench.TaobenchWorker;
import com.oltpbenchmark.benchmarks.taobench.utils.Generator;
import com.oltpbenchmark.benchmarks.taobench.utils.Hash;
import com.sun.jersey.api.client.WebResource;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class Read extends TaobenchProcedure {

    @Override
    public boolean run(WebResource wr, TaobenchWorker w) throws SQLException {
        boolean txnHit = true;
        int x = Generator.generateInt(0, 100);
        int object;
        if (x < TaobenchConstants.PROB_TRX_READ_1) {
            object = w.zipf_1.nextValue();
        } else {
            object = Generator.generateInt(0, TaobenchConstants.GROUP_1);
        }
        String row = TaobenchConstants.OBJECTS_TABLE + object;
        Long id = Hash.hashPersistent(row);
        String stmt = "SELECT data FROM " + TaobenchConstants.OBJECTS_TABLE + " WHERE id = " + id;
        List<Map<String, Object>> r0 = RestQuery.restReadQuery(wr, stmt, w.getId());
        if (r0.isEmpty() || r0.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        return txnHit;
    }

}