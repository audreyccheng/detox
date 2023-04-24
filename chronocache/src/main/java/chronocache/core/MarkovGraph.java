package chronocache.core;

import java.lang.Math;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Stack;
import java.util.Iterator;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;

import chronocache.core.qry.Query;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import chronocache.core.QueryMappingEntry;
import chronocache.core.MappingEntry;
import chronocache.util.UndirectedGraph;

import java.io.*;

/**
 * A representation of a markov graph
 * Used to store and lookup markov nodes
 *
 * How tracking works:
 * - We store the most recent resultSet and input parameters for a query on their
 *   MarkovNode
 * - When we would overwrite the most recent resultSet, we first check for any
 *   mappings to queries we have edges *to* and store these mappings on the local
 *   node
 * - When we are determining if we can predictively execute, we check all queries that
 *   have edges *to us* (in-edges) and check if we have enough mappings
 * - The engine itself handles constructing the query and loading in the necessary
 *   data
 * @author bjglasbe
 *
 */
public class MarkovGraph {
	private Map<QueryIdentifier, MarkovNode> nodeLookupTable;
	private static Logger logger = LoggerFactory.getLogger( MarkovGraph.class );
	private long hits;
	private long clientId;

	public MarkovGraph( long clientId ) {
		nodeLookupTable = new HashMap<QueryIdentifier, MarkovNode>();
		hits = 0;
		this.clientId = clientId;
	}

	public long getClientId() {
		return clientId;
	}

	/**
	 * Get a set of all query ids in this graph
	 */
	public Set<QueryIdentifier> getAllQueryIds() {
		return nodeLookupTable.keySet();
	}

	/**
	 * Increase the number of queries we've seen
	 */
	public void increaseHitCounter() {
		hits++;
	}

	/**
	 * Compute the probability of a query occuring
	 */
	public double computeQueryProbability( QueryIdentifier id ) {
		return (double) getNodeHits( id ) / hits;
	}
	
	/**
	 * Add a node for queryIdentifier if necessary, and return
	 * a copy of the MarkovNode for this queryId
	 */
	public MarkovNode getOrAddNode( QueryIdentifier id ) {
		MarkovNode node;
		if( (node = nodeLookupTable.get(id)) == null ) {
			logger.trace( "Creating markov node for {}", id.getId() );
			node = new MarkovNode( id );
			nodeLookupTable.put( id, node );
		}
		return node;
	}
	
	/**
	 * Return the number of times we've seen this query id
	 */
	public long getNodeHits( QueryIdentifier id ) {
		if( !nodeLookupTable.containsKey( id ) ) {
			return 0;
		}
		return nodeLookupTable.get( id ).getHitCount();
	}

	/**
	 * Get queries that are considered probabilistically related to the
	 * provided queryIdentifer (outgoing)
	 */
	public List<QueryIdentifier> getRelatedOutQueries( QueryIdentifier id ) {
		MarkovNode node = nodeLookupTable.get( id );
		if( node == null ) {
			logger.error( "Could not find node {}", id.getId() );
			return new LinkedList<QueryIdentifier>();
		}
		return node.getProbabilisticallyRelatedOutQueries();
	}

	/**
	 * Get queries that are considered probabilistically related to the
	 * provided queryIdentifer (incoming)
	 */
	public List<QueryIdentifier> getRelatedInQueries( QueryIdentifier id ) {
		MarkovNode node = nodeLookupTable.get( id );
		if( node == null ) {
			logger.error( "Could not find node {}", id.getId() );
			return new LinkedList<QueryIdentifier>();
		}
		return node.getProbabilisticallyRelatedInQueries();
	}

	public void addInputParameters( QueryIdentifier id, List<String> params ) {
		MarkovNode node = nodeLookupTable.get( id );
		if( node == null ) {
			logger.error( "Could not find node {} to add parameters", id.getId() );
			return;
		}
		node.addInputParameters( params );
	}

	/**
	 * Lookup mappings from query id to query relQuery
	 * Null if we don't have enough tracking information
	 */
	public Multimap<Integer, String> lookupMappings( QueryIdentifier id, QueryIdentifier relQuery ) {
		MarkovNode node = nodeLookupTable.get( id );
		if( node == null ) {
			logger.error( "Could not find node {} to lookup Mappings", id.getId() );
			return null;
		}
		return node.lookupMappings( relQuery );
	}

	/**
	 * Add the most recent resultSet to this MarkovNode
	 * Used for tracking purposes
	 */
	public void addResultSet( QueryIdentifier id, QueryResult resultSet ) {
		MarkovNode queryNode = nodeLookupTable.get( id );
		if( queryNode == null ) {
			logger.error( "Asked to add resultSet for qid {}, but it's not in the graph!", id.getId() );
			return;
		}

		//Add the new result set
		queryNode.addNewResultSet( resultSet );
	}

	public void addQueryString( QueryIdentifier id, String queryString ) {
		MarkovNode queryNode = nodeLookupTable.get( id );
		if( queryNode == null ) {
			logger.error( "Asked to add queryString for qid {}, but it's not in the graph!", id.getId() );
			return;
		}

		queryNode.addQueryString( queryString );
	}


	public String getQueryString( QueryIdentifier id ) {
		MarkovNode queryNode = nodeLookupTable.get( id );
		if( queryNode == null ) {
			logger.error( "Asked to add resultSet for qid {}, but it's not in the graph!", id.getId() );
			return null;
		}

		return queryNode.getQueryString();
	}

