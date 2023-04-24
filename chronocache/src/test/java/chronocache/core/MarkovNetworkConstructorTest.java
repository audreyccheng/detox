package chronocache.core;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.core.qry.QueryStream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Map;

public class MarkovNetworkConstructorTest {

	private VersionVector version = new VersionVector( new ArrayList<Long>() );

	@Test(timeout = 1000)
	public void testRespondsToStop() throws InterruptedException {
		QueryStream qryStream = new QueryStream();
		Duration delta = new Duration(1);
		MarkovConstructor constructor = new MarkovConstructor( 0, qryStream, delta );
		Thread t = new Thread(constructor);
		constructor.stop();
		t.start();
		t.join();
	}

	@Test(timeout = 3000)
	public void testNothingInStream() throws InterruptedException {
		QueryStream qryStream = new QueryStream();
		Duration delta = new Duration(1000);
		MarkovConstructor constructor = new MarkovConstructor( 0, qryStream, delta );
		Thread t = new Thread(constructor);
		t.start();
		Thread.sleep(500);
		constructor.stop();
		t.join();
	}

	@Test(timeout = 3000)
	public void testBlockUntilEnoughTimeHasElapsed() throws InterruptedException, ParseCancellationException {
		String query = "SELECT * FROM t";
		QueryStream qryStream = new QueryStream();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult parseResult = p.buildParseTree( query );
		qryStream.addQueryToStream(new ExecutedQuery(new Query(query, parseResult), DateTime.now(), 1000,
				new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		Duration delta = new Duration(1000);
		MarkovConstructor constructor = new MarkovConstructor( 0, qryStream, delta );
		Thread t = new Thread(constructor);
		DateTime startTime = DateTime.now();
		t.start();
		Thread.sleep(500);
		assertThat(t.isAlive(), equalTo(true));
		constructor.stop();
		t.join();
		assertThat(DateTime.now().isAfter(startTime.plus(new Duration(delta))), equalTo(true));
		assertThat(t.isAlive(), equalTo(false));
	}

	@Test(timeout = 3000000)
	public void testNetworkConstructionNoDuplicates() throws InterruptedException, ParseCancellationException {
		// Create a query stream of { q1, q2, q3, q1, q1 }
		String query1 = "SELECT * FROM t";
		String query2 = "SELECT * FROM abcd";
		String query3 = "SELECT * FROM efg";
		String query4 = "SELECT * FROM t";
		String query5 = "SELECT * FROM t";

		QueryStream qryStream = new QueryStream();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult t1 = p.buildParseTree(query1);
		AntlrParser.ParseResult t2 = p.buildParseTree(query2);
		AntlrParser.ParseResult t3 = p.buildParseTree(query3);
		AntlrParser.ParseResult t4 = p.buildParseTree(query4);
		AntlrParser.ParseResult t5 = p.buildParseTree(query5);

		Query q1 = new Query(query1, t1);
		Query q2 = new Query(query2, t2);
		Query q3 = new Query(query3, t3);
		Query q4 = new Query(query4, t4);
		Query q5 = new Query(query5, t5);

		// Sanity check that they have different ids, and thus different nodes
		assertThat(q1.getId(), not(equalTo(q2.getId())));
		assertThat(q1.getId(), not(equalTo(q3.getId())));
		assertThat(q2.getId(), not(equalTo(q3.getId())));

		// Assert that these have the same id
		assertThat(q1.getId(), equalTo(q4.getId()));
		assertThat(q1.getId(), equalTo(q5.getId()));

		qryStream.addQueryToStream( new ExecutedQuery(q1, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q2, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q3, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q4, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q2, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q3, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q5, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q2, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream( new ExecutedQuery(q3, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );

		// 500 ms delta, run algorithm, stop it
		Duration delta = new Duration(500);
		MarkovConstructor constructor = new MarkovConstructor( 0, qryStream, delta );
		Thread t = new Thread(constructor);
		t.start();
		Thread.sleep(1000);
		constructor.stop();
		t.join();

		// Ensure that thread is dead so we can safely access graph
		assertThat(t.isAlive(), equalTo(false));
		MarkovGraph graph = constructor.getGraph();
		MarkovNode node1 = graph.getOrAddNode(q1.getId());
		MarkovNode node2 = graph.getOrAddNode(q2.getId());
		MarkovNode node3 = graph.getOrAddNode(q3.getId());


		// A note about the 2/3:
		// The reason this happens is because for every instance of q_i (of which there are 3), they are followed by
		// each other q_j (i != j), except for the last iteration, in which case they are not followed by it.
		// So for the q1 example, q1 is followed by q2 and q3 in the query stream every time, hence the probability 1
		// But q1 is only followed by q1 2/3 of the time, because the last q1 in the stream does not have another q1 following it.


		//Forward edges q1 -> q1, q1 -> q2, q1 -> q3
		assertThat(node1.computeEdgeTraversalProbability(node1), equalTo((double) 2 / (double) 3));
		assertThat(node1.computeEdgeTraversalProbability(node2), equalTo(1.0));
		assertThat(node1.computeEdgeTraversalProbability(node3), equalTo(1.0));

		//Forward edges q2 -> q1, q2 -> q2, q2 -> q3
		assertThat(node2.computeEdgeTraversalProbability(node1), equalTo((double) 2 / (double) 3));
		assertThat(node2.computeEdgeTraversalProbability(node2), equalTo((double) 2 / (double) 3));
		assertThat(node2.computeEdgeTraversalProbability(node3), equalTo(1.0));

		//Forward edges q3 -> q1, q3 -> q2, q3 -> q3
		assertThat(node3.computeEdgeTraversalProbability(node1), equalTo((double) 2 / (double) 3));
		assertThat(node3.computeEdgeTraversalProbability(node2), equalTo((double) 2 / (double) 3));
		assertThat(node3.computeEdgeTraversalProbability(node3), equalTo((double) 2 / (double) 3));
	}

	@Test(timeout = 3000)
	public void testComplexStream() throws InterruptedException, ParseCancellationException {
		// Create a query stream of { q1, q2, q3 }
		String query1 = "SELECT * FROM t";
		String query2 = "SELECT * FROM abcd";
		String query3 = "SELECT * FROM efg";

		QueryStream qryStream = new QueryStream();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult t1 = p.buildParseTree(query1);
		AntlrParser.ParseResult t2 = p.buildParseTree(query2);
		AntlrParser.ParseResult t3 = p.buildParseTree(query3);
		Query q1 = new Query(query1, t1);
		Query q2 = new Query(query2, t2);
		Query q3 = new Query(query3, t3);

		// Sanity check that they have different ids, and thus different nodes
		assertThat(q1.getId(), not(equalTo(q2.getId())));
		assertThat(q1.getId(), not(equalTo(q3.getId())));
		assertThat(q2.getId(), not(equalTo(q3.getId())));
		qryStream.addQueryToStream( new ExecutedQuery(q1, DateTime.now(), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream(
				new ExecutedQuery(q2, DateTime.now().plus(new Duration(200)), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );
		qryStream.addQueryToStream(
				new ExecutedQuery(q3, DateTime.now().plus(new Duration(1000)), 1000, new QueryResult( new ArrayList<Map<String, Object>>(), version ) ) );

		// 500 ms delta, run algorithm, stop it
		Duration delta = new Duration(500);
		MarkovConstructor constructor = new MarkovConstructor( 0,qryStream, delta );
		Thread t = new Thread(constructor);
		t.start();
		Thread.sleep(1000);
		constructor.stop();
		t.join();

		// Ensure that thread is dead so we can safely access graph
		assertThat(t.isAlive(), equalTo(false));
		MarkovGraph graph = constructor.getGraph();
		MarkovNode node1 = graph.getOrAddNode(q1.getId());
		MarkovNode node2 = graph.getOrAddNode(q2.getId());
		MarkovNode node3 = graph.getOrAddNode(q3.getId());

		// Forward edges q1 -> q2
		assertThat(node1.computeEdgeTraversalProbability(node2), equalTo(1.0));

		// No edges otherwise
		assertThat(node1.computeEdgeTraversalProbability(node3), equalTo(0.0));
		assertThat(node2.computeEdgeTraversalProbability(node3), equalTo(0.0));
		assertThat(node1.computeEdgeTraversalProbability(node1), equalTo(0.0));
		assertThat(node2.computeEdgeTraversalProbability(node1), equalTo(0.0));
		assertThat(node2.computeEdgeTraversalProbability(node2), equalTo(0.0));
		assertThat(node3.computeEdgeTraversalProbability(node1), equalTo(0.0));
		assertThat(node3.computeEdgeTraversalProbability(node2), equalTo(0.0));
		assertThat(node3.computeEdgeTraversalProbability(node3), equalTo(0.0));
	}
}
