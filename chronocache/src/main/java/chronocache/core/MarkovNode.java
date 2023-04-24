package chronocache.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set; 
import java.util.HashSet;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import chronocache.core.QueryMappingEntry;
import chronocache.core.MappingEntry;
import chronocache.core.trackers.HistoryManager;

public class MarkovNode {

	private Map<MarkovNode, Long> outEdges;
	private Set<MarkovNode> inEdges;
	private Set<QueryIdentifier> relatedQueriesWithNoMappings;
	private QueryIdentifier id;
	private long hits;
	private Logger logger;
	private QueryResult lastResultSet;
	private List<String> lastInputParams;
	private String lastQueryString;
	private MappingManager mappingManager;

	/**
	 * A representation of a markov node, used for making predictions
	 * @param id
	 */
	public MarkovNode( QueryIdentifier id ) {
		outEdges = new HashMap<>();
		inEdges = new HashSet<>();
		relatedQueriesWithNoMappings = new HashSet<>();
		this.id = id;
		this.hits = 0;
		lastResultSet = null;
		lastInputParams = null;
		lastQueryString = null;
		mappingManager = new MappingManager();
		logger = LoggerFactory.getLogger( this.getClass() );
	}


	public String getQueryString() {
		return lastQueryString;
	}

	public void addQueryString( String query ) {
		lastQueryString = query;
	}

	/**
	 * Check if the related query has been previous examined for
	 * input mappings and not had any
	 */
	public boolean hasNoMappings( QueryIdentifier relQuery ) {
		return relatedQueriesWithNoMappings.contains( relQuery );
	}

	/**
	 * Confirm with the tracking subsystem that there are no mappings
	 * from the current query to relQuery
	 */
	public void confirmNoMappings( QueryIdentifier relQuery ) {
		relatedQueriesWithNoMappings.add( relQuery );
	}

	/**
	 * Add a new set of input parameters for this query
	 */
	public void addInputParameters( List<String> params ) {
		logger.trace( "Adding input params {} for qid {}", params, this.getId().getId() );
		lastInputParams = params;
	}

	/**
	 * Check if there are input parameters for this query
	 */
	public boolean hasInputParameters() {
		return lastInputParams != null;
	}

	/**
	 * Get the last input parameters we recorded for this query
	 */
	public List<String> getLastInputParams() {
		return lastInputParams;
	}

	/**
	 * Return the last result set seen for this query
	 */
	public QueryResult getPreviousResultSet() {
		return lastResultSet;
	}

	/**
	 * Add new resultset for tracking purposes
	 */
	public void addNewResultSet( QueryResult newResultSet ) {
		logger.trace( "Adding resultset {} for qid {}", newResultSet, this.getId().getId() );
		lastResultSet = newResultSet;
	}

	/**
	 * Check if we have a result set for this query
	 */
	public boolean hasPreviousResultSet() {
		return lastResultSet != null;
	}

	/**
	 * Add parameter mappings from this node to relQuery
	 */
	public void addMappings( QueryIdentifier relQuery, Multimap<Integer, String> mappings, int mappingRowNum ) {
		mappingManager.addToHistory( relQuery, mappings, mappingRowNum );
	}

	/**
	 * Lookup the mappings between this query and relQuery
	 */
	public Multimap<Integer, String> lookupMappings( QueryIdentifier relQuery ) {
		return mappingManager.getMappingsIfReady( relQuery );
	}

	/**
	 * Lookup the mapping delta between this query and relQuery, used to determine
	 * if/how results are being used by relQuery
	 */
	public int lookupMappingStride( QueryIdentifier relQuery ) {
		return mappingManager.getMappingStride( relQuery );
	}

	/**
	 * Used by the Markov Network construction algorithm to increase the number of
	 * times we've tested which other queries occur within some interval of this one
	 */
	public void increaseHitCounter() {
		hits++;
	}
	
	/**
	 * Add another instance of the query determined by dest occurring within some interval
	 * of this one
	 * @param dest
	 */
	public void addEdgeTraversal( MarkovNode dest ) {
		long traversalCount = 0;
		if( outEdges.containsKey( dest ) ) {
			traversalCount = outEdges.get( dest );
		}
		outEdges.put( dest, ++traversalCount );
		dest.inEdges.add( this );
	}

