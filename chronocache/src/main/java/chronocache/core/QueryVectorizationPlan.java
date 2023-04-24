package chronocache.core;

import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.ParserPool;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.core.qry.QueryIdentifier;

import java.lang.Iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

public class QueryVectorizationPlan implements Iterable<QueryVectorizationPlanNode> {

	public class Node {
		QueryIdentifier queryId;
		Integer height;
		Integer joinQueryNumberIfBase;

		public Node( QueryIdentifier queryId, Integer height, Integer joinQueryNumberIfBase ) {
			this.queryId = queryId;
			this.height = height;
			this.joinQueryNumberIfBase = joinQueryNumberIfBase;
		}

		public QueryIdentifier getQueryId() {
			return queryId;
		}

		public Integer getHeight() {
			return height;
		}

		public Integer getJoinQueryNumberIfBase() {
			return joinQueryNumberIfBase;
		}

	}

	protected DependencyGraph dependencyGraph;
	protected Map<QueryIdentifier, Query> queryShells;
	protected List<QueryVectorizationPlanNode> vectorizationPlan;

	private Logger logger = LoggerFactory.getLogger( this.getClass() );


	/**
	 * Return a plan that indicates how we vectorized the provided dependencyGraph.
	 */
	public QueryVectorizationPlan(
		DependencyGraph dependencyGraph,
		Map<QueryIdentifier, Query> queryShells,
		List<QueryVectorizationPlanNode> vectorizationPlan 
	) {
		this.dependencyGraph = dependencyGraph;
		this.queryShells = queryShells;
		this.vectorizationPlan = vectorizationPlan;
	}

	public static QueryVectorizationPlan constructVectorizationPlan(
		DependencyGraph dependencyGraph,
		Map<QueryIdentifier, Query> queryShells
	) {
		List<QueryVectorizationPlanNode> nodes = new LinkedList<>();

		Integer i = 0;
		Map<QueryIdentifier, Integer> topologicalHeights = dependencyGraph.getTopologicalHeights();
		for( QueryIdentifier queryId : dependencyGraph.getTopologicallyOrderedQueries() ) {
			Integer height = topologicalHeights.get( queryId );
			Integer joinQueryNumber = dependencyGraph.getFirstTopologicalQueryIdAtHeightMap().get( height );
			nodes.add( new QueryVectorizationPlanNode( queryId, height, joinQueryNumber ) );
			i++;
		}
		return new QueryVectorizationPlan( dependencyGraph, queryShells, nodes );

	}

	@Override
	public Iterator<QueryVectorizationPlanNode> iterator() {
		return vectorizationPlan.iterator();
	}

	public Multimap<Integer, QueryIdentifier> getTopologicalDepthToQueryIdMap() {
		return dependencyGraph.getTopologicalDepthToQueryIdMap();
	}

	public Integer getMaxTopologicalHeight() {
		return dependencyGraph.getMaxHeight();
	}

	/**
	 * Returns how many queries this plan vectorized.
	 */
	public Integer size() {
		return dependencyGraph.getTopologicallyOrderedQueries().size();
	}

	/**
	 * Get the topological order of these queries.
	 */
	public List<QueryIdentifier> getOrderedQueries() {
		return dependencyGraph.getTopologicallyOrderedQueries();
	}

	/**
	 * get the "slot" numbers of dependency queries for the query in "slot" queryNumber
	 */
	public List<Integer> getQueryDependenciesInTopologicalOrder( int queryNumber ) {
		QueryIdentifier curQueryId = dependencyGraph.getTopologicallyOrderedQueries().get( queryNumber );
		Collection<QueryMappingEntry> qmes = dependencyGraph.getQueryDependencies( curQueryId );
		List<Integer> topologicalQueryDependencies = new LinkedList<Integer>();
		for( QueryMappingEntry qme : qmes ) {
			QueryIdentifier dependencyId = qme.getDependencyQueryId();
			//Not the most efficient thing in the world, but we're joining like 5 queries
			//so it should be fine
			for( int i = 0; i < queryNumber; i++ ) {
				if( dependencyGraph.getTopologicallyOrderedQueries().get( i ) == dependencyId ) {
					topologicalQueryDependencies.add( i );
					break;
                }
			}
		}
		return topologicalQueryDependencies;
	}

	/**
	 * Determine which columns were selected by the query in slot queryNumber.
	 */
	public List<String> getSelectedColumnsForQuery( int queryNumber ) {
		logger.trace( "Got request for query Number: " + queryNumber );
		logger.trace( "Size: " + dependencyGraph.getTopologicallyOrderedQueries().size() );
		QueryIdentifier queryId = dependencyGraph.getTopologicallyOrderedQueries().get( queryNumber );
		logger.trace( "Got request for query Number: " + queryNumber );
		Query queryShell = queryShells.get( queryId );
		List<String> selectedCols = queryShell.getQueryMetadata().getSelectedColumnsInQuery();
		Map<Integer,String> colToAliasMap = queryShell.getQueryMetadata().getPositionToAliasesMap();

		List<String> qualifiedCols = new LinkedList<String>();

		int i = 0;
		for( String colName : selectedCols ) {
			String aliasName = colToAliasMap.get( i );
			i++;
			if( aliasName != null ) {
				qualifiedCols.add( aliasName );
				continue;
			}

			//Split in case they qualified the column already
			if( colName.contains( "." ) ) {
				String pieces[] = colName.split( "." );
				qualifiedCols.add(  pieces[1] );
			} else {
				qualifiedCols.add( colName );
			}
		}
		return qualifiedCols;
	}