	/**
	 * Check for all possible output-input mappings from related nodes to this node.
	 * This is where blacklisting happens.
	 */
	public synchronized void findAllParameterMappingsForNode( QueryIdentifier queryId ) {
		if( queryId.getId() == -7140234440160998155L ) {
			logger.trace( "Going to check/update mappings for {}", queryId );
		}
		MarkovNode queryNode = getOrAddNode( queryId );
		List<QueryIdentifier> relatedQueries = queryNode.getProbabilisticallyRelatedInQueries();
		if( queryId.getId() == -7140234440160998155L ) {
			logger.trace( "Found related in queries: {}", relatedQueries );
		}

		// Status so we blacklist in batches and not spuriously
		boolean parameterMatchingFound = false;

		for( QueryIdentifier relQuery : relatedQueries ) {
            // Can't map to ourselves.
			if( relQuery == queryId ) {
				continue;
			}
			MarkovNode relNode = nodeLookupTable.get( relQuery );
			if( relNode == null ) {
				logger.error( "Ask to find parameter mappings {} -> {}, but input query wasn't found!", relQuery.getId(), queryNode.getId().getId() );
				continue;
			}
			if( queryId.getId() == -7140234440160998155L ) {
				logger.trace( " Looking at query: {}, was blacklisted: {}", relQuery, relNode.hasNoMappings( queryNode.getId() ) );
			}
			// If our relQuery wasn't blacklisted for having no mappings
			if( !relNode.hasNoMappings( queryNode.getId() ) ) {
				// Get the outputs and inputs we'll be testing
				List<String> inputParams = queryNode.getLastInputParams();
				QueryResult outputResult = relNode.getPreviousResultSet();
				if( queryId.getId() == -7140234440160998155L ) {
					logger.trace( "Looking at not-blacklisted query: {}", relQuery.getId() );
				}

				// Make sure we have both valid input and output. Technically we should never have null inputParams but you never know...
				if( inputParams == null || outputResult == null ) {
					// Either this query accepts no input parameters, or
					// (in some edge-case), it hasn't executed at all yet
					// Blacklist and try the next query
					//
					//
					if( queryId.getId() == -7140234440160998155L ) {
						logger.trace( "Blacklisted query: {} for null params", relQuery.getId() );
					}

					logger.trace( "Q{} blacklisted Q{} for null inputParams or outputResults.", relQuery.getId(), queryNode.getId().getId() );
					relNode.confirmNoMappings( queryNode.getId() );
					continue;
				}

				if( queryId.getId() == -7140234440160998155L ) {
					logger.trace( "Looking for mappings from: {}", relQuery.getId() );
				}

				logger.trace( "Looking for parameter mappings: node {} -> node {}", relNode.getId().getId(), queryNode.getId().getId() );
				Multimap<Integer, String> mappings = findMappingsBetweenOutputInputSets( outputResult, inputParams, queryNode.getId() );
				/*
				if( relNode.getId().getId() == 4277460828552265354L && queryNode.getId().getId() == -7140234440160998155L ) {
					logger.info( "Found mappings between {} and {}: {}", relNode.getId(), queryNode.getId(), mappings );
					logger.info( "Got outputResult: {} and input params: {}", outputResult, inputParams );
					logger.info( "Offset Pos: {}", outputResult.getLastMappingPos( queryNode.getId() ) );
				}
				*/

				logger.trace( "Mappings are: {}", mappings );
				if( mappings.size() > 0 ) { //|| (relNode.lookupMappings( queryNode.getId() ) != null ) ) {
					relNode.addMappings( queryNode.getId(), mappings, outputResult.getLastMappingPos( queryNode.getId() ) );
					logger.trace( "Added mappings {} to node {}.", mappings, relNode.getId().getId() );
					if( mappings.size() > 0 ) {
						parameterMatchingFound = true;
					} else {
						logger.warn( "Previous mappings no longer hold! {} -> {}: {}", relNode.getId(), queryNode.getId(), mappings );
						logger.warn( "RS {} and input params {}", outputResult, inputParams );
					}
				}
			}
		}

		// If no queries gave us our parameters clearly their relation to us was spurious
		if( !parameterMatchingFound ) {
			for( QueryIdentifier relQuery : relatedQueries ) {
				MarkovNode relNode = nodeLookupTable.get( relQuery );
				if( relNode.hasNoMappings( queryNode.getId() ) ) {
					// Already blacklisted, we can skip
					continue;
				}
				relNode.confirmNoMappings( queryNode.getId() );
				logger.trace( "Q{} blacklisted Q{}", relQuery, queryNode.getId() );
			}
		}
	}

