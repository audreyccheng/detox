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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class QueryVectorizer {

	private Logger logger = LoggerFactory.getLogger( this.getClass() );
	/**
	 * A column name from a query with a given identifier.
	 */
	class QueryColumn {
		private QueryIdentifier id;
		private String columnName;

		public QueryColumn( QueryIdentifier qid, String columnName ) {
			this.id = qid;
			this.columnName = columnName;
		}

		public QueryIdentifier getQueryId() {
			return id;
		}

		public String getColumnName() {
			return columnName;
		}
	}

	/**
	 * A set of query identifiers and mappings to the current query
	 */
	class FlattenedQueryMappings {

		private Map<QueryIdentifier, Multimap<Integer, String>> mappings;
		private Query currentQuery;

		public FlattenedQueryMappings(
			Map<QueryIdentifier, Multimap<Integer, String>> mappings,
			Query currentQuery
		) {
			this.mappings = mappings;
			this.currentQuery = currentQuery;
		}

		public Map<QueryIdentifier, Multimap<Integer,String>> getFlattenedMappings() {
			return mappings;
		}

		/**
		 * Figure out which "QueryColumn" we need to map to given positions in the current
		 * query's replacement positions.
		 */
		public Map<Integer, QueryColumn> getPlaceToQueryColumnMappings() {
			Map<Integer, QueryColumn> invertedMappings = new HashMap<>();
			for( QueryIdentifier qid : mappings.keySet() ) {
				Multimap<Integer,String> entries = mappings.get( qid );
				for( Integer i : entries.keySet() ) {
					String colName = entries.get(i).iterator().next();
					QueryColumn qc = new QueryColumn( qid, colName );
					assert !invertedMappings.containsKey( i );
					invertedMappings.put( i, qc );
				}
			}
			return invertedMappings;
		}


		public Query getCurrentQueryShell() {
			return currentQuery;
		}

		public String getCurrentQueryText() {
			return currentQuery.getQueryString();
		}
	}

	/**
	 * If the dependency graph has a single query, then this is vectorizable by just returning the original query's text.
	 */
	public String getSingleQueryBaseText( QueryVectorizationPlan plan, DependencyGraph dependencyGraph ) {
		QueryIdentifier qid = plan.iterator().next().getQueryId();
		assert dependencyGraph.isBaseQuery( qid );
		return dependencyGraph.getBaseQueryText( qid );
	}

	/**
	 * Take a set of query mapping entries to the current query, and flatten them.
	 */
	public FlattenedQueryMappings flattenQueryMappings( Collection<QueryMappingEntry> mappingEntries ) {
		if( mappingEntries.isEmpty() ) {
			return null;
		}

		Map<QueryIdentifier, Multimap<Integer,String>> allMappingsFromPriorQueries = new HashMap<>();
		for( QueryMappingEntry mappingFromPriorQuery : mappingEntries ) {
			if( allMappingsFromPriorQueries.containsKey( mappingFromPriorQuery.getDependencyQueryId() ) ) {
				Multimap<Integer, String> currentMappings = allMappingsFromPriorQueries.get( mappingFromPriorQuery.getDependencyQueryId() );
				currentMappings.putAll( mappingFromPriorQuery.getQueryMappings() );
			} else {
				allMappingsFromPriorQueries.put( mappingFromPriorQuery.getDependencyQueryId(), HashMultimap.create( mappingFromPriorQuery.getQueryMappings() ) );
			}
		}
		Query currentQuery = mappingEntries.iterator().next().getQueryShell();
		return new FlattenedQueryMappings( allMappingsFromPriorQueries, currentQuery );

	}

	public String rewriteQueryWithColumnMappings( Collection<QueryMappingEntry> queryMappings, String queryString, int numberOfQueryConstants ) {
		if( queryMappings.isEmpty() ) {
			return queryString;
		}

		logger.trace( "Going to do the rewrite with columnMappings of size: {}", queryMappings.size() );
		for( QueryMappingEntry entry : queryMappings ) {
			logger.trace( "Have QME: {}", entry.toString() );
		}

		FlattenedQueryMappings flattenedEntries = flattenQueryMappings( queryMappings );
	
		Map<Integer, QueryColumn> placeToQueryColumnMap = flattenedEntries.getPlaceToQueryColumnMappings();
		logger.trace( "Got placeToQueryColumnMap: {}", placeToQueryColumnMap );

		logger.trace( "Constructed placement map of size: {}", placeToQueryColumnMap.size() );
		AntlrParser parser = new AntlrParser();

		// Get the column names we are mapping from, order them in a list
		List<String> columnNames = new LinkedList<String>();

		for( int i = 0; i < numberOfQueryConstants; i++ ) {
			logger.trace( "Getting column Name for field: {} = {}", i, placeToQueryColumnMap.get( i ) );
			QueryColumn qc = placeToQueryColumnMap.get( i );
			// Null means we don't have a constant to replace this with, likely comes
			// from a base text.
			if( qc == null ) {
				columnNames.add( null );
			} else {
				columnNames.add( qc.getColumnName() );
			}
		}

		logger.trace( "Going to ask parser to do the rewrite!" );
		AntlrParser.ParseResult parseResult = parser.buildParseTree( queryString );
		String rewrittenBodyString = parser.replaceQueryShellConstants( parseResult.getParseTree(), columnNames, true );
		logger.debug( "Parser done the rewrite, final string: {}", rewrittenBodyString );

		return rewrittenBodyString;
	}

	public abstract QueryVectorizerResult vectorize( long clientId, Vectorizable vectorizable, DB db );
	public abstract ArrayList<List<QueryResult>> splitApartResultSet( QueryResult combinedResults, QueryVectorizationPlan plan );


	/**
	 * For a given queryNumber at thisQueryDepth and a set of mappings coming from priorQueryDepth, figure out what row
	 * we should read from the result set at that level.
	 */
	private int determineRowNumberToRead(
		int queryNumber,
		int priorQueryDepth,
		List<List<QueryResult>> allRelevantQueryResults,
		List<Integer> relevantResultLevels,
		int thisQueryDepth
	) {
		logger.trace( "Going to determine row number to read. We are level {} and they are {}", thisQueryDepth, priorQueryDepth );
		logger.trace( "RelevantResultLevels: {}", relevantResultLevels );
		// If they are one topological level above us, then it is just their row number
		if( priorQueryDepth == thisQueryDepth - 1 ) {
			return queryNumber;
		}

		// Otherwise, find out which result set we are in for the query one topological level above us.

		// Find slot for that query
		int indexOfQueryOneLevelAbove = 0;
		for( Integer level : relevantResultLevels ) {
			if( level == thisQueryDepth - 1 ) {
				break;
			}
			indexOfQueryOneLevelAbove++;
		}

		List<QueryResult> relevantQueryResults = allRelevantQueryResults.get( indexOfQueryOneLevelAbove );
		Iterator<QueryResult> resultIterator = relevantQueryResults.iterator();

		int rowsSeenSoFar = 0;
		logger.trace( "query number is {}", queryNumber );
		int resultNumber = 0;
		while( resultIterator.hasNext() ) {
			List<Map<String,Object>> rows = resultIterator.next().getSelectResult();
			int rowSize = rows.size();
			if( rows.isEmpty() ) {
				rowSize = 1;
			}
			logger.trace( "Pulling out rows: {}", rows );
			if( rowsSeenSoFar + rowSize > queryNumber ) {
				break;
			}
			rowsSeenSoFar += rowSize;
			resultNumber++;
		}

		// Now we know the result set number, find out what row two levels above corresponds to the resultSet we are interested in.
		return determineRowNumberToRead( resultNumber, priorQueryDepth, allRelevantQueryResults, relevantResultLevels, thisQueryDepth-1 );

	}
		


	/**
	 * Get the row we need to split apart the results for the query-numberth entry in a given slot.
	 */
	private Map<String, Object> getRelevantRowFromResults(
		int queryNumber,
		int index,
		List<List<QueryResult>> allRelevantQueryResults,
		List<Integer> relevantResultDepths,
		int thisQueryDepth
		
	) {

		logger.trace( "Index is: {}", index );
		int priorQueryDepth = relevantResultDepths.get( index );
		logger.trace( "Determined that prior query depth is: {}", priorQueryDepth );

		int rowNumberToRead = determineRowNumberToRead( queryNumber, priorQueryDepth, allRelevantQueryResults, relevantResultDepths, thisQueryDepth );

		List<QueryResult> relevantQueryResults = allRelevantQueryResults.get( index );
		Iterator<QueryResult> resultIterator = relevantQueryResults.iterator();
		int rowsSeenSoFar = 0;
		logger.trace( "query number is {}", queryNumber );
		int resultNumber = 0;
		for( QueryResult result : relevantQueryResults ) {
			logger.trace( "Got rows: {}", result.getSelectResult() );
		}
		while( resultIterator.hasNext() ) {
			List<Map<String,Object>> rows = resultIterator.next().getSelectResult();
			int rowSize = rows.size();
			if( rows.isEmpty() ) {
				rowSize = 1;
			}
			logger.trace( "Pulling out rows: {}", rows );
			if( rowsSeenSoFar + rowSize > rowNumberToRead ) {
				logger.trace( "Trying to get row # {} from row: {}", rowNumberToRead - rowsSeenSoFar, rows );
				if( !rows.isEmpty() ) {
					return rows.get( rowNumberToRead - rowsSeenSoFar );
				} else {
					// Indicate that this row is the emptySet, and that you can't use it for anything
					return null;
				}
			}
			rowsSeenSoFar += rowSize;
			resultNumber++;
		}
		assert false;
		return null;
	}

	/**
	 * Figure out what depth the current query is at given relevant results from other levels.
	 * It's the max + 1
	 */
	private int getQueryDepth(
		List<Integer> relevantResultLevels
	) {
		int thisQueryDepth = 0;
		for( Integer depth : relevantResultLevels ) {
			if( depth > thisQueryDepth ) {
				thisQueryDepth = depth;
			}
		}
		return thisQueryDepth+1;

	}
	/**
	 * Rewrite a single query using the relevant results and mappings.
	 */
	public Query rewriteSingleQuery(
		int queryNumber,
		Query originalQueryShell,
		String queryText,
		AntlrParser parser,
		List<List<QueryResult>> relevantResults,
		List<Multimap<Integer,String>> relevantMappings,
		List<Integer> relevantResultLevels
	) {
		//XXX somehow need to use the dependency graph provided text in the rewrite

		int thisQueryDepth = getQueryDepth( relevantResultLevels );
	
		logger.trace( "Doing single query rewrite {} of {}", queryNumber, queryText != null ? queryText :originalQueryShell.getQueryString() );
		List<String> replacementConstantsRow = new ArrayList<String>( originalQueryShell.getParams().size() );

		for( int i = 0; i < originalQueryShell.getParams().size(); i++ ) {
			replacementConstantsRow.add( null );
		}

		logger.trace( "Finding relevant results." );
		logger.trace( "Relevant Results: {}", relevantResults );
		logger.trace( "RelevantResultLevels: {}", relevantResultLevels );

		// The idea here is that we are going to iterate over all of the relevant results and get the row
		// that we need for our mappings.

		for( int i = 0; i < relevantResults.size(); i++ ) {
			if( relevantMappings.get( i ) == null ) {
				logger.trace( "Stubbed mappings for proper row number calculations, skipping!" );
				continue;
			}

			logger.trace( "Getting relevantRow." );
			Map<String, Object> relevantRow = getRelevantRowFromResults( queryNumber, i, relevantResults, relevantResultLevels, thisQueryDepth );

			logger.trace( "Got relevantRow: {}", relevantRow );
			if( relevantRow == null ) {
				return null;
			}

			Multimap<Integer, String> mappingsFromThisQuery = relevantMappings.get( i );
			logger.trace( "Got mappings {}", mappingsFromThisQuery );
			for( Integer pos : mappingsFromThisQuery.keySet() ) {
				Collection<String> mappings = mappingsFromThisQuery.get( pos );
				// We care only about the first mapping to a given pos
				String field = mappings.iterator().next();
				logger.trace( "Doing mapping from field: {} to {}", field, pos );
				logger.trace( "Relevant Row: {}", relevantRow );
				String value = String.valueOf( relevantRow.get( field ) );
				logger.trace( "Value is {}", value );
				replacementConstantsRow.set( pos, value );
				logger.trace( "Set pos {} to {}", pos, value );
			}
		}

		logger.trace( "We have all constants." );

		logger.trace( "Going to rewrite with replacement constants!" );

		
		Query qShell = originalQueryShell;
		// HACK, we are rewriting a base query, need to reparse 
		if( queryText != null ) {
			long startParser = System.nanoTime()/1000;
			qShell = new Query( queryText, parser.buildParseTree( queryText ) );
			long endParser = System.nanoTime()/1000;
			logger.trace( "During rewriteSingleQuery, parse took: {}", endParser-startParser);
		}

		String rewrittenQueryString = parser.replaceQueryShellConstants( qShell.getParseTree(), replacementConstantsRow, false );
		logger.trace( "Done rewriting, trying to parse: {}", rewrittenQueryString );
		Query rewrittenQuery = new Query( rewrittenQueryString, parser );
		return rewrittenQuery;

	}


	/**
	 * Figure out what the cache keys are for each entry in splitResults.
	 */
	public ArrayList<List<String>> getCacheKeysForResults( ArrayList<List<QueryResult>> splitResults, QueryVectorizationPlan plan ) {

		logger.trace( "Getting cache keys for results!" );
		ArrayList<List<String>> cacheKeys = new ArrayList<>( splitResults.size() );
		for( int i = 0; i < splitResults.size(); i++ ) {
			cacheKeys.add( new LinkedList<String>() );
		}

		AntlrParser parser = new AntlrParser();
		DependencyGraph dependencyGraph = plan.getDependencyGraph();
		List<QueryIdentifier> orderedQueryIds = plan.getOrderedQueries();
		Iterator<QueryIdentifier> orderedQueryIterator = orderedQueryIds.iterator();
		int queryPos = 0;
		while( orderedQueryIterator.hasNext() ) {
			QueryIdentifier currentQueryId = orderedQueryIterator.next();
			logger.trace( "Making cacheKeys for query: {}", currentQueryId );
			Query queryShell = plan.getQueryShell( currentQueryId ); 
			
			// If this is a base query with no mappings at level 0, we execute it only once
			if( dependencyGraph.isBaseQuery( currentQueryId ) && dependencyGraph.getTopologicalHeights().get( currentQueryId ) == dependencyGraph.getMaxHeight() ) {
				logger.trace( "Determined that {} is a base query", currentQueryId );
				assert splitResults.get( queryPos ).size() == 1;
				logger.trace( "Setting queryPos {}", queryPos );
				String queryText = dependencyGraph.getBaseQueryText( queryShell.getId() );
				if( queryText == null ) {
					queryText = queryShell.getQueryString();
				}
				String cacheKey = String.valueOf( queryText.hashCode() );
				logger.debug( "BQ: Caching query: \"{}\" with cacheKey: {} vs {} ", queryText, queryShell.getCacheKey(), cacheKey );
				cacheKeys.set( queryPos, new LinkedList<String>() );
				cacheKeys.get( queryPos ).add( cacheKey );
			// Otherwise, we need to fill in what the parameters would have been to the
			// original queries and construct a Query to get what their cacheKey should be.
			} else {
				logger.trace( "Determined that {} is NOT a base query", currentQueryId );
				cacheKeys.set( queryPos, new LinkedList<String>() );

				Collection<QueryMappingEntry> qmes = dependencyGraph.getQueryDependencies( currentQueryId );
				logger.trace( "Found QMES: {}", qmes );
				List<List<QueryResult>> relevantResults = new ArrayList<>();
				List<Multimap<Integer, String>> relevantMappings = new ArrayList<>();
				List<Integer> relevantResultLevels = new LinkedList<>();

				// XXX it is possible (in general) that we end up with a base query at a non-zero topological level
				// that has no mappings to it. In this case, it will be zippered with some query from a later level.
				
				if( qmes.isEmpty() ) {
					Map<QueryIdentifier, Integer> heightMap = dependencyGraph.getTopologicalHeights();
					int currentQueryLevel = dependencyGraph.getMaxHeight() - heightMap.get( currentQueryId );
					Multimap<Integer, QueryIdentifier> depthMap = dependencyGraph.getTopologicalDepthToQueryIdMap();
					Collection<QueryIdentifier> queryIdsAtThisLevel = depthMap.get( currentQueryLevel );
					assert queryIdsAtThisLevel.size() > 1;
					Iterator<QueryIdentifier> queryIdsIterator = queryIdsAtThisLevel.iterator();

					// Could be multiple other base queries zipped at this level.
					while( queryIdsIterator.hasNext() ) {
						QueryIdentifier otherQueryId = queryIdsIterator.next();
						qmes = dependencyGraph.getQueryDependencies( otherQueryId );
						if( !qmes.isEmpty() ) {
							break;
						}
					}
					assert !qmes.isEmpty();

					// We care only about the number of follow-up queries we would issue, compute it based on available results from prior levels.
					int numberOfFollowUpQueries = plan.getRelevantResultsAndMappings( qmes, orderedQueryIds, splitResults, relevantResults, relevantMappings, relevantResultLevels );
					for( int queryInstanceNumber = 0; queryInstanceNumber < numberOfFollowUpQueries; queryInstanceNumber++ ) {
						String queryText = dependencyGraph.getBaseQueryText( queryShell.getId() );
						String cacheKey = String.valueOf( queryText.hashCode() );
						logger.debug( "BQ: Caching query: \"{}\" with cacheKey: {} vs {} ", queryText, queryShell.getCacheKey(), cacheKey );
						cacheKeys.set( queryPos, new LinkedList<String>() );
						cacheKeys.get( queryPos ).add( cacheKey );
					}

					// Done, go next
					continue;
				}

				int numberOfFollowUpQueries = plan.getRelevantResultsAndMappings( qmes, orderedQueryIds, splitResults, relevantResults, relevantMappings, relevantResultLevels );
				logger.trace( "Computing cache keys for number of follow up queries: {}", numberOfFollowUpQueries );

				for( int queryInstanceNumber = 0; queryInstanceNumber < numberOfFollowUpQueries; queryInstanceNumber++ ) {
					logger.trace( "Doing {} query rewrite", queryInstanceNumber);
					String queryText = dependencyGraph.getBaseQueryText( currentQueryId );
					Query rewrittenQuery = rewriteSingleQuery( queryInstanceNumber, queryShell, queryText, parser, relevantResults, relevantMappings, relevantResultLevels );
					if( rewrittenQuery != null ) {
						logger.trace( "Adding to querypos {}, queryNumber: {}", queryPos, queryInstanceNumber );
						logger.trace( "Determined {} queryString is {}", queryPos, rewrittenQuery.getQueryString() );

						logger.debug( "IQ: Caching query: \"{}\", cacheKey: {} vs {} ", rewrittenQuery.getQueryString(), rewrittenQuery.getCacheKey(), rewrittenQuery.getQueryString().hashCode() );
						cacheKeys.get( queryPos ).add( rewrittenQuery.getCacheKey() );
					} else {
						logger.trace( "Obtained null query!" );
						cacheKeys.get( queryPos ).add( null );
					}
				}
			}
			queryPos++;
		} 
		return cacheKeys;
	}
}
