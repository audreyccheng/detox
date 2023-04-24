package chronocache.core.qry;

import java.util.Collection;

/**
 * A class designed to represent a "source" point for query mappings.
 * Defined by a query identifier and a set of resultset column names.
 */
public class QueryOutputs {

	private QueryIdentifier qid;
	private Collection<String> columnNames;

	public QueryOutputs( QueryIdentifier qid, Collection<String> columnNames ) {
		this.qid = qid;
		this.columnNames = columnNames;
	}

	public Collection<String> getColumnNames() {
		return columnNames;
	}

	public QueryIdentifier getQueryId() {
		return qid;
	}
}
