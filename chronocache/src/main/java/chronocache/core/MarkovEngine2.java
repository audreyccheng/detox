package chronocache.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import org.joda.time.DateTime;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.HashMultimap;

import chronocache.core.future.OngoingSpeculativeExecutionTable;
import chronocache.core.future.ResultRegistration;
import chronocache.core.future.ResultBox;

import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryOutputs;
import chronocache.core.qry.QueryResult;
import chronocache.core.qry.QueryStream;
import chronocache.core.parser.ParserPool;
import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.AntlrQueryMetadata;
import chronocache.core.parser.AntlrQueryConstructor;

import chronocache.db.DB;
import chronocache.db.DBException;
import chronocache.db.DBFactory;

/**
 * A rewritten version of the Markov Engine
 * Provides the core predictive execution logic - based on
 * query streams and the MarkovGraph's parameter mappings,
 * decides when to predictively execute queries.
 */
public class MarkovEngine2 implements Engine {
	protected Map<Long, MarkovMultiModel> clientModels;
	protected Multimap<Long, QueryStream> queryStreams;
	protected Map<Long, VectorizableDependencyTable> clientVectorizableDependencyTables;
	protected Map<QueryIdentifier, Query> knownQueryShells;
	protected Map<QueryIdentifier, Vectorizable> alwaysDefinedQueries;
	protected SimpleQueryVectorizer simpleQueryVectorizer;
	protected LateralUnionVectorizer luQueryVectorizer;

	//The lock for queryTimesSeen and avgQueryResponseTimes
	protected Map<QueryIdentifier, Long> queryTimesSeen;
	protected Map<QueryIdentifier, Double> avgQueryResponseTimes;

	private Logger logger;
	protected OngoingSpeculativeExecutionTable currentlyExecutingQueries;
	protected ExecutorService asyncExecutor;
	protected DBFactory.DBType dbType;
	protected Thread clientVectorizableShareThread;

	public MarkovEngine2( DBFactory.DBType dbType ) {
		clientModels = new ConcurrentHashMap<>();
		queryStreams = HashMultimap.create();
		clientVectorizableDependencyTables = new ConcurrentHashMap<>();
		knownQueryShells = new ConcurrentHashMap<>();
		queryTimesSeen = new ConcurrentHashMap<>();
		avgQueryResponseTimes = new ConcurrentHashMap<>();
		alwaysDefinedQueries = new ConcurrentHashMap<>();
		logger = LoggerFactory.getLogger( this.getClass() );
		currentlyExecutingQueries = new OngoingSpeculativeExecutionTable();
		asyncExecutor = Executors.newFixedThreadPool( 1000 );
		simpleQueryVectorizer = new SimpleQueryVectorizer();
		this.dbType = dbType;
		luQueryVectorizer = new LateralUnionVectorizer();
		clientVectorizableShareThread = new Thread( new AsyncVectorizableShareWorker( this ) );
		clientVectorizableShareThread.start();
	}

	private class MarkovEngineWorker implements Runnable {
		private ExecutedQuery query;
		private long clientId;

		public MarkovEngineWorker( long clientId, ExecutedQuery query ) {
			this.clientId = clientId;
			this.query = query;
		}

		@Override
		public void run() {
			logger.trace( "Engine Worker processing qid {} = {}", query.getId().getId(), query.getQueryString() );
			long engineProcessStart = System.nanoTime()/1000;

			addAverageTrackingInfo( query );
			if( !query.isReadQuery() ) {
				//No need to proceed, Update queries don't provide any new dependencies, and ADQs
				//will have been backgrounded earlier
				return;
			}

			// Don't want to look every time because this can be expensive. We use exponential backoff.
			VectorizableDependencyTable dependencyTable = clientVectorizableDependencyTables.get( clientId );
			boolean shouldLookForVectorizables = dependencyTable.shouldCheckForVectorizables( query.getId() );
			if( shouldLookForVectorizables ) {

				//Find if it exists, the dependency graph that makes this query an FDQ
				long start = System.nanoTime() / 1000;
				findNewFDQs( clientId, query.getId(), query.getQueryString() );
				long end = System.nanoTime() / 1000;
				logger.trace("Time to find new FDQs: {}", end - start );

				// Find, if it exists, a new loop with the ExecutedQuery as its trigger
				if( !Parameters.SCALPEL_MODE ) {
					start = System.nanoTime() / 1000;
					findNewLoops( clientId, query.getId() );
					end = System.nanoTime() / 1000;
				}
				logger.debug("Time to find new Loops: {}", end - start );
			} else {
				logger.debug( "Not looking for FDQs for QID: {}, in backoff...", query.getId().getId() );
			}

			long engineProcessEnd = System.nanoTime()/1000;
			logger.debug( "Engine took {} to process query.", (engineProcessEnd - engineProcessStart) );
		}
	};


	/**
	 * An engine worker used to reload important queries
	 */
	private class EngineReloadWorker implements Runnable {
		private long clientId;

		public EngineReloadWorker( long clientId ) {
			this.clientId = clientId;
		}

