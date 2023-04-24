package chronocache.core;

import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.junit.Ignore;
import org.junit.Test;

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


public class SimpleQueryVectorizerTest {

	private Query createStudentQuery() {
		String queryString = "SELECT student_id FROM students WHERE student_id > 3000";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query createStudentNameQuery() {
		String queryString = "SELECT student_name FROM students WHERE student_id = 7";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query createGradeQuery() {
		String queryString = "SELECT grade FROM grades WHERE student_id = 5";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query createClassesQuery() {
		String queryString = "SELECT class_id FROM classes WHERE student_id = 5";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query createStudentClassQuery() {
		String queryString = "SELECT teacher_id FROM teachers WHERE student_id = 5 AND class_id = 7";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query createTeacherQuery() {
		String queryString = "SELECT teacher_id FROM teachers WHERE last_class = 'CS 454'";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query createGradeForStudentTeacherQuery() {
		String queryString = "SELECT grade FROM grades WHERE t_id = 2 AND s_id = 3";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query aggregateGradesForTeacherQuery() {
		//TODO fix aggregate queries. These would need to go in a group by in the rewrite
		String queryString = "SELECT COUNT(*) FROM grades WHERE teacher_id = 4 AND grade = 7";
		//SELECT COUNT(*), teacher_id, grade FROM grades GROUP by teacher_id, grade
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query rawClassForTeacherQuery() {
		//TODO: I think * queries may pose a problem for the rewriting, need to keep track
		//of these columns somewhere and pass them in
		String queryString = "SELECT num_students FROM classes WHERE teacher_id = 7 AND class_id = 5";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private Query getAllTeachersQuery() {
		String queryString = "SELECT teacher_id FROM teachers";
		AntlrParser p = new AntlrParser();
		return new Query( queryString, p.buildParseTree( queryString ) );
	}

	private String createObjectRN( String rowNumber ) throws SQLException {
		return new String( rowNumber );
	}

    @Ignore( "Cannot vectorize a single query." )
	@Test
	public void testVectorizeOneQuery() {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph oneQueryGraph = new DependencyGraph();
		Query q = createStudentQuery();
		oneQueryGraph.addBaseQuery( q.getId(), q );
		QueryVectorizerResult result = vectorizer.vectorize( oneQueryGraph );
		assertThat( result.getVectorizedQueryText(), equalTo( q.getQueryString() ) );
		assertThat( result.getTables().size(), equalTo( 1 ) );
		assertThat( result.getTables().iterator().next(), equalTo( "students" ) );
	}

	@Test
	public void testVectorizeSimpleLoop() {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query gradeQuery = createGradeQuery();
		Multimap<Integer,String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry studentGradeMap = new QueryMappingEntry( studentQuery.getId(), gradeQuery, mappings );
		loopGraph.addDependencyForQuery( gradeQuery.getId(), studentGradeMap );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );
		String output = result.getVectorizedQueryText();
		assertThat( output, equalTo( "WITH q0 AS ( SELECT student_id, CONCAT( students.ctid ) AS q0rn FROM students WHERE student_id > 3000 ), q1 AS ( SELECT grade, student_id, CONCAT( grades.ctid ) AS q1rn FROM grades )\nSELECT * FROM q0 LEFT JOIN q1 ON q1.student_id = q0.student_id" ) );
		assertThat( result.getTables().size(), equalTo( 2 ) );
		assertThat( result.getTables().contains( "students" ), equalTo( true ) );
		assertThat( result.getTables().contains( "grades" ), equalTo( true ) );
	}

	@Test
	public void testVectorizeTwoLoopBodyQueries() {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query gradeQuery = createGradeQuery();
		Multimap<Integer,String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry studentGradeMap = new QueryMappingEntry( studentQuery.getId(), gradeQuery, mappings );
		loopGraph.addDependencyForQuery( gradeQuery.getId(), studentGradeMap );

		Query classQuery = createClassesQuery();
		QueryMappingEntry studentClassMap = new QueryMappingEntry( studentQuery.getId(), classQuery, mappings );
		loopGraph.addDependencyForQuery( classQuery.getId(), studentClassMap );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );
		String output = result.getVectorizedQueryText();
		assertThat( output, equalTo( "WITH q0 AS ( SELECT student_id, CONCAT( students.ctid ) AS q0rn FROM students WHERE student_id > 3000 ), q1 AS ( SELECT class_id, student_id, CONCAT( classes.ctid ) AS q1rn FROM classes ), q2 AS ( SELECT grade, student_id, CONCAT( grades.ctid ) AS q2rn FROM grades )\nSELECT * FROM q0 LEFT JOIN q1 ON q1.student_id = q0.student_id LEFT JOIN q2 ON q2.student_id = q0.student_id" ) );
		assertThat( result.getTables().size(), equalTo( 3 ) );
		assertThat( result.getTables().contains( "students" ), equalTo( true ) );
		assertThat( result.getTables().contains( "classes" ), equalTo( true ) );
		assertThat( result.getTables().contains( "grades" ), equalTo( true ) );

	}

	@Test
	public void testVectorizeLoopWithInnerDependencies() {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query classQuery = createClassesQuery();
		Multimap<Integer,String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry studentClassMap = new QueryMappingEntry( studentQuery.getId(), classQuery, mappings );
		loopGraph.addDependencyForQuery( classQuery.getId(), studentClassMap );

		Query studentClassQuery = createStudentClassQuery();
		QueryMappingEntry studentStudentClassQueryMap = new QueryMappingEntry( studentQuery.getId(), studentClassQuery, mappings );
		loopGraph.addDependencyForQuery( studentClassQuery.getId(), studentStudentClassQueryMap );
		Multimap<Integer,String> mappings2 = HashMultimap.create();
		mappings2.put( 1, "class_id" );
		QueryMappingEntry classStudentClassMap = new QueryMappingEntry( classQuery.getId(), studentClassQuery, mappings2 );
		loopGraph.addDependencyForQuery( studentClassQuery.getId(), classStudentClassMap );

        assertThat( loopGraph.isVectorizable(), equalTo( true ) );
        assertThat( loopGraph.isSimpleVectorizable(), equalTo( true ) );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );
		String output = result.getVectorizedQueryText();
		assertThat( output, equalTo( "WITH q0 AS ( SELECT student_id, CONCAT( students.ctid ) AS q0rn FROM students WHERE student_id > 3000 ), q1 AS ( SELECT class_id, student_id, CONCAT( classes.ctid ) AS q1rn FROM classes ), q2 AS ( SELECT teacher_id, student_id, class_id, CONCAT( teachers.ctid ) AS q2rn FROM teachers )\nSELECT * FROM q0 LEFT JOIN q1 ON q1.student_id = q0.student_id LEFT JOIN q2 ON q2.student_id = q0.student_id AND q2.class_id = q1.class_id" ) );
		assertThat( result.getTables().size(), equalTo( 3 ) );
		assertThat( result.getTables().contains( "students" ), equalTo( true ) );
		assertThat( result.getTables().contains( "classes" ), equalTo( true ) );
		assertThat( result.getTables().contains( "teachers" ), equalTo( true ) );

	}

	@Test
	public void testVectorizeMultipleBaseQuery() {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query teacherQuery = createTeacherQuery();
		loopGraph.addBaseQuery( teacherQuery.getId(), teacherQuery );

		Query gradeQuery = createGradeForStudentTeacherQuery();
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 1, "student_id" );
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "teacher_id" );
		QueryMappingEntry qme = new QueryMappingEntry( studentQuery.getId(), gradeQuery, mappings );
		loopGraph.addDependencyForQuery( gradeQuery.getId(), qme );
		qme = new QueryMappingEntry( teacherQuery.getId(), gradeQuery, mappings2 ) ;
		loopGraph.addDependencyForQuery( gradeQuery.getId(), qme );
		assertThat( loopGraph.isSimpleVectorizable(), equalTo( false ) );
		assertThat( loopGraph.isVectorizable(), equalTo( false ) );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );
		String output = result.getVectorizedQueryText();
		assertThat( output, equalTo( null ) );
	}

	@Test
	public void testVectorizeMultiLevelBaseQuery() throws SQLException {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query classQuery = createClassesQuery();
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( studentQuery.getId(), classQuery, mappings );
		loopGraph.addDependencyForQuery( classQuery.getId(), qme1 );

		Query teacherQuery = getAllTeachersQuery();
		loopGraph.addBaseQuery( teacherQuery.getId(), teacherQuery );

		Query teacherClassComboQuery = rawClassForTeacherQuery();
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "teacher_id" );
		QueryMappingEntry qme2 = new QueryMappingEntry( teacherQuery.getId(), teacherClassComboQuery, mappings2 );
		loopGraph.addDependencyForQuery( teacherClassComboQuery.getId(), qme2 );

		Multimap<Integer, String> mappings3 = HashMultimap.create();
		mappings3.put( 1, "class_id" );
		QueryMappingEntry qme3 = new QueryMappingEntry( classQuery.getId(), teacherClassComboQuery, mappings3 );
		loopGraph.addDependencyForQuery( teacherClassComboQuery.getId(), qme3 );

		assertThat( loopGraph.isSimpleVectorizable(), equalTo( false ) );
		assertThat( loopGraph.isVectorizable(), equalTo( false ) );
		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );
		String output = result.getVectorizedQueryText();

		assertThat( output, equalTo( null ) );

	}

	@Test
	public void testSplitFDQChain() throws SQLException {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query classQuery = createClassesQuery();
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( studentQuery.getId(), classQuery, mappings );
		loopGraph.addDependencyForQuery( classQuery.getId(), qme1 );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( 0 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "student_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 1 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "class_id" ) );

		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		Integer studentId = 3001;
		String classId = "'CS 454'";
		row.put( "student_id", (Object) studentId );
		row.put( "q0rn", (Object) createObjectRN( "(123,1)" ) );
		row.put( "class_id", (Object) classId );
		row.put( "q1rn", (Object) createObjectRN( "(124,1)" ) );
		resultSet.add( row );
		ArrayList<Long> versions = new ArrayList<Long>( 1 );
		versions.add( 1L );
		VersionVector version = new VersionVector( versions );
		QueryResult queryResult = new QueryResult( resultSet, version );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( queryResult, plan );

		// Two query results
		assertThat( qResults.size(), equalTo( 2 ) );

		//one result for q0
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		QueryResult qResult = qResults.get( 0 ).get( 0 );
		List<Map<String, Object>> rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_id" ), equalTo( studentId ) );

		//one result for q1
		assertThat( qResults.get( 1 ).size(), equalTo( 1 ) );
		qResult = qResults.get( 1 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 454'" ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( qResults.size() ) );
		for( int i = 0 ; i < cacheKeys.size(); i++ ) {
			assertThat( cacheKeys.get( i ).size(), equalTo( qResults.get( i ).size() ) );
		}

		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( studentQuery.getCacheKey() ) );
		String rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 3001";
		Query rewrittenQuery1 = new Query( rewrittenQuery1String );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( rewrittenQuery1.getCacheKey() ) );
	}

	@Test
	public void testSplitOuterLoop() throws SQLException {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query classQuery = createClassesQuery();
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( studentQuery.getId(), classQuery, mappings );
		loopGraph.addDependencyForQuery( classQuery.getId(), qme1 );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( 0 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "student_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 1 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "class_id" ) );

		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		Integer studentId = 3001;
		String classId = "'CS 454'";
		String rowNumber1 = "(123,1)";
		String rowNumber2 = "(124,1)";
		row.put( "student_id", (Object) studentId );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1 ) );
		row.put( "class_id", (Object) classId );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2 ) );
		resultSet.add( row );
		row = new HashMap<>();
		Integer studentId2 = 3002;
		String classId2 = "'CS 454'";
		String rowNumber1_2 = "(123,2)";
		String rowNumber2_2 = "(124,17)";
		row.put( "student_id", (Object) studentId2 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_2 ) );
		row.put( "class_id", (Object) classId2 );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_2 ) );
		resultSet.add( row );

		ArrayList<Long> versions = new ArrayList<Long>( 1 );
		versions.add( 1L );
		VersionVector version = new VersionVector( versions );
		QueryResult queryResult = new QueryResult( resultSet, version );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( queryResult, plan );

		// Two query results
		assertThat( qResults.size(), equalTo( 2 ) );

		//one result for q0
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		QueryResult qResult = qResults.get( 0 ).get( 0 );
		List<Map<String, Object>> rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 2 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_id" ), equalTo( studentId ) );
		row = rows.get( 1 );
		assertThat( row.get( "student_id" ), equalTo( studentId2 ) );

		//two results for q1
		assertThat( qResults.get( 1 ).size(), equalTo( 2 ) );
		qResult = qResults.get( 1 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 454'" ) );
		qResult = qResults.get( 1 ).get( 1 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 454'" ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( qResults.size() ) );
		for( int i = 0; i < cacheKeys.size(); i++ ) {
			assertThat( cacheKeys.get( i ).size(), equalTo( qResults.get( i ).size() ) );
		}

		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( studentQuery.getCacheKey() ) );
		String rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 3001";
		Query rewrittenQuery1 = new Query( rewrittenQuery1String );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( rewrittenQuery1.getCacheKey() ) );

		rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 3002";
		rewrittenQuery1 = new Query( rewrittenQuery1String );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( rewrittenQuery1.getCacheKey() ) );

	}

	@Test
	public void testSplitInnerLoop() throws SQLException {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query classQuery = createClassesQuery();
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( studentQuery.getId(), classQuery, mappings );
		loopGraph.addDependencyForQuery( classQuery.getId(), qme1 );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( 0 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "student_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 1 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "class_id" ) );

		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		Integer studentId = 3001;
		String classId = "'CS 454'";
		row.put( "student_id", (Object) studentId );
		row.put( "q0rn", (Object) createObjectRN( "(123,1)" ) );
		row.put( "class_id", (Object) classId );
		String rowNumber2_1 = "(124,1)";
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_1 ) );
		resultSet.add( row );
		row = new HashMap<>();
		Integer studentId2 = 3001;
		String classId2 = "'CS 458'";
		String rowNumber1_2 = "(123,1)";
		String rowNumber2_2 = "(124,2)";
		row.put( "student_id", (Object) studentId2 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_2 ) );
		row.put( "class_id", (Object) classId2 );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_2 ) );
		resultSet.add( row );

		ArrayList<Long> versions = new ArrayList<Long>( 1 );
		versions.add( 1L );
		VersionVector version = new VersionVector( versions );
		QueryResult queryResult = new QueryResult( resultSet, version );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( queryResult, plan );

		// Two query results
		assertThat( qResults.size(), equalTo( 2 ) );

		//one result for q0
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		QueryResult qResult = qResults.get( 0 ).get( 0 );
		List<Map<String, Object>> rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_id" ), equalTo( studentId ) );

		//one result for q1
		assertThat( qResults.get( 1 ).size(), equalTo( 1 ) );
		qResult = qResults.get( 1 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 2 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 454'" ) );
		row = rows.get( 1 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 458'" ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( qResults.size() ) );
		for( int i = 0; i < cacheKeys.size(); i++ ) {
			assertThat( cacheKeys.get( i ).size(), equalTo( qResults.get( i ).size() ) );
		}

		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( studentQuery.getCacheKey() ) );
		String rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 3001";
		Query rewrittenQuery1 = new Query( rewrittenQuery1String );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( rewrittenQuery1.getCacheKey() ) );
	}

	@Test
	public void testSplitDoubleLoop() throws SQLException {
		// N.B.: This is the example in the code
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		Query studentQuery = createStudentQuery();
		loopGraph.addBaseQuery( studentQuery.getId(), studentQuery );

		Query nameQuery = createStudentNameQuery();
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( studentQuery.getId(), nameQuery, mappings );
		loopGraph.addDependencyForQuery( nameQuery.getId(), qme1 );

		Query classQuery = createClassesQuery();
		Multimap<Integer, String> mappings2 = HashMultimap.create();
		mappings2.put( 0, "student_id" );
		QueryMappingEntry qme2 = new QueryMappingEntry( studentQuery.getId(), classQuery, mappings2 );
		loopGraph.addDependencyForQuery( classQuery.getId(), qme2 );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( 0 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "student_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 1 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "class_id" ) );

		selectedCols = plan.getSelectedColumnsForQuery( 2 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "student_name" ) );

		//Relation:
		//1 1 | 1 1 CS454 | 1 1 Alice
		//2 2 | 2 2 CS458 | 2 2 Bob
		//2 2 | 2 2 CS458 | 3 2 Ben
		//3 3 | 1 3 CS454 | 4 3 Carol
		//3 3 | 2 3 CS458 | 4 3 Carol
		//4 1 | 1 1 CS454 | 1 1 Alice
		// Q1: ( 1, 2, 3, 1 )
		// Q2: ( (CS454), (CS458), (CS454, CS458), (CS454) )
		// Q3: ( (Alice), (Bob, Ben), (Carol), (Alice) )

		//Equivalent Relation:
		//1 1 | 1 1 Alice | 1 1 CS454
		//2 2 | 2 2 Bob   | 2 2 CS458
		//2 2 | 3 2 Ben   | 2 2 CS458
		//3 3 | 4 3 Carol | 1 3 CS454
		//3 3 | 4 3 Carol | 2 3 CS458
		//4 1 | 1 1 Alice | 1 1 CS454
		// Q1: ( 1, 2, 3, 1 )
		// Q2: ( (Alice), (Bob, Ben), (Carol), (Alice) )
		// Q3: ( (CS454), (CS458), (CS454, CS458), (CS454) )
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		Integer studentId = 1;
		String studentName = "'Alice'";
		String classId = "'CS 454'";
		String rowNumber1_1 = "(123,1)";
		String rowNumber2_1 = "(124,1)";
		String rowNumber3_1 = "(125,1)";
		row.put( "student_id", (Object) studentId );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_1 ) );
		row.put( "class_id", (Object) classId );
		row.put( "student_id", (Object) studentId );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_1 ) );
		row.put( "student_name", (Object) studentName );
		row.put( "student_id", (Object) studentId );
		row.put( "q2rn", (Object) createObjectRN( rowNumber3_1 ) );
		resultSet.add( row );

		row = new HashMap<>();
		Integer studentId2 = 2;
		String studentName2 = "'Bob'";
		String classId2 = "'CS 458'";
		String rowNumber1_2 = "(123,2)";
		String rowNumber2_2 = "(124,2)";
		String rowNumber3_2 = "(125,2)";
		row.put( "student_id", (Object) studentId2 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_2 ) );
		row.put( "class_id", (Object) classId2 );
		row.put( "student_id", (Object) studentId2 );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_2 ) );
		row.put( "student_name", (Object) studentName2 );
		row.put( "student_id", (Object) studentId2 );
		row.put( "q2rn", (Object) createObjectRN( rowNumber3_2 ) );
		resultSet.add( row );

		row = new HashMap<>();
		Integer studentId3 = 2;
		String studentName3 = "'Ben'";
		String classId3 = "'CS 458'";
		String rowNumber1_3 = "(123,2)";
		String rowNumber2_3 = "(124,2)";
		String rowNumber3_3 = "(125,3)";
		row.put( "student_id", (Object) studentId3 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_3 ) );
		row.put( "class_id", (Object) classId3 );
		row.put( "student_id", (Object) studentId3 );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_3 ) );
		row.put( "student_name", (Object) studentName3 );
		row.put( "student_id", (Object) studentId3 );
		row.put( "q2rn", (Object) createObjectRN( rowNumber3_3 ) );
		resultSet.add( row );

		row = new HashMap<>();
		Integer studentId4 = 3;
		String studentName4 = "'Carol'";
		String classId4 = "'CS 454'";
		String rowNumber1_4 = "(123,3)";
		String rowNumber2_4 = "(124,1)";
		String rowNumber3_4 = "(125,4)";
		row.put( "student_id", (Object) studentId4 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_4 ) );
		row.put( "class_id", (Object) classId4 );
		row.put( "student_id", (Object) studentId4 );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_4 ) );
		row.put( "student_name", (Object) studentName4 );
		row.put( "student_id", (Object) studentId4 );
		row.put( "q2rn", (Object) createObjectRN( rowNumber3_4 ) );
		resultSet.add( row );

		row = new HashMap<>();
		Integer studentId5 = 3;
		String studentName5 = "'Carol'";
		String classId5 = "'CS 458'";
		String rowNumber1_5 = "(123,3)";
		String rowNumber2_5 = "(124,2)";
		String rowNumber3_5 = "(125,4)";
		row.put( "student_id", (Object) studentId5 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_5 ) );
		row.put( "class_id", (Object) classId5 );
		row.put( "student_id", (Object) studentId5 );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_5 ) );
		row.put( "student_name", (Object) studentName5 );
		row.put( "student_id", (Object) studentId5 );
		row.put( "q2rn", (Object) createObjectRN( rowNumber3_5 ) );
		resultSet.add( row );

		row = new HashMap<>();
		Integer studentId6 = 1;
		String studentName6 = "'Alice'";
		String classId6 = "'CS 454'";
		String rowNumber1_6 = "(123,4)";
		String rowNumber2_6 = "(124,1)";
		String rowNumber3_6 = "(125,1)";
		row.put( "student_id", (Object) studentId6 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_6 ) );
		row.put( "class_id", (Object) classId6 );
		row.put( "student_id", (Object) studentId6 );
		row.put( "q1rn", (Object) createObjectRN( rowNumber2_6 ) );
		row.put( "student_name", (Object) studentName6 );
		row.put( "student_id", (Object) studentId6 );
		row.put( "q2rn", (Object) createObjectRN( rowNumber3_6 ) );
		resultSet.add( row );

		ArrayList<Long> versions = new ArrayList<Long>( 1 );
		versions.add( 1L );
		VersionVector version = new VersionVector( versions );
		QueryResult queryResult = new QueryResult( resultSet, version );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( queryResult, plan );

		// Three query results
		assertThat( qResults.size(), equalTo( 3 ) );

		//one result for q0
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		QueryResult qResult = qResults.get( 0 ).get( 0 );
		List<Map<String, Object>> rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 4 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_id" ), equalTo( studentId ) );
		row = rows.get( 1 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_id" ), equalTo( studentId2 ) );
		row = rows.get( 2 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_id" ), equalTo( studentId4 ) );
		row = rows.get( 3 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_id" ), equalTo( studentId6 ) );

		assertThat( qResults.get( 1 ).size(), equalTo( 4 ) );
		qResult = qResults.get( 1 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 454'" ) );
		qResult = qResults.get( 1 ).get( 1 );
		rows = qResult.getSelectResult();
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 458'" ) );
		qResult = qResults.get( 1 ).get( 2 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 2 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 454'" ) );
		row = rows.get( 1 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 458'" ) );
		qResult = qResults.get( 1 ).get( 3 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "class_id" ), equalTo( "'CS 454'" ) );


		// q2
		assertThat( qResults.get( 2 ).size(), equalTo( 4 ) );
		qResult = qResults.get( 2 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_name" ), equalTo( "'Alice'" ) );
		qResult = qResults.get( 2 ).get( 1 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 2 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_name" ), equalTo( "'Bob'" ) );
		row = rows.get( 1 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_name" ), equalTo( "'Ben'" ) );
		qResult = qResults.get( 2 ).get( 2 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_name" ), equalTo( "'Carol'" ) );
		qResult = qResults.get( 2 ).get( 3 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );
		row = rows.get( 0 );
		assertThat( row.size(), equalTo( 1 ) );
		assertThat( row.get( "student_name" ), equalTo( "'Alice'" ) ); 

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( qResults.size() ) );
		for( int i = 0; i < cacheKeys.size(); i++ ) {
			assertThat( cacheKeys.get( i ).size(), equalTo( qResults.get( i ).size() ) );
		}

		AntlrParser p = new AntlrParser();

		//Query 0 should be unchanged b/c it is a base query
		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( studentQuery.getCacheKey() ) );

		//Query 1
		String rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 1";
		Query rewrittenQuery1 = new Query( rewrittenQuery1String, p.buildParseTree( rewrittenQuery1String ) );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( rewrittenQuery1.getCacheKey() ) );
		rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 2";
		rewrittenQuery1 = new Query( rewrittenQuery1String, p.buildParseTree( rewrittenQuery1String ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( rewrittenQuery1.getCacheKey() ) );
		rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 3";
		rewrittenQuery1 = new Query( rewrittenQuery1String, p.buildParseTree( rewrittenQuery1String ) );
		assertThat( cacheKeys.get( 1 ).get( 2 ), equalTo( rewrittenQuery1.getCacheKey() ) );

		//This is a duplicate row so it is expected that we get another of the same result
		rewrittenQuery1String = "SELECT class_id FROM classes WHERE student_id = 1";
		rewrittenQuery1 = new Query( rewrittenQuery1String, p.buildParseTree( rewrittenQuery1String ) );
		assertThat( cacheKeys.get( 1 ).get( 3 ), equalTo( rewrittenQuery1.getCacheKey() ) );


		//Query 2
		String rewrittenQuery2String = "SELECT student_name FROM students WHERE student_id = 1";
		Query rewrittenQuery2 = new Query( rewrittenQuery2String, p.buildParseTree( rewrittenQuery2String ) );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( rewrittenQuery2.getCacheKey() ) );

		rewrittenQuery2String = "SELECT student_name FROM students WHERE student_id = 2";
		rewrittenQuery2 = new Query( rewrittenQuery2String, p.buildParseTree( rewrittenQuery2String ) );
		assertThat( cacheKeys.get( 2 ).get( 1 ), equalTo( rewrittenQuery2.getCacheKey() ) );


		rewrittenQuery2String = "SELECT student_name FROM students WHERE student_id = 3";
		rewrittenQuery2 = new Query( rewrittenQuery2String, p.buildParseTree( rewrittenQuery2String ) );
		assertThat( cacheKeys.get( 2 ).get( 2 ), equalTo( rewrittenQuery2.getCacheKey() ) );

		//This is a duplicate row so it is expected that we get another of the same result
		rewrittenQuery2String = "SELECT student_name FROM students WHERE student_id = 1";
		rewrittenQuery2 = new Query( rewrittenQuery2String, p.buildParseTree( rewrittenQuery2String ) );
		assertThat( cacheKeys.get( 2 ).get( 3 ), equalTo( rewrittenQuery2.getCacheKey() ) );
	}

	@Test
	public void vectorizeTPCWExample() throws SQLException {
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		DependencyGraph loopGraph = new DependencyGraph();
		String queryString = "SELECT c_id FROM customer WHERE c_uname = 'bob' AND c_pass = 'password'";
		AntlrParser p = new AntlrParser();
		Query customerQuery = new Query( queryString, p.buildParseTree( queryString ) );
		loopGraph.addBaseQuery( customerQuery.getId(), customerQuery );

		queryString = "SELECT MAX(o_id) AS m_o_id FROM orders WHERE o_c_id = 3";
		Query orderQuery = new Query( queryString, p.buildParseTree( queryString ) );
		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "c_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( customerQuery.getId(), orderQuery, mappings );
		loopGraph.addDependencyForQuery( orderQuery.getId(), qme1 );

		queryString = "SELECT stuff FROM order_line, item WHERE ol_i_id = i_id AND ol_o_id = 100";
		Query itemQuery = new Query( queryString, p.buildParseTree( queryString ) );
		mappings = HashMultimap.create();
		mappings.put( 0, "m_o_id" );
		QueryMappingEntry qme2 = new QueryMappingEntry( orderQuery.getId(), itemQuery, mappings );
		loopGraph.addDependencyForQuery( itemQuery.getId(), qme2 );

		QueryVectorizerResult result = vectorizer.vectorize( loopGraph );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();

		assertThat( loopGraph.isSimpleVectorizable(), equalTo( false ) );
		assertThat( result.getVectorizedQueryText(), equalTo( null ) );

	}

	@Test
	public void noVectorizeInnerJoin() {
		String queryString1 = "SELECT a FROM t";
		Query query1 = new Query( queryString1 );

		String queryString2 = "SELECT stuff FROM u INNER JOIN v on u.col = v.col + 1";
		Query query2 = new Query( queryString2 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "a" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme1 );

		assertThat( dependencyGraph.isSimpleVectorizable(), equalTo( false ) );
		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		QueryVectorizerResult result = vectorizer.vectorize( dependencyGraph );
		assertThat( result.getVectorizedQueryText(), equalTo( null ) );
	}

    @Test
    public void noVectorizeUnion() {
        String queryString1 = "SELECT a FROM t UNION SELECT b FROM t";
        Query query1 = new Query( queryString1 );

        String queryString2 = "SELECT c FROM t WHERE c = 1";
		Query query2 = new Query( queryString2 );

		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "a" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme1 );
        assertThat( dependencyGraph.isSimpleVectorizable(), equalTo( false ) );

    }

	@Test
	public void vectorizeWikipediaAnonymousPage() {
		//92% of Wikipedia queries.
		//Returns page_id
		String queryString1 = "SELECT page_id FROM page WHERE page_namespace = 'namespace' AND page_title = 'Apollo' LIMIT 1";
		Query query1 = new Query( queryString1 );

		//uses page_id
		String queryString2 = "SELECT whatever FROM page_restrictions WHERE pr_page = 3";
		Query query2 = new Query( queryString2 );

		//We can't predict this query, we don't have the ip address
		String queryString3 = "SELECT something FROM ipblocks WHERE ipb_address = '10.10.10.10'";
		Query query3 = new Query( queryString2 );

		//We can predict this, gives us rev text id
		//Still need to take care of not rewriting ``contained'' conditions.
		String queryString4 = "SELECT rev_id, rev_text_id " +
			"FROM page, revision " +
			" WHERE page_id = rev_page " +
			"   AND rev_page = 3 " +
			"   AND page_id = 3 " +
			"   AND rev_id = page_latest";
		Query query4 = new Query( queryString4 );

		//uses rev id
		String queryString5 = "SELECT old_text, old_flags FROM text WHERE old_id = 10";
		Query query5 = new Query( queryString5 );

		//Can vectorize all of these queries. (Except for the IP one), but that doesn't matter.
		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "page_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme1 );

		mappings = HashMultimap.create();
		mappings.put( 0, "page_id" );
		mappings.put( 1, "page_id" );
		QueryMappingEntry qme2 = new QueryMappingEntry( query1.getId(), query4, mappings );
		dependencyGraph.addDependencyForQuery( query4.getId(), qme2 );

		mappings = HashMultimap.create();
		mappings.put( 0, "rev_text_id" );
		QueryMappingEntry qme3 = new QueryMappingEntry( query4.getId(), query5, mappings );
		dependencyGraph.addDependencyForQuery( query5.getId(), qme3 );

		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();

		QueryVectorizerResult result = vectorizer.vectorize( dependencyGraph );
		assertThat( result.getVectorizedQueryText(), equalTo( "WITH q0 AS ( SELECT page_id, CONCAT( page.ctid ) AS q0rn FROM page WHERE page_namespace = 'namespace' AND page_title = 'Apollo' LIMIT 1 ), q1 AS ( SELECT whatever, pr_page, CONCAT( page_restrictions.ctid ) AS q1rn FROM page_restrictions ), q2 AS ( SELECT rev_id, rev_text_id, rev_page, page_id, CONCAT( page.ctid, revision.ctid ) AS q2rn FROM page, revision WHERE page_id = rev_page AND rev_id = page_latest ), q3 AS ( SELECT old_text, old_flags, old_id, CONCAT( text.ctid ) AS q3rn FROM text )\nSELECT * FROM q0 LEFT JOIN q1 ON q1.pr_page = q0.page_id LEFT JOIN q2 ON q2.rev_page = q0.page_id AND q2.page_id = q0.page_id LEFT JOIN q3 ON q3.old_id = q2.rev_text_id" ) );
		//TODO test value split

	}

    @Test
    public void testCacheNullResultForSecondQuery() throws SQLException {
		//Returns page_id
		String queryString1 = "SELECT page_id FROM page WHERE page_namespace = 'namespace' AND page_title = 'Apollo' LIMIT 1";
		Query query1 = new Query( queryString1 );

		//uses page_id
		String queryString2 = "SELECT whatever FROM page_restrictions WHERE pr_page = 3";
		Query query2 = new Query( queryString2 );

		// Set up vectorizable for second query
		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "page_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme1 );

		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		QueryVectorizerResult result = vectorizer.vectorize( dependencyGraph );

		assertThat( result.getVectorizedQueryText(), equalTo( "WITH q0 AS ( SELECT page_id, CONCAT( page.ctid ) AS q0rn FROM page WHERE page_namespace = 'namespace' AND page_title = 'Apollo' LIMIT 1 ), q1 AS ( SELECT whatever, pr_page, CONCAT( page_restrictions.ctid ) AS q1rn FROM page_restrictions )\nSELECT * FROM q0 LEFT JOIN q1 ON q1.pr_page = q0.page_id" ) );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( 0 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "page_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 1 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "whatever" ) );

		//Relation
		// page_id | q0rn | whatever | pr_page | q1rn
		// 1 | (1,1) | NULL | NULL | NULL
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		Integer pageId = 2;
		String rowNumber1_1 = "(1,1)";
		row.put( "page_id", (Object) pageId );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_1 ) );
		row.put( "whatever", null );
		row.put( "pr_page", null );
		row.put( "q1rn", null );
		resultSet.add( row );

        ArrayList<Long> versions = new ArrayList<Long>( 1 );
		versions.add( 1L );
		VersionVector version = new VersionVector( versions );
		QueryResult queryResult = new QueryResult( resultSet, version );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( queryResult, plan );

		// Two query results
		assertThat( qResults.size(), equalTo( 2 ) );

        // We should cache an empty result set for the second query.
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		QueryResult qResult = qResults.get( 0 ).get( 0 );
		List<Map<String, Object>> rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );

		assertThat( qResults.get( 1 ).size(), equalTo( 1 ) );
		qResult = qResults.get( 1 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 0 ) ); // < Empty result set

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( qResults.size() ) );
		for( int i = 0; i < cacheKeys.size(); i++ ) {
			assertThat( cacheKeys.get( i ).size(), equalTo( qResults.get( i ).size() ) );
		}

		AntlrParser p = new AntlrParser();

		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( query1.getCacheKey() ) );
		String rewrittenQuery1String = "SELECT whatever FROM page_restrictions WHERE pr_page = 2";
		Query rewrittenQuery1 = new Query( rewrittenQuery1String, p.buildParseTree( rewrittenQuery1String ) );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( rewrittenQuery1.getCacheKey() ) );

    }

    @Test
    public void testCacheNullResultForSecondQueryMultiResult() throws SQLException {
		//Returns page_id
		String queryString1 = "SELECT page_id FROM page WHERE page_namespace = 'namespace' AND page_title = 'Apollo' LIMIT 1";
		Query query1 = new Query( queryString1 );

		//uses page_id
		String queryString2 = "SELECT whatever FROM page_restrictions WHERE pr_page = 3";
		Query query2 = new Query( queryString2 );

		// Set up vectorizable for second query
		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "page_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme1 );

		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		QueryVectorizerResult result = vectorizer.vectorize( dependencyGraph );

		assertThat( result.getVectorizedQueryText(), equalTo( "WITH q0 AS ( SELECT page_id, CONCAT( page.ctid ) AS q0rn FROM page WHERE page_namespace = 'namespace' AND page_title = 'Apollo' LIMIT 1 ), q1 AS ( SELECT whatever, pr_page, CONCAT( page_restrictions.ctid ) AS q1rn FROM page_restrictions )\nSELECT * FROM q0 LEFT JOIN q1 ON q1.pr_page = q0.page_id" ) );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( 0 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "page_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 1 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "whatever" ) );

		//Relation
		// page_id | q0rn | whatever | pr_page | q1rn
		// 1 | (1,1) | NULL | NULL | NULL
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		Integer pageId = 2;
		String rowNumber1_1 = "(1,1)";
		row.put( "page_id", (Object) pageId );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_1 ) );
		row.put( "whatever", null );
		row.put( "pr_page", null );
		row.put( "q1rn", null );
		resultSet.add( row );

        row = new HashMap<>();
		Integer pageId2 = 3;
		String rowNumber1_2 = "(1,2)";
		row.put( "page_id", (Object) pageId2 );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_2 ) );
        row.put( "whatever", null );
        row.put( "pr_page", null );
        row.put( "q1rn", null );
		resultSet.add( row );

		ArrayList<Long> versions = new ArrayList<Long>( 1 );
		versions.add( 1L );
		VersionVector version = new VersionVector( versions );
		QueryResult queryResult = new QueryResult( resultSet, version );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( queryResult, plan );

		// Two query results
		assertThat( qResults.size(), equalTo( 2 ) );

		// We should cache an empty result set for the second query.
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		QueryResult qResult = qResults.get( 0 ).get( 0 );
		List<Map<String, Object>> rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 2 ) );

		assertThat( qResults.get( 1 ).size(), equalTo( 2 ) );
		qResult = qResults.get( 1 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 0 ) ); // < Empty result set

		qResult = qResults.get( 1 ).get( 1 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 0 ) ); // < Empty result set


		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( qResults.size() ) );
		for( int i = 0; i < cacheKeys.size(); i++ ) {
			assertThat( cacheKeys.get( i ).size(), equalTo( qResults.get( i ).size() ) );
		}

		AntlrParser p = new AntlrParser();

		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( query1.getCacheKey() ) );

		String rewrittenQuery1String = "SELECT whatever FROM page_restrictions WHERE pr_page = 2";
		Query rewrittenQuery1 = new Query( rewrittenQuery1String, p.buildParseTree( rewrittenQuery1String ) );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( rewrittenQuery1.getCacheKey() ) );

		String rewrittenQuery2String = "SELECT whatever FROM page_restrictions WHERE pr_page = 3";
		Query rewrittenQuery2 = new Query( rewrittenQuery2String, p.buildParseTree( rewrittenQuery2String ) );
		assertThat( cacheKeys.get( 1 ).get( 1 ), equalTo( rewrittenQuery2.getCacheKey() ) );
    }

    @Test
    public void testCacheNullResultForTripleNest() throws SQLException {
		//Returns page_id
		String queryString1 = "SELECT page_id FROM page WHERE page_namespace = 'namespace' AND page_title = 'Apollo' LIMIT 1";
		Query query1 = new Query( queryString1 );

		//uses page_id
		String queryString2 = "SELECT other_id FROM page_restrictions WHERE pr_page = 3";
		Query query2 = new Query( queryString2 );

		//uses other_id 
		String queryString3 = "SELECT blah FROM other_table WHERE s_id = 4";
		Query query3 = new Query( queryString3 );

		// Set up vectorizable for second query
		DependencyGraph dependencyGraph = new DependencyGraph();
		dependencyGraph.addBaseQuery( query1.getId(), query1 );

		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "page_id" );
		QueryMappingEntry qme1 = new QueryMappingEntry( query1.getId(), query2, mappings );
		dependencyGraph.addDependencyForQuery( query2.getId(), qme1 );

		mappings = HashMultimap.create();
		mappings.put( 0, "other_id" );
		QueryMappingEntry qme2 = new QueryMappingEntry( query2.getId(), query3, mappings );
		dependencyGraph.addDependencyForQuery( query3.getId(), qme2 );

		SimpleQueryVectorizer vectorizer = new SimpleQueryVectorizer();
		QueryVectorizerResult result = vectorizer.vectorize( dependencyGraph );

		QueryVectorizationPlan plan = result.getQueryVectorizationPlan();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( 0 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "page_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 1 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "other_id" ) );
		selectedCols = plan.getSelectedColumnsForQuery( 2 );
		assertThat( selectedCols.size(), equalTo( 1 ) );
		assertThat( selectedCols.get( 0 ), equalTo( "blah" ) );


		//Relation
		// page_id | q0rn | other_id | pr_page | q1rn | blah | s_id | q2rn
		// 1 | (1,1) | NULL | NULL | NULL | NULL | NULL | NULL
		List<Map<String,Object>> resultSet = new LinkedList<>();
		Map<String,Object> row = new HashMap<>();
		Integer pageId = 2;
		String rowNumber1_1 = "(1,1)";
		row.put( "page_id", (Object) pageId );
		row.put( "q0rn", (Object) createObjectRN( rowNumber1_1 ) );
		row.put( "other_id", null );
		row.put( "pr_page", null );
		row.put( "q1rn", null );
		row.put( "blah", null );
		row.put( "s_id", null );
		row.put( "q2rn", null );
		resultSet.add( row );

		ArrayList<Long> versions = new ArrayList<Long>( 1 );
		versions.add( 1L );
		VersionVector version = new VersionVector( versions );
		QueryResult queryResult = new QueryResult( resultSet, version );

		ArrayList<List<QueryResult>> qResults = vectorizer.splitApartResultSet( queryResult, plan );

		// Two query results
		assertThat( qResults.size(), equalTo( 3 ) );

		// We should cache an empty result set for the second query.
		assertThat( qResults.get( 0 ).size(), equalTo( 1 ) );
		QueryResult qResult = qResults.get( 0 ).get( 0 );
		List<Map<String, Object>> rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 1 ) );

		assertThat( qResults.get( 1 ).size(), equalTo( 1 ) );
		qResult = qResults.get( 1 ).get( 0 );
		rows = qResult.getSelectResult();
		assertThat( rows.size(), equalTo( 0 ) ); // < Empty result set

		assertThat( qResults.get( 2 ).size(), equalTo( 0 ) );

		ArrayList<List<String>> cacheKeys = vectorizer.getCacheKeysForResults( qResults, plan );
		assertThat( cacheKeys.size(), equalTo( qResults.size() ) );

		assertThat( cacheKeys.get( 0 ).size(), equalTo( qResults.get( 0 ).size() ) );
		assertThat( cacheKeys.get( 1 ).size(), equalTo( qResults.get( 1 ).size() ) );
		assertThat( cacheKeys.get( 2 ).get( 0 ), equalTo( null ) );

		AntlrParser p = new AntlrParser();

		assertThat( cacheKeys.get( 0 ).get( 0 ), equalTo( query1.getCacheKey() ) );

		String rewrittenQuery1String = "SELECT other_id FROM page_restrictions WHERE pr_page = 2";
		Query rewrittenQuery1 = new Query( rewrittenQuery1String, p.buildParseTree( rewrittenQuery1String ) );
		assertThat( cacheKeys.get( 1 ).get( 0 ), equalTo( rewrittenQuery1.getCacheKey() ) );

    }

}
