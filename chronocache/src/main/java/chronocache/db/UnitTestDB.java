package chronocache.db;

import com.datastax.driver.core.*;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import chronocache.core.CacheHandler;
import chronocache.core.VersionVector;
import chronocache.core.qry.QueryResult;
import chronocache.core.qry.Query;
import chronocache.core.Engine;
import chronocache.core.MarkovEngine2;
import chronocache.core.MarkovGraph;
import chronocache.core.MarkovNode;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.parser.AntlrParser;
import chronocache.core.WorkloadType;
import chronocache.db.MemcachedClient;

public class UnitTestDB implements DB {
	private static final ArrayList<Long> version = new ArrayList<Long>() {{
		add( 0L );
	}};


	private class ZeroVersionVector extends VersionVector { 
		public ZeroVersionVector() {
			super( version );
		}
	}

	private class UselessCacheHandler implements CacheHandler {

        private Map<Long, VersionVector> clientVersions;
		public UselessCacheHandler () {
            clientVersions = new HashMap<>();
		}

		@Override
		public void deleteStaleVersion( String queryKey, VersionVector version ) {
			return;
		}

		@Override
		public List<VersionVector> getActiveVersions( Query query, Collection<String> tables, VersionVector version, WorkloadType wl ) {
			//Force db
			return new LinkedList<>();
		}

		@Override
		public QueryResult checkCache( String cacheKey, VersionVector version ) {
			//Memcached always empty
			return null;
		}

		@Override
		public boolean cacheResult( String cacheKey, Collection<String> tables, QueryResult res ) {
			return true;
		}


        @Override
        public VersionVector getClientVersion( long cid ) {
            return clientVersions.get( cid );
        }

        @Override
	    public void setClientVersion( long cid, VersionVector vv ) {
            clientVersions.put( cid, vv );
        }

        @Override
    	public VersionVector updateDBVersionAndSetClientVersion( long cid, Query query ) {
            return new ZeroVersionVector();
        }

        @Override
        public VersionVector getDBVersionForRead( long clientId ) {
            return new ZeroVersionVector();
        }

        @Override
	    public VersionVector getZeroVersionVector() {
            return new ZeroVersionVector();
        }

	}

	private class FutureWrap<T> implements Future<T> {

		private T val;

		public FutureWrap( T val ) {
			this.val = val;
		}

		@Override
		public boolean cancel(boolean mayInterrupt) {
			return true;
		}

		@Override
		public T get() {
			return val;
		}

		@Override
		public T get( long timeout, TimeUnit unit ) {
			return val;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

	}

	private Logger logger = LoggerFactory.getLogger(UnitTestDB.class);

	private CacheHandler ch;
	protected Engine engine;

	public UnitTestDB( Engine e ) {
		engine = e;
        ch = new UselessCacheHandler();
	}

	public int getCacheHits(){ return 0; }
	public int getCacheMiss(){ return 0; }

	public DBFactory.DBType getDBType() {
		return DBFactory.DBType.TEST_DB;
	}

	public CacheHandler getCacheHandler(){
		return ch;
	}

	public QueryResult query( long clientId, String queryString ) throws DBException {
		return query( clientId, queryString, false );
	}

	public QueryResult query( long clientId, String queryString, boolean shouldGetMetadata ) throws DBException {
		AntlrParser parser = new AntlrParser();
		return query( clientId, queryString, parser, shouldGetMetadata );
	}

	public QueryResult query( long clientId, String queryString, AntlrParser parser, boolean shouldGetMetadata ) throws DBException {
		return query( clientId, queryString, new ZeroVersionVector(), parser, shouldGetMetadata );
	}

	public Future<QueryResult> asyncQuery( long clientId, VersionVector version, String queryString, boolean shouldUpdateClientVersion ) {
		try {
			return new FutureWrap( query( clientId, queryString ) );
		} catch( DBException e ) {
			return null;
		}
	}

	public QueryResult query( long clientId, String queryString, VersionVector version, AntlrParser parser, boolean shouldGetMetadata ) throws DBException {
		if( engine instanceof MarkovEngine2 ) {
			MarkovEngine2 eng = (MarkovEngine2) engine;
			MarkovGraph g = eng.getClientMarkovPredictionGraph( clientId );
			logger.debug("Going to return fake results for {}", queryString);
			Query query = new Query( queryString, parser );
			MarkovNode node = g.getOrAddNode( query.getId() );
			logger.debug("Returning {}", node.getPreviousResultSet() );
			return node.getPreviousResultSet();
		}
		return null;
	}

	public QueryResult querySpeculativelyToGetNumRows( long clientId, String queryString ) throws DBException {
		return null;
	}

	public QueryResult querySpeculatively( long clientId, String queryString, Set<String> tableNames, boolean shouldUpdateClientVersion ) throws DBException {
		return null;
	}

	public Connection getClientConn( long clientId ) throws SQLException {
		return null;
	}

	public void stopDB() {
		logger.trace("UnitTestDB stop DB called");
		engine.stopEngine();
	}

}
