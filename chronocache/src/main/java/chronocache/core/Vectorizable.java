package chronocache.core;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import com.google.common.collect.Multimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;

public class Vectorizable {

	public class VectorizableId {
		private Integer id;
		public VectorizableId( Integer id ) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals( Object o ) {
			if( o == null ) {
				return false;
			}

			if( !(o instanceof VectorizableId) ) {
				return false;
			}

			VectorizableId otherId = (VectorizableId) o;
			return id == otherId.getId();
		}

		@Override
		public String toString() {
			return String.valueOf( id );
		}

	}

	protected QueryIdentifier qid;
	private String queryString;
	private Set<String> qshIgnoreStringInstances;

	VectorizableType vectorizableType;
	private boolean shouldMaterializeVectorizable;
	private boolean isVectorizable;
	private boolean isSimpleVectorizable;

	private DependencyGraph vectorizableDependencyGraph;
	private Map<QueryIdentifier, Boolean> queryDependencies;

	// Vectorizables that are your parents in the graph, (superset of you)
	private Set<VectorizableId> parentVectorizables;

	// Vectorizables that are your child (they are a subset of you)
	private Set<VectorizableId> childVectorizables;

	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	/**
	 * Construct a Vectorizable from the provided dependency graph
	 */
	public Vectorizable( DependencyGraph dependencyGraph, QueryIdentifier qid,
			String queryString, VectorizableType vectorizableType) {
		this.qid = qid;
		this.queryString = queryString;
		this.vectorizableType = vectorizableType;
		vectorizableDependencyGraph = dependencyGraph;
		qshIgnoreStringInstances = new HashSet<>();
		queryDependencies = new HashMap<>();
		parentVectorizables = new HashSet<>();
		childVectorizables = new HashSet<>();
		shouldMaterializeVectorizable = false;

		// Set everything to executed initially and then clear all the dependencies which have > 0 params
		for( QueryIdentifier baseQueryId :
				vectorizableDependencyGraph.getAllBaseQueryIds() ) {
			queryDependencies.put( baseQueryId, true );
		}
		this.unsetReady();

		this.isVectorizable = vectorizableDependencyGraph.isVectorizable();
		this.isSimpleVectorizable = vectorizableDependencyGraph.isSimpleVectorizable();

		logger.debug("Vectorizable {} has dependencies {}", qid, queryDependencies);
	}

	public Vectorizable( Vectorizable v ) {
		this.qid = v.qid;
		this.queryString = new String( v.queryString );
		this.vectorizableType = new VectorizableType( v.vectorizableType );
		this.vectorizableDependencyGraph = new DependencyGraph( v.vectorizableDependencyGraph );
		qshIgnoreStringInstances = new HashSet<>( v.qshIgnoreStringInstances );
		queryDependencies = new HashMap<>( v.queryDependencies );
		parentVectorizables = new HashSet<>( v.parentVectorizables );
		childVectorizables = new HashSet<>( v.childVectorizables );
		shouldMaterializeVectorizable = v.shouldMaterializeVectorizable;
		this.isVectorizable = v.isVectorizable;
		this.isSimpleVectorizable = v.isSimpleVectorizable;
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}
		if( !( o instanceof Vectorizable ) ) {
			return false;
		}

