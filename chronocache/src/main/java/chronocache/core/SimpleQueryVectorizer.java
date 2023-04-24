package chronocache.core;

import chronocache.db.DB;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import chronocache.core.parser.ParserPool;
import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.AntlrQueryMetadata;

import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Multimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object that is able to vectorize a set of queries given their dependency graph
 */
public class SimpleQueryVectorizer extends QueryVectorizer {

	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	public SimpleQueryVectorizer() {
		super();
	}

	/**
	 * Given a vectorizable, vectorize it.
	 */
	@Override
	public QueryVectorizerResult vectorize( long clientId, Vectorizable vectorizableQueries, DB db ) {
		DependencyGraph dependencyGraph = vectorizableQueries.getVectorizationDependencies();
		QueryVectorizerResult result = vectorize( dependencyGraph );

		return result;
	}

	public void appendCtidCoalesceFuntion( Set<String> tableNames, int queryNumber, StringBuilder queryTextBuilder ) {
		assert tableNames.size() >= 1;
		queryTextBuilder.append( "CONCAT( " );
		Iterator<String> tableNameIterator = tableNames.iterator();
		queryTextBuilder.append( tableNameIterator.next() );
		queryTextBuilder.append( ".ctid" );
		while( tableNameIterator.hasNext() ) {
			queryTextBuilder.append( ", " );
			queryTextBuilder.append( tableNameIterator.next() );
			queryTextBuilder.append( ".ctid" );
		}
		queryTextBuilder.append( " ) AS q" );
		queryTextBuilder.append( queryNumber );
		queryTextBuilder.append( "rn" );
	}

	/**
	 * Take the raw text and write it out as a CTE.
	 */
	public void writeRawTextAsCTE( Query queryShell, String rawQueryText, int queryNumber, StringBuilder queryTextBuilder ) {
		if( queryNumber != 0 ) {
			queryTextBuilder.append( ", " );
		}

		queryTextBuilder.append( "WITH q" );
		queryTextBuilder.append( queryNumber );
		queryTextBuilder.append( " AS ( " );

		//XXX: This FROM split will not work with subqueries, need to use antlr parser to splice in
		//new columns
		String pieces[] = rawQueryText.split( " FROM " );

		queryTextBuilder.append( pieces[0] );
		queryTextBuilder.append( ", " );
		appendCtidCoalesceFuntion( queryShell.getQueryMetadata().getTablesInQuery(), queryNumber, queryTextBuilder );
		for( int i = 1; i < pieces.length; i++ ) {
			queryTextBuilder.append( " FROM " );
			queryTextBuilder.append( pieces[i] );
		}
		queryTextBuilder.append( " )" );

	}

	/**
	 * The first query in a vectorizable is always written out as is, but in a CTE.
	 */
	public void writeFirstQueryAsBaseText(
		Iterator<QueryVectorizationPlanNode> planIterator,
		DependencyGraph dependencyGraph,
		StringBuilder vectorizedQueryTextBuilder,
		Set<String> allTables
	) {
		QueryVectorizationPlanNode node = planIterator.next();
		QueryIdentifier firstQueryId = node.getQueryId();
		assert dependencyGraph.isBaseQuery( firstQueryId );
		allTables.addAll( dependencyGraph.getQueryShellFromGraph( firstQueryId ).getTables() );

		// Write out first query as a CTE
		writeRawTextAsCTE( dependencyGraph.getQueryShellFromGraph( firstQueryId ), dependencyGraph.getBaseQueryText( firstQueryId ), 0, vectorizedQueryTextBuilder );
	}

