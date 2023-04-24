package chronocache.db;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.joda.time.DateTime;
import org.mariadb.jdbc.MariaDbPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import chronocache.core.InMemoryCacheHandler;
import chronocache.core.CacheHandler;
import chronocache.core.Engine;
import chronocache.core.MarkovEngine2;
import chronocache.core.FidoEngine;
import chronocache.core.VersionVector;
import chronocache.core.VersionVectorFactory;
import chronocache.core.future.ResultBox;
import chronocache.core.future.ResultRegistration;
import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.core.Parameters;
import chronocache.core.fido.Trainer;
import chronocache.util.Configuration;
import chronocache.util.ResultSetConverter;

/**
 *
 * @author anilpacaci
 *
 *         Singleton DB Instance, Interface of Kronos
 */
public class DBImpl implements DB {
	private Logger logger = LoggerFactory.getLogger( DB.class );
	private final VersionVector DB_EXEC_VERSION;

	private static DB instance = new DBImpl();
	private DataSource source = null;
	private DBConnPool dbPool;
	private Engine engine;
	private int speculativeExecutionCounter = 0;
	private ConcurrentHashMap<String, Integer> keyCounts;
	private ConcurrentHashMap<String, Object> speculativeCacheMap;
	private ConcurrentHashMap<Long, Connection> connMap;
	private CacheHandler cacheHandler;

	private AtomicInteger cacheHitCounter;
	private AtomicInteger cacheMissCounter;

	protected ExecutorService asyncExecutor;

	private VersionVectorFactory versionVectorFactory;


	public static DB getInstance() {
		return instance;
	}

	public CacheHandler getCacheHandler() {
		return cacheHandler;
	}

	private DBImpl() {
		logger.info( "==================== ENGINE PARAMETERS ====================" );
		logger.info( "ENGINE_TYPE = {}", Configuration.getEngineType() );
		Field[] fields = Parameters.class.getDeclaredFields();
		for( Field f : fields ) {
			if( Modifier.isStatic( f.getModifiers() ) ) {
				try {
					logger.info( "{} = {}", f.getName(), f.get( null ) );
				} catch( IllegalAccessException e ) {
				}
			}
		}
		logger.info( "===========================================================" );

		if( Configuration.getDatabaseType().equals( "postgresql" ) ) {
			source = getPostgresqlDataSource();
			logger.info( "PostgreSQL: connected" );
		}
		if( Configuration.getDatabaseType().equals( "mysql" ) ) {
			source = getMySQLDataSource();
			logger.info( "MySQL: connected" );
		}
		if( Configuration.getDatabaseType().equals( "mariadb" ) ) {
			source = getMariaDBDataSource();
			logger.info( "MariaDB: connected" );
		}
		logger.info( "Creating conn pool" );
		dbPool = new DBConnPool( source, Parameters.REMOTE_DB_CONNS );
		logger.info( "Conn pool created" );

		if( Configuration.getEngineType().equals( "fido" ) ) {
			Trainer trainer = new Trainer();
			engine = new FidoEngine( trainer.train() );
		} else {
			engine = new MarkovEngine2( DBFactory.DBType.REAL_DB );
		}
		logger.info( "Engine started..." );
		speculativeCacheMap = new ConcurrentHashMap<String, Object>();
		keyCounts = new ConcurrentHashMap<>();
		connMap = new ConcurrentHashMap<>();

		// Initialize cache
		// logger.info( "Trying to connect to memcached..." );
		// Initialize memcached client
		// MemcachedClient memcachedClient = MemcachedClient.getInstance();
		// logger.info( "Memcached: connected" );
		logger.info("Trying to connect to redis...");
		RedisClient redisClient = new RedisClient();
		logger.info( "Redis: connected" );

		// Initialize Cassandra
		logger.info( "Starting distributed cache handler.." );
		cacheHandler = new InMemoryCacheHandler(redisClient);
		// cacheHandler = new InMemoryCacheHandler(memcachedClient);
		
		versionVectorFactory = new VersionVectorFactory( Parameters.WORKLOAD_TYPE );
		DB_EXEC_VERSION = versionVectorFactory.createDBVersionVector();

		asyncExecutor = Executors.newFixedThreadPool( 50 );

		// Cache hit-miss counters
		cacheHitCounter = new AtomicInteger( 0 );
		cacheMissCounter = new AtomicInteger( 0 );
	}