	/**
	 * Find parameter mappings between one queries output set and another's input set
	 */
	private static Multimap<Integer, String> findMappingsBetweenOutputInputSets( QueryResult firstQueryResult, List<String> secondQueryInputs, QueryIdentifier dstQueryId ) {

		if( !firstQueryResult.isSelect() ) {
			Multimap<Integer, String> pairings = ArrayListMultimap.create();
			return pairings;
		}
		List<Map<String,Object>> firstQueryOutputs = firstQueryResult.getSelectResult();
		if( secondQueryInputs != null && firstQueryOutputs.size() != 0 ) {
			
			// Check the first row of results and, if we've checked this mapping earlier,
			// more than the row we checked last time, maybe this is a loop
			int rowWindowSize = 10;
			List<Integer> rowsToCheck = new LinkedList<Integer>();

			for( int i = 1; i < rowWindowSize && firstQueryResult.getLastMappingPos( dstQueryId ) + i < firstQueryOutputs.size(); ++i ) {
				rowsToCheck.add( firstQueryResult.getLastMappingPos( dstQueryId ) + i );
			}

			// Prefer the offset (in loop) to 0.
			rowsToCheck.add( 0 );
			logger.debug( "Got firstQueryOutputs: {}", firstQueryOutputs );

			logger.trace( "Looking for mappings in rows: {}", rowsToCheck );
			
			for( int j : rowsToCheck ) {
				Multimap<Integer, String> pairings = ArrayListMultimap.create();
				// This is the inner checking loop where we look for mappings
				for( int i = 0; i < secondQueryInputs.size(); i++ ) {
					logger.trace( "Looking for entry {}, got {}", j, firstQueryOutputs.get( j ) );
					for( Entry<String, Object> entry : firstQueryOutputs.get( j ).entrySet() ) {
						String params = secondQueryInputs.get( i );
						Object value = entry.getValue();

						if( value != null && ( params.equals( value.toString() )
									// TODO we call Object.toString() here which is
									// actually the object of ResultSet
									|| params.equals( "'" + value.toString() + "'" ) ) ) {
							pairings.put( i, entry.getKey() );
							// Inform the result where someone last mapped from
						} // if value != null
					} // for entry
				} // for i
				if( !pairings.isEmpty() ) {
					firstQueryResult.setMappedPos( dstQueryId, j );
					return pairings;
				}
			} // for j
		} // if qryparams != null

		Multimap<Integer, String> pairings = ArrayListMultimap.create();
		return pairings;

	}

	/**
	 * Determine all prior query mappings for the given query id
	 */
	public Map<QueryIdentifier, QueryMappingEntry> getPriorQueryMappingEntries( QueryIdentifier qid ) {
		MarkovNode node = nodeLookupTable.get( qid );
		if( node == null ) {
			logger.error( "Couldn't find MarkovNode for qid {}", qid );
			return new HashMap<>();
		}
		return node.getPriorQueryMappingEntries();
	}

	/**
	 * Determine all prior query mappings for the given query id
	 */
	public Multimap<QueryIdentifier, MappingEntry> getPriorMappingEntries( QueryIdentifier qid ) {
		MarkovNode node = nodeLookupTable.get( qid );
		if( node == null ) {
			logger.error( "Couldn't find MarkovNode for qid {}", qid );
			return ArrayListMultimap.create();
		}
		return node.getPriorMappingEntries();
	}

	/**
	 * Determine all prior query mappings for the given query id
	 */
	public Multimap<Integer, MappingEntry> getPriorMappingEntriesByPosition( QueryIdentifier qid ) {
		MarkovNode node = nodeLookupTable.get( qid );
		if( node == null ) {
			logger.error( "Couldn't find MarkovNode for qid {}", qid );
			return ArrayListMultimap.create();
		}
		return node.getPriorMappingEntriesByPosition();
	}

	/**
	 * Determine all forward query mappings for the given query id
	 */
	public Map<QueryIdentifier, Multimap<Integer, String>> getForwardQueryMappings( QueryIdentifier qid ) {
		MarkovNode node = nodeLookupTable.get( qid );
		if( node == null ) {
			logger.error( "Couldn't find MarkovNode for qid {}", qid );
			return new HashMap<>();
		}
		return node.getForwardQueryMappings();
	}

	// Maintain the graph's connections and nodes' inputs/outputs
	public void maintainTransitionsAndMappings( ExecutedQuery qry, List<ExecutedQuery> slice )
	{
		synchronized( this ) {
			Iterator<ExecutedQuery> it = slice.iterator();
			logger.trace("QID {} had {} other queries within interval", qry.getId().getId(), slice.size());
			//Add this node to the graph if it doens't exist
			MarkovNode node = getOrAddNode( qry.getId() );

			//Increase number of times we have seen this node in the first position
			node.increaseHitCounter();
			increaseHitCounter();

			logger.trace("Adding edge traversals...");
			Map<QueryIdentifier, Integer> seenQueryIds = new HashMap<>();
			//Add edges and increment number of times we have walked down an edge
			while( it.hasNext() ){
				ExecutedQuery qryToLink = it.next();
				MarkovNode nodeToLinkTo = getOrAddNode( qryToLink.getId() );
				if( !seenQueryIds.containsKey( qryToLink.getId() ) ){
					node.addEdgeTraversal( nodeToLinkTo );
					logger.trace("Added edge count from {} to {}", node.getId().getId(), nodeToLinkTo.getId().getId());
					seenQueryIds.put( qryToLink.getId(), 1 );
				} else {
					int numTimesSeen = seenQueryIds.get( qryToLink.getId() ) + 1;
					seenQueryIds.put( qryToLink.getId(), numTimesSeen );
					logger.trace( "Have already seen queryId {} {} times in this chunk, considering decreasing time slice size", qryToLink.getId().getId(), numTimesSeen );
				}
			}

			// After constructing more of the Markov Graph check
			// for any parameter mappings the executed query may have
			addInputParameters( qry.getId(), qry.getParams() );
			addResultSet( qry.getId(), qry.getResults() );
			logger.trace( "Finding all parameter mappings for node: {}", qry.getId() );
			findAllParameterMappingsForNode( qry.getId() );
			//graph.blacklistSpuriousMappingsForNode( qry.getId() );
			// For blacklisting, we should get the set of all related queries
			// then walk backwards along the stream to find the first instance of one of these
			// if we see ourselves then immediately break
			// if we don't then check that the most recent query which supposedly implies our execution
			// does in fact have valid parameter mappings to us.
		}
	}