	/**
	 * Write out all the follow-up CTEs and store what conditions we need to join on.
	 */
	public List<String> writeInnerCTEsAndGetJoinConditions(
		Iterator<QueryVectorizationPlanNode> planIterator,
		DependencyGraph dependencyGraph,
		StringBuilder vectorizedQueryTextBuilder,
		Set<String> tables
	) {
		int queryCount = 1;
		List<String> orderedJoinConditions = new LinkedList<String>();

		QueryIdentifier subsequentQueryId;
		QueryVectorizationPlanNode planNode;

		while( planIterator.hasNext() ) {
			planNode = planIterator.next();
			subsequentQueryId = planNode.getQueryId();

			assert !dependencyGraph.isBaseQuery( subsequentQueryId );

			Collection<QueryMappingEntry> queryMappings = dependencyGraph.getQueryDependencies( subsequentQueryId );

			FlattenedQueryMappings flattenedEntries = flattenQueryMappings( queryMappings );
			Query currentQueryShell = flattenedEntries.getCurrentQueryShell();
			String bodyString = flattenedEntries.getCurrentQueryText();

			tables.addAll( currentQueryShell.getTables() );
			
			Map<Integer, QueryColumn> placeToQueryColumnMap = flattenedEntries.getPlaceToQueryColumnMappings();

			AntlrParser parser = new AntlrParser();

			// Get the column names we are mapping from, order them in a list
			List<String> columnNames = new LinkedList<String>();
			for( int i = 0; i < placeToQueryColumnMap.keySet().size(); i++ ) {
				QueryColumn qc = placeToQueryColumnMap.get( i );
				columnNames.add( qc.getColumnName() );
			}

			logger.trace( "columnNames: " + columnNames );

			AntlrQueryMetadata metadata = currentQueryShell.getQueryMetadata();
			String rewrittenBodyString = parser.replaceQueryShellConstants( currentQueryShell.getParseTree(), columnNames, true );
			AntlrParser.ExtractedConditions extractedConditions = parser.getCTEJoinConditions( currentQueryShell.getParseTree(), columnNames, true );

			List<String> containedConditions = extractedConditions.getContainedConditions();
			List<String> uncontainedConditions = extractedConditions.getUncontainedConditions();
			logger.trace( "Got contained conditions: {}", containedConditions );
			logger.trace( "Got uncontained conditions: {}", uncontainedConditions );

			logger.trace( "Got rewrittenBodyString: " + rewrittenBodyString );

			String rewrittenPieces[] = rewrittenBodyString.split( "FROM" );
			vectorizedQueryTextBuilder.append( ", q" );
			vectorizedQueryTextBuilder.append( queryCount );
			vectorizedQueryTextBuilder.append( " AS ( " );
			vectorizedQueryTextBuilder.append( rewrittenPieces[ 0 ].trim() );

			// Get every column that we need to add to the select list because it will be a CTE join condition
			for( String conditionalColumn : metadata.getUncontainedConditionalColumnsInQuery() ) {
				vectorizedQueryTextBuilder.append( ", " );
				vectorizedQueryTextBuilder.append( conditionalColumn );
			}

			vectorizedQueryTextBuilder.append( ", " );
			appendCtidCoalesceFuntion( metadata.getTablesInQuery(), queryCount, vectorizedQueryTextBuilder );

			vectorizedQueryTextBuilder.append( " FROM " );

			logger.trace( "Rewritten pieces 1: " + rewrittenPieces[1] );

			// This strips out all of the conditions 
			//String unqualifiedConditionClause = conditionClause.replaceAll( "QUALIFYME[0-9]*\\.", "" );
			//logger.info( "Unqualified condition clause: " + unqualifiedConditionClause );
			//String strippedSuffix = rewrittenPieces[1].replace( unqualifiedConditionClause, "" ).trim();

			String suffix = rewrittenPieces[1];
			suffix = suffix.replaceAll( " +", " " );
			logger.trace( "Got rewrittenPieces[1]: {}", suffix );
			for( String uncontainedCondition : uncontainedConditions ) {
				logger.trace( "Trying to replace {} with null", uncontainedCondition );
				//Order here is important --- only one of these will succeed
				suffix = suffix.replaceFirst( uncontainedCondition + "\\s*AND", "" ); // I am not the last condition
				suffix = suffix.replaceFirst( "AND\\s*" + uncontainedCondition, "" ); // I am the last condition
				suffix = suffix.replaceFirst( uncontainedCondition, "" ); // I am the only condition
			}

			// Now the where clause is empty. strip it
			if( containedConditions.isEmpty() ) {
				suffix = suffix.replace( " WHERE", "" );
			}
			String strippedSuffix = suffix.trim();

			logger.trace( "strippedSuffix: " + strippedSuffix );
			vectorizedQueryTextBuilder.append( strippedSuffix );
			vectorizedQueryTextBuilder.append( " )" );

			StringBuilder sb = new StringBuilder();
			assert !uncontainedConditions.isEmpty();
			Iterator<String> uncontainedConditionIterator = uncontainedConditions.iterator();
			sb.append( uncontainedConditionIterator.next() );

			while( uncontainedConditionIterator.hasNext() ) {
				sb.append( " AND" ); // TODO: hack, should be whatever the condition is
				sb.append( uncontainedConditionIterator.next() );
			}

			assert !extractedConditions.getUncontainedConditions().isEmpty();

			orderedJoinConditions.add( sb.toString() );
			queryCount++;
			logger.trace( "Exiting QME loop 1" );
            
		}
		return orderedJoinConditions;
	}

