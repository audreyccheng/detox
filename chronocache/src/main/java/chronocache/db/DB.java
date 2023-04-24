package chronocache.db;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import chronocache.core.VersionVector;
import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.QueryResult;
import chronocache.core.CacheHandler;


public interface DB {
	public int getCacheHits();
	public int getCacheMiss();
	public default void resetCacheStats() {
		return;
	}

	public QueryResult query( long clientId, String queryString ) throws DBException;
	public QueryResult query( long clientId, String queryString, boolean shouldGetNumColumns ) throws DBException;
	public QueryResult query( long clientId, String queryString, AntlrParser parser, boolean shouldGetNumColumns ) throws DBException;
	public QueryResult query( long clientId, String queryString, VersionVector version, AntlrParser parser, boolean shouldGetNumColumns ) throws DBException;
	public QueryResult querySpeculativelyToGetNumRows( long clientId, String queryString ) throws DBException;
	public QueryResult querySpeculatively( long clientId, String queryString, Set<String> tableNames, boolean shouldUpdateClientVersion ) throws DBException;

	public Future<QueryResult> asyncQuery( long clientId, VersionVector version, String queryString, boolean shouldUpdateClientVersion );

	public Connection getClientConn( long clientId ) throws SQLException;

	public CacheHandler getCacheHandler();

	public void stopDB();
}
