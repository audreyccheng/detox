package chronocache.core.hashers;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.parser.AntlrParser;

public class AntlrSelectHasherTest {
    private Logger log = LoggerFactory.getLogger(this.getClass());

	private AntlrParser parser = new AntlrParser();

	private boolean isSameQueryHash(String q1, String q2) {
		AntlrParser.ParseResult parseResult = parser.buildParseTree( q1 );
		long h1 = parser.getQueryHash( parseResult.getParseTree() );
		parseResult = parser.buildParseTree( q2 );
		long h2 = parser.getQueryHash( parseResult.getParseTree() );

		return (h1 == h2);
	}

	@Test
	public void testSimpleHash() {
		String sql = "select a from b";
		String sql2 = "select b from c";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testHashDifferentColumnSameTable() {
		String sql = "select a from c";
		String sql2 = "select b from c";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testHashDifferentNumberOfColumnsSameTable() {
		String sql = "select a,b from c";
		String sql2 = "select b from c";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testSameQuerySameHash() {
		String sql = "select b from c";
		String sql2 = "select b from c";
		assertTrue(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testDifferentConstantsSameHash() {
		String sql = "select b from c where b = 3";
		String sql2 = "select b from c where b = 2";
		assertTrue(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testAdditionalPredicatesDifferentHash() {
		String sql = "select b from c where b = 3 AND b = 1";
		String sql2 = "select b from c where b = 2";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testCaseDoesntMatter() {
		String sql = "SELECT b FROM c WHERE b = 3 AND b = 1";
		String sql2 = "select b from c where b = 2 and b = 1";
		assertTrue( isSameQueryHash(sql, sql2));
	}

	@Test
	public void testDifferentPredicateOperators() {
		String sql = "SELECT b FROM c WHERE b = 3 OR b = 1";
		String sql2 = "select b from c where b = 2 and b = 1";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testGroupBy() {
		String sql = "SELECT b FROM c GROUP BY b";
		String sql2 = "SELECT b FROM c GROUP BY c";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testSameGroupDifferentHavingAggregate() {
		String sql = "SELECT b FROM c GROUP BY b HAVING(COUNT(b)=1)";
		String sql2 = "SELECT b FROM c GROUP BY b HAVING(MAX(b)=1)";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testSameGroupDifferentHavingSameAggregateDifferentConst() {
		String sql = "SELECT b FROM c GROUP BY b HAVING(COUNT(b)=1)";
		String sql2 = "SELECT b FROM c GROUP BY b HAVING(COUNT(b)=2)";
		assertTrue(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testSubQueryPredicate() {
		String sql = "SELECT b FROM c WHERE b in (SELECT c FROM d WHERE c = 1)";
		String sql2 = "SELECT b FROM c WHERE b in (SELECT c FROM d where c = 2)";
		assertTrue(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testCrossJoin() {
		String sql = "SELECT b FROM c,d";
		String sql2 = "SELECT b FROM c,e";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testInnerJoin() {
		String sql = "SELECT b FROM c INNER JOIN d";
		String sql2 = "SELECT b FROM c,d";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testJoinConditions() {
		String sql = "SELECT b FROM c INNER JOIN d ON c.col = d.col2";
		String sql2 = "SELECT b FROM c JOIN d ON c.col = d.col1";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testMathJoinConditions() {
		String sql = "SELECT b FROM c INNER JOIN d ON c.col = d.col2 - 1";
		String sql2 = "SELECT b FROM c INNER JOIN d ON c.col = d.col2 - 3";
		assertTrue(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testSelectListConstants() {
		String sql = "SELECT 1";
		String sql2 = "SELECT 2";
		assertFalse(isSameQueryHash(sql, sql2));
	}

	@Test
	public void testSelectLimit() {
		String sql = "SELECT * FROM t LIMIT 1";
		String sql2 = "SELECT * FROM t LIMIT 2";
		assertFalse(isSameQueryHash(sql, sql2));
	}


	@Test
	public void testOrderGroup() {
		String sql = "SELECT * FROM t GROUP BY 1 ORDER BY 1";
		String sql2 = "SELECT * FROM t GROUP BY 1 ORDER BY 2";
		String sql3 = "SELECT * FROM t GROUP BY 2 ORDER BY 1";
		assertFalse(isSameQueryHash(sql, sql2));
		assertFalse(isSameQueryHash(sql, sql3));
		assertFalse(isSameQueryHash(sql2, sql3));

	}

}
