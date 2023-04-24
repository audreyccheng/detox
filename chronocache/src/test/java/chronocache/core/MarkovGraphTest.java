package chronocache.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.AnyOf.anyOf;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ArrayListMultimap;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import chronocache.core.Parameters;
import chronocache.core.Vectorizable;
import org.junit.Test;

public class MarkovGraphTest {

	private Logger logger = LoggerFactory.getLogger( this.getClass() );
	
	@Test
	public void testAddNode() {
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id );
		assertThat( node, sameInstance( node2 ) );
	}

	@Test
	public void testAddTwoNodes() {
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		assertThat( node, not( sameInstance( node2 ) ) );
	}

	@Test
	public void testAddRetrieveResultSet() {
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		List<Map<String,Object>> resultSet = new LinkedList<>();
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );
		assertThat( node.hasPreviousResultSet(), is( true ) );
		assertThat( node2.hasPreviousResultSet(), is( false ) );
	}

	@Test
	public void testAddRetrieveInputSet() {
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		List<String> params = new LinkedList<>();
		g.addInputParameters( id, params );
		assertThat( node.hasInputParameters(), is( true ) );
		assertThat( node2.hasInputParameters(), is( false ) );
	}

	@Test
	public void testGeneralSCCDetection() {
		// This graph is two triangle cycles connected by a
		// directed bridge from the cycle 0,1,2 to the cycle 3,4,5

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );
		QueryIdentifier q4 = new QueryIdentifier( 4 );
		QueryIdentifier q5 = new QueryIdentifier( 5 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );
		MarkovNode node4 = g.getOrAddNode( q4 );
		MarkovNode node5 = g.getOrAddNode( q5 );

		// Setup the directed edges
		// SCC 1
		node0.addEdgeTraversal( node1 );
		node1.addEdgeTraversal( node2 );
		node2.addEdgeTraversal( node0 );
		// Bridge
		node2.addEdgeTraversal( node3 );
		// SCC 2
		node3.addEdgeTraversal( node4 );
		node4.addEdgeTraversal( node5 );
		node5.addEdgeTraversal( node3 );

		// Assert there are 2 components each with 3 vertices
		assertThat( g.findSCC( q0 ).size(), is( 3 ) );
		assertThat( g.findSCC( q4 ).size(), is( 3 ) );
	}

	@Test
	public void testSingleVertexSCCDetection() {
		// This graph is six vertices with no edges

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );
		QueryIdentifier q4 = new QueryIdentifier( 4 );
		QueryIdentifier q5 = new QueryIdentifier( 5 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );
		MarkovNode node4 = g.getOrAddNode( q4 );
		MarkovNode node5 = g.getOrAddNode( q5 );

		// Assert there are 6 components each with 1 vertex
		assertThat( g.findSCC( q0 ).size(), is( 1 ) );
		assertThat( g.findSCC( q1 ).size(), is( 1 ) );
		assertThat( g.findSCC( q2 ).size(), is( 1 ) );
		assertThat( g.findSCC( q3 ).size(), is( 1 ) );
		assertThat( g.findSCC( q4 ).size(), is( 1 ) );
		assertThat( g.findSCC( q5 ).size(), is( 1 ) );
	}

	@Test
	public void testSingleVertexSelfEdgeSCCDetection() {
		// This graph is six vertices each with one self-edge

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );
		QueryIdentifier q4 = new QueryIdentifier( 4 );
		QueryIdentifier q5 = new QueryIdentifier( 5 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );
		MarkovNode node4 = g.getOrAddNode( q4 );
		MarkovNode node5 = g.getOrAddNode( q5 );

		// Setup the directed edges
		node0.addEdgeTraversal( node0 );
		node1.addEdgeTraversal( node1 );
		node2.addEdgeTraversal( node2 );
		node3.addEdgeTraversal( node3 );
		node4.addEdgeTraversal( node4 );
		node5.addEdgeTraversal( node5 );

		// Assert there are 6 components each with 1 vertex
		assertThat( g.findSCC( q0 ).size(), is( 1 ) );
		assertThat( g.findSCC( q1 ).size(), is( 1 ) );
		assertThat( g.findSCC( q2 ).size(), is( 1 ) );
		assertThat( g.findSCC( q3 ).size(), is( 1 ) );
		assertThat( g.findSCC( q4 ).size(), is( 1 ) );
		assertThat( g.findSCC( q5 ).size(), is( 1 ) );
	}

	@Test
	public void testGeneralSelfEdgesSCCDetection() {
		// This graph is two cycles of two vertices connected by
		// a directed bridge. Every vertex has a self-edge

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );

		// Setup the directed edges
		// SCC 1
		node0.addEdgeTraversal( node1 );
		node1.addEdgeTraversal( node0 );
		// Bridge
		node0.addEdgeTraversal( node2 );
		// SCC 2
		node2.addEdgeTraversal( node3 );
		node3.addEdgeTraversal( node2 );
		// Self-edges
		node0.addEdgeTraversal( node0 );
		node1.addEdgeTraversal( node1 );
		node2.addEdgeTraversal( node2 );
		node3.addEdgeTraversal( node3 );

		// Assert there are 2 components each with 2 vertices
		assertThat( g.findSCC( q1 ).size(), is( 2 ) );
		assertThat( g.findSCC( q2 ).size(), is( 2 ) );
	}

	@Test
	public void testNoHamiltonianCycleSCCDetection() {
		// This graph is four vertices. The last three are all
		// connected to the first via both an in- and an out-edge

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );

		// Setup the directed edges
		node0.addEdgeTraversal( node1 );
		node1.addEdgeTraversal( node0 );
		node0.addEdgeTraversal( node2 );
		node2.addEdgeTraversal( node0 );
		node0.addEdgeTraversal( node3 );
		node3.addEdgeTraversal( node0 );

		// Assert there is 1 component with all 4 vertices
		assertThat( g.findSCC( q0 ).size(), is( 4 ) );
	}

	@Test
	public void testK5SCCDetection() {
		// This graph is the completely connected graph K5

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );
		QueryIdentifier q4 = new QueryIdentifier( 4 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );
		MarkovNode node4 = g.getOrAddNode( q4 );

		// Setup the directed edges
		// 0 -> All
		node0.addEdgeTraversal( node1 );
		node0.addEdgeTraversal( node2 );
		node0.addEdgeTraversal( node3 );
		node0.addEdgeTraversal( node4 );
		// 1 -> All
		node1.addEdgeTraversal( node0 );
		node1.addEdgeTraversal( node2 );
		node1.addEdgeTraversal( node3 );
		node1.addEdgeTraversal( node4 );
		// 2 -> All
		node2.addEdgeTraversal( node0 );
		node2.addEdgeTraversal( node1 );
		node2.addEdgeTraversal( node3 );
		node2.addEdgeTraversal( node4 );
		// 3 -> All
		node3.addEdgeTraversal( node0 );
		node3.addEdgeTraversal( node1 );
		node3.addEdgeTraversal( node2 );
		node3.addEdgeTraversal( node4 );
		// 4 -> All
		node4.addEdgeTraversal( node0 );
		node4.addEdgeTraversal( node1 );
		node4.addEdgeTraversal( node2 );
		node4.addEdgeTraversal( node3 );

		// Assert there is 1 component with all 5 vertices
		assertThat( g.findSCC( q0 ).size(), is( 5 ) );
	}

	@Test
	public void testPaperGraphSCCDetection() {
		// This graph is 8 vertices connected in a fairly random way
		// There are 3 components. Comes from the example in the paper
		// on Tarjan's algorithm

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );
		QueryIdentifier q4 = new QueryIdentifier( 4 );
		QueryIdentifier q5 = new QueryIdentifier( 5 );
		QueryIdentifier q6 = new QueryIdentifier( 6 );
		QueryIdentifier q7 = new QueryIdentifier( 7 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );
		MarkovNode node4 = g.getOrAddNode( q4 );
		MarkovNode node5 = g.getOrAddNode( q5 );
		MarkovNode node6 = g.getOrAddNode( q6 );
		MarkovNode node7 = g.getOrAddNode( q7 );

		// Setup the directed edges
		node0.addEdgeTraversal( node1 );
		node1.addEdgeTraversal( node2 );
		node1.addEdgeTraversal( node7 );
		node2.addEdgeTraversal( node3 );
		node2.addEdgeTraversal( node6 );
		node3.addEdgeTraversal( node4 );
		node4.addEdgeTraversal( node2 );
		node4.addEdgeTraversal( node5 );
		node6.addEdgeTraversal( node3 );
		node6.addEdgeTraversal( node5 );
		node7.addEdgeTraversal( node0 );
		node7.addEdgeTraversal( node6 );

		// Assert components has all the same components
		Set<QueryIdentifier> sccOne = new HashSet<>();
		sccOne.add( q0 );
		sccOne.add( q1 );
		sccOne.add( q7 );
		Set<QueryIdentifier> sccTwo = new HashSet<>();
		sccTwo.add( q2 );
		sccTwo.add( q3 );
		sccTwo.add( q4 );
		sccTwo.add( q6 );
		Set<QueryIdentifier> sccThree = new HashSet<>();
		sccThree.add( q5 );
		List<Set<QueryIdentifier>> sccAll = new LinkedList<>();
		sccAll.add( sccThree );
		sccAll.add( sccTwo );
		sccAll.add( sccOne );

		assertThat( g.findSCC( q0 ).equals( sccOne ), is( true ) );
		assertThat( g.findSCC( q3 ).equals( sccTwo ), is( true ) );
		assertThat( g.findSCC( q5 ).equals( sccThree ), is( true ) );
	}

	@Test
	public void testTreeSCCDetection() {
		// This graph is a balanced binary tree all the edges
		// are directed downwards towards the leaves.
		// The tree is labeled left to right one level at a time.

		// Make a new graph
		MarkovGraph g = new MarkovGraph( 0 );

		// Setup the identifiers to go into the graph
		QueryIdentifier q0 = new QueryIdentifier( 0 );
		QueryIdentifier q1 = new QueryIdentifier( 1 );
		QueryIdentifier q2 = new QueryIdentifier( 2 );
		QueryIdentifier q3 = new QueryIdentifier( 3 );
		QueryIdentifier q4 = new QueryIdentifier( 4 );
		QueryIdentifier q5 = new QueryIdentifier( 5 );
		QueryIdentifier q6 = new QueryIdentifier( 6 );

		// Add all the qid's to the graph
		MarkovNode node0 = g.getOrAddNode( q0 );
		MarkovNode node1 = g.getOrAddNode( q1 );
		MarkovNode node2 = g.getOrAddNode( q2 );
		MarkovNode node3 = g.getOrAddNode( q3 );
		MarkovNode node4 = g.getOrAddNode( q4 );
		MarkovNode node5 = g.getOrAddNode( q5 );
		MarkovNode node6 = g.getOrAddNode( q6 );

		// Setup the tree
		node0.addEdgeTraversal( node1 );
		node0.addEdgeTraversal( node2 );
		node1.addEdgeTraversal( node3 );
		node1.addEdgeTraversal( node4 );
		node2.addEdgeTraversal( node5 );
		node2.addEdgeTraversal( node6 );

		// Assert there are 7 components since every vertex will
		// end up a component
		assertThat( g.findSCC( q0 ).size(), is( 1 ) );
		assertThat( g.findSCC( q1 ).size(), is( 1 ) );
		assertThat( g.findSCC( q2 ).size(), is( 1 ) );
		assertThat( g.findSCC( q3 ).size(), is( 1 ) );
		assertThat( g.findSCC( q4 ).size(), is( 1 ) );
		assertThat( g.findSCC( q5 ).size(), is( 1 ) );
		assertThat( g.findSCC( q6 ).size(), is( 1 ) );
	}

	@Test
	public void testAddResultSetNoParameterMappings() {
		Parameters.TRACKING_PERIOD = 1;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		node.addEdgeTraversal( node2 );

		// Add result set for node 1
		List<Map<String,Object>> resultSet = new LinkedList<>();
		resultSet.add( new HashMap<String, Object>() );
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> params = new LinkedList<>();
		g.addInputParameters( id2, params );

		g.findAllParameterMappingsForNode( id2 );

		// Null if the tracking has never been run or has never found mappings before
		// Empty mappings otherwise
		assertThat( g.lookupMappings( id, id2 ), nullValue() );
		assertThat( g.lookupMappings( id2, id ), nullValue() );
	}

	@Test
	public void testAddResultSetGetParameterMappings() {
		Parameters.TRACKING_PERIOD = 1;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		node.addEdgeTraversal( node2 );

		// Add result set for node 1
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String, Object> firstRow = new HashMap<>();
		firstRow.put( "id", (Object) "3" );
		resultSet.add( firstRow );
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> params = new LinkedList<>();
		params.add( "3" );
		g.addInputParameters( id2, params );

		// Look for parameter mappings
		g.findAllParameterMappingsForNode( id2 );

		Multimap<Integer, String> mappings = g.lookupMappings( id, id2 );
		assertThat( mappings, not( nullValue() ) );
		assertThat( mappings.keySet().size(), is( 1 ) );
		assertThat( g.lookupMappings( id2, id ), nullValue() );
	}

	@Test
	public void testAddResultSetNoParameterMappingFound() {
		Parameters.TRACKING_PERIOD = 1;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		node.addEdgeTraversal( node2 );

		// Add result set for node 1
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String, Object> firstRow = new HashMap<>();
		firstRow.put( "id", (Object) "3" );
		resultSet.add( firstRow );
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> params = new LinkedList<>();
		params.add( "129389141" ); //Doesn't match with 3
		g.addInputParameters( id2, params );

		// Look for parameter mappings
		g.findAllParameterMappingsForNode( id2 );

		assertThat( g.lookupMappings( id, id2 ), nullValue() ); // Here
		assertThat( g.lookupMappings( id2, id ), nullValue() );
	}

	@Test
	public void testAddResultSetAdjustOverMultipleRuns() {
		Parameters.TRACKING_PERIOD = 1;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		node.addEdgeTraversal( node2 );

		// Add result set for node 1
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String, Object> firstRow = new HashMap<>();
		firstRow.put( "id", (Object) "3" );
		firstRow.put( "id2", (Object) "3" );
		resultSet.add( firstRow );
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> params = new LinkedList<>();
		params.add( "3" );
		g.addInputParameters( id2, params );

		// Look for mappings for node 2
		g.findAllParameterMappingsForNode( id2 );

		Multimap<Integer, String> mappings = g.lookupMappings( id, id2 );
		assertThat( mappings, not( nullValue() ) );
		assertThat( mappings.keySet().size(), is( 1 ) );
		assertThat( mappings.get(0).size(), is( 2 ) );
		assertThat( g.lookupMappings( id2, id ), nullValue() );

		//Change output set, to have only id2 match
		//We should remove id as a possible mapping
		List<Map<String,Object>> resultSet2 = new LinkedList<>();
		Map<String, Object> firstRow2 = new HashMap<>();
		firstRow2.put( "id", (Object) "2" );
		firstRow2.put( "id2", (Object) "3" );
		resultSet2.add( firstRow2 );
		g.addResultSet( id, new QueryResult( resultSet2, new VersionVector( new ArrayList<Long>() ) ) );

		g.findAllParameterMappingsForNode( id2 );

		//Assert only id2 maps
		mappings = g.lookupMappings( id, id2 );
		assertThat( mappings, not( nullValue() ) );
		assertThat( mappings.keySet().size(), is( 1 ) );
		assertThat( mappings.get(0).size(), is( 1 ) );
		assertThat( g.lookupMappings( id2, id ), nullValue() );
	}

	@Test
	public void testAddResultSetTrackingPeriodNotExceeded() {
		Parameters.TRACKING_PERIOD = 2;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		node.addEdgeTraversal( node2 );

		// Add result set for node 1
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String, Object> firstRow = new HashMap<>();
		firstRow.put( "id", (Object) "3" );
		resultSet.add( firstRow );
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> params = new LinkedList<>();
		params.add( "3" ); //Doesn't match with 3
		g.addInputParameters( id2, params );

		//Null because not enough time has passed
		assertThat( g.lookupMappings( id, id2 ), nullValue() );
		assertThat( g.lookupMappings( id2, id ), nullValue() );
	}

	@Test
	public void testAddResultSetDoesIndependentTracking() {
		Parameters.TRACKING_PERIOD = 1;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		QueryIdentifier id3 = new QueryIdentifier( 2 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		MarkovNode node3 = g.getOrAddNode( id3 );
		node.addEdgeTraversal( node2 );
		node.addEdgeTraversal( node3 );

		// Add result set for node 1
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String, Object> firstRow = new HashMap<>();
		firstRow.put( "id", (Object) "3" );
		resultSet.add( firstRow );
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> node2Params = new LinkedList<>();
		node2Params.add( "3" );
		g.addInputParameters( id2, node2Params );

		// Lookup parameter mappings
		g.findAllParameterMappingsForNode( id2 );

		List<String> node3Params = new LinkedList<>();
		node3Params.add( "4" ); // Doesn't match with 3
		g.addInputParameters( id3, node3Params );

		// Lookup parameter mappings
		g.findAllParameterMappingsForNode( id2 );

		// Test mappings from node -> node2
		Multimap<Integer, String> mappings = g.lookupMappings( id, id2 );
		assertThat( mappings, not( nullValue() ) );
		assertThat( mappings.keySet().size(), is(1) );
		assertThat( g.lookupMappings( id2, id ), nullValue() );

		// Test mappings from node -> node3
		assertThat( g.lookupMappings( id, id3 ), nullValue() );
		assertThat( g.lookupMappings( id3, id ), nullValue() );
	}

	@Test
	public void testAddUpdateResultSetNoMapping() {
		Parameters.TRACKING_PERIOD = 1;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		node.addEdgeTraversal( node2 );

		// Add result set for node 1
		g.addResultSet( id, new QueryResult( 1, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> params = new LinkedList<>();
		params.add( "1" );
		g.addInputParameters( id2, params );

		// Look for parameter mappings
		g.findAllParameterMappingsForNode( id2 );

		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String, Object> firstRow = new HashMap<>();
		firstRow.put( "id", (Object) "1" );
		resultSet.add( firstRow );

		// Add resultset for the second time
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Null because no mappings were found
		Multimap<Integer, String> mappings = g.lookupMappings( id, id2 );
		assertThat( mappings, nullValue() );
	}

	@Test
	public void testAddSameAsUpdateResultSetButFoundMapping() {
		Parameters.TRACKING_PERIOD = 1;
		// Add nodes
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node = g.getOrAddNode( id );
		MarkovNode node2 = g.getOrAddNode( id2 );
		node.addEdgeTraversal( node2 );

		// Add result set for node 1
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String, Object> firstRow = new HashMap<>();
		firstRow.put( "id", (Object) "1" );
		resultSet.add( firstRow );
		g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

		// Add params for node 2
		List<String> params = new LinkedList<>();
		params.add( "1" );
		g.addInputParameters( id2, params );

		// Find parameter mappings
		g.findAllParameterMappingsForNode( id2 );

		//Not Null because it is a select result
		Multimap<Integer, String> mappings = g.lookupMappings( id, id2 );
		assertThat( mappings, not( nullValue() ) );
		assertThat( mappings.keySet().size(), is( 1 ) );
	}

	@Test
    public void testLoopMappings() {
        Parameters.TRACKING_PERIOD = 1;

        // Add nodes
        MarkovGraph g = new MarkovGraph( 0 );
        QueryIdentifier id  = new QueryIdentifier( 0 );
        QueryIdentifier id2 = new QueryIdentifier( 1 );
        QueryIdentifier id3 = new QueryIdentifier( 2 );
        MarkovNode node = g.getOrAddNode( id );
        MarkovNode node2 = g.getOrAddNode( id2 );
        MarkovNode node3 = g.getOrAddNode( id3 );

        node.addEdgeTraversal( node2 );
        node2.addEdgeTraversal( node2 );
        node2.addEdgeTraversal( node3 );
        node.addEdgeTraversal( node2 );
        node2.addEdgeTraversal( node2 );

        List<Map<String, Object>> resultSet = new LinkedList<>();
        Map<String, Object> firstRow = new HashMap<>();
        Map<String, Object> secondRow = new HashMap<>();
        firstRow.put( "id", (Object) "1" );
        secondRow.put( "id", (Object) "2" );
        resultSet.add( firstRow );
        resultSet.add( secondRow );

        g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

        List<String> params = new LinkedList<>();
        params.add( "1" );
        g.addInputParameters( id2, params );
        g.findAllParameterMappingsForNode( id2 );

        g.addResultSet( id, new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() ) ) );

        // Modify resultSet to 'move' to the next row
        //resultSet.remove(firstRow);

        params = new LinkedList<>();
        params.add( "2" );
        g.addInputParameters( id2, params );
        g.findAllParameterMappingsForNode( id2 );

        Multimap<Integer, String> mappings = g.lookupMappings( id, id2 );
        assertThat( mappings, not( nullValue() ) );
        assertThat( mappings.keySet().size(), is( 1 ) );
    }

    @Test
    public void testMergeMappingsIndependentColumns() throws Exception {
    	Map<QueryIdentifier, QueryMappingEntry> priorQueryMappings = new HashMap<>();
		// No Q1 Mappings
    	QueryIdentifier q1 = new QueryIdentifier( 1 );
    	Multimap<Integer, String> q1Mappings = LinkedListMultimap.create();
    	QueryMappingEntry qme1 = new QueryMappingEntry( q1, new Query( "SELECT 1" ), q1Mappings );
    	priorQueryMappings.put( q1, qme1 );

		// Q2 mapping on index 1
    	QueryIdentifier q2 = new QueryIdentifier( 2 );
    	Multimap<Integer, String> q2Mappings = LinkedListMultimap.create();
    	q2Mappings.put( 1, "col1" );
    	QueryMappingEntry qme2 = new QueryMappingEntry( q2, new Query( "SELECT 2" ), q2Mappings );
    	priorQueryMappings.put( q2, qme2 );

		// Q3 mapping on index 2 and 3
    	QueryIdentifier q3 = new QueryIdentifier( 3 );
    	Multimap<Integer, String> q3Mappings = LinkedListMultimap.create();
    	q3Mappings.put( 2, "col1" );
    	q3Mappings.put( 2, "col2" );
    	q3Mappings.put( 3, "col3" );
    	QueryMappingEntry qme3 = new QueryMappingEntry( q3, new Query( "SELECT 3" ), q3Mappings );
    	priorQueryMappings.put( q3, qme3 );

    	MarkovGraph g = new MarkovGraph( 0 );
    	Multimap<Integer, String> mergedResults = g.mergeResultSetMappings( priorQueryMappings );

		//3 unique keys, 4 mappings altogether
    	assertThat( mergedResults.keySet().size(), is( 3 ) );
    	assertThat( mergedResults.size(), is( 4 ) );
    	assertThat( mergedResults.get( 1 ).size(), is( 1 ) );
    	assertThat( mergedResults.get( 2 ).size(), is( 2 ) );
    	assertThat( mergedResults.get( 3 ).size(), is( 1 ) );
    }

    @Test
    public void testMergeMappingsOverlappingColumns() throws Exception {
    	Map<QueryIdentifier, QueryMappingEntry> priorQueryMappings = new HashMap<>();

		// Q1 Mapping on index 1 and 2
    	QueryIdentifier q1 = new QueryIdentifier( 1 );
    	Multimap<Integer, String> q1Mappings = LinkedListMultimap.create();
    	q1Mappings.put( 1, "col1" );
    	q1Mappings.put( 2, "col2" );
    	QueryMappingEntry qme1 = new QueryMappingEntry( q1, new Query( "SELECT 1" ), q1Mappings );
    	priorQueryMappings.put( q1, qme1 );

		// Q2 mapping on index 1
    	QueryIdentifier q2 = new QueryIdentifier( 2 );
    	Multimap<Integer, String> q2Mappings = LinkedListMultimap.create();
    	q2Mappings.put( 1, "col1" );
    	QueryMappingEntry qme2 = new QueryMappingEntry( q1, new Query( "SELECT 2" ), q2Mappings );
    	priorQueryMappings.put( q2, qme2 );

		// Q3 mapping on index 2 and 3
    	QueryIdentifier q3 = new QueryIdentifier( 3 );
    	Multimap<Integer, String> q3Mappings = LinkedListMultimap.create();
    	q3Mappings.put( 2, "col1" );
    	q3Mappings.put( 2, "col2" );
    	q3Mappings.put( 3, "col3" );
    	QueryMappingEntry qme3 = new QueryMappingEntry( q3, new Query( "SELECT 3" ), q3Mappings );
    	priorQueryMappings.put( q3, qme3 );

    	MarkovGraph g = new MarkovGraph( 0 );
    	Multimap<Integer, String> mergedResults = g.mergeResultSetMappings( priorQueryMappings );

		// 3 unique keys, 4 mappings altogether (since a few mappings collide)
    	assertThat( mergedResults.keySet().size(), is( 3 ) );
    	assertThat( mergedResults.size(), is( 4 ) );

		// 1 Mapping for column 1, 2 Mappings for column 2, and 1 Mapping for column 3
    	assertThat( mergedResults.get( 1 ).size(), is( 1 ) );
    	assertThat( mergedResults.get( 2 ).size(), is( 2 ) );
    	assertThat( mergedResults.get( 3 ).size(), is( 1 ) );

		// Assert that the right columns are associated with the right indices.
    	assertThat( mergedResults.get( 1 ).contains( "col1" ), is( true ) );
    	assertThat( mergedResults.get( 2 ).contains( "col1" ), is( true ) );
    	assertThat( mergedResults.get( 2 ).contains( "col2" ), is( true ) );
    	assertThat( mergedResults.get( 3 ).contains( "col3" ), is( true ) );
    }

	@Test
	public void testCompareSameGraph() {
		
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node1 = g.getOrAddNode( id );
		MarkovNode node12 = g.getOrAddNode( id2 );
		node1.addEdgeTraversal( node12 );
		node1.increaseHitCounter();

		MarkovGraph g2 = new MarkovGraph( 1 );
		MarkovNode node2 = g2.getOrAddNode( id );
		MarkovNode node22 = g2.getOrAddNode( id2 );
		node2.addEdgeTraversal( node22 );
		node2.increaseHitCounter();

		assertThat( g.compareToOtherMarkovGraph( g2 ), equalTo( 0.0 ) );

	}

	@Test
	public void testCompareSameGraphIsCommutative() {
		
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		MarkovNode node1 = g.getOrAddNode( id );
		MarkovNode node12 = g.getOrAddNode( id2 );
		node1.addEdgeTraversal( node12 );
		node1.increaseHitCounter();

		MarkovGraph g2 = new MarkovGraph( 1 );

		assertThat( g.compareToOtherMarkovGraph( g2 ), equalTo( 1.0 ) );
		assertThat( g2.compareToOtherMarkovGraph( g ), equalTo( 1.0 ) );
	}

	@Test
	public void testCompareMissingSomeConnections() {
		MarkovGraph g = new MarkovGraph( 0 );
		QueryIdentifier id = new QueryIdentifier( 0 );
		QueryIdentifier id2 = new QueryIdentifier( 1 );
		QueryIdentifier id3 = new QueryIdentifier( 2 );
		MarkovNode node1 = g.getOrAddNode( id );
		MarkovNode node12 = g.getOrAddNode( id2 );
		MarkovNode node13 = g.getOrAddNode( id3 );
		node1.addEdgeTraversal( node12 );
		node1.addEdgeTraversal( node13 );
		node1.increaseHitCounter();

		MarkovGraph g2 = new MarkovGraph( 1 );
		MarkovNode node2 = g2.getOrAddNode( id );
		MarkovNode node22 = g2.getOrAddNode( id2 );
		node2.addEdgeTraversal( node22 );
		node2.increaseHitCounter();

		MarkovGraph g3 = new MarkovGraph( 2 );

		assertThat( g.compareToOtherMarkovGraph( g2 ), equalTo( 1.0 ) );
		assertThat( g2.compareToOtherMarkovGraph( g ), equalTo( 1.0 ) );

		assertThat( g.compareToOtherMarkovGraph( g3 ), equalTo( 2.0 ) );
	}

	public void testCrossProduct() {
		Query q0 = new Query( "SELECT stuff FROM T0 WHERE C0 = 0 AND C1 = 1" );
		Query q1 = new Query( "SELECT stuff FROM T1 WHERE C0 = 0 AND C1 = 1" );
		Query q2 = new Query( "SELECT stuff FROM T2 WHERE C0 = 0" );
		Query q3 = new Query( "SELECT stuff FROM T3 WHERE C0 = 0 AND C2 = 1" );
		Query q4 = new Query( "SELECT stuff FROM T4" );
		Query q5 = new Query( "SELECT stuff FROM T5" );
		Query q6 = new Query( "SELECT stuff FROM T6" );

		MappingEntry me0 = new MappingEntry( q0.getId(), q1.getId(), 0, "stuff" );
		MappingEntry me1 = new MappingEntry( q0.getId(), q2.getId(), 0, "stuff" );
		MappingEntry me2 = new MappingEntry( q0.getId(), q3.getId(), 1, "stuff" );
		MappingEntry me3 = new MappingEntry( q1.getId(), q4.getId(), 0, "stuff" );
		MappingEntry me4 = new MappingEntry( q1.getId(), q5.getId(), 0, "stuff" );
		MappingEntry me5 = new MappingEntry( q1.getId(), q6.getId(), 1, "stuff" );

		MappingEntry[] array0 = { me0 };
		MappingEntry[] array1 = { me1 };
		MappingEntry[] array2 = { me2 };
		MappingEntry[] array3 = { me3 };
		MappingEntry[] array4 = { me4 };
		MappingEntry[] array5 = { me5 };

		// Result arrays
		MappingEntry[] array6 = { me0, me2 };
		MappingEntry[] array7 = { me1, me2 };

		MappingEntry[] array8 = { me2, me4 };
		MappingEntry[] array9 = { me2, me5 };

		MappingEntry[] array10 = { me0, me4 };
		MappingEntry[] array11 = { me0, me5 };
		MappingEntry[] array12 = { me1, me4 };
		MappingEntry[] array13 = { me1, me5 };

		// Print out ids
		logger.debug( "q0 = {}", q0.getId() );
		logger.debug( "q1 = {}", q1.getId() );
		logger.debug( "q2 = {}", q2.getId() );
		logger.debug( "q3 = {}", q3.getId() );
		logger.debug( "q4 = {}", q4.getId() );
		logger.debug( "q5 = {}", q5.getId() );

		List<MappingEntry[]> listOne = new LinkedList<>();
		listOne.add( array0 );
		listOne.add( array1 );

		List<MappingEntry[]> listTwo = new LinkedList<>();
		listTwo.add( array2 );

		List<MappingEntry[]> listThree = new LinkedList<>();
		listThree.add( array4 );
		listThree.add( array5 );

		MarkovGraph g = new MarkovGraph( 0 );

		// First cross product
		List<MappingEntry[]> resultOne = g.crossProduct( listOne, listTwo );

		logger.debug( "First cross product = {" );
		for( MappingEntry[] arrayN : resultOne ) {
			logger.debug( "{} size = {}", arrayN, arrayN.length );
		}
		logger.debug( "}" );

		assertThat( resultOne.size(), is( 2 ) );
		assertThat( Arrays.deepEquals( resultOne.get( 0 ), array6 ), is( true ) );
		assertThat( Arrays.deepEquals( resultOne.get( 1 ), array7 ), is( true ) );

		// Second cross product
		List<MappingEntry[]> resultTwo = g.crossProduct( listTwo, listThree );

		logger.debug( "Second cross product = {" );
		for( MappingEntry[] arrayN : resultTwo ) {
			logger.debug( "{} size = {}", arrayN, arrayN.length );
		}
		logger.debug( "}" );

		assertThat( resultTwo.size(), is( 2 ) );
		assertThat( Arrays.deepEquals( resultTwo.get( 0 ), array8 ), is( true ) );
		assertThat( Arrays.deepEquals( resultTwo.get( 1 ), array9 ), is( true ) );

		// Third cross product
		List<MappingEntry[]> resultThree = g.crossProduct( listOne, listThree );

		logger.debug( "Third cross product = {" );
		for( MappingEntry[] arrayN : resultThree ) {
			logger.debug( "{} size = {}", arrayN, arrayN.length );
		}
		logger.debug( "}" );

		assertThat( resultThree.size(), is( 4 ) );
		assertThat( Arrays.deepEquals( resultThree.get( 0 ), array10 ), is( true ) );
		assertThat( Arrays.deepEquals( resultThree.get( 1 ), array11 ), is( true ) );
		assertThat( Arrays.deepEquals( resultThree.get( 2 ), array12 ), is( true ) );
		assertThat( Arrays.deepEquals( resultThree.get( 3 ), array13 ), is( true ) );

		// Fourth cross product
		List<MappingEntry[]> resultFour = g.crossProduct( resultThree, resultThree );

		logger.debug( "Fourth cross product = {" );
		for( MappingEntry[] arrayN : resultFour ) {
			logger.debug( "{} size = {}", arrayN, arrayN.length );
		}
		logger.debug( "}" );

		assertThat( resultFour.size(), is( 16 ) );
	}

	@Test
	public void testGenerateValidMappingConfigurations() {
		// Setup test queries
		Query q0 = new Query( "SELECT stuff FROM T0 WHERE C0 = 0 AND C1 = 1 AND C2 = 2 AND C3 = 3 AND C4 = 4 AND C5 = 5" );
		Query q1 = new Query( "SELECT stuff FROM T1 LIMIT 1" );
		Query q2 = new Query( "SELECT stuff FROM T2 LIMIT 2" );
		Query q3 = new Query( "SELECT stuff FROM T3 LIMIT 3" );
		Query q4 = new Query( "SELECT stuff FROM T4 LIMIT 4" );
		Query q5 = new Query( "SELECT stuff FROM T5 LIMIT 5" );
		Query q6 = new Query( "SELECT stuff FROM T6 LIMIT 6" );

		// Print query id to query mappings
		logger.debug( "q0 = {}", q0.getId() );
		logger.debug( "q1 = {}", q1.getId() );
		logger.debug( "q2 = {}", q2.getId() );
		logger.debug( "q3 = {}", q3.getId() );
		logger.debug( "q4 = {}", q4.getId() );
		logger.debug( "q5 = {}", q5.getId() );
		logger.debug( "q6 = {}", q6.getId() );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node0 = g.getOrAddNode( q0.getId() );
		node0.addQueryString( q0.getQueryString() );
		MarkovNode node1 = g.getOrAddNode( q1.getId() );
		node1.addQueryString( q1.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( q2.getId() );
		node2.addQueryString( q2.getQueryString() );
		MarkovNode node3 = g.getOrAddNode( q3.getId() );
		node3.addQueryString( q3.getQueryString() );
		MarkovNode node4 = g.getOrAddNode( q4.getId() );
		node4.addQueryString( q4.getQueryString() );
		MarkovNode node5 = g.getOrAddNode( q5.getId() );
		node5.addQueryString( q5.getQueryString() );
		MarkovNode node6 = g.getOrAddNode( q6.getId() );
		node6.addQueryString( q6.getQueryString() );

		// Add traversals to the markov graph
		node1.addEdgeTraversal( node0 );
		node2.addEdgeTraversal( node0 );
		node3.addEdgeTraversal( node0 );
		node4.addEdgeTraversal( node0 );
		node5.addEdgeTraversal( node0 );
		node6.addEdgeTraversal( node0 );

		// Add mappings to the markov graph
		Multimap<Integer, String> q1Mappings = ArrayListMultimap.create();
		q1Mappings.put( 0, "stuff" );
		node1.addMappings( q0.getId(), q1Mappings, 0);
		Multimap<Integer, String> q2Mappings = ArrayListMultimap.create();
		q2Mappings.put( 0, "stuff" );
		node2.addMappings( q0.getId(), q2Mappings, 0);
		Multimap<Integer, String> q3Mappings = ArrayListMultimap.create();
		q3Mappings.put( 1, "stuff" );
		node3.addMappings( q0.getId(), q3Mappings, 0);
		Multimap<Integer, String> q4Mappings = ArrayListMultimap.create();
		q4Mappings.put( 5, "stuff" );
		node4.addMappings( q0.getId(), q4Mappings, 0);
		Multimap<Integer, String> q5Mappings = ArrayListMultimap.create();
		q5Mappings.put( 5, "stuff" );
		node5.addMappings( q0.getId(), q5Mappings, 0);
		Multimap<Integer, String> q6Mappings = ArrayListMultimap.create();
		q6Mappings.put( 5, "stuff" );
		node6.addMappings( q0.getId(), q6Mappings, 0);

		// Add everything to known query shells
		Map<QueryIdentifier, Query> knownQueryShells = new HashMap<>();
		knownQueryShells.put( q0.getId(), q0 );
		knownQueryShells.put( q1.getId(), q1 );
		knownQueryShells.put( q2.getId(), q2 );
		knownQueryShells.put( q3.getId(), q3 );
		knownQueryShells.put( q4.getId(), q4 );
		knownQueryShells.put( q5.getId(), q5 );
		knownQueryShells.put( q6.getId(), q6 );

		Multimap<Integer, MappingEntry[]> singletonValues = g.generateSingletonValues( g.getPriorMappingEntriesByPosition( q0.getId() ) );
		// Expand these single-element arrays into full size-of-mapped-parameters configurations by
		// taking the cross product of the sets of mappings for each position
		Iterator<Integer> it = singletonValues.keySet().iterator();
		Collection<MappingEntry[]> validMappingConfigurations = singletonValues.get( it.next() );
		while( it.hasNext() ) {
			validMappingConfigurations = g.crossProduct( validMappingConfigurations, singletonValues.get( it.next() ) );
		}

		logger.debug( "Valid Configurations = {" );
		for( MappingEntry[] mappingEntryArray : validMappingConfigurations ) {
			logger.debug( "    {} size = {}", mappingEntryArray, mappingEntryArray.length );
		}
		logger.debug( "}" );

		assertThat( validMappingConfigurations.size(), is( 6 ) );
	}

	@Test
	public void testMultipleDependencyGraphGeneration() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;

		// Setup test queries
		Query q0 = new Query( "SELECT stuff FROM T0 WHERE C0 = 0 AND C1 = 1" );
		Query q1 = new Query( "SELECT stuff FROM T1 WHERE C0 = 0 AND C1 = 1" );
		Query q2 = new Query( "SELECT stuff FROM T2 WHERE C0 = 0" );
		Query q3 = new Query( "SELECT stuff FROM T3 WHERE C0 = 0 AND C2 = 1" );
		Query q4 = new Query( "SELECT stuff FROM T4" );
		Query q5 = new Query( "SELECT stuff FROM T5" );
		Query q6 = new Query( "SELECT stuff FROM T6" );
		Query q7 = new Query( "SELECT stuff FROM T7" );
		Query q8 = new Query( "SELECT stuff FROM T8" );
		Query q9 = new Query( "SELECT stuff FROM T9" );
		Query q10 = new Query( "SELECT stuff FROM T10" );

		// Print query id to query mappings
		logger.debug( "q0 = {}", q0.getId() );
		logger.debug( "q1 = {}", q1.getId() );
		logger.debug( "q2 = {}", q2.getId() );
		logger.debug( "q3 = {}", q3.getId() );
		logger.debug( "q4 = {}", q4.getId() );
		logger.debug( "q5 = {}", q5.getId() );
		logger.debug( "q6 = {}", q6.getId() );
		logger.debug( "q7 = {}", q7.getId() );
		logger.debug( "q8 = {}", q8.getId() );
		logger.debug( "q9 = {}", q9.getId() );
		logger.debug( "q10 = {}", q10.getId() );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node0 = g.getOrAddNode( q0.getId() );
		node0.addQueryString( q0.getQueryString() );
		MarkovNode node1 = g.getOrAddNode( q1.getId() );
		node1.addQueryString( q1.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( q2.getId() );
		node2.addQueryString( q2.getQueryString() );
		MarkovNode node3 = g.getOrAddNode( q3.getId() );
		node3.addQueryString( q3.getQueryString() );
		MarkovNode node4 = g.getOrAddNode( q4.getId() );
		node4.addQueryString( q4.getQueryString() );
		MarkovNode node5 = g.getOrAddNode( q5.getId() );
		node5.addQueryString( q5.getQueryString() );
		MarkovNode node6 = g.getOrAddNode( q6.getId() );
		node6.addQueryString( q6.getQueryString() );
		MarkovNode node7 = g.getOrAddNode( q7.getId() );
		node7.addQueryString( q7.getQueryString() );
		MarkovNode node8 = g.getOrAddNode( q8.getId() );
		node8.addQueryString( q8.getQueryString() );
		MarkovNode node9 = g.getOrAddNode( q9.getId() );
		node9.addQueryString( q9.getQueryString() );
		MarkovNode node10 = g.getOrAddNode( q10.getId() );
		node10.addQueryString( q10.getQueryString() );

		// Add traversals to the markov graph
		node1.addEdgeTraversal( node0 );
		node2.addEdgeTraversal( node0 );
		node3.addEdgeTraversal( node0 );
		
		node4.addEdgeTraversal( node1 );
		node5.addEdgeTraversal( node1 );
		node6.addEdgeTraversal( node1 );
		
		node7.addEdgeTraversal( node2 );
		
		node8.addEdgeTraversal( node3 );
		node9.addEdgeTraversal( node3 );
		node10.addEdgeTraversal( node3 );

		// Add mappings to the markov graph
		Multimap<Integer, String> q1Mappings = LinkedListMultimap.create();
		q1Mappings.put( 0, "stuff" );
		node1.addMappings( q0.getId(), q1Mappings, 0);
		Multimap<Integer, String> q2Mappings = LinkedListMultimap.create();
		q2Mappings.put( 0, "stuff" );
		node2.addMappings( q0.getId(), q2Mappings, 0);
		Multimap<Integer, String> q3Mappings = LinkedListMultimap.create();
		q3Mappings.put( 1, "stuff" );
		node3.addMappings( q0.getId(), q3Mappings, 0);

		Multimap<Integer, String> q4Mappings = LinkedListMultimap.create();
		q4Mappings.put( 0, "stuff" );
		node4.addMappings( q1.getId(), q4Mappings, 0);
		Multimap<Integer, String> q5Mappings = LinkedListMultimap.create();
		q5Mappings.put( 0, "stuff" );
		node5.addMappings( q1.getId(), q5Mappings, 0);
		Multimap<Integer, String> q6Mappings = LinkedListMultimap.create();
		q6Mappings.put( 1, "stuff" );
		node6.addMappings( q1.getId(), q6Mappings, 0);

		Multimap<Integer, String> q7Mappings = LinkedListMultimap.create();
		q7Mappings.put( 0, "stuff" );
		node7.addMappings( q2.getId(), q7Mappings, 0);

		Multimap<Integer, String> q8Mappings = LinkedListMultimap.create();
		q8Mappings.put( 0, "stuff" );
		node8.addMappings( q3.getId(), q8Mappings, 0);
		Multimap<Integer, String> q9Mappings = LinkedListMultimap.create();
		q9Mappings.put( 1, "stuff" );
		node9.addMappings( q3.getId(), q9Mappings, 0);
		Multimap<Integer, String> q10Mappings = LinkedListMultimap.create();
		q10Mappings.put( 1, "stuff" );
		node10.addMappings( q3.getId(), q10Mappings, 0);

		// Add everything to known query shells
		Map<QueryIdentifier, Query> knownQueryShells = new HashMap<>();
		knownQueryShells.put( q0.getId(), q0 );
		knownQueryShells.put( q1.getId(), q1 );
		knownQueryShells.put( q2.getId(), q2 );
		knownQueryShells.put( q3.getId(), q3 );
		knownQueryShells.put( q4.getId(), q4 );
		knownQueryShells.put( q5.getId(), q5 );
		knownQueryShells.put( q6.getId(), q6 );
		knownQueryShells.put( q7.getId(), q7 );
		knownQueryShells.put( q8.getId(), q8 );
		knownQueryShells.put( q9.getId(), q9 );
		knownQueryShells.put( q10.getId(), q10 );

		// Find the graphs for the test
		List<Vectorizable> allTheGraphs = g.constructNewFDQs( q0.getId(), q0.getQueryString(), knownQueryShells );

		// Log everything
		g.printParameterMappings();
		logger.debug( "Generated {} graphs", allTheGraphs.size() );
		for( Vectorizable graph : allTheGraphs ) {
			logger.debug( "Vectorizable Id: {}", graph.getVectorizableId() );
			graph.getVectorizationDependencies().dumpToLog();
		}

		// Test our found graphs
		assertThat( allTheGraphs.size(), is( 6 ) );

		// Test graph 1
		List<QueryIdentifier> topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
            logger.warn( "VECTORIZABLE: {}", v.getVectorizableId().getId() );
			if( v.getVectorizableId().getId() == -914176209 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
        logger.warn( "Q9 ID is: {}", q9.getId() );
		assertThat( topologicalOrder.get( 0 ), is( q9.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q6.getId() ) );
		assertThat( topologicalOrder.get( 4 ), is( q4.getId() ) );
		assertThat( topologicalOrder.get( 5 ), is( q1.getId() ) );
		assertThat( topologicalOrder.get( 6 ), is( q0.getId() ) );

		// Test graph 2
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() ==  -807352529 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q6.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q4.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q1.getId() ) );
		assertThat( topologicalOrder.get( 4 ), is( q10.getId() ) );
		assertThat( topologicalOrder.get( 5 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 6 ), is( q0.getId() ) );

		// Test graph 3
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == 1943062319 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q6.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q5.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q1.getId() ) );
		assertThat( topologicalOrder.get( 4 ), is( q10.getId() ) );
		assertThat( topologicalOrder.get( 5 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 6 ), is( q0.getId() ) );

		// Test graph 4
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == 1836238639 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q9.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q6.getId() ) );
		assertThat( topologicalOrder.get( 4 ), is( q5.getId() ) );
		assertThat( topologicalOrder.get( 5 ), is( q1.getId() ) );
		assertThat( topologicalOrder.get( 6 ), is( q0.getId() ) );

		// Test graph 5
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == -1107830980 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q7.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q10.getId() ) );
		assertThat( topologicalOrder.get( 4 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 5 ), is( q0.getId() ) );

		// Test graph 6
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == -1214654660 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q9.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q7.getId() ) );
		assertThat( topologicalOrder.get( 4 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 5 ), is( q0.getId() ) );
	}

	@Test
	public void testMultipleDependencyGraphGenerationDoubleConflicting() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		// Here we have a Q0 with two constants. Both Q1 and Q2 can provide both constants for Q0
		// that means we should get back 4 possible dep. graphs. One where Q1 provides everything,
		// one where Q2 provides everything, one where Q1 and Q2 provide the first and second
		// constants respectively, and finally one where Q2 and Q1 provide the first and second
		// constants respectively.

		// Setup test queries
		Query q0 = new Query( "SELECT stuff FROM T0 WHERE C0 = 0 AND C1 = 1" );
		Query q1 = new Query( "SELECT C1, C2 FROM T1 LIMIT 1" );
		Query q2 = new Query( "SELECT C1, C2 FROM T2 LIMIT 2" );

		// Print query id to query mappings
		logger.debug( "q0 = {}", q0.getId() );
		logger.debug( "q1 = {}", q1.getId() );
		logger.debug( "q2 = {}", q2.getId() );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node0 = g.getOrAddNode( q0.getId() );
		node0.addQueryString( q0.getQueryString() );
		MarkovNode node1 = g.getOrAddNode( q1.getId() );
		node1.addQueryString( q1.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( q2.getId() );

		// Add traversals to the markov graph
		node1.addEdgeTraversal( node0 );
		node2.addEdgeTraversal( node0 );

		// Add mappings to the markov graph
		Multimap<Integer, String> q1Mappings = LinkedListMultimap.create();
		q1Mappings.put( 0, "C1" );
		q1Mappings.put( 1, "C2" );
		node1.addMappings( q0.getId(), q1Mappings, 0);
		Multimap<Integer, String> q2Mappings = LinkedListMultimap.create();
		q2Mappings.put( 0, "C1" );
		q2Mappings.put( 1, "C2" );
		node2.addMappings( q0.getId(), q2Mappings, 0);

		// Add everything to known query shells
		Map<QueryIdentifier, Query> knownQueryShells = new HashMap<>();
		knownQueryShells.put( q0.getId(), q0 );
		knownQueryShells.put( q1.getId(), q1 );
		knownQueryShells.put( q2.getId(), q2 );

		// Find the graphs for the test
		List<Vectorizable> allTheGraphs = g.constructNewFDQs( q0.getId(), q0.getQueryString(), knownQueryShells );

		// Log everything
		g.printParameterMappings();
		logger.debug( "Generated {} graphs", allTheGraphs.size() );
		for( Vectorizable graph : allTheGraphs ) {
			logger.debug( "Vectorizable Id: {}", graph.getVectorizableId() );
			graph.getVectorizationDependencies().dumpToLog();
		}

		// Test our found graphs
		assertThat( allTheGraphs.size(), is( 4 ) );

		// Test graph 1
		List<QueryIdentifier> topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
            logger.warn( "VECTORIZABLE ID: {}", v.getVectorizableId().getId() );
			if( v.getVectorizableId().getId() == -1050859396 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q1.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q0.getId() ) );

		// Test graph 2
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == 1030563964 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q0.getId() ) );

		// Test graph 3
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == -544175903 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), anyOf( is( q2.getId() ), is( q1.getId() ) ) );
		assertThat( topologicalOrder.get( 1 ), anyOf( is( q2.getId() ), is( q1.getId() ) ) );
		assertThat( topologicalOrder.get( 0 ) == topologicalOrder.get( 1 ), is( false ) );
		assertThat( topologicalOrder.get( 2 ), is( q0.getId() ) );

		// Test graph 4
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == -544175903 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), anyOf( is( q2.getId() ), is( q1.getId() ) ) );
		assertThat( topologicalOrder.get( 1 ), anyOf( is( q2.getId() ), is( q1.getId() ) ) );
		assertThat( topologicalOrder.get( 0 ) == topologicalOrder.get( 1 ), is( false ) );
		assertThat( topologicalOrder.get( 2 ), is( q0.getId() ) );
	}

	@Test
	public void testMultipleDependencyGraphGenerationNoChoice() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		// Here we have a test with no choices so at each query there is exactly one valid mapping
		// configuration. Therefore this should collapse to be the same as our previous dependency
		// graph finding and give exactly one graph. The setup here is the same as in
		// testMultipleDependencyGraphGeneration but with Q1, Q10 and their dependencies removed.

		// Setup test queries
		Query q0 = new Query( "SELECT stuff FROM T0 WHERE C0 = 0 AND C1 = 1" );
		Query q2 = new Query( "SELECT stuff FROM T2 WHERE C0 = 0" );
		Query q3 = new Query( "SELECT stuff FROM T3 WHERE C0 = 0 AND C2 = 1" );
		Query q7 = new Query( "SELECT stuff FROM T7" );
		Query q8 = new Query( "SELECT stuff FROM T8" );
		Query q9 = new Query( "SELECT stuff FROM T9" );

		// Print query id to query mappings
		logger.debug( "q0 = {}", q0.getId() );
		logger.debug( "q2 = {}", q2.getId() );
		logger.debug( "q3 = {}", q3.getId() );
		logger.debug( "q7 = {}", q7.getId() );
		logger.debug( "q8 = {}", q8.getId() );
		logger.debug( "q9 = {}", q9.getId() );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node0 = g.getOrAddNode( q0.getId() );
		node0.addQueryString( q0.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( q2.getId() );
		node2.addQueryString( q2.getQueryString() );
		MarkovNode node3 = g.getOrAddNode( q3.getId() );
		node3.addQueryString( q3.getQueryString() );
		MarkovNode node7 = g.getOrAddNode( q7.getId() );
		node7.addQueryString( q7.getQueryString() );
		MarkovNode node8 = g.getOrAddNode( q8.getId() );
		node8.addQueryString( q8.getQueryString() );
		MarkovNode node9 = g.getOrAddNode( q9.getId() );
		node9.addQueryString( q9.getQueryString() );

		// Add traversals to the markov graph
		node2.addEdgeTraversal( node0 );
		node3.addEdgeTraversal( node0 );

		node7.addEdgeTraversal( node2 );

		node8.addEdgeTraversal( node3 );
		node9.addEdgeTraversal( node3 );

		// Add mappings to the markov graph
		Multimap<Integer, String> q2Mappings = LinkedListMultimap.create();
		q2Mappings.put( 0, "stuff" );
		node2.addMappings( q0.getId(), q2Mappings, 0);
		Multimap<Integer, String> q3Mappings = LinkedListMultimap.create();
		q3Mappings.put( 1, "stuff" );
		node3.addMappings( q0.getId(), q3Mappings, 0);

		Multimap<Integer, String> q7Mappings = LinkedListMultimap.create();
		q7Mappings.put( 0, "stuff" );
		node7.addMappings( q2.getId(), q7Mappings, 0);

		Multimap<Integer, String> q8Mappings = LinkedListMultimap.create();
		q8Mappings.put( 0, "stuff" );
		node8.addMappings( q3.getId(), q8Mappings, 0);
		Multimap<Integer, String> q9Mappings = LinkedListMultimap.create();
		q9Mappings.put( 1, "stuff" );
		node9.addMappings( q3.getId(), q9Mappings, 0);

		// Add everything to known query shells
		Map<QueryIdentifier, Query> knownQueryShells = new HashMap<>();
		knownQueryShells.put( q0.getId(), q0 );
		knownQueryShells.put( q2.getId(), q2 );
		knownQueryShells.put( q3.getId(), q3 );
		knownQueryShells.put( q7.getId(), q7 );
		knownQueryShells.put( q8.getId(), q8 );
		knownQueryShells.put( q9.getId(), q9 );

		// Find the graphs for the test
		List<Vectorizable> allTheGraphs = g.constructNewFDQs( q0.getId(), q0.getQueryString(), knownQueryShells );

		// Log everything
		g.printParameterMappings();
		logger.debug( "Generated {} graphs", allTheGraphs.size() );
		for( Vectorizable graph : allTheGraphs ) {
			graph.getVectorizationDependencies().dumpToLog();
		}

		// Test our found graphs
		assertThat( allTheGraphs.size(), is( 1 ) );

		List<QueryIdentifier> topologicalOrder = allTheGraphs.get( 0 ).getVectorizationDependencies().getTopologicallyOrderedQueries();
		assertThat( topologicalOrder.get( 0 ), is( q9.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q7.getId() ) );
		assertThat( topologicalOrder.get( 4 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 5 ), is( q0.getId() ) );
	}

	@Test
	public void testMultipleDependencyGraphGenerationManyChoices() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		// Here we have the same setup as in testMultipleDependencyGraphGeneration with the mapping
		// of Q3 shifted to position 0 => 3 choices for mappings at position 0

		// Setup test queries
		Query q0 = new Query( "SELECT stuff FROM T0 WHERE C0 = 0" );
		Query q1 = new Query( "SELECT stuff FROM T1 WHERE C0 = 0 AND C1 = 1" );
		Query q2 = new Query( "SELECT stuff FROM T2 WHERE C0 = 0" );
		Query q3 = new Query( "SELECT stuff FROM T3 WHERE C0 = 0 AND C2 = 1" );
		Query q4 = new Query( "SELECT stuff FROM T4" );
		Query q5 = new Query( "SELECT stuff FROM T5" );
		Query q6 = new Query( "SELECT stuff FROM T6" );
		Query q7 = new Query( "SELECT stuff FROM T7" );
		Query q8 = new Query( "SELECT stuff FROM T8" );
		Query q9 = new Query( "SELECT stuff FROM T9" );
		Query q10 = new Query( "SELECT stuff FROM T10" );

		// Print query id to query mappings
		logger.debug( "q0 = {}", q0.getId() );
		logger.debug( "q1 = {}", q1.getId() );
		logger.debug( "q2 = {}", q2.getId() );
		logger.debug( "q3 = {}", q3.getId() );
		logger.debug( "q4 = {}", q4.getId() );
		logger.debug( "q5 = {}", q5.getId() );
		logger.debug( "q6 = {}", q6.getId() );
		logger.debug( "q7 = {}", q7.getId() );
		logger.debug( "q8 = {}", q8.getId() );
		logger.debug( "q9 = {}", q9.getId() );
		logger.debug( "q10 = {}", q10.getId() );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node0 = g.getOrAddNode( q0.getId() );
		node0.addQueryString( q0.getQueryString() );
		MarkovNode node1 = g.getOrAddNode( q1.getId() );
		node1.addQueryString( q1.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( q2.getId() );
		node2.addQueryString( q2.getQueryString() );
		MarkovNode node3 = g.getOrAddNode( q3.getId() );
		node3.addQueryString( q3.getQueryString() );
		MarkovNode node4 = g.getOrAddNode( q4.getId() );
		node4.addQueryString( q4.getQueryString() );
		MarkovNode node5 = g.getOrAddNode( q5.getId() );
		node5.addQueryString( q5.getQueryString() );
		MarkovNode node6 = g.getOrAddNode( q6.getId() );
		node6.addQueryString( q6.getQueryString() );
		MarkovNode node7 = g.getOrAddNode( q7.getId() );
		node7.addQueryString( q7.getQueryString() );
		MarkovNode node8 = g.getOrAddNode( q8.getId() );
		node8.addQueryString( q8.getQueryString() );
		MarkovNode node9 = g.getOrAddNode( q9.getId() );
		node9.addQueryString( q9.getQueryString() );
		MarkovNode node10 = g.getOrAddNode( q10.getId() );
		node10.addQueryString( q10.getQueryString() );

		// Add traversals to the markov graph
		node1.addEdgeTraversal( node0 );
		node2.addEdgeTraversal( node0 );
		node3.addEdgeTraversal( node0 );
		
		node4.addEdgeTraversal( node1 );
		node5.addEdgeTraversal( node1 );
		node6.addEdgeTraversal( node1 );
		
		node7.addEdgeTraversal( node2 );
		
		node8.addEdgeTraversal( node3 );
		node9.addEdgeTraversal( node3 );
		node10.addEdgeTraversal( node3 );

		// Add mappings to the markov graph
		Multimap<Integer, String> q1Mappings = LinkedListMultimap.create();
		q1Mappings.put( 0, "stuff" );
		node1.addMappings( q0.getId(), q1Mappings, 0);
		Multimap<Integer, String> q2Mappings = LinkedListMultimap.create();
		q2Mappings.put( 0, "stuff" );
		node2.addMappings( q0.getId(), q2Mappings, 0);
		Multimap<Integer, String> q3Mappings = LinkedListMultimap.create();
		q3Mappings.put( 0, "stuff" );
		node3.addMappings( q0.getId(), q3Mappings, 0);

		Multimap<Integer, String> q4Mappings = LinkedListMultimap.create();
		q4Mappings.put( 0, "stuff" );
		node4.addMappings( q1.getId(), q4Mappings, 0);
		Multimap<Integer, String> q5Mappings = LinkedListMultimap.create();
		q5Mappings.put( 0, "stuff" );
		node5.addMappings( q1.getId(), q5Mappings, 0);
		Multimap<Integer, String> q6Mappings = LinkedListMultimap.create();
		q6Mappings.put( 1, "stuff" );
		node6.addMappings( q1.getId(), q6Mappings, 0);

		Multimap<Integer, String> q7Mappings = LinkedListMultimap.create();
		q7Mappings.put( 0, "stuff" );
		node7.addMappings( q2.getId(), q7Mappings, 0);

		Multimap<Integer, String> q8Mappings = LinkedListMultimap.create();
		q8Mappings.put( 0, "stuff" );
		node8.addMappings( q3.getId(), q8Mappings, 0);
		Multimap<Integer, String> q9Mappings = LinkedListMultimap.create();
		q9Mappings.put( 1, "stuff" );
		node9.addMappings( q3.getId(), q9Mappings, 0);
		Multimap<Integer, String> q10Mappings = LinkedListMultimap.create();
		q10Mappings.put( 1, "stuff" );
		node10.addMappings( q3.getId(), q10Mappings, 0);

		// Add everything to known query shells
		Map<QueryIdentifier, Query> knownQueryShells = new HashMap<>();
		knownQueryShells.put( q0.getId(), q0 );
		knownQueryShells.put( q1.getId(), q1 );
		knownQueryShells.put( q2.getId(), q2 );
		knownQueryShells.put( q3.getId(), q3 );
		knownQueryShells.put( q4.getId(), q4 );
		knownQueryShells.put( q5.getId(), q5 );
		knownQueryShells.put( q6.getId(), q6 );
		knownQueryShells.put( q7.getId(), q7 );
		knownQueryShells.put( q8.getId(), q8 );
		knownQueryShells.put( q9.getId(), q9 );
		knownQueryShells.put( q10.getId(), q10 );

		// Find the graphs for the test
		List<Vectorizable> allTheGraphs = g.constructNewFDQs( q0.getId(), q0.getQueryString(), knownQueryShells );

		// Log everything
		g.printParameterMappings();
		logger.debug( "Generated {} graphs", allTheGraphs.size() );
		for( Vectorizable graph : allTheGraphs ) {
			logger.debug( "Vectorizable Id: {}", graph.getVectorizableId() );
			graph.getVectorizationDependencies().dumpToLog();
		}

		// Test our found graphs
		assertThat( allTheGraphs.size(), is( 5 ) );

		// Test graph 1
		List<QueryIdentifier> topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
            logger.warn( "vectorizable ID: {}", v.getVectorizableId() );
			if( v.getVectorizableId().getId() == 1028438711 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q10.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q0.getId() ) );

		// Test graph 2
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == 921615031 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q9.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q8.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q0.getId() ) );

		// Test graph 3
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == 489583320 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q7.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q0.getId() ) );

		// Test graph 4
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == -1003078225 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q6.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q5.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q1.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q0.getId() ) );

		// Test graph 5
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == 541474223 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q6.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q4.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q1.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q0.getId() ) );
	}

	@Test
	public void testMultipleDependencyGraphGenerationLoops() {
		Parameters.TRACKING_PERIOD = 1;
		Parameters.IS_UNIT_TEST = true;
		// Here we have a simple loop with 3 body queries two of which are in a dependency
		// relationship. There are additionally 3 trigger queries which connect to every query in
		// the loop body.

		// Setup test queries
		Query q0 = new Query( "SELECT stuff FROM T0 WHERE C0 = 0" );
		Query q1 = new Query( "SELECT stuff FROM T1 WHERE C0 = 0 AND C1 = 1" );
		Query q2 = new Query( "SELECT stuff FROM T2 WHERE C0 = 0" );
		Query q3 = new Query( "SELECT stuff FROM T3" ); // This one is the trigger
		Query q4 = new Query( "SELECT stuff FROM T4" ); // And this one is the trigger
		Query q5 = new Query( "SELECT stuff FROM T5" ); // And this one is the trigger

		// Print query id to query mappings
		logger.debug( "q0 = {}", q0.getId() );
		logger.debug( "q1 = {}", q1.getId() );
		logger.debug( "q2 = {}", q2.getId() );
		logger.debug( "q3 = {}", q3.getId() );
		logger.debug( "q4 = {}", q4.getId() );
		logger.debug( "q5 = {}", q5.getId() );

		// Setup the markov graph
		MarkovGraph g = new MarkovGraph( 0 );
		MarkovNode node0 = g.getOrAddNode( q0.getId() );
		node0.addQueryString( q0.getQueryString() );
		MarkovNode node1 = g.getOrAddNode( q1.getId() );
		node1.addQueryString( q1.getQueryString() );
		MarkovNode node2 = g.getOrAddNode( q2.getId() );
		node2.addQueryString( q2.getQueryString() );
		MarkovNode node3 = g.getOrAddNode( q3.getId() );
		node3.addQueryString( q3.getQueryString() );
		MarkovNode node4 = g.getOrAddNode( q4.getId() );
		node4.addQueryString( q4.getQueryString() );
		MarkovNode node5 = g.getOrAddNode( q5.getId() );
		node5.addQueryString( q5.getQueryString() );

		// Add traversals to the markov graph
		// SCC
		node0.addEdgeTraversal( node1 );
		node1.addEdgeTraversal( node2 );
		node2.addEdgeTraversal( node0 );
		// Transitions from first trigger
		node3.addEdgeTraversal( node0 );
		node3.addEdgeTraversal( node1 );
		node3.addEdgeTraversal( node2 );
		// Transitions from second trigger
		node4.addEdgeTraversal( node0 );
		node4.addEdgeTraversal( node1 );
		node4.addEdgeTraversal( node2 );
		// Transitions from third trigger
		node5.addEdgeTraversal( node0 );
		node5.addEdgeTraversal( node1 );
		node5.addEdgeTraversal( node2 );

		// Add mappings to the markov graph
		Multimap<Integer, String> q0Mappings = LinkedListMultimap.create();
		q0Mappings.put( 0, "stuff" );
		node0.addMappings( q1.getId(), q0Mappings, 0);

		// Mappings for first trigger
		Multimap<Integer, String> q3Mappings = LinkedListMultimap.create();
		q3Mappings.put( 0, "stuff" );
		node3.addMappings( q0.getId(), q3Mappings, 0);
		q3Mappings.clear();
		q3Mappings.put( 1, "stuff" );
		node3.addMappings( q1.getId(), q3Mappings, 0);
		q3Mappings.clear();
		q3Mappings.put( 0, "stuff" );
		node3.addMappings( q2.getId(), q3Mappings, 0);

		// Mappings for second trigger
		Multimap<Integer, String> q4Mappings = LinkedListMultimap.create();
		q4Mappings.put( 0, "stuff" );
		node4.addMappings( q0.getId(), q4Mappings, 0);
		q4Mappings.clear();
		q4Mappings.put( 1, "stuff" );
		node4.addMappings( q1.getId(), q4Mappings, 0);
		q4Mappings.clear();
		q4Mappings.put( 0, "stuff" );
		node4.addMappings( q2.getId(), q4Mappings, 0);

		// Mappings for third trigger
		Multimap<Integer, String> q5Mappings = LinkedListMultimap.create();
		q5Mappings.put( 0, "stuff" );
		node5.addMappings( q0.getId(), q5Mappings, 0);
		q5Mappings.clear();
		q5Mappings.put( 1, "stuff" );
		node5.addMappings( q1.getId(), q5Mappings, 0);
		q5Mappings.clear();
		q5Mappings.put( 0, "stuff" );
		node5.addMappings( q2.getId(), q5Mappings, 0);

		// Add everything to known query shells
		Map<QueryIdentifier, Query> knownQueryShells = new HashMap<>();
		knownQueryShells.put( q0.getId(), q0 );
		knownQueryShells.put( q1.getId(), q1 );
		knownQueryShells.put( q2.getId(), q2 );
		knownQueryShells.put( q3.getId(), q3 );
		knownQueryShells.put( q4.getId(), q4 );
		knownQueryShells.put( q5.getId(), q5 );

		// Find the graphs for the test
		List<Vectorizable> allTheGraphs = g.constructNewLoops( q0.getId(),  knownQueryShells );

		// Log everything
		g.printParameterMappings();
		logger.debug( "Generated {} graphs", allTheGraphs.size() );
		for( Vectorizable graph : allTheGraphs ) {
			logger.debug( "Vectorizable Id: {}", graph.getVectorizableId() );
			graph.getVectorizationDependencies().dumpToLog();
		}

		// Test our found graphs
		assertThat( allTheGraphs.size(), is( 3 ) );

		// Test graph 1
		List<QueryIdentifier> topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
            logger.warn( "Vectorizable ID: {}", v.getVectorizableId().getId() );
			if( v.getVectorizableId().getId() == -171135221 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q4.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q0.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q1.getId() ) );

		// Test graph 2
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == -1715671285 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q5.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q0.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q1.getId() ) );

		// Test graph 3
		topologicalOrder = null;
		for( Vectorizable v : allTheGraphs ) {
			if( v.getVectorizableId().getId() == -2095026421 ) {
				topologicalOrder = v.getVectorizationDependencies().getTopologicallyOrderedQueries();
			}
		}
		assertThat( topologicalOrder.get( 0 ), is( q3.getId() ) );
		assertThat( topologicalOrder.get( 1 ), is( q0.getId() ) );
		assertThat( topologicalOrder.get( 2 ), is( q2.getId() ) );
		assertThat( topologicalOrder.get( 3 ), is( q1.getId() ) );
	}
}
