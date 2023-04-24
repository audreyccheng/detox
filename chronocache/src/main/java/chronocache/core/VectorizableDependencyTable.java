package chronocache.core;

import java.lang.Math;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.HashMultimap;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A per-client dependency table for known vectorizables
 * Stores which queries are required to execute for an vectorizables
 */
public class VectorizableDependencyTable {

	/**
	 * A class used to track when we should look for FDQs from a given QID again
	 * for a particular client. Doing the FDQ search every time is expensive, so we
	 * use backoffs to search periodically.
	 *
	 * TODO: When we handle workload changes (if the workload is the same but transitions have changed,
	 * we will probably want to recognize that and decay this counter.
	 */
	private class VectorizableBackoffTracker {
		private Map<QueryIdentifier, Long> counter;
		private Map<QueryIdentifier, Long> timesWeSearchedForVectorizables;

		public VectorizableBackoffTracker() {
			this.counter = new HashMap<>();
			this.timesWeSearchedForVectorizables = new HashMap<>();
		}

		public synchronized boolean markExecutedAndCheckIfWeShouldFindVectorizables( QueryIdentifier qid ) {
			Long counterVal = counter.get( qid );
			if( counterVal == null ) {
				counter.put( qid, new Long( 1L ) );
				timesWeSearchedForVectorizables.put( qid, 1L );
				return true;
			}

			counterVal = counterVal + 1;
			counter.put( qid, counterVal );

			Long numSearches = timesWeSearchedForVectorizables.get( qid );
			if( counterVal >= Math.pow( 2, numSearches ) ) {
				numSearches++;
				timesWeSearchedForVectorizables.put( qid, numSearches );
				return true;
			}
			return false;
		}
	}

	private Multimap<QueryIdentifier, Vectorizable> dependencyToVectorizableMapping;
	private HashMultimap<QueryIdentifier, Vectorizable> vectorizables;
	private ConcurrentHashMap<Vectorizable.VectorizableId, Vectorizable> idToVecMap;
	private VectorizableBackoffTracker backoffTracker;
	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	/**
	 * Create an empty VectorizableDependencyTable
	 */
	public VectorizableDependencyTable() {
		dependencyToVectorizableMapping = HashMultimap.create();
		vectorizables = HashMultimap.create();
		idToVecMap = new ConcurrentHashMap<>();
		backoffTracker = new VectorizableBackoffTracker();
	}


	private synchronized void mergeInVectorizable( Vectorizable v ) {

		if( !v.getParentVectorizables().isEmpty() ) {
			logger.info( "Not adding vectorizable b/c it has parents: {}", v.getVectorizationDependencies() );
			return;
		}
		Set<Vectorizable> vecs = getVectorizables( v.getQueryId() );

		logger.debug( "mergeInVectorizable:{}", v.getQueryId() );
		logger.debug( "Found other fdqs with this QID: {}", vecs );

		// If there is nobody with the same QID, just add us
		if( vecs.isEmpty() ) {
			vectorizables.put( v.getQueryId(), v );
			idToVecMap.put( v.getVectorizableId(), v );
			return;
		}

		// Try and merge us with someone. We assume there is only one person with which we can merge
		boolean hasMerged = false;
		Set<Vectorizable> adjustedVecs = new HashSet<>();
		for( Vectorizable vec : vecs ) {
			if( !hasMerged && getVectorizableSubsumingRelationship( v, vec ) != VectorizableSubsumingRelationship.NONE ) {
				logger.info( "Going to merge v with vec: {} with {}", v.getVectorizationDependencies(), vec.getVectorizationDependencies() );
				Vectorizable mergedVectorizable = Vectorizable.createMergedVectorizable( v, vec );
				logger.info( "merge done, made: {}", mergedVectorizable.getVectorizationDependencies() );
				// push these in somehow
				hasMerged = true;
				adjustedVecs.add( mergedVectorizable );
				idToVecMap.put( mergedVectorizable.getVectorizableId(), mergedVectorizable );
			} else {
				adjustedVecs.add( vec );
			}
		}

		// If we merged, we need to replace the map. The old child/parent pointers will still stick around, but won't be in the map anymore.
        // XXX BUG: But will still be in the dependency map!!!
		if( hasMerged ) {
			logger.trace( "We've merged some stuff! Need to merge!" );
			vectorizables.replaceValues( v.getQueryId(), adjustedVecs );
		} else {
			// Otherwise there was no one to merge with, so record us separately
			logger.info( "Added v {} to dependencyTable.", v.getVectorizationDependencies() );
			vectorizables.put( v.getQueryId(), v );
			idToVecMap.put( v.getVectorizableId(), v );
		}

	}