		// Are these referring to the same object, not a true equals.
		Vectorizable v = (Vectorizable) o;
		return getVectorizableId().equals( v.getVectorizableId() );

	}

	/**
	 * Get this Vectorizable instance's query id
	 */
	public QueryIdentifier getQueryId() {
		return qid;
	}
	public QueryIdentifier getId() {
		return getQueryId();
	}

	public QueryIdentifier getTriggerQueryId() {
		return getQueryId();
	}

	/**
	 * Get this Vectorizable instance's query string
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * Return if this Vectorizable is an ADQ
	 */
	public boolean isAlwaysDefined() {
		return vectorizableType.isADQ();
	}
	public boolean isFDQ() {
		return vectorizableType.isFDQ();
	}
	public boolean isTriggerQuery() {
		return vectorizableType.isTriggerQuery();
	}

	/**
	 * Convenient bean wrapper hamcrest functions during testing
	 */
	public boolean getAlwaysDefined() {
		return isAlwaysDefined();
	}

	/**
	 * Return if this Vectorizable is executable aka can be vectorized
	 */
	public boolean isVectorizable() {
		return isVectorizable;
	}

	public boolean isSimpleVectorizable() {
		return isSimpleVectorizable;
	}

	/**
	 * Add a newly discovered querystring instance for this ADQ if we don't
	 * already know about it
	 *
	 * Used for when qsh_ignore collides multiple distinct query shell instances
	 */
	public void addADQStringIfUnknown( String queryString ) {
		if( !isAlwaysDefined() || queryString == null) {
			return;
		} else if( !queryString.equals( getQueryString() ) &&
				 !qshIgnoreStringInstances.contains( queryString ) ) {
			logger.warn("Vectorizable {} added new querystring {}", getId().getId(), queryString );
			qshIgnoreStringInstances.add( queryString );
		} else {
			logger.debug("Vectorizable {} already had querystring {}", getId().getId(), queryString );
		}
	}

	/**
	 * Return all other strings that we know about for this ADQ
	 */
	public Set<String> getAlternativeADQStringInstances() {
		return qshIgnoreStringInstances;
	}

	/**
	 * Get the dependency graph that describes the dependencies and relationships
	 * we need to vectorize this Vectorizable
	 */
	public DependencyGraph getVectorizationDependencies() {
		return vectorizableDependencyGraph;
	}

	/**
	 * Get the IDs of queries that we need text for before this Vectorizable can execute
	 */
	public Collection<QueryIdentifier> getAllQueryIdsWeNeedTextFor() {
		return queryDependencies.keySet();
	}

	/**
	 * Mark a dependency of this loop as executed, and with what query text.
	 * If the query was not already a base query, it will be marked as such now.
	 */
	public void markDependencyAsExecuted( QueryIdentifier queryId, String queryText ) {
		// We assume there is one top-level base query
		if( vectorizableDependencyGraph.getQueryDependencies( queryId ).isEmpty() ) {
			logger.info( "{} Got top level dependency, unsetting all non-toplevel dependencies.", vectorizableDependencyGraph );
			// We assume one trigger query here.
			for( QueryIdentifier dependencyQueryId : queryDependencies.keySet() ) {
				if( !vectorizableDependencyGraph.getQueryDependencies( dependencyQueryId ).isEmpty() ) {
					logger.info( "{} unsetting {}", vectorizableDependencyGraph, dependencyQueryId );
					queryDependencies.replace( dependencyQueryId, false );
				}
			}
		}
		vectorizableDependencyGraph.markQueryAsBaseQuery( queryId, queryText );
		queryDependencies.replace( queryId, true );
	}

	/**
	 * Get the base text for this dependency if it is met.
	 */
	public String getBaseTextIfDependencyMet( QueryIdentifier dependencyQueryId ) {
		assert queryDependencies.containsKey( dependencyQueryId );
		Boolean isSatisfied = queryDependencies.get( dependencyQueryId );
		if( isSatisfied ) {
			return vectorizableDependencyGraph.getBaseQueryText( dependencyQueryId );
		}
		return null;
	}

	/**
	 * Return if this loop is ready to be vectorized
	 */
	public boolean isReady() {
		for( Boolean satisfied : queryDependencies.values() ) {
			if( !satisfied ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Unset ready dependencies
	 */
	public void unsetReady() {
		for( QueryIdentifier queryId : queryDependencies.keySet() ) {
			// Only base queries with > 0 parameters should be reset
			Query query = vectorizableDependencyGraph.getQueryShellFromGraph( queryId );
			// triggerQueries unset ready to false always
			if( ( query.getParams().size() > 0 ) || vectorizableType.isTriggerQuery( ) ) {
				queryDependencies.replace( queryId, false );
			}
		}
	}

	public void addParentVectorizable( VectorizableId parent ) {
		logger.trace( "Adding parent to {}: {}", this, parent );
		parentVectorizables.add( parent );
	}
	public void removeParentVectorizables( Collection<VectorizableId> oldParents ) {
		logger.trace( "Removing parents to {}: {}", this, oldParents );
		parentVectorizables.removeAll( oldParents );
	}

	public void addChildVectorizable( VectorizableId child ) {
		logger.trace( "Adding child to {}: {}", this, child );
		childVectorizables.add( child );
	}
	public void removeChildVectorizables( Collection<VectorizableId> oldChildren ) {
		logger.trace( "Removing children to {}: {}", this, oldChildren );
		childVectorizables.removeAll( oldChildren );
	}


	// TODO : mtabebe, figure out how to detect this and use this
	public void setShouldMaterializeVectorizable( ) {
		shouldMaterializeVectorizable = true;
	}
	public boolean getShouldMaterializeVectorizable( ) {
		return shouldMaterializeVectorizable;
	}

	public boolean shouldExecuteIfReady() {
		// we only want to execute Vectorizables that we should materialize or if we have no parents
		// subsuming us us
		return ( shouldMaterializeVectorizable ||  parentVectorizables.isEmpty() );
	}


	public boolean hasVectorizableAsParent( VectorizableId vecId ) {
		return parentVectorizables.contains( vecId );
	}

	public boolean hasVectorizableAsChild( VectorizableId vecId ) {
		return childVectorizables.contains( vecId );
	}

	public Set<VectorizableId> getParentVectorizables() {
		return parentVectorizables;
	}
	public Set<VectorizableId> getChildVectorizables() {
		return childVectorizables;
	}

	public static Vectorizable createMergedVectorizable(
		Vectorizable v1,
		Vectorizable v2
	) {
		assert v1.qid == v2.qid;

		VectorizableType mergedType =
			VectorizableType.createMergedVectorizableType( v1.vectorizableType,
					v2.vectorizableType );

		DependencyGraph mergedGraph =
			DependencyGraph.createMergedDependencyGraph(
					v1.vectorizableDependencyGraph,
					v2.vectorizableDependencyGraph );

		Vectorizable mergedVec = new Vectorizable( mergedGraph, v1.qid,
				v1.queryString, mergedType );

		mergedVec.qshIgnoreStringInstances.addAll( v1.qshIgnoreStringInstances );
		mergedVec.qshIgnoreStringInstances.addAll( v2.qshIgnoreStringInstances );

		mergedVec.parentVectorizables.addAll( v1.parentVectorizables );
		mergedVec.parentVectorizables.addAll( v2.parentVectorizables );

		mergedVec.childVectorizables.addAll( v1.childVectorizables );
		mergedVec.childVectorizables.addAll( v2.childVectorizables );

		return mergedVec;
	}

	@Override
	public String toString() {
		return "" + this.getId().getId();
	}

	public VectorizableId getVectorizableId() {
		int hash = 7;
		hash = 31 * hash + vectorizableDependencyGraph.hashCode();
		hash = 31 * hash + (int) qid.getId();
		return new VectorizableId( hash );
	}

}
