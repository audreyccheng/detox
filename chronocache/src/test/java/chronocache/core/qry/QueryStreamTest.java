package chronocache.core.qry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import chronocache.core.VersionVector;
import chronocache.core.parser.AntlrParser;

//TODO add concurrency tests after adding locks
public class QueryStreamTest {

	@Test
	public void testSimpleAdd() throws ParseCancellationException {
		QueryStream strm = new QueryStream();
		strm.addQueryToStream(createExecutedQuery());
		assertThat(strm.getQueryStreamSize(), equalTo(1));
	}

	@Test
	public void testSimpleTailAdd() throws ParseCancellationException {
		QueryStream strm = new QueryStream();
		strm.addQueryToTail(createExecutedQuery());
		assertThat(strm.getQueryStreamTailSize(), equalTo(1));
	}

	@Test
	public void testMultipleAdd() throws ParseCancellationException {
		QueryStream strm = new QueryStream();
		strm.addQueryToStream(createExecutedQuery());
		strm.addQueryToStream(createExecutedQuery());
		strm.addQueryToStream(createExecutedQuery());
		assertThat(strm.getQueryStreamSize(), equalTo(3));
	}

	@Test
	public void testMultipleTailAdd() throws ParseCancellationException {
		QueryStream strm = new QueryStream();
		strm.addQueryToTail(createExecutedQuery());
		strm.addQueryToTail(createExecutedQuery());
		strm.addQueryToTail(createExecutedQuery());
		assertThat(strm.getQueryStreamTailSize(), equalTo(3));
	}

	@Test
	public void testPeek() throws ParseCancellationException {
		QueryStream strm = new QueryStream();
		strm.addQueryToStream(createExecutedQuery());
		strm.addQueryToStream(createExecutedQuery());
		strm.addQueryToStream(createExecutedQuery());
		assertThat(strm.getQueryStreamSize(), equalTo(3));
		ExecutedQuery exec = strm.peek();
		assertThat(strm.getQueryStreamSize(), equalTo(3));
	}

	@Test
	public void testTailPeek() throws ParseCancellationException {
		QueryStream strm = new QueryStream();
		strm.addQueryToTail(createExecutedQuery());
		strm.addQueryToTail(createExecutedQuery());
		strm.addQueryToTail(createExecutedQuery());
		assertThat(strm.getQueryStreamTailSize(), equalTo(3));
		ExecutedQuery exec = strm.tailPeek();
		assertThat(strm.getQueryStreamTailSize(), equalTo(3));
	}

	@Test
	public void testPop() throws ParseCancellationException {
		QueryStream strm = new QueryStream();
		strm.addQueryToStream(createExecutedQuery());
		strm.addQueryToStream(createExecutedQuery());
		strm.addQueryToStream(createExecutedQuery());
		assertThat(strm.getQueryStreamSize(), equalTo(3));
		ExecutedQuery exec = strm.pop();
		assertThat(strm.getQueryStreamSize(), equalTo(2));
		assertThat(strm.getQueryStreamTailSize(), equalTo(1));
	}

	@Test
	public void testTailPop() throws ParseCancellationException	{
		QueryStream strm = new QueryStream();
		strm.addQueryToTail(createExecutedQuery());
		strm.addQueryToTail(createExecutedQuery());
		strm.addQueryToTail(createExecutedQuery());
		assertThat(strm.getQueryStreamTailSize(), equalTo(3));
		ExecutedQuery exec = strm.tailPop();
		assertThat(strm.getQueryStreamSize(), equalTo(0));
		assertThat(strm.getQueryStreamTailSize(), equalTo(2));
	}

	private ExecutedQuery createExecutedQuery() throws ParseCancellationException {
		String query = "SELECT * FROM t";
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult parseResult = p.buildParseTree(query);
		return new ExecutedQuery(new Query(query, parseResult), DateTime.now(), 1000,
				new QueryResult( new ArrayList<Map<String, Object>>(), new VersionVector( new ArrayList<Long>() ) ) );
	}
}
