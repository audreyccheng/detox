package chronocache.core;

import chronocache.db.DB;
import chronocache.db.DBException;

import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;

import chronocache.core.parser.PlSqlParser;
import chronocache.core.parser.PlSqlLexer;
import chronocache.core.parser.CaseChangingCharStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.Future;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.junit.Test;
import org.junit.Ignore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;


public class LateralUnionVectorizerTest {

	private class TestDB implements DB {

		private int curResultSetNumber;
		private List<QueryResult> storedResultSets;

		public TestDB() {
			super();
			curResultSetNumber = 0;
			storedResultSets = new LinkedList<>();
		}

		public void addResultSet( QueryResult resultSet ) {
			storedResultSets.add( resultSet );
		}

		@Override
		public int getCacheHits() {
			return 0;
		}

		@Override
		public int getCacheMiss() {
			return 0;
		}


		@Override
		public QueryResult query( long clientId, String queryString ) throws DBException {
			QueryResult resultSet = storedResultSets.get( curResultSetNumber );
			curResultSetNumber++;
			return resultSet;
		}


		@Override
		public QueryResult query( long clientId, String queryString, boolean shouldGetNumColumns ) throws DBException {
			QueryResult resultSet = storedResultSets.get( curResultSetNumber );
			curResultSetNumber++;
			return resultSet;
		}


		@Override
		public QueryResult query( long clientId, String queryString, AntlrParser parser, boolean shouldGetNumColumns ) throws DBException {
			QueryResult resultSet = storedResultSets.get( curResultSetNumber );
			curResultSetNumber++;
			return resultSet;
		}

		@Override
		public QueryResult query( long clientId, String queryString, VersionVector version, AntlrParser parser, boolean shouldGetNumColumns ) throws DBException {
			QueryResult resultSet = storedResultSets.get( curResultSetNumber );
			curResultSetNumber++;
			return resultSet;
		}

		@Override
		public QueryResult querySpeculatively( long clientId, String queryString, Set<String> tableNames, boolean shouldUpdateClientVersion ) throws DBException {
			QueryResult resultSet = storedResultSets.get( curResultSetNumber );
			curResultSetNumber++;
			return resultSet;
		}


		@Override
		public QueryResult querySpeculativelyToGetNumRows( long clientId, String queryString ) throws DBException {
			QueryResult resultSet = storedResultSets.get( curResultSetNumber );
			curResultSetNumber++;
			return resultSet;
		}



		@Override
		public Future<QueryResult> asyncQuery( long clientId, VersionVector version, String queryString, boolean shouldUpdateClientVersion ) {
			return null;

		}

		@Override
		public Connection getClientConn( long clientId ) throws SQLException {
			return null;
		}

		@Override
		public CacheHandler getCacheHandler() {
			return null;
		}

		@Override
		public void stopDB() {
		}

	}


	@Test
	public void testAddRowNumbers() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		String queryString1 = "SELECT colA FROM T";
		String queryString2 = "SELECT colA, MAX(colB) FROM T GROUP BY colA";
		String queryString3 = "SELECT * FROM ( SELECT * FROM T )";

		String outString = vectorizer.addRowNumberColToQueryString( queryString1, new LinkedList<String>(), new HashMap<String, List<String>>(), 1 );
		assertThat( outString, equalTo( "SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT colA FROM T ) k" ) );

		outString = vectorizer.addRowNumberColToQueryString( queryString2, new LinkedList<String>(), new HashMap<String, List<String>>(), 2 );
		assertThat( outString, equalTo( "SELECT *, ROW_NUMBER() OVER () AS rn2 FROM ( SELECT colA, MAX(colB) FROM T GROUP BY colA ) k" ) );