	/**
	 * Write out the final query as a CTE and then write out all the join conditions.
	 */
	public void writeFinalQueryAndJoin(
		QueryVectorizationPlan plan,
		List<String> orderedJoinConditions,
		DependencyGraph dependencyGraph,
		StringBuilder vectorizedQueryTextBuilder
	) {
		List<QueryIdentifier> orderedQueries = plan.getOrderedQueries();

		//Last query
		vectorizedQueryTextBuilder.append( "\nSELECT * FROM q0" );
		for( int i = 1; i < orderedJoinConditions.size()+1; i++ ) {
			logger.trace( "Got Join Condition: " + orderedJoinConditions.get(i-1) );
			vectorizedQueryTextBuilder.append( " LEFT JOIN q" );
			vectorizedQueryTextBuilder.append( i );
			vectorizedQueryTextBuilder.append( " ON" );
			String joinCondition = orderedJoinConditions.get(i-1).replace( "WHERE", "" );

			//Replace all columns coming from the query being joined into with the appropriate
			//queryId
			joinCondition = joinCondition.replace( "QUALIFYME0", "q" + i );

			for( QueryMappingEntry mappingEntry : dependencyGraph.getQueryDependencies( orderedQueries.get( i ) ) ) {
				QueryIdentifier priorQueryId = mappingEntry.getDependencyQueryId();
				int priorQueryPosition;
				for( priorQueryPosition = 0; priorQueryPosition < orderedQueries.size(); priorQueryPosition++ ) {
					logger.trace( "Comparing: " + orderedQueries.get(priorQueryPosition) + " vs. " + priorQueryId );
					if( orderedQueries.get(priorQueryPosition).equals( priorQueryId ) ) {
						break;
					}
				}
				assert priorQueryPosition < orderedQueries.size();
				Multimap<Integer,String> mappings = mappingEntry.getQueryMappings();
				for( Integer mapNumber : mappings.keySet() ) {
					Collection<String> colsWeCanMapFrom = mappings.get( mapNumber );
					assert colsWeCanMapFrom.size() == 1;
					String colWeWillMapFrom = colsWeCanMapFrom.iterator().next();
					logger.trace( "Replacing: " + "QUALIFYME1." + colWeWillMapFrom );
					joinCondition = joinCondition.replaceFirst( "QUALIFYME1\\." + colWeWillMapFrom, "q" + priorQueryPosition + "." + colWeWillMapFrom );
				}
			}

			vectorizedQueryTextBuilder.append( joinCondition );
		}
	}

