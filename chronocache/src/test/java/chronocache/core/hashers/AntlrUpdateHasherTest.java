package chronocache.core.hashers;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

import chronocache.core.parser.AntlrParser;

public class AntlrUpdateHasherTest {
	private AntlrParser parser = new AntlrParser();

	private boolean isSameQueryHash(String q1, String q2) {
		AntlrParser.ParseResult parseResult = parser.buildParseTree( q1 );
		long h1 = parser.getQueryHash( parseResult.getParseTree() );
		parseResult = parser.buildParseTree( q2 );
		long h2 = parser.getQueryHash( parseResult.getParseTree() );

		return (h1 == h2);
	}

	@Test
	public void testSimpleUpdateHash() {
		String stmt = "UPDATE tab SET col1 = 1 WHERE col2 = 'a'";
		String stmt2 = "UPDATE tab SET col2 = 1 WHERE col1 = 'a'";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void testSameStringSameHash() {
		String stmt = "UPDATE tab SET col1 = 1 WHERE col2 = 'a'";
		String stmt2 = "UPDATE tab SET col1 = 1 WHERE col2 = 'a'";
		assertTrue(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void testDifferentPredicates() {
		String stmt = "UPDATE tab SET col1 = 1 WHERE col1 = 'a'";
		String stmt2 = "UPDATE tab SET col1 = 1 WHERE col1 = 'b'";
		assertTrue(isSameQueryHash(stmt, stmt2));
	}

	@Test
	public void testDifferentSet() {
		String stmt = "UPDATE tab SET col1 = 1 WHERE col1 = 'a'";
		String stmt2 = "UPDATE tab SET col1 = 2 WHERE col1 = 'a'";
		assertFalse(isSameQueryHash(stmt, stmt2));
	}

}
