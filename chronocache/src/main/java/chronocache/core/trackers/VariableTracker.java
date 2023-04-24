package chronocache.core.trackers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.TrackingQuery;
import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;;


/**
 * A class designed to track mappings from a base query result set to related
 * query parameters
 * 
 * @author bjglasbe
 *
 */
public class VariableTracker {

	private Map<QueryIdentifier, TrackingQuery> trackQueryBasePoint;
	private Multimap<QueryIdentifier, QueryIdentifier> recordQueryMap;
	private HistoryManager historyManager;
	private ShellParamSkewCalculator shellSkew;
	
	private static Logger log = LoggerFactory.getLogger(VariableTracker.class);

	public VariableTracker() {
		trackQueryBasePoint = new HashMap<QueryIdentifier, TrackingQuery>();
		recordQueryMap = ArrayListMultimap.create();
		historyManager = new HistoryManager();
		shellSkew = new ShellParamSkewCalculator();
		log = LoggerFactory.getLogger(this.getClass());
	}

	/**
	 * Return a boolean indicating whether we are tracking this query id or not
	 * @param id
	 * @return
	 */
	public boolean areTrackingQuery(QueryIdentifier id) {
		synchronized( this ){
			return trackQueryBasePoint.containsKey(id);
		}
	}

	/**
	 * Start tracking all read-only related queries for the provided executed query
	 * @param q
	 * @param relatedReadQueries
	 * @param knownQueryShells
	 */
	public void startTracking(ExecutedQuery q, List<QueryIdentifier> relatedReadQueries) {
		synchronized( this ){
            log.trace("Tracking {}, output params {}", q.getId().getId(), q.getResults().getSelectResult().get(0));
			TrackingQuery trackQuery = new TrackingQuery(q, relatedReadQueries);
			trackQueryBasePoint.put(q.getId(), trackQuery);
			for (QueryIdentifier id : relatedReadQueries) {
				recordQueryMap.put(id, q.getId());
			}
		}
	}

	/**
	 * Stop tracking all related read-only queries for the 
	 * @param q
	 */
	public void doneTracking(Query q) {
		synchronized( this ){
			TrackingQuery tq = trackQueryBasePoint.remove(q.getId());
			if( tq != null ) {
				for (QueryIdentifier id : tq.getRelatedQueries()) {
					recordQueryMap.remove(id,q.getId());
				}
				Map<QueryIdentifier, Multimap<Integer, String>> queryPairings = tq.getOutputToInputMapping();
				historyManager.addToHistory(q.getId(), queryPairings);
			} else {
				log.warn("Missing tracking information for query we are supposedly tracking... {}", q.getId().getId());
			}
		}
		

	}

	/**
	 * Record the variables for the provided query
	 * @param q
	 */
	public void recordVariables(Query q) {
		synchronized( this ){
			Collection<QueryIdentifier> trackOwnerId = recordQueryMap.get(q.getId());
			for ( QueryIdentifier id: trackOwnerId ){
				TrackingQuery tq = trackQueryBasePoint.get(id);
                log.trace("Recording \"{}\" for base query {}", q.getParams(), id.getId() );
				if (tq != null) {
					tq.recordVariables(q);
				} else {
					log.warn("Could not find base tracking query for id {}", id.getId());
				}
			}
		}
	}

	/**
	 * Return a boolean indicating whether we are looking to record variables for this query id
	 * @param id
	 * @return
	 */
	public boolean areRecordingQuery(QueryIdentifier id) {
		synchronized( this ){
			return recordQueryMap.containsKey(id);
		}
	}

	/**
	 * Return a set of mappings from this query id's outputs  to its related read only queries' inputs
	 * @param id
	 * @return
	 */
	public Map<QueryIdentifier, Multimap<Integer, String>> getMappingsIfReady(QueryIdentifier id) {
		synchronized( this ){
			return historyManager.getMappingsIfReady(id);
		}
	}

	public void blacklist(QueryIdentifier id, List<QueryIdentifier> relatedQueryIds) {
		synchronized( this ){
			historyManager.blacklist(id, relatedQueryIds);
		}
	}
}