	public void resetCacheStats() {
		cacheHitCounter.set(0);
		cacheMissCounter.set(0);
	}

	public int getCacheHits() {
		return cacheHitCounter.get();
	}

	public int getCacheMiss() {
		return cacheMissCounter.get();
	}

	/**
	 * Assume this a REST provided query and we do not need extra metadata
	 */
	public QueryResult query( long clientId, String queryString ) throws DBException {
		return query( clientId, queryString, false );
	}

	/**
	 * Assume this a REST provided query with some explicit requirement on metadata
	 */
	public QueryResult query( long clientId, String queryString, boolean shouldGetNumColumns ) throws DBException {
		logger.debug( "Going to query \"{}\" (Metadata? {}) for clientId: {}", queryString, shouldGetNumColumns, clientId );

		AntlrParser parser = new AntlrParser();
		assert parser != null;

		try {
			return query( clientId, queryString, parser, shouldGetNumColumns );
		} catch( DBException e ) {
			throw new DBException( e );
		}
	}

	/**
	 * Assume this a REST provided query and that we are using the client's current timestamp
	 */
	public QueryResult query( long clientId, String queryString, AntlrParser parser, boolean shouldGetNumColumns ) throws DBException {
		return query( clientId, queryString, cacheHandler.getClientVersion( clientId ), parser, shouldGetNumColumns );
	}

	/**
	 * Parse the query string into a Query instance
	 */
	private Query parseStatementIntoQuery( String queryString, AntlrParser parser ) throws ParseCancellationException {
		try {
			return new Query( queryString, parser );
		} catch( ParseCancellationException e ) {
			throw new ParseCancellationException( e );
		}
	}

	/**
	 * Execute the write query and update our timestamp
	 */
	private QueryResult executeWriteQuery( long clientId, Query query, String queryString ) throws DBException {

		// Need to drain predictions first for this client to satisfy client sessions
		// This also registers us
		ResultRegistration<QueryResult> reg = new ResultRegistration( new ReentrantLock(), new ResultBox<QueryResult>() );
		engine.registerOrBlockIfQueryCurrentlyExecuting( clientId, queryString, DB_EXEC_VERSION, reg );
		try {
			// Run the write query against the database
			Integer queryResult = updateDatabase( clientId, queryString );

			// Increment the version on all written tables.
			// Get DB version
			// Assign it to client and return.
			VersionVector version = cacheHandler.updateDBVersionAndSetClientVersion( clientId, query );

			QueryResult qr = new QueryResult( queryResult, version );

			// Unblock anyone queued on us.
			engine.doneExecutingQuery( clientId, queryString, DB_EXEC_VERSION, qr );

			return qr;
		} catch( SQLException e ) {
			// Unblock anyone queued on us.
			engine.doneExecutingQuery( clientId, queryString, DB_EXEC_VERSION, new QueryResult( new LinkedList<Map<String,Object>>(), null ) );
			VersionVector version = cacheHandler.updateDBVersionAndSetClientVersion( clientId, query );
			logger.error( "Could not execute: \"{}\", got error: {}", queryString, e );
			throw new DBException( e );
		}
	}

	/**
	 * Notify everyone waiting on us in waitingTimestamps, and feed the query results
	 * through to the engine
	 */
	private void notifyEngineAndAllWaiters( long clientId, Query query, String queryString, QueryResult queryResult, List<VersionVector> waitingVersions, long startExecTime ) {
		// The queryResult will contain the timestamp of the entry we've retrieved so fast foward to
		// that time
		informEngineOfResult( clientId, ( System.nanoTime() / 1000 - startExecTime ), queryResult, query, queryString );

		if( !waitingVersions.isEmpty() ) {
			// Notify all clients waiting on timestamps that this client is leader of
			for( VersionVector version : waitingVersions ) {
				engine.doneExecutingQuery( clientId, queryString, version, queryResult );
				logger.debug( "Notifying waiters of \"{}\" at {}", queryString, version );
			}
		}
	}