	/**
	 * Determine all prior queries that have mappings for this node,
	 * and return a map of that query's id to the mappings they have
	 */
	public Map<QueryIdentifier, QueryMappingEntry> getPriorQueryMappingEntries() {
		logger.trace( "Looking for prior query mappings on qid {}", this.getId().getId() );
		Iterator<MarkovNode> it = inEdges.iterator();
		Map<QueryIdentifier, QueryMappingEntry> priorQueryMappings = new HashMap<>();
		while( it.hasNext() ) {
			MarkovNode node = it.next();
			logger.trace( "Checking if qid {} has any mappings...", node.getId().getId() );
			Multimap<Integer, String> queryMappings = node.lookupMappings( this.getId() );
			if( queryMappings != null && !queryMappings.isEmpty() ) {
				Query query = new Query( lastQueryString );
				QueryMappingEntry qme = new QueryMappingEntry( node.getId(), query, queryMappings );
				priorQueryMappings.put( node.getId(), qme );
			}
		}
		return priorQueryMappings;
	}

	/**
	 * Determine all prior queries that have mappings for this node,
	 * and return a map of that query's id to the mappings they have
	 */
	public Multimap<QueryIdentifier, MappingEntry> getPriorMappingEntries() {
		logger.trace( "Looking for prior query mappings to qid {}", this.getId().getId() );
		Multimap<QueryIdentifier, MappingEntry> priorQueryMappings = ArrayListMultimap.create();

		for( MarkovNode node : inEdges ) {
			logger.trace( "Checking if qid {} has any mappings...", node.getId().getId() );
			Multimap<Integer, String> queryMappings = node.lookupMappings( this.getId() );
			if( queryMappings != null && !queryMappings.isEmpty() ) {
				for( Map.Entry<Integer, String> entry : queryMappings.entries() ) {
					MappingEntry me = new MappingEntry( this.getId(), node.getId(), entry.getKey(), entry.getValue() );
					priorQueryMappings.put( node.getId(), me );
				}
			}
		}

		return priorQueryMappings;
	}

	/**
	 * Determine all prior queries that have mappings for this node.
	 * Return a map of positions to mappings for that position.
	 */
	public Multimap<Integer, MappingEntry> getPriorMappingEntriesByPosition() {
		logger.trace( "Looking for prior query mappings to qid {}", this.getId().getId() );
		Multimap<Integer, MappingEntry> priorQueryMappings = ArrayListMultimap.create();

		for( MarkovNode node : inEdges ) {
			logger.trace( "Checking if qid {} has any mappings...", node.getId().getId() );
			Multimap<Integer, String> queryMappings = node.lookupMappings( this.getId() );
			if( queryMappings != null && !queryMappings.isEmpty() ) {
				for( Map.Entry<Integer, String> entry : queryMappings.entries() ) {
					MappingEntry me = new MappingEntry( this.getId(), node.getId(), entry.getKey(), entry.getValue() );
					priorQueryMappings.put( me.getConstantPosition(), me );
				}
			}
		}

		return priorQueryMappings;
	}

	/**
	 * Determine all forward queries that have mappings from this node,
	 * and return a map of that query's id to the mappings they have
	 */
	public Map<QueryIdentifier, Multimap<Integer, String>> getForwardQueryMappings() {
		logger.trace( "Looking for forward query mappings on qid {}", this.getId().getId() );

		Iterator<MarkovNode> it = outEdges.keySet().iterator();
		Map<QueryIdentifier, Multimap<Integer, String>> forwardQueryMappings = new HashMap<>();
		while( it.hasNext() )
		{
			MarkovNode node = it.next();
			logger.trace( "Checking if qid {} has any mappings...", node.getId().getId() );
			Multimap<Integer, String> queryMappings = this.lookupMappings( node.getId() );
			if( queryMappings != null && !queryMappings.isEmpty() ) {
				forwardQueryMappings.put( node.getId(), queryMappings );
			}
		}

		return forwardQueryMappings;
	}
	
	/**
	 * Get the identifier of the query this node represents
	 * @return
	 */
	public QueryIdentifier getId() {
		return id;
	}
	
	/**
	 * Get the number of times we've tested what other queries occur within some interval
	 * of this one
	 * @return
	 */
	public long getHitCount() {
		return hits;
	}
	
