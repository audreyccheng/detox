package chronocache.core;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import chronocache.core.qry.QueryIdentifier;
import chronocache.core.Vectorizable;
import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;

public class VectorizableDependencyTableTest {

    @Test
    public void insertRetrieveSingleADQ() {
        String queryText = "SELECT 1";
        QueryIdentifier queryId = new QueryIdentifier(1);
        DependencyGraph dependencyGraph = new DependencyGraph();

        VectorizableType vType = new VectorizableType();
        vType.markAsADQ();

        Vectorizable fdq = new Vectorizable(dependencyGraph, queryId, queryText, vType);

        VectorizableDependencyTable dependencyTable =
        new VectorizableDependencyTable();

        // Test insertion
        dependencyTable.addFDQ(fdq);
        assertThat(dependencyTable.size(), is(1));

        // Test retrieveal
        Set<Vectorizable> adqs = dependencyTable.getAlwaysDefinedQueries();
        assertThat(adqs.size(), is(1));

        // Test retrieval
        Map<QueryIdentifier, Vectorizable> adqsMap =
        dependencyTable.getIdKeyedADQs();
        assertThat(adqsMap.get(queryId), is(fdq));
    }

    @Test
    public void insertRetrieveSingleFDQ() {
        String queryText = "SELECT 1";
        QueryIdentifier queryId = new QueryIdentifier(1);
        DependencyGraph dependencyGraph = new DependencyGraph();
        VectorizableType vType = new VectorizableType();
        vType.markAsFDQ();
        Vectorizable fdq =
        new Vectorizable(dependencyGraph, queryId, queryText, vType);

        VectorizableDependencyTable dependencyTable =
        new VectorizableDependencyTable();

        // Test insertion
        dependencyTable.addFDQ(fdq);
        assertThat(dependencyTable.size(), is(1));

        // Test retrieval
        Set<Vectorizable> fdqs = dependencyTable.getFDQs( queryId );
		assertThat( fdqs.size(), equalTo( 1 ) );
		Vectorizable fdqRetrieved = fdqs.iterator().next();
        assertThat( fdq, is( fdqRetrieved ) );

        // Test that this isn't an ADQ
        Set<Vectorizable> adqs = dependencyTable.getAlwaysDefinedQueries();
        assertThat( adqs.size(), is( 0 ) );

        // Test FDQ is ready since the dependency graph is empty
        List<Vectorizable> readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        assertThat(readyFDQs.size(), is(1));
    }

    @Test
    public void insertRetrieveMultipleFDQ() {
        String queryText1 = "SELECT 1 FROM T";
        String queryText2 = "SELECT 2 FROM T2";
        QueryIdentifier queryId1 = new QueryIdentifier(1);
        QueryIdentifier queryId2 = new QueryIdentifier(2);
        DependencyGraph dependencyGraph1 = new DependencyGraph();
        DependencyGraph dependencyGraph2 = new DependencyGraph();

        VectorizableType vType1 = new VectorizableType();
        vType1.markAsFDQ();
        Vectorizable fdq1 =
        new Vectorizable(dependencyGraph1, queryId1, queryText1, vType1);
        VectorizableType vType2 = new VectorizableType();
        vType2.markAsADQ();
        Vectorizable fdq2 =
        new Vectorizable(dependencyGraph2, queryId2, queryText2, vType2);

        VectorizableDependencyTable dependencyTable =
        new VectorizableDependencyTable();

        // Test insertion
        dependencyTable.addFDQ(fdq1);
        dependencyTable.addFDQ(fdq2);
        assertThat( dependencyTable.size(), is( 2 ) );

        // Test retrieval
		Set<Vectorizable> fdqs = dependencyTable.getFDQs( queryId1 );
		assertThat( fdqs.size(), equalTo( 1 ) );
        Vectorizable fdqRetrieved1 = fdqs.iterator().next();
		fdqs = dependencyTable.getFDQs( queryId2 );
		assertThat( fdqs.size(), equalTo( 1 ) );
        Vectorizable fdqRetrieved2 = fdqs.iterator().next();

        assertThat(fdq1, is(fdqRetrieved1));
        assertThat(fdq2, is(fdqRetrieved2));

        // Test retrieving only ADQs
        Set<Vectorizable> adqs = dependencyTable.getAlwaysDefinedQueries();
        assertThat(adqs.size(), is(1));

        // Test FDQ is ready
        List<Vectorizable> readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        assertThat(readyFDQs.size(), is(2));
    }

    public Query constructTestQuery(String s) {
        AntlrParser p = new AntlrParser();
        return new Query(s, p.buildParseTree(s));
    }