	// Helper function for findSCC
	public void stronglyConnected( QueryIdentifier givenVertex,
		int i,
		Stack<QueryIdentifier> points,
		Map<QueryIdentifier, Integer> lowlink,
		Map<QueryIdentifier, Integer> number,
		Set<QueryIdentifier> pointsOnStack,
		List<Set<QueryIdentifier>> components )
	{
		// Number the given vertex with the next integer
		// and setup initial value of lowlink
		i += 1;
		lowlink.put( givenVertex, i );
		number.put( givenVertex, i );

		// Add this vertex to the stack
		pointsOnStack.add( givenVertex );
		points.push( givenVertex );

		logger.trace( "[stronglyConnected] i = {} stack = {}", i, points );

		// Go through the adjacency list of the given vertex
		for( QueryIdentifier adjacentVertex : getRelatedOutQueries( givenVertex ) ) {
			// If the adjacent vertex hasn't been numbered yet we will do this process first before
			// updating updating lowlink value of the given vertex
			if( !number.containsKey( adjacentVertex ) ) {
				// Recurse
				stronglyConnected( adjacentVertex, i, points, lowlink, number, pointsOnStack, components );
				// We have to increment i since we can't pass by reference in Java
				// normally this could be done by the recursion but oh well
				i += 1;
				// Update lowlink value if the edge is a spanning subtree di-edge
				lowlink.put( givenVertex, Integer.min( lowlink.get( givenVertex ), lowlink.get( adjacentVertex ) ) );
			} else if ( number.get( adjacentVertex ) < number.get( givenVertex ) && pointsOnStack.contains( adjacentVertex ) ) {
				// Update lowlink value if the edge is a frond or crosslink
				lowlink.put( givenVertex, Integer.min( lowlink.get( givenVertex ), lowlink.get( adjacentVertex ) ) );
			}
		}

		// The given vertex was the root of an SCC
		if( lowlink.get( givenVertex ) == number.get( givenVertex ) ) {
			// Make a new component
			Set<QueryIdentifier> newComponent = new HashSet<>();

			// Fill the new component
			while( !points.empty() && number.get( points.peek() ) >= number.get( givenVertex ) ) {
				// Delete top of stack and put it in the new component
				pointsOnStack.remove( points.peek() );
				newComponent.add( points.pop() );
			}

			components.add( newComponent );
		}
	}

	// Strongly connected components algorithm from the paper
	// Depth-First Search and Linear Graph Algorithms by Robert Trajan
	public Set<QueryIdentifier> findSCC( QueryIdentifier startingPoint ) {
		int i = 0;
		Stack<QueryIdentifier> points = new Stack<>();
		Map<QueryIdentifier, Integer> lowlink = new HashMap<>();
		Map<QueryIdentifier, Integer> number = new HashMap<>();
		Set<QueryIdentifier> pointsOnStack = new HashSet<>();
		List<Set<QueryIdentifier>> components = new LinkedList<>();

		// Run the recursive scc finding algorithm on the weakly connected component at startingPoint
		stronglyConnected( startingPoint, i, points, lowlink, number, pointsOnStack, components );

		// Return the component to which starting point belongs
		for( Set<QueryIdentifier> component : components ) {
			if( component.contains( startingPoint ) ) {
				logger.trace( "Containing component is: {}", component );
				return component;
			}
		}

		// Should never happen, should always have at least one component of startingPoint iteslf
		return new HashSet<>();
	}

	/**
	 * Given a set of prior query mappings, filter out all empty mappings to other
	 * queries
	 */
	public Map<QueryIdentifier, Multimap<Integer, String>> filterMappings( Map<QueryIdentifier, Multimap<Integer, String>> mappings ) {
		Iterator<Map.Entry<QueryIdentifier,Multimap<Integer,String>>> iter = mappings.entrySet().iterator();
		while( iter.hasNext() ) {
			Map.Entry<QueryIdentifier, Multimap<Integer,String>> entry = iter.next();
			QueryIdentifier qid = entry.getKey();
			if( entry.getValue().keySet().size() == 0 ) {
				iter.remove();
			}
		}
		return mappings;
	}

	/**
	 * Given a set of query mappings segregated by QueryIdentifier, merge them together
	 * based on the param number they influence
	 */
	public Multimap<Integer, String> mergeResultSetMappings( Map<QueryIdentifier, QueryMappingEntry> priorQueryMappings ) {
		Multimap<Integer, String> mergedMap = HashMultimap.create();

		for( QueryMappingEntry qme : priorQueryMappings.values() ) {
			mergedMap.putAll( qme.getQueryMappings() );
		}

		return mergedMap;
	}

	/*
	 * This method takes two iterable collections of mapping points expressed as arrays and produces
	 * a set of points (whose dimension = dimension of points in firstSet + dimension of points in secondSet)
	 * where {p1, p2} X {p3, p4} = {p1p3, p1p4, p2p3, p2p4}, aka the concatenation of all possible
	 * pairs. The concatenation retains the relative order of the sets e.g. there can be no p3p1.
	 */
	public List<MappingEntry[]> crossProduct( Collection<MappingEntry[]> firstSet, Collection<MappingEntry[]> secondSet ) {
		List<MappingEntry[]> xResult = new LinkedList<>();

		// Handle edge cases where either firstSet or secondSet is empty. In such a case we want to
		// return the other set. Since X = concatenation we will concat nothing to each point in the
		// non-empty set and thus we should return the original. If both are empty then returning
		// the empty set is correct
		if( firstSet.isEmpty() ) {
			xResult.addAll( secondSet );
			return xResult;
		} else if( secondSet.isEmpty() ) {
			xResult.addAll( firstSet );
			return xResult;
		}

		// Do the cross producting, if either firstSet or secondSet is empty this will do nothing
		for ( MappingEntry[] firstPoint : firstSet ) {
			for ( MappingEntry[] secondPoint : secondSet ) {
				MappingEntry[] point = new MappingEntry[ firstPoint.length + secondPoint.length ];
				// Set first 'half' of array to be firstPoint
				System.arraycopy( firstPoint, 0, point, 0, firstPoint.length );
				// Set second 'half' of array to be secondPoint
				System.arraycopy( secondPoint, 0, point, firstPoint.length, secondPoint.length );
				// Add the resulting array to our set
				xResult.add( point );
			}
		}

		return xResult;
	}

