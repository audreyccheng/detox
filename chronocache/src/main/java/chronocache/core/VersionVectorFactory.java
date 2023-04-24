package chronocache.core;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.StringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import chronocache.core.qry.QueryResult;
import chronocache.core.qry.Query;

public class VersionVectorFactory {
	private WorkloadType wl;
	private RelationVersionTable relationVersionTable;

	private Logger logger = LoggerFactory.getLogger( VersionVectorFactory.class );

	/**
	 * Initialize a version vector factory for the provided workload
	 */
	public VersionVectorFactory( WorkloadType wl ){
		this.wl = wl;
		relationVersionTable = getRelationVersionTable();
	}

	/**
	 * Create a version vector at the current time for this workload
	 */
	public VersionVector createVersionVector() {
		if( wl == WorkloadType.TPCW ) {
			return new TPCWVersionVector();
		} else if( wl == WorkloadType.TPCC ) {
			return new TPCCVersionVector();
		} else if( wl == WorkloadType.TPCE ) {
			return new TPCEVersionVector();
		} else if( wl == WorkloadType.WIKI ) {
			return new WIKIVersionVector();
		} else if( wl == WorkloadType.SEATS ) {
			return new SEATSVersionVector();
		} else if (wl == WorkloadType.SMALLBANK) {
			return new SmallBankVersionVector();
		} else if (wl == WorkloadType.TAOBENCH) {
			return new TaobenchVersionVector();
		} else if (wl == WorkloadType.EPINIONS) {
			return new EpinionsVersionVector();
		} else {
			return new AuctionMarkVersionVector();
		}
	}

	/**
	 * Create special DB version vector for this workload
	 */
	public VersionVector createDBVersionVector() {
		int numRelations;
		if( wl == WorkloadType.TPCW ) {
			numRelations = TPCWVersionVector.relationIndexes.size();
		} else if( wl == WorkloadType.TPCC ) {
			numRelations = TPCCVersionVector.relationIndexes.size();
		} else if( wl == WorkloadType.TPCE ) {
			numRelations = TPCEVersionVector.relationIndexes.size();
		} else if( wl == WorkloadType.WIKI ) {
			numRelations = WIKIVersionVector.relationIndexes.size();
		} else if( wl == WorkloadType.SEATS ) {
			numRelations = SEATSVersionVector.relationIndexes.size();
		} else if ( wl == WorkloadType.SMALLBANK) {
			numRelations = SmallBankVersionVector.relationIndexes.size();
		} else if ( wl == WorkloadType.TAOBENCH) {
			numRelations = TaobenchVersionVector.relationIndexes.size();
		} else if ( wl == WorkloadType.EPINIONS) {
			numRelations = EpinionsVersionVector.relationIndexes.size();
		} else {
			numRelations = AuctionMarkVersionVector.relationIndexes.size();
		}
		ArrayList<Long> dbVersionArr = new ArrayList<Long>( Collections.nCopies( numRelations, -1L ) );
		return new VersionVector( dbVersionArr );
	}

	public Integer getRelationIndexBinding( String relation ) {
		if( wl == WorkloadType.TPCW ) {
			return TPCWVersionVector.relationIndexes.get( relation );
		} else if( wl == WorkloadType.TPCC ) {
			return TPCCVersionVector.relationIndexes.get( relation );
		} else if( wl == WorkloadType.TPCE ) {
			return TPCEVersionVector.relationIndexes.get( relation );
		} else if( wl == WorkloadType.WIKI ) {
			return WIKIVersionVector.relationIndexes.get( relation );
		} else if( wl == WorkloadType.SEATS ) {
			return SEATSVersionVector.relationIndexes.get( relation );
		} else if( wl == WorkloadType.SMALLBANK ) {
			return SmallBankVersionVector.relationIndexes.get( relation );
		} else if( wl == WorkloadType.TAOBENCH ) {
			return TaobenchVersionVector.relationIndexes.get( relation );
		} else if( wl == WorkloadType.EPINIONS ) {
			return EpinionsVersionVector.relationIndexes.get( relation );
		} else {
			return AuctionMarkVersionVector.relationIndexes.get( relation );
		}
	}

