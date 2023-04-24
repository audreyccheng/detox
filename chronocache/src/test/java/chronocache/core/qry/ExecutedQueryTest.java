package chronocache.core.qry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import chronocache.core.parser.AntlrParser;
import chronocache.core.VersionVector;

public class ExecutedQueryTest {

	@Test
	public void testFieldRetrieval() throws ParseCancellationException {
		String s = "SELECT * FROM t";
		AntlrParser p = new AntlrParser();
		DateTime dt = DateTime.now();
		AntlrParser.ParseResult parseResult = p.buildParseTree(s);
		ExecutedQuery qry = new ExecutedQuery(new Query(s, parseResult), dt,
				1000, new QueryResult( new ArrayList<Map<String, Object>>(), new VersionVector( new ArrayList<Long>() ) ) );
		assertThat(qry.getId().getId(), equalTo(p.getQueryHash(parseResult.getParseTree())));
		assertThat(qry.getExecutionTime(), equalTo(dt));
	}

}