	/*
	 * This method transforms a multimap of mapping entries into a multimap of single-elemnt-array
	 * mapping entries for use in crossProduct
	 */
	public Multimap<Integer, MappingEntry[]> generateSingletonValues( Multimap<Integer, MappingEntry> priorMappingEntries ) {
		Multimap<Integer, MappingEntry[]> singletonValues = ArrayListMultimap.create();

		for( Map.Entry<Integer, MappingEntry> entry : priorMappingEntries.entries() ) {
			// Create our 1-D point
			MappingEntry[] newOption = { entry.getValue() };
			singletonValues.put( entry.getKey(), newOption );
		}

		return singletonValues;
	}

	/*
	 * This method creates all possible dependency graphs from the Markov Graph. Careful this is
	 * combinatorial in nature.
	 */
	private List<DependencyGraph> constructFDQDependencyGraphs(
		QueryIdentifier qid,
		DependencyGraph basisDependencyGraph,
		Stack<QueryIdentifier> contextStack,
		Map<QueryIdentifier, Query> knownQueryShells
	) throws IllegalArgumentException {
		// Log the node we are at
		logger.trace( "Reached node {}", qid.getId() );

		// If our qid already exists on the stack then throw an illegal argument exception, the
		// graph handed to us is not a DAG. Otherwise add us to the context stack.
		if( contextStack.search( qid ) > 0 ) {
			logger.error( "Found cycle with {} : {}", qid, contextStack );
			throw new IllegalArgumentException( "Not a DAG!" );
		} else {
			contextStack.push( qid );
		}

		// Generate all valid (non-conflicting i.e. two queries don't map to same constant position)
		// configurations of prior query mappings
		// Begin by converting mapping entries into single-element arrays
		Multimap<Integer, MappingEntry[]> singletonValues = generateSingletonValues( getPriorMappingEntriesByPosition( qid ) );
		// Next expand these single-element arrays into full size-of-mapped-parameters
		// configurations by taking the cross product of the sets of mappings for each position
		Collection<MappingEntry[]> validMappingConfigurations = new HashSet<>();
		for( Integer key : singletonValues.keySet() ) {
			validMappingConfigurations = crossProduct( validMappingConfigurations, singletonValues.get( key ) );
		}

		// Log the validMappingConfigurations
		logger.trace( "Valid Configurations = {" );
		for( MappingEntry[] mappingEntryArray : validMappingConfigurations ) {
			logger.trace( "    {} size = {}", mappingEntryArray, mappingEntryArray.length );
		}
		logger.trace( "}" );

		// Copy our basis dependency graph for each of these valid configurations and add one
		// configuration to each dependency graph
		Map<MappingEntry[], DependencyGraph> graphsWithValidMappingConfigurations = new HashMap<>();
		for( MappingEntry[] validMappingConfiguration : validMappingConfigurations ) {
			// Deep copy the basis dependency graph
			DependencyGraph graphForConfiguration = new DependencyGraph( basisDependencyGraph );

			// Add the chosen mapping configuration to the dependency graph
			for( MappingEntry me : validMappingConfiguration ) {
				graphForConfiguration.addDependencyForQuery( me.getHeadQuery(), me, knownQueryShells.get( me.getHeadQuery() ) );
			}

			// Add this configuration, graph pair to our map. We want to keep the configuration
			// around because it defines along what branches to recurse to fill in the rest of the
			// graph
			graphsWithValidMappingConfigurations.put( validMappingConfiguration, graphForConfiguration );
		}

		// Log the number of <configuration, graph> pairs
		logger.trace( "Have {} <configuration, graph> pairs.", graphsWithValidMappingConfigurations.keySet().size() );

		// Recursive step. We will work on one <configuration, graph> pair at a time. The result
		// will be a list of complete dependency graphs which we will add to our final return
		List<DependencyGraph> completeGraphs = new LinkedList<>();
		for( Map.Entry<MappingEntry[], DependencyGraph> entry : graphsWithValidMappingConfigurations.entrySet() ) {
			// Graphs which we have to send up the tree still
			List<DependencyGraph> incompleteGraphs = new LinkedList<>();
			incompleteGraphs.add( entry.getValue() );
			// Graphs which came back to us complete from further up the tree
			List<DependencyGraph> receivedGraphs = new LinkedList<>();

			// Recurse along each of the branches in the dependency tree as defined by the current
			// configuration i.e. choice of mappings
			for( MappingEntry me : entry.getKey() ) {
				// Send each not-yet-complete graph along the current configuration branch
				for( DependencyGraph incompleteGraph : incompleteGraphs ) {
					receivedGraphs.addAll( constructFDQDependencyGraphs( me.getTailQuery(), incompleteGraph, contextStack, knownQueryShells ) );
				}

				// Shift the lists of graphs
				incompleteGraphs = receivedGraphs;
				receivedGraphs = new LinkedList<>();
			}

			completeGraphs.addAll( incompleteGraphs );
		}

		// If completeGraphs contains nothing it means we are a terminal query so duplicate the
		// basisDependencyGraph once
		if( completeGraphs.size() == 0 ) {
			// Deep copy the basis dependency graph
			DependencyGraph graphForConfiguration = new DependencyGraph( basisDependencyGraph );
			completeGraphs.add( graphForConfiguration );
		}

		// Log the number of complete graphs
		logger.trace( "Generated {} completeGraphs.", completeGraphs.size() );

		// 1. |Parameters| > |Mappings| => Base query, not an FDQ, not an ADQ
		// 2. |Parameters| = |Mappings| => Not a base query, is an FDQ, maybe an ADQ
		// 3. |Parameters| = |Mappings| = 0 => Base query, is an FDQ, is an ADQ
		Query query = knownQueryShells.get( qid );
		int parametersSize = query.getParams().size();
		int mappingsSize = mergeResultSetMappings( getPriorQueryMappingEntries( qid ) ).keySet().size();

		// Log the parameter and mapping size
		logger.trace( "Node {} has |Parameters| = {} and |Mappings| = {}", qid.getId(), parametersSize, mappingsSize );

		// Mark base queries in our finished graphs, use add here just in case something was missed
		if( parametersSize == 0 ) {
			for( DependencyGraph completeGraph : completeGraphs ) {
				// Mark as base query
				completeGraph.addBaseQuery( qid, query );
			}
		} else if ( parametersSize != mappingsSize ) {
			for( DependencyGraph completeGraph : completeGraphs ) {
				// Mark as base query
				completeGraph.addBaseQuery( qid, query );
				// Downgrade to FDQ, this is not an ADQ
				completeGraph.markAsFDQ();
			}
		}

		// Remove us from the context stack
		contextStack.pop();

		return completeGraphs;
	}

