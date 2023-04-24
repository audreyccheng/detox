package com.oltpbenchmark.benchmarks.taobench;

import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.*;
import com.oltpbenchmark.benchmarks.taobench.procedures.Read;
import com.oltpbenchmark.benchmarks.taobench.utils.ZipfianIntGenerator;
import com.oltpbenchmark.util.ClassUtil;
import com.sun.jersey.api.client.Client;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaoBenchmark extends BenchmarkModule {
    private ZipfianIntGenerator zipf_1;
    private ZipfianIntGenerator zipf_2;

    public TaoBenchmark(WorkloadConfiguration workConf) {
        super("taobench", workConf, false);
        zipf_1 = new ZipfianIntGenerator(TaobenchConstants.NB_OBJECTS, TaobenchConstants.a_1);
        zipf_2 = new ZipfianIntGenerator(TaobenchConstants.NB_OBJECTS, TaobenchConstants.a_2);
    }

    @Override
    protected List<Worker<? extends BenchmarkModule>> makeWorkersImpl(boolean verbose) throws IOException {
        List<Worker<? extends BenchmarkModule>> workers = new ArrayList<Worker<? extends BenchmarkModule>>();
        Client client = RestQuery.makeClient();
        for (int i = 0; i < workConf.getTerminals(); ++i) {
            workers.add(new TaobenchWorker(this, i, zipf_1, zipf_2, client));
        }
        return workers;
    }

    @Override
    protected Loader<? extends BenchmarkModule> makeLoaderImpl() throws SQLException {
        return new TaobenchLoader(this);
    }

    @Override
    protected Package getProcedurePackageImpl() {
        return (Read.class.getPackage());
    }

}