	/**
	 * Generate strings to create the cassandra schema for the workload
	 */
	public List<String> getSchemaCreationStrings() {
		String[] indexToRelation;
		if( wl == WorkloadType.TPCW ) {
			indexToRelation = TPCWVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TPCC ) {
			indexToRelation = TPCCVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TPCE ) {
			indexToRelation = TPCEVersionVector.indexToRelation;
		} else if( wl == WorkloadType.WIKI ) {
			indexToRelation = WIKIVersionVector.indexToRelation;
		} else if( wl == WorkloadType.SEATS ) {
			indexToRelation = SEATSVersionVector.indexToRelation;
		} else if( wl == WorkloadType.SMALLBANK ) {
			indexToRelation = SmallBankVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TAOBENCH ) {
			indexToRelation = TaobenchVersionVector.indexToRelation;
		} else if( wl == WorkloadType.EPINIONS ) {
			indexToRelation = EpinionsVersionVector.indexToRelation;
		} else {
			indexToRelation = AuctionMarkVersionVector.indexToRelation;
		}

		StringBuilder sb = new StringBuilder();
		sb.append( "CREATE TABLE cached_keys( k text" );
		for( int i = 0; i < indexToRelation.length; i++ ) {
			sb.append( ", " );
			sb.append( indexToRelation[ i ] );
			sb.append( " bigint" );
		}
		sb.append( ", PRIMARY KEY( k ) )" );

		List<String> schemaStrs = new LinkedList<>();
		schemaStrs.add( sb.toString() );

		schemaStrs.addAll( relationVersionTable.getSchemaCreationStrings() );

		return schemaStrs;
	}

	/**
	 * Generate strings to delete cassandra schema for the workload
	 */
	public List<String> getSchemaDestructionStrings() {
		List<String> schemaStrs = new LinkedList<>();
		schemaStrs.add( "DROP TABLE IF EXISTS cached_keys" );
		schemaStrs.addAll( relationVersionTable.getSchemaDestructionStrings() );
		return schemaStrs;
	}

	/**
	 * Generate a lookup query for all versions of the cacheKey >= clientVersion
	 */
	public String generateLookupQuery( Query query, VersionVector clientVersion ) {
		StringBuilder sb = new StringBuilder();
		Map<String,Integer> relationIndexes;
		if( wl == WorkloadType.TPCW ) {
			relationIndexes = TPCWVersionVector.relationIndexes;
		} else if(wl == WorkloadType.TPCC ) {
			relationIndexes = TPCCVersionVector.relationIndexes;
		} else if( wl == WorkloadType.TPCE ) {
			relationIndexes = TPCEVersionVector.relationIndexes;
		} else if( wl == WorkloadType.WIKI ) {
			relationIndexes = WIKIVersionVector.relationIndexes;
		} else if( wl == WorkloadType.SEATS ) {
			relationIndexes = SEATSVersionVector.relationIndexes;
		} else {
			relationIndexes = AuctionMarkVersionVector.relationIndexes;
		}

		sb.append( "SELECT * FROM cached_keys WHERE k = '" );
		sb.append( query.getCacheKey() );
		sb.append( "'");

		for( String rel : query.getTables() ) {
			Integer indexBinding = getRelationIndexBinding( rel );
			// Skip unrecognized tables which might appear in a Maria CTE
			if (indexBinding == null) {
				continue;
			}
			sb.append( " AND " );
			sb.append( rel );
			sb.append( " >= " );
			sb.append( clientVersion.versionAtIndex( indexBinding ) );
		}
		sb.append( " ALLOW FILTERING");
		String cQuery = sb.toString();
		logger.trace( "Generated CQL query: {}", cQuery );
		return cQuery;
	}

	/**
	 * Delete all timestamps for the key with a version <= staleVersion
	 */
	public String generateDeleteStaleVersionsQuery( String cacheKey, VersionVector staleVersion ) {
		StringBuilder sb = new StringBuilder();
		Map<String,Integer> relationIndexes;
		if( wl == WorkloadType.TPCW ) {
			relationIndexes = TPCWVersionVector.relationIndexes;
		} else if( wl == WorkloadType.TPCC ) {
			relationIndexes = TPCCVersionVector.relationIndexes;
		} else if( wl == WorkloadType.TPCE ) {
			relationIndexes = TPCEVersionVector.relationIndexes;
		} else if( wl == WorkloadType.WIKI ) {
			relationIndexes = WIKIVersionVector.relationIndexes;
		} else if( wl == WorkloadType.SEATS ) {
			relationIndexes = SEATSVersionVector.relationIndexes;
		} else if (wl == WorkloadType.SMALLBANK ) {
			relationIndexes = SmallBankVersionVector.relationIndexes;
		} else if (wl == WorkloadType.TAOBENCH ) {
			relationIndexes = TaobenchVersionVector.relationIndexes;
		} else if (wl == WorkloadType.EPINIONS ) {
			relationIndexes = EpinionsVersionVector.relationIndexes;
		} else {
			relationIndexes = AuctionMarkVersionVector.relationIndexes;
		}

		sb.append( "DELETE * FROM cached_keys WHERE k = '" );
		sb.append( cacheKey );
		sb.append( "'" );
		for( String rel : relationIndexes.keySet() ) {
			sb.append( " AND " );
			sb.append( rel );
			sb.append( " <= " );
			sb.append( staleVersion.versionAtIndex( getRelationIndexBinding( rel ) ) );
		}
		return sb.toString();
	}