    @Test
    public void insertParentFDQ() {
        // graph:
        //			3--|
        //			   v
        // 1 --> 2 --> 4

        Query q1 = constructTestQuery(
            "SELECT A from T1 where T1.B > 7"); // not actually an ADQ (7 is an ?)
        Query q2 = constructTestQuery(
            "SELECT C, D from T2 where T2.A = 8"); // dependent on result from q1
        Query q3 = constructTestQuery("SELECT E from T3"); // an ADQ
        Query q4 = constructTestQuery(
            "SELECT F, G from T4 where T4.E = 23 and T4.C = 16"); // dependent on q3
                                                                  // and q2

        VectorizableDependencyTable dependencyTable =
        new VectorizableDependencyTable();

        DependencyGraph d3 = new DependencyGraph();
        VectorizableType vType3 = new VectorizableType();
        vType3.markAsADQ();
        Vectorizable fdq3 = new Vectorizable( d3, q3.getId(), q3.getQueryString(), vType3 );

        // Test insertion
        dependencyTable.addFDQ(fdq3);
        List<Vectorizable> readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        // Test FDQ is ready
        assertThat(readyFDQs.size(), is(1));
        assertThat(readyFDQs, contains(fdq3));

        // Test retrieving only ADQs
        Set<Vectorizable> adqs = dependencyTable.getAlwaysDefinedQueries();
        assertThat(adqs.size(), is(1));
        assertThat(adqs, contains(fdq3));

        DependencyGraph d2 = new DependencyGraph();
        d2.addBaseQuery(q1.getId(), q1);
        Multimap<Integer, String> mappings2 = HashMultimap.create();
        mappings2.put(0, "A");
        QueryMappingEntry qme2 = new QueryMappingEntry(q1.getId(), q2, mappings2);
        d2.addDependencyForQuery(q2.getId(), qme2);
        VectorizableType vType2 = new VectorizableType();
        vType2.markAsFDQ();
        Vectorizable fdq2 =
        new Vectorizable(d2, q2.getId(), q2.getQueryString(), vType2);
        dependencyTable.addFDQ(fdq2);

        // fdq2 is not ready
        readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        // Test FDQ is ready
        assertThat(readyFDQs.size(), is(1));
        assertThat(readyFDQs, contains(fdq3));

        // Test retrieving only ADQs
        adqs = dependencyTable.getAlwaysDefinedQueries();
        assertThat(adqs.size(), is(1));
        assertThat(adqs, contains(fdq3));

        dependencyTable.markExecutedDependency(q1);
        // now fdq2 is ready
        readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        // Test FDQ is ready
        assertThat(readyFDQs.size(), is(2));
        assertThat(readyFDQs, containsInAnyOrder(fdq3, fdq2));

        // now add q4
        // this should get rid of q2 from the readyFDQs because it is subsumed by q4
        DependencyGraph d4 = new DependencyGraph();
        d4.addBaseQuery(q1.getId(), q1);
        d4.addBaseQuery(q3.getId(), q3);

        Multimap<Integer, String> mappings4a = HashMultimap.create();
        mappings4a.put(0, "E");
        QueryMappingEntry qme4a = new QueryMappingEntry(q3.getId(), q4, mappings4a);
        d4.addDependencyForQuery(q4.getId(), qme4a);
        Multimap<Integer, String> mappings4b = HashMultimap.create();
        mappings4b.put(1, "C");
        QueryMappingEntry qme4b = new QueryMappingEntry(q2.getId(), q4, mappings4b);
        d4.addDependencyForQuery(q4.getId(), qme4b);
        Multimap<Integer, String> mappings4c = HashMultimap.create();
        mappings4c.put(0, "A");
        QueryMappingEntry qme4c = new QueryMappingEntry(q1.getId(), q2, mappings4c);
        d4.addDependencyForQuery(q2.getId(), qme4c);

        VectorizableType vType4 = new VectorizableType();
        vType4.markAsFDQ();
        Vectorizable fdq4 =
        new Vectorizable(d4, q4.getId(), q4.getQueryString(), vType4);

        dependencyTable.addFDQ(fdq4);

        dependencyTable.markExecutedDependency(q1);
        // now fdq4, fdq3 are ready. FDQ2 is also ready but it is a child of fdq4
        readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        // Test FDQ is ready
        assertThat(readyFDQs.size(), is(2));
        assertThat(readyFDQs, containsInAnyOrder(fdq3, fdq4));
    }

