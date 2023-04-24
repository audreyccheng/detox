package chronocache.core;

import java.util.Set;

/**
 * The result of trying to vectorize a dependency graph.
 * Returns a vectorization plan, the vectorized query text, and the accessed tables
 */
public class QueryVectorizerResult {

	QueryVectorizationPlan plan;
	String vectorizedQueryText;
	Set<String> tables;

	public QueryVectorizerResult( QueryVectorizationPlan plan, String vectorizedQueryText, Set<String> tables ) {
		this.plan = plan;
		this.vectorizedQueryText = vectorizedQueryText;
		this.tables = tables;
	}

	public QueryVectorizationPlan getQueryVectorizationPlan() {
		return plan;
	}

	public String getVectorizedQueryText() {
		return vectorizedQueryText;
	}

	public Set<String> getTables() {
		return tables;
	}
}