	/**
	 * Generate a query to store the current version for the cacheKey in cassandra
	 */
	public String generateRecordVersionQuery( String cacheKey, VersionVector resultVersion ) { 
		StringBuilder sb = new StringBuilder();
		Map<String,Integer> relationIndexes;
		String[] indexToRelation;
		if( wl == WorkloadType.TPCW ) {
			relationIndexes = TPCWVersionVector.relationIndexes;
			indexToRelation = TPCWVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TPCC ) {
			relationIndexes = TPCCVersionVector.relationIndexes;
			indexToRelation = TPCCVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TPCE ) {
			relationIndexes = TPCEVersionVector.relationIndexes;
			indexToRelation = TPCEVersionVector.indexToRelation;
		} else if( wl == WorkloadType.WIKI ) {
			relationIndexes = WIKIVersionVector.relationIndexes;
			indexToRelation = WIKIVersionVector.indexToRelation;
		} else if( wl == WorkloadType.SEATS ) {
			relationIndexes = SEATSVersionVector.relationIndexes;
			indexToRelation = SEATSVersionVector.indexToRelation;
		} else if( wl == WorkloadType.SMALLBANK ) {
			relationIndexes = SmallBankVersionVector.relationIndexes;
			indexToRelation = SmallBankVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TAOBENCH ) {
			relationIndexes = TaobenchVersionVector.relationIndexes;
			indexToRelation = TaobenchVersionVector.indexToRelation;
		} else if( wl == WorkloadType.EPINIONS ) {
			relationIndexes = EpinionsVersionVector.relationIndexes;
			indexToRelation = EpinionsVersionVector.indexToRelation;
		} else {
			relationIndexes = AuctionMarkVersionVector.relationIndexes;
			indexToRelation = AuctionMarkVersionVector.indexToRelation;
		}

		sb.append( "INSERT INTO cached_keys( k" );
		for( int i = 0; i < indexToRelation.length; i++ ) {
			sb.append( ", " );
			sb.append( indexToRelation[ i ] );
		}
		sb.append( " ) VALUES ( '" );
		sb.append( cacheKey );
			sb.append( "'" );
		for( int i = 0 ; i < indexToRelation.length; i++ ) {
			sb.append( ", ");
			sb.append( resultVersion.versionAtIndex( i ) );
		}
		sb.append( " )" );
		return sb.toString();
	}

	public Set<Integer> getTableIndexes( Set<String> tables ) {
		Map<String,Integer> relationIndexes;
		if( wl == WorkloadType.TPCW ) {
			relationIndexes = TPCWVersionVector.relationIndexes;
		} else if( wl == WorkloadType.TPCC ) {
			relationIndexes = TPCCVersionVector.relationIndexes;
		} else if( wl == WorkloadType.TPCE ) {
			relationIndexes = TPCEVersionVector.relationIndexes;
		} else if( wl == WorkloadType.WIKI ) {
			relationIndexes = WIKIVersionVector.relationIndexes;
		} else if( wl == WorkloadType.SEATS ) {
			relationIndexes = SEATSVersionVector.relationIndexes;
		} else if( wl == WorkloadType.SMALLBANK ) {
			relationIndexes = SmallBankVersionVector.relationIndexes;
		} else if( wl == WorkloadType.TAOBENCH ) {
			relationIndexes = TaobenchVersionVector.relationIndexes;
		} else if( wl == WorkloadType.EPINIONS ) {
			relationIndexes = TaobenchVersionVector.relationIndexes;
		} else {
			relationIndexes = AuctionMarkVersionVector.relationIndexes;
		}

		Set<Integer> tableIndexes = new HashSet<>();
		for( String table : tables ) {
			tableIndexes.add( relationIndexes.get( table ) );
		}
		return tableIndexes;

	}

