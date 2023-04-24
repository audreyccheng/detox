package chronocache.core;

import chronocache.db.DB;
import chronocache.db.DBFactory;
import chronocache.db.DBException;
import chronocache.core.qry.QueryIdentifier;
import chronocache.util.UndirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import java.util.*;
import java.io.*;

public class CliqueDetectionTest
{
    private Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Test
    public void detectCliquesGeneralTest() throws InterruptedException
    {
        // Build the graph which will have clique detection run on it
        // The graph is K4 with two 'antenae' (connected at vertices 2 and 4)
        // and one completely disconnected vertex
        UndirectedGraph graph = new UndirectedGraph();

        QueryIdentifier[] queryIds = new QueryIdentifier[8];
        for (int i = 0; i < 8; i++)
        {
            queryIds[i] = new QueryIdentifier(i + 1);
        }
        
        // Cleanest way I can see to initialize the graph
        ArrayList<List<QueryIdentifier>> adjacentVertices = new ArrayList<List<QueryIdentifier>>();
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[1], queryIds[2], queryIds[3])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[0], queryIds[2], queryIds[3], queryIds[4])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[0], queryIds[1], queryIds[3])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[0], queryIds[1], queryIds[2], queryIds[5])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[1])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[3], queryIds[6])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[5])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>());

        for (int i = 0; i < 8; i ++)
        {
            graph.add(queryIds[i], adjacentVertices.get(i));
        }

        assertThat(graph.detectCliques().size(), equalTo(4));
    }

    @Test
    public void detectCliquesIsolatedVerticesTest() throws InterruptedException
    {
        UndirectedGraph graph = new UndirectedGraph();

        QueryIdentifier[] queryIds = new QueryIdentifier[6];
        for (int i = 0; i < 6; i++)
        {
            queryIds[i] = new QueryIdentifier(i + 1);
        }

        ArrayList<List<QueryIdentifier>> adjacentVertices = new ArrayList<List<QueryIdentifier>>();
        
        for (int i = 0; i < 6; i ++)
        {
            adjacentVertices.add(new LinkedList<QueryIdentifier>());
            graph.add(queryIds[i], adjacentVertices.get(i));
        }

        assertThat(graph.detectCliques().size(), equalTo(0));
    }

    @Test
    public void detectCliquesIsolatedVerticesWithSelfEdgesTest() throws InterruptedException
    {
        UndirectedGraph graph = new UndirectedGraph();

        QueryIdentifier[] queryIds = new QueryIdentifier[6];
        for (int i = 0; i < 6; i++)
        {
            queryIds[i] = new QueryIdentifier(i + 1);
        }

        ArrayList<List<QueryIdentifier>> adjacentVertices = new ArrayList<List<QueryIdentifier>>();
        
        for (int i = 0; i < 6; i ++)
        {
            adjacentVertices.add(new LinkedList<QueryIdentifier>());
            graph.add(queryIds[i], adjacentVertices.get(i));
            graph.add(queryIds[i], queryIds[i]);
        }

        assertThat(graph.detectCliques().size(), equalTo(6));
    }

    @Test
    public void detectCliquesSingleEdgesTest() throws InterruptedException
    {
        UndirectedGraph graph = new UndirectedGraph();

        QueryIdentifier[] queryIds = new QueryIdentifier[6];
        for (int i = 0; i < 6; i++)
        {
            queryIds[i] = new QueryIdentifier(i + 1);
        }

        ArrayList<List<QueryIdentifier>> adjacentVertices = new ArrayList<List<QueryIdentifier>>();
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[1])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[0])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[3])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[2])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[5])));
        adjacentVertices.add(new LinkedList<QueryIdentifier>(Arrays.asList(queryIds[4])));
        
        for (int i = 0; i < 6; i ++)
        {
            graph.add(queryIds[i], adjacentVertices.get(i));
        }

        assertThat(graph.detectCliques().size(), equalTo(3));
    }

    @Test
    public void detectCliquesK12Test() throws InterruptedException
    {
        UndirectedGraph graph = new UndirectedGraph();

        QueryIdentifier[] queryIds = new QueryIdentifier[12];
        for (int i = 0; i < 12; i++)
        {
            queryIds[i] = new QueryIdentifier(i + 1);
        }

        List<QueryIdentifier> adjacentVertices = new LinkedList<QueryIdentifier>();
        for (int i = 0; i < 12; i++)
        {
            adjacentVertices.add(queryIds[i]);
        }
        
        for (int i = 0; i < 12; i ++)
        {
            graph.add(queryIds[i], adjacentVertices);
        }

        assertThat(graph.detectCliques().size(), equalTo(1));
    }

    @Test
    public void detectCliquesForestK6Test() throws InterruptedException
    {
        UndirectedGraph graph = new UndirectedGraph();

        QueryIdentifier[] queryIds = new QueryIdentifier[12];
        for (int i = 0; i < 12; i++)
        {
            queryIds[i] = new QueryIdentifier(i + 1);
        }

        ArrayList<List<QueryIdentifier>> adjacentVertices = new ArrayList<List<QueryIdentifier>>();
        adjacentVertices.add(new LinkedList<QueryIdentifier>());
        adjacentVertices.add(new LinkedList<QueryIdentifier>());

        for (int i = 0; i < 12; i++)
        {
            adjacentVertices.get(i / 6).add(queryIds[i]);
        }
        
        for (int i = 0; i < 12; i ++)
        {
            graph.add(queryIds[i], adjacentVertices.get(i / 6));
        }
        assertThat(graph.detectCliques().size(), equalTo(2));
    }
}
