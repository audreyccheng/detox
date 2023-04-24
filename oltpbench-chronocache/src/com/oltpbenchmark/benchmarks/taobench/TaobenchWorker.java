package com.oltpbenchmark.benchmarks.taobench;

import com.oltpbenchmark.api.RestQuery;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.taobench.procedures.TaobenchProcedure;
import com.oltpbenchmark.benchmarks.taobench.utils.ZipfianIntGenerator;
import com.oltpbenchmark.types.TransactionStatus;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import java.sql.SQLException;

public class TaobenchWorker extends Worker<TaoBenchmark> {
    public ZipfianIntGenerator zipf_1;
    public ZipfianIntGenerator zipf_2;
    private WebResource wr;
    private long txnHits;
    private long txnTotal;
    public TaobenchWorker(TaoBenchmark benchmarkModule, int id, ZipfianIntGenerator zipf_1, ZipfianIntGenerator zipf_2, Client client
    ) {
        super(benchmarkModule, id);
        this.zipf_1 = zipf_1;
        this.zipf_2 = zipf_2;
        String hostname = getWorkloadConfiguration().getDBRestHost();
        String target = "http://" + hostname + ":8080/chronocache/rest/query/" + this.getId();
        this.wr = client.resource(target);

        txnHits = 0;
        txnTotal = 0;
    }

    public long getTxnHits() {
        return txnHits;
    }

    public long getTxnTotal() {
        return txnTotal;
    }

    public void resetTxnStats() {
        txnHits = 0;
        txnTotal = 0;
    }

    @Override
    protected TransactionStatus executeWork(TransactionType txnType) {
        TaobenchProcedure proc = (TaobenchProcedure) this.getProcedure(txnType.getProcedureClass());
        try {
            if (proc.run(wr, this)) {
                txnHits += 1;
            }
        } catch (SQLException e) {
            return TransactionStatus.RETRY;
        }
        
        txnTotal += 1;
        return TransactionStatus.SUCCESS;
    }
}