	public void addVectorizable( Vectorizable v ) {
		logger.debug( "Add Vectorizable:{}", v.getQueryId(), v.getVectorizableId() );

		Collection<QueryIdentifier> allQueriesWeNeedTextFor = v.getAllQueryIdsWeNeedTextFor();

		// Shortcut outside of lock to avoid grabbing it if we already have tihs entry
		if( idToVecMap.containsKey( v.getVectorizableId() ) ) {
            // If this thing contains the same queries and the same baseTexts, and the same forward edges
            // (No guarantee on mappings though), we don't care about this vectorizable.
			Vectorizable v2 = idToVecMap.get( v.getVectorizableId() );
			if( v.getVectorizationDependencies().getAllDependencyMappings().hashCode() == v2.getVectorizationDependencies().getAllDependencyMappings().hashCode() ) {
				logger.info( "Not adding Vectorizable for qid: {} because we already know about it.", v.getQueryId() );
				return;
			}
            // Likely different forward mappings, need to update
		}

		synchronized( this ) {
			// Still need to check if the lock in case we lost the race to insert it.
			if( idToVecMap.containsKey( v.getVectorizableId() ) ) {
				Vectorizable v2 = idToVecMap.get( v.getVectorizableId() );
				if( v.getVectorizationDependencies().getAllDependencyMappings().hashCode() == v2.getVectorizationDependencies().getAllDependencyMappings().hashCode() ) {
					logger.info( "Not adding Vectorizable for qid: {} because we already know about it.", v.getQueryId() );
					return;
				}
				// We must have updated the vectorizable's mappings, replace
				idToVecMap.put( v.getVectorizableId(), v );
				logger.info( "Refined mappings for vectorizable v: {}", v.getQueryId() );
				return;
			}

			logger.info( "Adding Vectorizable for qid: {}", v.getQueryId() );

			Vectorizable copyV = new Vectorizable( v );

			// We don't have this vectorizable yet. Add the dependencies for this vectorizable and merge it into the map
			// TODO: We assume at this point that we are adding, which might not be the case if we have parents.
			for( QueryIdentifier queryId : allQueriesWeNeedTextFor ) {
				logger.debug( "Add Vectorizable:{} dependency:{}", copyV.getQueryId(), queryId );
				Collection<Vectorizable> otherVectorizablesWithThisDependency = dependencyToVectorizableMapping.get( queryId );
                // As we add this vectorizable, forward along the basequery to any other people.
                // Not sure if this matters?
				if( !otherVectorizablesWithThisDependency.isEmpty() ) {
					Vectorizable vectorizable = otherVectorizablesWithThisDependency.iterator().next();
					logger.trace( "Found other query with same dependency: {}", vectorizable.getId() );
					String baseText = vectorizable.getBaseTextIfDependencyMet( queryId );
					logger.trace( "Got base text: {}", baseText );
					if( baseText != null ) {
						// Dependency met, forward along the mapping
						logger.debug( "Found dependency as executed, forwarding base text from {} to {}", vectorizable.getId(), vectorizable.getId() );
						copyV.markDependencyAsExecuted( queryId, baseText );
					}
				}
                // BUG: should not be done yet b/c we haven't determined we are adding this.
                // Needs to not have a parent in the map!
                // Should not affect anything b/c if it has parents it will not be added to the vectorizables,
                // which means getAndClearAllReadyVectorizablesFiltered() will not fire for it.
                // Preserved for compatibility with old code just in case.
				dependencyToVectorizableMapping.put( queryId, copyV );
			}

            // Figure out if we have any parents
			updateVectorizableSubsumingRelationships( copyV );

			// TODO: This may be expensive, so if we need to optimize, consider
			// examining this function call.
			mergeInVectorizable( copyV );
		}
	}

	public synchronized Set<Vectorizable> getVectorizables( QueryIdentifier q ) {
		return vectorizables.get( q );
	}

	public synchronized Vectorizable getVectorizable( Vectorizable.VectorizableId vecId ) {
		return idToVecMap.get( vecId );
	}

	public void addFDQ( Vectorizable fdq ) {
		addVectorizable( fdq );
	}

	public Set<Vectorizable> getFDQs( QueryIdentifier qid ) {
		Set<Vectorizable> vectorizables = getVectorizables( qid );
		Set<Vectorizable> fdqs = new HashSet<>();
		for( Vectorizable v : vectorizables ) {
			if( v.isFDQ() ) {
				fdqs.add( v );
			}
		}
		return fdqs;
	}

