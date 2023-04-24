package com.oltpbenchmark.benchmarks.taobench;

import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.benchmarks.taobench.utils.TaobenchGenerator;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

public class TaobenchLoader extends Loader<TaoBenchmark> {
    private static final Logger LOG = Logger.getLogger(TaobenchLoader.class);

    public TaobenchLoader(TaoBenchmark benchmark) {
        super(benchmark);
    }

    @Override
    public List<LoaderThread> createLoaderThreads() throws SQLException {
        // First load data
        int ranges = TaobenchConstants.NB_OBJECTS / TaobenchConstants.NB_LOADER_THREADS;
        int accountsToLoad = ranges > 0 ? ranges : 1;
        List<LoaderThread> threads = new LinkedList<>();

        AtomicLong progressCount = new AtomicLong(0);
        for (int i = 0; i < TaobenchConstants.NB_OBJECTS; i += accountsToLoad) {
            final int j = i;
            threads.add(new LoaderThread() {
                @Override
                public void load(Connection conn) throws SQLException {
                    int endAccount =
                            (j + accountsToLoad > TaobenchConstants.NB_OBJECTS) ? TaobenchConstants.NB_OBJECTS :
                                    (j + accountsToLoad);
                    loadAccounts(j, endAccount, progressCount, conn);
                }
            });
        }
        return threads;
    }

    private static void loadAccounts(int start, int end, AtomicLong count, Connection conn)
            throws SQLException {

        assert (end <= TaobenchConstants.NB_OBJECTS);

        LOG.info("Loading Accounts: " + start + " to " + end);

        Integer custId, custId2;
        while (start < end) {
            for (int ij = 0; ij < 10; ij += 2) {
                custId = start + ij;
                custId2 = custId + 1;
                if (custId >= end)
                    break;
                // Update objects table
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + TaobenchConstants.OBJECTS_TABLE + " (id, time, data) VALUES (?, ?, ?)");
                String data = TaobenchGenerator.RandDiscreteString(TaobenchConstants.DATA_SIZES, TaobenchConstants.DATA_WEIGHTS, false);
                stmt.setLong(1, custId);
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(3, data);
                stmt.execute();

                data = TaobenchGenerator.RandDiscreteString(TaobenchConstants.DATA_SIZES, TaobenchConstants.DATA_WEIGHTS, false);
                stmt.setLong(1, custId2);
                stmt.setLong(2, System.currentTimeMillis() + 1);
                stmt.setString(3, data);
                stmt.execute();
                // TODO(jchan): insert edge
            }
            LOG.info(count.incrementAndGet() * 10 + "/" + TaobenchConstants.NB_OBJECTS + " Start: " + start);
            start += 10;
        }
    }

}