		/**
		 * Reload "important" ADQs back into cache as defined by the cost function
		 */
		@Override
		public void run() {
			logger.trace( "Reload worker is running for clientId {}", clientId );
			Collection<Vectorizable> importantQueries;
			importantQueries = getImportantQueries( clientId );
			logger.debug("Going to async reload {} ADQs...", importantQueries.size() );
			asyncAlwaysDefinedQueryReload( clientId, importantQueries );
		}
	};

	/**
	 * An engine worker used to async call executeFullyDefinedQuery
	 */
	private class AsyncVectorizableExecWorker implements Runnable {
		private long clientId;
		private Vectorizable vectorizable;
		private boolean shouldUpdateClientVersion;

		public AsyncVectorizableExecWorker( long clientId, Vectorizable vectorizable, boolean shouldUpdateClientVersion ) {
			this.clientId = clientId;
			this.vectorizable = vectorizable;
			this.shouldUpdateClientVersion = shouldUpdateClientVersion;
		}

		@Override
		public void run() {
			executeVectorizable( clientId, vectorizable, shouldUpdateClientVersion );
		}
	};

	private class AsyncVectorizableShareWorker implements Runnable {
		private MarkovEngine2 engine;
		private AtomicBoolean stop;

		public AsyncVectorizableShareWorker( MarkovEngine2 engine ) {
			this.engine = engine;
			this.stop = new AtomicBoolean(false);
		}

		@Override
		public void run() {
			while( !stop.get() ) {
				Map<Long, VectorizableDependencyTable> clientVectorizableTables = engine.clientVectorizableDependencyTables;
				Map<Vectorizable.VectorizableId, Vectorizable> vectorizables = new HashMap<>();
				for( VectorizableDependencyTable vecTable : clientVectorizableTables.values() ) {
					Map<Vectorizable.VectorizableId, Vectorizable> clientVectorizables = vecTable.getVectorizableMap();
					for( Map.Entry<Vectorizable.VectorizableId, Vectorizable> entry : clientVectorizables.entrySet() ) {
						if( !vectorizables.containsKey( entry.getKey() ) ) {
							vectorizables.put( entry.getKey(), entry.getValue() );
						}
					}
				}

				logger.info( "Loading vectorizables into client graphs." );
				for( Map.Entry<Long, VectorizableDependencyTable> vecTableEntry : clientVectorizableTables.entrySet() ) {
					logger.info( "Loading into clientId {}'s vec table.", vecTableEntry.getKey() );
					vecTableEntry.getValue().loadInNewVectorizablesFrom( vectorizables );
				}
				logger.info( "Done! Sleeping for {}", Parameters.CLIENT_SHARE_SLEEP_TIME );
				try {
					Thread.sleep( Parameters.CLIENT_SHARE_SLEEP_TIME.getMillis() );
				} catch( InterruptedException e ) {
					logger.warn( "Could not sleep! Got exception: {}", e );
				}
			}
		}

		public void stop() {
			this.stop.set( true );
		}
	};

	public DBFactory.DBType getDBType( ) {
		return dbType;
	}

