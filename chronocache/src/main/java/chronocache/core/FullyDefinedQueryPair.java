package chronocache.core;

import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryStream;

import com.google.common.collect.Multimap;

public class FullyDefinedQueryPair {
	private ExecutedQuery q1;
	private Query q2;
	private Multimap<Integer,String> mappings;

	public FullyDefinedQueryPair( ExecutedQuery q1, Query q2, Multimap<Integer,String> mappings ){
		this.q1 = q1;	
		this.q2 = q2;
		this.mappings = mappings;
	}

	public String getFirstQuery() {
		return q1.getParseTree().toString();
	}
	
	public Query getSecondQueryShell() {
		return q2;	
	}
	public Multimap<Integer,String> getMappings() {
		return mappings;
	}
}