		outString = vectorizer.addRowNumberColToQueryString( queryString3, new LinkedList<String>(), new HashMap<String, List<String>>(), 20 );
		assertThat( outString, equalTo( "SELECT *, ROW_NUMBER() OVER () AS rn20 FROM ( SELECT * FROM ( SELECT * FROM T ) ) k" ) );
	}

	@Test
	public void testZipperTextForTwoSimpleQueries() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		String queryString1 = "SELECT t2 FROM T";
		// rn0 as set up for recursion
		List<String> query1Cols = new LinkedList<String>() {{ add( "t2" ); add( "rn0" ); }};
		String queryString2 = "SELECT s2 FROM S";
		List<String> query2Cols = new LinkedList<String>() {{ add( "s2" ); }};

		//Base case, do it here
		String revisedQueryString1 = vectorizer.addRowNumberColToQueryString( queryString1, new LinkedList<String>(), new HashMap<String, List<String>>(), 0 );

		String rewrittenQueryText = vectorizer.zipperMergeTwoQueries( revisedQueryString1, queryString2,  new LinkedList<String>(), new HashMap<String, List<String>>(), 0, 1 );
		assertThat( rewrittenQueryText, equalTo( "SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s2 FROM S ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s2 FROM S ) k ) z1 ON z0.rn0 = z1.rn1" ) );
	}

	@Test
	public void testZipperTextForTwoSimpleQueriesOrder() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		String queryString1 = "SELECT t2 FROM T";
		// rn0 as set up for recursion
		String queryString2 = "SELECT t2 AS t2_0 FROM S";

		//Base case, do it here
		String revisedQueryString1 = vectorizer.addRowNumberColToQueryString( queryString1, new LinkedList<String>(), new HashMap<String, List<String>>(), 0 );

		Map<String, List<String>> colsToAadjust = new HashMap<>();
		colsToAadjust.put( "t2", new LinkedList<String>() {{ add( "t2_0" ); }} );
		String rewrittenQueryText = vectorizer.zipperMergeTwoQueries( revisedQueryString1, queryString2,  new LinkedList<String>() {{ add( "t2 ASC" ); }}, colsToAadjust, 0, 1 );
		assertThat( rewrittenQueryText, equalTo( "SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2_0 ASC) AS rn1 FROM ( SELECT t2 AS t2_0 FROM S ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2_0 ASC) AS rn1 FROM ( SELECT t2 AS t2_0 FROM S ) k ) z1 ON z0.rn0 = z1.rn1" ) );
	}

	@Test
	public void testZipperTextForTwoSimpleQueriesOrderDoubleAlias() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		String queryString1 = "SELECT t2 FROM T";
		// rn0 as set up for recursion
		String queryString2 = "SELECT t2 AS t2_0, t2 AS t2_0_0 FROM S";

		//Base case, do it here
		String revisedQueryString1 = vectorizer.addRowNumberColToQueryString( queryString1, new LinkedList<String>(), new HashMap<String, List<String>>(), 0 );

		Map<String, List<String>> colsToAadjust = new HashMap<>();
		colsToAadjust.put( "t2", new LinkedList<String>() {{ add( "t2_0" ); add( "t2_0_0" ); }} );
		String rewrittenQueryText = vectorizer.zipperMergeTwoQueries( revisedQueryString1, queryString2,  new LinkedList<String>() {{ add( "t2 ASC, t2 DESC" ); }}, colsToAadjust, 0, 1 );
		assertThat( rewrittenQueryText, equalTo( "SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2_0 ASC, t2_0_0 DESC) AS rn1 FROM ( SELECT t2 AS t2_0, t2 AS t2_0_0 FROM S ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2_0 ASC, t2_0_0 DESC) AS rn1 FROM ( SELECT t2 AS t2_0, t2 AS t2_0_0 FROM S ) k ) z1 ON z0.rn0 = z1.rn1" ) );
	}



	@Test
	public void testZipperTextForTwoQueries() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		String queryString1 = "SELECT colA FROM T";
		// rn0 as set up for recursion
		List<String> query1Cols = new LinkedList<String>() {{ add( "colA" ); add( "rn0" ); }};
		String queryString2 = "SELECT colA, MAX(colB) AS m FROM T GROUP BY colA";
		List<String> query2Cols = new LinkedList<String>() {{ add( "colA" ); add( "m" ); }};

		//Base case, do it here
		String revisedQueryString1 = vectorizer.addRowNumberColToQueryString( queryString1, new LinkedList<String>(), new HashMap<String, List<String>>(), 0 );

		String rewrittenQueryText = vectorizer.zipperMergeTwoQueries( revisedQueryString1, queryString2, new LinkedList<String>(), new HashMap<String, List<String>>(),  0, 1 );
		assertThat( rewrittenQueryText, equalTo( "SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT colA FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT colA, MAX(colB) AS m FROM T GROUP BY colA ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT colA FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT colA, MAX(colB) AS m FROM T GROUP BY colA ) k ) z1 ON z0.rn0 = z1.rn1" ));

	}


	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testZipperMergeSimple() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT colA FROM T";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT colB FROM T";
		Query query2 = new Query( queryString2 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );

		List<QueryIdentifier> queryIds = new LinkedList<>();
		queryIds.add( query1.getId() );
		queryIds.add( query2.getId() );

		//First query is SELECT colA FROM T
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "colA", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query is SELECT colB FROM T
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "colB", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		LateralUnionVectorizer.ZipperMergeResult zipperedResult = vectorizer.zipperMerge( 0, queryIds, dependencyGraph.getVectorizationPlan(), db, new HashMap<QueryIdentifier, Map<String,List<String>>>() );
		assertThat( zipperedResult.getZipperedQueryText(), equalTo( "SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT colA FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT colB FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT colA FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT colB FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ORDER BY rn0, rn1" ) );
		assertThat( zipperedResult.getTables().size(), equalTo( 1 ) );
		assertThat( zipperedResult.getTables().iterator().next(), equalTo( "T" ) );
	}

	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testZipperMergeQueriesWithSameColumnName() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT colA FROM T";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT colA, colB FROM T2";
		Query query2 = new Query( queryString2 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );

		List<QueryIdentifier> queryIds = new LinkedList<>();
		queryIds.add( query1.getId() );
		queryIds.add( query2.getId() );

		//First query is SELECT colA FROM T
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "colA", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query is SELECT colB FROM T2
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "colA", (Object) new Long(3L) );
		row.put( "colB", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 2L ); }} ), 1 );
		db.addResultSet( resultToStore );

		LateralUnionVectorizer.ZipperMergeResult zipperedResult = vectorizer.zipperMerge( 0, queryIds, dependencyGraph.getVectorizationPlan(), db, new HashMap<QueryIdentifier, Map<String,List<String>>>() );
		assertThat( zipperedResult.getZipperedQueryText(), equalTo( "SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT colA FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT colA AS colA_0, colB FROM T2 ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT colA FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT colA AS colA_0, colB FROM T2 ) k ) z1 ON z0.rn0 = z1.rn1 ORDER BY rn0, rn1" ) );
		assertThat( zipperedResult.getTables().size(), equalTo( 2 ) );

	}


	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testZipperMergeOneQuery() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT colA FROM T";
		Query query1 = new Query( queryString1 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		List<QueryIdentifier> queryIds = new LinkedList<>();
		queryIds.add( query1.getId() );

		//First query is SELECT colA FROM T
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "colA", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		LateralUnionVectorizer.ZipperMergeResult zipperedResult = vectorizer.zipperMerge( 0, queryIds, dependencyGraph.getVectorizationPlan(), db, new HashMap<QueryIdentifier, Map<String,List<String>>>() );
		// Still need to qualify the rows via a candidate key in case we don't have one
		assertThat( zipperedResult.getZipperedQueryText(), equalTo( "SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT colA FROM T ) k") );
		assertThat( zipperedResult.getTables().size(), equalTo( 1 ) );

	}


	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testZipperMergeThreeQueries() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT t2 FROM T";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT s2 FROM S";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT v2 FROM V";
		Query query3 = new Query( queryString3 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );
		dependencyGraph.addBaseQuery( query3.getId(), query3 );

		List<QueryIdentifier> queryIds = new LinkedList<>();
		queryIds.add( query1.getId() );
		queryIds.add( query2.getId() );
		queryIds.add( query3.getId() );

		//First query is SELECT t2 FROM T
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );


		// Second query is SELECT s2 FROM S
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Third query is SELECT v2 FROM v
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );





		LateralUnionVectorizer.ZipperMergeResult zipperedResult = vectorizer.zipperMerge( 0, queryIds, dependencyGraph.getVectorizationPlan(), db, new HashMap<QueryIdentifier, Map<String, List<String>>>() );
		assertThat( zipperedResult.getZipperedQueryText(), equalTo( "SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s2 FROM S ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s2 FROM S ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT v2 FROM V ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s2 FROM S ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s2 FROM S ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT v2 FROM V ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ORDER BY rn0, rn1, rn3" ) );

	}

	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testZipperMergeFiveQueries() {
		String queryString1 = "SELECT t2 FROM T";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1, t2 FROM T";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT s1 FROM S";
		Query query3 = new Query( queryString3 );
		String queryString4 = "SELECT s2 FROM S";
		Query query4 = new Query( queryString4 );
		String queryString5 = "SELECT s1, s2 FROM S";
		Query query5 = new Query( queryString5 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );
		dependencyGraph.addBaseQuery( query3.getId(), query3 );
		dependencyGraph.addBaseQuery( query4.getId(), query4 );
		dependencyGraph.addBaseQuery( query5.getId(), query5 );

		List<QueryIdentifier> queryIds = new LinkedList<>();
		queryIds.add( query1.getId() );
		queryIds.add( query2.getId() );
		queryIds.add( query3.getId() );
		queryIds.add( query4.getId() );
		queryIds.add( query5.getId() );

		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		//First query is SELECT t2 FROM T
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		//Second query is SELECT t1, t2 FROM T
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		//Third query is SELECT s1 FROM S
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		//Fourth query is SELECT s2 FROM S
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		//Fifth query is SELECT s1, s2 FROM S
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		LateralUnionVectorizer.ZipperMergeResult zipperedResult = vectorizer.zipperMerge( 0, queryIds, dependencyGraph.getVectorizationPlan(), db, new HashMap<QueryIdentifier, Map<String, List<String>>>() );
		assertThat( zipperedResult.getZipperedQueryText(), equalTo( "SELECT * FROM ( SELECT * FROM ( SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ) z4 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn5 FROM ( SELECT s2 FROM S ) k ) z5 ON z4.rn0 = z5.rn5 OR z4.rn1 = z5.rn5 OR z4.rn3 = z5.rn5 UNION SELECT * FROM ( SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ) z4 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn5 FROM ( SELECT s2 FROM S ) k ) z5 ON z4.rn0 = z5.rn5 OR z4.rn1 = z5.rn5 OR z4.rn3 = z5.rn5 ) z6 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn7 FROM ( SELECT s1 AS s1_0, s2 AS s2_0 FROM S ) k ) z7 ON z6.rn0 = z7.rn7 OR z6.rn1 = z7.rn7 OR z6.rn3 = z7.rn7 OR z6.rn5 = z7.rn7 UNION SELECT * FROM ( SELECT * FROM ( SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ) z4 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn5 FROM ( SELECT s2 FROM S ) k ) z5 ON z4.rn0 = z5.rn5 OR z4.rn1 = z5.rn5 OR z4.rn3 = z5.rn5 UNION SELECT * FROM ( SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1, t2 AS t2_0 FROM T ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 FROM S ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ) z4 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn5 FROM ( SELECT s2 FROM S ) k ) z5 ON z4.rn0 = z5.rn5 OR z4.rn1 = z5.rn5 OR z4.rn3 = z5.rn5 ) z6 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn7 FROM ( SELECT s1 AS s1_0, s2 AS s2_0 FROM S ) k ) z7 ON z6.rn0 = z7.rn7 OR z6.rn1 = z7.rn7 OR z6.rn3 = z7.rn7 OR z6.rn5 = z7.rn7 ORDER BY rn0, rn1, rn3, rn5, rn7" ) );

	}

	@Test
	public void testVectorizeNestedSingles(){
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1 FROM T WHERE t2 = 3";
		Query query2 = new Query( queryString2 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );


		//Next query we check is "SELECT t1 FROM T WHERE t2 = P0.s1"
		db.addResultSet( resultToStore );
		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t1 FROM T WHERE t2 = P0.s1 ) k) I0) U( type0, u0, u1, u2, u3 ) ORDER BY ordKey0, type0" ) );


		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );

		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// t1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// base Level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );


		// The result set
		// ordkey0 | type0 | u0 | u1 | u2 | u3 
		//---------+-------+----+----+----+----
		//       1 |     0 |  1 |  1 |    |   
		//       1 |     1 |    |    | f  |  1
		//       2 |     0 |  2 |  2 |    |   
		//       2 |     1 |    |    | h  |  1
		//       3 |     0 |  3 |  3 |    |   
		//       3 |     1 |    |    | j  |  1
		//       4 |     0 |  4 |  4 |    |   
		//       5 |     0 |  5 |  5 |    |   

		List<Map<String,Object>> rows = new LinkedList<>();
		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new String( "f" ) );
		row.put( "u3", (Object) new Long( 1L) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 2L ) );
		row.put( "u1", (Object) new Long( 2L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new String( "h" ) );
		row.put( "u3", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 3L ) );
		row.put( "u1", (Object) new Long( 3L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new String( "j" ) );
		row.put( "u3", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 4L ) );
		row.put( "u1", (Object) new Long( 4L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 5L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 5L ) );
		row.put( "u1", (Object) new Long( 5L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		rows.add( row );


		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );
		assertThat( qResults.size(), equalTo( 2 ) );
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().size(), equalTo( 5 ) );
		assertThat( qResults.get( 1 ).size(), equalTo( 5 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().isEmpty(), equalTo( false ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().isEmpty(), equalTo( false ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().isEmpty(), equalTo( false ) );
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().isEmpty(), equalTo( true ) );
		assertThat( qResults.get( 1 ).get( 4 ).getSelectResult().isEmpty(), equalTo( true ) );


		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		Query q0 = new Query( "SELECT s1 FROM S" );
		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( q0.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).size(), equalTo( 5 ) );
		Query q1 = new Query( "SELECT t1 FROM T WHERE t2 = 1" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q1.getCacheKey() ) );
		Query q2 = new Query( "SELECT t1 FROM T WHERE t2 = 2" );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( q2.getCacheKey() ) );
		Query q3 = new Query( "SELECT t1 FROM T WHERE t2 = 3" );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( q3.getCacheKey() ) );
		Query q4 = new Query( "SELECT t1 FROM T WHERE t2 = 4" );
		assertThat( cacheKeys.get( 1 ).get( 3 ), equalTo( q4.getCacheKey() ) );
		Query q5 = new Query( "SELECT t1 FROM T WHERE t2 = 5" );
		assertThat( cacheKeys.get( 1 ).get( 4 ), equalTo( q5.getCacheKey() ) );
	}

	@Test
	public void testVectorizeNestedDouble(){
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1 FROM T WHERE t2 < 3";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT t2 FROM T WHERE t2 < 3";
		Query query3 = new Query( queryString3 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );
		mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		qme = new QueryMappingEntry( query1.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t1 ASC"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1 ASC"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		//Lowest level query (when zipped) has 4 columns
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "rn0", (Object) new Long(3L) );
		row.put( "t1", (Object) new Long(3L) );
		row.put( "rn1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 4 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T WHERE t2 < P0.s1 ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1 FROM T WHERE t2 < P0.s1 ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t2 FROM T WHERE t2 < P0.s1 ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT t1 FROM T WHERE t2 < P0.s1 ) k ) z1 ON z0.rn0 = z1.rn1 ORDER BY rn0, rn1) I0) U( type0, u0, u1, u2, u3, u4, u5 ) ORDER BY ordKey0, type0, u3, u5" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );


		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// t1, RN0, t2, RN)
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 4 ) );
		// base Level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );


		// ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 
		//---------+-------+----+----+----+----+----+----
		//       1 |     0 |  1 |  1 |    |    |    |   
		//       2 |     0 |  2 |  2 |    |    |    |   
		//       2 |     1 |    |    |  1 |  1 | f  |  1
		//       3 |     0 |  3 |  3 |    |    |    |   
		//       3 |     1 |    |    |  1 |  1 | f  |  1
		//       3 |     1 |    |    |  2 |  2 | h  |  2
		//       4 |     0 |  4 |  4 |    |    |    |   
		//       4 |     1 |    |    |  1 |  1 | f  |  1
		//       4 |     1 |    |    |  2 |  2 | h  |  2
		//       4 |     1 |    |    |  3 |  3 | j  |  3
		//       5 |     0 |  5 |  5 |    |    |    |   
		//       5 |     1 |    |    |  1 |  1 | f  |  1
		//       5 |     1 |    |    |  2 |  2 | h  |  2
		//       5 |     1 |    |    |  3 |  3 | j  |  3

		List<Map<String,Object>> rows = new LinkedList<>();
		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 2L ) );
		row.put( "u1", (Object) new Long( 2L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new String( "f" ) );
		row.put( "u5", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 3L ) );
		row.put( "u1", (Object) new Long( 3L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new String( "f" ) );
		row.put( "u5", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new String( "h" ) );
		row.put( "u5", (Object) new Long( 2L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 4L ) );
		row.put( "u1", (Object) new Long( 4L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new String( "f" ) );
		row.put( "u5", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new String( "h" ) );
		row.put( "u5", (Object) new Long( 2L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 3L ) );
		row.put( "u4", (Object) new String( "j" ) );
		row.put( "u5", (Object) new Long( 3L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 5L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 5L ) );
		row.put( "u1", (Object) new Long( 5L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 5L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new String( "f" ) );
		row.put( "u5", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 5L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new String( "h" ) );
		row.put( "u5", (Object) new Long( 2L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 5L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 3L ) );
		row.put( "u4", (Object) new String( "j" ) );
		row.put( "u5", (Object) new Long( 3L ) );
		rows.add( row );

		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );
		assertThat( qResults.size(), equalTo( 3 ) );
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().size(), equalTo( 5 ) );
		assertThat( qResults.get( 1 ).size(), equalTo( 5 ) );
		assertThat( qResults.get( 2 ).size(), equalTo( 5 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().isEmpty(), equalTo( true ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().isEmpty(), equalTo( true ) );

		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().size(), equalTo( 1) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) );

		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().size(), equalTo( 2 ) );
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().size(), equalTo( 3 ) );
		assertThat( qResults.get( 2 ).get( 3 ).getSelectResult().size(), equalTo( 3 ) );
		assertThat( qResults.get( 1 ).get( 4 ).getSelectResult().size(), equalTo( 3 ) );
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().size(), equalTo( 3 ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( 3 ) );
		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		Query q0 = new Query( "SELECT s1 FROM S" );
		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( q0.getCacheKey() ) );

		assertThat( cacheKeys.get( 1 ).size(), equalTo( 5 ) );
		Query q11 = new Query( "SELECT t1 FROM T WHERE t2 < 1" );
		Query q12 = new Query( "SELECT t1 FROM T WHERE t2 < 2" );
		Query q13 = new Query( "SELECT t1 FROM T WHERE t2 < 3" );
		Query q14 = new Query( "SELECT t1 FROM T WHERE t2 < 4" );
		Query q15 = new Query( "SELECT t1 FROM T WHERE t2 < 5" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q11.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( q12.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( q13.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 3 ), equalTo( q14.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 4 ), equalTo( q15.getCacheKey() ) );

		assertThat( cacheKeys.get( 2 ).size(), equalTo( 5 ) );
		Query q21 = new Query( "SELECT t2 FROM T WHERE t2 < 1" );
		Query q22 = new Query( "SELECT t2 FROM T WHERE t2 < 2" );
		Query q23 = new Query( "SELECT t2 FROM T WHERE t2 < 3" );
		Query q24 = new Query( "SELECT t2 FROM T WHERE t2 < 4" );
		Query q25 = new Query( "SELECT t2 FROM T WHERE t2 < 5" );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( q21.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 1 ), equalTo( q22.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 2 ), equalTo( q23.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 3 ), equalTo( q24.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 4 ), equalTo( q25.getCacheKey() ) );
	}

	@Test
	public void testNestedNestedQuery() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT s1 FROM S WHERE s1 = 3";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT s1,s2 FROM S WHERE s1 = 3";
		Query query3 = new Query( queryString3 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );
		mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		qme = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Third query we check is "SELECT s1,s2 FROM S WHERE s1 = 3"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s11", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT P1.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = P0.s1 ) k ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I1.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s2 FROM S WHERE s1 = P1.s1 ) k) I1) U( type1, u0, u1, u2, u3, u4 ) ORDER BY ordKey0, type1) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8 ) ORDER BY ordKey0, type0, u2, u3" ) );

		assertThat( result.getTables().size(), equalTo( 1 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );

		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 1 ) );
		// s1, s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 2 ), equalTo( 3 ) );
		// Base Level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 2 ), equalTo( 0 ) );

		//  ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 | u6 | u7 | u8 
		//---------+-------+----+----+----+----+----+----+----+----+----
		//       1 |     0 |  1 |  1 |    |    |    |    |    |    |   
		//       1 |     1 |    |    |  1 |  0 |  1 |  1 |    |    |   
		//       1 |     1 |    |    |  1 |  1 |    |    |  1 |  D |  1
		//       2 |     0 |  2 |  2 |    |    |    |    |    |    |   
		//       2 |     1 |    |    |  1 |  0 |  2 |  1 |    |    |   
		//       2 |     1 |    |    |  1 |  1 |    |    |  2 |  E |  1
		//       3 |     0 |  3 |  3 |    |    |    |    |    |    |   
		//       3 |     1 |    |    |  1 |  0 |  3 |  1 |    |    |   
		//       3 |     1 |    |    |  1 |  1 |    |    |  3 |  F |  1

		List<Map<String,Object>> rows = new LinkedList<>();

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 0 ) );
		row.put( "u4", (Object) new Long( 1L ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", (Object) new String( "D" ) );
		row.put( "u8", (Object) new Long( 1L ));
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 2L ) );
		row.put( "u1", (Object) new Long( 2L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 0 ) );
		row.put( "u4", (Object) new Long( 2L ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 2L ) );
		row.put( "u7", (Object) new String( "E" ) );
		row.put( "u8", (Object) new Long( 1L ));
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 3L ) );
		row.put( "u1", (Object) new Long( 3L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 0 ) );
		row.put( "u4", (Object) new Long( 3L ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 3L ) );
		row.put( "u7", (Object) new String( "F" ) );
		row.put( "u8", (Object) new Long( 1L ));
		rows.add( row );

		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );

		assertThat( qResults.size(), equalTo( 3 ) );
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().size(), equalTo( 3 ) );
		assertThat( qResults.get( 1 ).size(), equalTo( 3 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().get(0).size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().get(0).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().get(0).get( "s1" ), equalTo( 2L ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().get(0).size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get(0).size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get(0).get( "s1" ), equalTo( 3L ) );
		assertThat( qResults.get( 2 ).size(), equalTo( 3 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get(0).size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get(0).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get(0).get( "s2" ), equalTo( "D" ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(0).size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(0).get( "s1" ), equalTo( 2L ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(0).get( "s2" ), equalTo( "E" ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(0).size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(0).get( "s1" ), equalTo( 3L ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(0).get( "s2" ), equalTo( "F" ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( 3 ) );

		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		Query q0 = new Query( "SELECT s1 FROM S" );

		assertThat( cacheKeys.get( 1 ).size(), equalTo( 3 ) );
		Query q11 = new Query( "SELECT s1 FROM S WHERE s1 = 1" );
		Query q12 = new Query( "SELECT s1 FROM S WHERE s1 = 2" );
		Query q13 = new Query( "SELECT s1 FROM S WHERE s1 = 3" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q11.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( q12.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( q13.getCacheKey() ) );

		assertThat( cacheKeys.get( 2 ).size(), equalTo( 3 ) );
		Query q21 = new Query( "SELECT s1,s2 FROM S WHERE s1 = 1" );
		Query q22 = new Query( "SELECT s1,s2 FROM S WHERE s1 = 2" );
		Query q23 = new Query( "SELECT s1,s2 FROM S WHERE s1 = 3" );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( q21.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 1 ), equalTo( q22.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 2 ), equalTo( q23.getCacheKey() ) );

	}

	@Test
	public void testNestedNestedNestedQuery() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT s1 FROM S WHERE s1 = 3";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT s1,s2 FROM S WHERE s1 = 3";
		Query query3 = new Query( queryString3 );
		// XXX Can't have duplicate column names, so our result set size will be reduced.
		String queryString4 = "SELECT s1,s1,s1 FROM S WHERE s1 = 3";
		Query query4 = new Query( queryString4 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );
		mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		qme = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );
		mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		qme = new QueryMappingEntry( query3.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Third query we check is "SELECT s1,s1 FROM S WHERE s1 = 3"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s11", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		// Fourth query we check is "SELECT s1,s1,s1 FROM S WHERE s1 = 3"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s11", (Object) new Long(3L) );
		row.put( "s111", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 3 );

		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT P1.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = P0.s1 ) k ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I1.* FROM ( SELECT P2.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s2 FROM S WHERE s1 = P1.s1 ) k ) P2, LATERAL ( SELECT 0, P2.*, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, I2.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s1,s1 FROM S WHERE s1 = P2.s1 ) k) I2) U( type2, u0, u1, u2, u3, u4, u5, u6 ) ORDER BY ordKey0, type2) I1) U( type1, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10 ) ORDER BY ordKey0, type1, u2, u3) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14 ) ORDER BY ordKey0, type0, u2, u3, u6, u7" ) );

		assertThat( result.getTables().size(), equalTo( 1 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );

		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 1 ) );
		// s1, s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 2 ), equalTo( 3 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 2 ), equalTo( 1 ) );
		// s1, s1, s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 3 ), equalTo( 4 ) );
		// Base Level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 3 ), equalTo( 0 ) );

		/*
		 ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 | u6 | u7 | u8 | u9 | u10 | u11 | u12 | u13 | u14 
		---------+-------+----+----+----+----+----+----+----+----+----+----+-----+-----+-----+-----+-----
			   1 |     0 |  1 |  1 |    |    |    |    |    |    |    |    |     |     |     |     |    
			   1 |     1 |    |    |  1 |  0 |  1 |  1 |    |    |    |    |     |     |     |     |    
			   1 |     1 |    |    |  1 |  1 |    |    |  1 |  0 |  1 | A  |   1 |     |     |     |    
			   1 |     1 |    |    |  1 |  1 |    |    |  1 |  1 |    |    |     |   1 |   1 |   1 |   1
			   2 |     0 |  2 |  2 |    |    |    |    |    |    |    |    |     |     |     |     |    
			   2 |     1 |    |    |  1 |  0 |  2 |  1 |    |    |    |    |     |     |     |     |    
			   2 |     1 |    |    |  1 |  1 |    |    |  1 |  0 |  2 | B  |   1 |     |     |     |    
			   2 |     1 |    |    |  1 |  1 |    |    |  1 |  1 |    |    |     |   2 |   2 |   2 |   1
			   3 |     0 |  3 |  3 |    |    |    |    |    |    |    |    |     |     |     |     |    
			   3 |     1 |    |    |  1 |  0 |  3 |  1 |    |    |    |    |     |     |     |     |    
			   3 |     1 |    |    |  1 |  1 |    |    |  1 |  0 |  3 | C  |   1 |     |     |     |    
			   3 |     1 |    |    |  1 |  1 |    |    |  1 |  1 |    |    |     |   3 |   3 |   3 |   1
		*/

		List<Map<String,Object>> rows = new LinkedList<>();

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		row.put( "u10", null );
		row.put( "u11", null );
		row.put( "u12", null );
		row.put( "u13", null );
		row.put( "u14", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 0 ) );
		row.put( "u4", (Object) new Long( 1L ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		row.put( "u10", null );
		row.put( "u11", null );
		row.put( "u12", null );
		row.put( "u13", null );
		row.put( "u14", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", (Object) new Integer( 0 ) );
		row.put( "u8", (Object) new Long( 1L ) );
		row.put( "u9", (Object) new String( "A" ) );
		row.put( "u10", (Object) new Long( 1L ) );
		row.put( "u11", null );
		row.put( "u12", null );
		row.put( "u13", null );
		row.put( "u14", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", (Object) new Integer( 1 ) );
		row.put( "u8", null );
		row.put( "u9", null );
		row.put( "u10", null );
		row.put( "u11", (Object) new Long( 1L ));
		row.put( "u12", (Object) new Long( 1L ));
		row.put( "u13", (Object) new Long( 1L ));
		row.put( "u14", (Object) new Long( 1L ));
		rows.add( row );

		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );

		assertThat( qResults.size(), equalTo( 4 ) );
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 0 ).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 1 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().get( 0 ).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 2 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get( 0 ).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get( 0 ).get( "s2" ), equalTo( "A" ) );
		assertThat( qResults.get( 3 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 3 ).get( 0 ).getSelectResult().get( 0 ).get( "s1" ), equalTo( 1L ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );

		assertThat( cacheKeys.size(), equalTo( 4 ) );

		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		Query q0 = new Query( "SELECT s1 FROM S" );

		assertThat( cacheKeys.get( 1 ).size(), equalTo( 1 ) );
		Query q11 = new Query( "SELECT s1 FROM S WHERE s1 = 1" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q11.getCacheKey() ) );

		assertThat( cacheKeys.get( 2 ).size(), equalTo( 1 ) );
		Query q21 = new Query( "SELECT s1,s2 FROM S WHERE s1 = 1" );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( q21.getCacheKey() ) );

		assertThat( cacheKeys.get( 3 ).size(), equalTo( 1 ) );
        Query q31 = new Query( "SELECT s1,s1,s1 FROM S WHERE s1 = 1" );
		assertThat( cacheKeys.get( 3 ).get( 0 ), equalTo( q31.getCacheKey() ) );

	}

	@Test
	public void testVectorizeNestedSinglesWithBottomOrderBy(){
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S ORDER BY s1";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1";
		Query query2 = new Query( queryString2 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );


		// Next query we check is "SELECT t1 FROM T WHERE t2 = P0.s1"
		// Also selects 2 columns, so we can just reuse the result set
		db.addResultSet( resultToStore );
		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.s1 AS ordKey0, P0.rn0 AS ordKey1, U.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY s1) AS rn0 FROM ( SELECT s1 FROM S ORDER BY s1 ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t1) AS rn0 FROM ( SELECT t1 FROM T WHERE t2 < P0.s1 ORDER BY t1 ) k) I0) U( type0, u0, u1, u2, u3 ) ORDER BY ordKey0, ordKey1, type0, u2" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );

		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// ORDER BY s1, CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// t1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// No additional keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );

	}

	@Test
	public void testVectorizeNestedSinglesDoubleOrderByTop() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 AS v, s1 AS v2 FROM S ORDER BY v, v2";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1";
		Query query2 = new Query( queryString2 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "v" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		// First query we check is "SELECT s1,s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "v", (Object) new Long(3L) );
		row.put( "v2", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );


		//Next query we check is "SELECT t1 FROM T WHERE t2 = P0.s1"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.v AS ordKey0, P0.v2 AS ordKey1, P0.rn0 AS ordKey2, U.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY v, v2) AS rn0 FROM ( SELECT s1 AS v, s1 AS v2 FROM S ORDER BY v, v2 ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t1) AS rn0 FROM ( SELECT t1 FROM T WHERE t2 < P0.v ORDER BY t1 ) k) I0) U( type0, u0, u1, u2, u3, u4 ) ORDER BY ordKey0, ordKey1, ordKey2, type0, u3" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );


		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// v, v2, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 3 ) );
		// ORDER BY v, v2, CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 3 ) );
		// t1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// No additional keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );
	}

	@Test
	public void testVectorizeNestedSinglesDoubleQualifiedOrderBy() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 AS v, s1 AS v2 FROM S ORDER BY v ASC, v2 DESC";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1 DESC";
		Query query2 = new Query( queryString2 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "v" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		// First query we check is "SELECT s1,s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "v", (Object) new Long(3L) );
		row.put( "v2", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );


		// Next query we check is "SELECT t1 FROM T WHERE t2 = P0.s1"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.v AS ordKey0, P0.v2 AS ordKey1, P0.rn0 AS ordKey2, U.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY v ASC, v2 DESC) AS rn0 FROM ( SELECT s1 AS v, s1 AS v2 FROM S ORDER BY v ASC, v2 DESC ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t1 DESC) AS rn0 FROM ( SELECT t1 FROM T WHERE t2 < P0.v ORDER BY t1 DESC ) k) I0) U( type0, u0, u1, u2, u3, u4 ) ORDER BY ordKey0 ASC, ordKey1 DESC, ordKey2, type0, u3 DESC" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );
 
		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// v, v2, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 3 ) );
		// ORDER BY v, v2, CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 3 ) );
		// t1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// No additional keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );


	}

	@Test
	public void testNestedNestedWithOrderBys() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t2 DESC";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT t1, t2 FROM T WHERE t2 < 3 ORDER BY t2 ASC";
		Query query3 = new Query( queryString3 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 0, "t2" );
		qme = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC )"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t1, t2 FROM T WHERE t2 < P1.t2 ORDER BY t2 ASC"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT P1.t2 AS ordKey0, P1.rn0 AS ordKey1, U.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2 DESC) AS rn0 FROM ( SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC ) k ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I1.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2 ASC) AS rn0 FROM ( SELECT t1, t2 FROM T WHERE t2 < P1.t2 ORDER BY t2 ASC ) k) I1) U( type1, u0, u1, u2, u3, u4 ) ORDER BY ordKey0 DESC, ordKey1, type1, u3 ASC) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9 ) ORDER BY ordKey0, type0, u2 DESC, u3, u4, u8 ASC" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );

		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// No order keys, RN0 CK
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// t1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// Order on t2, RN0 CK
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// t1, t2, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 2 ), equalTo( 3 ) );
		// No additional keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 2 ), equalTo( 0 ) );

		/*
		 ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 | u6 | u7 | u8 | u9 
		---------+-------+----+----+----+----+----+----+----+----+----+----
			   1 |     0 |  1 |  1 |    |    |    |    |    |    |    |   
			   2 |     0 |  2 |  2 |    |    |    |    |    |    |    |   
			   2 |     1 |    |    |  1 |  1 |  0 |  1 |  1 |    |    |   
			   3 |     0 |  3 |  3 |    |    |    |    |    |    |    |   
			   3 |     1 |    |    |  2 |  1 |  0 |  2 |  1 |    |    |   
			   3 |     1 |    |    |  2 |  1 |  1 |    |    | f  |  1 |  1
			   3 |     1 |    |    |  1 |  2 |  0 |  1 |  2 |    |    |   
			   4 |     0 |  4 |  4 |    |    |    |    |    |    |    |   
			   4 |     1 |    |    |  3 |  1 |  0 |  3 |  1 |    |    |   
			   4 |     1 |    |    |  3 |  1 |  1 |    |    | f  |  1 |  1
			   4 |     1 |    |    |  3 |  1 |  1 |    |    | h  |  2 |  2
			   4 |     1 |    |    |  2 |  2 |  0 |  2 |  2 |    |    |   
			   4 |     1 |    |    |  2 |  2 |  1 |    |    | f  |  1 |  1
			   4 |     1 |    |    |  1 |  3 |  0 |  1 |  3 |    |    |   
		*/

		List<Map<String,Object>> rows = new LinkedList<>();

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 2L ) );
		row.put( "u1", (Object) new Long( 2L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );


		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 3L ) );
		row.put( "u1", (Object) new Long( 3L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 2L ) );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "f" ) );
		row.put( "u8", (Object) new Long( 1L ) );
		row.put( "u9", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", (Object) new Long( 2L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 4L ) );
		row.put( "u1", (Object) new Long( 4L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 3L ) );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "f" ) );
		row.put( "u8", (Object) new Long( 1L ) );
		row.put( "u9", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "h" ) );
		row.put( "u8", (Object) new Long( 2L ) );
		row.put( "u9", (Object) new Long( 2L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 2L ) );
		row.put( "u6", (Object) new Long( 2L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "f" ) );
		row.put( "u8", (Object) new Long( 1L ) );
		row.put( "u9", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 3L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", (Object) new Long( 3L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );

		assertThat( qResults.size(), equalTo( 3 ) );

		// First query is "SELECT s1 FROM S"
		// Next query is "SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC )"
		// Next query is "SELECT t1, t2 FROM T WHERE t2 < P1.t2 ORDER BY t2 ASC"
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().size(), equalTo( 4 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 0 ).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 1 ).get( "s1" ), equalTo( 2L ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 2 ).get( "s1" ), equalTo( 3L ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 3 ).get( "s1" ), equalTo( 4L ) );

		// 1 Result for each result in q1
		assertThat( qResults.get( 1 ).size(), equalTo( 4 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().isEmpty(), equalTo( true ) ); //nothing < 1
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) ); // one thing < 2
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); // one thing < 2
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().size(), equalTo( 2 ) ); //two things < 3
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 2L ) ); // two things < 2
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get( 1 ).get( "t2" ), equalTo( 1L ) ); // two things < 2
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().size(), equalTo( 3 ) ); //three things < 3
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 3L ) ); // three things < 3
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().get( 1 ).get( "t2" ), equalTo( 2L ) ); // three things < 3
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().get( 2 ).get( "t2" ), equalTo( 1L ) ); // three things < 3

		// 1 (NULL) + 1 + 2 + 3 = 7
		assertThat( qResults.get( 2 ).size(), equalTo( 7 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().isEmpty(), equalTo( true ) ); //Nothing here because nothing matches q2's NULL here
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().isEmpty(), equalTo( true ) ); //Nothing here because nothing < 1
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().size(), equalTo( 1 ) ); // One thing here
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); //One thing here
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "f" ) ); //One thing here
		assertThat( qResults.get( 2 ).get( 3 ).getSelectResult().isEmpty(), equalTo( true ) ); // Nothing here because nothing < 1 
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().size(), equalTo( 2 ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "f" ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 1 ).get( "t2" ), equalTo( 2L ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 1 ).get( "t1" ), equalTo( "h" ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 5 ).getSelectResult().size(), equalTo( 1 ) ); // One row < 2 
		assertThat( qResults.get( 2 ).get( 5 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); // One row < 2
		assertThat( qResults.get( 2 ).get( 5 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "f" ) ); // One row < 2
		assertThat( qResults.get( 2 ).get( 6 ).getSelectResult().isEmpty(), equalTo( true ) ); // No rows < 1

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( 3 ) );
		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		Query q0 = new Query( "SELECT s1 FROM S" );
		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( q0.getCacheKey() ) );
		
		assertThat( cacheKeys.get( 1 ).size(), equalTo( 4 ) );
		Query q11 = new Query( "SELECT t2 FROM T WHERE t2 < 1 ORDER BY t2 DESC" );
		Query q12 = new Query( "SELECT t2 FROM T WHERE t2 < 2 ORDER BY t2 DESC" );
		Query q13 = new Query( "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t2 DESC" );
		Query q14 = new Query( "SELECT t2 FROM T WHERE t2 < 4 ORDER BY t2 DESC" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q11.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( q12.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( q13.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 3 ), equalTo( q14.getCacheKey() ) );

		assertThat( cacheKeys.get( 2 ).size(), equalTo( 7 ) );
		Query q21 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 1 ORDER BY t2 ASC" );
		Query q22 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 2 ORDER BY t2 ASC" );
		Query q23 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 3 ORDER BY t2 ASC" );
		Query q24 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 4 ORDER BY t2 ASC" );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( null ) );
		assertThat( cacheKeys.get( 2 ).get( 1 ), equalTo( q21.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 2 ), equalTo( q22.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 3 ), equalTo( q21.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 4 ), equalTo( q23.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 5 ), equalTo( q22.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 6 ), equalTo( q21.getCacheKey() ) );
	}

	@Test
	public void testNestedDoubleWithOrderBys() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1 DESC";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t2 DESC";
		Query query3 = new Query( queryString3 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );
		mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		qme = new QueryMappingEntry( query1.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t1 ASC"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1 ASC"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		//Lowest level query (when zipped) has 4 columns
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "rn0", (Object) new Long(3L) );
		row.put( "t1", (Object) new Long(3L) );
		row.put( "rn1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 4 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2 DESC) AS rn0 FROM ( SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER (ORDER BY t1 DESC) AS rn1 FROM ( SELECT t1 FROM T WHERE t2 < P0.s1 ORDER BY t1 DESC ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2 DESC) AS rn0 FROM ( SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER (ORDER BY t1 DESC) AS rn1 FROM ( SELECT t1 FROM T WHERE t2 < P0.s1 ORDER BY t1 DESC ) k ) z1 ON z0.rn0 = z1.rn1 ORDER BY rn0, rn1) I0) U( type0, u0, u1, u2, u3, u4, u5 ) ORDER BY ordKey0, type0, u3, u5" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );

		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// No order keys, RN0 CK
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// t1, t2, RN0, RN1
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 4 ) );
		// No additional order keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );

		/*
		 ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 
		---------+-------+----+----+----+----+----+----
			   1 |     0 |  1 |  1 |    |    |    |   
			   2 |     0 |  2 |  2 |    |    |    |   
			   2 |     1 |    |    |  1 |  1 | f  |  1
			   3 |     0 |  3 |  3 |    |    |    |   
			   3 |     1 |    |    |  2 |  1 | h  |  1
			   3 |     1 |    |    |  1 |  2 | f  |  2
		*/

		List<Map<String,Object>> rows = new LinkedList<>();

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 2L ) );
		row.put( "u1", (Object) new Long( 2L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new String( "f" ) );
		row.put( "u5", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 3L ) );
		row.put( "u1", (Object) new Long( 3L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new String( "h" ) );
		row.put( "u5", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new String( "f" ) );
		row.put( "u5", (Object) new Long( 2L ) );
		rows.add( row );

		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );
		assertThat( qResults.size(), equalTo( 3 ) );
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().size(), equalTo( 3 ) );

		assertThat( qResults.get( 1 ).size(), equalTo( 3 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().size(), equalTo( 0 ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().size(), equalTo( 2 ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 2L ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get( 1 ).get( "t2" ), equalTo( 1L ) );

		assertThat( qResults.get( 2 ).size(), equalTo( 3 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().size(), equalTo( 0 ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "f" ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "h" ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get( 1 ).get( "t1" ), equalTo( "f" ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( 3 ) );
		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );


		/*
        String queryString1 = "SELECT s1 FROM S";
        Query query1 = new Query( queryString1 );
        String queryString2 = "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1 DESC";
        Query query2 = new Query( queryString2 );
        String queryString3 = "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t2 DESC";
        Query query3 = new Query( queryString3 );
		*/

		Query q0 = new Query( "SELECT s1 FROM S" );
		assertThat( cacheKeys.size(), equalTo( 3 ) );
		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( q0.getCacheKey() ) );

		assertThat( cacheKeys.get( 1 ).size(), equalTo( 3 ) );
		Query q11 = new Query( "SELECT t2 FROM T WHERE t2 < 1 ORDER BY t2 DESC" );
		Query q12 = new Query( "SELECT t2 FROM T WHERE t2 < 2 ORDER BY t2 DESC" );
		Query q13 = new Query( "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t2 DESC" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q11.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( q12.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( q13.getCacheKey() ) );

		assertThat( cacheKeys.get( 2 ).size(), equalTo( 3 ) );
		Query q21 = new Query( "SELECT t1 FROM T WHERE t2 < 1 ORDER BY t1 DESC" );
		Query q22 = new Query( "SELECT t1 FROM T WHERE t2 < 2 ORDER BY t1 DESC" );
		Query q23 = new Query( "SELECT t1 FROM T WHERE t2 < 3 ORDER BY t1 DESC" );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( q21.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 1 ), equalTo( q22.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 2 ), equalTo( q23.getCacheKey() ) );



	}

	@Test
	public void testVectorizeNestedSinglesTwice(){
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t1 FROM T WHERE t2 = 3";
		Query query2 = new Query( queryString2 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t1 FROM T WHERE t2 = P0.s1"
		// Also selects 2 columns, so we can just reuse the result set
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t1 FROM T WHERE t2 = P0.s1 ) k) I0) U( type0, u0, u1, u2, u3 ) ORDER BY ordKey0, type0" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );


		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// No order keys, RN0 CK
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// t1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// No additional order keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );


		//Vectorize again, confirm we don't get an error from not having enough DB results (we should be able to use the cache).
		result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t1 FROM T WHERE t2 = P0.s1 ) k) I0) U( type0, u0, u1, u2, u3 ) ORDER BY ordKey0, type0" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );



		plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// No order keys, RN0 CK
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// t1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// No additional order keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );


	}

	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testVectorizeDoubleToNextLayer() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );

		String queryString2 = "SELECT s1 AS z FROM S";
		Query query2 = new Query( queryString2 );

		String queryString3 = "SELECT t1 FROM T WHERE t2 = 3 OR t2 = 4";
		Query query3 = new Query( queryString3 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

        mappings = HashMultimap.create();
		mappings.put( 1, "z" );
		qme = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		// First query we check is "SELECT s2 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s2", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		//Next query we check is "SELECT s1 FROM S"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query is "SELECT t1 FROM T WHERE t2 = 3 OR t2 = 4"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query is "SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t1 FROM T WHERE t2 = P0.s1 OR t2 = P0.s1 ) k"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "11", (Object) new Long(3L) );
		row.put( "rn", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, P0.rn1 AS ordKey1, P0.rn0 AS ordKey2, P0.rn1 AS ordKey3, U.* FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS z FROM S ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS z FROM S ) k ) z1 ON z0.rn0 = z1.rn1 ORDER BY rn0, rn1 ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t1 FROM T WHERE t2 = P0.s1 OR t2 = P0.z ) k) I0) U( type0, u0, u1, u2, u3, u4, u5 ) ORDER BY ordKey0, ordKey1, ordKey2, ordKey3, type0" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );



		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();
		// s1, RN0, s2, RN1
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 4 ) );
		// order keys (RN0/RN1) and CK: RN0/RN1
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 4 ) );
		// t1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// No additional order keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 0 ) );
	}

	@Test
	public void testNestedNestedTwoLayerPassdownQuery() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT s1 FROM S WHERE s1 = 3";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT s1,s2 FROM S WHERE s1 = 1 OR s1 = 3";
		Query query3 = new Query( queryString3 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		qme = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 1, "s1" );
		qme = new QueryMappingEntry( query1.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );


		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Third query we check is "SELECT s1,s2 FROM S WHERE s1 = 3 OR s1 = 3"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s11", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT P1.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = P0.s1 ) k ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I1.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s2 FROM S WHERE s1 = P1.s1 OR s1 = P0.s1 ) k) I1) U( type1, u0, u1, u2, u3, u4 ) ORDER BY ordKey0, type1) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8 ) ORDER BY ordKey0, type0, u2, u3" ) );

		assertThat( result.getTables().size(), equalTo( 1 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );

		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// CK: RN0
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 1 ) );
		// s1, s1, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 2 ), equalTo( 3 ) );
		// Base Level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 2 ), equalTo( 0 ) );

		//  ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 | u6 | u7 | u8 
		//---------+-------+----+----+----+----+----+----+----+----+----
		//       1 |     0 |  1 |  1 |    |    |    |    |    |    |   
		//       1 |     1 |    |    |  1 |  0 |  1 |  1 |    |    |   
		//       1 |     1 |    |    |  1 |  1 |    |    |  1 |  D |  1
		//       2 |     0 |  2 |  2 |    |    |    |    |    |    |   
		//       2 |     1 |    |    |  1 |  0 |  2 |  1 |    |    |   
		//       2 |     1 |    |    |  1 |  1 |    |    |  2 |  E |  1
		//       2 |     1 |    |    |  1 |  1 |    |    |  1 |  D |  2
		//       3 |     0 |  3 |  3 |    |    |    |    |    |    |   
		//       3 |     1 |    |    |  1 |  0 |  3 |  1 |    |    |   
		//       3 |     1 |    |    |  1 |  1 |    |    |  3 |  F |  1
		//       3 |     1 |    |    |  1 |  1 |    |    |  2 |  E |  2
		//       3 |     1 |    |    |  1 |  1 |    |    |  1 |  D |  3

		List<Map<String,Object>> rows = new LinkedList<>();

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 0 ) );
		row.put( "u4", (Object) new Long( 1L ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", (Object) new String( "D" ) );
		row.put( "u8", (Object) new Long( 1L ));
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 2L ) );
		row.put( "u1", (Object) new Long( 2L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 0 ) );
		row.put( "u4", (Object) new Long( 2L ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 2L ) );
		row.put( "u7", (Object) new String( "E" ) );
		row.put( "u8", (Object) new Long( 1L ));
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", (Object) new String( "D" ) );
		row.put( "u8", (Object) new Long( 2L ));
		rows.add( row );


		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 3L ) );
		row.put( "u1", (Object) new Long( 3L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 0 ) );
		row.put( "u4", (Object) new Long( 3L ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 3L ) );
		row.put( "u7", (Object) new String( "F" ) );
		row.put( "u8", (Object) new Long( 1L ));
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 2L ) );
		row.put( "u7", (Object) new String( "E" ) );
		row.put( "u8", (Object) new Long( 2L ));
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Integer( 1 ) );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", (Object) new String( "D" ) );
		row.put( "u8", (Object) new Long( 3L ));
		rows.add( row );

		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );

		assertThat( qResults.size(), equalTo( 3 ) );
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().size(), equalTo( 3 ) );
		assertThat( qResults.get( 1 ).size(), equalTo( 3 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().get(0).size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().get(0).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().get(0).get( "s1" ), equalTo( 2L ) );
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().get(0).size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get(0).size(), equalTo( 1 ) );
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get(0).get( "s1" ), equalTo( 3L ) );
		assertThat( qResults.get( 2 ).size(), equalTo( 3 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().size(), equalTo( 1 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get(0).size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get(0).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().get(0).get( "s2" ), equalTo( "D" ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(0).size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(0).get( "s1" ), equalTo( 2L ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(0).get( "s2" ), equalTo( "E" ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(1).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().get(1).get( "s2" ), equalTo( "D" ) );

		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().size(), equalTo( 3 ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(0).size(), equalTo( 2 ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(0).get( "s1" ), equalTo( 3L ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(0).get( "s2" ), equalTo( "F" ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(1).get( "s1" ), equalTo( 2L ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(1).get( "s2" ), equalTo( "E" ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(2).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get(2).get( "s2" ), equalTo( "D" ) );



		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( 3 ) );

		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		Query q0 = new Query( "SELECT s1 FROM S" );

		assertThat( cacheKeys.get( 1 ).size(), equalTo( 3 ) );
		Query q11 = new Query( "SELECT s1 FROM S WHERE s1 = 1" );
		Query q12 = new Query( "SELECT s1 FROM S WHERE s1 = 2" );
		Query q13 = new Query( "SELECT s1 FROM S WHERE s1 = 3" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q11.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( q12.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( q13.getCacheKey() ) );

		//  ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 | u6 | u7 | u8 
		//---------+-------+----+----+----+----+----+----+----+----+----
		//       1 |     0 |  1 |  1 |    |    |    |    |    |    |   
		//       1 |     1 |    |    |  1 |  0 |  1 |  1 |    |    |   
		//       1 |     1 |    |    |  1 |  1 |    |    |  1 |  D |  1
		//       2 |     0 |  2 |  2 |    |    |    |    |    |    |   
		//       2 |     1 |    |    |  1 |  0 |  2 |  1 |    |    |   
		//       2 |     1 |    |    |  1 |  1 |    |    |  2 |  E |  1
		//       2 |     1 |    |    |  1 |  1 |    |    |  1 |  D |  2
		//       3 |     0 |  3 |  3 |    |    |    |    |    |    |   
		//       3 |     1 |    |    |  1 |  0 |  3 |  1 |    |    |   
		//       3 |     1 |    |    |  1 |  1 |    |    |  3 |  F |  1
		//       3 |     1 |    |    |  1 |  1 |    |    |  2 |  E |  2
		//       3 |     1 |    |    |  1 |  1 |    |    |  1 |  D |  3



		assertThat( cacheKeys.get( 2 ).size(), equalTo( 3 ) );
		Query q21 = new Query( "SELECT s1,s2 FROM S WHERE s1 = 1 OR s1 = 1" );
		Query q22 = new Query( "SELECT s1,s2 FROM S WHERE s1 = 2 OR s1 = 2" );
		Query q23 = new Query( "SELECT s1,s2 FROM S WHERE s1 = 3 OR s1 = 3" );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( q21.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 1 ), equalTo( q22.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 2 ), equalTo( q23.getCacheKey() ) );

	}

	@Test
	public void testNestedNestedDoublePassdownWithMiddleEntries() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t2 DESC";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT t1, t2 FROM T WHERE t2 < 3 OR t2 < 3 ORDER BY t2 ASC";
		Query query3 = new Query( queryString3 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 0, "t2" );
		qme = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 1, "s1" );
		qme = new QueryMappingEntry( query1.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC )"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Next query we check is "SELECT t1, t2 FROM T WHERE t2 < P1.t2 ORDER BY t2 ASC"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "t1", (Object) new Long(3L) );
		row.put( "t2", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT P1.t2 AS ordKey0, P1.rn0 AS ordKey1, U.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2 DESC) AS rn0 FROM ( SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC ) k ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I1.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY t2 ASC) AS rn0 FROM ( SELECT t1, t2 FROM T WHERE t2 < P1.t2 OR t2 < P0.s1 ORDER BY t2 ASC ) k) I1) U( type1, u0, u1, u2, u3, u4 ) ORDER BY ordKey0 DESC, ordKey1, type1, u3 ASC) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9 ) ORDER BY ordKey0, type0, u2 DESC, u3, u4, u8 ASC" ) );

		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );
		assertThat( result.getTables().contains( "T" ), equalTo( true ) );


		LateralUnionVectorizationPlan plan = (LateralUnionVectorizationPlan) result.getQueryVectorizationPlan();

		// s1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 0 ), equalTo( 2 ) );
		// No order keys, RN0 CK
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 0 ), equalTo( 1 ) );
		// t1/RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// Order on t2, RN0 CK
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 1 ), equalTo( 2 ) );
		// t1, t2, RN0
		assertThat( plan.getNumColsAtTopologicalLevel( 2 ), equalTo( 3 ) );
		// No additional keys, base level
		assertThat( plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( 2 ), equalTo( 0 ) );

		/*
		 ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 | u6 | u7 | u8 | u9 
		---------+-------+----+----+----+----+----+----+----+----+----+----
			   1 |     0 |  1 |  1 |    |    |    |    |    |    |    |   
			   2 |     0 |  2 |  2 |    |    |    |    |    |    |    |   
			   2 |     1 |    |    |  1 |  1 |  0 |  1 |  1 |    |    |   
			   3 |     0 |  3 |  3 |    |    |    |    |    |    |    |   
			   3 |     1 |    |    |  2 |  1 |  0 |  2 |  1 |    |    |   
			   3 |     1 |    |    |  2 |  1 |  1 |    |    | f  |  1 |  1
			   3 |     1 |    |    |  1 |  2 |  0 |  1 |  2 |    |    |   
			   4 |     0 |  4 |  4 |    |    |    |    |    |    |    |   
			   4 |     1 |    |    |  3 |  1 |  0 |  3 |  1 |    |    |   
			   4 |     1 |    |    |  3 |  1 |  1 |    |    | f  |  1 |  1
			   4 |     1 |    |    |  3 |  1 |  1 |    |    | h  |  2 |  2
			   4 |     1 |    |    |  2 |  2 |  0 |  2 |  2 |    |    |   
			   4 |     1 |    |    |  2 |  2 |  1 |    |    | f  |  1 |  1
			   4 |     1 |    |    |  1 |  3 |  0 |  1 |  3 |    |    |   
		*/

		List<Map<String,Object>> rows = new LinkedList<>();

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 1L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 1L ) );
		row.put( "u1", (Object) new Long( 1L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 2L ) );
		row.put( "u1", (Object) new Long( 2L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );


		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 2L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 3L ) );
		row.put( "u1", (Object) new Long( 3L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 2L ) );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "f" ) );
		row.put( "u8", (Object) new Long( 1L ) );
		row.put( "u9", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 3L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", (Object) new Long( 2L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 0 ) );
		row.put( "u0", (Object) new Long( 4L ) );
		row.put( "u1", (Object) new Long( 4L ) );
		row.put( "u2", null );
		row.put( "u3", null );
		row.put( "u4", null );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 3L ) );
		row.put( "u6", (Object) new Long( 1L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "f" ) );
		row.put( "u8", (Object) new Long( 1L ) );
		row.put( "u9", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 3L ) );
		row.put( "u3", (Object) new Long( 1L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "h" ) );
		row.put( "u8", (Object) new Long( 2L ) );
		row.put( "u9", (Object) new Long( 2L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 2L ) );
		row.put( "u6", (Object) new Long( 2L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 2L ) );
		row.put( "u3", (Object) new Long( 2L ) );
		row.put( "u4", (Object) new Integer( 1 ) );
		row.put( "u5", null );
		row.put( "u6", null );
		row.put( "u7", (Object) new String( "f" ) );
		row.put( "u8", (Object) new Long( 1L ) );
		row.put( "u9", (Object) new Long( 1L ) );
		rows.add( row );

		row = new HashMap<>();
		row.put( "ordKey0", (Object) new Long( 4L ) );
		row.put( "type0", (Object) new Integer( 1 ) );
		row.put( "u0", null );
		row.put( "u1", null );
		row.put( "u2", (Object) new Long( 1L ) );
		row.put( "u3", (Object) new Long( 3L ) );
		row.put( "u4", (Object) new Integer( 0 ) );
		row.put( "u5", (Object) new Long( 1L ) );
		row.put( "u6", (Object) new Long( 3L ) );
		row.put( "u7", null );
		row.put( "u8", null );
		row.put( "u9", null );
		rows.add( row );

		QueryResult combinedResultSet = new QueryResult( rows, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ) );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( combinedResultSet, plan );

		assertThat( qResults.size(), equalTo( 3 ) );

		// First query is "SELECT s1 FROM S"
		// Next query is "SELECT t2 FROM T WHERE t2 < P0.s1 ORDER BY t2 DESC )"
		// Next query is "SELECT t1, t2 FROM T WHERE t2 < P1.t2 ORDER BY t2 ASC"
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().size(), equalTo( 4 ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 0 ).get( "s1" ), equalTo( 1L ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 1 ).get( "s1" ), equalTo( 2L ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 2 ).get( "s1" ), equalTo( 3L ) );
		assertThat( qResults.get( 0 ).get( 0 ).getSelectResult().get( 3 ).get( "s1" ), equalTo( 4L ) );

		// 1 Result for each result in q1
		assertThat( qResults.get( 1 ).size(), equalTo( 4 ) );
		assertThat( qResults.get( 1 ).get( 0 ).getSelectResult().isEmpty(), equalTo( true ) ); //nothing < 1
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().size(), equalTo( 1 ) ); // one thing < 2
		assertThat( qResults.get( 1 ).get( 1 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); // one thing < 2
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().size(), equalTo( 2 ) ); //two things < 3
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 2L ) ); // two things < 2
		assertThat( qResults.get( 1 ).get( 2 ).getSelectResult().get( 1 ).get( "t2" ), equalTo( 1L ) ); // two things < 2
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().size(), equalTo( 3 ) ); //three things < 3
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 3L ) ); // three things < 3
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().get( 1 ).get( "t2" ), equalTo( 2L ) ); // three things < 3
		assertThat( qResults.get( 1 ).get( 3 ).getSelectResult().get( 2 ).get( "t2" ), equalTo( 1L ) ); // three things < 3

		// 1 (NULL) + 1 + 2 + 3 = 7
		assertThat( qResults.get( 2 ).size(), equalTo( 7 ) );
		assertThat( qResults.get( 2 ).get( 0 ).getSelectResult().isEmpty(), equalTo( true ) ); //Nothing here because nothing matches q2's NULL here
		assertThat( qResults.get( 2 ).get( 1 ).getSelectResult().isEmpty(), equalTo( true ) ); //Nothing here because nothing < 1
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().size(), equalTo( 1 ) ); // One thing here
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); //One thing here
		assertThat( qResults.get( 2 ).get( 2 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "f" ) ); //One thing here
		assertThat( qResults.get( 2 ).get( 3 ).getSelectResult().isEmpty(), equalTo( true ) ); // Nothing here because nothing < 1 
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().size(), equalTo( 2 ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "f" ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 1 ).get( "t2" ), equalTo( 2L ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 4 ).getSelectResult().get( 1 ).get( "t1" ), equalTo( "h" ) ); // Two rows < 3
		assertThat( qResults.get( 2 ).get( 5 ).getSelectResult().size(), equalTo( 1 ) ); // One row < 2 
		assertThat( qResults.get( 2 ).get( 5 ).getSelectResult().get( 0 ).get( "t2" ), equalTo( 1L ) ); // One row < 2
		assertThat( qResults.get( 2 ).get( 5 ).getSelectResult().get( 0 ).get( "t1" ), equalTo( "f" ) ); // One row < 2
		assertThat( qResults.get( 2 ).get( 6 ).getSelectResult().isEmpty(), equalTo( true ) ); // No rows < 1

		/*
		 ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 | u6 | u7 | u8 | u9 
		---------+-------+----+----+----+----+----+----+----+----+----+----
			   1 |     0 |  1 |  1 |    |    |    |    |    |    |    |   
			   2 |     0 |  2 |  2 |    |    |    |    |    |    |    |   
			   2 |     1 |    |    |  1 |  1 |  0 |  1 |  1 |    |    |   
			   3 |     0 |  3 |  3 |    |    |    |    |    |    |    |   
			   3 |     1 |    |    |  2 |  1 |  0 |  2 |  1 |    |    |   
			   3 |     1 |    |    |  2 |  1 |  1 |    |    | f  |  1 |  1
			   3 |     1 |    |    |  1 |  2 |  0 |  1 |  2 |    |    |   
			   4 |     0 |  4 |  4 |    |    |    |    |    |    |    |   
			   4 |     1 |    |    |  3 |  1 |  0 |  3 |  1 |    |    |   
			   4 |     1 |    |    |  3 |  1 |  1 |    |    | f  |  1 |  1
			   4 |     1 |    |    |  3 |  1 |  1 |    |    | h  |  2 |  2
			   4 |     1 |    |    |  2 |  2 |  0 |  2 |  2 |    |    |   
			   4 |     1 |    |    |  2 |  2 |  1 |    |    | f  |  1 |  1
			   4 |     1 |    |    |  1 |  3 |  0 |  1 |  3 |    |    |   
		*/

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( 3 ) );
		assertThat( cacheKeys.get( 0 ).size(), equalTo( 1 ) );
		Query q0 = new Query( "SELECT s1 FROM S" );
		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( q0.getCacheKey() ) );
		
		assertThat( cacheKeys.get( 1 ).size(), equalTo( 4 ) );
		Query q11 = new Query( "SELECT t2 FROM T WHERE t2 < 1 ORDER BY t2 DESC" );
		Query q12 = new Query( "SELECT t2 FROM T WHERE t2 < 2 ORDER BY t2 DESC" );
		Query q13 = new Query( "SELECT t2 FROM T WHERE t2 < 3 ORDER BY t2 DESC" );
		Query q14 = new Query( "SELECT t2 FROM T WHERE t2 < 4 ORDER BY t2 DESC" );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( q11.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( q12.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( q13.getCacheKey() ) );
		assertThat( cacheKeys.get( 1 ).get( 3 ), equalTo( q14.getCacheKey() ) );

		//String queryString3 = "SELECT t1, t2 FROM T WHERE t2 < 3 OR t2 < 3 ORDER BY t2 ASC";
		assertThat( cacheKeys.get( 2 ).size(), equalTo( 7 ) );
		Query q21 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 1 OR t2 < 2 ORDER BY t2 ASC" );
		Query q22 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 2 OR t2 < 3 ORDER BY t2 ASC" );
		Query q23 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 1 OR t2 < 3 ORDER BY t2 ASC" );
		Query q24 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 3 OR t2 < 4 ORDER BY t2 ASC" );
		Query q25 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 2 OR t2 < 4 ORDER BY t2 ASC" );
		Query q26 = new Query( "SELECT t1, t2 FROM T WHERE t2 < 1 OR t2 < 4 ORDER BY t2 ASC" );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( null ) );
		assertThat( cacheKeys.get( 2 ).get( 1 ), equalTo( q21.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 2 ), equalTo( q22.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 3 ), equalTo( q23.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 4 ), equalTo( q24.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 5 ), equalTo( q25.getCacheKey() ) );
		assertThat( cacheKeys.get( 2 ).get( 6 ), equalTo( q26.getCacheKey() ) );
	}

	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testOverlappingColumnNamesInZipperPassdown() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S WHERE s1 = 3 AND s1 = 3";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT s1 FROM S WHERE s1 = 4";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT s1,s2 FROM S WHERE s1 = 3 OR s1 = 4";
		Query query3 = new Query( queryString3 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 1, "s1" );
		qme = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Third query we check is "SELECT s1,s2 FROM S WHERE s1 = 3"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s11", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, P0.rn1 AS ordKey1, P0.rn0 AS ordKey2, P0.rn1 AS ordKey3, U.* FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 3 AND s1 = 3 ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 3 AND s1 = 3 ) k ) z1 ON z0.rn0 = z1.rn1 ORDER BY rn0, rn1 ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s2 FROM S WHERE s1 = P0.s1_0 OR s1 = P0.s1 ) k) I0) U( type0, u0, u1, u2, u3, u4, u5, u6 ) ORDER BY ordKey0, ordKey1, ordKey2, ordKey3, type0" ) );

		assertThat( result.getTables().size(), equalTo( 1 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );

	}

	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testOverlappingColumnNamesInThreeZipperPassdown() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S WHERE s1 = 3 AND s1 = 3";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT s1 FROM S WHERE s1 = 4";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT s1 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5";
		Query query3 = new Query( queryString3 );

		String queryString4 = "SELECT s1,s2 FROM S WHERE s1 = 3 OR s1 = 4 OR s1 = 5";
		Query query4 = new Query( queryString4 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );
		dependencyGraph.addBaseQuery( query3.getId(), query3 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 1, "s1" );
		qme = new QueryMappingEntry( query2.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 2, "s1" );
		qme = new QueryMappingEntry( query3.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme );


		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Third query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Fourth query we check is "SELECT s1,s2 FROM S WHERE s1 = 3"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s11", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, P0.rn1 AS ordKey1, P0.rn3 AS ordKey2, P0.rn0 AS ordKey3, P0.rn1 AS ordKey4, P0.rn3 AS ordKey5, U.* FROM ( SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 AS s1_0_0 FROM S WHERE s1 = 3 AND s1 = 3 ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 AS s1_0_0 FROM S WHERE s1 = 3 AND s1 = 3 ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ORDER BY rn0, rn1, rn3 ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s2 FROM S WHERE s1 = P0.s1_0_0 OR s1 = P0.s1 OR s1 = P0.s1_0 ) k) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8 ) ORDER BY ordKey0, ordKey1, ordKey2, ordKey3, ordKey4, ordKey5, type0" ) );

		assertThat( result.getTables().size(), equalTo( 1 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );

	}

	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testOverlappingColumnNamesInThreeZipperPassdownMultiLevel() {
		TestDB db = new TestDB();
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();

		String queryString1 = "SELECT s1 FROM S WHERE s1 = 3 AND s1 = 3";
		Query query1 = new Query( queryString1 );
		String queryString2 = "SELECT s1 FROM S WHERE s1 = 4";
		Query query2 = new Query( queryString2 );
		String queryString3 = "SELECT s1 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5";
		Query query3 = new Query( queryString3 );

		String queryString4 = "SELECT s1,s2 FROM S WHERE s1 = 3 OR s1 = 4 OR s1 = 5";
		Query query4 = new Query( queryString4 );

		String queryString5 = "SELECT s1,s2 FROM S WHERE s1 = 3 OR s1 = 4 OR s1 = 5 OR s1 = 6";
		Query query5 = new Query( queryString5 );


		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );
		dependencyGraph.addBaseQuery( query2.getId(), query2 );
		dependencyGraph.addBaseQuery( query3.getId(), query3 );

        Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		QueryMappingEntry qme = new QueryMappingEntry( query1.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 1, "s1" );
		qme = new QueryMappingEntry( query2.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 2, "s1" );
		qme = new QueryMappingEntry( query3.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 0, "s1" );
		qme = new QueryMappingEntry( query1.getId(), query5, mappings );
		dependencyGraph.addDependencyForQuery( query5.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 1, "s1" );
		qme = new QueryMappingEntry( query2.getId(), query5, mappings );
		dependencyGraph.addDependencyForQuery( query5.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 2, "s1" );
		qme = new QueryMappingEntry( query3.getId(), query5, mappings );
		dependencyGraph.addDependencyForQuery( query5.getId(), qme );

		mappings = HashMultimap.create();
		mappings.put( 3, "s1" );
		qme = new QueryMappingEntry( query4.getId(), query5, mappings );
		dependencyGraph.addDependencyForQuery( query5.getId(), qme );

		// First query we check is "SELECT s1 FROM S"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Third query we check is "SELECT s1 FROM S WHERE s1 = 3"
		// Same result set size
		db.addResultSet( resultToStore );

		// Fourth query we check is "SELECT s1,s2 FROM S WHERE s1 = 3"
		resultSet = new LinkedList<>();
		row = new HashMap<>();
		row.put( "s1", (Object) new Long(3L) );
		row.put( "s11", (Object) new Long(3L) );
		resultSet.add( row );
		resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 2 );
		db.addResultSet( resultToStore );

		//Fifth query has same # of columns
		db.addResultSet( resultToStore );

		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, P0.rn1 AS ordKey1, P0.rn3 AS ordKey2, P0.rn0 AS ordKey3, P0.rn1 AS ordKey4, P0.rn3 AS ordKey5, U.* FROM ( SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 AS s1_0_0 FROM S WHERE s1 = 3 AND s1 = 3 ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1 FROM S WHERE s1 = 4 ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s1 AS s1_0 FROM S WHERE s1 = 5 AND s1 = 5 AND s1 = 5 ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT s1 AS s1_0_0 FROM S WHERE s1 = 3 AND s1 = 3 ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ORDER BY rn0, rn1, rn3 ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, NULL, NULL, I0.* FROM ( SELECT P1.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s2 FROM S WHERE s1 = P0.s1_0_0 OR s1 = P0.s1 OR s1 = P0.s1_0 ) k ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, I1.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s1,s2 FROM S WHERE s1 = P0.s1_0_0 OR s1 = P0.s1 OR s1 = P0.s1_0 OR s1 = P1.s1 ) k) I1) U( type1, u0, u1, u2, u3, u4, u5 ) ORDER BY ordKey0, type1) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13 ) ORDER BY ordKey0, ordKey1, ordKey2, ordKey3, ordKey4, ordKey5, type0, u6, u7" ) );

		assertThat( result.getTables().size(), equalTo( 1 ) );
		assertThat( result.getTables().contains( "S" ), equalTo( true ) );

	}

	@Test
	public void testConstructSpeculativeLoopWorks() throws Exception {

		TestDB db = new TestDB();
		String queryString = "SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 4300000872";
		String queryString1 = "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'ABFS'";
		String queryString2 = "SELECT s_num_out FROM security WHERE s_symb = 'ABFS'";
		String queryString3 = "SELECT dm_close FROM daily_market WHERE dm_s_symb = 'ABFS' AND dm_date = '2003-10-10'";

		AntlrParser p = new AntlrParser();
		Query query = new Query( queryString,  p.buildParseTree(queryString) );
		Query query1 = new Query( queryString1, p.buildParseTree(queryString1) );
		Query query2 = new Query( queryString2, p.buildParseTree(queryString2) );
		Query query3 = new Query( queryString3, p.buildParseTree(queryString3) );

		Multimap<Integer, String> mapping = HashMultimap.create();
		mapping.put( 0, "symb" );

		DependencyGraph loopDependencyGraph = new DependencyGraph();
		loopDependencyGraph.addBaseQuery( query.getId(), query );
		QueryMappingEntry qme1 = new QueryMappingEntry( query.getId(), query1, mapping );
		QueryMappingEntry qme2 = new QueryMappingEntry( query.getId(), query2, mapping );
		QueryMappingEntry qme3 = new QueryMappingEntry( query.getId(), query3, mapping );
		loopDependencyGraph.addDependencyForQuery( query1.getId(), qme1 );
		loopDependencyGraph.addDependencyForQuery( query2.getId(), qme2 );
		loopDependencyGraph.addDependencyForQuery( query3.getId(), qme3 );
		loopDependencyGraph.markQueryAsBaseQuery( query3.getId(), query3.getQueryString() );

		assertThat( loopDependencyGraph.isSimpleVectorizable(), equalTo( false ) );
		assertThat( loopDependencyGraph.isVectorizable(), equalTo( true ) );

		// First query we check is "SELECT wi_s_symb as symb FROM watch_item..."
		List<Map<String, Object>> resultSet = new LinkedList<>();
		Map<String, Object> row = new HashMap<>();
		row.put( "symb", (Object) new Long(3L) );
		resultSet.add( row );
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is ""SELECT lt_price FROM last_trade WHERE lt_s_symb = 'ABFS'
		// Same result set size
		db.addResultSet( resultToStore );

		// Third query we check is "SELECT s_num_out FROM security WHERE s_symb = 'ABFS'"
		// Same result set size
		db.addResultSet( resultToStore );

		// Fourth query we check is "SELECT dm_close FROM daily_market WHERE dm_s_symb = 'ABFS' AND dm_date = '2003-10-10'"
		// Same result set size
		db.addResultSet( resultToStore );

		// Add the found loop
		VectorizableType vType = new VectorizableType();
		vType.markAsLoopBaseQuery();

		Vectorizable specLoop = new Vectorizable( loopDependencyGraph,
				query.getId(), query.getQueryString(), vType );

		assertThat( specLoop.getTriggerQueryId(), equalTo( query.getId() ) );

		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		QueryVectorizerResult vectorizerResult = vectorizer.vectorize( 0, specLoop, db );
		QueryVectorizationPlan plan = vectorizerResult.getQueryVectorizationPlan();
		assertThat( plan.size(), equalTo( 4 ) );
		assertThat( vectorizerResult.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT wi_s_symb as symb FROM watch_item, watch_list WHERE wi_wl_id = wl_id AND wl_c_id = 4300000872 ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s_num_out FROM security WHERE s_symb = P0.symb ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT dm_close FROM daily_market WHERE dm_s_symb = P0.symb AND dm_date = '2003-10-10' ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s_num_out FROM security WHERE s_symb = P0.symb ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT dm_close FROM daily_market WHERE dm_s_symb = P0.symb AND dm_date = '2003-10-10' ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT lt_price FROM last_trade WHERE lt_s_symb = P0.symb ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 UNION SELECT * FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s_num_out FROM security WHERE s_symb = P0.symb ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT dm_close FROM daily_market WHERE dm_s_symb = P0.symb AND dm_date = '2003-10-10' ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s_num_out FROM security WHERE s_symb = P0.symb ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT dm_close FROM daily_market WHERE dm_s_symb = P0.symb AND dm_date = '2003-10-10' ) k ) z1 ON z0.rn0 = z1.rn1 ) z2 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn3 FROM ( SELECT lt_price FROM last_trade WHERE lt_s_symb = P0.symb ) k ) z3 ON z2.rn0 = z3.rn3 OR z2.rn1 = z3.rn3 ORDER BY rn0, rn1, rn3) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7 ) ORDER BY ordKey0, type0, u3, u5, u7" ) );

		assertThat( vectorizerResult.getTables().size(), equalTo( 5 ) );
		assertThat( vectorizerResult.getTables().contains( "watch_item" ), equalTo( true ) );
		assertThat( vectorizerResult.getTables().contains( "watch_list" ), equalTo( true ) );
		assertThat( vectorizerResult.getTables().contains( "last_trade" ), equalTo( true ) );
		assertThat( vectorizerResult.getTables().contains( "security" ), equalTo( true ) );
		assertThat( vectorizerResult.getTables().contains( "daily_market" ), equalTo( true ) );

	}

	@Test
	public void testSplitTPCEResultSetBug() throws Exception {

		// This is from the customer position transaction, with the last query being a holdover from some subsequent txn.
		TestDB db = new TestDB();
		String queryString = "SELECT c_id FROM customer WHERE c_tax_id = '338KX9036NW568'";
		String queryString1 = "SELECT ca_id, ca_bal, COALESCE(sum(hs_qty * lt_price), 0) as asset_total FROM customer_account LEFT OUTER JOIN holding_summary ON hs_ca_id = ca_id, last_trade WHERE ca_c_id = 4300000871 AND lt_s_symb = hs_s_symb GROUP BY ca_id, ca_bal ORDER BY asset_total ASC LIMIT 10";
		String queryString2 = "SELECT t_id, t_s_symb, t_qty, st_name, th_dts FROM ( SELECT t_id as id FROM trade WHERE t_ca_id = 43000008701 ORDER BY t_dts DESC LIMIT 10 ) T, trade, trade_history, status_type WHERE t_id = id AND th_t_id = t_id AND st_id = th_st_id ORDER BY th_dts DESC LIMIT 30";
		String queryString3 = "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'ACTR'";

		AntlrParser p = new AntlrParser();
		Query query0 = new Query( queryString,  p.buildParseTree(queryString) );
		Query query1 = new Query( queryString1, p.buildParseTree(queryString1) );
		Query query2 = new Query( queryString2, p.buildParseTree(queryString2) );
		Query query3 = new Query( queryString3, p.buildParseTree(queryString3) );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query0.getId(), query0 );
		Multimap<Integer, String> mapping = HashMultimap.create();
		mapping.put( 0, "c_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query0.getId(), query1, mapping );
		dependencyGraph.addDependencyForQuery( query1.getId(), qme1 );

		mapping = HashMultimap.create();
		mapping.put( 0, "ca_id" );
		QueryMappingEntry qme2 = new QueryMappingEntry( query1.getId(), query2, mapping );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme2 );

		mapping = HashMultimap.create();
		mapping.put( 0, "t_s_symb" );
		QueryMappingEntry qme3 = new QueryMappingEntry( query2.getId(), query3, mapping );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme3 );

		// First query we check is "SELECT c_id FROM customer WHERE c_tax_id = '338KX9036NW568'"
		List<Map<String, Object>> resultSet = new LinkedList<>();
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT ca_id, ca_bal, COALESCE(sum(hs_qty * lt_price), 0) as asset_total FROM customer_account LEFT OUTER JOIN holding_summary ON hs_ca_id = ca_id, last_trade WHERE ca_c_id = 4300000871 AND lt_s_symb = hs_s_symb GROUP BY ca_id, ca_bal ORDER BY asset_total ASC LIMIT 10"
		resultSet = new LinkedList<>();
        resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 3 );
        db.addResultSet( resultToStore );

		// Third query we check is "SELECT t_id, t_s_symb, t_qty, st_name, th_dts FROM ( SELECT t_id as id FROM trade WHERE t_ca_id = 43000008701 ORDER BY t_dts DESC LIMIT 10 ) T, trade, trade_history, status_type WHERE t_id = id AND th_t_id = t_id AND st_id = th_st_id ORDER BY th_dts DESC LIMIT 30"
		resultSet = new LinkedList<>();
        resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 5 );
		db.addResultSet( resultToStore );

		// Fourth query we check is "SELECT lt_price FROM last_trade WHERE lt_s_symb = 'ACTR'"
		resultSet = new LinkedList<>();
        resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		assertThat( dependencyGraph.isVectorizable(), equalTo( true ) );
		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );
		assertThat( result.getVectorizedQueryText(), equalTo( "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT c_id FROM customer WHERE c_tax_id = '338KX9036NW568' ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT P1.asset_total AS ordKey0, P1.rn0 AS ordKey1, U.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY asset_total ASC) AS rn0 FROM ( SELECT ca_id, ca_bal, COALESCE(sum(hs_qty * lt_price), 0) as asset_total FROM customer_account LEFT OUTER JOIN holding_summary ON hs_ca_id = ca_id, last_trade WHERE ca_c_id = P0.c_id AND lt_s_symb = hs_s_symb GROUP BY ca_id, ca_bal ORDER BY asset_total ASC LIMIT 10 ) k ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, I1.* FROM ( SELECT P2.th_dts AS ordKey0, P2.rn0 AS ordKey1, U.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY th_dts DESC) AS rn0 FROM ( SELECT t_id, t_s_symb, t_qty, st_name, th_dts FROM ( SELECT t_id as id FROM trade WHERE t_ca_id = P1.ca_id ORDER BY t_dts DESC LIMIT 10 ) T, trade, trade_history, status_type WHERE t_id = id AND th_t_id = t_id AND st_id = th_st_id ORDER BY th_dts DESC LIMIT 30 ) k ) P2, LATERAL ( SELECT 0, P2.*, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, NULL, NULL, I2.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT lt_price FROM last_trade WHERE lt_s_symb = P2.t_s_symb ) k) I2) U( type2, u0, u1, u2, u3, u4, u5, u6, u7 ) ORDER BY ordKey0 DESC, ordKey1, type2) I1) U( type1, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14 ) ORDER BY ordKey0 ASC, ordKey1, type1, u4 DESC, u5, u6) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16, u17, u18, u19 ) ORDER BY ordKey0, type0, u2 ASC, u3, u4, u9 DESC, u10, u11" ) );
	}


	@Ignore( "Ignored because we don't vectorize base queries with no deps at non-zero topological position" )
	@Test
	public void testSplitTPCEResultSetBug2() throws Exception {
		// This is from the customer position transaction, with the last query being a holdover from some subsequent txn.
		TestDB db = new TestDB();
		String queryString = "SELECT co_id FROM company WHERE co_name = 'Abercrombie & Fitch'";
		String queryString1 = "SELECT t_ca_id, t_tt_id, t_s_symb, t_qty, t_chrg, t_lifo, t_is_cash FROM trade WHERE t_id = 200000087266122";
		String queryString2 = "SELECT s_ex_id, s_name, s_symb FROM security WHERE s_co_id = 4300000038 AND s_issue = 'COMMON'";
		String queryString3 = "SELECT h_t_id, h_qty, h_price, h_dts FROM holding WHERE h_ca_id = 43000044602 AND h_s_symb = 'ANF' ORDER BY h_dts ASC";

		AntlrParser p = new AntlrParser();
		Query query0 = new Query( queryString,  p.buildParseTree(queryString) );
		Query query1 = new Query( queryString1, p.buildParseTree(queryString1) );
		Query query2 = new Query( queryString2, p.buildParseTree(queryString2) );
		Query query3 = new Query( queryString3, p.buildParseTree(queryString3) );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query0.getId(), query0 );
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		Multimap<Integer, String> mapping = HashMultimap.create();
		mapping.put( 0, "co_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query0.getId(), query2, mapping );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme1 );

		mapping = HashMultimap.create();
		mapping.put( 0, "ca_id" );
		QueryMappingEntry qme2 = new QueryMappingEntry( query1.getId(), query3, mapping );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme2 );

		mapping = HashMultimap.create();
		mapping.put( 1, "s_symb" );
		QueryMappingEntry qme3 = new QueryMappingEntry( query2.getId(), query3, mapping );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme3 );

		assertThat( dependencyGraph.isVectorizable(), equalTo( true ) );

		// First query we check is "SELECT co_id FROM company WHERE co_name = 'Abercrombie & Fitch'";
		List<Map<String, Object>> resultSet = new LinkedList<>();
		QueryResult resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 1 );
		db.addResultSet( resultToStore );

		// Second query we check is "SELECT t_ca_id, t_tt_id, t_s_symb, t_qty, t_chrg, t_lifo, t_is_cash FROM trade WHERE t_id = 200000087266122"
		resultSet = new LinkedList<>();
        resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 7 );
        db.addResultSet( resultToStore );

		// Third query we check is "SELECT s_ex_id, s_name, s_symb FROM security WHERE s_co_id = 4300000038 AND s_issue = 'COMMON'"
		resultSet = new LinkedList<>();
        resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 3 );
		db.addResultSet( resultToStore );

		// Fourth query we check is "SELECT h_t_id, h_qty, h_price, h_dts FROM holding WHERE h_ca_id = 43000044602 AND h_s_symb = 'ANF' ORDER BY h_dts ASC"
		resultSet = new LinkedList<>();
        resultToStore = new QueryResult( resultSet, new VersionVector( new ArrayList<Long>() {{ add( 1L ); }} ), 4 );
		db.addResultSet( resultToStore );

		LateralUnionVectorizer vectorizer = new LateralUnionVectorizer();
		QueryVectorizerResult result = vectorizer.vectorize( 0, dependencyGraph, db );

		assertThat( result.getVectorizedQueryText(), equalTo("SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT co_id FROM company WHERE co_name = 'Abercrombie & Fitch' ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, I0.* FROM ( SELECT P1.rn0 AS ordKey0, P1.rn1 AS ordKey1, P1.rn0 AS ordKey2, P1.rn1 AS ordKey3, U.* FROM ( SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t_ca_id, t_tt_id, t_s_symb, t_qty, t_chrg, t_lifo, t_is_cash FROM trade WHERE t_id = 200000087266122 ) k ) z0 LEFT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s_ex_id, s_name, s_symb FROM security WHERE s_co_id = P0.co_id AND s_issue = 'COMMON' ) k ) z1 ON z0.rn0 = z1.rn1 UNION SELECT * FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT t_ca_id, t_tt_id, t_s_symb, t_qty, t_chrg, t_lifo, t_is_cash FROM trade WHERE t_id = 200000087266122 ) k ) z0 RIGHT JOIN ( SELECT *, ROW_NUMBER() OVER () AS rn1 FROM ( SELECT s_ex_id, s_name, s_symb FROM security WHERE s_co_id = P0.co_id AND s_issue = 'COMMON' ) k ) z1 ON z0.rn0 = z1.rn1 ORDER BY rn0, rn1 ) P1, LATERAL ( SELECT 0, P1.*, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, I1.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY h_dts ASC) AS rn0 FROM ( SELECT h_t_id, h_qty, h_price, h_dts FROM holding WHERE h_ca_id = P1.ca_id AND h_s_symb = P1.s_symb ORDER BY h_dts ASC ) k) I1) U( type1, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16 ) ORDER BY ordKey0, ordKey1, ordKey2, ordKey3, type1, u15 ASC) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16, u17, u18, u19, u20, u21, u22, u23 ) ORDER BY ordKey0, type0, u2, u3, u4, u5, u6, u22 ASC") );
	}

}