	public List<Vectorizable> constructNewFDQs( QueryIdentifier qid, String queryString, Map<QueryIdentifier, Query> knownQueryShells ) {
		List<DependencyGraph> fdqDependencyGraphs;
		List<Vectorizable> vectorizables = new LinkedList<>();

		synchronized( this ) {
			// If there are not enough mappings to make qid an FDQ then return an empty list, we
			// didn't find any dependency graphs
			Query query = knownQueryShells.get( qid );
			int parametersSize = query.getParams().size();
			Multimap<Integer, String> mappings = mergeResultSetMappings( getPriorQueryMappingEntries( qid ) );
			int mappingsSize = mappings.keySet().size();
			logger.trace( "Found mappings for {}: {}", qid, mappings );

			if( parametersSize > mappingsSize ) {
				logger.trace( "{} has insufficient mappings to be an FDQ. There are {} parameters and only {} mappings.", qid, parametersSize, mappingsSize );
				return vectorizables;
			}

			// Start with an empty dependency graph
			DependencyGraph emptyGraph = new DependencyGraph();
			// Create all possible dependency graphs out of what we have starting with nothing
			fdqDependencyGraphs = constructFDQDependencyGraphs( qid, emptyGraph, new Stack<>(), knownQueryShells );
		}

		// Make the vectorizables to be returned
		for( DependencyGraph graph : fdqDependencyGraphs ) {
			VectorizableType vectorizableType = new VectorizableType();
			vectorizableType.markAsFDQ();
			if ( graph.isADQ() ) {
				vectorizableType.markAsADQ();
			}
			logger.info( "Vectorizable: {} is FDQ.", graph );
			vectorizables.add( new Vectorizable( graph, qid, queryString, vectorizableType ) );
		}
		
		// Return the new FDQs
		return vectorizables;
	}

	/**
	 * For now, support only loops that are one level deep.
	 * This could be easily extended to be recursive to support more levels
	 */
	private boolean transitivelyDependsOnTrigger(
		QueryIdentifier triggerQueryId,
		QueryIdentifier queryId
	) {
		if( queryId == triggerQueryId ) {
			return true;
		}
		Map<QueryIdentifier, QueryMappingEntry> mappingsToThisQuery = getPriorQueryMappingEntries( queryId );
		if( mappingsToThisQuery.containsKey( triggerQueryId ) ) {
			return true;
		}
		return false;
	}


	private boolean constructLoopDependencyGraph(
		QueryIdentifier triggerQueryId,
		Set<QueryIdentifier> candidateTriggers,
		DependencyGraph dependencyGraph,
		Set<QueryIdentifier> loopQueryIds,
		Map<QueryIdentifier, Query> knownQueryShells
	) {
		Map<QueryIdentifier, Multimap<Integer, String>> mappingsFromTriggerToBody = filterMappings( getForwardQueryMappings( triggerQueryId ) );

		Query triggerQuery = knownQueryShells.get( triggerQueryId );
		List<String> triggerQueryParams = triggerQuery.getParams();

		//We assume that a trigger query is THE starting point for the loop, but the trigger
		//could have its params provided by another query. In theory the vectorizer can take
		//care of this (and should) but we simplify for now. FIXME
		if( !triggerQueryParams.isEmpty() ) {
			// This trigger query's text has stale params, so we will need to replace the text
			// before we execute it
			dependencyGraph.addBaseQuery( triggerQueryId, triggerQuery );
		} else {
			//Text is OK, since it has no params
			dependencyGraph.addBaseQuery( triggerQueryId, triggerQuery );
		}

		Set<QueryIdentifier> reducedLoopQueryIds = new HashSet<QueryIdentifier>( loopQueryIds );
		reducedLoopQueryIds.remove( triggerQueryId );

		for( QueryIdentifier bodyQueryId : reducedLoopQueryIds ) {
			//Should have at least one mapping (to our trigger query)
			Map<QueryIdentifier, QueryMappingEntry> mappingsToBodyQuery = getPriorQueryMappingEntries( bodyQueryId );
			Query bodyQuery = knownQueryShells.get( bodyQueryId );
			if( bodyQuery == null ) {
				return false;
			}

			if( !mappingsToBodyQuery.containsKey( triggerQueryId ) ) {
				return false;
			}

			// Add all dependencies to the graphs and track which positions have been mapped
			Set<Integer> mappedPositions = new HashSet<>();
			for( QueryIdentifier dependencyQueryId : mappingsToBodyQuery.keySet() ) {
				// Do not take mappings from potential other trigger queries, make sure all of these queries depend on the trigger.
				if( dependencyQueryId == triggerQueryId || ( !candidateTriggers.contains( dependencyQueryId ) && transitivelyDependsOnTrigger( triggerQueryId, dependencyQueryId ) && dependencyQueryId != bodyQueryId ) ) {
					dependencyGraph.addDependencyForQuery( bodyQueryId, mappingsToBodyQuery.get( dependencyQueryId ) );
					mappedPositions.addAll( mappingsToBodyQuery.get( dependencyQueryId ).getQueryMappings().keySet() );
				}
			}

			// If the body query's mappings are not sufficient then mark the body query as a base
			// query. That is, we will need to wait to see a new query string before the whole dep.
			// graph can be vectorized and executed
			if( mappedPositions.size() != bodyQuery.getParams().size() ) {
				dependencyGraph.markQueryAsBaseQuery( bodyQuery.getId(), bodyQuery.getQueryString() );
			}
		}

		return dependencyGraph.isVectorizable();
	}

