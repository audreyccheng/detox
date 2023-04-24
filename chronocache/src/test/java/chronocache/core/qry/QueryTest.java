package chronocache.core.qry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import chronocache.core.parser.AntlrParser;

public class QueryTest {

	@Test
	public void testQueryHashCode() throws ParseCancellationException {
		String s = "SELECT * FROM t";
		AntlrParser p = new AntlrParser();
		AntlrParser.ParseResult result = p.buildParseTree(s);
		Query qry = new Query(s, result);
		assertThat(qry.getId().getId(), equalTo(p.getQueryHash(result.getParseTree())));
		Query q2 = new Query(qry);
		assertThat(q2.getId().getId(), equalTo(p.getQueryHash(result.getParseTree())));
	}

}
