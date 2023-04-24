package chronocache.core;

import chronocache.core.parser.AntlrQueryMetadata;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Collection;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a set of dependencies among executable queries
 */
public class DependencyGraph {

	private enum DependencyGraphType {
		ADQ,
		FDQ,
		LOOP
	}

	Set<QueryIdentifier> nodes;
	Multimap<QueryIdentifier, QueryIdentifier> forwardEdges;
	Multimap<QueryIdentifier, QueryMappingEntry> queryDependencyGraph;
	Map<QueryIdentifier, String> providedBaseQueryTexts;
	Map<QueryIdentifier, Query> allQueryShells;
	Map<QueryIdentifier, Integer> topologicalHeights;
	Multimap<Integer, QueryIdentifier> topologicalDepthToQueryIdMap;
	List<QueryIdentifier> topologicallyOrderedQueries;
	Map<Integer, Integer> firstTopologicalQueryIdAtHeight;
	DependencyGraphType definitionLevel;
	Integer maxHeight;

	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	public DependencyGraph() {
		nodes = new HashSet<QueryIdentifier>();
		forwardEdges = HashMultimap.create();
		queryDependencyGraph = HashMultimap.create();
		providedBaseQueryTexts = new HashMap<>();
		allQueryShells = new HashMap<>();
		topologicalHeights = null;
		topologicallyOrderedQueries = null;
		topologicalDepthToQueryIdMap = null;
		firstTopologicalQueryIdAtHeight = new HashMap<>();
		maxHeight = -1;
		definitionLevel = DependencyGraphType.ADQ;
	}

	// Deep copy the given dependency graph
	public DependencyGraph( DependencyGraph dependencyGraph ) {
		assert dependencyGraph != null;
		nodes = new HashSet<QueryIdentifier>();
		nodes.addAll( dependencyGraph.nodes );

		forwardEdges = HashMultimap.create();
		forwardEdges.putAll( dependencyGraph.forwardEdges );

		queryDependencyGraph = HashMultimap.create();
		queryDependencyGraph.putAll( dependencyGraph.queryDependencyGraph );

		providedBaseQueryTexts = new HashMap<>();
		providedBaseQueryTexts.putAll( dependencyGraph.providedBaseQueryTexts );

		allQueryShells = new HashMap<>();
		allQueryShells.putAll( dependencyGraph.allQueryShells );

		if( dependencyGraph.topologicalHeights != null &&
			dependencyGraph.topologicallyOrderedQueries != null &&
			dependencyGraph.topologicalDepthToQueryIdMap != null
		) {
			topologicalHeights = new HashMap<>();
			topologicalHeights.putAll( dependencyGraph.topologicalHeights );

			topologicallyOrderedQueries = new LinkedList<>();
			topologicallyOrderedQueries.addAll( dependencyGraph.topologicallyOrderedQueries );

			topologicalDepthToQueryIdMap = HashMultimap.create();
			topologicalDepthToQueryIdMap.putAll( dependencyGraph.topologicalDepthToQueryIdMap );
		}

		firstTopologicalQueryIdAtHeight = new HashMap<>();
		firstTopologicalQueryIdAtHeight.putAll( dependencyGraph.firstTopologicalQueryIdAtHeight );

		maxHeight = dependencyGraph.maxHeight;
		definitionLevel = dependencyGraph.definitionLevel;
	}

	public Set<QueryIdentifier> getAllQueryIds() {
		return nodes;
	}

	// Takes all the information from dependencyGraph and adds it to this one
	public void mergeWith( DependencyGraph dependencyGraph ) {
		logger.debug( "Merging this dependency graph with: {}", dependencyGraph );
		logger.trace( "Adding their nodes : {}", dependencyGraph.nodes);
		nodes.addAll( dependencyGraph.nodes );
		forwardEdges.putAll( dependencyGraph.forwardEdges );
		queryDependencyGraph.putAll( dependencyGraph.queryDependencyGraph );
		providedBaseQueryTexts.putAll( dependencyGraph.providedBaseQueryTexts );
		logger.trace( "Adding their base query shells: {}", dependencyGraph.allQueryShells );
		allQueryShells.putAll( dependencyGraph.allQueryShells );
	}

	public static DependencyGraph createMergedDependencyGraph( DependencyGraph d1,
			DependencyGraph d2 ) {
		DependencyGraph mergedGraph  = new DependencyGraph();
		mergedGraph.mergeWith( d1 );
		mergedGraph.mergeWith( d2 );

		return mergedGraph;
	}