    public List<Vectorizable> constructNewLoops( QueryIdentifier qid, Map<QueryIdentifier, Query> knownQueryShells ) {
		List<Vectorizable> vectorizables = new LinkedList<>();
		Map<QueryIdentifier, DependencyGraph> loopDependencyGraphs = new HashMap<>();

		synchronized( this ) {
			logger.trace( "Looking for loops containing: {}", qid );
			// Find the component to which qid belongs, guarenteed to exist
			Set<QueryIdentifier> candidateComponent = findSCC( qid );
			logger.trace( "Candidate component for {} is {}", qid, candidateComponent );
			Set<QueryIdentifier> candidateTriggers = getPriorQueryMappingEntries( qid ).keySet();

			// Find the incoming mappings for each query in the component. Intersect them with
			// candidateTriggers to iteratively narrow down what the loop triggers can be
			// TODO: This chunk of code assumes that the trigger query provides at least one
			// mapping, but that is not necessarily the case. Need to crawl the mappings like in
			// findNewFDQs to properly see which queries we need text for.
			logger.trace( "Looking for Candidate triggers: {}", candidateTriggers );
			for( QueryIdentifier nodeId : candidateComponent ) {
				Set<QueryIdentifier> nodeCandidateTriggers = getPriorQueryMappingEntries( nodeId ).keySet();
				logger.trace( "{} map to node {}", nodeCandidateTriggers, nodeId );
				candidateTriggers.retainAll( nodeCandidateTriggers );
			}
			logger.trace( "Candidate triggers: {}", candidateTriggers );

			// Construct loops for all possible triggers
			for ( QueryIdentifier candidateTrigger : candidateTriggers ) {
				assert candidateTrigger != null;
				DependencyGraph loopDependencyGraph = new DependencyGraph();
				loopDependencyGraph.markAsLoop();
				// Construct a new loop if possible and if it's okay add it to our map
				if( constructLoopDependencyGraph( candidateTrigger, candidateTriggers, loopDependencyGraph, candidateComponent, knownQueryShells ) ) {
					logger.trace( "Found loop dependency graph: {}", loopDependencyGraph );
					loopDependencyGraphs.put( candidateTrigger, loopDependencyGraph );
				} else {
					logger.trace( "Could not build loop dependency graph: {}", loopDependencyGraph );
				}
			}
		}

		logger.trace( "Remaining Dependency Graphs: {}", loopDependencyGraphs );

		// Make all the vectorizables to be returned
		for( Map.Entry<QueryIdentifier, DependencyGraph> entry : loopDependencyGraphs.entrySet() ) {
			String triggerString = knownQueryShells.get( entry.getKey() ).getQueryString();
			VectorizableType vectorizableType = new VectorizableType();
			vectorizableType.markAsLoopBaseQuery();
			logger.trace( "{} is marked as a LoopBaseQuery", entry.getValue() );
			vectorizables.add( new Vectorizable( entry.getValue(), entry.getKey(), triggerString, vectorizableType ) );
		}

		// Return the new Loop
		return vectorizables;
    }

	/*
	 * Creates a brand new undirected graph which results from transforming & pruning the Markov graph
	 */
	public UndirectedGraph transformGraph( double pruneThreshold ) {
		UndirectedGraph prunedGraph = new UndirectedGraph();
		for( QueryIdentifier queryId : nodeLookupTable.keySet() ) {
			prunedGraph.add( queryId, nodeLookupTable.get( queryId ).transformNode( pruneThreshold ) );
		}

		return prunedGraph;
	}

	/*
	 * Utility function for debugging
	 */
	public void printGraph() {
		logger.trace( "BEGIN PRINT MARKOV GRAPH" );
		for( QueryIdentifier qid : nodeLookupTable.keySet() ) {
			logger.trace( "Q{} = {}", qid.getId(), nodeLookupTable.get( qid ).getQueryString() );
		}
		// For each node in graph
		for( QueryIdentifier qid : nodeLookupTable.keySet() ) {
			// For each node it relates to
			logger.trace( "Q{} -> [", qid.getId() );
			for( QueryIdentifier relatedQid : getRelatedOutQueries( qid ) ) {
				logger.trace( "    Q{} ({})", relatedQid.getId(), nodeLookupTable.get( qid ).computeEdgeTraversalProbability( nodeLookupTable.get( relatedQid ) ) );
			}
			logger.trace( "]" );
		}
		logger.trace( "END PRINT MARKOV GRAPH" );
	}

