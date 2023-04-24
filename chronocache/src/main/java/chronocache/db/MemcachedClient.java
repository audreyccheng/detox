package chronocache.db;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whalin.MemCached.MemCachedClient;
import com.whalin.MemCached.SockIOPool;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.util.Configuration;

public class MemcachedClient implements QueryResultCache {
	private static MemcachedClient instance;

	private static Logger logger = LoggerFactory.getLogger( MemcachedClient.class );

	private MemCachedClient memCachedClient;
	private static final String MEMCACHED_INSTANCE_NAME = "chronocache";

	private MemcachedClient() {
		init();
	}

	public static MemcachedClient getInstance() {
		if (instance == null) {
			instance = new MemcachedClient();
		}
		return instance;
	}

	private void init() {

		String[] servers = { Configuration.getMemcachedAddress() + ":" + Configuration.getMemcachedPort() };
		//String[] servers = Configuration.getMemcachedServers();
		SockIOPool pool = SockIOPool.getInstance(MEMCACHED_INSTANCE_NAME);
		pool.setServers(servers);
		pool.setFailover(true);
		pool.setInitConn(100);
		pool.setMinConn(100);
		pool.setMaxConn(250);
		pool.setMaintSleep(30);
		pool.setNagle(false);
		pool.setSocketTO(3000);
		pool.setAliveCheck(true);
		pool.initialize();

		memCachedClient = new MemCachedClient(MEMCACHED_INSTANCE_NAME);
		memCachedClient.flushAll();
		logger.debug("Memcached Client on: {} is initialized", servers[0]);
	}

	@Override
	public QueryResult get( String cacheKey ) {
		List<Map<String,Object>> result = (List<Map<String,Object>>) memCachedClient.get(cacheKey);
		if( result == null ) {
			return null;
		}
		return new QueryResult( result, null );

	}

	@Override
	public void put(String cacheKey, QueryResult result ) {
		memCachedClient.set(cacheKey, result.getSelectResult() );
	}

	public boolean cache_exists(String cacheKey) {
		return memCachedClient.keyExists(cacheKey);
	}

	private Map<String, Map<String, String>> getstats() {
		Map<String, Map<String, String>> stats = memCachedClient.stats();
		return stats;
	}

	private Map<String, Map<String, String>> getslabs() {
		Map<String, Map<String, String>> slabs = memCachedClient.statsSlabs();
		return slabs;
	}

	public boolean invalidate(List<String> key) {
		for (String k : key) {
			memCachedClient.delete(k);
			//if (!memCachedClient.delete(k))
		//		return false;
		}
		return true;
	}

	public boolean invalidateAll() {
		return memCachedClient.flushAll();
	}

	private boolean evict() {
		boolean succ = true;
		// todo
		// default: lru (already have)

		return succ;
	}
}