    @Test
    public void insertParentFDQShouldMaterialize() {
        // graph, 3 is a base query
        //			3--|
        //			   v
        // 1 --> 2 --> 4

        Query q1 = constructTestQuery(
            "SELECT A from T1 where T1.B > 7"); // not actually an ADQ (7 is an ?)
        Query q2 = constructTestQuery(
            "SELECT C, D from T2 where T2.A = 8"); // dependent on result from q1
        Query q3 = constructTestQuery("SELECT E from T3 where E < 27"); // an FDQ
        Query q4 = constructTestQuery(
            "SELECT F, G from T4 where T4.E = 23 and T4.C = 16"); // dependent on q3
                                                                  // and q2

        VectorizableDependencyTable dependencyTable =
        new VectorizableDependencyTable();

        DependencyGraph d2 = new DependencyGraph();
        d2.addBaseQuery(q1.getId(), q1);
        Multimap<Integer, String> mappings2 = HashMultimap.create();
        mappings2.put(0, "A");
        QueryMappingEntry qme2 = new QueryMappingEntry(q1.getId(), q2, mappings2);
        d2.addDependencyForQuery(q2.getId(), qme2);
        VectorizableType vType2 = new VectorizableType();
        vType2.markAsFDQ();
        Vectorizable fdq2 =
        new Vectorizable(d2, q2.getId(), q2.getQueryString(), vType2);
        dependencyTable.addFDQ(fdq2);

        // fdq2 is not ready
        List<Vectorizable> readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        assertThat(readyFDQs.size(), is(0));

        dependencyTable.markExecutedDependency(q1);
        // now fdq2 is ready
        readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        // Test FDQ is ready
        assertThat(readyFDQs.size(), is(1));
        assertThat(readyFDQs, containsInAnyOrder(fdq2));

        // now add q4
        // this should not get rid of q2 from the readyFDQs because it should be
        // materialized
        DependencyGraph d4 = new DependencyGraph();
        d4.addBaseQuery(q1.getId(), q1);
        d4.addBaseQuery(q3.getId(), q3);

        Multimap<Integer, String> mappings4a = HashMultimap.create();
        mappings4a.put(0, "E");
        QueryMappingEntry qme4a = new QueryMappingEntry(q3.getId(), q4, mappings4a);
        d4.addDependencyForQuery(q4.getId(), qme4a);
        Multimap<Integer, String> mappings4b = HashMultimap.create();
        mappings4b.put(1, "C");
        QueryMappingEntry qme4b = new QueryMappingEntry(q2.getId(), q4, mappings4b);
        d4.addDependencyForQuery(q4.getId(), qme4b);
        Multimap<Integer, String> mappings4c = HashMultimap.create();
        mappings4c.put(0, "A");
        QueryMappingEntry qme4c = new QueryMappingEntry(q1.getId(), q2, mappings4c);
        d4.addDependencyForQuery(q2.getId(), qme4c);

        VectorizableType vType4 = new VectorizableType();
        vType4.markAsFDQ();
        Vectorizable fdq4 =
        new Vectorizable(d4, q4.getId(), q4.getQueryString(), vType4);

        dependencyTable.addFDQ(fdq4);
		Set<Vectorizable> fdq2Clones = dependencyTable.getFDQs( fdq2.getId() );
		assertThat( fdq2Clones.size(), equalTo( 1 ) );
		Vectorizable fdq2Clone = fdq2Clones.iterator().next();

		Set<Vectorizable> fdq4Clones = dependencyTable.getFDQs( fdq4.getId() );
		assertThat( fdq4Clones.size(), equalTo( 1 ) );
		Vectorizable fdq4Clone = fdq4Clones.iterator().next();

        assertThat(fdq2Clone.getShouldMaterializeVectorizable(), is(true));
        assertThat(fdq4Clone.getShouldMaterializeVectorizable(), is(false));

        dependencyTable.markExecutedDependency(q1);
        // now fdq2 is ready
        readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        // Test FDQ is ready
        assertThat(readyFDQs.size(), is(1));
        assertThat(readyFDQs, containsInAnyOrder(fdq2Clone));

        dependencyTable.markExecutedDependency(q1);
        dependencyTable.markExecutedDependency(q3);
        // now fdq4 is ready
        readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
        // Test FDQ's are ready
        assertThat(readyFDQs.size(), is(2));
        assertThat(readyFDQs, containsInAnyOrder(fdq2Clone, fdq4Clone));
    }

    @Test
    public void testFilterReadyVectorizables() {
        // Two dependency graphs, each of which have two queries for four queries total
        Query q0 = constructTestQuery( "SELECT a FROM hats WHERE a > 7" );
        Query q1 = constructTestQuery( "SELECT b FROM bats WHERE b > 12" );
        Query q2 = constructTestQuery( "SELECT c FROM cats WHERE c = 8" );
        Query q3 = constructTestQuery( "SELECT d FROM mats WHERE d < 8" );

        // Create the dependency table
        VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

        // Create the dependency graphs to go in the table
        DependencyGraph graph0 = new DependencyGraph();
        graph0.addBaseQuery( q0.getId(), q0 );
        Multimap<Integer, String> mappings0 = HashMultimap.create();
        mappings0.put(0, "a");
        QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
        graph0.addDependencyForQuery( q1.getId(), qme0 );

        DependencyGraph graph1 = new DependencyGraph();
        graph1.addBaseQuery( q2.getId(), q2 );
        Multimap<Integer, String> mappings1 = HashMultimap.create();
        mappings1.put(0, "c");
        QueryMappingEntry qme1 = new QueryMappingEntry( q2.getId(), q3, mappings1 );
        graph1.addDependencyForQuery( q3.getId(), qme1 );

        // Create the vectorizables from the dependency graphs
        VectorizableType vType0 = new VectorizableType();
        vType0.markAsFDQ();
        Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0);

        VectorizableType vType1 = new VectorizableType();
        vType1.markAsFDQ();
        Vectorizable fdq1 = new Vectorizable( graph1, q3.getId(), q3.getQueryString(), vType1);