	public QueryVectorizerResult vectorize( DependencyGraph dependencyGraph ) {
		if( !dependencyGraph.isSimpleVectorizable() ) {
			logger.trace( "Returning null,null" );
			return new QueryVectorizerResult( null, null, null );
		}

		logger.trace( "Going to topological sort!" );

		//TODO: This should not be a method on the dependency graph
		QueryVectorizationPlan plan = dependencyGraph.getVectorizationPlan();

		logger.trace( "Going to vectorize dependencyGraph." );

		assert plan.size() >= 1;
		if( plan.size() == 1 ) {
			QueryIdentifier qid = plan.iterator().next().getQueryId();
			Query q = plan.getQueryShell( qid );
			// Nothing to vectorize, send back base text
			QueryVectorizerResult result = new QueryVectorizerResult( new SimpleQueryVectorizationPlan( plan ), getSingleQueryBaseText( plan, dependencyGraph ), q.getTables() );
			return result;
		}

		Set<String> allTables = new HashSet<>();

		//Vectorize queries
		StringBuilder vectorizedQueryTextBuilder = new StringBuilder();
		Iterator<QueryVectorizationPlanNode> planIterator = plan.iterator();

		long firstQueryStart = System.nanoTime()/1000;
		writeFirstQueryAsBaseText( planIterator, dependencyGraph, vectorizedQueryTextBuilder, allTables );
		long firstQueryEnd = System.nanoTime()/1000;
		logger.debug( "Time to write out first query: {}", firstQueryEnd-firstQueryStart );


		// Logic:
		// If this is a baseQuery, add the raw text
		// Otherwise, get the body's query text and rewrite it via mappings
		// Store conditions for left joins on CTEs
		// All queries (other than the last one) are CTEs
		long innerCTEStart = System.nanoTime()/1000;
		List<String> orderedJoinConditions = writeInnerCTEsAndGetJoinConditions( planIterator, dependencyGraph, vectorizedQueryTextBuilder, allTables );
		long innerCTEEnd = System.nanoTime()/1000;
		logger.debug( "Time to write out innerCTEs: {}", innerCTEEnd-innerCTEStart );

		logger.trace( "JoinConditions: " + orderedJoinConditions );

		long finalJoinStart = System.nanoTime()/1000;
		writeFinalQueryAndJoin( plan, orderedJoinConditions, dependencyGraph, vectorizedQueryTextBuilder );
		long finalJoinEnd = System.nanoTime()/1000;
		logger.debug( "Time to write final join: {}", finalJoinEnd-finalJoinStart );

		String vectorizedQueryText = vectorizedQueryTextBuilder.toString();

		QueryVectorizerResult result = new QueryVectorizerResult( new SimpleQueryVectorizationPlan( plan ), vectorizedQueryText, allTables );
		return result;
	}

	/**
	 * Determine what the current row numbers are for this row (which rows did our vectorized results come from
	 * in the original CTEs.
	 */
	public void setCurrentRowNumbers( String currentRowNumbers[], Map<String, Object> row, int numVectorizedQueries ) {
		logger.trace( "Setting current row numbers for row: {}", row );
		for( int i = 0; i < numVectorizedQueries; i++ ) {
			String rowNumberCol = "q" + i + "rn";
			assert row.containsKey( rowNumberCol );
			
			Object o = row.get( rowNumberCol );
			logger.trace( "Getting col: {}", rowNumberCol );

			if( o != null ) {
				logger.trace( "Got o: {}", o );
				String rowNumber = (String) o;
				logger.trace( "Got rowNumber: {}", rowNumber );
				currentRowNumbers[i] = rowNumber;
			} else {
				currentRowNumbers[i] = null;
			}
		}
	}

	/**
	 * Add the columns we want from this row to the ongoing result set for query queryNumber.
	 */
	public void addColumnsFromQueryRowToOngoingResultSet(
		List<Map<String,Object>> ongoingResultSet,
		Map<String, Object> row,
		QueryVectorizationPlan plan,
		int queryNumber
	) {
		logger.trace( "Going to add cols: {} from row: {} ", plan.getSelectedColumnsForQuery( queryNumber ), row );
		Map<String, Object> queryRow = new HashMap<String, Object>();
		List<String> selectedCols = plan.getSelectedColumnsForQuery( queryNumber );
		for( String selectedCol : selectedCols ) {
			assert row.containsKey( selectedCol );
			Object o = row.get( selectedCol );
			logger.trace( "Found {} for col: {}", o, selectedCol );
			if( o != null ) {
				queryRow.put( selectedCol, o );
			}
		}
		if( !queryRow.isEmpty() ) {
			ongoingResultSet.add( queryRow );
		} else {
			logger.warn( "Empty result set, putting null" );
		}
	}

