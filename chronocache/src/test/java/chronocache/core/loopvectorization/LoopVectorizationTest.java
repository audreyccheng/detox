package chronocache.core;

import chronocache.db.DB;
import chronocache.db.DBFactory;
import chronocache.db.DBException;
import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.core.qry.QueryStream;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.MarkovGraph;
import chronocache.core.MarkovNode;
import chronocache.util.UndirectedGraph;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;
import java.io.*;

public class LoopVectorizationTest
{
    int defaultQueryExecutionTime = 17;

    private VersionVector version = new VersionVector(new ArrayList<Long>());
    private String queryTraceDirectoryPath = "/home/kjlangen/Documents/";
    private static final DB db = new DBFactory().getDBInstance(DBFactory.DBType.REAL_DB); // Set to REAL_DB for simpleEndToEndTest
    private Logger logger = LoggerFactory.getLogger( this.getClass() );

    //@Test
    public void simpleExperiment() throws Exception
    {
        // Query Trace to use
        BufferedReader queryStrings = new BufferedReader(new FileReader(queryTraceDirectoryPath + "QueryTrace.txt"));

        // Statistics
        long totalQueryTime = 0;
        long executedQueryCount = 0;
        long currentAverage = 0;
        long start = 0;
        long end = 0;

        //Test
        String query;
        long testStart = System.nanoTime();
        while((query = queryStrings.readLine()) != null && ((System.nanoTime() - testStart) / 1000000) < 900000)
        {
            // Send query to database and time latency
            start = System.nanoTime();
            db.query(1, query);
            end = System.nanoTime();

            // Calculate average latency
            totalQueryTime += ((end - start) / 1000000); // Convert to ms from ns
            executedQueryCount += 1;
            currentAverage = (totalQueryTime / executedQueryCount); // Current average in ms
            logger.debug("STAT: Time spent executing queries: {}ms", totalQueryTime);
            logger.debug("STAT: Executed queries: {}", executedQueryCount);
            logger.debug("STAT: Average latency: {}ms", currentAverage);

            Thread.sleep(199); // Keep the queries executing at a consistent 200ms interval
        }

        // Stop and print statistics
        db.stopDB();

    }
}
