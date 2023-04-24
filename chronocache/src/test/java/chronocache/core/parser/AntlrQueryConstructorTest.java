package chronocache.core.parser;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.lang.IllegalArgumentException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.parser.AntlrParser;

public class AntlrQueryConstructorTest {
	private AntlrParser parser = new AntlrParser();
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private boolean checkConstructor( String stmt, String expected, List<String> newConsts ) {
		AntlrParser.ParseResult result = parser.buildParseTree( stmt );
		String constructed = parser.replaceQueryShellConstants( result.getParseTree(), newConsts, false );
		log.trace( "given {}, constants {}, expected {}, got {}",
				stmt, newConsts, expected, constructed );
		return expected.equals( constructed );
	}

	private boolean checkReplacement( String query, String queryExpected, String conditionExpected, List<String> replacements ) {
		AntlrParser.ParseResult result = parser.buildParseTree( query );
		String replacedString = parser.replaceQueryShellConstants( result.getParseTree(), replacements, false );
		AntlrParser.ExtractedConditions ec = parser.getCTEJoinConditions( result.getParseTree(), replacements, true );
		List<String> uncontainedConditions = ec.getUncontainedConditions();
		StringBuilder sb = new StringBuilder();
		assertFalse( uncontainedConditions.isEmpty() );
		Iterator<String> uncontainedConditionsIterator = uncontainedConditions.iterator();
		sb.append( "WHERE" );
		sb.append( uncontainedConditionsIterator.next() );
		while( uncontainedConditionsIterator.hasNext() ) {
			sb.append( " AND" );
			sb.append( uncontainedConditionsIterator.next() );
		}
		String newConditionString = sb.toString();
		assertThat( queryExpected, equalTo( replacedString ) );
		assertThat( conditionExpected, equalTo( newConditionString ) );
		return queryExpected.equals( replacedString );
	}

	@Test
	public void simpleDeparserTest() {
		String stmt = "SELECT a FROM b WHERE a = 3";
		String expModStmt = "SELECT a FROM b WHERE a = 5";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("5");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}

	@Test
	public void orderedVariableDeparserTest() {
		String stmt = "SELECT a FROM b WHERE a = 1 AND c = 2";
		String expModStmt = "SELECT a FROM b WHERE a = 3 AND c = 4";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("3");
		newConsts.add("4");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}

	@Test
	public void doubleVariableModificationTest() {
		String stmt = "SELECT a FROM b WHERE a = 0.8 AND c = 0.4";
		String expModStmt = "SELECT a FROM b WHERE a = 1.7 AND c = 22.3";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("1.7");
		newConsts.add("22.3");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}

	@Test
	public void simpleSignedReplacement() {
		String stmt = "SELECT a FROM b WHERE a = -0.8";
		String expModStmt = "SELECT a FROM b WHERE a = 1.7";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("1.7");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}

	@Test
	public void multipleSignedReplacement() {
		String stmt = "SELECT a FROM b WHERE a = -0.8 - +1.2";
		String expModStmt = "SELECT a FROM b WHERE a = 1.7 - 3";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("1.7");
		newConsts.add("3");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}

	@Test
	public void multipleNegativeSignedReplacement() {
		String stmt = "SELECT a FROM b WHERE a = -0.8 - -1.2";
		String expModStmt = "SELECT a FROM b WHERE a = 1.7 - 3";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("1.7");
		newConsts.add("3");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}

	@Test
	public void replaceWithNegative() {
		String stmt = "SELECT a FROM b WHERE a = 4";
		String expModStmt = "SELECT a FROM b WHERE a = -5";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("-5");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}


