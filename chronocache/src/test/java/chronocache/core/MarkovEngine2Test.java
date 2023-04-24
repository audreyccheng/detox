package chronocache.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.QueryMappingEntry;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryOutputs;
import chronocache.core.qry.QueryResult;
import chronocache.core.qry.QueryStream;
import chronocache.core.parser.AntlrParser;
import chronocache.core.Parameters;
import chronocache.db.DBFactory;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;

/**
 * Unit tests for the new Markov Engine
 */
public class MarkovEngine2Test {

	private MarkovGraph myMarkovGraph;

	private Logger logger = LoggerFactory.getLogger( this.getClass() );
	private VersionVector version = new VersionVector( new ArrayList<Long>() );

	private ExecutedQuery createExecutedQuery( long execTime ) throws Exception {
		String s = "SELECT * FROM t";
		AntlrParser p = new AntlrParser();
		DateTime dt = DateTime.now();
		AntlrParser.ParseResult parseResult = p.buildParseTree( s );
		ExecutedQuery qry = new ExecutedQuery( new Query(s, parseResult ),
				dt, execTime, new QueryResult( new ArrayList<Map<String, Object>>(), version ) );
		return qry;
	}

	private ExecutedQuery createExecutedQueryWithOneParam( long execTime ) throws Exception {
		String s = "SELECT * FROM t WHERE col1 = 3";
		AntlrParser p = new AntlrParser();
		DateTime dt = DateTime.now();
		AntlrParser.ParseResult parseResult = p.buildParseTree( s );
		ExecutedQuery qry = new ExecutedQuery( new Query(s, parseResult ),
				dt, execTime, new QueryResult( new ArrayList<Map<String, Object>>(), version ) );
		return qry;
	}

	private ExecutedQuery createExecutedQueryWithThreeParams( long execTime ) throws Exception {
		String s = "SELECT * FROM t WHERE col1 = 3 AND col2 = 2 AND col3 = 'test'";
		AntlrParser p = new AntlrParser();
		DateTime dt = DateTime.now();
		AntlrParser.ParseResult parseResult = p.buildParseTree( s );
		ExecutedQuery qry = new ExecutedQuery( new Query(s, parseResult),
				dt, execTime, new QueryResult( new ArrayList<Map<String, Object>>(), version ) );
		return qry;
	}

	/**
	 * A hacky class to bypass the existing async MarkovConstructor
	 * stuff in MarkovEngine2 to make stuff easier to unit test
	 */
	private class MarkovConstructorUnderTest extends MarkovConstructor {

		private MarkovGraph g;

		public MarkovConstructorUnderTest( MarkovGraph g ){
			super( 0, new QueryStream(), new Duration(100) );
			this.g = g;
		}

		@Override
		public MarkovGraph getGraph() {
			return g;
		}
	};

	/**
	 * Essentially a wrapper around the protected members of the MarkovEngine2
	 * to unit test all the important methods
	 */
	private class EngineUnderTest extends MarkovEngine2 {

		private Map<String, QueryResult> memcachedMap;

		public EngineUnderTest() {
			super( DBFactory.DBType.TEST_DB );
			memcachedMap = new HashMap<>();
		}

		public ExecutorService retrieveThreadPool() {
			return asyncExecutor;
		}

		public MarkovGraph retrieveGraph( long clientId ) {
			return getClientMarkovPredictionGraph( clientId );
		}

		public boolean isQueryShellRecorded( QueryIdentifier qid ) {
			return knownQueryShells.containsKey( qid );
		}

		public Map<QueryIdentifier, Query> getKnownQueryShells() {
			return knownQueryShells;
		}

		public void overrideMarkovGraph( long clientId, MarkovGraph g ) {
			MarkovMultiModel models = clientModels.get( clientId );
			models.swapConstructor( new MarkovConstructorUnderTest( g ) );
		}

		public List<QueryIdentifier> getRelatedOutQueries( long clientId, QueryIdentifier qid ) {
			return lookupRelatedOutQueries( clientId, qid );
		}

		public long getQueryTimesSeen( QueryIdentifier qid ) {
			return queryTimesSeen.get( qid );
		}

		public double getAverageQueryResponseTime( QueryIdentifier qid ) {
			return avgQueryResponseTimes.get( qid );
		}

		public Map<QueryIdentifier, QueryMappingEntry> getPriorMappings( long clientId, QueryIdentifier qid ) {
			return getAllPriorQueryMappings( clientId, qid );
		}

		public boolean canExecuteFDQ( long clientId, Vectorizable fdq ) {
			try {
				executeVectorizable( clientId, fdq, false );
			} catch( Exception e ) {
				logger.error( "Could not execute FDQ, exception {}", e );
				return false;
			}
			return true;
		}

		public String getIndexData( long clientId, Collection<QueryOutputs> mappings ) {
			return getDataForIndex( clientId, mappings );
		}

		public List<String> getAllConstants( long clientId, Multimap<Integer, QueryOutputs> mappings ) {
			return tryGetRequiredData( clientId, mappings );
		}

		public Collection<Vectorizable> findImportantQueries( long clientId ) {
			return getImportantQueries( clientId );
		}

		public void addADQ( long clientId, Vectorizable fdq ) {
			alwaysDefinedQueries.put( fdq.getId(), fdq );
		}

		public void addFDQ( long clientId, Vectorizable fdq ) {
			VectorizableDependencyTable fdqTable = clientVectorizableDependencyTables.get( clientId );
			fdqTable.addFDQ( fdq );
		}

		public VectorizableDependencyTable getVectorizableDependencyTable( long clientId ) {
			return clientVectorizableDependencyTables.get( clientId );
		}

		public void addFDQToDependencyTab( long clientId, Vectorizable fdq ) {
			clientVectorizableDependencyTables.get( clientId ).addFDQ( fdq );
		}

		public void addLoop( long clientId, Vectorizable loop ) {
			clientVectorizableDependencyTables.get( clientId ).addLoop( loop );
		}

		public List<List<String>> getConstantPermutations( List<Set<String>> constants ) {
			return getAllConstantPermutations( constants );
		}

		public void findNewFDQPub( long clientId, QueryIdentifier qid, String queryString ) {
			findNewFDQs( clientId, qid, queryString );
		}

		public void findNewLoopPub( long clientId, QueryIdentifier qid) {
			findNewLoops( clientId, qid );
		}

		@Override
		protected QueryResult checkMemcached( CacheHandler cacheHandler, String cacheKey, VersionVector versionVector ) {
			return memcachedMap.get( cacheKey );
		}

		public void addToMemcached( String cacheKey, QueryResult result ) {
			memcachedMap.put( cacheKey, result );
		}

	};

	@Rule
    public ExpectedException exceptionTester = ExpectedException.none();

	@Test
	public void testNoRetrieveMarkovGraph() throws Exception {
		EngineUnderTest eut = new EngineUnderTest();
		exceptionTester.expect(NullPointerException.class);
		eut.retrieveGraph( 0 );
	}

	@Test
	public void testRetrieveMarkovGraph() throws Exception {
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		assertThat( eut.retrieveGraph( 0 ), not( nullValue() ) );
		exceptionTester.expect(NullPointerException.class);
		eut.retrieveGraph( 1 );
		eut.stopEngine();
	}

	@Test
	public void testFindKnownQueryShells() throws Exception {
		EngineUnderTest eut = new EngineUnderTest();
		ExecutedQuery qry = createExecutedQuery( 1000 );
		eut.recordQueryShell( qry );
		assertThat( eut.isQueryShellRecorded( qry.getId() ), is( true ) );
		assertThat( eut.isQueryShellRecorded( new QueryIdentifier( 0 ) ), is( false ) );
	}

	@Test
	public void testLookupRelatedQueries() throws Exception {
		EngineUnderTest eut = new EngineUnderTest();
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier qid1 = new QueryIdentifier( 1 );
		QueryIdentifier qid2 = new QueryIdentifier( 2 );
		MarkovNode node1 = g.getOrAddNode( qid1 );
		MarkovNode node2 = g.getOrAddNode( qid2 );
		node1.addEdgeTraversal( node2 );
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		//Graph was set up to have qid1 related to qid2
		List<QueryIdentifier> relQueries = eut.lookupRelatedOutQueries( 0, qid1 );
		assertThat( relQueries.size(), is( 1 ) );
		assertThat( relQueries.get( 0 ), is( qid2 ) );

		//qid2 is unrelated to qid1
		relQueries = eut.lookupRelatedOutQueries( 0, qid2 );
		assertThat(relQueries.size(), is( 0 ) );

		//Wrong clientId
		exceptionTester.expect(NullPointerException.class);
		relQueries = eut.lookupRelatedOutQueries( 1, qid1 );
	}

	@Test
	public void testAverageQueryResponseTime() throws Exception {
		EngineUnderTest eut = new EngineUnderTest();
		ExecutedQuery firstQueryRun = createExecutedQuery( 1000 );
		ExecutedQuery secondQueryRun = createExecutedQuery( 2000 );
		eut.addAverageTrackingInfo( firstQueryRun );
		assertThat( eut.getQueryTimesSeen( firstQueryRun.getId() ), is( 1L ) );
		assertThat( eut.getAverageQueryResponseTime( firstQueryRun.getId() ), is( 1000.0 ) );
		eut.addAverageTrackingInfo( secondQueryRun );
		//Same query id, so interchangeable
		assertThat( eut.getQueryTimesSeen( firstQueryRun.getId() ), is( 2L ) );
		assertThat( eut.getAverageQueryResponseTime( firstQueryRun.getId() ), is( 1500.0 ) );
	}

	@Test
	public void testGetPriorQueryMappingsSingleQuery() throws Exception {
		// Set up the Markov Graph
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		n1.addQueryString("SELECT 1");
		MarkovNode n2 = g.getOrAddNode( q2 );
		n2.addQueryString("SELECT 2");
		n1.addEdgeTraversal( n2 );
		Multimap<Integer, String> mappings = LinkedListMultimap.create();
		mappings.put( 1, "col1" );
		n1.addMappings( q2, mappings, 0);

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );

		Map<QueryIdentifier, QueryMappingEntry> priorMappings = eut.getPriorMappings( 0, q2 );
		assertThat( priorMappings, not( nullValue() ) );
		assertThat( priorMappings.keySet().size(), is( 1 ) );
		assertThat( priorMappings.keySet().iterator().next(), is( q1 ) );
	}

	@Test
	public void testGetPriorQueryMappingsNoPriorQueries() throws Exception {
		// Set up the Markov Graph
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );

		// should return empty list of mappings
		Map<QueryIdentifier, QueryMappingEntry> priorMappings = eut.getPriorMappings( 0, q2 );
		assertThat( priorMappings, not( nullValue() ) );
		assertThat( priorMappings.keySet().size(), is( 0 ) );
	}

	@Test
	public void testGetPriorQueryMappingsSinglePriorEmptyMappings() throws Exception {
		// Set up the Markov Graph
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );
		Multimap<Integer, String> mappings = LinkedListMultimap.create();
		n1.addMappings( q2, mappings, 0 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );

		Map<QueryIdentifier, QueryMappingEntry> priorMappings = eut.getPriorMappings( 0, q2 );
		assertThat( priorMappings, not( nullValue() ) );
		assertThat( priorMappings.keySet().size(), is( 0 ) );
		// Empty mappings
		assertThat( priorMappings.get( q1 ), nullValue() );
	}

	@Test
	public void testGetPriorQueryMappingsMultiplePriors() throws Exception {
		// Set up the Markov Graph
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );
		QueryIdentifier q4 = new QueryIdentifier( 4 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		MarkovNode n3 = g.getOrAddNode( q3 );
		MarkovNode n4 = g.getOrAddNode( q4 );
		n1.addQueryString("SELECT 1");
		n2.addQueryString("SELECT 2");
		n3.addQueryString("SELECT 3");
		n4.addQueryString("SELECT 4");
		n1.addEdgeTraversal( n4 );
		n2.addEdgeTraversal( n4 );
		n3.addEdgeTraversal( n4 );
		Multimap<Integer, String> mappings = LinkedListMultimap.create();
		n1.addMappings( q4, mappings, 0 );
		Multimap<Integer, String> mappings2 = LinkedListMultimap.create();
		mappings2.put( 1, "col1" );
		n2.addMappings( q4, mappings2, 0 );
		Multimap<Integer, String> mappings3 = LinkedListMultimap.create();
		mappings3.put( 1, "col1" );
		mappings3.put( 1, "col2" );
		mappings3.put( 2, "col3" );
		n3.addMappings( q4, mappings3, 0 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );

		Map<QueryIdentifier, QueryMappingEntry> priorMappings = eut.getPriorMappings( 0, q4 );
		assertThat( priorMappings, not( nullValue() ) );
		assertThat( priorMappings.keySet().size(), is( 2 ) );
		// Q1 has no mappings
		assertThat( priorMappings.get( q1 ), nullValue() );

		//Q2 only has the one mapping
		assertThat( priorMappings.get( q2 ).getQueryMappings().size(), is( 1 ) );

		//Q3 has 3 mappings, but two unique param mappings
		assertThat( priorMappings.get( q3 ).getQueryMappings().size(), is( 3 ) );
		assertThat( priorMappings.get( q3 ).getQueryMappings().keySet().size(), is( 2 ) );
	}

    @Ignore( "We don't optimize all base queries." )
	@Test
	public void testCanPredictivelyFindExecuteFDQNoMappings() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup the test queries
		Query prevQuery = new Query( "SELECT citations FROM employees" );
		Query query = new Query( "SELECT salary FROM employees" );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode prevNode = g.getOrAddNode( prevQuery.getId() );
		prevNode.addQueryString( prevNode.getQueryString() );
		MarkovNode node = g.getOrAddNode( query.getId() );
		node.addQueryString( node.getQueryString() );

		// Add traversals to the markov  graph
		prevNode.addEdgeTraversal( node );

		// Setup inputs and outputs for our nodes
		g.addInputParameters( prevQuery.getId(), new LinkedList<>() );
		g.addResultSet( prevQuery.getId(), new QueryResult( new LinkedList<>(), version ) );
		g.addInputParameters( query.getId(), new LinkedList<>() );
		g.addResultSet( query.getId(), new QueryResult( new LinkedList<>(), version ) );
		g.findAllParameterMappingsForNode( query.getId() );

		// Run the engine
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( prevQuery );
		eut.recordQueryShell( query );
		eut.findNewFDQPub( 0, query.getId(), query.getQueryString() );


		List<Vectorizable> readyFdqs = eut.getVectorizableDependencyTable( 0 ).getAndClearAllReadyVectorizables();

		assertThat( readyFdqs.size(), is( 1 ) );
		assertThat( readyFdqs.get( 0 ).isAlwaysDefined(), is( true ) );
		assertThat( readyFdqs.get( 0 ).getQueryId(), is( query.getId() ) );
		assertThat( readyFdqs.get( 0 ).isSimpleVectorizable(), is( true ) );
	}

	@Test
	public void testCanPredictivelyFindExecuteFDQOneMapping() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup test queries
		Query prevQuery = new Query( "SELECT numbers FROM numberTable" );
		Query query = new Query( "SELECT salary FROM employees WHERE salary = 3" );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode prevNode = g.getOrAddNode( prevQuery.getId() );
		prevNode.addQueryString( prevQuery.getQueryString() );
		MarkovNode node = g.getOrAddNode( query.getId() );
		node.addQueryString( query.getQueryString() );

		// Add traversals to the markov graph
		prevNode.addEdgeTraversal( node );

		// Pretend to execute prevQuery then query
		List<Map<String,Object>> results = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		row.put( "numbers", new String("3") );
		results.add( row );

		g.addInputParameters( prevQuery.getId(), new LinkedList<>() );
		g.addResultSet( prevQuery.getId(), new QueryResult( results, version ) );

		g.addInputParameters( query.getId(), query.getParams() );
		g.addResultSet( query.getId(), new QueryResult( new LinkedList<>(), version ) );
		g.findAllParameterMappingsForNode( query.getId() );

		// Run the engine
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( prevQuery );
		eut.recordQueryShell( query );
		eut.findNewFDQPub( 0, query.getId(), query.getQueryString() );

		// Pretend prevQuery was executed by marking it as such
		eut.getVectorizableDependencyTable( 0 ).markExecutedDependency( prevQuery );

		// Assert that query is now a ready FDQ, an ADQ, and runnable
		List<Vectorizable> readyFdqs = eut.getVectorizableDependencyTable( 0 ).getAndClearAllReadyVectorizables();
		assertThat( readyFdqs.size(), is( 1 ) );
		assertThat( readyFdqs.get( 0 ).isAlwaysDefined(), is( true ) );
		assertThat( readyFdqs.get( 0 ).getQueryId(), is( query.getId() ) );
		assertThat( readyFdqs.get( 0 ).isSimpleVectorizable(), is( true ) );
	}

	@Test
	public void testCanPredictivelyFindExecuteFDQOneMappingComplex() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup test queries
		Query prevQuery = new Query( "SELECT numbers FROM numberTable" );
		Query query = new Query( "SELECT MAX(salary) as m, numbers FROM employees WHERE numbers = 3 GROUP BY numbers ORDER BY m ASC" );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode prevNode = g.getOrAddNode( prevQuery.getId() );
		prevNode.addQueryString( prevQuery.getQueryString() );
		MarkovNode node = g.getOrAddNode( query.getId() );
		node.addQueryString( query.getQueryString() );

		// Add traversals to the markov graph
		prevNode.addEdgeTraversal( node );

		// Pretend to execute prevQuery then query
		List<Map<String,Object>> results = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		row.put( "numbers", new String("3") );
		results.add( row );

		g.addInputParameters( prevQuery.getId(), new LinkedList<>() );
		g.addResultSet( prevQuery.getId(), new QueryResult( results, version ) );

		g.addInputParameters( query.getId(), query.getParams() );
		g.addResultSet( query.getId(), new QueryResult( new LinkedList<>(), version ) );
		g.findAllParameterMappingsForNode( query.getId() );

		// Run the engine
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( prevQuery );
		eut.recordQueryShell( query );
		eut.findNewFDQPub( 0, query.getId(), query.getQueryString() );

		// Pretend prevQuery was executed by marking it as such
		eut.getVectorizableDependencyTable( 0 ).markExecutedDependency( prevQuery );

		// Assert that query is now a ready FDQ, an ADQ, and runnable
		List<Vectorizable> readyFdqs = eut.getVectorizableDependencyTable( 0 ).getAndClearAllReadyVectorizables();
		assertThat( readyFdqs.size(), is( 1 ) );
		assertThat( readyFdqs.get( 0 ).isAlwaysDefined(), is( true ) );
		assertThat( readyFdqs.get( 0 ).getQueryId(), is( query.getId() ) );
		assertThat( readyFdqs.get( 0 ).isSimpleVectorizable(), is( false ) );
		assertThat( readyFdqs.get( 0 ).isVectorizable(), is( true ) );
	}

    @Ignore("We disabled multiple top-level base query optimization")
	@Test
	public void testCanPredictivelyFindExecuteFDQMultipleMappings() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup queries
		Query query1 = new Query( "SELECT bats FROM t" );
		Query query2 = new Query( "SELECT hats FROM t" );
		Query query3 = new Query( "SELECT mats FROM t" );
		Query query4 = new Query( "SELECT cats FROM t WHERE col1 = 1 AND col2 = 2 AND col3 = 'test'" );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node1 = g.getOrAddNode( query1.getId() );
		node1.addQueryString( query1.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( query2.getId() );
		node2.addQueryString( query2.getQueryString() );
		MarkovNode node3 = g.getOrAddNode( query3.getId() );
		node3.addQueryString( query3.getQueryString() );
		MarkovNode node4 = g.getOrAddNode( query4.getId() );
		node4.addQueryString( query4.getQueryString() );

		node1.addEdgeTraversal( node4 );
		node2.addEdgeTraversal( node4 );
		node3.addEdgeTraversal( node4 );

		// Setup results
		List<Map<String,Object>> query1Result = new LinkedList<>();
		Map<String,Object> query1Row = new HashMap<>();
		query1Row.put( "bats", new String( "1" ) );
		query1Result.add( query1Row );

		List<Map<String,Object>> query2Result = new LinkedList<>();
		Map<String,Object> query2Row = new HashMap<>();
		query2Row.put( "hats", new String( "2" ) );
		query2Result.add( query2Row );

		List<Map<String,Object>> query3Result = new LinkedList<>();
		Map<String,Object> query3Row = new HashMap<>();
		query3Row.put( "mats", new String( "test" ) );
		query3Result.add( query3Row );

		//Engine setup
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query1 );
		eut.recordQueryShell( query2 );
		eut.recordQueryShell( query3 );
		eut.recordQueryShell( query4 );

		List<Vectorizable> readyFdqs = new LinkedList<>();

		// Add query 1's inputs and outputs
		g.addInputParameters( query1.getId(), query1.getParams() );
		g.addResultSet( query1.getId(), new QueryResult( query1Result, version ) );

		// Can't predict yet because query 4 does not have enough mappings to be an FDQ
		eut.findNewFDQPub( 0, query4.getId(), query4.getQueryString() );
		assertThat( eut.getVectorizableDependencyTable( 0 ).size(), is( 0 ) );

		// Add query 2's inputs and outputs
		g.addInputParameters( query2.getId(), query2.getParams() );
		g.addResultSet( query2.getId(), new QueryResult( query2Result, version ) );

		// Can't predict yet because query4 still does not have enough mappings to be an FDQ
		eut.findNewFDQPub( 0, query4.getId(), query4.getQueryString() );
		assertThat( eut.getVectorizableDependencyTable( 0 ).size(), is( 0 ) );

		// Add query 3's inputs and outputs
		g.addInputParameters( query3.getId(), query3.getParams() );
		g.addResultSet( query3.getId(), new QueryResult( query3Result, version ) );

		// Can't predict yet because query 4 still does not have enough mappings to be an FDQ
		eut.findNewFDQPub( 0, query4.getId(), query4.getQueryString() );
		assertThat( eut.getVectorizableDependencyTable( 0 ).size(), is( 0 ) );

		// Add query 4's inputs and outputs
		g.addInputParameters( query4.getId(), query4.getParams() );
		g.addResultSet( query4.getId(), new QueryResult( new LinkedList<>(), version ) );
		g.findAllParameterMappingsForNode( query4.getId() );

		// Mark queries 1-3 as excuted
		eut.getVectorizableDependencyTable( 0 ).markExecutedDependency( query1 );
		eut.getVectorizableDependencyTable( 0 ).markExecutedDependency( query2 );
		eut.getVectorizableDependencyTable( 0 ).markExecutedDependency( query3 );

		// Find all FDQ's, should find query 4
		eut.findNewFDQPub( 0, query4.getId(), query4.getQueryString() );

		// All mappings should be working, can predict
		VectorizableDependencyTable dependencyTable = eut.getVectorizableDependencyTable( 0 );
		assertThat( dependencyTable.size(), is( 1 ) );
		assertThat( dependencyTable.getAndClearAllReadyVectorizables().size(), is( 1 ) );
		assertThat( dependencyTable.getAndClearAllReadyVectorizables().get( 0 ).getQueryId(), is( query4.getId() ) );
		assertThat( dependencyTable.getAndClearAllReadyVectorizables().get( 0 ).isAlwaysDefined(), is( true ) );
		assertThat( dependencyTable.getAndClearAllReadyVectorizables().get( 0 ).isSimpleVectorizable(), is( false ) );
		assertThat( dependencyTable.getAndClearAllReadyVectorizables().get( 0 ).isVectorizable(), is( true ) );
	}

	@Test
	public void testPredictiveFDQVectorizedCorrectly() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup queries
		Query query1 = new Query( "SELECT bats FROM t" );
		logger.info( "q1 is {}", query1.getId() );

		Query query2 = new Query( "SELECT hats FROM t WHERE bats = 1" );
		logger.info( "q2 is {}", query2.getId() );
		logger.info( "q2 params: {}", query2.getParams() );
		Query query3 = new Query( "SELECT cats FROM t WHERE bats = 1 AND hats = 'blue'" );
		logger.info( "q3 is {}", query3.getId() );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node1 = g.getOrAddNode( query1.getId() );
		node1.addQueryString( query1.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( query2.getId() );
		node2.addQueryString( query2.getQueryString() );
		MarkovNode node3 = g.getOrAddNode( query3.getId() );
		node3.addQueryString( query3.getQueryString() );

		node1.addEdgeTraversal( node2 );
		node1.addEdgeTraversal( node3 );
		node2.addEdgeTraversal( node3 );

		// Setup results
		List<Map<String,Object>> query1Result = new LinkedList<>();
		Map<String,Object> query1Row = new HashMap<>();
		query1Row.put( "bats", new Long( 1L ) );
		query1Result.add( query1Row );

		List<Map<String,Object>> query2Result = new LinkedList<>();
		Map<String,Object> query2Row = new HashMap<>();
		query2Row.put( "hats", new String( "blue" ) );
		query2Result.add( query2Row );

		//Engine setup
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query1 );
		eut.recordQueryShell( query2 );
		eut.recordQueryShell( query3 );

		List<Vectorizable> readyFdqs = new LinkedList<>();

		// Add query 1's inputs and outputs
		g.addQueryString( query1.getId(), query1.getQueryString() );
		g.addInputParameters( query1.getId(), query1.getParams() );
		g.addResultSet( query1.getId(), new QueryResult( query1Result, version ) );
		g.findAllParameterMappingsForNode( query1.getId() );
		logger.info( "added q1's result set." );

		// Add query 2's inputs and outputs
		g.addQueryString( query2.getId(), query2.getQueryString() );
		g.addInputParameters( query2.getId(), query2.getParams() );
		g.addResultSet( query2.getId(), new QueryResult( query2Result, version ) );
		g.findAllParameterMappingsForNode( query2.getId() );
		logger.info( "added q2's result set." );

		List<Vectorizable> resultantFDQ = g.constructNewFDQs( query2.getId(), query2.getQueryString(), eut.getKnownQueryShells() );
		// Add query 3's inputs and outputs
		g.addQueryString( query3.getId(), query3.getQueryString() );
		g.addInputParameters( query3.getId(), query3.getParams() );
		g.addResultSet( query3.getId(), new QueryResult( query2Result, version ) );
		g.findAllParameterMappingsForNode( query3.getId() );
		logger.info( "added q3's result set." );

		// Assert that the FDQ was built and vectorized correctly
		resultantFDQ = g.constructNewFDQs( query3.getId(), query3.getQueryString(), eut.getKnownQueryShells() );
		assertThat( resultantFDQ.get( 0 ).isReady(), is( true ) );
		assertThat( resultantFDQ.get( 0 ).isAlwaysDefined(), is( true ) );
		assertThat( resultantFDQ.get( 0 ).isSimpleVectorizable(), equalTo( true ) );
		assertThat( resultantFDQ.get( 0 ).isVectorizable(), equalTo( true ) );

		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		QueryVectorizerResult result = vectorizer.vectorize( 0, resultantFDQ.get( 0 ), null );

		assertThat( result.getVectorizedQueryText(), equalTo( "WITH q0 AS ( SELECT bats, CONCAT( t.ctid ) AS q0rn FROM t ), q1 AS ( SELECT hats, bats, CONCAT( t.ctid ) AS q1rn FROM t ), q2 AS ( SELECT cats, bats, hats, CONCAT( t.ctid ) AS q2rn FROM t )\nSELECT * FROM q0 LEFT JOIN q1 ON q1.bats = q0.bats LEFT JOIN q2 ON q2.bats = q0.bats AND q2.hats = q1.hats" ) );
	}

	@Test
	public void testGetDataForIndexNoResultSet() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		List<String> columnNames = new LinkedList<>();
		QueryOutputs qo = new QueryOutputs( q1, columnNames );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );
		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		List<QueryOutputs> queryOutputs = new LinkedList<>();
		queryOutputs.add( qo );
		exceptionTester.expect(NullPointerException.class);
		eut.getIndexData( 0, queryOutputs );
	}

	@Test
	public void testGetDataForIndexNoColumns() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		List<String> columnNames = new LinkedList<>();
		QueryOutputs qo = new QueryOutputs( q1, columnNames );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );
		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		Map<String,Object> q1ResultSetFirstRow = new HashMap<>();
		q1ResultSetFirstRow.put( "col", "test" );
		q1ResultSet.add( q1ResultSetFirstRow );
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		List<QueryOutputs> queryOutputs = new LinkedList<>();
		queryOutputs.add( qo );
		exceptionTester.expect(NullPointerException.class);
		eut.getIndexData( 0, queryOutputs );
	}

	@Test
	public void testGetDataForIndexNoSources() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );
		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		Map<String,Object> q1ResultSetFirstRow = new HashMap<>();
		q1ResultSetFirstRow.put( "col", "test" );
		q1ResultSet.add( q1ResultSetFirstRow );
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		List<QueryOutputs> queryOutputs = new LinkedList<>();
		exceptionTester.expect(NullPointerException.class);
		eut.getIndexData( 0, queryOutputs );
	}

	@Test
	public void testGetDataForIndexColumnMismatch() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		List<String> columnNames = new LinkedList<>();
		columnNames.add("col2");
		QueryOutputs qo = new QueryOutputs( q1, columnNames );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );
		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		Map<String,Object> q1ResultSetFirstRow = new HashMap<>();
		q1ResultSetFirstRow.put( "col", "test" );
		q1ResultSet.add( q1ResultSetFirstRow );
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		List<QueryOutputs> queryOutputs = new LinkedList<>();
		queryOutputs.add( qo );
		exceptionTester.expect(NullPointerException.class);
		eut.getIndexData( 0, queryOutputs );
	}

	@Test
	public void testGetDataForIndexWorks() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		List<String> columnNames = new LinkedList<>();
		columnNames.add("col");
		QueryOutputs qo = new QueryOutputs( q1, columnNames );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );
		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		Map<String,Object> q1ResultSetFirstRow = new HashMap<>();
		q1ResultSetFirstRow.put( "col", "test" );
		q1ResultSet.add( q1ResultSetFirstRow );
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		List<QueryOutputs> queryOutputs = new LinkedList<>();
		queryOutputs.add( qo );
		assertThat( eut.getIndexData( 0, queryOutputs ), is("test") );
	}

	@Test
	public void testTryGetRequiredDataZeroConstants() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );

		Multimap<Integer, QueryOutputs> map = LinkedListMultimap.create();
		List<String> constList = eut.getAllConstants( 0, map );
		assertThat( constList, not( nullValue() ) );
		assertThat( constList.size(), is( 0 ) );
	}

	@Test
	public void testTryGetRequiredDataNotEnoughConstants() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );

		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		Map<String,Object> q1ResultSetFirstRow = new HashMap<>();
		q1ResultSetFirstRow.put( "col1", "test" );
		q1ResultSet.add( q1ResultSetFirstRow );
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		//Query outputs are both from q1, but only col1 exists
		List<String> indOneColumns = new LinkedList<>();
		indOneColumns.add( "col1" );
		List<String> indTwoColumns = new LinkedList<>();
		indTwoColumns.add( "col2" );
		QueryOutputs qo1 = new QueryOutputs( q1, indOneColumns );
		QueryOutputs qo2 = new QueryOutputs( q1, indTwoColumns );

		Multimap<Integer, QueryOutputs> map = LinkedListMultimap.create();
		map.put( 0, qo1 );
		map.put( 1, qo2 );
		exceptionTester.expect(NullPointerException.class);
		List<String> constList = eut.getAllConstants( 0, map );
	}

	@Test
	public void testTryGetRequiredDataWorks() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );

		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		Map<String,Object> q1ResultSetFirstRow = new HashMap<>();
		q1ResultSetFirstRow.put( "col1", "test" );
		q1ResultSetFirstRow.put( "col2", "test" );
		q1ResultSet.add( q1ResultSetFirstRow );
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		//Query outputs are both from q1, but only col1 exists
		List<String> indOneColumns = new LinkedList<>();
		indOneColumns.add( "col1" );
		List<String> indTwoColumns = new LinkedList<>();
		indTwoColumns.add( "col2" );
		QueryOutputs qo1 = new QueryOutputs( q1, indOneColumns );
		QueryOutputs qo2 = new QueryOutputs( q1, indTwoColumns );

		Multimap<Integer, QueryOutputs> map = LinkedListMultimap.create();
		map.put( 0, qo1 );
		map.put( 1, qo2 );
		List<String> constList = eut.getAllConstants( 0, map );
		assertThat( constList, not( nullValue() ) );
		assertThat( constList.size(), is( 2 ) );
	}

	@Test
	public void testTryGetRequiredDataBrokenMappings() throws Exception {
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n1.addEdgeTraversal( n2 );

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		g.addInputParameters( q1, new LinkedList<>() );

		List<Map<String,Object>> q1ResultSet = new LinkedList<>();
		Map<String,Object> q1ResultSetFirstRow = new HashMap<>();
		q1ResultSetFirstRow.put( "col1", "test" );
		q1ResultSetFirstRow.put( "col2", "test" );
		q1ResultSet.add( q1ResultSetFirstRow );
		g.addResultSet( q1, new QueryResult( q1ResultSet, version ) );

		List<String> indOneColumns = new LinkedList<>();
		indOneColumns.add( "col1" );
		List<String> indTwoColumns = new LinkedList<>();
		indTwoColumns.add( "col2" );
		QueryOutputs qo1 = new QueryOutputs( q1, indOneColumns );
		QueryOutputs qo2 = new QueryOutputs( q1, indTwoColumns );

		//Query mappings are from 0,2... but should be 0-1
		Multimap<Integer, QueryOutputs> map = LinkedListMultimap.create();
		map.put( 0, qo1 );
		map.put( 2, qo2 );
		exceptionTester.expect(NullPointerException.class);
		List<String> constList = eut.getAllConstants( 0, map );
	}

	@Test
	public void testFindImportantQueries() throws Exception {
		//Note: q2 is not an FDQ, but we consider it to be for testing purposes
		// q3 is kept as an FDQ to ensure we don't reload FDQs along with ADQs
		ExecutedQuery firstQueryRun = createExecutedQuery( 1000 );
		ExecutedQuery q2FirstQueryRun = createExecutedQueryWithOneParam( 500 );
		ExecutedQuery q3FirstQueryRun = createExecutedQueryWithThreeParams( 1000 );
		DependencyGraph dependencyGraph1 = new DependencyGraph();
		DependencyGraph dependencyGraph2 = new DependencyGraph();
		DependencyGraph dependencyGraph3 = new DependencyGraph();
		VectorizableType vType1 = new VectorizableType();
		vType1.markAsADQ();
		Vectorizable fdq1 = new Vectorizable( dependencyGraph1, firstQueryRun.getId(), "SELECT 1", vType1 );

		VectorizableType vType2 = new VectorizableType();
		vType2.markAsADQ();
		Vectorizable fdq2 = new Vectorizable( dependencyGraph2, q2FirstQueryRun.getId(), "SELECT 1", vType2 );

		VectorizableType vType3 = new VectorizableType();
		vType3.markAsADQ();
		Vectorizable fdq3 = new Vectorizable( dependencyGraph3, q3FirstQueryRun.getId(), "SELECT 1", vType3 );

		MarkovGraph g = new MarkovGraph( 0 );
		g.getOrAddNode( firstQueryRun.getId() ).increaseHitCounter();
		g.getOrAddNode( q2FirstQueryRun.getId() ).increaseHitCounter();
		g.increaseHitCounter();
		g.increaseHitCounter();

		//We've recorded two queries with different query shells, so prob
		//of either executing is 0.5
		//Given cost = prob * avgRunTime, we have scores of 500 and 250
		//Set threshold = 300 to only get one
		Parameters.COMPUTED_RELOAD_THRESHOLD = 300;

		EngineUnderTest eut = new EngineUnderTest();

		eut.addAverageTrackingInfo( firstQueryRun );
		eut.addAverageTrackingInfo( q2FirstQueryRun );
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );

		eut.addADQ( 0, fdq1 );
		eut.addADQ( 0, fdq2 );
		eut.addFDQ( 0, fdq3 );

		Collection<Vectorizable> importantQueries = eut.findImportantQueries( 0 );
		assertThat( importantQueries.size(), is( 1 ) );
	}

	@Test
	public void testConstructFullyDefinedQueryNoPriors() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup the query
		Query query = new Query( "SELECT cats FROM t" );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node = g.getOrAddNode( query.getId() );

		// Setup the engine
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );

		// Pretend to run the query
		g.addQueryString( query.getId(), query.getQueryString() );
		g.addInputParameters( query.getId(), query.getParams() );
		g.addResultSet( query.getId(), new QueryResult( new LinkedList<>(), version ) );
		g.findAllParameterMappingsForNode( query.getId() );

		// Build the FDQ, it should turn out as an ADQ since it's self-contained and has no params
		List<Vectorizable> fdqs = g.constructNewFDQs( query.getId(), query.getQueryString(), eut.getKnownQueryShells() );
		assertThat( fdqs.get( 0 ), not( nullValue() ) );
		assertThat( fdqs.get( 0 ).isAlwaysDefined(), is( true ) );
	}

	@Test
	public void testCannotMapToSelf() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup queries
		Query query = new Query( "SELECT 1 FROM tab WHERE id = 1");

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node1 = g.getOrAddNode( query.getId() );
		node1.addEdgeTraversal( node1 );

		// Setup the engine
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );

		// Setup the outputs of query
		List<Map<String,Object>> results = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		row.put( "1", "1" );
		results.add( row );

		// For q1 to transition to itself there must have been a previous result set
		g.addResultSet( query.getId(), new QueryResult( results, version ) );
		// Process the query normally
		g.addQueryString( query.getId(), query.getQueryString() );
		g.addInputParameters( query.getId(), query.getParams() );
		g.addResultSet( query.getId(), new QueryResult( results, version ) );
		g.findAllParameterMappingsForNode( query.getId() );

		Map<QueryIdentifier, QueryMappingEntry> mappings = g.getPriorQueryMappingEntries( query.getId() );
		assertThat( mappings.get( query.getId() ), is( nullValue() ) );
	}

	@Test
	public void testConstructFullyDefinedQueryRecursivePriors() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		String queryString = "SELECT 2 FROM tab WHERE id=1";
		String queryString2 = "SELECT 1 FROM tab";
		List<String> inputParams = new LinkedList<>();
		inputParams.add( "1" );
		List<String> inputParams2 = new LinkedList<>();

		AntlrParser p = new AntlrParser();
		Query query = new Query(queryString,  p.buildParseTree( queryString ) );
		Query query2 = new Query(queryString,  p.buildParseTree( queryString2 ) );
		QueryIdentifier q1 = new QueryIdentifier( query.getId().getId() );
		QueryIdentifier q2 = new QueryIdentifier( query2.getId().getId() );
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n1 = g.getOrAddNode( q1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		n2.addEdgeTraversal( n1 );
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );
		eut.recordQueryShell( query2 );

		List<Map<String,Object>> resultSet2 = new LinkedList<>();
		Map<String,Object> resultMap2 = new HashMap<>();
		resultMap2.put( "1", "1" );
		resultSet2.add( resultMap2 );
		g.addInputParameters( q2, inputParams2 );
		g.addQueryString( q2, queryString2 );
		g.addResultSet( q2, new QueryResult( resultSet2, version ) );
		g.addInputParameters( q1, inputParams );
		g.addQueryString( q1, queryString );
		g.addResultSet( q1, new QueryResult( new LinkedList<>(), version ) );

		g.findAllParameterMappingsForNode( q1 );
		g.findAllParameterMappingsForNode( q2 );

		Map<QueryIdentifier, QueryMappingEntry> mappings = g.getPriorQueryMappingEntries( query.getId() );
		assertThat( mappings.get( q2 ), not( nullValue() ) );
		List<Vectorizable> fdqs = g.constructNewFDQs( query.getId(), queryString, eut.getKnownQueryShells() );
		assertThat( fdqs.get( 0 ), not( nullValue() ) );
		assertThat( fdqs.get( 0 ).isAlwaysDefined(), is( true ) );
	}

	/**
	 * Test that we can find ADQs for related queries (and ourselves)
	 */
	@Test
	public void findADQs() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup queries
		Query prevQuery = new Query( "SELECT hats FROM t" );
		Query query = new Query( "SELECT bats FROM t WHERE hats = 'blue'" );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode prevNode = g.getOrAddNode( prevQuery.getId() );
		MarkovNode node = g.getOrAddNode( query.getId() );

		prevNode.addEdgeTraversal( node );

		// Setup the inputs/outputs of prevQuery and query
		List<Map<String,Object>> results = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		row.put( "hats", "blue" );
		results.add( row );

		// Setup the engine
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( prevQuery );
		eut.recordQueryShell( query );

		// Pretend to run the engine, adding inputs/outputs of prevQuery and query
		g.addQueryString( prevQuery.getId(), prevQuery.getQueryString() );
		g.addInputParameters( prevQuery.getId(), prevQuery.getParams() );
		g.addResultSet( prevQuery.getId(), new QueryResult( results, version ) );
		g.findAllParameterMappingsForNode( prevQuery.getId() );
		eut.findNewFDQPub( 0, prevQuery.getId(), prevQuery.getQueryString() );

		g.addQueryString( query.getId(), query.getQueryString() );
		g.addInputParameters( query.getId(), query.getParams() );
		g.addResultSet( query.getId(), new QueryResult( new LinkedList<>(), version ) );
		g.findAllParameterMappingsForNode( query.getId() );
		eut.findNewFDQPub( 0, query.getId(), query.getQueryString() );

		// Mark prevQuery as if it as been executed once again
		eut.getVectorizableDependencyTable( 0 ).markExecutedDependency( prevQuery );

		// Get all ready FDQ which should be just query, as prevQuery is a subset
		// they should also be ADqs.
		List<Vectorizable> readyFDQs = eut.getVectorizableDependencyTable( 0 ).getAndClearAllReadyVectorizables();

		assertThat( readyFDQs.size(), is( 1 ) );
		assertThat( readyFDQs.get( 0 ).getQueryId(), is( query.getId() ) );
		assertThat( readyFDQs.get( 0 ).isAlwaysDefined(), is( true ) );
		assertThat( readyFDQs.get( 0 ).getVectorizationDependencies().getAllDependencyMappings().size(), is( 1 ) );
		assertThat( readyFDQs.get( 0 ).isSimpleVectorizable(), is( true ) );
	}

	@Test
	public void discoverAllRunnableFDQsOneAdditionalQuery() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Chain of query transitions is Q1 -> Q2 -> Q3 -> Q4
		// Chain of query mappings is Q2 -> Q3 -> Q4

		// Setup queries
		Query query1 = new Query( "SELECT bats FROM t WHERE id = 1" );
		Query query2 = new Query( "SELECT cats FROM t WHERE id = 2" );
		Query query3 = new Query( "SELECT hats FROM t WHERE id = 3" );
		Query query4 = new Query( "SELECT mats FROM t WHERE id = 7" );

		// Setup markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node1 = g.getOrAddNode( query1.getId() );
		MarkovNode node2 = g.getOrAddNode( query2.getId() );
		MarkovNode node3 = g.getOrAddNode( query3.getId() );
		MarkovNode node4 = g.getOrAddNode( query4.getId() );
		node1.addEdgeTraversal( node2 );
		node2.addEdgeTraversal( node3 );
		node3.addEdgeTraversal( node4 );
		node4.addEdgeTraversal( node1 );

		// Setup engine
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query1 );
		eut.recordQueryShell( query2 );
		eut.recordQueryShell( query3 );
		eut.recordQueryShell( query4 );

		// Setup the inputs/outputs of queries 1-4
		List<Map<String,Object>> results1 = new LinkedList<>();
		Map<String,Object> row1 = new HashMap<>();
		row1.put( "bats", "0" );
		results1.add( row1 );

		List<Map<String,Object>> results2 = new LinkedList<>();
		Map<String,Object> row2 = new HashMap<>();
		row2.put( "cats", "3" );
		results2.add( row2 );

		List<Map<String,Object>> results3 = new LinkedList<>();
		Map<String,Object> row3 = new HashMap<>();
		row3.put( "hats", "7" );
		results3.add( row3 );

		List<Map<String,Object>> results4 = new LinkedList<>();
		Map<String,Object> row4 = new HashMap<>();
		row4.put( "mats", "8" );
		results4.add( row4 );

		// Pretend to execute queries 1 - 4
		g.addQueryString( query1.getId(), query1.getQueryString() );
		g.addInputParameters( query1.getId(), query1.getParams() );
		g.addResultSet( query1.getId(), new QueryResult( results1, version ) );
		g.findAllParameterMappingsForNode( query1.getId() );
		eut.findNewFDQPub( 0, query1.getId(), query1.getQueryString() );

		g.addQueryString( query2.getId(), query2.getQueryString() );
		g.addInputParameters( query2.getId(), query2.getParams() );
		g.addResultSet( query2.getId(), new QueryResult( results2, version ) );
		g.findAllParameterMappingsForNode( query2.getId() );
		eut.findNewFDQPub( 0, query2.getId(), query2.getQueryString() );

		g.addQueryString( query3.getId(), query3.getQueryString() );
		g.addInputParameters( query3.getId(), query3.getParams() );
		g.addResultSet( query3.getId(), new QueryResult( results3, version ) );
		g.findAllParameterMappingsForNode( query3.getId() );
		eut.findNewFDQPub( 0, query3.getId(), query3.getQueryString() );

		g.addQueryString( query4.getId(), query4.getQueryString() );
		g.addInputParameters( query4.getId(), query4.getParams() );
		g.addResultSet( query4.getId(), new QueryResult( results4, version ) );
		g.findAllParameterMappingsForNode( query4.getId() );
		eut.findNewFDQPub( 0, query4.getId(), query4.getQueryString() );

		// Assert that both FDQs were found and that none are ready
		VectorizableDependencyTable dependencyTable = eut.getVectorizableDependencyTable( 0 );
		List<Vectorizable> readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
		assertThat( dependencyTable.size(), is( 2 ) );
		assertThat( readyFDQs.size(), is( 0 ) );

		// Assert that if we mark Q2 as executed we get Q4 is ready, as Q3, is child of Q4
		dependencyTable.markExecutedDependency( query2 );
		readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
		assertThat( readyFDQs.size(), is( 1 ) );
		// this doesn't make sense to me, but it when I print out the readyFDQs
		// query string it is query1 not query4, but it does correctly match query4's id.
		assertThat( readyFDQs.get( 0 ).getQueryId(), is( query4.getId() ) );

		// Clear the readiness of all the fdqs
		for( Vectorizable fdq : readyFDQs ) {
			fdq.unsetReady();
		}

		// Assert that if we only mark Q1 as executed nothing is ready
		dependencyTable.markExecutedDependency( query1 );
		readyFDQs = dependencyTable.getAndClearAllReadyVectorizables();
		assertThat( readyFDQs.size(), is( 0 ) );
	}

	/**
	 * Test we can call getAllConstantPermutations with no
	 * multi-choice constants
	 */
	@Test
	public void testSimpleGetAllConstantPermutations() {
		EngineUnderTest eut = new EngineUnderTest();
		List<Set<String>> constants = new LinkedList<>();
		Set<String> constOpts = new HashSet<>();
		constOpts.add( "'test'" );
		constants.add(  constOpts );
		constants.add(  constOpts );
		constants.add(  constOpts );

		List<List<String>> permutations = eut.getConstantPermutations( constants );
		assertThat( permutations.size(), is( 1 ) );
		List<String> firstPermutation = permutations.get( 0 );
		Iterator<String> constIter = firstPermutation.iterator();
		assertThat( constIter.next(), equalTo( "'test'" ) );
		assertThat( constIter.next(), equalTo( "'test'" ) );
		assertThat( constIter.next(), equalTo( "'test'" ) );
	}

	/**
	 * Test we can handle a multi-choice constant at the start of the list
	 */
	@Test
	public void testInitialMultipleGetAllConstantPermutations() {
		EngineUnderTest eut = new EngineUnderTest();
		List<Set<String>> constants = new LinkedList<>();
		Set<String> firstConstOpts = new HashSet<>();
		Set<String> otherConstOpts = new HashSet<>();
		firstConstOpts.add( "'test'" );
		firstConstOpts.add( "'asdf'" );
		otherConstOpts.add( "3" );
		constants.add(  firstConstOpts );
		constants.add(  otherConstOpts );
		constants.add(  otherConstOpts );

		List<List<String>> permutations = eut.getConstantPermutations( constants );
		assertThat( permutations.size(), is( 2 ) );
		List<String> firstPermutation = permutations.get( 0 );
		Iterator<String> constIter = firstPermutation.iterator();
		assertThat( constIter.next(), equalTo( "'test'" ) );
		assertThat( constIter.next(), equalTo( "3" ) );
		assertThat( constIter.next(), equalTo( "3" ) );

		List<String> secondPermutation = permutations.get( 1 );
		constIter = secondPermutation.iterator();
		assertThat( constIter.next(), equalTo( "'asdf'" ) );
		assertThat( constIter.next(), equalTo( "3" ) );
		assertThat( constIter.next(), equalTo( "3" ) );
	}

	/**
	 * Test we can handle a multi-choice constant mid-list
	 */
	@Test
	public void testMiddleMultipleGetAllConstantPermutations() {
		EngineUnderTest eut = new EngineUnderTest();
		List<Set<String>> constants = new LinkedList<>();
		Set<String> multiConstOpts = new HashSet<>();
		Set<String> singleConstOpts = new HashSet<>();
		multiConstOpts.add( "'test'" );
		multiConstOpts.add( "'asdf'" );
		singleConstOpts.add( "3" );
		constants.add(  singleConstOpts );
		constants.add(  multiConstOpts );
		constants.add(  singleConstOpts );

		List<List<String>> permutations = eut.getConstantPermutations( constants );
		assertThat( permutations.size(), is( 2 ) );
		List<String> firstPermutation = permutations.get( 0 );
		Iterator<String> constIter = firstPermutation.iterator();
		assertThat( constIter.next(), equalTo( "3" ) );
		assertThat( constIter.next(), equalTo( "'test'" ) );
		assertThat( constIter.next(), equalTo( "3" ) );

		List<String> secondPermutation = permutations.get( 1 );
		constIter = secondPermutation.iterator();
		assertThat( constIter.next(), equalTo( "3" ) );
		assertThat( constIter.next(), equalTo( "'asdf'" ) );
		assertThat( constIter.next(), equalTo( "3" ) );
	}

	/**
	 * Test that we can do multiple "multi-choice" constant permutations
	 */
	@Test
	public void testMultipleOptChoicesGetAllConstantPermutations() {
		EngineUnderTest eut = new EngineUnderTest();
		List<Set<String>> constants = new LinkedList<>();
		Set<String> multiConstOpts1 = new HashSet<>();
		Set<String> multiConstOpts2 = new HashSet<>();
		Set<String> singleConstOpts = new HashSet<>();
		multiConstOpts1.add( "'test'" );
		multiConstOpts1.add( "'asdf'" );
		multiConstOpts2.add( "3" );
		multiConstOpts2.add( "5" );
		singleConstOpts.add( "7" );
		constants.add(  multiConstOpts1 );
		constants.add(  singleConstOpts );
		constants.add(  multiConstOpts2 );
		constants.add(  singleConstOpts );

		List<List<String>> permutations = eut.getConstantPermutations( constants );
		assertThat( permutations.size(), is( 4 ) );
		List<String> firstPermutation = permutations.get( 0 );
		Iterator<String> constIter = firstPermutation.iterator();
		assertThat( constIter.next(), equalTo( "'test'" ) );
		assertThat( constIter.next(), equalTo( "7" ) );
		assertThat( constIter.next(), equalTo( "3" ) );
		assertThat( constIter.next(), equalTo( "7" ) );

		List<String> secondPermutation = permutations.get( 1 );
		constIter = secondPermutation.iterator();
		assertThat( constIter.next(), equalTo( "'asdf'" ) );
		assertThat( constIter.next(), equalTo( "7" ) );
		assertThat( constIter.next(), equalTo( "3" ) );
		assertThat( constIter.next(), equalTo( "7" ) );

		List<String> thirdPermutation = permutations.get( 2 );
		constIter = thirdPermutation.iterator();
		assertThat( constIter.next(), equalTo( "'test'" ) );
		assertThat( constIter.next(), equalTo( "7" ) );
		assertThat( constIter.next(), equalTo( "5" ) );
		assertThat( constIter.next(), equalTo( "7" ) );

		List<String> fourthPermutation = permutations.get( 3 );
		constIter = fourthPermutation.iterator();
		assertThat( constIter.next(), equalTo( "'asdf'" ) );
		assertThat( constIter.next(), equalTo( "7" ) );
		assertThat( constIter.next(), equalTo( "5" ) );
		assertThat( constIter.next(), equalTo( "7" ) );

	}

	/**
	 * Test that we only look at related queries during FindFDQs
	 */
	@Test
	public void testFDQDependancyTableStructure() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		String queryString = "SELECT * FROM tab";
		String queryString1 = "SELECT * FROM tab2";
		String queryString2 = "SELECT 1 FROM tab WHERE id=1 AND type=2";
		List<String> inputParams = new LinkedList<>();
		inputParams.add("1");
		inputParams.add("2");

		AntlrParser p = new AntlrParser();
		Query query = new Query(queryString,  p.buildParseTree(queryString));
		Query query1 = new Query(queryString1, p.buildParseTree(queryString1));
		Query query2 = new Query(queryString2, p.buildParseTree(queryString2));

		QueryIdentifier q = new QueryIdentifier(query.getId().getId());
		QueryIdentifier q1 = new QueryIdentifier(query1.getId().getId());
		QueryIdentifier q2 = new QueryIdentifier(query2.getId().getId());

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n = g.getOrAddNode(q);
		MarkovNode n1 = g.getOrAddNode(q1);
		MarkovNode n2 = g.getOrAddNode(q2);

		n.addEdgeTraversal(n2);

		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );
		eut.recordQueryShell(query1);
		eut.recordQueryShell(query2);

		List<Map<String,Object>> resultSet1 = new LinkedList<>();
		Map<String,Object> resultMap1 = new HashMap<>();
		resultMap1.put( "1", "1" );
		resultSet1.add( resultMap1 );

		// What if two different queries independantly trigger Q2?
		g.addResultSet(q, new QueryResult(resultSet1, version));
		g.addInputParameters(q2, inputParams);

		n1.addEdgeTraversal(n2);
		eut.overrideMarkovGraph(0, g);
		g.addResultSet(q1, new QueryResult(resultSet1, version));
		g.addInputParameters(q2, inputParams);

		List<Vectorizable> fdqs = eut.getVectorizableDependencyTable( 0 ).getAndClearAllReadyVectorizables();
		// eut.findFDQs(0, null, q);
		fdqs = eut.getVectorizableDependencyTable( 0 ).getAndClearAllReadyVectorizables();
		// eut.findFDQs(0, null, q1);

		logger.trace("VectorizableDependencyTable for client 0 is: {}", eut.getVectorizableDependencyTable(0));
	}

	// Test discovery of a loop
    @Ignore( "Doesn't work because of multiple base queries without params." )
	@Test
	public void testFindNewLoop() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		String queryString = "SELECT * FROM tab";
		String queryString1 = "SELECT * FROM tab1";
		String queryString2 = "SELECT * FROM tab2";
		String queryString3 = "SELECT * FROM tab3";

		AntlrParser p = new AntlrParser();
		Query query = new Query( "SELECT * FROM tab" );
		Query query1 = new Query( "SELECT * FROM tab1" );
		Query query2 = new Query( "SELECT * FROM tab2" );
		Query query3 = new Query( "SELECT * FROM tab3" );

		QueryIdentifier q = new QueryIdentifier( query.getId().getId() );
		QueryIdentifier q1 = new QueryIdentifier( query1.getId().getId() );
		QueryIdentifier q2 = new QueryIdentifier( query2.getId().getId() );
		QueryIdentifier q3 = new QueryIdentifier( query3.getId().getId() );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n = g.getOrAddNode( q );
		g.addQueryString( query.getId(), query3.getQueryString() );
		MarkovNode n1 = g.getOrAddNode( q1 );
		g.addQueryString( query1.getId(), query3.getQueryString() );
		MarkovNode n2 = g.getOrAddNode( q2 );
		g.addQueryString( query2.getId(), query3.getQueryString() );
		MarkovNode n3 = g.getOrAddNode( q3 );
		g.addQueryString( query3.getId(), query3.getQueryString() );

		n.addEdgeTraversal( n1 );
		n.addEdgeTraversal( n2 );
		n.addEdgeTraversal( n3 );

		// Create connections for loop in transition graph
		n1.addEdgeTraversal( n2 );
		n1.addEdgeTraversal( n3 );
		n1.addEdgeTraversal( n1 );

		n2.addEdgeTraversal( n3 );
		n2.addEdgeTraversal( n1 );
		n2.addEdgeTraversal( n2 );

		n3.addEdgeTraversal( n1 );
		n3.addEdgeTraversal( n2 );
		n3.addEdgeTraversal( n3 );

		// Add dummy parameter mappings as well, same mapping
		// is used multiple times here for convenience
		HashMultimap mappings = HashMultimap.create();
		mappings.put( 0, "column" );
		n.addMappings( q1, mappings, 0 );
		n.addMappings( q2, mappings, 0 );
		n.addMappings( q3, mappings, 0 );

		// Have the engine perform the test
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );
		eut.recordQueryShell( query1 );
		eut.recordQueryShell( query2 );
		eut.recordQueryShell( query3 );
		try {
			eut.findNewLoopPub( 0, q1 );
		} catch(NullPointerException e) {
			//logger.debug("{}", e.getStackTrace());
			e.printStackTrace();
		}

		VectorizableDependencyTable loopDependencyTable = eut.getVectorizableDependencyTable( 0 );
		assertThat( loopDependencyTable.size(), equalTo( 1 ) );
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
		loopDependencyTable.markExecutedDependency( query );
		List<Vectorizable> readyLoops = loopDependencyTable.getAndClearAllReadyVectorizables();
		assertThat( readyLoops.size(), equalTo( 1 ) );
		Vectorizable loop = readyLoops.iterator().next();
		assertThat( loop.getTriggerQueryId(), equalTo( query.getId() ) );
		loop.unsetReady();
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );

	}

	// Test area for now to exercise finding of new loops
    @Ignore( "Cannot have multiple base queries without dependencies." )
	@Test
	public void testLoopBodyFuzzy() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		String queryString = "SELECT s_ FROM tab";
		String queryString1 = "SELECT * FROM tab1";
		String queryString2 = "SELECT * FROM tab2";
		String queryString3 = "SELECT * FROM tab3";
		String queryString4 = "SELECT * FROM tab4";

		AntlrParser p = new AntlrParser();
		Query query = new Query( queryString,  p.buildParseTree( queryString ) );
		Query query1 = new Query( queryString1, p.buildParseTree( queryString1 ) );
		Query query2 = new Query( queryString2, p.buildParseTree( queryString2 ) );
		Query query3 = new Query( queryString3, p.buildParseTree( queryString3 ) );
		Query query4 = new Query( queryString4, p.buildParseTree( queryString4 ) );

		QueryIdentifier q = new QueryIdentifier( query.getId().getId() );
		QueryIdentifier q1 = new QueryIdentifier( query1.getId().getId() );
		QueryIdentifier q2 = new QueryIdentifier( query2.getId().getId() );
		QueryIdentifier q3 = new QueryIdentifier( query3.getId().getId() );
		QueryIdentifier q4 = new QueryIdentifier( query4.getId().getId() );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n = g.getOrAddNode( q );
		g.addQueryString( q, queryString );
		MarkovNode n1 = g.getOrAddNode( q1 );
		g.addQueryString( q1, queryString1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		g.addQueryString( q2, queryString2 );
		MarkovNode n3 = g.getOrAddNode( q3 );
		g.addQueryString( q3, queryString3 );
		MarkovNode n4 = g.getOrAddNode( q4 );
		g.addQueryString( q4, queryString4 );

		n.addEdgeTraversal( n1 );
		n.addEdgeTraversal( n2 );
		n.addEdgeTraversal( n3 );

		// Create connections for loop in transition graph
		// This mimics a graph with window size 4 (inclusive)
		// and sequence Q Q1 Q2 Q3 Q1 Q2 Q3 Q4 Q2
	 	// Here we see that Q4 -> Q2 fuzzes the strict loop rel.
		n1.addEdgeTraversal( n2 );
		n1.addEdgeTraversal( n3 );
		n1.addEdgeTraversal( n1 );

		n2.addEdgeTraversal( n3 );
		n2.addEdgeTraversal( n1 );
		n2.addEdgeTraversal( n2 );

		n3.addEdgeTraversal( n1 );
		n3.addEdgeTraversal( n2 );
		n3.addEdgeTraversal( n3 );

		n1.addEdgeTraversal( n2 );
		n1.addEdgeTraversal( n3 );
		n1.addEdgeTraversal( n4 );

		n2.addEdgeTraversal( n3 );
		n2.addEdgeTraversal( n4 );
		n2.addEdgeTraversal( n2 );

		n3.addEdgeTraversal( n4 );
		n3.addEdgeTraversal( n2 );

		// Add dummy parameter mappings as well, same mapping
		// is used multiple times here for convenience
		HashMultimap mappings = HashMultimap.create();
		mappings.put( 0, "column" );
		n.addMappings( q1, mappings, 0 );
		n.addMappings( q2, mappings, 0 );
		n.addMappings( q3, mappings, 0 );

		// Have the engine perform the test
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );
		eut.recordQueryShell( query1 );
		eut.recordQueryShell( query2 );
		eut.recordQueryShell( query3 );
		eut.recordQueryShell( query4 );
		eut.findNewLoopPub( 0, q1 );


		VectorizableDependencyTable loopDependencyTable = eut.getVectorizableDependencyTable( 0 );
		assertThat( loopDependencyTable.size(), equalTo( 1 ) );
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
		loopDependencyTable.markExecutedDependency( query );
		List<Vectorizable> readyLoops = loopDependencyTable.getAndClearAllReadyVectorizables();
		assertThat( readyLoops.size(), equalTo( 1 ) );
		Vectorizable loop = readyLoops.iterator().next();
		assertThat( loop.getTriggerQueryId(), equalTo( query.getId() ) );
		loop.unsetReady();
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
	}

	// Test when a trigger query is related to everything in one clique and only
	// some stuff in a second clique
    @Ignore( "Can't have all base queries." )
	@Test
	public void testLoopTriggerNotFullyConnected() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		String queryString = "SELECT * FROM tab";
		String queryString1 = "SELECT * FROM tab1";
		String queryString2 = "SELECT * FROM tab2";
		String queryString3 = "SELECT * FROM tab3";
		String queryString4 = "SELECT * FROM tab4";

		AntlrParser p = new AntlrParser();
		Query query = new Query( queryString,  p.buildParseTree( queryString ) );
		Query query1 = new Query( queryString1, p.buildParseTree( queryString1 ) );
		Query query2 = new Query( queryString2, p.buildParseTree( queryString2 ) );
		Query query3 = new Query( queryString3, p.buildParseTree( queryString3 ) );
		Query query4 = new Query( queryString4, p.buildParseTree( queryString4 ) );

		QueryIdentifier q = new QueryIdentifier( query.getId().getId() );
		QueryIdentifier q1 = new QueryIdentifier( query1.getId().getId() );
		QueryIdentifier q2 = new QueryIdentifier( query2.getId().getId() );
		QueryIdentifier q3 = new QueryIdentifier( query3.getId().getId() );
		QueryIdentifier q4 = new QueryIdentifier( query4.getId().getId() );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n = g.getOrAddNode( q );
		g.addQueryString( q, queryString );
		MarkovNode n1 = g.getOrAddNode( q1 );
		g.addQueryString( q1, queryString1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		g.addQueryString( q2, queryString2 );
		MarkovNode n3 = g.getOrAddNode( q3 );
		g.addQueryString( q3, queryString3 );
		MarkovNode n4 = g.getOrAddNode( q4 );
		g.addQueryString( q4, queryString4 );

		n.addEdgeTraversal( n1 );
		n.addEdgeTraversal( n2 );
		n.addEdgeTraversal( n3 );

		// Create connections for loop in transition graph
		// Two cliques but only one is fully related to q
		n1.addEdgeTraversal( n2 );
		n2.addEdgeTraversal( n1 );

		n3.addEdgeTraversal( n4 );
		n4.addEdgeTraversal( n3 );

		// Add dummy parameter mappings as well
		HashMultimap mappings = HashMultimap.create();
		mappings.put( 0, "column" );
		n.addMappings( q1, mappings, 0 );
		n.addMappings( q2, mappings, 0 );

		// Have the engine perform the test
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );
		eut.recordQueryShell( query1 );
		eut.recordQueryShell( query2 );
		eut.recordQueryShell( query3 );
		eut.recordQueryShell( query4 );
		eut.findNewLoopPub( 0, q1 );


		VectorizableDependencyTable loopDependencyTable = eut.getVectorizableDependencyTable( 0 );
		assertThat( loopDependencyTable.size(), equalTo( 1 ) );
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
		loopDependencyTable.markExecutedDependency( query );
		List<Vectorizable> readyLoops = loopDependencyTable.getAndClearAllReadyVectorizables();
		assertThat( readyLoops.size(), equalTo( 1 ) );
		Vectorizable loop = readyLoops.iterator().next();
		assertThat( loop.getTriggerQueryId(), equalTo( query.getId() ) );
		loop.unsetReady();
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
		assertThat( loop.getVectorizationDependencies().getTopologicallyOrderedQueries().size(), equalTo( 3 ) );
	}

	// Test when a query is related to everything in two distinct cliques
    @Ignore( "Can't have all base queries." )
	@Test
	public void testLoopTriggerTwoFullyConnected() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		String queryString = "SELECT * FROM tab";
		String queryString1 = "SELECT * FROM tab1";
		String queryString2 = "SELECT * FROM tab2";
		String queryString3 = "SELECT * FROM tab3";
		String queryString4 = "SELECT * FROM tab4";

		AntlrParser p = new AntlrParser();
		Query query = new Query( queryString,  p.buildParseTree( queryString ) );
		Query query1 = new Query( queryString1, p.buildParseTree( queryString1 ) );
		Query query2 = new Query( queryString2, p.buildParseTree( queryString2 ) );
		Query query3 = new Query( queryString3, p.buildParseTree( queryString3 ) );
		Query query4 = new Query( queryString4, p.buildParseTree( queryString4 ) );

		QueryIdentifier q = new QueryIdentifier( query.getId().getId() );
		QueryIdentifier q1 = new QueryIdentifier( query1.getId().getId() );
		QueryIdentifier q2 = new QueryIdentifier( query2.getId().getId() );
		QueryIdentifier q3 = new QueryIdentifier( query3.getId().getId() );
		QueryIdentifier q4 = new QueryIdentifier( query4.getId().getId() );

		logger.debug( "{} is {}", queryString, q.getId() );
		logger.debug( "{} is {}", queryString1, q1.getId() );
		logger.debug( "{} is {}", queryString2, q2.getId() );
		logger.debug( "{} is {}", queryString3, q3.getId() );
		logger.debug( "{} is {}", queryString4, q4.getId() );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n = g.getOrAddNode( q );
		g.addQueryString( q, queryString );
		MarkovNode n1 = g.getOrAddNode( q1 );
		g.addQueryString( q1, queryString1 );
		MarkovNode n2 = g.getOrAddNode( q2 );
		g.addQueryString( q2, queryString2 );
		MarkovNode n3 = g.getOrAddNode( q3 );
		g.addQueryString( q3, queryString3 );
		MarkovNode n4 = g.getOrAddNode( q4 );
		g.addQueryString( q4, queryString4 );

		n.addEdgeTraversal( n1 );
		n.addEdgeTraversal( n2 );
		n.addEdgeTraversal( n3 );
		n.addEdgeTraversal( n4 );

		// Create connections for loop in transition graph
		// Two cliques and q related fully related to both
		n1.addEdgeTraversal( n2 );
		n2.addEdgeTraversal( n1 );

		n3.addEdgeTraversal( n4 );
		n4.addEdgeTraversal( n3 );

		// Add dummy parameter mappings as well
		HashMultimap mappings = HashMultimap.create();
		mappings.put( 0, "column" );
		n.addMappings( q1, mappings, 0 );
		n.addMappings( q2, mappings, 0 );
		n.addMappings( q3, mappings, 0 );
		n.addMappings( q4, mappings, 0 );

		// Have the engine perform the test
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );
		eut.recordQueryShell( query1 );
		eut.recordQueryShell( query2 );
		eut.recordQueryShell( query3 );
		eut.recordQueryShell( query4 );
		eut.findNewLoopPub( 0, q1 );

		VectorizableDependencyTable loopDependencyTable = eut.getVectorizableDependencyTable( 0 );
		assertThat( loopDependencyTable.size(), equalTo( 1 ) );
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
		loopDependencyTable.markExecutedDependency( query );
		List<Vectorizable> readyLoops = loopDependencyTable.getAndClearAllReadyVectorizables();
		assertThat( readyLoops.size(), equalTo( 1 ) );
		Vectorizable loop = readyLoops.iterator().next();
		assertThat( loop.getTriggerQueryId(), equalTo( query.getId() ) );
		loop.unsetReady();
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
		assertThat( loop.getVectorizationDependencies().getTopologicallyOrderedQueries().size(), equalTo( 3 ) );
	}

	@Test
	public void testOneParamLoop() throws Exception {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		String queryString = "SELECT s_id FROM students";
		String queryString1 = "SELECT grade FROM grades WHERE s_id = 1";

		AntlrParser p = new AntlrParser();
		Query query = new Query( queryString,  p.buildParseTree( queryString ) );
		Query query1 = new Query( queryString1, p.buildParseTree( queryString1 ) );

		QueryIdentifier q = new QueryIdentifier( query.getId().getId() );
		QueryIdentifier q1 = new QueryIdentifier( query1.getId().getId() );

		logger.debug( "{} is {}", queryString, q.getId() );
		logger.debug( "{} is {}", queryString1, q1.getId() );

		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode n = g.getOrAddNode( q );
		g.addQueryString( q, queryString );
		MarkovNode n1 = g.getOrAddNode( q1 );
		g.addQueryString( q1, queryString1 );

		n.addEdgeTraversal( n1 );

		// Create connections for loop in transition graph
		n1.addEdgeTraversal( n1 );

		HashMultimap mappings = HashMultimap.create();
		mappings.put( 0, "s_id" );
		n.addMappings( q1, mappings, 0 );

		// Have the engine perform the test
		EngineUnderTest eut = new EngineUnderTest();
		eut.registerNewClient( 0 );
		eut.overrideMarkovGraph( 0, g );
		eut.recordQueryShell( query );
		eut.recordQueryShell( query1 );
		eut.findNewLoopPub( 0, q1 );


		VectorizableDependencyTable loopDependencyTable = eut.getVectorizableDependencyTable( 0 );
		assertThat( loopDependencyTable.size(), equalTo( 1 ) );

		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 0 ) );
		loopDependencyTable.markExecutedDependency( query );
		assertThat( loopDependencyTable.getAndClearAllReadyVectorizables().size(), equalTo( 1 ) );
	}

	@Test
	public void testIsAlreadyCachedNothingCached() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		EngineUnderTest eut = new EngineUnderTest();

		Query q1 = new Query( "SELECT s_id FROM students" );
		Query q2 = new Query( "SELECT grade FROM grades WHERE s_id = 1" );

		DependencyGraph dependencyGraph = new DependencyGraph();

		dependencyGraph.addBaseQuery( q1.getId(), q1 ) ;
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s_id" );
		QueryMappingEntry qme = new QueryMappingEntry( q1.getId(), q2, mappings );
		dependencyGraph.addDependencyForQuery( q2.getId(), qme );

		VectorizableType vecType = new VectorizableType();
		vecType.markAsFDQ();

		Vectorizable vectorizable = new Vectorizable( dependencyGraph, q2.getId(), q2.getQueryString(), vecType );

		assertThat( vectorizable.isSimpleVectorizable(), equalTo( true ) );
		assertThat( vectorizable.isVectorizable(), equalTo( true ) );

		assertThat( eut.isAlreadyCached( 0, vectorizable, dependencyGraph.getVectorizationPlan() ), equalTo( false ) );
		
	}

	@Test
	public void testIsAlreadyCachedOneLayerCached() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		EngineUnderTest eut = new EngineUnderTest();

		Query q1 = new Query( "SELECT s_id FROM students" );
		Query q2 = new Query( "SELECT grade FROM grades WHERE s_id = 1" );

		DependencyGraph dependencyGraph = new DependencyGraph();

		dependencyGraph.addBaseQuery( q1.getId(), q1 ) ;
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s_id" );
		QueryMappingEntry qme = new QueryMappingEntry( q1.getId(), q2, mappings );
		dependencyGraph.addDependencyForQuery( q2.getId(), qme );

		VectorizableType vecType = new VectorizableType();
		vecType.markAsFDQ();

		Vectorizable vectorizable = new Vectorizable( dependencyGraph, q2.getId(), q2.getQueryString(), vecType );
		assertThat( vectorizable.isSimpleVectorizable(), equalTo( true ) );
		assertThat( vectorizable.isVectorizable(), equalTo( true ) );

		List<Map<String,Object>> rows = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		row.put( "s_id", 1 );
		rows.add( row );

		QueryResult resultSet = new QueryResult( rows, version );
		eut.addToMemcached( q1.getCacheKey(), resultSet );

		assertThat( eut.isAlreadyCached( 0, vectorizable, dependencyGraph.getVectorizationPlan() ), equalTo( false ) );
	}

	@Test
	public void testIsAlreadyCachedBothLayersCached() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		EngineUnderTest eut = new EngineUnderTest();

		Query q1 = new Query( "SELECT s_id FROM students" );
		Query q2 = new Query( "SELECT grade FROM grades WHERE s_id = 1" );

		DependencyGraph dependencyGraph = new DependencyGraph();

		dependencyGraph.addBaseQuery( q1.getId(), q1 ) ;
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s_id" );
		QueryMappingEntry qme = new QueryMappingEntry( q1.getId(), q2, mappings );
		dependencyGraph.addDependencyForQuery( q2.getId(), qme );

		VectorizableType vecType = new VectorizableType();
		vecType.markAsFDQ();

		Vectorizable vectorizable = new Vectorizable( dependencyGraph, q2.getId(), q2.getQueryString(), vecType );
		assertThat( vectorizable.isSimpleVectorizable(), equalTo( true ) );
		assertThat( vectorizable.isVectorizable(), equalTo( true ) );

		List<Map<String,Object>> rows = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		row.put( "s_id", 2 );
		rows.add( row );
		row = new HashMap<>();
		row.put( "s_id", 3 );
		rows.add( row );
		row = new HashMap<>();
		row.put( "s_id", 4 );
		rows.add( row );
		QueryResult resultSet = new QueryResult( rows, version );
		eut.addToMemcached( q1.getCacheKey(), resultSet );

		rows = new LinkedList<>();
		row = new HashMap<>();
		row.put( "grade", 100 );
		rows.add( row );
		resultSet = new QueryResult( rows,version );
		eut.addToMemcached( new Query( "SELECT grade FROM grades WHERE s_id = 2").getCacheKey(), resultSet );
		eut.addToMemcached( new Query( "SELECT grade FROM grades WHERE s_id = 3").getCacheKey(), resultSet );
		eut.addToMemcached( new Query( "SELECT grade FROM grades WHERE s_id = 4").getCacheKey(), resultSet );

		assertThat( eut.isAlreadyCached( 0, vectorizable, dependencyGraph.getVectorizationPlan() ), equalTo( true ) );
	}

	@Test
	public void testPreprocessing() throws InterruptedException {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		EngineUnderTest eut = new EngineUnderTest();
		ExecutorService execServ = eut.retrieveThreadPool();
		eut.registerNewClient( 0 );

		// Setup most of the test
		// Two dependency graphs, both containing q1
		Query q0 = new Query( "SELECT a FROM hats WHERE a > 7" );
		Query q1 = new Query( "SELECT b FROM bats WHERE b > 12" );
		Query q2 = new Query( "SELECT c FROM cats WHERE c = 8" );
		Query q3 = new Query( "SELECT d FROM mats WHERE d < 8" );

		// Create the dependency table
		VectorizableDependencyTable dependencyTable = new VectorizableDependencyTable();

		// Create the dependency graphs to go in the table
		// First FDQ is q0 -> q1
		// Second FDQ is q2 -> q1 -> q3
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

		// Client's vectorization table
		VectorizableDependencyTable vecTable = eut.getVectorizableDependencyTable( 0 );

		// Add the vectorizables to the dependency table
		vecTable.addFDQ(fdq0);
		vecTable.addFDQ(fdq1);

		// Mark base queries as ready
		vecTable.markExecutedDependency( q0 );
		vecTable.markExecutedDependency( q2 );

		// Make sure we record all the query shells
		eut.getKnownQueryShells().put( q0.getId(), q0 );
		eut.getKnownQueryShells().put( q1.getId(), q1 );
		eut.getKnownQueryShells().put( q2.getId(), q2 );
		eut.getKnownQueryShells().put( q3.getId(), q3 );

		// Do the preprocessing
		eut.preprocessQuery( 0, q1 );

		// Terminate the thread pool so we know everything is finished
		execServ.shutdown();
		execServ.awaitTermination(10000, TimeUnit.MILLISECONDS);

		assertThat( eut.getVectorizableDependencyTable( 0 ).getAndClearAllReadyVectorizables().size(), is( 0 ) );
	}

}