	public void addDependencyForQuery( QueryIdentifier queryId, MappingEntry dependency, Query headQuery ) {
		Multimap<Integer, String> newMap = HashMultimap.create();
		newMap.put( dependency.getConstantPosition(), dependency.getColumnName() );
		QueryMappingEntry newQME = new QueryMappingEntry( dependency.getTailQuery(), headQuery, newMap );
		addDependencyForQuery( dependency.getHeadQuery(), newQME );
	}

	public void addDependencyForQuery( QueryIdentifier queryId, QueryMappingEntry dependency ) {
		logger.info( "Adding dependency id {} for queryId: {}", dependency.getDependencyQueryId(), queryId );
		logger.info( "Query Mappings: {}", dependency.getQueryMappings() );
		nodes.add( queryId );
		QueryIdentifier priorQueryId = dependency.getDependencyQueryId();
		Query dependencyQueryShell = dependency.getQueryShell();
		nodes.add( priorQueryId );
		forwardEdges.put( priorQueryId, queryId );
		queryDependencyGraph.put( queryId, dependency );
		logger.trace( "Adding dependencyQueryShell: {}, for id: {} ", dependencyQueryShell, queryId);
		allQueryShells.put( queryId, dependencyQueryShell );
	}

	public boolean containsQueryId( QueryIdentifier queryId ) {
		return nodes.contains( queryId );
	}

	public void markQueryAsBaseQuery( QueryIdentifier queryId, String queryText ) {
		providedBaseQueryTexts.put( queryId, queryText );
	}

	public void addBaseQuery( QueryIdentifier queryId, Query queryShell ) {
		logger.debug( "Adding: {} as a base query with queryShell: {}", queryId, queryShell );
		nodes.add( queryId );
		providedBaseQueryTexts.put( queryId, queryShell.getQueryString() );
		allQueryShells.put( queryId, queryShell );
	}

	public Collection<QueryMappingEntry> getQueryDependencies( QueryIdentifier queryId ) {
		return queryDependencyGraph.get( queryId );
	}

	public Multimap<QueryIdentifier, QueryMappingEntry> getAllDependencyMappings() {
		return queryDependencyGraph;
	}

	public boolean isBaseQuery( QueryIdentifier queryId ) {
		return providedBaseQueryTexts.containsKey( queryId );
	}

	public String getBaseQueryText( QueryIdentifier queryId ) {
		return providedBaseQueryTexts.get( queryId );
	}

	public void visit(
		QueryIdentifier node,
		Set<QueryIdentifier> unSeenNodes,
		Set<QueryIdentifier> tempMarkedNodes,
		Set<QueryIdentifier> permMarkedNodes,
		LinkedList<QueryIdentifier> orderedQueries
	) {
		logger.info( "Visiting node: " + node.getId() );

		unSeenNodes.remove( node );

		if( permMarkedNodes.contains( node ) ) {
			return;
		}

		if( tempMarkedNodes.contains( node ) ) {
			throw new IllegalArgumentException( "Not a DAG!" );
		}

		tempMarkedNodes.add( node );
		for( QueryIdentifier connectedNode : forwardEdges.get( node ) ) {
			logger.info( "Found connecting node: " + connectedNode.getId() );
			visit( connectedNode, unSeenNodes, tempMarkedNodes, permMarkedNodes, orderedQueries );
		}

		permMarkedNodes.add( node );
		orderedQueries.addFirst( node );
	}