	public boolean alreadyHaveLoopForTrigger( QueryIdentifier triggerQueryId ) {
		Set<Vectorizable> vectorizables = getVectorizables( triggerQueryId );
		if( vectorizables.isEmpty() ) {
			return false;
		}
		for( Vectorizable v : vectorizables ) {
			if( v.isTriggerQuery() ) {
				return true;
			}
		}
		return false;
	}

	public void addLoop( Vectorizable loop ) {
		addVectorizable( loop );
	}

    /**
	* Return a set of all known ADQs
	*/
	public Set<Vectorizable> getAlwaysDefinedQueries() {
		Set<Vectorizable> adqs = new HashSet<>();
		synchronized( this ) {
			for( Vectorizable potentialADQ : vectorizables.values() ) {
				if( potentialADQ.isAlwaysDefined() ) {
					adqs.add( potentialADQ );
				}
			}
		}
		return adqs;
	}

	/**
	* Return a set of all known ADQs
	*/
	public Map<QueryIdentifier, Vectorizable> getIdKeyedADQs() {
		Map<QueryIdentifier,Vectorizable> adqMap = new HashMap<>();
		synchronized( this ) {
			for( Vectorizable potentialADQ : vectorizables.values() ) {
				if( potentialADQ.isAlwaysDefined() ) {
					adqMap.put( potentialADQ.getQueryId(), potentialADQ );
				}
			}
		}
		return adqMap;
	}

	/**
	 * Mark a dependency as executed.
	 */
	public void markExecutedDependency( Query query ) {
		QueryIdentifier queryId = query.getId();
		String queryText = query.getQueryString();
		synchronized( this ) {
			for( Vectorizable vecWithThisDependency : dependencyToVectorizableMapping.get( queryId ) ) {
				logger.info( "Marking {}, graph: {} as {} executed with queryText: {}", vecWithThisDependency.getId(), vecWithThisDependency.getVectorizationDependencies(), queryId, queryText );
				vecWithThisDependency.markDependencyAsExecuted( queryId, queryText );
			}
		}
	}

	public List<Vectorizable> getAndClearAllReadyVectorizables() {
		List<Vectorizable> readyVectorizables = new LinkedList<>();
		synchronized( this ) {
			for( Vectorizable vec : vectorizables.values() ) {
				// should be ready if it's ready and we actually want to execute it
				logger.trace( "Vectorizable:{}, Graph: {}, isReady:{}, shouldExecuteIfReady:{}",
						vec.getQueryId(), vec.getVectorizationDependencies(), vec.isReady(), vec.shouldExecuteIfReady() );
				if( vec.isReady() && vec.shouldExecuteIfReady() ) {
					readyVectorizables.add( vec );
					vec.unsetReady();
				}
			}
		}
		return readyVectorizables;
	}

	public synchronized void getAndClearAllReadyVectorizablesFiltered( QueryIdentifier qid, Set<Vectorizable> containingVectorizables, Set<Vectorizable> otherVectorizables ) {
		for( Vectorizable vec : vectorizables.values() ) {
			// should be ready if it's ready and we actually want to execute it
			logger.debug( "Vectorizable:{}, Graph: {}, isReady:{}, shouldExecuteIfReady:{}",
				vec.getQueryId(), vec.getVectorizationDependencies(), vec.isReady(), vec.shouldExecuteIfReady() );
			if( vec.isReady() && vec.shouldExecuteIfReady() ) {
				// Put vec into containingVectorizables if it appears in the vectorizables
				// dependency graph
				if( vec.getVectorizationDependencies().containsQueryId( qid ) ) {
					containingVectorizables.add( vec );
				} else {
					otherVectorizables.add( vec );
				}
				// Clear the ready state
				vec.unsetReady();
			}
		}
	}

	private enum VectorizableSubsumingRelationship {
		NONE,
		PARENT,
		CHILD
	};

	private boolean childVectorizableHasDifferentBaseQueriesFromParentAndNotAlreadyMaterialized(
		Vectorizable child,
		Vectorizable parent
	) {
		if ( child.getShouldMaterializeVectorizable() ) {
			logger.trace( "Child has materialize!" );
			return false;
		}

		// if they have different base queries
		Set<QueryIdentifier> childBaseQueries = new HashSet( child.getAllQueryIdsWeNeedTextFor() );
		Collection<QueryIdentifier> parentBaseQueries = parent.getAllQueryIdsWeNeedTextFor();

		for ( QueryIdentifier parentBaseQuery : parentBaseQueries ) {
			if ( !childBaseQueries.contains( parentBaseQuery) )  {
				// safe if the base query is an ADQ
				Set<Vectorizable> baseVecs = getVectorizables( parentBaseQuery );

				// If we have nothing vectorizables for this query, we need to wait until it is executed
				// The same is true if the base query isn't always ready to go.
				if( baseVecs.isEmpty() ) {
					logger.trace( "baseVecs is empty!" );
					return true;
				}

				for( Vectorizable vec : baseVecs ) {
					if( !vec.isAlwaysDefined() ) {
						logger.trace( "vec {} is always defined!", vec.getQueryId() );
						return true;
					}
				}
			}
		}

		logger.trace( "Returning false!" );
		return false;
	}

