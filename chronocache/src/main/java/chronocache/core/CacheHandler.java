package chronocache.core;

import java.util.Collection;
import java.util.List;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.db.MemcachedClient;


public interface CacheHandler {

	public VersionVector getZeroVersionVector();

	public VersionVector updateDBVersionAndSetClientVersion( long cid, Query query );
	public VersionVector getDBVersionForRead( long clientId );
	public VersionVector getClientVersion( long cid );

	public void setClientVersion( long cid, VersionVector vv );

	public void deleteStaleVersion( String queryKey, VersionVector vv );

	public List<VersionVector> getActiveVersions( Query query, Collection<String> tables, VersionVector vv, WorkloadType wl );

	public QueryResult checkCache( String cacheKey, VersionVector vv );

	public boolean cacheResult( String cacheKey, Collection<String> tables, QueryResult result );

}