	public synchronized List<QueryIdentifier> getTopologicallyOrderedQueries() {
		logger.trace( "Going to get toplogically ordered queries." );
		if( topologicallyOrderedQueries != null ) {
			return topologicallyOrderedQueries;
		}

		logger.trace( "topologicallyOrderedQueries is null on graph {}, doing sort!", this );
		topologicallyOrderedQueries = topologicalSort();

		assert topologicalHeights == null;
		topologicalHeights = new HashMap<>();

		if( topologicallyOrderedQueries.isEmpty() ) {
			return topologicallyOrderedQueries;
		}

		ListIterator<QueryIdentifier> reverseTopologicalIterator = topologicallyOrderedQueries.listIterator( topologicallyOrderedQueries.size() );

		// Set up the height counts to determine on which RNs to join
		while( reverseTopologicalIterator.hasPrevious() ) {
			QueryIdentifier curQueryId = reverseTopologicalIterator.previous();
			Integer height = 0;
			if( topologicalHeights.containsKey( curQueryId ) ) {
				height = topologicalHeights.get( curQueryId );
			} else {
				topologicalHeights.put( curQueryId, height );
			}

			height++;
			Collection<QueryMappingEntry> curDependencies = getQueryDependencies( curQueryId );
			for( QueryMappingEntry qme : curDependencies ) {
				QueryIdentifier dependencyQueryId = qme.getDependencyQueryId();
				if( topologicalHeights.containsKey( dependencyQueryId ) ) {
					Integer recordedHeight = topologicalHeights.get( dependencyQueryId );
					if( height > recordedHeight ) {
						topologicalHeights.put( dependencyQueryId, height );
					}
				} else {
					topologicalHeights.put( dependencyQueryId, height );
				}
			}
		}

		Integer i = 0;
		for( QueryIdentifier queryId : topologicallyOrderedQueries ) {
			Integer height = topologicalHeights.get( queryId );
			logger.trace( "Got height {} for queryId {}", height, queryId );
			if( height > maxHeight ) {
				maxHeight = height;
			}
			if( !firstTopologicalQueryIdAtHeight.containsKey( height ) ) {
				firstTopologicalQueryIdAtHeight.put( height, i );
			}
			i++;
		}

		topologicalDepthToQueryIdMap = HashMultimap.create();
		for( QueryIdentifier queryId : topologicallyOrderedQueries ) {
			Integer height = topologicalHeights.get( queryId );
			Integer depth = maxHeight - height;
			logger.trace( "Going to put depth {} in map for queryId {}, got height {} with maxHeight {}", depth, queryId, height, maxHeight );
			topologicalDepthToQueryIdMap.put( depth, queryId );
		}

		return topologicallyOrderedQueries;
	}

	public Map<QueryIdentifier, Integer> getTopologicalHeights() {
		return topologicalHeights;
	}

	public Multimap<Integer, QueryIdentifier> getTopologicalDepthToQueryIdMap() {
		return topologicalDepthToQueryIdMap;
	}

	public Integer getMaxHeight() {
		return maxHeight;
	}

	public Map<Integer,Integer> getFirstTopologicalQueryIdAtHeightMap() {
		return firstTopologicalQueryIdAtHeight;
	}

	private List<QueryIdentifier> topologicalSort() {
		logger.info( "Calling topological sort on graph: {}", this );
		LinkedList<QueryIdentifier> orderedDependencies = new LinkedList<>();
		Set<QueryIdentifier> unSeenNodes = new HashSet<>( nodes );
		Set<QueryIdentifier> tempMarkedNodes = new HashSet<>();
		Set<QueryIdentifier> permMarkedNodes = new HashSet<>();

		while( !unSeenNodes.isEmpty() ) {
			QueryIdentifier nextNode = unSeenNodes.iterator().next();
			visit( nextNode, unSeenNodes, tempMarkedNodes, permMarkedNodes, orderedDependencies );
		}

		assert orderedDependencies.size() == nodes.size();
		return orderedDependencies;
	}

	public QueryVectorizationPlan getVectorizationPlan() {

		if( !isVectorizable() ) {
			return null;
		}

		List<QueryIdentifier> topologicalOrder = getTopologicallyOrderedQueries();
		return QueryVectorizationPlan.constructVectorizationPlan( this, allQueryShells );
	}

	/**
	 * Check if all our dependencies are executed before we are in the topological order
	 */
	private boolean checkDependenciesSatisfied(
		QueryIdentifier currentOrderedQueryId,
		List<QueryIdentifier> orderedQueryIds,
		Collection<QueryMappingEntry> dependenciesForQuery
	) {
		// Changed to for loops here since previous iterators were not iterating over
		// all the desired dependencies.
		for( QueryMappingEntry qme : dependenciesForQuery ) {
			QueryIdentifier queryDependencyId = qme.getDependencyQueryId();
			for( QueryIdentifier orderedQueryId : orderedQueryIds ) {
				if( orderedQueryId.equals( currentOrderedQueryId ) ) {
					return false;
				}
				if( orderedQueryId.equals( queryDependencyId ) ) {
					break;
				}
			}
		}
		return true;
	}