	public Set<Integer> getTableIndexes( Query query ) {
		return getTableIndexes( query.getTables() );
	}

	/**
	 * Take rows returned by Cassandra and convert them into an ordered list of version vectors
	 */
	public static List<VersionVector> cassandraResultToVV( List<Row> result, VersionVector clientVersion, WorkloadType wl ) {
		List<VersionVector> versionVectors = new LinkedList<>();
		Map<String,Integer> relationIndexes;
		String[] indexToRelation;
		if( wl == WorkloadType.TPCW ) {
			relationIndexes = TPCWVersionVector.relationIndexes;
			indexToRelation = TPCWVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TPCC ) {
			relationIndexes = TPCCVersionVector.relationIndexes;
			indexToRelation = TPCCVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TPCE ) {
			relationIndexes = TPCEVersionVector.relationIndexes;
			indexToRelation = TPCEVersionVector.indexToRelation;
		} else if( wl == WorkloadType.WIKI ) {
			relationIndexes = WIKIVersionVector.relationIndexes;
			indexToRelation = WIKIVersionVector.indexToRelation;
		} else if( wl == WorkloadType.SEATS ) {
			relationIndexes = SEATSVersionVector.relationIndexes;
			indexToRelation = SEATSVersionVector.indexToRelation;
		} else if( wl == WorkloadType.SMALLBANK ) {
			relationIndexes = SmallBankVersionVector.relationIndexes;
			indexToRelation = SmallBankVersionVector.indexToRelation;
		} else if( wl == WorkloadType.TAOBENCH ) {
			relationIndexes = TaobenchVersionVector.relationIndexes;
			indexToRelation = TaobenchVersionVector.indexToRelation;
		} else if( wl == WorkloadType.EPINIONS ) {
			relationIndexes = EpinionsVersionVector.relationIndexes;
			indexToRelation = EpinionsVersionVector.indexToRelation;
		} else {
			relationIndexes = AuctionMarkVersionVector.relationIndexes;
			indexToRelation = AuctionMarkVersionVector.indexToRelation;

		}
		for( Row row : result ) {
			ArrayList<Long> versions = new ArrayList<>( relationIndexes.size() );
			for( int i = 0; i < relationIndexes.size(); i++ ) {
				versions.add( row.getLong( indexToRelation[ i ] ) );
			}
			VersionVector vv = new VersionVector( versions );
			versionVectors.add( vv );
		}

		sortByMagnitudeOfDeflection( versionVectors, clientVersion );
		return versionVectors;
	}

	/**
	 * Sort version vectors by the magnitude of their deflection relative to some base vector
	 */
	public static void sortByMagnitudeOfDeflection( List<VersionVector> versionVectors, VersionVector clientVersion ) {
		Comparator<VersionVector> versionComparator = new Comparator<VersionVector>() {
			public int compare( VersionVector v1, VersionVector v2 ) {
				VersionVector.DeflectionCalculation firstDeflection = v1.deflectionFrom( clientVersion );
				VersionVector.DeflectionCalculation secondDeflection = v2.deflectionFrom( clientVersion );
				if( firstDeflection.totalDeflection == secondDeflection.totalDeflection ) {
					return (int) (firstDeflection.numDeflectingIndexes - secondDeflection.numDeflectingIndexes);
				}
				return (int) (firstDeflection.totalDeflection - secondDeflection.totalDeflection);
			}
		};

		Collections.sort( versionVectors, versionComparator );
	}

	public RelationVersionTable getRelationVersionTable() {
		if( wl == WorkloadType.TPCW ) {
			return new TPCWRelationVersionTable();
		} else if( wl == WorkloadType.TPCC ) {
		 	return new TPCCRelationVersionTable();
		} else if( wl == WorkloadType.TPCE ) {
			return new TPCERelationVersionTable();
		} else if( wl == WorkloadType.WIKI ) {
			return new WIKIRelationVersionTable();
		} else if( wl == WorkloadType.SEATS ) {
			return new SEATSRelationVersionTable();
		} else if( wl == WorkloadType.SMALLBANK ) {
			return new SmallBankRelationVersionTable();
		} else if( wl == WorkloadType.TAOBENCH ) {
			return new TaobenchRelationVersionTable();
		} else if( wl == WorkloadType.EPINIONS ) {
			return new EpinionsRelationVersionTable();
		} else {
			return new AuctionMarkRelationVersionTable();
		}
	}
}