	/**
	 * Try to retrieve queryString's resultset from memcached for any timestamp >= the
	 * provided one, updating our client timestamp and notifying waiters
	 */
	private QueryResult searchAllActiveVersionsForCachedResult( long clientId, CacheHandler ch, VersionVector clientVersion, Query query, String queryString, List<VersionVector> waitingVersions, long executionStart ) {
		// Get all active timestamps that are >= client's session timestamp
		logger.debug( "ClientId {} searching memcached for query \"{}\" with cacheKey {} vs {} and version >= {}", clientId, queryString, query.getCacheKey(), queryString.hashCode(), clientVersion );
		List<VersionVector> versions = cacheHandler.getActiveVersions( query, query.getTables(), clientVersion, Parameters.WORKLOAD_TYPE );
		logger.debug( "client {} has {} active versions", clientId, versions.size() );

		// Search the active versions
		for( VersionVector version : versions ) {
			logger.trace( "Got version: {}", version );
			QueryResult queryResult = retrieveFromMemcachedForFixedVersion( clientId, clientVersion, version, query, queryString, waitingVersions, executionStart );
			if( queryResult != null ) {
				logger.debug("client {} retrieved result for \"{}\" from cache", clientId, queryString );
				
				return queryResult;
			}
			logger.trace( "Stale version, trying next one..." );
		}
		logger.debug( "client {} could not retrieve result from cache!", clientId );
		
		return null;
	}