	/**
	 * Compute an intersection of two sets, returning true if not null
	 * O( n log n ) unless hashsets, in which case O(n) of set1
	 */
	private <T> boolean hasIntersection( Set<T> set1, Set<T> set2 ) {
		for( T t : set1 ) {
			if( set2.contains( t ) ) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Look at probabilistically related queries of qid and determine if it could be an FDQ
	 */
	protected void findNewFDQs( long clientId, QueryIdentifier qid, String queryString ) {
		// Get the dependency table and markov graph for this client
		VectorizableDependencyTable dependencyTable = clientVectorizableDependencyTables.get( clientId );
		MarkovGraph clientMarkovGraph = getClientMarkovPredictionGraph( clientId );
		logger.trace( "Retrieved client {}'s loop table and prediction graph successfully.", clientId );

		try {
			List<Vectorizable> vectorizables = clientMarkovGraph.constructNewFDQs( qid, queryString, knownQueryShells );

			logger.info( "Found Vectorizables for QID: {} -> {}", qid, vectorizables );

			for( Vectorizable vectorized : vectorizables ) {
				if( vectorized.isVectorizable() ) {
					dependencyTable.addVectorizable( vectorized );
					if( vectorized.isAlwaysDefined() ) {
						alwaysDefinedQueries.put( qid, vectorized );
					}
				} else {
					logger.info( "Vectorized {} is not vectorizable.", vectorized.getId() );
				}
			}
		} catch( IllegalArgumentException e ) {
			//Not a DAG
		}
	}

	/*
	 * Find new loops via detecting strongly connected components
	*/
	protected void findNewLoops( long clientId, QueryIdentifier qid ) {
		// Get the client's loops and markov graph
		VectorizableDependencyTable dependencyTable = clientVectorizableDependencyTables.get( clientId );
		MarkovGraph clientMarkovGraph = getClientMarkovPredictionGraph( clientId );
		logger.trace( "Retrieved client {}'s loop table and prediction graph successfully.", clientId );

		// Query is part of an OK loop so create its vectorizable and add it to the table
		List<Vectorizable> vectorizables = clientMarkovGraph.constructNewLoops( qid, knownQueryShells );
		for( Vectorizable vectorized : vectorizables ) {
			if( vectorized.isVectorizable() ) {
				dependencyTable.addVectorizable( vectorized );
				logger.trace( "New Loop {} found and added to the dependency table.", vectorized.getQueryId() );
			}
		}
	}

	protected int getSlotNumber( QueryIdentifier queryId, List<QueryIdentifier> queryIds ) {
		int i = 0;
		for( QueryIdentifier curQueryId : queryIds ) {
			if( curQueryId == queryId ) {
				return i;
			}
			i++;
		}
		return -1;
	}

	/** 
	 * Wrapper to make this easy to test.
	 */
	protected QueryResult checkMemcached( CacheHandler cacheHandler, String cacheKey, VersionVector versionVector ) {
		return cacheHandler.checkCache( cacheKey, versionVector );
	}

	protected boolean isAlreadyCached( long clientId, Vectorizable thingToExecute, QueryVectorizationPlan plan ) {
		DependencyGraph dependencyGraph = thingToExecute.getVectorizationDependencies();
		logger.trace( "Getting topologically ordered queries for graph: {}", dependencyGraph );
		List<QueryIdentifier> queryIds = dependencyGraph.getTopologicallyOrderedQueries();
		if( queryIds.isEmpty() ) {
			return true;
		}

		// Create array of empty result sets
		ArrayList<List<QueryResult>> splitResults = new ArrayList<>();
		for( QueryIdentifier queryId : queryIds ) {
			splitResults.add( new LinkedList<QueryResult>() );
		}

		Multimap<Integer, QueryIdentifier> depthToQueryMap = dependencyGraph.getTopologicalDepthToQueryIdMap();
		logger.trace( "Constructed depth to query map: {}", depthToQueryMap );
		int maxDepth = dependencyGraph.getMaxHeight();
		logger.trace( "We have max depth: {}", maxDepth );

		AntlrParser parser = new AntlrParser();
		CacheHandler cacheHandler = getCacheHandler();

		for( int level = 0; level <= maxDepth; level++ ) {
			Collection<QueryIdentifier> queryIdsAtThisLevel = depthToQueryMap.get( level );
			logger.trace( "We found {} at depth {}", queryIdsAtThisLevel, level );

			for( QueryIdentifier queryId : queryIdsAtThisLevel ) {
				// If this a base query with no mappings, then we can just run the text
				if( dependencyGraph.isBaseQuery( queryId ) && dependencyGraph.getQueryDependencies( queryId ).isEmpty() ) {
					String baseQueryText = dependencyGraph.getBaseQueryText( queryId );
					String cacheKey = String.valueOf( baseQueryText.hashCode() );

					VersionVector clientVersion = cacheHandler.getClientVersion( clientId );
					logger.trace( "Checking memcached for {}, key: {}, version: {}", baseQueryText, cacheKey, clientVersion );
					QueryResult result = checkMemcached( cacheHandler, cacheKey, clientVersion );
					if( result == null ) {
						logger.info( "Got null result for {}, not already cached.", baseQueryText );
						return false;
					}
					// Else put the result in the right slot.
					logger.trace( "Got result, going to next level." );
					int slot = getSlotNumber( queryId, queryIds );
					splitResults.get( slot ).add( result );
				} else {
					Collection<QueryMappingEntry> mappings = dependencyGraph.getQueryDependencies( queryId );
					List<List<QueryResult>> relevantResults = new LinkedList<>();
					List<Multimap<Integer,String>> relevantMappings = new LinkedList<>();
					List<Integer> relevantResultLevels = new LinkedList<>();
					List<QueryIdentifier> orderedQueryIds = plan.getOrderedQueries();
					
					int numRows = plan.getRelevantResultsAndMappings( mappings, orderedQueryIds, splitResults, relevantResults, relevantMappings, relevantResultLevels );

					Query curQueryShell = dependencyGraph.getQueryShellFromGraph( queryId );

					long constructAllQueryStart = System.nanoTime()/1000;
					for( int queryNumber = 0; queryNumber < numRows; queryNumber++ ) {
						logger.trace( "Going to construct query: {}", queryNumber );
						long singleQueryConstructStart = System.nanoTime()/1000;
						String queryText = dependencyGraph.getBaseQueryText( queryId );
						Query q = simpleQueryVectorizer.rewriteSingleQuery( queryNumber, curQueryShell, queryText, parser, relevantResults, relevantMappings, relevantResultLevels );
						long singleQueryConstructEnd = System.nanoTime()/1000;
						logger.trace( "Constructed single query in {}", singleQueryConstructEnd-singleQueryConstructStart );
						logger.trace( "Constructed: {}", q.getQueryString() );
						String cacheKey = q.getCacheKey();

						logger.trace( "Checking memcached for key: {}", cacheKey );
						QueryResult qResult = checkMemcached( cacheHandler, cacheKey, cacheHandler.getClientVersion( clientId ) );
						logger.trace( "Found: {}", qResult );
						if( qResult == null ) {
							logger.info( "Could not find inner query in memcached!" );
							return false;
						}
						int slot = getSlotNumber( queryId, queryIds );
						splitResults.get( slot ).add( qResult );
					}
					long constructAllQueryEnd= System.nanoTime()/1000;
					logger.debug( "Time to construct all queries: {}", constructAllQueryEnd-constructAllQueryStart );
				}
			}
		}
		logger.info( "Found in memcached! Returning true." );

		return true;

	}

	protected void executeVectorizable( long clientId, Vectorizable thingToExecute, boolean shouldUpdateClientVersion )
	{
		QueryVectorizerResult vectorizerResult;
		DBFactory dbFact = new DBFactory( this );

		logger.trace( "Client {} trying to execute vectorizable: {}", clientId, thingToExecute );
		if( !Parameters.SCALPEL_MODE && thingToExecute.isSimpleVectorizable() ) {
			long simpleVecStart = System.nanoTime() / 1000;
			logger.trace( "Executing vectorizable using simpleQueryVectorizer" );
			vectorizerResult = simpleQueryVectorizer.vectorize( clientId, thingToExecute, dbFact.getDBInstance( dbType ) );
			long simpleVecEnd = System.nanoTime() / 1000;
			logger.debug( "Time to get vectorized text: \"{}\": {}", vectorizerResult.getVectorizedQueryText(), simpleVecEnd - simpleVecStart );
		} else {
			long luVecStart = System.nanoTime() / 1000;
			logger.trace( "Executing vectorizable using luQueryVectorizer" );
			vectorizerResult = luQueryVectorizer.vectorize( clientId, thingToExecute, dbFact.getDBInstance( dbType ) );
			long luVecEnd = System.nanoTime() / 1000;
			logger.debug( "Time to get vectorized text: \"{}\": {}", vectorizerResult.getVectorizedQueryText(), luVecEnd - luVecStart );
		}
		logger.info( "Generated vectorized queryText: {}", vectorizerResult.getVectorizedQueryText() );

		long cacheCheckStart = System.nanoTime() / 1000;
		boolean isCached = isAlreadyCached( clientId, thingToExecute, vectorizerResult.getQueryVectorizationPlan() );
		long cacheCheckEnd = System.nanoTime() / 1000;
		logger.debug( "Cache check took: {}", cacheCheckEnd - cacheCheckStart );
		if( isCached ) { 
			logger.debug( "Result is already cached. Not executing {}", thingToExecute.getQueryId() );
			return;
		}
		logger.trace( "Done checking cache!" );


		//After executing, split and cache the results
		long execStart = System.nanoTime() / 1000;
		QueryResult combinedResult = executeCombinedQuery( clientId, vectorizerResult, shouldUpdateClientVersion );
		long execEnd = System.nanoTime() / 1000;
		logger.trace( "executeCombined Time: {}", execEnd - execStart );
		QueryVectorizationPlan plan = vectorizerResult.getQueryVectorizationPlan();

		long splitStart = System.nanoTime() / 1000;
		ArrayList<List<QueryResult>> splitResults;
		ArrayList<List<String>> cacheKeys;
		if( !Parameters.SCALPEL_MODE && thingToExecute.isSimpleVectorizable() ) {
			splitResults = simpleQueryVectorizer.splitApartResultSet( combinedResult, plan );
			cacheKeys = simpleQueryVectorizer.getCacheKeysForResults( splitResults, plan );
		} else {
			splitResults = luQueryVectorizer.splitApartResultSet( combinedResult, plan );
			cacheKeys = luQueryVectorizer.getCacheKeysForResults( splitResults, plan );
		}
		long splitEnd = System.nanoTime() / 1000;
		logger.trace( "Time to split: {}", splitEnd - splitStart );

		for( int i = 0; i < splitResults.size(); i++ ) {
			for( int j = 0; j < splitResults.get( i ).size(); j++ ) {
				QueryResult result = splitResults.get( i ).get( j );
				try {
					String cacheKey = cacheKeys.get( i ).get( j );
					logger.debug( "Got cacheKey: {} for splitResults[{}][{}]", cacheKey, i, j );
					if( cacheKey != null ) {
						logger.debug( "Caching {} using cacheKey: {}", result.getSelectResult(), cacheKey );
						getCacheHandler().cacheResult( cacheKey, null, result );
					} else {
						logger.debug( "Found null cache key, not caching result: {}", result.getSelectResult() );
					}
				} catch( IndexOutOfBoundsException e ) {
					logger.warn( "Can't get cacheKeys[{}][{}]", i, j );
					logger.warn( "Combined query: {}", vectorizerResult.getVectorizedQueryText() );
					for( int i2 = 0; i2 < splitResults.size(); i2++ ) {
						for( int j2 = 0; j2 < splitResults.get( i2 ).size(); j2++ ) {
							logger.warn( "splitResults[{}][{}] = {}", i2, j2, splitResults.get(i2).get(j2) );
						}
					}
					for( int i2 = 0; i2 < cacheKeys.size(); i2++ ) {
						for( int j2 = 0; j2 < cacheKeys.get( i2 ).size(); j2++ ) {
							logger.warn( "cacheKeys[{}][{}] = {}", i2, j2, cacheKeys.get(i2).get(j2) );
						}
					}
				}
			}
		}
	}

	private QueryResult executeCombinedQuery(
		long clientId,
		QueryVectorizerResult vectorizerResult,
		boolean shouldUpdateClientVersion
	) {
		String combinedQueryString = vectorizerResult.getVectorizedQueryText();
		QueryVectorizationPlan plan = vectorizerResult.getQueryVectorizationPlan();

		DBFactory dbFact = new DBFactory( this );
		CacheHandler cache = getCacheHandler();
		VersionVector predictiveVersion = cache.getClientVersion( clientId );
		AntlrParser parser = new AntlrParser();

		QueryResult result = null;
		try {
			logger.trace( "Executing DB Q: \"{}\"", combinedQueryString );
			result = dbFact.getDBInstance( dbType ).querySpeculatively( clientId, combinedQueryString, vectorizerResult.getTables(), true );
		} catch( DBException e ) {
			logger.error( "Received error while executing: {}, {}", combinedQueryString, e.getMessage() );
		}
		return result;
	}

	/**
	 * Execute all of the provided ADQ's against the database
	 */
	private void asyncAlwaysDefinedQueryReload( long clientId, Collection<Vectorizable> importantQueries ) {
		logger.debug("Told to execute adq's {}", importantQueries );
		for( Vectorizable fdq : importantQueries ) {
			AsyncVectorizableExecWorker vectorizableWorker = new AsyncVectorizableExecWorker( clientId, fdq, false );
			asyncExecutor.execute( vectorizableWorker );
		}
	}

	/**
	 * Spawn a worker used to reload important queries
	 */
	public void reloadImportantQueries( long clientId ){
		if( Parameters.ENABLE_COMPUTED_RELOAD ) {
			EngineReloadWorker worker = new EngineReloadWorker( clientId );
			asyncExecutor.execute( worker );
		}
	}

	/**
	 * Find the ADQs that we would ideally want to reload according to the
	 * cost function
	 */
	protected Collection<Vectorizable> getImportantQueries( long clientId ) {
		MarkovGraph graph = getClientMarkovPredictionGraph( clientId );
		if( graph == null ) {
			logger.error( "Could not retrieve important queries - could not find markov graph for {}", clientId );
			throw new NullPointerException();
		}

		Collection<Vectorizable> adqs = alwaysDefinedQueries.values();
		logger.trace( "System knows about {} adqs...", adqs.size() );
		Set<Vectorizable> importantAdqs = new HashSet<>();
		for( Vectorizable adq : adqs ) {
			QueryIdentifier qid = adq.getId();
			double prob = graph.computeQueryProbability( qid );

			logger.trace( "QID {} has prob {}", qid.getId(), prob );
			double cost = avgQueryResponseTimes.get( qid ) * prob;
			logger.debug( "Computed Reload score for {}: {} = {} x {}", qid.getId(), cost, prob, avgQueryResponseTimes.get( qid ) );
			if( cost > Parameters.COMPUTED_RELOAD_THRESHOLD ) {
				importantAdqs.add( adq );
			}
		}

		return importantAdqs;
	}

	/**
	 * Return the cache handler that the DBImpl is using
	 */
	protected CacheHandler getCacheHandler() {
		DB db = new DBFactory( this ).getDBInstance( dbType );
		return db.getCacheHandler();
	}

	protected List<List<String>> getAllConstantPermutations( List<Set<String>> constants ) {
		logger.trace("Getting constant permutations for {}, size: {}", constants, constants.size() );
		//If there are no constants left, then there are no permutations
		if( constants.size() == 0 ) {
			return new LinkedList<List<String>>();
		}

		//This function is supposed to return a set of all possible constant permutations
		//for an upcoming query, with the input provided like:
		//( [ 'test' ], [ 'a', 'b' ], [ 3, 5 ] )
		//i.e. a List of List of constants, where the inner lists give options for
		//a constant choice at that position in the query.
		//Therefore, we want to return:
		// [ 'test', 'a', 3 ], [ 'test', 'a', 5 ], [ 'test', 'b', 3 ], [ 'test', 'b', 5 ]
		//
		//As we consider a new constant position, we want to add each of the new constant
		//options to each of the already known combinations of constants.

		//Initialize permutation set using first constant
		List<List<String>> constantPermutations = new LinkedList<>();
		Iterator<Set<String>> constantIterator = constants.iterator();
		Set<String> firstConstOpts = constantIterator.next();
		for( String constant : firstConstOpts ) {
			List<String> constList = new LinkedList<String>();
			constList.add( constant );
			constantPermutations.add( constList );
		}
		logger.trace( "After first constant proc {}", constantPermutations );

		while( constantIterator.hasNext() ) {
			Set<String> constantEntry = constantIterator.next();
			logger.trace( "Processing {}, currently {}", constantEntry, constantPermutations );
			//If there is only one constant in this set (i.e. only one option), then
			//just add it to the existing options
			if( constantEntry.size() == 1 ) {
				for( List<String> permutation : constantPermutations ) {
					permutation.add( constantEntry.iterator().next() );
				}
			} else {
				//For each new constant option, add it to each of the existing permutations
				List<List<String>> newConstantPermutations = new LinkedList<>();
				for( String constant : constantEntry ) {
					for( List<String> permutation : constantPermutations ) {
						List<String> permutationCopy = new LinkedList<>( permutation );
						permutationCopy.add( constant );
						newConstantPermutations.add( permutationCopy );
					}
				}
				constantPermutations = newConstantPermutations;
			}
		}

		return constantPermutations;
	}

	/**
	 * Get a list of the constants we should use as replacements in the query shell
	 */
	protected List<String> tryGetRequiredData( long clientId, Multimap<Integer,QueryOutputs> mappings ) {
		int numMappings = mappings.keySet().size();
		List<String> constants = new LinkedList<>();
		for( int ind = 0; ind < numMappings; ind++ ) {
			Collection<QueryOutputs> mappingsForInd = mappings.get( ind );
			if( mappingsForInd.size() == 0 ) {
				logger.error( "Trying to find required data for mappings but mappings for index {} are missing!", ind );
				throw new NullPointerException();
			}
			String constant = getDataForIndex( clientId, mappingsForInd );
			if( constant == null ) {
				throw new NullPointerException();
			}
			constants.add( constant );
		}
		return constants;
	}

	/**
	 * Retrieve the constant we should use for a parameter, given the indexes mappings
	 * and the clientId trying to perform predictive execution
	 */
	protected String getDataForIndex( long clientId, Collection<QueryOutputs> mappings ) {
		MarkovGraph clientGraph = getClientMarkovPredictionGraph( clientId );
		if( clientGraph == null ) {
			logger.error( "Couldn't find data for mappings because clientId {}'s markov graph was not found", clientId );
			throw new NullPointerException();
		}
		if( mappings.size() == 0 ) {
			logger.error( "Couldn't retrieve data for index, no data sources found!");
			throw new NullPointerException();
		}
		//Just use the first source - other mappings may be removed later on but for
		//now are considered redundant sources
		QueryOutputs source = mappings.iterator().next();
		QueryIdentifier sourceQuery = source.getQueryId();
		MarkovNode queryNode = clientGraph.getOrAddNode( sourceQuery );
		QueryResult queryResultSet = queryNode.getPreviousResultSet();
		List<Map<String,Object>> resultMap = queryResultSet.getSelectResult();
		if( source.getColumnNames().size() == 0 ) {
			logger.error( "Couldn't retrieve data from qid {}'s resultset, mapping exists but there are no column mappings", sourceQuery.getId() );
			throw new NullPointerException();
		}
		String columnName = source.getColumnNames().iterator().next();
		if( resultMap.size() == 0 ) {
			logger.error( "Couldn't retrieve data from qid {}'s resultset, no rows in resultset", sourceQuery.getId() );
			throw new NullPointerException();
		}
		Object dataItem = resultMap.get(0).get( columnName );
		if( dataItem == null ) {
			logger.error( "Couldn't retrieve data from qid {}'s resulset, no data found for column name {}", sourceQuery.getId(), columnName );
			throw new NullPointerException();
		}
		return dataItem.toString();
	}

	/**
	 * Return a set of query identifiers that have mappings to this query
	 */
	protected Map<QueryIdentifier, QueryMappingEntry> getAllPriorQueryMappings( long clientId, QueryIdentifier qid ) {
		MarkovGraph clientGraph = getClientMarkovPredictionGraph( clientId );
		if( clientGraph == null ) {
			logger.error( "Couldn't lookup client {}'s markov graph!", clientId );
			throw new NullPointerException();
		}
		return clientGraph.getPriorQueryMappingEntries( qid );
	}

	/**
	 * Return a set of query identifiers that have mappings from this query
	 */
	protected Map<QueryIdentifier, Multimap<Integer, String>> getAllForwardQueryMappings( long clientId, QueryIdentifier qid ) {
		MarkovGraph clientGraph = getClientMarkovPredictionGraph( clientId );
		if( clientGraph == null ) {
			logger.error( "Couldn't lookup client {}'s markov graph!", clientId );
			throw new NullPointerException();
		}
		return clientGraph.getForwardQueryMappings( qid );
	}

	/**
	 * Add average response time tracking information for the given query
	 * Used as part of the cost function reload threshold
	 */
	protected void addAverageTrackingInfo( ExecutedQuery query ) {
		logger.trace( "Updating tracking information for qid {}", query.getId().getId() );
		Long timesSeen;
		logger.debug( "Raw exec time for {} is {}", query.getQueryString(), query.getResponseTime() );
		if( (timesSeen = queryTimesSeen.get( query.getId() )) == null ) {
			double averageRunTime = query.getResponseTime();
			queryTimesSeen.put( query.getId(), 1L );
			avgQueryResponseTimes.put( query.getId(), averageRunTime );
		} else {
			double currentAverage = avgQueryResponseTimes.get( query.getId() );
			double newAverage = ((timesSeen * currentAverage) + query.getResponseTime()) / (timesSeen + 1);
			queryTimesSeen.put( query.getId(), timesSeen+1 );
			avgQueryResponseTimes.put( query.getId(), newAverage );
			logger.trace( "New average resp time for {} is {}", query.getId().getId(), newAverage );
		} // if
	}

	/**
	 * Store a copy of the query shell so we can recall it later
	 */
	protected void recordQueryShell( Query query ) {
		if( !knownQueryShells.containsKey( query.getId() ) ) {
			logger.info( "QID {} = \"{}\"", query.getId().getId(), query.getQueryString() );
			knownQueryShells.put( query.getId(), query );
		} else {
			//TODO: confirm no hash collision
			knownQueryShells.replace( query.getId(), query );
		}
	}

	/**
	 * Get a client's Markov models
	 * Shouldn't ever return null in practice, but you should handle it anyways
	 */
	public MarkovMultiModel getClientMarkovMultiModel( long clientId ) {
		MarkovMultiModel model = clientModels.get( clientId );
		if( model == null ) {
			logger.error( "Couldn't retrieve clientId {}'s markov models.", clientId );
			throw new NullPointerException();
		}
		return model;
	}

	 /* Get a client's "Prediction Markov graph"
	 * The client has multiple graphs, but the lower graphs are used to determine
	 * if queries would invalidate our results
	 * Shouldn't ever return null in practice, but you should handle it anyways
	 */
	public MarkovGraph getClientMarkovPredictionGraph( long clientId ) {
		MarkovMultiModel model = clientModels.get( clientId );
		if( model == null ) {
			logger.error( "Couldn't retrieve clientId {}'s markov models.", clientId );
			throw new NullPointerException();
		}
		return model.getPredictGraph();
	}

	/**
	 * Get all query identifiers that are considered related according to threshold
	 */
	protected List<QueryIdentifier> lookupRelatedOutQueries( long clientId, QueryIdentifier qid ) {
		MarkovGraph clientMarkovGraph = getClientMarkovPredictionGraph( clientId );
		if( clientMarkovGraph == null ) {
			logger.error( "Couldn't lookup markov graph for clientId {}!", clientId );
			throw new NullPointerException();
		}

		return clientMarkovGraph.getRelatedOutQueries( qid );
	}

	/**
	 * Method asynchronously executes any ready vectorizables we are not part of and synchronously
	 * executes any ready vectorizables we are part of
	 */
	private boolean splitSynchExecuteVectorizables( long clientId, Query query ) {
		VectorizableDependencyTable clientVectorizableTable = clientVectorizableDependencyTables.get( clientId );

		// Get all the ready vectorizables while filtering them into one set which contains our
		// query id and one set that does not
		Set<Vectorizable> containingVectorizables = new HashSet<>();
		Set<Vectorizable> otherVectorizables = new HashSet<>();
		clientVectorizableTable.getAndClearAllReadyVectorizablesFiltered( query.getId(), containingVectorizables, otherVectorizables );

		logger.info( "ClientId {} has {} containing vectorizables and {} other vectorizables for queryId: {}", clientId, containingVectorizables.size(), otherVectorizables.size(), query.getId() );
		logger.info( "Contanining vectorizables:" );
		for( Vectorizable vec: containingVectorizables ) {
			vec.getVectorizationDependencies().dumpToLog();
		}

		logger.info( "Other vectorizables:" );
		for( Vectorizable vec: otherVectorizables ) {
			vec.getVectorizationDependencies().dumpToLog();
		}

		long asyncStart = System.nanoTime()/1000;
		// Execute any other ready vectorizable asynchronously
		for( Vectorizable vectorizableToExecute : otherVectorizables ) {
			// Background results are not immediately visible to the client and thus do
			// not update the session
			AsyncVectorizableExecWorker vectorizableWorker = new AsyncVectorizableExecWorker( clientId, vectorizableToExecute, false );
			asyncExecutor.execute( vectorizableWorker );
			logger.debug( "Async Vectorizable Engine Worker processing vectorizable {}", vectorizableToExecute.getQueryId() );
		}
		long asyncEnd = System.nanoTime()/1000;
		logger.debug( "Time to launch async workers: {}", asyncEnd-asyncStart );

		// Execute any ready vectorizable we are part of asynchronously but wait for them all to complete
		long critStart = System.nanoTime()/1000;
		Set<Callable<Object>> criticalPathWorkers = new HashSet<>();
		for( Vectorizable vectorizableToExecute : containingVectorizables ) {
			// These results are immediately visible to the client, and thus do update its session
			criticalPathWorkers.add( Executors.callable( new AsyncVectorizableExecWorker( clientId, vectorizableToExecute, true ) ) );
			logger.debug( "Engine added criticalPathWorker for {}", vectorizableToExecute.getQueryId() );
		}
		try {
			logger.debug( "Engine starting all criticalPathWorkers." );
			asyncExecutor.invokeAll( criticalPathWorkers );
		} catch( InterruptedException e ) {
			logger.debug("Interruped while executing ready vectorizables that contained {}", query.getId().getId() );
		}
		long critEnd = System.nanoTime()/1000;
		logger.debug( "Time to execute all critical path workers: {}", critEnd - critStart );
		return !containingVectorizables.isEmpty();
	}

	public boolean preprocessQuery( long clientId, Query query ) {
		boolean ret = false;
		if( Parameters.ENABLE_SPECULATIVE_EXECUTION ) {
			logger.info( "Engine preprocessing qid {} for clientId {}", query.getId().getId(), clientId );
			long enginePreprocessStart = System.nanoTime()/1000;

			recordQueryShell( query );
			if( !query.isReadQuery() ) {
				//No need to proceed, Update queries don't provide any new dependencies, and ADQs
				//will have been backgrounded earlier
				return false;
			}

			// Get all ready Loops or FDQs
			VectorizableDependencyTable clientVectorizableTable = clientVectorizableDependencyTables.get( clientId );
			if( clientVectorizableTable != null ) {
				clientVectorizableTable.markExecutedDependency( query );

				// Execute the ready loops and fdqs, some of them synchronously, some of the asynchronously
				ret = splitSynchExecuteVectorizables( clientId, query );

			}
			long enginePreprocessEnd = System.nanoTime()/1000;
			logger.trace( "Engine took {} to process query.", (enginePreprocessEnd - enginePreprocessStart) );
		}
		return ret;
	}

	public void postprocessQuery( long clientId, ExecutedQuery q ) {
		if( Parameters.ENABLE_SPECULATIVE_EXECUTION ) {
			logger.debug( "Engine processing qid {} for clientId {}", q.getId().getId(), clientId );
			MarkovEngineWorker worker = new MarkovEngineWorker( clientId, q );
			asyncExecutor.execute( worker );
		}
	}

	public void addToQueryStream( long clientId, ExecutedQuery q, String queryString ) {
		if( Parameters.ENABLE_SPECULATIVE_EXECUTION ) {
			Collection<QueryStream> qStreams = queryStreams.get( clientId );
			if( qStreams.isEmpty() ) {
				registerNewClient( clientId );
				qStreams = queryStreams.get( clientId );
			}
			logger.debug("Adding {} to client {}'s {} queryStreams", q.getId().getId(), clientId, qStreams.size());

			for( QueryStream qStream : qStreams ) {
				qStream.addQueryToStream( q );
			}

			MarkovMultiModel multiModel = getClientMarkovMultiModel( clientId );
			if( multiModel == null ) {
				logger.error( "Couldn't find client {}'s Markov models, not adding resultSet" );
				return;
			}
			multiModel.pushQueryToAllModels( q, queryString );
			logger.trace("Adding {} to result set for {} for client {}", q.getResults().getSelectResult(), q.getId().getId(), clientId);
		}
	}

	/**
	 * Register the current clientId, creating threads to monitor their querystreams
	 */
	protected void registerNewClient( long clientId ) {
		if( queryStreams.containsKey( clientId ) ) {
			logger.warn( "ClientId {} already has querystream, not adding another...", clientId );
			return;
		}
		logger.info( "Registering new querystream for clientId {}", clientId );
		MarkovMultiModel multiModel = new MarkovMultiModel( clientId, Parameters.MIN_WIDTH, Parameters.WIDTH_FACTOR, Parameters.MAX_WIDTH_EXP );
		List<QueryStream> modelStreams = multiModel.getQueryStreams();
		queryStreams.putAll( clientId, modelStreams );
		clientModels.put( clientId, multiModel );
		VectorizableDependencyTable vecTab = new VectorizableDependencyTable();
		clientVectorizableDependencyTables.put( clientId, vecTab );
	}

	public void stopEngine() {
		logger.trace("MarkovEngine stopEngine called.");
		// Stop each model
		for( MarkovMultiModel model : clientModels.values() ) {
			model.stop();
		}
		clientVectorizableShareThread.stop();
		clientVectorizableShareThread.interrupt();
	}

	public boolean registerOrBlockIfQueryCurrentlyExecuting( long clientId, String queryString, VersionVector version,
			ResultRegistration<QueryResult> reg ) {
		return currentlyExecutingQueries.registerOrBlockIfQueryCurrentlyExecuting( clientId, queryString, version, reg );
	}

	public void doneExecutingQuery( long clientId, String queryString, VersionVector version, QueryResult result ) {
		currentlyExecutingQueries.doneExecutingQuery( clientId, queryString, version, result );
	}

}