	public boolean isVectorizable() {
		List<QueryIdentifier> orderedQueries;
		try {
			 orderedQueries = getTopologicallyOrderedQueries();
		} catch( IllegalArgumentException e ) {
			logger.error( "Dependency Cycle!" );
			return false;
		}

		if( orderedQueries.isEmpty() ) {
            logger.debug( "No queries, graph is not vectorizable" );
			return false;
		}

		if( providedBaseQueryTexts.size() == orderedQueries.size() ) {
            logger.debug( "All queries are base queries, not vectorizable." );
			return false;
		}

		logger.trace( "topological order is {}.", orderedQueries );
		QueryIdentifier firstQueryId = orderedQueries.iterator().next();
		for( QueryIdentifier queryId : orderedQueries ) {
			Collection<QueryMappingEntry> dependencies = getQueryDependencies( queryId );
			if( isBaseQuery( queryId ) && queryId != firstQueryId ) {
				if( Parameters.SCALPEL_MODE || getQueryDependencies( queryId ).isEmpty() ) {
                    if( Parameters.SCALPEL_MODE ) {
                        logger.debug( "Not optimizing dependencyGraph with non-toplevel base query (SCALPEL)." );
                    } else {
                        logger.debug( "Base query without dependencies and not first, not optimizing." );
                    }
					return false;
				}
			}

			boolean areDependenciesSatisfied = checkDependenciesSatisfied( queryId, orderedQueries, dependencies );
			if( !areDependenciesSatisfied ) {
				logger.trace("{} dependencies not satisfied.", queryId);
				logger.trace("Dependencies are:\n{}", dependencies);
				return false;
			}
		}
        if( nodes.size() < 2 ) {
            logger.warn( "Not vectorizing single node graph!" );
            return false;
        }
		return true;
	}

	public boolean isSimpleVectorizable() {
		List<QueryIdentifier> orderedQueries;
		try {
			 orderedQueries = getTopologicallyOrderedQueries();
		} catch( IllegalArgumentException e ) {
			logger.error( "Dependency Cycle!" );
			return false;
		}

		logger.trace("topological order is {}.", orderedQueries);
		logger.trace("All query shells: {}", allQueryShells );
		for( QueryIdentifier queryId : orderedQueries ) {
			Query queryShell = allQueryShells.get( queryId );
			if( queryShell == null ) {
				return false;
			}
			AntlrQueryMetadata metadata = queryShell.getQueryMetadata();

			// The Simple Vectorizable Checks
			if( metadata.hasUnion() ) {
                logger.error( "Not vectorizable via simple b/c has union!" );
				return false;
			}

			// If a query is not simpleVectorizable and it is not a base query
			if( !isBaseQuery( queryId ) && !metadata.isSimpleVectorizable() ) {
				return false;
			}

			if( isBaseQuery( queryId ) && queryId != orderedQueries.iterator().next() ) {
				return false;
			}

			Collection<QueryMappingEntry> dependencies = getQueryDependencies( queryId );
			boolean areDependenciesSatisfied = checkDependenciesSatisfied( queryId, orderedQueries, dependencies );
			if( !areDependenciesSatisfied ) {
				logger.trace("{} dependencies not satisfied.", queryId);
				logger.trace("Dependencies are:\n{}", dependencies);
				return false;
			}

		}
		return nodes.size() >= 2;
	}

	public Query getQueryShellFromGraph( QueryIdentifier queryId ) {
		return allQueryShells.get( queryId );
	}

	public Collection<QueryIdentifier> getAllBaseQueryIds() {
		return providedBaseQueryTexts.keySet();
	}

	public boolean isADQ() {
		return definitionLevel == DependencyGraphType.ADQ;
	}
	
	public void markAsFDQ() {
		definitionLevel = DependencyGraphType.FDQ;
	}

	public void markAsLoop() {
		definitionLevel = DependencyGraphType.LOOP;
	}

	public void dumpToLog() {
		logger.info( "==========================================================================================" );
		logger.info( "Dumping dependency graph: {}", this );
		logger.info( "HashCode: {}", hashCode() );
		logger.info( "OrderedQueryIds: {}", topologicallyOrderedQueries );
		logger.info( "BaseQueryIds: {}", providedBaseQueryTexts.keySet() );
		logger.info( "Forward Edges: {}", forwardEdges );
		logger.info( "Topological Heights: {}", topologicalHeights );
		logger.info( "TopologicalDepthToQueryIdMap: {}", topologicalDepthToQueryIdMap );
		logger.info( "TopologicalOrder: {}", getTopologicallyOrderedQueries() );
		logger.info( "Mappings.: ");
		for( QueryIdentifier qid : queryDependencyGraph.keySet() ) {
			Collection<QueryMappingEntry> qmes = queryDependencyGraph.get( qid );
			for( QueryMappingEntry qme : qmes ) {
				logger.info( "{}", qme.toString() );
			}
		}
		logger.info( "==========================================================================================" );
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + nodes.hashCode(); 
		hash = 31 * hash + forwardEdges.hashCode();
		//hash = 31 * hash + queryDependencyGraph.hashCode(); // are my QMEs the same
		hash = 31 * hash + allQueryShells.keySet().hashCode();
		hash = 31 * hash + providedBaseQueryTexts.keySet().hashCode();
		return hash;
	}


}
