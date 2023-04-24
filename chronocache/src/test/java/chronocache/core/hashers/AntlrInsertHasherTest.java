package chronocache.core.hashers;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

import chronocache.core.parser.AntlrParser;

public class AntlrInsertHasherTest {
	private AntlrParser parser = new AntlrParser();

	private boolean isSameQueryHash(String q1, String q2) {
		AntlrParser.ParseResult parseResult = parser.buildParseTree( q1 );
		long h1 = parser.getQueryHash( parseResult.getParseTree() );
		parseResult = parser.buildParseTree( q2 );
		long h2 = parser.getQueryHash( parseResult.getParseTree() );

		return (h1 == h2);
	}

	@Test
	public void testHashSimpleInsert() {
		String stmt = "INSERT INTO a VALUES (1)";
		String stmt2 = "INSERT INTO b VALUES (1)";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void testHashSameInsert() {
		String stmt = "INSERT INTO a VALUES (1)";
		String stmt2 = "INSERT INTO a VALUES (1)";
		assertTrue(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void testHashDifferentNumberOfParams() {
		String stmt = "INSERT INTO a VALUES (1)";
		String stmt2 = "INSERT INTO a VALUES (1,2)";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void testHashDifferentParamsSameHash() {
		String stmt = "INSERT INTO a VALUES (1)";
		String stmt2 = "INSERT INTO a VALUES (2)";
		assertTrue(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void testHashNestedSelect() {
		String stmt = "INSERT INTO table2 SELECT * from table1";
		String stmt2 = "INSERT INTO table2 SELECT * from table3";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}
}
