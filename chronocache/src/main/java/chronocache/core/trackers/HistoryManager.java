package chronocache.core.trackers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import chronocache.core.Parameters;
import chronocache.core.qry.QueryIdentifier;

/**
 * A class used to handle tracking histories and merge them.
 * Decides when a tracking history is ready for use in speculative execution
 * @author bjglasbe
 *
 */
public class HistoryManager {

	private Map<QueryIdentifier, Map<QueryIdentifier, Multimap<Integer, String>>> queryMappings;
	private Map<QueryIdentifier, Integer> numTimesCounted;

	public HistoryManager() {
		queryMappings = new HashMap<>();
		numTimesCounted = new HashMap<>();
	}

	/**
	 * Add the following queryPairings (column names of result set to input parameters for query id)
	 * for the provided query id
	 * @param id
	 * @param queryPairings
	 */
	public void addToHistory(QueryIdentifier id, Map<QueryIdentifier, Multimap<Integer, String>> queryPairings) {
		synchronized( this ){
			if (!queryMappings.containsKey(id)) {
				queryMappings.put(id, queryPairings);
				numTimesCounted.put(id, 1);
			} else {
				// Intersection
				intersection(id, queryPairings);
				numTimesCounted.put(id, numTimesCounted.get(id).intValue() + 1);
			}
		}

	}

	/**
	 * Merge the query pairing with existing history
	 * @param id
	 * @param pairings2
	 */
	private void intersection(QueryIdentifier id, Map<QueryIdentifier, Multimap<Integer, String>> pairings2) {
		Map<QueryIdentifier, Multimap<Integer, String>> myMappings = queryMappings.get(id);
		Iterator<QueryIdentifier> iter = myMappings.keySet().iterator();
		while (iter.hasNext()) {
			QueryIdentifier qid = iter.next();
			if (!pairings2.containsKey(qid)) {
				iter.remove();
			}
			else{
				Multimap<Integer, String>  inputOutput2 = pairings2.get(qid);
				Multimap<Integer, String>  inputOutput = myMappings.get(qid);
				Iterator<Integer>iter1 = inputOutput.keySet().iterator();
				while(iter1.hasNext()){
					int Iid = iter1.next();
					Collection<String> Str2 = inputOutput2.get(Iid);
					if (!Str2.isEmpty()) {
						Collection<String> Str1 = inputOutput.get(Iid);
						Iterator<String> iter2 = Str1.iterator();
						while(iter2.hasNext()){
							String str=iter2.next();
							if (!Str2.contains(str)) {
								iter2.remove();
							}
						}
					}
					else{
						iter1.remove();
					}
				}
			}
			//TODO: remove stuff in myMappings QID not in pairings QID
		}
		queryMappings.put(id, myMappings);
	}

	/**
	 * If the mappings for a query have been seen enough times, then return it
	 * @param id
	 * @return
	 */
	public Map<QueryIdentifier, Multimap<Integer, String>> getMappingsIfReady(QueryIdentifier id) {
		synchronized( this ){
			if (!queryMappings.containsKey(id) || numTimesCounted.get(id) < Parameters.TRACKING_PERIOD) {
				return null;
			}
			return queryMappings.get(id);
		}
	}

	/**
	 * Blacklist a set of related query ids so they are no longer provided by getMappingsIfReady()
	 * @param id
	 * @param relatedQueryId
	 */
	public void blacklist(QueryIdentifier id, List<QueryIdentifier> relatedQueryIds) {
		 Map<QueryIdentifier, Multimap<Integer, String>> relatedQueryMappings = queryMappings.get(id);
		 for( QueryIdentifier qid : relatedQueryIds){
			 relatedQueryMappings.remove(qid);
		 }
	}
}
