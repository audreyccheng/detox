package chronocache.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap; 
import chronocache.core.Parameters;
import chronocache.core.qry.QueryIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class used to handle tracking histories and merge them.
 * Decides when a tracking history is ready for use in speculative execution
 * @author bjglasbe
 *
 */
public class MappingManager {

	private Map<QueryIdentifier, Multimap<Integer, String>> queryMappings;
	private Map<QueryIdentifier, Integer> strides;
	private Map<QueryIdentifier, Integer> lastMappingRow;
	private Map<QueryIdentifier, Long> numTimesCounted;
	private Logger logger;

	public MappingManager() {
		queryMappings = new HashMap<>();
		strides = new HashMap<>();
		lastMappingRow = new HashMap<>();
		numTimesCounted = new HashMap<>();
		logger = LoggerFactory.getLogger( this.getClass() );
	}

	/**
	 * Add the following queryPairings (column names of result set to input parameters for query id)
	 * for the provided query id
	 */
	public void addToHistory( QueryIdentifier id, Multimap<Integer, String> queryPairings, int mappingRowNum ) {
		Multimap<Integer, String> pairingsCopy = HashMultimap.create( queryPairings );
		synchronized( this ){
			if ( !queryMappings.containsKey( id ) ) {
				queryMappings.put( id, pairingsCopy );
				strides.put( id, 0 );
				lastMappingRow.put( id, mappingRowNum );
				numTimesCounted.put( id, 1L );
			} else {
				// Intersection
				intersection( id, pairingsCopy );
				numTimesCounted.put( id, numTimesCounted.get( id ) + 1 );
				strides.put( id, mappingRowNum - lastMappingRow.get( id ) );
				logger.trace( "stride to {} updated to {}.", id, strides.get( id ) );
				lastMappingRow.put( id, mappingRowNum );
			}
		}
	}

	/**
	 * Merge the query pairing with existing history
	 * @param id
	 * @param pairings2
	 */
	private void intersection( QueryIdentifier id, Multimap<Integer, String> otherPairings ) {
		Multimap<Integer, String> mappings = queryMappings.get( id );
		if( mappings == null ) {
			return;
		}
		Multimap<Integer, String> newMappings = HashMultimap.create();
		Iterator<Integer> myIter = mappings.keySet().iterator();

		while( myIter.hasNext() ) {
			int colId = myIter.next();
			Collection<String> theirStrings = otherPairings.get( colId );
			if( !theirStrings.isEmpty() ) {
				Collection<String> myStrings = mappings.get( colId );
				Iterator<String> myStringIterator = myStrings.iterator();
				while( myStringIterator.hasNext() ) {
					String myStr = myStringIterator.next();
					if( theirStrings.contains( myStr ) ) {
						newMappings.put( colId, myStr );
					}
				}
			}
		}

		queryMappings.put( id, newMappings );
		logger.trace( "After intersection, mappings for {} are: {}", id, mappings );
	}

	/**
	 * If the mappings for a query have been seen enough times, then return it
	 * @param id
	 * @return
	 */
	public Multimap<Integer, String> getMappingsIfReady( QueryIdentifier id ) {
		synchronized( this ){
			if( !queryMappings.containsKey( id ) || numTimesCounted.get( id ) < Parameters.TRACKING_PERIOD ) {
				return null;
			}
			return HashMultimap.create( queryMappings.get( id ) );
		}
	}

	/**
	 * Give back the stride/delta for how the mapping from id is consuming the data
	 * It essentially is the 'rate' at which id is using our data
	 */
	public int getMappingStride( QueryIdentifier id ) {
		return strides.get( id );
	}
}