        // Add the vectorizables to the dependency table
        dependencyTable.addFDQ(fdq0);
        dependencyTable.addFDQ(fdq1);

        // Mark base queries as ready
        dependencyTable.markExecutedDependency( q0 );
        dependencyTable.markExecutedDependency( q2 );

        // Now get ready things split into different sets by q1
        Set<Vectorizable> containingVectorizables = new HashSet<>();
        Set<Vectorizable> otherVectorizables = new HashSet<>();
        dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );


		Vectorizable fdq0Clone = dependencyTable.getFDQs( fdq0.getId() ).iterator().next();
		Vectorizable fdq1Clone = dependencyTable.getFDQs( fdq1.getId() ).iterator().next();

        // Each set should have one dependency graph one, and the total of course is then two
        assertThat( containingVectorizables.size(), is( 1 ) );
        assertThat( otherVectorizables.size(), is( 1 ) );
        assertThat( containingVectorizables.contains( fdq0Clone ), is( true ) );
        assertThat( otherVectorizables.contains( fdq1Clone ), is( true ) );

        // Mark base queries as ready
        dependencyTable.markExecutedDependency( q0 );
        dependencyTable.markExecutedDependency( q2 );

        // Now perform the reverse and get ready things split into different sets by q3
        containingVectorizables = new HashSet<>();
        otherVectorizables = new HashSet<>();
        dependencyTable.getAndClearAllReadyVectorizablesFiltered( q3.getId(), containingVectorizables, otherVectorizables );

        // Each set should have one dependency graph one, and the total of course is then two
        assertThat( containingVectorizables.size(), is( 1 ) );
        assertThat( otherVectorizables.size(), is( 1 ) );
        assertThat( containingVectorizables.contains( fdq1Clone ), is( true ) );
        assertThat( otherVectorizables.contains( fdq0Clone ), is( true ) );
    }

    @Test
    public void testFilterReadyVectorizablesAllContaining() {
        // Two dependency graphs, both containing q1
        Query q0 = constructTestQuery( "SELECT a FROM hats WHERE a > 7" );
        Query q1 = constructTestQuery( "SELECT b FROM bats WHERE b > 12" );
        Query q2 = constructTestQuery( "SELECT c FROM cats WHERE c = 8" );
        Query q3 = constructTestQuery( "SELECT d FROM mats WHERE d < 8" );

        // Create the dependency table
        VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

        // Create the dependency graphs to go in the table
        DependencyGraph graph0 = new DependencyGraph();
        graph0.addBaseQuery( q0.getId(), q0 );
        Multimap<Integer, String> mappings0 = HashMultimap.create();
        mappings0.put(0, "a");
        QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
        graph0.addDependencyForQuery( q1.getId(), qme0 );

        DependencyGraph graph1 = new DependencyGraph();
        graph1.addBaseQuery( q2.getId(), q2 );
        Multimap<Integer, String> mappings1 = HashMultimap.create();
        mappings1.put(0, "c");
        QueryMappingEntry qme1 = new QueryMappingEntry( q2.getId(), q1, mappings1 );
        graph1.addDependencyForQuery( q1.getId(), qme1 );
        Multimap<Integer, String> mappings2 = HashMultimap.create();
        mappings2.put(0, "b");
        QueryMappingEntry qme2 = new QueryMappingEntry( q1.getId(), q3, mappings2 );
        graph1.addDependencyForQuery( q3.getId(), qme2 );

        // Create the vectorizables from the dependency graphs
        VectorizableType vType0 = new VectorizableType();
        vType0.markAsFDQ();
        Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0);

        VectorizableType vType1 = new VectorizableType();
        vType1.markAsFDQ();
        Vectorizable fdq1 = new Vectorizable( graph1, q3.getId(), q3.getQueryString(), vType1);

        // Add the vectorizables to the dependency table
        dependencyTable.addFDQ(fdq0);
        dependencyTable.addFDQ(fdq1);

		Vectorizable fdq0Clone = dependencyTable.getFDQs( fdq0.getId() ).iterator().next();
		Vectorizable fdq1Clone = dependencyTable.getFDQs( fdq1.getId() ).iterator().next();

        // Mark base queries as ready
        dependencyTable.markExecutedDependency( q0 );
        dependencyTable.markExecutedDependency( q1 );
        dependencyTable.markExecutedDependency( q2 );

        // Now get ready things split into different sets by q1
        Set<Vectorizable> containingVectorizables = new HashSet<>();
        Set<Vectorizable> otherVectorizables = new HashSet<>();
        dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );

        // Since we are splitting on a query contained by both dependency graphs we should have
        // everything in the containing set
        assertThat( containingVectorizables.size(), is( 2 ) );
        assertThat( otherVectorizables.size(), is( 0 ) );
        assertThat( containingVectorizables.contains( fdq0Clone ), is( true ) );
        assertThat( containingVectorizables.contains( fdq1Clone ), is( true ) );
    }

    @Test
    public void testFilterReadyVectorizablesNoneContaining() {
        // Two dependency graphs, both containing q1
        Query q0 = constructTestQuery( "SELECT a FROM hats WHERE a > 7" );
        Query q1 = constructTestQuery( "SELECT b FROM bats WHERE b > 12" );
        Query q2 = constructTestQuery( "SELECT c FROM cats WHERE c = 8" );
        Query q3 = constructTestQuery( "SELECT d FROM mats WHERE d < 8" );
        Query q4 = constructTestQuery( "SELECT * FROM dogs");

        // Create the dependency table
        VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

        // Create the dependency graphs to go in the table
        DependencyGraph graph0 = new DependencyGraph();
        graph0.addBaseQuery( q0.getId(), q0 );
        Multimap<Integer, String> mappings0 = HashMultimap.create();
        mappings0.put(0, "a");
        QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
        graph0.addDependencyForQuery( q1.getId(), qme0 );

        DependencyGraph graph1 = new DependencyGraph();
        graph1.addBaseQuery( q2.getId(), q2 );
        Multimap<Integer, String> mappings1 = HashMultimap.create();
        mappings1.put(0, "c");
        QueryMappingEntry qme1 = new QueryMappingEntry( q2.getId(), q3, mappings1 );
        graph1.addDependencyForQuery( q1.getId(), qme1 );

        // Create the vectorizables from the dependency graphs
        VectorizableType vType0 = new VectorizableType();
        vType0.markAsFDQ();
        Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0);

        VectorizableType vType1 = new VectorizableType();
        vType1.markAsFDQ();
        Vectorizable fdq1 = new Vectorizable( graph1, q3.getId(), q3.getQueryString(), vType1);

        // Add the vectorizables to the dependency table
        dependencyTable.addFDQ(fdq0);
        dependencyTable.addFDQ(fdq1);

		Vectorizable fdq0Clone = dependencyTable.getFDQs( fdq0.getId() ).iterator().next();
		Vectorizable fdq1Clone = dependencyTable.getFDQs( fdq1.getId() ).iterator().next();

        // Mark base queries as ready
        dependencyTable.markExecutedDependency( q0 );
        dependencyTable.markExecutedDependency( q2 );

        // Now get ready things split into different sets by q4
        Set<Vectorizable> containingVectorizables = new HashSet<>();
        Set<Vectorizable> otherVectorizables = new HashSet<>();
        dependencyTable.getAndClearAllReadyVectorizablesFiltered( q4.getId(), containingVectorizables, otherVectorizables );

        // Since we are splitting on a query not in any of the dependency graphs everything should
        // be in the other set.
        assertThat( containingVectorizables.size(), is( 0 ) );
        assertThat( otherVectorizables.size(), is( 2 ) );
        assertThat( otherVectorizables.contains( fdq0Clone ), is( true ) );
        assertThat( otherVectorizables.contains( fdq1Clone ), is( true ) );
    }

	@Test
	public void testMultipleFDQsWithSameQIDRecorded() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT hs_s_symb as symb FROM holding_summary WHERE hs_ca_id = 17" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		// Create the dependency graphs to go in the table
		DependencyGraph graph0 = new DependencyGraph();
		graph0.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings0 = HashMultimap.create();
		mappings0.put( 0, "wi_s_symb" );
		QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
		graph0.addDependencyForQuery( q1.getId(), qme0 );

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q2.getId(), q2 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "hs_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q2.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		// Create the vectorizables from the dependency graphs
		VectorizableType vType0 = new VectorizableType();
		vType0.markAsFDQ();
		Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsFDQ();
		Vectorizable fdq1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		dependencyTable.addFDQ( fdq0 );
		dependencyTable.addFDQ( fdq1 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );

		// Mark q0 as executed, make sure we get one vectorizable
		dependencyTable.markExecutedDependency( q0 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( fdq0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		containingVectorizables = new HashSet<>();

		// Should be unset, make sure it isn't still ready to go
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );

		// Should be unset, make sure it isn't still ready to go
		dependencyTable.markExecutedDependency( q2 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( fdq1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );

	}

	@Test
	public void testTryRecordSameFDQ() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		// Create the dependency graphs to go in the table
		DependencyGraph graph0 = new DependencyGraph();
		graph0.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings0 = HashMultimap.create();
		mappings0.put( 0, "wi_s_symb" );
		QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
		graph0.addDependencyForQuery( q1.getId(), qme0 );

		// Create the vectorizables from the dependency graphs
		VectorizableType vType0 = new VectorizableType();
		vType0.markAsFDQ();
		Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0 );
		Vectorizable fdq1 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0 );
		assertThat( fdq0.getVectorizableId(), equalTo( fdq1.getVectorizableId() ) );

		dependencyTable.addFDQ( fdq0 );
		dependencyTable.addFDQ( fdq1 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();

		// Mark q0 as executed, make sure we get ONLY one vectorizable
		dependencyTable.markExecutedDependency( q0 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
	}

	@Test
	public void testRecordSameLoop() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		graph1.addBaseQuery( q1.getId(), q1 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsLoopBaseQuery();
		Vectorizable loop1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );
		Vectorizable loop2 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		// Register both loops, only one should exist
		dependencyTable.addFDQ( loop1 );
		dependencyTable.addFDQ( loop2 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.markExecutedDependency( q0 );

		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );

		assertThat( containingVectorizables.size(), equalTo( 0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );

		dependencyTable.markExecutedDependency( q1 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop1 ) );
	}


	@Test
	public void tryMergeParentWithMultipleFDQsForSameQid() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT hs_s_symb as symb FROM holding_summary WHERE hs_ca_id = 17" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		// Create the dependency graphs to go in the table
		DependencyGraph graph0 = new DependencyGraph();
		graph0.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings0 = HashMultimap.create();
		mappings0.put( 0, "wi_s_symb" );
		QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
		graph0.addDependencyForQuery( q1.getId(), qme0 );

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q2.getId(), q2 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "hs_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q2.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		// Create the vectorizables from the dependency graphs
		VectorizableType vType0 = new VectorizableType();
		vType0.markAsFDQ();
		Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsFDQ();
		Vectorizable fdq1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		dependencyTable.addFDQ( fdq0 );
		dependencyTable.addFDQ( fdq1 );

		// This FDQ is a superset of fdq0, should replace it.
		Query q3 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );
		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "wi_s_symb" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q0.getId(), q1, mappings2 );
		graph2.addDependencyForQuery( q1.getId(), qme2 );
		mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme22 = new QueryMappingEntry( q1.getId(), q3, mappings2 );
		graph2.addDependencyForQuery( q3.getId(), qme22 );
		VectorizableType vType2 = new VectorizableType();
		vType2.markAsFDQ();
		Vectorizable fdq2 = new Vectorizable( graph2, q3.getId(), q3.getQueryString(), vType2 );

		dependencyTable.addFDQ( fdq2 );

		dependencyTable.markExecutedDependency( q0 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( fdq2 ) );

		containingVectorizables = new HashSet<>();
		dependencyTable.markExecutedDependency( q2 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( fdq1 ) );
	}

	@Test
	public void tryMergeChildWithMultipleFDQsForSameQid() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT hs_s_symb as symb FROM holding_summary WHERE hs_ca_id = 17" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		// Create the dependency graphs to go in the table
		DependencyGraph graph0 = new DependencyGraph();
		graph0.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings0 = HashMultimap.create();
		mappings0.put( 0, "wi_s_symb" );
		QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
		graph0.addDependencyForQuery( q1.getId(), qme0 );

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q2.getId(), q2 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "hs_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q2.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		// Create the vectorizables from the dependency graphs
		VectorizableType vType0 = new VectorizableType();
		vType0.markAsFDQ();
		Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsFDQ();
		Vectorizable fdq1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		//Register only FDQ 1 for now, hold off on registering fdq0
		dependencyTable.addFDQ( fdq1 );

		// This FDQ is a superset of fdq0
		Query q3 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );
		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "wi_s_symb" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q0.getId(), q1, mappings2 );
		graph2.addDependencyForQuery( q1.getId(), qme2 );
		mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme22 = new QueryMappingEntry( q1.getId(), q3, mappings2 );
		graph2.addDependencyForQuery( q3.getId(), qme22 );
		VectorizableType vType2 = new VectorizableType();
		vType2.markAsFDQ();
		Vectorizable fdq2 = new Vectorizable( graph2, q3.getId(), q3.getQueryString(), vType2 );

		dependencyTable.addFDQ( fdq2 );

		// Add the child FDQ
		dependencyTable.addFDQ( fdq0 );

		dependencyTable.markExecutedDependency( q0 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( fdq2 ) );

		containingVectorizables = new HashSet<>();
		dependencyTable.markExecutedDependency( q2 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( fdq1 ) );
	}

	@Test
	public void testLoopAndFDQAreTheSame() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		// Create the dependency graphs to go in the table
		DependencyGraph graph0 = new DependencyGraph();
		graph0.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings0 = HashMultimap.create();
		mappings0.put( 0, "wi_s_symb" );
		QueryMappingEntry qme0 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
		graph0.addDependencyForQuery( q1.getId(), qme0 );

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		graph1.addBaseQuery( q1.getId(), q1 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings0 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		// Create the vectorizables from the dependency graphs
		VectorizableType vType0 = new VectorizableType();
		vType0.markAsFDQ();
		Vectorizable fdq0 = new Vectorizable( graph0, q1.getId(), q1.getQueryString(), vType0 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsLoopBaseQuery();
		Vectorizable loop1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		//Register only FDQ 1 for now, hold off on registering fdq0
		dependencyTable.addFDQ( fdq0 );
		dependencyTable.addFDQ( loop1 );

		dependencyTable.markExecutedDependency( q0 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( fdq0 ) );

		dependencyTable.markExecutedDependency( q1 );
		containingVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop1 ) );

	}

	@Test
	public void testRecordLoopAndLoopExtension() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		graph1.addBaseQuery( q1.getId(), q1 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsLoopBaseQuery();
		Vectorizable loop1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		graph2.addBaseQuery( q1.getId(), q1 );
		graph2.addBaseQuery( q2.getId(), q1 );
		qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph2.addDependencyForQuery( q1.getId(), qme1 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q1.getId(), q2, mappings2 );
		graph2.addDependencyForQuery( q2.getId(), qme2 );

		Vectorizable loop2 = new Vectorizable( graph2, q2.getId(), q2.getQueryString(), vType1 );

		// Register both loops, both should exist
		dependencyTable.addFDQ( loop1 );
		dependencyTable.addFDQ( loop2 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.markExecutedDependency( q0 );

		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );

		assertThat( containingVectorizables.size(), equalTo( 0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );

		dependencyTable.markExecutedDependency( q1 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop1 ) );

		containingVectorizables = new HashSet<>();
		dependencyTable.markExecutedDependency( q2 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop2 ) );
	}

	@Test
	public void testRecordLoopSuperset() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		graph1.addBaseQuery( q1.getId(), q1 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsLoopBaseQuery();
		Vectorizable loop1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		graph2.addBaseQuery( q1.getId(), q1 );
		qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph2.addDependencyForQuery( q1.getId(), qme1 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q1.getId(), q2, mappings2 );
		graph2.addDependencyForQuery( q2.getId(), qme2 );

		Vectorizable loop2 = new Vectorizable( graph2, q2.getId(), q2.getQueryString(), vType1 );

		dependencyTable.addFDQ( loop1 );

		//Loop 2 is a superset of loop1
		dependencyTable.addFDQ( loop2 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.markExecutedDependency( q0 );

		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );

		assertThat( containingVectorizables.size(), equalTo( 0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );

		dependencyTable.markExecutedDependency( q1 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop2 ) );

		containingVectorizables = new HashSet<>();
		dependencyTable.markExecutedDependency( q2 );
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 0 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
	}

	@Test
	public void testLoadInVectorizablesFromOtherClients() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsFDQ();
		Vectorizable fdq0 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		graph2.addBaseQuery( q1.getId(), q1 );
		qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph2.addDependencyForQuery( q1.getId(), qme1 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q1.getId(), q2, mappings2 );
		graph2.addDependencyForQuery( q2.getId(), qme2 );

		VectorizableType vType2 = new VectorizableType();
		vType2.markAsLoopBaseQuery();
		Vectorizable loop2 = new Vectorizable( graph2, q2.getId(), q2.getQueryString(), vType1 );

		VectorizableDependencyTable vecDepTab0 = new VectorizableDependencyTable();

		Map<Vectorizable.VectorizableId, Vectorizable> vecMap = new HashMap<>();
		vecMap.put( fdq0.getVectorizableId(), fdq0 );
		vecMap.put( loop2.getVectorizableId(), loop2 );

		vecDepTab0.loadInNewVectorizablesFrom( vecMap );
		Map<Vectorizable.VectorizableId, Vectorizable> allVectorizables = vecDepTab0.getVectorizableMap();
		assertThat( allVectorizables.size(), equalTo( 2 ) );
		assertThat( allVectorizables.containsKey( fdq0.getVectorizableId() ), equalTo( true ) );
		assertThat( allVectorizables.containsKey( loop2.getVectorizableId() ), equalTo( true ) );
	}

	@Test
	public void testLoadInAndMergeVectorizablesFromOtherClients() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		//graph1.addBaseQuery( q1.getId(), q1 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		assertThat( graph1.isVectorizable(), equalTo( true ) );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsLoopBaseQuery();
		Vectorizable loop1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph2.addDependencyForQuery( q1.getId(), qme1 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q1.getId(), q2, mappings2 );
		graph2.addDependencyForQuery( q2.getId(), qme2 );

		assertThat( graph2.isVectorizable(), equalTo( true ) );

		Vectorizable loop2 = new Vectorizable( graph2, q2.getId(), q2.getQueryString(), vType1 );

		dependencyTable.addVectorizable( loop1 );

		// Load everything into dependencyTable
		VectorizableDependencyTable vectorizableDependencyGraph2 = new VectorizableDependencyTable();
		vectorizableDependencyGraph2.addVectorizable( loop2 );
		dependencyTable.loadInNewVectorizablesFrom( vectorizableDependencyGraph2.getVectorizableMap() );

		// loop2 should become parent of loop1
		dependencyTable.markExecutedDependency( q0 );
		dependencyTable.markExecutedDependency( q1 );
		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop2 ) );

		// load everything into vectorizableDependencyGraph2
		vectorizableDependencyGraph2.loadInNewVectorizablesFrom( dependencyTable.getVectorizableMap() );

		// loop 2 should become parent of loop1
		vectorizableDependencyGraph2.markExecutedDependency( q0 );
		vectorizableDependencyGraph2.markExecutedDependency( q1 );

		containingVectorizables = new HashSet<>();
		otherVectorizables = new HashSet<>();

		vectorizableDependencyGraph2.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop2 ) );

	}

	@Test
	public void testLoadParentInWhenDependenciesInFlight() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		graph1.addBaseQuery( q1.getId(), q1 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsLoopBaseQuery();
		Vectorizable loop1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		graph2.addBaseQuery( q1.getId(), q1 );
		qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph2.addDependencyForQuery( q1.getId(), qme1 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q1.getId(), q2, mappings2 );
		graph2.addDependencyForQuery( q2.getId(), qme2 );

		Vectorizable loop2 = new Vectorizable( graph2, q2.getId(), q2.getQueryString(), vType1 );

		dependencyTable.addVectorizable( loop1 );

		// Load everything into dependencyTable, dependencies for loop1 partially satisfied
		VectorizableDependencyTable vectorizableDependencyGraph2 = new VectorizableDependencyTable();
		vectorizableDependencyGraph2.addVectorizable( loop2 );

		dependencyTable.markExecutedDependency( q0 );
		dependencyTable.loadInNewVectorizablesFrom( vectorizableDependencyGraph2.getVectorizableMap() );

		// loop2 should become parent of loop1
		dependencyTable.markExecutedDependency( q1 );
		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop2 ) );

		// load everything into vectorizableDependencyGraph2, dependencies for loop2 should be partially satisfied
		vectorizableDependencyGraph2.markExecutedDependency( q0 );
		vectorizableDependencyGraph2.loadInNewVectorizablesFrom( dependencyTable.getVectorizableMap() );

		// loop 2 should become parent of loop1
		vectorizableDependencyGraph2.markExecutedDependency( q1 );
		containingVectorizables = new HashSet<>();
		otherVectorizables = new HashSet<>();
		vectorizableDependencyGraph2.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 1 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
		assertThat( containingVectorizables.iterator().next(), equalTo( loop2 ) );

	}

	@Test
	public void testSetDependenciesOnMultipleNewQueries() {
		Query q0 = constructTestQuery( "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 17" );
		Query q1 = constructTestQuery( "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'CVS'" );
		Query q2 = constructTestQuery( "SELECT product FROM products WHERE lt_price = 2.00" );

		Query q3 = constructTestQuery( "SELECT product FROM products, owners WHERE lt_price = 2.00" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		DependencyGraph graph1 = new DependencyGraph();
		graph1.addBaseQuery( q0.getId(), q0 );
		graph1.addBaseQuery( q1.getId(), q1 );
		Multimap<Integer, String> mappings1 = HashMultimap.create();
		mappings1.put( 0, "wi_s_symb" );
		QueryMappingEntry qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph1.addDependencyForQuery( q1.getId(), qme1 );

		VectorizableType vType1 = new VectorizableType();
		vType1.markAsLoopBaseQuery();
		Vectorizable loop1 = new Vectorizable( graph1, q1.getId(), q1.getQueryString(), vType1 );

		DependencyGraph graph2 = new DependencyGraph();
		graph2.addBaseQuery( q0.getId(), q0 );
		graph2.addBaseQuery( q1.getId(), q1 );
		qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph2.addDependencyForQuery( q1.getId(), qme1 );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "lt_price" );
		QueryMappingEntry qme2 = new QueryMappingEntry( q1.getId(), q2, mappings2 );
		graph2.addDependencyForQuery( q2.getId(), qme2 );

		Vectorizable loop2 = new Vectorizable( graph2, q2.getId(), q2.getQueryString(), vType1 );

		DependencyGraph graph3 = new DependencyGraph();
		graph3.addBaseQuery( q0.getId(), q0 );
		graph3.addBaseQuery( q1.getId(), q1 );
		qme1 = new QueryMappingEntry( q0.getId(), q1, mappings1 );
		graph3.addDependencyForQuery( q1.getId(), qme1 );
		Multimap<Integer, String> mappings3 = HashMultimap.create();
		mappings3.put( 0, "lt_price" );
		QueryMappingEntry qme3 = new QueryMappingEntry( q1.getId(), q3, mappings3 );
		graph3.addDependencyForQuery( q3.getId(), qme3 );

		Vectorizable loop3 = new Vectorizable( graph3, q3.getId(), q3.getQueryString(), vType1 );

		dependencyTable.addVectorizable( loop1 );

		// Load everything into dependencyTable, dependencies for loop1 partially satisfied
		VectorizableDependencyTable vectorizableDependencyGraph2 = new VectorizableDependencyTable();
		vectorizableDependencyGraph2.addVectorizable( loop2 );
		vectorizableDependencyGraph2.addVectorizable( loop3 );

		dependencyTable.markExecutedDependency( q0 );
		dependencyTable.loadInNewVectorizablesFrom( vectorizableDependencyGraph2.getVectorizableMap() );
		dependencyTable.markExecutedDependency( q1 );

		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		dependencyTable.getAndClearAllReadyVectorizablesFiltered( q1.getId(), containingVectorizables, otherVectorizables );
		assertThat( containingVectorizables.size(), equalTo( 2 ) );
		assertThat( otherVectorizables.size(), equalTo( 0 ) );
	}
}
