package chronocache.core;

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
import com.google.common.collect.HashMultimap;

public class QueryVectorizationPlanNode {
	QueryIdentifier queryId;
	Integer height;
	Integer joinQueryNumberIfBase;

	public QueryVectorizationPlanNode( QueryIdentifier queryId, Integer height, Integer joinQueryNumberIfBase ) {
		this.queryId = queryId;
		this.height = height;
		this.joinQueryNumberIfBase = joinQueryNumberIfBase;
	}

	public QueryIdentifier getQueryId() {
		return queryId;
	}

	public Integer getHeight() {
		return height;
	}

	public Integer getJoinQueryNumberIfBase() {
		return joinQueryNumberIfBase;
	}

}

