package chronocache.core;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.db.QueryResultCache;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class InMemoryCacheHandlerTest {

    private class HashMapCache implements QueryResultCache {
        private Map<String, QueryResult> cache;

        public HashMapCache() {
            cache = new HashMap<>();
        }

        @Override
        public void put( String key, QueryResult result ) {
            cache.put( key, result ); 
        }

        @Override
        public QueryResult get( String key ) {
            return cache.get( key );
        }

    }

    @Test
    public void testPutGet() {
        InMemoryCacheHandler ch = new InMemoryCacheHandler( new HashMapCache(), WorkloadType.TPCE );
        List<Map<String,Object>> selResult = new LinkedList<>();
        VersionVector resultVersion = ch.getZeroVersionVector();
        Query q = new Query( "SELECT b FROM customer" );
        QueryResult qr = new QueryResult( selResult, resultVersion );
        ch.cacheResult( q.getCacheKey(), q.getTables(), qr );

        List<VersionVector> versions = ch.getActiveVersions( q, q.getTables(), ch.getZeroVersionVector(), WorkloadType.TPCE );
        assertThat( versions.size(), equalTo( 1 ) );
        assertThat( versions.iterator().next(), equalTo( resultVersion ) );
    }

    @Test
    public void incrementDBVersion() {
        InMemoryCacheHandler ch = new InMemoryCacheHandler( new HashMapCache(), WorkloadType.TPCE );
        Query q = new Query( "SELECT t FROM last_trade, customer_account" );
        VersionVector vv = ch.updateDBVersionAndSetClientVersion( 0, q );
        VersionVector vv2 = ch.getDBVersionForRead( 0 );
        TPCERelationVersionTable rvt = new TPCERelationVersionTable();
        for( String tableName : rvt.getTableNames() ) {
            int relationIndex = TPCEVersionVector.relationIndexes.get( tableName );
            if( tableName.equals( "last_trade" ) || tableName.equals( "customer_account" ) ) {
                assertThat( vv.versionAtIndex( relationIndex ), equalTo( 1L ) );
                assertThat( vv2.versionAtIndex( relationIndex ), equalTo( 1L ) );
            } else {
                assertThat( vv.versionAtIndex( relationIndex ), equalTo( 0L ) );
                assertThat( vv2.versionAtIndex( relationIndex ), equalTo( 0L ) );
            }
        }
    }

    @Test
    public void properMatching() {
        InMemoryCacheHandler ch = new InMemoryCacheHandler( new HashMapCache(), WorkloadType.TPCE );
        List<Map<String,Object>> selResult = new LinkedList<>();
        VersionVector resultVersion = ch.getZeroVersionVector();
        TPCERelationVersionTable rvt = new TPCERelationVersionTable();
        Query q = new Query( "SELECT t FROM last_trade, customer" );
        QueryResult qr = new QueryResult( selResult, resultVersion );
        ch.cacheResult( q.getCacheKey(), q.getTables(), qr );

        VersionVector clientVersion = new VersionVector( resultVersion );
        clientVersion.setTableVersion( TPCEVersionVector.relationIndexes.get( "last_trade" ), 2L );

        List<VersionVector> versions = ch.getActiveVersions( q, q.getTables(), clientVersion, WorkloadType.TPCE );
        assertThat( versions.size(), equalTo( 0 ) );

        clientVersion = new VersionVector( resultVersion );
        clientVersion.setTableVersion( TPCEVersionVector.relationIndexes.get( "customer_account" ), 2L );
        versions = ch.getActiveVersions( q, q.getTables(), clientVersion, WorkloadType.TPCE );
        assertThat( versions.size(), equalTo( 1 ) );

    }
}
