package chronocache.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents a client's version, with each index representing the version of a relation
 */
public class VersionVector {

	protected ArrayList<Long> relationVersions;

	public class DeflectionCalculation {

		public DeflectionCalculation() {
			totalDeflection = 0;
			numDeflectingIndexes = 0;
		}

		public long totalDeflection;
		public long numDeflectingIndexes;
	}

	/**
	 * Create a version vector from an arraylist of relation versions
	 */
	public VersionVector( ArrayList<Long> versions ) {
		relationVersions = new ArrayList<Long>( versions );
	}

	/**
	 * Create a version vector from a different version vector, copying its relation versions
	 */
	public VersionVector( VersionVector other ) {
		relationVersions = new ArrayList<Long>( other.relationVersions );
	}


	public static VersionVector useCachedVersionForTableIndexes( VersionVector locVersion, VersionVector cacheVersion, Set<Integer> indexesToMax ) {
		ArrayList<Long> outVecArr = new ArrayList<Long>( locVersion.relationVersions.size() );
		for( int i = 0; i < locVersion.relationVersions.size(); i++ ) {
			if( indexesToMax.contains( i ) ) {
				Long cacheVersionInd = cacheVersion.relationVersions.get( i );
				outVecArr.add( cacheVersionInd );
			} else {
				Long locVersionInd = locVersion.relationVersions.get( i );
				outVecArr.add( locVersionInd );
			}
		}
		return new VersionVector( outVecArr );
	}

	/**
	 * Determine if this version vector is considered 'newer' than the provided version vector
	 * N.B. if version vectors have indexes where one index is greater in this vector and another index
	 * is greater than the provided one, we will still return true, even though this is technically incorrect
	 */
	public boolean isNewerThan( VersionVector other ) { 
		Iterator<Long> myVersions = relationVersions.iterator();
		Iterator<Long> theirVersions = other.relationVersions.iterator();
		while( myVersions.hasNext() ) {
			if( myVersions.next() > theirVersions.next() ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if this version vector represents the same version as the provided version vector
	 * N.B. it is assumed they are the same length
	 */
	public boolean isSameVersion( VersionVector other ) {
		Iterator<Long> myVersions = relationVersions.iterator();
		Iterator<Long> theirVersions = other.relationVersions.iterator();
		while( myVersions.hasNext() ) {
			Long theirVersion = theirVersions.next();
			Long myVersion = myVersions.next();
			if( !theirVersion.equals( myVersion ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get function to underlying array version
	 */
	public Long versionAtIndex( int index ) {
		return relationVersions.get( index );
	}

	public void setTableVersion( int tableIndex, Long ts ) {
		relationVersions.set( tableIndex, ts );
	}

	/**
	 * Determine the magnitude of the difference between the provided base vector and this vector
	 * Throws IllegalArgumentException if they are different sizes or the base version is not older than this vector
	 */
	public DeflectionCalculation deflectionFrom( VersionVector base ) {
		if( base.relationVersions.size() != relationVersions.size() ) {
			throw new IllegalArgumentException( "Invalid deflection calculation, version vectors are different sizes!");
		}
		Iterator<Long> v = relationVersions.iterator();
		Iterator<Long> bv = base.relationVersions.iterator();
		DeflectionCalculation dc = new DeflectionCalculation();
		while( bv.hasNext() ) {
			Long baseVersion = bv.next();
			Long thisVersion = v.next();
			if( baseVersion > thisVersion ) {
				throw new IllegalArgumentException( "Not a valid deflection calculation, base version has index in future!" );
			}
			if( baseVersion != thisVersion ) {
				dc.totalDeflection += thisVersion - baseVersion;
				dc.numDeflectingIndexes++;
			}
		}
		return dc;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<Long> iter = relationVersions.iterator();
		if( iter.hasNext() ) {
			sb.append( Long.toString( iter.next() ) );
		}
		while( iter.hasNext() ) {
			sb.append( "_" );
			sb.append( Long.toString( iter.next() ) );
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return relationVersions.hashCode();
	}

	@Override
	public boolean equals( Object other ) {
		if( other instanceof VersionVector ) {
			return isSameVersion( (VersionVector) other );
		}
		return false;
	}

	public static VersionVector maxAllIndexes( VersionVector vec1, VersionVector vec2 ) {
		assert( vec1.relationVersions.size() == vec2.relationVersions.size() );
		ArrayList<Long> newVals = new ArrayList<>( vec1.relationVersions.size() );
		ArrayList<Long> vec1Vals = vec1.relationVersions;
		ArrayList<Long> vec2Vals = vec2.relationVersions;
		for( int i = 0; i < vec1.relationVersions.size(); i++ ) {
			Long vec1Val = vec1Vals.get( i );
			Long vec2Val = vec2Vals.get( i );
			Long maxVal = vec1Val > vec2Val ? vec1Val : vec2Val;
			newVals.add( maxVal );
		}
		return new VersionVector( newVals );
	}

}
