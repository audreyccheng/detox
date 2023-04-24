package chronocache.core;

import chronocache.db.DB;
import chronocache.db.DBFactory;
import chronocache.db.DBException;
import chronocache.core.MarkovGraph;
import chronocache.core.MarkovNode;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import chronocache.core.Parameters;
import chronocache.core.VersionVector;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.Multimap;

import org.junit.Test;

import java.util.*;

public class BlacklistingAvoidanceTest
{
    @Test
    public void testCliqueBlacklistAvoidance()
    {
        Parameters.TRACKING_PERIOD = 1;

        // Add nodes
        // Nodes 0, 1, 2
        MarkovGraph g = new MarkovGraph( 0 );
        QueryIdentifier id = new QueryIdentifier(0);
        QueryIdentifier id1 = new QueryIdentifier(1);
        QueryIdentifier id2 = new QueryIdentifier(2);
        QueryIdentifier id3 = new QueryIdentifier(3);
        MarkovNode node = g.getOrAddNode(id);
        MarkovNode node1 = g.getOrAddNode(id1);
        MarkovNode node2 = g.getOrAddNode(id2);
        MarkovNode node3 = g.getOrAddNode(id3);

        // Add 12 results, one for each query in the loop
        List<Map<String, Object>> resultSet = new LinkedList<>();
        for (int i = 0; i < 2; i++)
        {
            Map<String, Object> nextRow = new HashMap<>();
            nextRow.put("id", (Object) ("" + i));
            resultSet.add(nextRow);
        }
        g.addResultSet(id, new QueryResult(resultSet, new VersionVector(new ArrayList<Long>())));

        // Build the MarkovGraph
        node.addEdgeTraversal(node1);
        node.addEdgeTraversal(node2);
        node.addEdgeTraversal(node3);

        // Add input parameters for mappping test
        List<String> params = new LinkedList<>();
        params.add("0");
        g.addInputParameters(id1, params);
        g.findAllParameterMappingsForNode(id1);
        params = new LinkedList<>();
        params.add("1");
        g.addInputParameters(id2, params);
        g.findAllParameterMappingsForNode(id2);
        params = new LinkedList<>();
        params.add("2");
        g.addInputParameters(id3, params);
        g.findAllParameterMappingsForNode(id3);

        // Test query0 -> query1 mapping
        Multimap<Integer, String> mappings = g.lookupMappings(id, id1);
        assertThat(mappings, not(nullValue()));
        assertThat(mappings.keySet().size(), is(1));

        // Test query0 -> query2 mapping
        mappings = g.lookupMappings(id, id2);
        assertThat(mappings, not(nullValue()));
        assertThat(mappings.keySet().size(), is(1));

        // Test query0 -> query3 mapping
        mappings = g.lookupMappings(id, id3);
        assertThat(mappings, nullValue());
    }
}
