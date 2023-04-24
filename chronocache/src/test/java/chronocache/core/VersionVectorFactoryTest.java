package chronocache.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

import chronocache.core.qry.Query;

public class VersionVectorFactoryTest {

	@Test
	public void createTPCWVersionVector() {
		VersionVectorFactory vvf = new VersionVectorFactory( WorkloadType.TPCW );
		VersionVector vv = vvf.createVersionVector();
		assertThat( vv.versionAtIndex( 9 ), not( is( 0 ) ) );
		assertThat( vv instanceof TPCWVersionVector, is( true ) );
	}

	@Test
	public void createTPCWCassandraSchema() {
		VersionVectorFactory vvf = new VersionVectorFactory( WorkloadType.TPCW );
		Iterator<String> creationStringsIter = vvf.getSchemaCreationStrings().iterator();
		assertThat( creationStringsIter.next(), equalTo(
			"CREATE TABLE cached_keys( k text, address bigint, author bigint, cc_xacts bigint, " +
			"country bigint, customer bigint, item bigint, order_line bigint, orders bigint, shopping_cart bigint, " +
			"shopping_cart_line bigint, PRIMARY KEY( k ) )" ) );
		assertThat( creationStringsIter.next(), equalTo( 
			"CREATE TABLE relation_versions( name text, version counter, PRIMARY KEY( name ) )" ) );

		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'address'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'author'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'cc_xacts'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'country'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'customer'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'item'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'order_line'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'orders'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'shopping_cart'" ) );
		assertThat( creationStringsIter.next(), equalTo(
			"UPDATE relation_versions SET version = version + 1 WHERE name = 'shopping_cart_line'" ) );
		assertThat( creationStringsIter.hasNext(), is( false ) );

		Iterator<String> deletionStringsIter = vvf.getSchemaDestructionStrings().iterator();

		assertThat( deletionStringsIter.next(), equalTo( "DROP TABLE IF EXISTS cached_keys" ) );
		assertThat( deletionStringsIter.next(), equalTo( "DROP TABLE IF EXISTS relation_versions" ) );
		assertThat( deletionStringsIter.hasNext(), is( false ) );
	}

	@Test
	public void createTPCWLookupQuery() {
		VersionVectorFactory vvf = new VersionVectorFactory( WorkloadType.TPCW );
		//use special DB version, so we know everything is -1
		VersionVector vv = vvf.createDBVersionVector();
		Query q = new Query( "SELECT 1 FROM author" );
		String expString = "SELECT * FROM cached_keys WHERE k = '1049052462' AND author >= -1 ALLOW FILTERING";
		assertThat( vvf.generateLookupQuery( q, vv ), equalTo( expString ) );
	}

	@Test
	public void createTPCWDeleteStaleVersionsQuery() {
		VersionVectorFactory vvf = new VersionVectorFactory( WorkloadType.TPCW );
		String cacheKey = "1234";
		//use special DB version, so we know everything is -1
		VersionVector vv = vvf.createDBVersionVector();
		String expString = "DELETE * FROM cached_keys WHERE k = '1234' AND shopping_cart <= -1 " + 
		"AND country <= -1 AND item <= -1 AND address <= -1 AND author <= -1 AND order_line <= -1 " +
		"AND cc_xacts <= -1 AND orders <= -1 AND shopping_cart_line <= -1 AND customer <= -1";

		assertThat( vvf.generateDeleteStaleVersionsQuery( cacheKey, vv ), equalTo( expString ) );
	}

	@Test
	public void createTPCWRecordVersionQuery() {
		VersionVectorFactory vvf = new VersionVectorFactory( WorkloadType.TPCW );
		String cacheKey = "1234";
		//use special DB version, so we know everything is -1
		VersionVector vv = vvf.createDBVersionVector();
		String expString = "INSERT INTO cached_keys( k, address, author, cc_xacts, " +
		"country, customer, item, order_line, orders, shopping_cart, shopping_cart_line ) VALUES (" +
		" '1234', -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 )";

		assertThat( vvf.generateRecordVersionQuery( cacheKey, vv ), equalTo( expString ) );
	}

	private VersionVector createBaseVersionVector() {
		ArrayList<Long> baseVersionArr = new ArrayList<Long>();
		baseVersionArr.add( 1L );
		baseVersionArr.add( 2L );
		return new VersionVector( baseVersionArr );
	}

	private VersionVector createOneDeflectVersionVector() {
		ArrayList<Long> versionArr = new ArrayList<Long>();
		versionArr.add( 2L );
		versionArr.add( 2L );
		return new VersionVector( versionArr );
	}

	private VersionVector createTwoDeflectOneIndex() {
		ArrayList<Long> versionArr = new ArrayList<Long>();
		versionArr.add( 3L );
		versionArr.add( 2L );
		return new VersionVector( versionArr );
	}

	private VersionVector createTwoDeflectTwoIndex() {
		ArrayList<Long> versionArr = new ArrayList<Long>();
		versionArr.add( 2L );
		versionArr.add( 3L );
		return new VersionVector( versionArr );
	}

	@Test
	public void testSortVersionVectorsByMagnitude() {
		VersionVector baseVersionVector = createBaseVersionVector();
		VersionVector noDeflectVector = createBaseVersionVector();
		VersionVector oneDeflect = createOneDeflectVersionVector();
		VersionVector twoDeflectOneIndex = createTwoDeflectOneIndex();
		VersionVector twoDeflectTwoIndex = createTwoDeflectTwoIndex();
		List<VersionVector> versions = new LinkedList<>();
		versions.add( twoDeflectTwoIndex );
		versions.add( oneDeflect );
		versions.add( noDeflectVector );
		versions.add( twoDeflectOneIndex );

		VersionVectorFactory vvf = new VersionVectorFactory( WorkloadType.TPCW );
		vvf.sortByMagnitudeOfDeflection( versions, baseVersionVector );
		Iterator<VersionVector> iter = versions.iterator();
		assertThat( iter.next(), equalTo( noDeflectVector ) );
		assertThat( iter.next(), equalTo( oneDeflect ) );
		assertThat( iter.next(), equalTo( twoDeflectOneIndex ) );
		assertThat( iter.next(), equalTo( twoDeflectTwoIndex ) );
	}
}
