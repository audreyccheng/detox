package chronocache.core;

import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.ParserPool;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryResult;
import chronocache.core.qry.QueryIdentifier;

import java.lang.Iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

/**
 * Represents the plan we will use to vectorize this set of queries.
 * Iterable so we walk over it.
 */
public class LateralUnionVectorizationPlan extends QueryVectorizationPlan {

	private Logger logger = LoggerFactory.getLogger( this.getClass() );
	private Map<Integer, Integer> numColsAtTopologicalLevels;
	private Map<Integer, Integer> numNonTypeLUMetadataKeysAtTopologicalLevels;
	private Map<QueryIdentifier, Integer> numColsForQueryIds;

	public LateralUnionVectorizationPlan(
		QueryVectorizationPlan plan
	) {
		super( plan.dependencyGraph, plan.queryShells, plan.vectorizationPlan );
		numColsAtTopologicalLevels = new HashMap<>();
		numNonTypeLUMetadataKeysAtTopologicalLevels = new HashMap<>();
		numColsForQueryIds = new HashMap<>();
	}

	public int getNumColsAtTopologicalLevel( int level ) {
		Integer numCols = numColsAtTopologicalLevels.get( level );
		assert numCols != null;
		return numCols;
	}

	public int getNumNonTypeLUMetadataKeysAtTopologicalLevel( int level ) {
		Integer numKeys = numNonTypeLUMetadataKeysAtTopologicalLevels.get( level );
		logger.trace( "Getting numNonTypeLUMetadataKeysAtTopologicalLevels for level {} = {}", level, numKeys );
		assert numKeys != null;
		return numKeys;
	}

	public int getNumColsForQueryId( QueryIdentifier queryId ) {
		Integer numCols = numColsForQueryIds.get( queryId );
		assert numCols != null;
		return numCols;
	}

	public void setNumColsAtTopologicalLevel( int level, int numCols ) {
		numColsAtTopologicalLevels.put( level, numCols );
	}

	public void setNumNonTypeLUMetadataKeysAtTopologicalLevel( int level, int orderKeys ) {
		logger.trace( "Setting numNonTypeLUMetadataKeysAtTopologicalLevels {} orderkeys = {}", level, orderKeys );
		numNonTypeLUMetadataKeysAtTopologicalLevels.put( level, orderKeys );
	}

	public void setNumColsForQueryId( QueryIdentifier queryId, int numCols ) {
		numColsForQueryIds.put( queryId, numCols );
	}
}
