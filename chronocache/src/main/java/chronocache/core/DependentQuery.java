package chronocache.core;

import java.util.List;
import java.util.Map;
import com.google.common.collect.Multimap;

import chronocache.core.qry.QueryIdentifier;

/**
 * A DependentQuery for an FDQ
 *
 * This query is not fully defined, but represents a dependency that
 * must be satisfied before the FDQ can execute.
 */
public class DependentQuery extends DefiningQuery {

	public DependentQuery( QueryIdentifier qid ) {
		this.qid = qid;
	}

}