	private synchronized void updateVectorizableSubsumingRelationships( Vectorizable newVectorizable ) {
		logger.debug( "updateVectorizableSubsumingRelationships:{}", newVectorizable.getQueryId() );

		Set<Vectorizable.VectorizableId> seenVecIds = new HashSet<>();
		Vectorizable.VectorizableId newVectorizableId = newVectorizable.getVectorizableId();

		// For every vectorizable we know about
		for ( Vectorizable existingVectorizable : vectorizables.values() ) {
			Vectorizable.VectorizableId existingVecId = existingVectorizable.getVectorizableId();

			// skip things we have already seen
			if ( seenVecIds.contains( existingVecId ) ) {
				continue;
			}
			seenVecIds.add( existingVecId );

			// Get the relationship between these Vectorizables
			VectorizableSubsumingRelationship relationship =
				getVectorizableSubsumingRelationship( newVectorizable, existingVectorizable );
			logger.trace( "Determine that relationship between {} and {} is: {}", newVectorizable, existingVectorizable, relationship );

			if ( relationship == VectorizableSubsumingRelationship.PARENT ) {
				// new vectorizable is a parent of existing vectorizable
				// so record that we are their parent, and therefore the parent of any of their children

				existingVectorizable.addParentVectorizable( newVectorizableId );
				newVectorizable.addChildVectorizable( existingVecId );
				logger.debug( "Adding child to {}: {}", newVectorizable, existingVectorizable );

				if ( childVectorizableHasDifferentBaseQueriesFromParentAndNotAlreadyMaterialized(
							existingVectorizable, newVectorizable ) ) {
					logger.trace( "Setting should materialize!" );
					existingVectorizable.setShouldMaterializeVectorizable( );
				}

				List<Vectorizable.VectorizableId> childrenToRemove = new LinkedList<>();

				for ( Vectorizable.VectorizableId child :
						existingVectorizable.getChildVectorizables() ) {

					if ( seenVecIds.contains( child ) ) {
						continue;
					}

					Vectorizable childVectorizable = getVectorizable( child );
					if ( childVectorizable == null) {
						// add to delete
						childrenToRemove.add( child );
						continue;
					}
					assert childVectorizable != null;
					if ( childVectorizable.isAlwaysDefined() &&
							!newVectorizable.isAlwaysDefined())  {
						// don't add child if it is always defined and we are not
						continue;
					}

					newVectorizable.addChildVectorizable( child );
					childVectorizable.addParentVectorizable( newVectorizableId );
					seenVecIds.add( child );

					if ( childVectorizableHasDifferentBaseQueriesFromParentAndNotAlreadyMaterialized(
								childVectorizable, newVectorizable ) ) {
						childVectorizable.setShouldMaterializeVectorizable();
					}
				}
				existingVectorizable.removeChildVectorizables( childrenToRemove );

			} else if ( relationship == VectorizableSubsumingRelationship.CHILD ) {
				existingVectorizable.addChildVectorizable( newVectorizableId );
				newVectorizable.addParentVectorizable( existingVecId );

				if ( childVectorizableHasDifferentBaseQueriesFromParentAndNotAlreadyMaterialized(
							newVectorizable, existingVectorizable ) ) {
					newVectorizable.setShouldMaterializeVectorizable();
				}

				List<Vectorizable.VectorizableId> parentsToRemove = new LinkedList<>();

				for ( Vectorizable.VectorizableId parent : existingVectorizable.getParentVectorizables() ) {
					if ( seenVecIds.contains( parent ) ) {
						continue;
					}

					Vectorizable parentVectorizable = getVectorizable( parent );
					if( parentVectorizable == null) {
						// add to delete
						parentsToRemove.add( parent );
						continue;
					}

					newVectorizable.addParentVectorizable( parent );
					parentVectorizable.addChildVectorizable( newVectorizableId );
					seenVecIds.add( parent );

					if ( childVectorizableHasDifferentBaseQueriesFromParentAndNotAlreadyMaterialized(
								newVectorizable, parentVectorizable ) ) {
						newVectorizable.setShouldMaterializeVectorizable();
					}
				}

				existingVectorizable.removeParentVectorizables( parentsToRemove );
			}
		}
	}

