package chronocache.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.db.QueryResultCache;

public class InMemoryCacheHandler implements CacheHandler {

	public class MemcachedKey {
		public String queryKey;
		public VersionVector vv;
		public MemcachedKey( String queryKey, VersionVector vv ) {
			this.queryKey = queryKey;
			this.vv = vv;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			return queryKey + "_" + vv.toString();
		}

	}

	private Logger logger = LoggerFactory.getLogger( InMemoryCacheHandler.class );

	private QueryResultCache cache;
	private ConcurrentHashMap<Long, VersionVector> clientVersionMap;
	private ConcurrentHashMap<String, VersionVector> cachedQueryVersions;
	private Map<String, Long> currentDBVersion;

	private VersionVectorFactory versionVectorFactory;
	private RelationVersionTable relationVersionTable;

	public InMemoryCacheHandler( QueryResultCache cache, WorkloadType wlType ) {
		this.cache = cache;
		clientVersionMap = new ConcurrentHashMap<>();
		cachedQueryVersions = new ConcurrentHashMap<>();
		currentDBVersion = new HashMap<String, Long>();

		versionVectorFactory = new VersionVectorFactory( wlType );
		relationVersionTable = versionVectorFactory.getRelationVersionTable();
		for( String tableName : relationVersionTable.getTableNames() ) {
			currentDBVersion.put( tableName, new Long(0L) );
		}
	}

	public InMemoryCacheHandler( QueryResultCache cache ) {
		this( cache, Parameters.WORKLOAD_TYPE );
	}

	public VersionVector getZeroVersionVector() {
		return versionVectorFactory.createVersionVector();
	}

	// Atomic update table versions and read full vector
	private Map<String, Long> getTableVersions( Collection<String> queryTables, boolean increment ) {
		Map<String, Long> tableVersions = new HashMap<>();
		String[] allTables = relationVersionTable.getTableNames();

		// Lock
		synchronized( currentDBVersion ) {

			// Pass 1: Increment table versions if necessary
			if( increment ) {
				// For each table in the query, find it and increment it
				for( String table : queryTables ) {
					Long version = currentDBVersion.get( table );
					if( version != null ) {
						version++;
						// put it back
						currentDBVersion.put( table, version );
					} // Could be null for CTE tables
				}
			}

			// Pass 2: Get all table versions
			for( String table: allTables ) {
				Long version = currentDBVersion.get( table );
				if( version != null ) {
					// copy
					Long ourVersion = new Long( version );
					tableVersions.put( table, ourVersion );
				} // Could be null for CTE tables
			}
		} // drop lock
		return tableVersions;
	}

	public VersionVector updateDBVersionAndSetClientVersion( long cid, Query query ) {
		return updateDBVersionAndSetClientVersion( cid, query.getTables() );

	}
	public VersionVector updateDBVersionAndSetClientVersion( long cid, Collection<String> tables ) {

		// Atomic increment the version of tables, do read to get DB version vector.
		Map<String, Long> newTableVersions = getTableVersions( tables, true );

		// The DB Version >= our version vector in every index, so just set and forget
		VersionVector cvv = getClientVersion( cid );
		for( String tableName : newTableVersions.keySet() ) {
			Integer i = versionVectorFactory.getRelationIndexBinding( tableName );
			cvv.setTableVersion( i, newTableVersions.get( tableName ) );
		}

		// Update client's version vector
		clientVersionMap.put( cid, cvv );
		return cvv;
	}

	public VersionVector getDBVersionForRead( long clientId ) {
		VersionVector cvv = getClientVersion( clientId ); 

		// Atomic read DB version vector. DOES NOT UPDATE CLIENT VERSION VECTOR
		Map<String, Long> tableVersions = getTableVersions( null, false );
		for( String tableName : tableVersions.keySet() ) {
			Integer i = versionVectorFactory.getRelationIndexBinding( tableName );
			cvv.setTableVersion( i, tableVersions.get( tableName ) );
		}

		return cvv;
	}

