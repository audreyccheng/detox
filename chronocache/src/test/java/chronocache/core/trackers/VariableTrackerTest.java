package chronocache.core.trackers;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import chronocache.core.trackers.VariableTracker;
import chronocache.core.Parameters;
import chronocache.core.VersionVector;

public class VariableTrackerTest {

	public static String s = "[{\"W_TAX\":0.0977,\"W_YTD\":300000.00,\"W_NAME\":\"sxvnj\",\"W_STATE\":\"AQ\",\"W_ID\":1,\"W_STREET_1\":\"pdqdxvcrastvybc\""
			+ ",\"W_CITY\":\"jdgyluegqf\",\"W_STREET_2\":\"vmgnykrxvzxkgxtsp\",\"W_ZIP\":\"123456789\"},{\"W_TAX\":0.0576,\"W_YTD\":300000.00,\"W_NAME\":\"ocflj\","
			+ "\"W_STATE\":\"VX\",\"W_ID\":2,\"W_STREET_1\":\"epowfnsomyarh\",\"W_CITY\":\"wgsmzjgnlon\",\"W_STREET_2\":\"opufojhhdxehxjb\",\"W_ZIP\":\"123456789\"}]";

	public static QueryResult result;

	private static String query = "select a from b";
	private static String query2 = "select b from c WHERE b = 1";
	private static String insert = "insert into tab values (1)";

	@BeforeClass
	public static void initializeResultList() throws JsonParseException, JsonMappingException, IOException {
		List<Map<String,Object>> resultArr = new ObjectMapper().readValue(s, new TypeReference<List<Map<String, Object>>>() { } );
		result = new QueryResult( resultArr, new VersionVector( new ArrayList<Long>() ) );
	}

	/**
	 * Test that the variabletracker knows what it is tracking
	 *
	 * @throws ParseCancellationException
	 */
	@Test
	public void testSimpleTracking() throws ParseCancellationException {
		Parameters.TRACKING_PERIOD = 3;
		VariableTracker tracker = new VariableTracker();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult parseResult = p.buildParseTree(query);
		ExecutedQuery q = new ExecutedQuery( new Query(query, parseResult), DateTime.now(), 1000, result);
		tracker.startTracking(q, new LinkedList<QueryIdentifier>());
		assertThat(tracker.areTrackingQuery(q.getId()), equalTo(true));
		tracker.doneTracking(q);
		assertThat(tracker.getMappingsIfReady(q.getId()), nullValue());
		assertThat(tracker.areTrackingQuery(q.getId()), equalTo(false));
	}

	/**
	 * Test that the variable tracker knows to record related queries
	 *
	 * @throws ParseCancellationException
	 */
	@Test
	public void testKnowsToRecordRelatedQueries() throws ParseCancellationException {
		Parameters.TRACKING_PERIOD = 3;
		VariableTracker tracker = new VariableTracker();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult parseResult = p.buildParseTree(query);
		ExecutedQuery q = new ExecutedQuery(new Query(query, parseResult), DateTime.now(), 1000, result);
		List<QueryIdentifier> relatedQueries = new LinkedList<QueryIdentifier>();
		relatedQueries.add(new QueryIdentifier(1));
		relatedQueries.add(new QueryIdentifier(2));
		tracker.startTracking(q, relatedQueries);
		assertThat(tracker.areTrackingQuery(q.getId()), equalTo(true));
		assertThat(tracker.areRecordingQuery(new QueryIdentifier(1)), equalTo(true));
		assertThat(tracker.areRecordingQuery(new QueryIdentifier(2)), equalTo(true));
		assertThat(tracker.areRecordingQuery(q.getId()), equalTo(false));
		tracker.doneTracking(q);
		assertThat(tracker.getMappingsIfReady(q.getId()), nullValue());
		assertThat(tracker.areTrackingQuery(q.getId()), equalTo(false));
		assertThat(tracker.areRecordingQuery(new QueryIdentifier(1)), equalTo(false));
		assertThat(tracker.areRecordingQuery(new QueryIdentifier(2)), equalTo(false));
		assertThat(tracker.areRecordingQuery(q.getId()), equalTo(false));
	}

	@Test
	public void testRecordRelatedQuery() throws ParseCancellationException {
		Parameters.TRACKING_PERIOD = 3;
		VariableTracker tracker = new VariableTracker();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult parseResult = p.buildParseTree(query);
		ExecutedQuery q = new ExecutedQuery(new Query(query, parseResult), DateTime.now(), 1000, result);
		parseResult = p.buildParseTree(query2);
		ExecutedQuery q2 = new ExecutedQuery(new Query(query2, parseResult), DateTime.now(), 1000, result);
		List<QueryIdentifier> relatedQueries = new LinkedList<QueryIdentifier>();
		relatedQueries.add(q2.getId());
		tracker.startTracking(q, relatedQueries);
		tracker.recordVariables(q2);
		tracker.doneTracking(q);
		assertThat(tracker.getMappingsIfReady(q.getId()), nullValue());
	}

	@Test
	public void testHistoryManagerIntegrations() throws ParseCancellationException {
		Parameters.TRACKING_PERIOD = 3;
		VariableTracker tracker = new VariableTracker();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult parseResult = p.buildParseTree(query);
		ExecutedQuery q = new ExecutedQuery(new Query(query, parseResult), DateTime.now(), 1000, result);
		parseResult = p.buildParseTree(query2);
		ExecutedQuery q2 = new ExecutedQuery(new Query(query2, parseResult), DateTime.now(), 1000, result);
		List<QueryIdentifier> relatedQueries = new LinkedList<QueryIdentifier>();
		relatedQueries.add(q2.getId());
		tracker.startTracking(q, relatedQueries);
		tracker.recordVariables(q2);
		tracker.doneTracking(q);
		assertThat(tracker.getMappingsIfReady(q.getId()), nullValue());
		tracker.startTracking(q, relatedQueries);
		tracker.recordVariables(q2);
		tracker.doneTracking(q);
		assertThat(tracker.getMappingsIfReady(q.getId()), nullValue());
		tracker.startTracking(q, relatedQueries);
		tracker.recordVariables(q2);
		tracker.doneTracking(q);
		assertThat(tracker.getMappingsIfReady(q.getId()), not(nullValue()));
		Map<QueryIdentifier, Multimap<Integer, String>> pairings = tracker.getMappingsIfReady(q.getId());
		assertThat(pairings.size(), equalTo(1));
		assertThat(pairings.get(q2.getId()), not(nullValue()));
		assertThat(pairings.get(q2.getId()).size(), equalTo(1));
		assertThat(Iterables.get(pairings.get(q2.getId()).get(0), 0), equalTo("W_ID"));
	}

	@Test
	public void doNotTrackWriteQueries() throws ParseCancellationException {
		VariableTracker tracker = new VariableTracker();
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult parseResult = p.buildParseTree(insert);
		ExecutedQuery q = new ExecutedQuery( new Query(insert, parseResult), DateTime.now(), 1000, new QueryResult( new LinkedList<Map<String,Object>>(), new VersionVector( new ArrayList<Long>() ) ) );
		parseResult = p.buildParseTree(query2);
		ExecutedQuery q2 = new ExecutedQuery( new Query(query2, parseResult), DateTime.now(), 1000, result );

		assertThat( q.isReadQuery(), equalTo(false) );
	}

}