	/**
	 * If all of the row numbers of our dependencies are the same, then return true.
	 */
	public boolean allDependencyQueryRowNumsAreSame(
		String lastRowNumbers[],
		String currentRowNumbers[],
		int queryNumber,
		QueryVectorizationPlan plan
	) {

		logger.trace( "{} vs {}", lastRowNumbers, currentRowNumbers );
		List<Integer> topologicalDependencies = plan.getQueryDependenciesInTopologicalOrder( queryNumber );
		for( Integer i : topologicalDependencies ) {
            logger.trace( "Checking index: {}, {} = {} ?", i, lastRowNumbers[i], currentRowNumbers[i] );
			if( !lastRowNumbers[i].equals( currentRowNumbers[i] ) && !lastRowNumbers[i].equals( "\n" ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Split this row's results among the vectorized query results.
	 */
	public void splitRowAmongQueries(
		ArrayList<List<QueryResult>> splitQueryResults,
		ArrayList<List<Map<String,Object>>> ongoingResultSets,
		QueryVectorizationPlan plan,
		Map<String, Object> row,
		String lastRowNumbers[],
		VersionVector version
	) {
		// The results in each row are unordered due to the map, so we use fully qualified names
		// to pull out the row numbers for each query
		// String row numbers because these look like (ID, offset) in postgres
		String currentRowNumbers[] = new String[ plan.size() ];
		setCurrentRowNumbers( currentRowNumbers, row, plan.size() );

		for( int i = 0; i < plan.size(); i++ ) {
			//If our dependency queries' row numbers are the same as they were last time
			//then we are either duplicated from a join to our right, or we are another row returned
			//for the values from our dependency queries
			logger.info( "{}: Splitting row among queries: {}", i, row );
			if( allDependencyQueryRowNumsAreSame( lastRowNumbers, currentRowNumbers, i, plan ) ) {
				logger.trace( "All dependency query row nums are the same." );
				logger.trace( "{} vs {}", lastRowNumbers[i], currentRowNumbers[i] );
				if( (lastRowNumbers[i] == null && currentRowNumbers[i] != null ) ||
					(lastRowNumbers[i] != null && currentRowNumbers[i] == null ) ||
					!lastRowNumbers[i].equals( currentRowNumbers[i] ) ) {
					logger.trace( "Flushing out column." );
					addColumnsFromQueryRowToOngoingResultSet( ongoingResultSets.get(i), row, plan, i );
				}
				//If it is the same, then we are duplicated row because we matched to the right
			} else {
				assert i != 0;
				logger.info( "Should start new resultSet for query Number: " + i );

				List<Map<String,Object>> resultSet = ongoingResultSets.get( i );
				QueryResult result = new QueryResult( resultSet, version );
				splitQueryResults.get( i ).add( result );
				ongoingResultSets.set( i,  new LinkedList<Map<String,Object>>() );

				addColumnsFromQueryRowToOngoingResultSet( ongoingResultSets.get( i ), row, plan, i );

			}
		}

		for( int i = 0; i < plan.size(); i++ ) {
			lastRowNumbers[i] = currentRowNumbers[i];
		}
	}

	/**
	 * Split apart this result set among the vectorized queries.
	 */
	@Override
	public ArrayList<List<QueryResult>> splitApartResultSet( QueryResult combinedResults, QueryVectorizationPlan plan ) {
		assert combinedResults.isSelect();
		assert ( plan instanceof SimpleQueryVectorizationPlan );
		logger.trace( "Got past header check." );

		// Logic:
		// Result looks like: ( Q0 cols, Q0 RN, Q1 cols, Q1 RN, ... Qk cols, Qk RN )
		// Example: ( 'student_id', 1, 'student_name, 'student_id', 1, 'class_name', 'student_id', 1 )
		//          ( 'student_id', 2, 'student_name', 'student_id', 2, 'class_name', 'student_id', 2 )
		//          ( 'student_id', 2, 'student_name', 'student_id', 3, 'class_name', 'student_id', 2 )
		//          ( 'student_id', 3, 'student_name', 'student_id', 4, 'class_name', 'student_id', 4 )
		//          ( 'student_id', 3, 'student_name', 'student_id', 4, 'class_name', 'student_id', 5 )
		// There are 3 unique student ids returned. One of them matches 2 student_names, and one student_id
		// with the same name is enrolled in two classes
		// ***********
		// Algorithm:
		// ***********
		// Recall that every new row from a prior query is presumed to map to a query instance of the subsquent query.
		// Therefore: we iterate down each row and check if any of the row numbers for our dependencies have changed. If they have,
		// then we know that we are the first result for a new instance of query Qi (by the above property). If they haven't,
		// but our row number has changed, then we know that we are another row returned by this instance of Qi. If neither is
		// true, then we are duplicated row because we matched twice on some join with a query Qj where j >= i.
		// By this logic, Q0 holds all the studentIds (never has any dependencies, so all part of the same result set).
		// Q1 instances: ('student_name'), ('student_name', 'student_name'), ('student_name' ) ( from (1), (2,3), (4,4) ).
		// Q2 instances: ('class_name'), ('class_name'), ('class_name', 'class_name') ( from (1), (2), (3), (4,5) )

		ArrayList<List<QueryResult>> splitQueryResults = new ArrayList<>();
		ArrayList<List<Map<String,Object>>> ongoingResultSets = new ArrayList<>();
		for( int i = 0; i < plan.size(); i++ ) {
			splitQueryResults.add( new LinkedList<QueryResult>() );
			ongoingResultSets.add( new LinkedList<Map<String,Object>>() );
		}

		if( plan.size() == 1 ) {
			logger.trace( "Only vectorized one query, returning result set as is!" );
			splitQueryResults.get( 0 ).add( combinedResults );
			return splitQueryResults;
		}

		String lastRowNumbers[] = new String[ plan.size() ];
		for( int i = 0; i < lastRowNumbers.length; i++ ) {
			lastRowNumbers[i] = "\n"; // HACK: token that won't appear in RN so we can see if it is the start token
		}

		for( Map<String, Object> row : combinedResults.getSelectResult() ) {
			splitRowAmongQueries( splitQueryResults, ongoingResultSets, plan, row, lastRowNumbers, combinedResults.getResultVersion() );
		}

		logger.info( "Done splitting all rows." );

		// If we have any ongoing result sets that haven't been flushed out yet, we should flush them.
		// Also, if we have a query at topological level i that has an ongoing result set then we executed
		// the queries at topological level i+1. If they don't have an ongoing result set, it is because
		// we had no matches for that parameter mapping. We should still cache this!
		int maxDepth = plan.getMaxTopologicalHeight();
		boolean hasResultsFromPriorDepth = true;
		Multimap<Integer, QueryIdentifier> queryDepthMap = plan.getTopologicalDepthToQueryIdMap();
		List<QueryIdentifier> topologicallyOrderedQueries = plan.getDependencyGraph().getTopologicallyOrderedQueries();

		for( int depth = 0; depth <= maxDepth; depth++ ) {
			Collection<QueryIdentifier> queriesAtThisDepth = queryDepthMap.get( depth );
			logger.trace( "Outputting unflushed rows." );
			assert !queriesAtThisDepth.isEmpty();
			boolean missingResultsAtThisLevel = false;
			for( QueryIdentifier qid : queriesAtThisDepth ) {
				int slot = plan.getSlotNumber( qid, topologicallyOrderedQueries );
				List<Map<String,Object>> resultSet = ongoingResultSets.get( slot );
				if( !resultSet.isEmpty() || hasResultsFromPriorDepth ) {
					QueryResult result = new QueryResult( resultSet, combinedResults.getResultVersion() );
					splitQueryResults.get( slot ).add( result );
					if( resultSet.isEmpty() ) {
						missingResultsAtThisLevel = true;
					}
				}
			}
			if( missingResultsAtThisLevel ) {
				hasResultsFromPriorDepth = false;
			}
		}

		/*
           for( int i = 0; i < plan.size(); i++ ) {
           List<Map<String,Object>> resultSet = ongoingResultSets.get( i );
           if( !resultSet.isEmpty() ) {
           QueryResult result = new QueryResult( resultSet, combinedResults.getResultVersion() );
           splitQueryResults.get(i).add( result );
           }
           }
         */

		return splitQueryResults;
	}

}
