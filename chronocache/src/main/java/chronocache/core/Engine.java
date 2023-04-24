package chronocache.core;

import java.util.List;
import java.util.Map;

import chronocache.core.qry.Query;
import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.QueryResult;

import chronocache.core.future.ResultRegistration;

import chronocache.db.DBFactory;

public interface Engine {


	public DBFactory.DBType getDBType();
	/**
	 * Have the engine preprocess the query not yet executed by the given clientId
	 */
	public boolean preprocessQuery( long clientId, Query q );

	/**
	 * Have the engine process the query executed by the given clientId
	 */
	public void postprocessQuery( long clientId, ExecutedQuery q );

	/**
	 * Indicate to the engine that now would be a good time to reload
	 * any queries it deems important for clientId.
	 */
	public void reloadImportantQueries( long clientId );

	/**
	 * Add the query to the client's query stream - note that this
	 * is different from processing the query!
	 */
	public void addToQueryStream( long clientId, ExecutedQuery q, String query );

	/**
	 * Gracefully stop the engine
	 */
	public void stopEngine();

	/**
	 * Register for a query that is currently executing, or lead the execution against the database
	 * We assume that there can be only one write query ongoing at a time for a client, and that if that write query is ongoing it is the non-prediction query for that client.
	 * Any concurrent reads are from predictions and ought to be aborted to ensure client session safety.
	 */
	public boolean registerOrBlockIfQueryCurrentlyExecuting( 
			long clientId,
			String queryString,
			VersionVector version,
			ResultRegistration<QueryResult> reg
	);

	/**
	 * Tell the engine that the given query is done executing, and returned
	 * with the given result.
	 */
	public void doneExecutingQuery( long clientId, String queryString, VersionVector version, QueryResult result );

}
