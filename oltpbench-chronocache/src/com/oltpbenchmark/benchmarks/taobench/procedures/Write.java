package com.oltpbenchmark.benchmarks.taobench.procedures;

import com.oltpbenchmark.api.RestQuery;
import com.oltpbenchmark.benchmarks.taobench.TaobenchConstants;
import com.oltpbenchmark.benchmarks.taobench.TaobenchWorker;
import com.oltpbenchmark.benchmarks.taobench.utils.Generator;
import com.oltpbenchmark.benchmarks.taobench.utils.Hash;
import com.oltpbenchmark.benchmarks.taobench.utils.TaobenchGenerator;
import com.sun.jersey.api.client.WebResource;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class Write extends TaobenchProcedure {

    @Override
    public boolean run(WebResource wr, TaobenchWorker w) throws SQLException {
        boolean txnHit = false; // write is never txn hit
        int x = Generator.generateInt(0,100);
        int object;
        if (x < TaobenchConstants.PROB_TRX_READ_1) {
            object = w.zipf_1.nextValue() + TaobenchConstants.GROUP_3;
        } else {
            object = Generator.generateInt(0, TaobenchConstants.NB_OBJECTS) + TaobenchConstants.GROUP_3;
        }
        String row = TaobenchConstants.OBJECTS_TABLE + object;
        Long id = Hash.hashPersistent(row);
        String data = TaobenchGenerator.RandDiscreteString(TaobenchConstants.DATA_SIZES, TaobenchConstants.DATA_WEIGHTS, false);
        String stmt = "INSERT INTO " + TaobenchConstants.OBJECTS_TABLE +
                "(id, data) VALUES (" + id + ", '" + data + "')";
        RestQuery.restOtherQuery(wr, stmt, w.getId());
        return txnHit;
    }
}
