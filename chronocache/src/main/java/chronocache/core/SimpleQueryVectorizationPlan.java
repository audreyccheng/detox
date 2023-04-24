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
public class SimpleQueryVectorizationPlan extends QueryVectorizationPlan {

	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	public SimpleQueryVectorizationPlan(
		QueryVectorizationPlan plan
	) {
		super( plan.dependencyGraph, plan.queryShells, plan.vectorizationPlan );
	}

}
