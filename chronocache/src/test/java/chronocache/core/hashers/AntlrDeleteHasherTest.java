package chronocache.core.hashers;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

import chronocache.core.parser.AntlrParser;

public class AntlrDeleteHasherTest {
	private AntlrParser parser = new AntlrParser();

	private boolean isSameQueryHash(String q1, String q2) {
		AntlrParser.ParseResult parseResult = parser.buildParseTree( q1 );
		long h1 = parser.getQueryHash( parseResult.getParseTree() );
		parseResult = parser.buildParseTree( q2 );
		long h2 = parser.getQueryHash( parseResult.getParseTree() );

		return (h1 == h2);
	}

	@Test
	public void simpleDeleteHashTest() {
		String stmt = "DELETE FROM tab WHERE col1 = 1";
		String stmt2 = "DELETE FROM tab WHERE col2 = 1";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void differentTableHash() {
		String stmt = "DELETE FROM tab WHERE col1 = 1";
		String stmt2 = "DELETE FROM tab2 WHERE col1 = 1";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void sameStatementSameHash() {
		String stmt = "DELETE FROM tab WHERE col1 = 1";
		String stmt2 = "DELETE FROM tab WHERE col1 = 1";
		assertTrue(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void differentConstantSameHash() {
		String stmt = "DELETE FROM tab WHERE col1 = 1";
		String stmt2 = "DELETE FROM tab WHERE col1 = 2";
		assertTrue(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void noWhereCriteriaHash() {
		String stmt = "DELETE FROM tab";
		String stmt2 = "DELETE FROM tab WHERE col1 = 2";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}
}