	@Test
	public void noReplaceSelectList() {
		String stmt = "SELECT 1 FROM b WHERE col = 3";
		String expModStmt = "SELECT 1 FROM b WHERE col = 5";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("5");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));
	}

	@Test
	public void noReplaceLimit() {
		String stmt = "SELECT id FROM (SELECT id FROM t LIMIT 1 ORDER BY id DESC) WHERE id = 1";
		String expModStmt = "SELECT id FROM (SELECT id FROM t LIMIT 1 ORDER BY id DESC) WHERE id = 5";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("5");
		assertTrue(checkConstructor(stmt, expModStmt, newConsts));

	}

	@Test( expected = IllegalArgumentException.class)
	public void tooFewConstants() {
		String stmt = "SELECT id FROM tab WHERE id = 1";
		List<String> newConsts = new LinkedList<>();
		assertNull(parser.replaceQueryShellConstants(parser.buildParseTree( stmt ).getParseTree(), newConsts, false));
	}

	@Test
	public void tooManyConstants() {
		String stmt = "SELECT id FROM tab WHERE id = 1";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("2");
		newConsts.add("3");
		assertThat( parser.replaceQueryShellConstants(parser.buildParseTree( stmt ).getParseTree(), newConsts, false ), equalTo( "SELECT id FROM tab WHERE id = 2"));
	}

	@Test
	public void stringConstant() {
		String stmt = "SELECT id FROM tab WHERE id = '1'";
		String expModStmt = "SELECT id FROM tab WHERE id = '2'";
		List<String> newConsts = new LinkedList<>();
		newConsts.add("2");
		assertTrue( checkConstructor( stmt, expModStmt, newConsts ) );
	}

	// Start of new tests

	@Test
	public void simpleVerbatimDeparserTest() {
		String query = "SELECT a FROM b WHERE a = 3";
		String updatedQuery = "SELECT a FROM b WHERE a = c";
		String updatedCondition = "WHERE QUALIFYME0.a = QUALIFYME1.c";
		List<String> replacements = new LinkedList<>();
		replacements.add( "c" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void orderedVariableVerbatimDeparserTest() {
		String query = "SELECT a FROM b WHERE a = 1 AND c = 2";
		String updatedQuery = "SELECT a FROM b WHERE a = repOne AND c = repTwo";
		String updatedCondition = "WHERE QUALIFYME0.a = QUALIFYME1.repOne AND QUALIFYME0.c = QUALIFYME1.repTwo";
		List<String> replacements = new LinkedList<>();
		replacements.add( "repOne" );
		replacements.add( "repTwo" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void doubleVariableVerbatimModificationTest() {
		String query = "SELECT a FROM b WHERE a = 0.8 AND c = 0.4";
		String updatedQuery = "SELECT a FROM b WHERE a = one AND c = two";
		String updatedCondition = "WHERE QUALIFYME0.a = QUALIFYME1.one AND QUALIFYME0.c = QUALIFYME1.two";
		List<String> replacements = new LinkedList<>();
		replacements.add( "one" );
		replacements.add( "two" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void simpleVerbatimSignedReplacement() {
		String query = "SELECT a FROM b WHERE a = -0.8";
		String updatedQuery = "SELECT a FROM b WHERE a = c";
		String updatedCondition = "WHERE QUALIFYME0.a = QUALIFYME1.c";
		List<String> replacements = new LinkedList<>();
		replacements.add( "c" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void multipleVerbatimSignedReplacement() {
		String query = "SELECT a FROM b WHERE a = -0.8 - +1.2";
		String updatedQuery = "SELECT a FROM b WHERE a = c - d";
		String updatedCondition = "WHERE QUALIFYME0.a = QUALIFYME1.c - QUALIFYME1.d";
		List<String> replacements = new LinkedList<>();
		replacements.add( "c" );
		replacements.add( "d" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void multipleVerbatimNegativeSignedReplacement() {
		String query = "SELECT a FROM b WHERE a = -0.8 - -1.2";
		String updatedQuery = "SELECT a FROM b WHERE a = c - d";
		String updatedCondition = "WHERE QUALIFYME0.a = QUALIFYME1.c - QUALIFYME1.d";
		List<String> replacements = new LinkedList<>();
		replacements.add( "c" );
		replacements.add( "d" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void noReplaceSelectListVerbatim() {
		String query = "SELECT 1 FROM b WHERE col = 3";
		String updatedQuery = "SELECT 1 FROM b WHERE col = c";
		String updatedCondition = "WHERE QUALIFYME0.col = QUALIFYME1.c";
		List<String> replacements = new LinkedList<>();
		replacements.add( "c" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void noReplaceLimitVerbatim() {
		String query = "SELECT id FROM c WHERE id = 1 LIMIT 10";
		String updatedQuery = "SELECT id FROM c WHERE id = a LIMIT 10";
		String updatedCondition = "WHERE QUALIFYME0.id = QUALIFYME1.a";
		List<String> replacements = new LinkedList<>();
		replacements.add( "a" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

	@Test
	public void noReplaceLimitTwoVerbatim() {
		String query = "SELECT id FROM c WHERE id = 1 LIMIT 10";
		String updatedQuery = "SELECT id FROM c WHERE id = a LIMIT 10";
		List<String> replacements = new LinkedList<>();
		replacements.add( "a" );
		replacements.add( "b" );
		assertThat( parser.replaceQueryShellConstants( parser.buildParseTree( query ).getParseTree(), replacements, false ), equalTo( updatedQuery ) );
	}

	@Test
	public void tooManyConstantsReplacement() {
		String query = "SELECT id FROM tab WHERE id = 1";
		String updatedQuery = "SELECT id FROM tab WHERE id = c";
		String updatedCondition = "WHERE QUALIFYME0.id = QUALIFYME1.c";
		List<String> replacements = new LinkedList<>();
		replacements.add( "c" );
		replacements.add( "d" );
		assertThat( parser.replaceQueryShellConstants( parser.buildParseTree( query ).getParseTree(), replacements, false ), equalTo( updatedQuery ) );
	}

	@Test
	public void stringConstantVerbatim() {
		String query = "SELECT id FROM tab WHERE id = '1'";
		String updatedQuery = "SELECT id FROM tab WHERE id = 'replaced'";
		String updatedCondition = "WHERE QUALIFYME0.id = QUALIFYME1.replaced";
		List<String> replacements = new LinkedList<>();
		replacements.add( "replaced" );
		assertTrue( checkReplacement( query, updatedQuery, updatedCondition, replacements ) );
	}

}
