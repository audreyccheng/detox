package chronocache.db;

import chronocache.core.qry.QueryResult;

public interface QueryResultCache {

	public void put( String key, QueryResult result );

	public QueryResult get( String key );

}