	private VectorizableSubsumingRelationship
		getVectorizableSubsumingRelationship( Vectorizable a, Vectorizable b) {
		if ( isVectorizableParentOf( a, b ) ) {
			return VectorizableSubsumingRelationship.PARENT;
		} else if ( isVectorizableParentOf( b, a ) ) {
			return VectorizableSubsumingRelationship.CHILD;
		}
		return VectorizableSubsumingRelationship.NONE;
	}

	// is a a parent of b
	private boolean isVectorizableParentOf( Vectorizable a, Vectorizable b ) {
		DependencyGraph aDependencyGraph = a.getVectorizationDependencies();
		DependencyGraph bDependencyGraph = b.getVectorizationDependencies();
		logger.info( "Determining whether {} is a parent of {}", aDependencyGraph, bDependencyGraph );
		Multimap<QueryIdentifier, QueryIdentifier> aEdges = aDependencyGraph.forwardEdges;
		Multimap<QueryIdentifier, QueryIdentifier> bEdges = bDependencyGraph.forwardEdges;

		// a has to be larger
		if ( aEdges.size() < bEdges.size() ) {
			logger.info( "{} is not a parent of {}: smaller edges", aDependencyGraph, bDependencyGraph );
			return false;
		}

		if ( b.isAlwaysDefined( ) && !a.isAlwaysDefined( ) ) {
			// if b is always defined it doesn't matter if a subsumes b,
			// unless a is also always defined, because otherwise we would not
			// execute b as expected
			logger.info( "{} is not a parent of {}: not always defined", aDependencyGraph, bDependencyGraph );
			return false;
		}

		// every edge of b has to be in a
		for( Map.Entry<QueryIdentifier, QueryIdentifier> bEntry : bEdges.entries() ) {
			if( !aEdges.containsEntry( bEntry.getKey(), bEntry.getValue() ) ) {
				logger.info( "{} is not a parent of {}: entry in B not in A", aDependencyGraph, bDependencyGraph );
				return false;
			}
		}

		// The "base query" status of each node should be the same, or it isn't a strict parent/child.
		for( QueryIdentifier qid : bDependencyGraph.getAllQueryIds() ) {
			boolean aIsBaseQuery = aDependencyGraph.isBaseQuery( qid );
			boolean bIsBaseQuery = bDependencyGraph.isBaseQuery( qid );
			logger.trace( "QID {} is base query ({} vs {})", qid, aIsBaseQuery, bIsBaseQuery );
			if( (aIsBaseQuery && !bIsBaseQuery) || (!aIsBaseQuery && bIsBaseQuery) ) {
				logger.info( "{} is not a parent of {}: QID {} base query status", aDependencyGraph, bDependencyGraph, qid );
				return false;
			}
		}


		// if b is zero sized then b should be in a's forward mapping for a to be a parent
		if ( bEdges.size() == 0 ) {
			boolean isParent = aEdges.containsKey( b.getQueryId() );
			logger.info( "{} is parent of {}: {} b no edges", aDependencyGraph, bDependencyGraph, isParent );
			return isParent;
		}

		if ( aEdges.size() == bEdges.size() ) {
			// match everything but same size, break tie on qID
			boolean isParent = a.getQueryId().getId() < b.getQueryId().getId();
			logger.info( "{} is a parent of {}: {} same edges", aDependencyGraph, bDependencyGraph, isParent );
			return isParent;
		}

		logger.info( "{} is a parent of {}.", aDependencyGraph, bDependencyGraph );
		return true;
	}

	/**
	 * Given a set Map of vectorizables, add them if we don't know about them already.
	 */
	public void loadInNewVectorizablesFrom( Map<Vectorizable.VectorizableId, Vectorizable> vectorizables ) {
		for( Vectorizable vec : vectorizables.values() ) {
			logger.info( "Loading in: {}", vec.getVectorizationDependencies() );
			addVectorizable( vec );
		}
	}

	public int size() {
		return vectorizables.size();
	}

	public boolean shouldCheckForVectorizables( QueryIdentifier qid ) {
		return backoffTracker.markExecutedAndCheckIfWeShouldFindVectorizables( qid );
	}

	public Map<Vectorizable.VectorizableId, Vectorizable> getVectorizableMap() {
		return idToVecMap;
	}
}
