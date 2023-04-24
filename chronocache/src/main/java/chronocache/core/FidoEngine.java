package chronocache.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.qry.Query;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.QueryResult;

import chronocache.core.future.OngoingSpeculativeExecutionTable;
import chronocache.core.future.ResultRegistration;

import chronocache.core.fido.FidoModel;

import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.ParserPool;

import chronocache.db.DB;
import chronocache.db.DBFactory;
import chronocache.db.DBException;

public class FidoEngine implements Engine {
	private Logger logger = LoggerFactory.getLogger(FidoEngine.class);

	private OngoingSpeculativeExecutionTable currentlyExecutingQueriesTable;
	private ConcurrentMap<Long, LinkedList<String>> previousQueries;
	private FidoModel model;

	private Executor backgroundExecutor;
	private VersionVectorFactory versionVectorFactory;

	public FidoEngine(FidoModel model)  {
		this.currentlyExecutingQueriesTable = new OngoingSpeculativeExecutionTable();
		this.previousQueries = new ConcurrentHashMap<>();
		this.model = model;
		this.versionVectorFactory = new VersionVectorFactory( Parameters.WORKLOAD_TYPE );
		this.backgroundExecutor = Executors.newFixedThreadPool( 1500 );
	}

	private class FidoWorker implements Runnable {
		long clientId;
		private List<String> previousQueryKeys;

		public FidoWorker( long clientId, List<String> previousQueryKeys ) {
			this.clientId = clientId;
			this.previousQueryKeys = previousQueryKeys;
		}

		@Override
			public void run() {
				logger.info("Starting fido worker run for client {}", clientId);
				List<String> predictedQueries =
					model.makePrediction( previousQueryKeys,
							Parameters.FIDO_MAX_NUM_PREDICTIONS );

				for( String predictedQuery : predictedQueries ) {
					if( Math.random() <= Parameters.CHANCE_TO_SPEC_EXEC ) {
						speculativelyExecuteQuery( predictedQuery );
					}
				}
			}

		private void speculativelyExecuteQuery( String query ) {

			DB db = new DBFactory().getDBInstance( DBFactory.DBType.REAL_DB );
			VersionVector version = db.getCacheHandler().getClientVersion( clientId );
			AntlrParser parser = new AntlrParser();
			Query q = new Query( query, parser );
			try {
				logger.info("fido worker for client {} trying to speculatively execute {}", clientId, query);
				db.querySpeculatively( clientId, query, q.getTables(), false );
			} catch( DBException e ) {
				logger.error( "Couldn't execute speculative query, error {}", e.getMessage() );
			}
		}
	}

	public DBFactory.DBType getDBType() {
		return DBFactory.DBType.REAL_DB;
	}

	public boolean preprocessQuery( long clientId, Query q ) {
		return false;
	}

	public void postprocessQuery( long clientId, ExecutedQuery q ) {
		FidoWorker worker = new FidoWorker( clientId, previousQueries.get( clientId ) );
		backgroundExecutor.execute( worker );
	}

	public void addToQueryStream( long clientId, ExecutedQuery q, String query ) {
		LinkedList<String> queries = previousQueries.get(clientId);
		if( queries == null ) {
			previousQueries.put( clientId, new LinkedList<>() );
			logger.info("Adding new client {} to queryStream", clientId);
		}

		queries = previousQueries.get( clientId );
		queries.offerLast( q.getCacheKey() );
		logger.info("queryStream: {}", queries);
		while( queries.size() > model.getPrefixLength() ) {
			String cacheKey = queries.removeFirst();
			logger.info("Removing old query {} from new client {} queryStream", cacheKey, clientId);
			logger.info("queryStream: {}", queries);
		}

		previousQueries.put( clientId, queries );
	}


	public void stopEngine() {}

	public boolean registerOrBlockIfQueryCurrentlyExecuting( long clientId, String queryString, VersionVector version,
			ResultRegistration<QueryResult> reg ) {
		return currentlyExecutingQueriesTable
			.registerOrBlockIfQueryCurrentlyExecuting( clientId, queryString, version, reg );
	}

	public void doneExecutingQuery( long clientId, String queryString, VersionVector version,
			QueryResult result) {
		currentlyExecutingQueriesTable.doneExecutingQuery( clientId, queryString, version, result );
	}

	/* Ignore for now */
	public void reloadImportantQueries( long clientId ) {
		return;
	}
}