	/*
	 * Utility function for debugging
	 */
	public void printParameterMappings() {
		logger.debug( "BEGIN PRINT PARAMETERS" );
		for( MarkovNode node : nodeLookupTable.values() ) {
			logger.debug( "Q{} -> [", node.getId().getId() );
			for( MarkovNode otherQuery : nodeLookupTable.values() ) {
				Multimap<Integer, String> mappings = node.lookupMappings( otherQuery.getId() );
				if (mappings != null) {
					logger.debug( "    Q{} {},", otherQuery.getId().getId(), mappings );
				}
			}
			logger.debug( "]" );
		}
		logger.debug( "END PRINT PARAMETERS" );
	}


	private double computeGraphDiffScore( MarkovGraph otherGraph ) {
		// Must hold sync locks
		double diff_score = 0.0;

		// Consider all nodes in this graph
		for( QueryIdentifier thisGraphQid : nodeLookupTable.keySet() ) {
			logger.trace( "Considering {}", thisGraphQid );
			MarkovNode thisGraphNode = nodeLookupTable.get( thisGraphQid );
			MarkovNode otherGraphNode = otherGraph.nodeLookupTable.get( thisGraphQid );

			// If they don't have this node, then we differ on all connections
			if( otherGraphNode == null ) {
				logger.trace( "Other graph does not have: {}", thisGraphQid );
				Map<QueryIdentifier, MarkovNode> thisGraphConnectedNodes = thisGraphNode.getConnectedNodes();
				for( MarkovNode connectedNode : thisGraphConnectedNodes.values() ) {
					double edgeTraversalProb = thisGraphNode.computeEdgeTraversalProbability( connectedNode );
					diff_score += Math.pow( edgeTraversalProb, 2 );
				}
				// If they do have this node, then consider each connection individually
			} else {
				logger.trace( "Other graph has: {}", thisGraphQid );
				Map<QueryIdentifier, MarkovNode> thisGraphConnectedNodes = thisGraphNode.getConnectedNodes();
				Map<QueryIdentifier, MarkovNode> otherGraphConnectedNodes = otherGraphNode.getConnectedNodes();
				for( QueryIdentifier thisGraphConnectedQid : thisGraphConnectedNodes.keySet() ) {
					logger.trace( "Considering connection: {} -> {}", thisGraphQid, thisGraphConnectedQid );
					MarkovNode thisGraphConnectedNode = thisGraphConnectedNodes.get( thisGraphConnectedQid );
					MarkovNode otherGraphConnectedNode = otherGraphConnectedNodes.get( thisGraphConnectedQid );
					// If they don't have this connection
					if( otherGraphConnectedNode == null ) {
						logger.trace( "Other graph does not have this connection." );
						double edgeTraversalProb = thisGraphNode.computeEdgeTraversalProbability( thisGraphConnectedNode );
						diff_score += Math.pow( edgeTraversalProb, 2 );
						// If they do, compute the difference
					} else {
						logger.trace( "Other graph does have this connection!" );
						double thisEdgeTraversalProb = thisGraphNode.computeEdgeTraversalProbability( thisGraphConnectedNode );
						double otherEdgeTraversalProb = otherGraphNode.computeEdgeTraversalProbability( otherGraphConnectedNode );
						logger.trace( "Computed probs: {} vs {}", thisEdgeTraversalProb, otherEdgeTraversalProb );
						diff_score += Math.pow( thisEdgeTraversalProb - otherEdgeTraversalProb, 2 );
					}
				}

				// Consider any edges the other graph has that this one does not
				for( QueryIdentifier otherGraphConnectedQid : otherGraphConnectedNodes.keySet() ) {
					MarkovNode thisGraphConnectedNode = thisGraphConnectedNodes.get( otherGraphConnectedQid );
					if( thisGraphConnectedNode != null ) {
						continue;
					}
					MarkovNode otherGraphConnectedNode = otherGraphConnectedNodes.get( otherGraphConnectedQid );
					double otherEdgeTraversalProb = otherGraphNode.computeEdgeTraversalProbability( otherGraphConnectedNode );
					diff_score += Math.pow( otherEdgeTraversalProb, 2 );
				}
			} // else
		} // all nodes in this graph

		// Now consider any nodes the other graph has that this one doesn't
		for( QueryIdentifier otherGraphQid : otherGraph.nodeLookupTable.keySet() ) {
			MarkovNode thisGraphNode = nodeLookupTable.get( otherGraphQid );
			if( thisGraphNode != null ) {
				continue;
			}
			MarkovNode otherGraphNode = otherGraph.nodeLookupTable.get( otherGraphQid );
			Map<QueryIdentifier, MarkovNode> otherGraphConnectedNodes = otherGraphNode.getConnectedNodes();
			for( MarkovNode otherGraphConnectedNode : otherGraphConnectedNodes.values() ) {
				double otherEdgeTraversalProb = otherGraphNode.computeEdgeTraversalProbability( otherGraphConnectedNode );
				diff_score += Math.pow( otherEdgeTraversalProb, 2 );
			}
		} // Other nodes end
		return diff_score;
	}

	public double compareToOtherMarkovGraph( MarkovGraph otherGraph ) {
		assert this.clientId != otherGraph.clientId;


		if( clientId < otherGraph.clientId ) {
			synchronized( this ) {
				synchronized( otherGraph ) {
					return computeGraphDiffScore( otherGraph );
				}
			}
		} else {
			synchronized( otherGraph ) {
				synchronized( this ) {
					return computeGraphDiffScore( otherGraph );
				}
			}
		}
	}
}