	/**
	 * Try to retrieve a resultset from memcached for the provided query at the given
	 * timestamp, notifying waiters and adjusting our timestamp if necessary
	 */
	private QueryResult retrieveFromMemcachedForFixedVersion( long clientId, VersionVector clientVersion, VersionVector version, Query query, String queryString, List<VersionVector> waitingVersions, long executionStart ) {
		logger.trace( "Trying to retrieve \"{}\" from memcached at ts {}", queryString, version );
		QueryResult queryResult = null;
	
		// Wait for results if there's something already running for this ts
		// Early exit if the query is already executing
		logger.trace( "Checking if query is already running: \"{}\"", queryString );

		// Any clients waiting on this timestamp will be notified once a QueryResult is obtained

		logger.trace( "Checking if query is already cached: \"{}\" at version {}", queryString, version );
		queryResult = cacheHandler.checkCache( query.getCacheKey(), version );

		// Early exit if the query is cached
		if( queryResult != null ) {
			logger.debug( "Client {} retrieved result for query \"{}\" from cache", clientId, queryString );
			return queryResult;
		}

		// Timestamp is an active timestamp in cassandra but has been evicted from query result cache,
		// delete from cassandra to prevent this from happening again
		logger.trace( "Client {}: result no longer in query result cache, deleting stale timestamp", clientId );
		cacheHandler.deleteStaleVersion( query.getCacheKey(), version );

		return null;
	}
	/**
	 * Check if there is another thread already executing this database query, and if so,
	 * use its result, notifying our waiters and generating a new client timestamp
	 */
	private QueryResult reuseDBExecSpecIfPossible( long clientId, CacheHandler cacheHandler, Set<String> tableNames, String queryString, List<VersionVector> waitingVersions, boolean shouldUpdateClientVersion ) {

		// Register ourselves using a special DB ts here (-1) in case someone else is
		// executing against the DB. forward results back as before
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), new ResultBox<QueryResult>() );

		logger.trace( "Checking if someone is already executing \"{}\" against DB", queryString );

		if( engine.registerOrBlockIfQueryCurrentlyExecuting( clientId, queryString, DB_EXEC_VERSION, reg ) ) {
			logger.debug( "Retrieved \"{}\" from someone who executed against DB", queryString );

			// Cool, someone else is executing against the DB, lets get their result
			QueryResult queryResult = reg.getResult();

			// Duplicate the query result so we don't change the shared object
			queryResult = QueryResult.surfaceCopyQueryResult( queryResult );

			VersionVector newVersion = VersionVector.maxAllIndexes( queryResult.getResultVersion(), cacheHandler.getClientVersion( clientId ) );
			queryResult.setResultVersion( newVersion );

			if( shouldUpdateClientVersion ) {
				cacheHandler.setClientVersion( clientId, newVersion );
			}
			
			if( !waitingVersions.isEmpty() ) {
				// Notify all clients waiting on timestamps that this client is leader of
				for( VersionVector version : waitingVersions ) {
					engine.doneExecutingQuery( clientId, queryString, version, queryResult );
					logger.debug( "Notifying waiters of \"{}\" at {}", queryString, version );
				}
			}

			// Just return, don't notify the engine
			return queryResult;
		}
		return null;
	}


	/**
	 * Check if there is another thread already executing this database query, and if so,
	 * use its result, notifying our waiters and generating a new client timestamp
	 */
	private QueryResult reuseDBExecNonSpecIfPossible( long clientId, CacheHandler cacheHandler, Query query, String queryString, List<VersionVector> waitingVersions, long executionStart ) {

		// Register ourselves using a special DB ts here (-1) in case someone else is
		// executing against the DB. forward results back as before
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), new ResultBox<QueryResult>() );

		logger.trace( "Checking if someone is already executing \"{}\" against DB", queryString );

		if( engine.registerOrBlockIfQueryCurrentlyExecuting( clientId, queryString, DB_EXEC_VERSION, reg ) ) {
			logger.debug( "Retrieved \"{}\" from someone who executed against DB", queryString );

			// Cool, someone else is executing against the DB, lets get their result
			QueryResult queryResult = reg.getResult();

			VersionVector newVersion = VersionVector.maxAllIndexes( queryResult.getResultVersion(), cacheHandler.getClientVersion( clientId ) );
			cacheHandler.setClientVersion( clientId, newVersion );

			logger.debug( "Updated client {}'s version to {}", clientId, newVersion );
			
			notifyEngineAndAllWaiters( clientId, query, queryString, queryResult, waitingVersions, executionStart );
			
			return queryResult;
		}
		return null;
	}

	/**
	 * Retrieve the query result from the database, notifying all waiters and generating
	 * a new client timestamp
	 */
	private QueryResult retrieveDBResult( long clientId, CacheHandler cacheHandler, Query query, String queryString, List<VersionVector> waitingVersions ) throws DBException {
		try {
			logger.trace( "Going to execute {} = {} against DB from retrieveDBResult", query.getCacheKey(), queryString );

			// Query the database
			QueryResult qr = queryDatabase( clientId, queryString, false );

			// Figure out what version the database is at. That's our version now
			// Updates all indexes of the client's version vector.
			VersionVector clientVersion = cacheHandler.getDBVersionForRead( clientId );
			cacheHandler.setClientVersion( clientId, clientVersion );
			qr.setResultVersion( clientVersion );

			logger.trace( "Client {}'s version advanced to {}", clientId, clientVersion );

			return qr;
		} catch( SQLException e ) {
			logger.error( "Query could NOT be executed: {} , Exception {}", queryString, e.getMessage() );
			for( VersionVector version : waitingVersions ) {
				engine.doneExecutingQuery( clientId, queryString, version, null );
			}

			throw new DBException( e );
		}
	}

	private Callable<QueryResult> asyncExec( long clientId, VersionVector version, String queryString, boolean shouldUpdateClientVersion ) {
		return () -> {
			AntlrParser parser = new AntlrParser();
			try {
				Query q = new Query( queryString, parser );
				QueryResult res = querySpeculatively( clientId, queryString, q.getTables(), shouldUpdateClientVersion );
				return res;
			} catch( Exception e ) {
				logger.error( "Couldn't async execute {}, reason: {}", queryString, e.getMessage() );
				return null;
			}
		};
	}

	public Future<QueryResult> asyncQuery( long clientId, VersionVector version, String queryString, boolean shouldUpdateClientVersion ) {
		Callable<QueryResult> asyncExecFunc = asyncExec( clientId, version, queryString, shouldUpdateClientVersion );
		return asyncExecutor.submit( asyncExecFunc );
	}

	public QueryResult querySpeculativelyToGetNumRows( long clientId, String queryString ) throws DBException {
		try {
			return queryDatabase( clientId, queryString, true );
		} catch( SQLException e ) {
			logger.error( "Could not execute: \"{}\", got error: {}", queryString, e );
			throw new DBException( e );
		}
	}

	/**
	 * Execute the provided queryString for the client, speculatively executing the query and not
	 * feeding the results back into the engine
	 */
	public QueryResult querySpeculatively( long clientId, String queryString, Set<String> tableNames, boolean shouldUpdateClientVersion ) throws DBException {
		try {

			// Speculative queries will never be write queries and they are never cached so we will
			// always execute the query against the database.

			// Check if we can reuse a running query.
			List<VersionVector> waitingVersions = new LinkedList<>();

			QueryResult qr = reuseDBExecSpecIfPossible( clientId, cacheHandler, tableNames, queryString, waitingVersions, shouldUpdateClientVersion );
			if( qr != null ) {
				// Hot Damn!
				return qr;
			}

			// OK, now we are registered as the one running this query.
			waitingVersions.add( DB_EXEC_VERSION );

			qr = queryDatabase( clientId, queryString, false );

			// Figure out what version the database is at. If this is not a containing query,
			// then we need to update our version to that. In any case, the query result should be
			// at this version.
			VersionVector newVersion = cacheHandler.getDBVersionForRead( clientId );

			if( shouldUpdateClientVersion ) {
				cacheHandler.setClientVersion( clientId, newVersion );
			}

			qr.setResultVersion( newVersion );

			if( !waitingVersions.isEmpty() ) {
				// Notify all clients waiting on timestamps that this client is leader of
				for( VersionVector version : waitingVersions ) {
					engine.doneExecutingQuery( clientId, queryString, version, qr );
					logger.debug( "Notifying waiters of \"{}\" at {}", queryString, version );
				}
			}

			return qr;

		} catch( SQLException e ) {
			logger.error( "Could not execute: \"{}\", got error: {}", queryString, e );
			throw new DBException( e );
		}
	}

	/**
	 * Execute the provided queryString for the client, feeding the results back into the client
	 */
	public QueryResult query( long clientId, String queryString, VersionVector version, AntlrParser parser, boolean shouldGetNumColumns ) throws DBException {
		logger.info( "Executing \"{}\" against DB!", queryString );
		QueryResult queryResult;
		Query query = null;

		long queryRespStart = System.nanoTime() / 1000;

		// Create statement from the query
		try {
			query = parseStatementIntoQuery( queryString, parser );
		} catch( ParseCancellationException e ) {
			// We couldn't parse it, not sure if its a read or a write
			logger.error( "Query cannot be parsed: {}", queryString );
			logger.error( "Reason: {}", e.getMessage() );
			throw new DBException( e );
		}

		// Early exit for write queries
		if( !query.isReadQuery() ) {
			queryResult = executeWriteQuery( clientId, query, queryString );
			informEngineOfResult( clientId, ( System.nanoTime() / 1000 - queryRespStart ), queryResult, query, queryString );
			promptEngineForImportantQueryReload( clientId );

			return queryResult;
		}

		long clockStart = System.nanoTime() / 1000;

		// If we need to get query metadata, run against the database directly
		// Retrieving from caching and grabbing other's SelectLists won't let us compute this
		// TODO: Should we cache query metadata as well or mark queryResults that will obtain metadata in the future?
		if( shouldGetNumColumns ) {
			try {
				QueryResult qr = queryDatabase( clientId, queryString, shouldGetNumColumns );

				// We read the database, so we'd better update our version vector to reflect it
				VersionVector clientVersion = cacheHandler.getDBVersionForRead( clientId );
				cacheHandler.setClientVersion( clientId, clientVersion );
				qr.setResultVersion( clientVersion );
				logger.trace( "Client {}'s version advanced to {}", clientId, clientVersion );

				return qr;
			} catch( SQLException e ) {
				throw new DBException( e );
			}
		}

		// Read query fallthrough
		// Preprocess query
		boolean didCritLoad = promptEngineForQueryPreprocessing( clientId, query, queryString );

		// Search cache for result
		// If this query was part of a vectorizable that was taken care of by pre-processing, then query will be cached here.
		List<VersionVector> waitingVersions = new LinkedList<VersionVector>();
		VersionVector clientVersion = cacheHandler.getClientVersion( clientId );
		logger.trace( "Client {}'s version is {}", clientId, clientVersion );

		queryResult = searchAllActiveVersionsForCachedResult( clientId, cacheHandler, version, query, queryString, waitingVersions, clockStart );

		// Early exit for cached queries
		if( queryResult != null ) {
			VersionVector queryResultVersion = queryResult.getResultVersion();
			VersionVector newVersion = VersionVector.maxAllIndexes( queryResultVersion, version );
			cacheHandler.setClientVersion( clientId, newVersion );
			if( !didCritLoad ) {
				logger.info( "Retrieved query result from cache: \"{}\"", queryString );
			} else {
				logger.info( "Just put query result in cache: \"{}\"", queryString );
			}
			this.cacheHitCounter.incrementAndGet();
			queryResult.announceCached();
			informEngineOfResult( clientId, ( System.nanoTime() / 1000 - queryRespStart ), queryResult, query, queryString );
			return queryResult;
		} else {
			logger.info( "Could not get cached result for query: \"{}\"", queryString );
		}

		// Out of cached timestamps, need to check DB
		logger.debug( "client {} found no cached results! Going to check db!", clientId );

		// See if anyone else is running on the DB
		queryResult = reuseDBExecNonSpecIfPossible( clientId, cacheHandler, query, queryString, waitingVersions, clockStart );
		
		// Early exit for queries that are already running
		if( queryResult != null ) {
			return queryResult;
		}

		// No running queries
		logger.debug( "client {} found no running queries, going to query DB.", clientId );

		this.cacheMissCounter.incrementAndGet();

		// Query is not already running or cached so we will lead execution against the db
		waitingVersions.add( DB_EXEC_VERSION );

		// Result is obtained from database
		queryResult = retrieveDBResult( clientId, cacheHandler, query, queryString, waitingVersions );

		// Write the query result to the cache
		if( queryResult != null ) {
			boolean success = cacheHandler.cacheResult( query.getCacheKey(), query.getTables(), queryResult );
			if( success ) {
				logger.debug( "Result for query:{} is stored on query result cache", query.getCacheKey() );
			} else {
				logger.warn( "!!!Result for query:{} is NOT stored on query result cache", query.getCacheKey() );
			}

		}


		// Initiate engine post processing
		notifyEngineAndAllWaiters( clientId, query, queryString, queryResult, waitingVersions, clockStart );
		promptEngineForImportantQueryReload( clientId );

		logger.debug( "Returning result for client {}.", clientId );

		return queryResult;
	}

	private boolean promptEngineForQueryPreprocessing( long clientId, Query query, String queryString ) {
		logger.debug( "QID {} = {}", query.getId().getId(), queryString );
		return engine.preprocessQuery( clientId, query );
	}

	/**
	 * Tell the engine we have query results for the provided select query It
	 * will track the result and look for speculative executions in the
	 * background
	 *
	 * @param resultArray
	 * @param query
	 */
	private void informEngineOfResult( long clientId, long responseTime, QueryResult queryResult, Query query, String queryString ) {
		ExecutedQuery eq = new ExecutedQuery( query, DateTime.now(), responseTime, queryResult );
		engine.addToQueryStream( clientId, eq, queryString );
		logger.debug( "Added QID {} to clientId {}'s query stream", query.getId().getId(), clientId );
		logger.debug( "QID {} = {}", query.getId().getId(), queryString );
		engine.postprocessQuery( clientId, eq );
	}

	private void promptEngineForImportantQueryReload( long clientId ) {
		engine.reloadImportantQueries( clientId );
	}

	/**
	 * Get the client's conn from the map, or add it to the map if it doesn't exist.
	 * @param clientId
	 * @return conn
	 * @throws SQLException
	 */
	public Connection getClientConn( long clientId ) throws SQLException {
		//Connection conn = source.getConnection();
		Connection conn = dbPool.getConn();
		/*
		if( connMap.containsKey(clientId) ){
			logger.debug("Conn already exists for clientId {}", clientId);
			conn = connMap.get(clientId);
		} else {
			logger.debug("Creating new conn for clientId {}", clientId);
			conn = source.getConnection();
			connMap.put(clientId, conn);
		}
		*/
		logger.trace( "Returned conn is null? {}", conn == null );
		return conn;
	}

	private QueryResult queryDatabase( long clientId, String query, boolean shouldGetMetadata ) throws SQLException {
		Connection conn = null;
		ResultSet result = null;
		QueryResult queryResult = null;
		int numColumns = -1;

		logger.trace( "Executing {} against DB", query );

		try {
			// Get client connection and time it
			long connstart = System.nanoTime() / 1000;
			conn = getClientConn(clientId);
			long connend = System.nanoTime() / 1000;
			logger.debug( "Time to get conn: {}", connend - connstart );

			// Get result from database and time it
			long queryStart = System.nanoTime() / 1000;
			result = conn.createStatement().executeQuery( query );
			long queryEnd = System.nanoTime() / 1000;
			logger.debug( "Raw DB Exec Time: {}", queryEnd - queryStart );

			// Sort out metadata if requested
			if( shouldGetMetadata ) {
				// Get metadata and time it
				long mdGetStart = System.nanoTime() / 1000;
				ResultSetMetaData rsmd = result.getMetaData();
				numColumns = rsmd.getColumnCount();
				long mdGetEnd = System.nanoTime() / 1000;
				logger.debug( "Time to get metadata: {}", mdGetEnd - mdGetStart );
			}

			// Change results into a java thing
			List<Map<String,Object>> resultArray = ResultSetConverter.getEntitiesFromResultSet(result);
			
			// Record the total processing time for Q
			long processingEnd = System.nanoTime() / 1000;
			logger.debug( "DB Q Time for \"{}\": {}", query, processingEnd - queryStart );

			queryResult = new QueryResult( resultArray, null, numColumns );
		} catch ( SQLException e ) {
			throw e;
		} finally {
			dbPool.returnConn( conn );
		}

		return queryResult;
	}

	private Integer updateDatabase( long clientId, String query ) throws SQLException {
		Connection conn = null;
		Integer result = null;

		try {
			conn = getClientConn( clientId );
			result = conn.createStatement().executeUpdate( query );
		} catch( SQLException e ) {
			// This is a hack for TAOBench. The parser doesn't know how to handle
			// ON CONFLICT, so we'll just do the insert and ignore the uniqueness violation.
			if (!e.getMessage().contains("duplicate key value violates unique constraint")
					|| !e.getMessage().contains("objects_pkey")) {
				throw e;
			} else {
				result = 1;
			}
		} finally {
			dbPool.returnConn( conn );
		}

		return result;
	}

	private DataSource getPostgresqlDataSource() {
		BasicDataSource source = new BasicDataSource();
		String dbUrl = "jdbc:postgresql://" + Configuration.getDatabaseServer() + ":"
				+ String.valueOf( Configuration.getDatabasePort() ) + "/" + Configuration.getDatabaseName();
		source.setUsername( Configuration.getDatabaseUsername() );
		source.setPassword( Configuration.getDatabasePassword() );
		source.setDriverClassName( Configuration.getDatabaseDriver() );
		source.setUrl( dbUrl );
		source.setInitialSize( Parameters.REMOTE_DB_CONNS );
		source.setMaxIdle( Parameters.REMOTE_DB_CONNS );
		source.setMaxTotal( Parameters.REMOTE_DB_CONNS );
		source.setDefaultTransactionIsolation( java.sql.Connection.TRANSACTION_SERIALIZABLE );
		return source;
	}

	private DataSource getMySQLDataSource() {
		String dbUrl = "jdbc:mysql://" + Configuration.getDatabaseServer() + ":"
				+ String.valueOf( Configuration.getDatabasePort() ) + "/" + Configuration.getDatabaseName() +
				"?zeroDateTimeBehavior=CONVERT_TO_NULL&useSSL=false&allowPublicKeyRetrieval=true";
		logger.info( "Connecting using dbURL: {}", dbUrl );
		BasicDataSource source = new BasicDataSource();

		source.setUsername( Configuration.getDatabaseUsername() );
		source.setPassword( Configuration.getDatabasePassword() );
		source.setDriverClassName( Configuration.getDatabaseDriver() );
		source.setUrl( dbUrl );
		source.setInitialSize( Parameters.REMOTE_DB_CONNS );
		source.setMaxIdle( Parameters.REMOTE_DB_CONNS );
		source.setMaxTotal( Parameters.REMOTE_DB_CONNS );
		source.setDefaultTransactionIsolation( java.sql.Connection.TRANSACTION_SERIALIZABLE );
		return source;
	}

	private DataSource getMariaDBDataSource() {
		String dbUrl = "jdbc:mariadb://" + Configuration.getDatabaseServer() + ":"
				+ String.valueOf( Configuration.getDatabasePort() ) + "/" + Configuration.getDatabaseName();
		logger.info( "Connecting using dbUrl: {}", dbUrl );
		MariaDbPoolDataSource source = new MariaDbPoolDataSource();

		try	{
			source.setUser( Configuration.getDatabaseUsername() );
			source.setPassword( Configuration.getDatabasePassword() );
			source.setUrl( dbUrl );
			source.setMaxPoolSize( Parameters.REMOTE_DB_CONNS );
			source.setMinPoolSize( Parameters.REMOTE_DB_CONNS );
		} catch ( SQLException e ) {
			logger.error( "SQL Exception while getting MariaDB connection: {}", e.getMessage() );
		}

		return source;
	}

	/*
	 * Stop the markov constructors associated with the engine associated with this db
	 */
	public void stopDB() {
		logger.trace( "DBImpl stop DB called" );
		engine.stopEngine();
	}
}