	public void setClientVersion( long cid, VersionVector vv ) {
		logger.debug( "DCH++++: Hard Setting version for client {} to {}", cid, vv );
		clientVersionMap.put( cid, vv );
	}

	public void deleteStaleVersion( String queryKey, VersionVector vv ) {
		return;
	}


	public List<VersionVector> getActiveVersions( Query query, Collection<String> tables, VersionVector vv, WorkloadType wl ) {
		String cacheKey = query.getCacheKey();
        logger.debug( "Looking for active versions of {} from {}", cacheKey, vv );
        logger.debug( "Tables: {}", tables );

		List<VersionVector> matchingVectors = new LinkedList<>();
		// Find the most recent version of this query that we have cached.
		VersionVector cachedVV  = cachedQueryVersions.get( cacheKey );
		if( cachedVV != null ) {
			boolean shouldAdd = true;

			// If any of its versions in its accessed tables are less than our version vector, we can't add it.
			for( String table : tables ) {
				Integer i = versionVectorFactory.getRelationIndexBinding( table );
				if( i != null ) {
					if( cachedVV.versionAtIndex( i ) < vv.versionAtIndex( i ) ) {
						logger.debug( "Can't use cached version, index {} is bigger: {} for table {}", i, table );
						shouldAdd = false;
						break;
					}
				} // again, CTE bs
			}

			// Huzzah!
			if( shouldAdd ) {
				matchingVectors.add( cachedVV );
			}
		}
		return matchingVectors;
	}

	public QueryResult checkCache( String cacheKey, VersionVector vv ) {
		MemcachedKey memcachedKey = new MemcachedKey( cacheKey, vv );
		QueryResult result = cache.get( memcachedKey.toString() );
		if( result != null ) {
			result.setResultVersion( vv );
			return QueryResult.surfaceCopyQueryResult( result );
		}
		return null;
	}

	private void recordReadQuery( String cacheKey, Collection<String> tables, VersionVector vv ) {
		// Get the most recent version of this thing in the cache
		VersionVector cachedVV = cachedQueryVersions.get( cacheKey );

		if( cachedVV == null ) {
			// There's nothing, so put it in the cache if we won the race
			cachedQueryVersions.putIfAbsent( cacheKey, vv );
			return;
		}

		// A version is there. See if we are the most recent version.
		boolean shouldReplace = true;
		// Tables is only null if we were constructed speculatively. In this case we were executed against the DB and are always
		// the most recent.
		if( tables != null ) {
			//  If any of its versions on the accessed tables are higher than ours, we can't replace it.
			for( String table: tables ) {
				Integer i = versionVectorFactory.getRelationIndexBinding( table );
				if( i != null ) {
					if( cachedVV.versionAtIndex( i ) > vv.versionAtIndex( i ) ) { 
						shouldReplace = false;
					}
				}
			}
		}
		if( !shouldReplace ) {
			return;
		}

		// OK, try to put us in the cache
		cachedQueryVersions.replace( cacheKey, cachedVV, vv );
	}

	public boolean cacheResult( String cacheKey, Collection<String> tables, QueryResult result ) {

		//Put in memcached
		VersionVector vv = result.getResultVersion();
		MemcachedKey memcachedKey = new MemcachedKey( cacheKey, vv );
		cache.put( memcachedKey.toString(), result );
		
		// Record this most recent version. This could fail, but it doesn't matter. We just won't find
		// the key during lookup.
		recordReadQuery( cacheKey, tables, vv );

		logger.debug( "Caching query: {} at version: {}", cacheKey, result.getResultVersion() );
		logger.trace( "Result for {} is stored in memcached and cassandra", cacheKey );
		return true;
	}


	public VersionVector getClientVersion( long cid ) {
		if( clientVersionMap.containsKey( cid ) ) {
			logger.trace( "Version already exists for client {}", cid );
			return new VersionVector( clientVersionMap.get( cid ) );
		} else {
			VersionVector vv = getZeroVersionVector();
			clientVersionMap.put( cid, vv );
			logger.debug( "DCH++++: Hard Setting version for client {} to {}", cid, vv );
			return vv;
		}
	}
}
