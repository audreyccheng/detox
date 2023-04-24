package chronocache.core.qry;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * A query representation we use for tracking purposes
 * 
 * @author bjglasbe
 *
 */
public class TrackingQuery extends Query {

	private Set<QueryIdentifier> relatedQueries;
	private Map<QueryIdentifier, List<String>> relatedQueryParams;
	private List<Map<String, Object>> resultSet;
	private Logger log;

	/**
	 * Construct a tracking query an executed *base* query and a list of related
	 * query ids
	 * 
	 * @param q
	 * @param relatedQueries
	 */
	public TrackingQuery(ExecutedQuery q, Collection<QueryIdentifier> relatedQueries) {
		super(q);
		this.relatedQueries = new HashSet<QueryIdentifier>(relatedQueries);
		relatedQueryParams = new HashMap<QueryIdentifier, List<String>>();
		this.resultSet = q.getResults().getSelectResult();
		log = LoggerFactory.getLogger(this.getClass());
	}

	/**
	 * Get the queries we believe are related to the base one
	 * 
	 * @return
	 */
	public Set<QueryIdentifier> getRelatedQueries() {
		return relatedQueries;
	}

	/**
	 * Record the input variables of a related query
	 * 
	 * @param q
	 */
	public void recordVariables(Query q) {
		// log.trace("Recording variables {} for query {} for base {}",
		// q.getParams().toString(), q.getId().getId(), super.getId().getId());
		relatedQueryParams.put(q.getId(), q.getParams());
	}

	/**
	 * Generates the input output mapping of related queries.
	 * 
	 * @return Empty Map if the resultSet of the query is empty <b> otherwise,
	 *         Map of Column names in result set and input order in related
	 *         query based on value match
	 */
	public Map<QueryIdentifier, Multimap<Integer, String>> getOutputToInputMapping() {
		// TODO: extend to handle multiple rows
		Map<QueryIdentifier, Multimap<Integer, String>> queryPairings = new HashMap<>();
		// log.trace("Constructing output to input mapping for QID {}",
		// this.getId().getId());

		for (QueryIdentifier id : relatedQueries) {
			List<String> qryParams = relatedQueryParams.get(id);
			Multimap<Integer, String> pairings = ArrayListMultimap.create();

			if (qryParams != null && this.resultSet.size() != 0) {
				for (int i = 0; i < qryParams.size(); i++) {
					for (Entry<String, Object> entry : this.resultSet.get(0).entrySet()) {

						String params = qryParams.get(i);
						Object value = entry.getValue();

						if (value != null && (params.equals(value.toString())
								// TODO we call Object.toString() here which is
								// actually the object of ResultSet
								|| params.equals("'" + value.toString() + "'"))) {
							// log.trace("Related Query {} mapping from column
							// {} to input number {}", getId().getId(),
							// entry.getKey(), i);
							pairings.put(i, entry.getKey());
							break;
						}
					}
				}
			}
			queryPairings.put(id, pairings);
		}
		return queryPairings;
	}

}
