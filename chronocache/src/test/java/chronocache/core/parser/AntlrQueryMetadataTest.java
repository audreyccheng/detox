package chronocache.core.parser;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.AntlrQueryType;
import chronocache.core.parser.AntlrQueryMetadata;

public class AntlrQueryMetadataTest {
	private AntlrParser parser = new AntlrParser();
	private Logger log = LoggerFactory.getLogger(this.getClass());


	/**
	 * Returns true if the expected and actual lists are equal
	 * @param: expected
	 * @param: actual
	 **/
	private boolean checkList(List<String> expected, List<String> actual) {
		log.trace("expected: {}, got: {}", expected, actual);
		if (expected.size() != actual.size()) {
			return false;
		}

		for (int i = 0; i < expected.size(); i++) {
			if (!expected.get(i).equals(actual.get(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns true if the expected and actual sets are equal
	 * @param: expected
	 * @param: actual
	 **/
	private boolean checkSet(Set<String> expected, Set<String> actual) {
		log.trace("expected: {}, got: {}", expected, actual);
		if (expected.size() != actual.size()) {
			return false;
		}

		for (String expectedElem : expected) {
			if (!actual.contains(expectedElem)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * asserts that the query metadata for query, has the same expectedQueryType,
	 * expectedConstants, and expectedTables
	 * @param: query
	 * @param: expectedQueryType
	 * @param: expectedConstants
	 * @param: expectedTables
	 **/
	private void checkQueryMetadata(String query, AntlrQueryType expectedQueryType,
			List<String> expectedConstants, Set<String> expectedTables) {
		log.trace("checking: {}", query);
		AntlrParser.ParseResult parseResult = parser.buildParseTree( query );
		AntlrQueryMetadata metadata = parser.getQueryMetadata(parseResult.getParseTree());
		assertEquals(expectedQueryType, metadata.getQueryType());
		assertTrue(checkList(expectedConstants, metadata.getConstantsFromQuery()));
		assertTrue(checkSet(expectedTables, metadata.getTablesInQuery()));
	}

	// Overload of checkQueryMetadata to additionally check columns, aliases, and where clause
	private void checkQueryMetadata(String query,
		AntlrQueryType expectedQueryType,
		List<String> expectedConstants,
		Set<String> expectedTables,
		List<String> expectedAliases,
		List<String> expectedSelectedColumns,
		List<String> expectedConditionalColumns )
	{
		log.trace("checking: {}", query);
		AntlrParser.ParseResult parseResult = parser.buildParseTree( query );
		AntlrQueryMetadata metadata = parser.getQueryMetadata(parseResult.getParseTree());
		assertEquals(expectedQueryType, metadata.getQueryType());
		assertTrue(checkList(expectedConstants, metadata.getConstantsFromQuery()));
		assertTrue(checkSet(expectedTables, metadata.getTablesInQuery()));
		assertTrue(checkList(expectedAliases, metadata.getAliasesInQuery()));
		assertTrue(checkList(expectedSelectedColumns, metadata.getSelectedColumnsInQuery()));
		assertTrue(checkList(expectedConditionalColumns, metadata.getUncontainedConditionalColumnsInQuery()));
	}

	@Test
	public void simpleTest() {
		String stmt = "SELECT a FROM b WHERE a = 3";

		List<String> consts = new LinkedList<>();
		consts.add("3");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void orderedVariableDeparserTest() {
		String stmt = "SELECT a FROM b WHERE a = 1 AND c = 2";

		List<String> consts = new LinkedList<>();
		consts.add("1");
		consts.add("2");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void doubleVariableModificationTest() {
		String stmt = "SELECT a FROM b WHERE a = 0.8 AND c = 0.4";

		List<String> consts = new LinkedList<>();
		consts.add("0.8");
		consts.add("0.4");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void simpleSignedReplacement() {
		String stmt = "SELECT a FROM b WHERE a = -0.8";

		List<String> consts = new LinkedList<>();
		consts.add("-0.8");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void multipleSignedReplacement() {
		String stmt = "SELECT a FROM b WHERE a = -0.8 - +1.2";

		List<String> consts = new LinkedList<>();
		consts.add("-0.8");
		consts.add("+1.2");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void multipleNegativeSignedReplacement() {
		String stmt = "SELECT a FROM b WHERE a = -0.8 - -1.2";

		List<String> consts = new LinkedList<>();
		consts.add("-0.8");
		consts.add("-1.2");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void testMathJoinReplaced() {
		String stmt = "SELECT b FROM c INNER JOIN d ON c.col = d.col2 - 1";

		List<String> consts = new LinkedList<>();
		consts.add("1");

		Set<String> tables = new HashSet<>();
		tables.add("c");
		tables.add("d");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void replaceWithNegative() {
		String stmt = "SELECT a FROM b WHERE a = 4";

		List<String> consts = new LinkedList<>();
		consts.add("4");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void complicatedReplacementOrdering() {
		String stmt = "SELECT a FROM b INNER JOIN c ON c.a = 4 WHERE a = 4 AND EXISTS (SELECT * FROM d WHERE d.col = 7)";

		List<String> consts = new LinkedList<>();
		consts.add("4");
		consts.add("4");
		consts.add("7");

		Set<String> tables = new HashSet<>();
		tables.add("b");
		tables.add("c");
		tables.add("d");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void noReplaceSelectList() {
		String stmt = "SELECT 1 FROM b WHERE col = 3";

		List<String> consts = new LinkedList<>();
		consts.add("3");

		Set<String> tables = new HashSet<>();
		tables.add("b");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void noTableOrConsts() {
		String stmt = "SELECT 1";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void noLimitConst() {
		String stmt = "SELECT id FROM (SELECT id FROM t LIMIT 1 ORDER BY id DESC) WHERE id = 5";

		List<String> consts = new LinkedList<>();
		consts.add("5");

		Set<String> tables = new HashSet<>();
		tables.add("t");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void noConstsInOrderGroup() {
		String stmt = "SELECT * FROM t GROUP BY 2 ORDER BY 1";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("t");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void simpleDelete() {
		String stmt = "DELETE FROM tab WHERE col1 = 1";

		List<String> consts = new LinkedList<>();
		consts.add("1");

		Set<String> tables = new HashSet<>();
		tables.add("tab");

		checkQueryMetadata(stmt, AntlrQueryType.DELETE, consts, tables);

	}

	@Test
	public void noWhereCriteria() {
		String stmt = "DELETE FROM tab";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("tab");

		checkQueryMetadata(stmt, AntlrQueryType.DELETE, consts, tables);

	}

	@Test
	public void testSimpleUpdate() {
		String stmt = "UPDATE tab SET col1 = 1 WHERE col2 = 'a'";

		List<String> consts = new LinkedList<>();
		consts.add("'a'");

		Set<String> tables = new HashSet<>();
		tables.add("tab");

		checkQueryMetadata(stmt, AntlrQueryType.UPDATE, consts, tables);
	}

	@Test
	public void testHashComplicatedUpdate() {
		String stmt = "UPDATE u SET u.assid = s.assid"; //"FROM ud u INNER JOIN sale s ON u.id = s.udid";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("u");

		checkQueryMetadata(stmt, AntlrQueryType.UPDATE, consts, tables);
	}

	@Test
	public void testSimpleInsert() {
		String stmt = "INSERT INTO a VALUES (1)";

		List<String> consts = new LinkedList<>();
		consts.add("1");

		Set<String> tables = new HashSet<>();
		tables.add("a");

		checkQueryMetadata(stmt, AntlrQueryType.INSERT, consts, tables);
	}

	@Test
	public void testDifferentNumberOfParams() {
		String stmt = "INSERT INTO a VALUES (1,2)";

		List<String> consts = new LinkedList<>();
		consts.add("1");
		consts.add("2");

		Set<String> tables = new HashSet<>();
		tables.add("a");

		checkQueryMetadata(stmt, AntlrQueryType.INSERT, consts, tables);
	}

	@Test
	public void testNestedSelect() {
		String stmt = "INSERT INTO table2 SELECT * from table1";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("table2");
		tables.add("table1");

		checkQueryMetadata(stmt, AntlrQueryType.INSERT, consts, tables);
	}

	@Test
	public void testSoundex() {
		String stmt = "SELECT * FROM item, author WHERE item.i_a_id = author.a_id AND substring(soundex(item.i_title),0,4)=substring(soundex('BABABABABAOGNG'),0,4) ORDER BY item.i_title limit 50";
		Set<String> tables = new HashSet<>();
		tables.add("item");
		tables.add("author");

		List<String> consts = new LinkedList<>();
		consts.add("'BABABABABAOGNG'");

		checkQueryMetadata(stmt, AntlrQueryType.SELECT, consts, tables);
	}

	@Test
	public void testSimpleAliases() {
		String query = "SELECT columnOne AS columnTwo, columnThree AS columnFour FROM books";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();
		aliases.add("columnTwo");
		aliases.add("columnFour");

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");
		selectedColumns.add("columnThree");

		List<String> conditionalColumns = new LinkedList<>();


		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	}

	@Test
	public void testConstantAliasedColumn() {
		String query = "SELECT ' 'AS columnTwo, columnThree AS columnFour FROM books";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();
		aliases.add("columnTwo");
		aliases.add("columnFour");

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnTwo");
		selectedColumns.add("columnThree");

		List<String> conditionalColumns = new LinkedList<>();


		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	
	}

	@Test
	public void testAliasesUsed() {
		String query = "SELECT columnOne AS columnTwo FROM books WHERE columnOne = columnTwo";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();
		aliases.add("columnTwo");

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");

		List<String> conditionalColumns = new LinkedList<>();

		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	}

	@Test
	public void testConditionalColumns() {
		String query = "SELECT columnOne FROM books WHERE columnTwo = 7";

		List<String> consts = new LinkedList<>();
		consts.add("7");

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");

		List<String> conditionalColumns = new LinkedList<>();
		conditionalColumns.add("columnTwo");

		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	}

	@Test
	public void testColumnsOneOfEach() {
		String query = "SELECT columnOne AS columnThree FROM books WHERE columnTwo = 3";

		List<String> consts = new LinkedList<>();
		consts.add( "3" );

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();
		aliases.add("columnThree");

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");

		List<String> conditionalColumns = new LinkedList<>();
		conditionalColumns.add("columnTwo");

		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	}

	@Test
	public void testMultipleTables() {
		String query = "SELECT columnOne FROM books, authors, movies";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("books");
		tables.add("authors");
		tables.add("movies");

		List<String> aliases = new LinkedList<>();

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");

		List<String> conditionalColumns = new LinkedList<>();

		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	}

	@Test
	public void testMultipleSelectedColumns() {
		String query = "SELECT columnOne, columnTwo, columnThree FROM books";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");
		selectedColumns.add("columnTwo");
		selectedColumns.add("columnThree");

		List<String> conditionalColumns = new LinkedList<>();

		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	}

	@Test
	public void testMultipleConditionalColumns() {
		String query = "SELECT columnOne FROM books WHERE columnTwo = 2 AND columnFour = 4 AND columnThree = columnTwo";

		List<String> consts = new LinkedList<>();
		consts.add( "2" );
		consts.add( "4" );

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");

		List<String> uncontainedConditionalColumns = new LinkedList<>();
		uncontainedConditionalColumns.add("columnTwo");
		uncontainedConditionalColumns.add("columnFour");

		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, uncontainedConditionalColumns );
	}

	@Test
	public void testMultipleAliases() {
		String query = "SELECT columnOne AS one, columnTwo AS two, columnThree AS three FROM books";

		List<String> consts = new LinkedList<>();

		Set<String> tables = new HashSet<>();
		tables.add("books");

		List<String> aliases = new LinkedList<>();
		aliases.add("one");
		aliases.add("two");
		aliases.add("three");

		List<String> selectedColumns = new LinkedList<>();
		selectedColumns.add("columnOne");
		selectedColumns.add("columnTwo");
		selectedColumns.add("columnThree");

		List<String> conditionalColumns = new LinkedList<>();

		checkQueryMetadata(query, AntlrQueryType.SELECT, consts, tables, aliases, selectedColumns, conditionalColumns );
	}

	@Test
	public void testSimpleVectorizable() {
		String query = "SELECT COUNT(*) FROM t";
		AntlrParser.ParseResult parseResult = parser.buildParseTree( query );
		AntlrQueryMetadata queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );

		query = "SELECT MAX(a) FROM t";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );

		query = "SELECT MIN(b) FROM t";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );

		query = "SELECT b, c FROM t INNER JOIN u ON t.col = u.col";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );

		query = "SELECT b, c FROM t LEFT JOIN u ON t.col = u.col";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );

		query = "SELECT b, c FROM t FULL OUTER JOIN u ON t.col = u.col";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );

		query = "SELECT b, c FROM t, u WHERE t.col = u.col";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertTrue( queryMetadata.isSimpleVectorizable() );

		query = "SELECT b, c FROM t, u WHERE t.col = u.col LIMIT 5";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );

		query = "SELECT b FROM t WHERE col = 5 ORDER BY b";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );
		List<String> orderByConditions = queryMetadata.getOrderByConditions();
		assertThat( orderByConditions.size(), equalTo( 1 ) );
		assertThat( orderByConditions.get( 0 ), equalTo( "b" ) );

		query = "SELECT b FROM t WHERE col = 5 ORDER BY b, SUM(c), d";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );
		orderByConditions = queryMetadata.getOrderByConditions();
		assertThat( orderByConditions.size(), equalTo( 3 ) );
		assertThat( orderByConditions.get( 0 ), equalTo( "b" ) );
		assertThat( orderByConditions.get( 1 ), equalTo( "SUM(c)" ) );
		assertThat( orderByConditions.get( 2 ), equalTo( "d" ) );


		query = "SELECT b FROM ( SELECT * FROM T ORDER BY a ) foo ORDER BY b LIMIT 1";
		parseResult = parser.buildParseTree( query );
		queryMetadata = parser.getQueryMetadata( parseResult.getParseTree() );
		assertFalse( queryMetadata.isSimpleVectorizable() );
		orderByConditions = queryMetadata.getOrderByConditions();
		assertThat( orderByConditions.size(), equalTo( 1 ) );
		assertThat( orderByConditions.get( 0 ), equalTo( "b" ) );
	}
}