	private class MinMax {
		int min;
		int max;
		public MinMax( int min, int max ) {
			this.min = min;
			this.max = max;
		}

		public int getMin() {
			return min;
		}

		public int getMax() {
			return max;
		}
	}

	public MinMax getMinMax( Set<Integer> levels ) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		logger.debug( "Finding min and max levels: {}", levels );
		for( Integer level : levels ) {
			if( level < min ) {
				min = level;
			}
			if( level > max ) {
				max = level;
			}
		}
		logger.debug( "min {} and max {}", min, max );
		return new MinMax( min, max );
	}

	public static int getSlotNumber( QueryIdentifier queryId, List<QueryIdentifier> queryIds ) {
		int i = 0;
		for( QueryIdentifier curQueryId : queryIds ) {
			if( curQueryId == queryId ) {
				return i;
			}
			i++;
		}
		return -1;
	}


	public void addMissingLevels( int min, int max, Set<Integer> allLevelsInRelevantResults, List<QueryIdentifier> orderedQueryIds, ArrayList<List<QueryResult>> splitResults, List<List<QueryResult>> relevantResults, List<Multimap<Integer, String>> relevantMappings, List<Integer> relevantResultLevels ) {
		Multimap<Integer,QueryIdentifier> topologicalDepthMap = getTopologicalDepthToQueryIdMap();
		logger.debug( "Going to add missing levels with min {} and max {}", min, max );
		for( int i = min; i <= max; i++ ) {
			if( allLevelsInRelevantResults.contains( i ) ) {
				continue;
			}

			Collection<QueryIdentifier> queriesAtThisDepth = topologicalDepthMap.get( i );

			// Only need one from this level.
			QueryIdentifier relevantQueryId = queriesAtThisDepth.iterator().next();
			int slotNumber = getSlotNumber( relevantQueryId, orderedQueryIds );
			List<QueryResult> resultSets = splitResults.get( slotNumber );

			// NULL mappings so we know that these are only useful to determine which row we should use
			// from result sets at higher topological levels.
			relevantResults.add( resultSets );
			relevantResultLevels.add( i );
			relevantMappings.add( null );
		}
	}

	/**
	 * Figure out which results and mappings are relevant for each of the queries.
	 */
	public int getRelevantResultsAndMappings(
		Collection<QueryMappingEntry> qmes,
		List<QueryIdentifier> orderedQueryIds,
		ArrayList<List<QueryResult>> splitResults,
		List<List<QueryResult>> relevantResults,
		List<Multimap<Integer,String>> relevantMappings,
		List<Integer> relevantResultLevels
	) {
		int numberOfFollowUpQueries = 0;
		int curNumberOfFollowUpQueries = 0;

		Set<Integer> levelsOfPriorQueries = new HashSet<>();

		for( QueryMappingEntry qme : qmes ) {
			QueryIdentifier priorQueryId = qme.getDependencyQueryId();
			Multimap<Integer, String> mappingsFromPriorQuery = qme.getQueryMappings();
                                
			int priorQueryPosition = orderedQueryIds.indexOf( priorQueryId );

			int totalFollowupQueries = 0;
			//The number of follow up queries is the number of rows we have from a prior query
			for( QueryResult priorResult : splitResults.get( priorQueryPosition ) ) {
				if( priorResult.getSelectResult().isEmpty() ) {
					totalFollowupQueries += 1;
				} else {
					totalFollowupQueries += priorResult.getSelectResult().size();
				}
			}
			curNumberOfFollowUpQueries = totalFollowupQueries;

			logger.trace( "Found mappings {}", mappingsFromPriorQuery );
			relevantResults.add( splitResults.get( priorQueryPosition ) );
			relevantMappings.add( mappingsFromPriorQuery );
			int depth = getMaxTopologicalHeight() - dependencyGraph.getTopologicalHeights().get( priorQueryId );
			relevantResultLevels.add( depth );
			levelsOfPriorQueries.add( depth );

			if( curNumberOfFollowUpQueries > numberOfFollowUpQueries ) {
				numberOfFollowUpQueries = curNumberOfFollowUpQueries;
			}

		}

		MinMax minMax = getMinMax( levelsOfPriorQueries );
		logger.trace( "current RelevantResultLevels: {}, going to add missing.", relevantResultLevels );
		addMissingLevels( minMax.getMin(), minMax.getMax(), levelsOfPriorQueries, orderedQueryIds, splitResults, relevantResults, relevantMappings, relevantResultLevels );
		logger.trace( "current RelevantResultLevels: {}, done adding missing.", relevantResultLevels );
		

		logger.debug( "Computed number of follow up queries: {}", numberOfFollowUpQueries );
		return numberOfFollowUpQueries;
	}

	public DependencyGraph getDependencyGraph() {
		return dependencyGraph;
	}

	public Query getQueryShell( QueryIdentifier queryId ) {
		return queryShells.get( queryId );
	}

}