	/**
	 * Frequency based approach to computing the probability of another query occurring
	 * within some time of this one
	 * @param dest
	 * @return probability
	 */
	public double computeEdgeTraversalProbability( MarkovNode dest ) {
		if( !outEdges.containsKey( dest ) ) {
			//Perhaps smooth to almost 0?
			return 0;
		}
		return (double) outEdges.get( dest ) / (double) hits;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if( this == obj )
			return true;
		if( obj == null )
			return false;
		if( getClass() != obj.getClass() )
			return false;
		MarkovNode other = (MarkovNode) obj;
		if( id == null ) {
			if( other.id != null )
				return false;
		} else if( !id.equals( other.id ) )
			return false;
		return true;
	}

	/**
	 * Get a list of query ids that we think are related to the this one
	 * @return
	 */
	public List<QueryIdentifier> getProbabilisticallyRelatedInQueries() {
		List<QueryIdentifier> relatedQueries = new LinkedList<QueryIdentifier>();
		logger.trace( "Asked to find related queries for QID {}", this.getId().getId() );
		for( MarkovNode src : inEdges ) {
			//TODO: should other queries have a base hit count before computing their probability
			if( src.computeEdgeTraversalProbability( this ) >= Parameters.SPECULATIVE_EXECUTION_THRESHOLD ) {
				logger.trace( "QID {} is likely related - Hit count {}", src.getId().getId(), src.getHitCount() );
				relatedQueries.add( src.getId() );
			} else {
				logger.trace( " QID {} is likely not related, prob : {}", src.getId().getId(), src.computeEdgeTraversalProbability( this ) );
			}
		}
		return relatedQueries;
	}

	/**
	 * Get a list of query ids that we think are related to the this one
	 * @return
	 */
	public List<QueryIdentifier> getProbabilisticallyRelatedOutQueries() {
		List<QueryIdentifier> relatedQueries = new LinkedList<QueryIdentifier>();
		logger.trace( "Asked to find related queries for QID {}", this.getId().getId() );
		for( MarkovNode dest : outEdges.keySet() ) {
			//TODO: should other queries have a base hit count before computing their probability
			if( computeEdgeTraversalProbability( dest ) >= Parameters.SPECULATIVE_EXECUTION_THRESHOLD ) {
				logger.trace( "QID {} is likely related - Hit count {}", dest.getId().getId(), dest.getHitCount() );
				relatedQueries.add( dest.getId() );
			} else {
				logger.trace( " QID {} is likely not related, prob : {}", dest.getId().getId(), computeEdgeTraversalProbability( dest ) );
			}
		}
		return relatedQueries;
	}

	/**
	 * Create a pruned, undirected version of the edges we can see as this MarkovNode
	 * This is a good-enough-for-testing implementation and will need to be cleaned up in the future.
	 * @param pruneThreshold
	 * @return
	 */
	public List<QueryIdentifier> transformNode( double pruneThreshold ) {
		// Create this list of nodes which have bidirectional edges with prob. greater than pruneThreshold
		List<QueryIdentifier> desirableEdges = new LinkedList<QueryIdentifier>();
		Set<MarkovNode> bidirectionalEdges = new HashSet<MarkovNode>();

		// This is a deep copy to prevent this node's HashMap being altered by pruning
		bidirectionalEdges.addAll( outEdges.keySet() );
		bidirectionalEdges.retainAll( inEdges );

		// Consider all the bidirectionalEdge edges
		for( MarkovNode bidirectionalEdge : bidirectionalEdges ) {
			// Add an edge only if both directions of the edge are more than the desired threshold
			if( computeEdgeTraversalProbability( bidirectionalEdge ) >= pruneThreshold &&
					bidirectionalEdge.computeEdgeTraversalProbability( this ) >= pruneThreshold ) {
				desirableEdges.add( bidirectionalEdge.getId() );
			}
		}

		return desirableEdges;
	}

	/**
	 * Get a map from queryIdentifier to MarkovNode.
	 */
	public Map<QueryIdentifier, MarkovNode> getConnectedNodes() {
		Map<QueryIdentifier, MarkovNode> connectedNodes = new HashMap<>();
		for( MarkovNode node : outEdges.keySet() ) {
			connectedNodes.put( node.getId(), node );
		}
		return connectedNodes;
	}
}
