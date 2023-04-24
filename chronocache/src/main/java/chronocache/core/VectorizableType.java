package chronocache.core;

public class VectorizableType {
	private boolean isBaseQuery; // our original definition of base query (require input parameters)
	private boolean isAlwaysDefinedQuery; // our original definition of adq
	private boolean isFullyDefinedQuery; // is Fully defined query
	private boolean isLoopBaseQuery;  // a loop query, that requires some sort of parameter, before it can be vectorized

	VectorizableType() {
		isBaseQuery = false;
		isAlwaysDefinedQuery = false;
		isFullyDefinedQuery = false;
		isLoopBaseQuery = false;
	}

	public VectorizableType( VectorizableType t ) {
		isBaseQuery = t.isBaseQuery;
		isAlwaysDefinedQuery = t.isAlwaysDefinedQuery;
		isFullyDefinedQuery = t.isFullyDefinedQuery;
		isLoopBaseQuery = t.isLoopBaseQuery;
	}

	public void markAsADQ() {
		isAlwaysDefinedQuery = true;
		isFullyDefinedQuery = true;
	}
	public void markAsFDQ() {
		isFullyDefinedQuery = true;
	}
	public void markAsBaseQuery() {
		isBaseQuery = true;
	}
	public void markAsLoopBaseQuery() {
		isLoopBaseQuery = true;
		isBaseQuery = true;
	}

	public boolean isADQ( ) {
		return isAlwaysDefinedQuery;
	}

	public boolean isFDQ() {
		return isFullyDefinedQuery;
	}

	public boolean isTriggerQuery() {
		return isBaseQuery && isLoopBaseQuery;
	}

	public static VectorizableType createMergedVectorizableType( VectorizableType v1, VectorizableType v2 ) {
		VectorizableType mergedType = new VectorizableType();

		mergedType.isBaseQuery = v1.isBaseQuery || v2.isBaseQuery;
		// not necessarily true that is always defined or fdq unless both are
		mergedType.isAlwaysDefinedQuery = v1.isAlwaysDefinedQuery && v2.isAlwaysDefinedQuery;
		mergedType.isFullyDefinedQuery = v1.isFullyDefinedQuery && v2.isFullyDefinedQuery;
		mergedType.isLoopBaseQuery = v1.isLoopBaseQuery || v2.isLoopBaseQuery;

		return mergedType;
	}
};
