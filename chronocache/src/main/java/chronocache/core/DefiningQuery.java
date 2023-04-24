package chronocache.core;

import java.util.List;
import java.util.Map;
import com.google.common.collect.Multimap;

import chronocache.core.qry.QueryIdentifier;

/**
 * A Defining Query for an FDQ
 *
 * A defining query is either an FDQ itself, or a DependentQuery
 */
public abstract class DefiningQuery {

	protected QueryIdentifier qid;

	/**
	 * Get the query identifier for this DefiningQuery
	 */
	public QueryIdentifier getId() {
		return qid;
	}

	@Override
	public String toString() {
		return qid.toString();
	}
}
