package chronocache.core;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.lang.Cloneable;

import chronocache.core.qry.QueryIdentifier;

/**
 * A mapping of dependencies required for execution of a FDQ
 */ public class DependencyMapping {
	protected List<QueryIdentifier> dependencies;
	protected Set<QueryIdentifier> unsatisfiedDependencies;
	protected Vectorizable vectorizable;

	/**
	 * Create a dependency mapping from a list of dependencies to the FDQ they define
	 */
	public DependencyMapping( List<QueryIdentifier> depends, Vectorizable vectorizable ) {
		this.dependencies = depends;
		this.unsatisfiedDependencies = new HashSet<>( depends );
		this.vectorizable = vectorizable;
	}

	/**
	* Hidden constructor for deep copy construction
	*/
	private DependencyMapping(){
	}

	/**
	* Deep-copy this DependencyMapping and return
	*/
	public DependencyMapping getCopy() {
		List<QueryIdentifier> copyDeps = new LinkedList<>( dependencies );
		Set<QueryIdentifier> copyUnsat = new HashSet<>( unsatisfiedDependencies );
		DependencyMapping copy = new DependencyMapping();
		copy.dependencies = copyDeps;
		copy.unsatisfiedDependencies = copyUnsat;
		copy.vectorizable = vectorizable;
		return copy;
	}

	/**
	 * Return a list of stored dependencies for the FDQ
	 */
	public List<QueryIdentifier> getDependencies() {
		return dependencies;
	}

	/**
	* Get FDQ for this dependency mapping
	*/
	public Vectorizable getVectorizable() {
		return vectorizable;
	}
	public Vectorizable getFDQ() {
		return getVectorizable();
	}


	/**
	 * Mark a dependency as executed, returning true if the corresponding
	 * FDQ is ready for execution.
	 */
	public boolean markDependencyAsExecuted( QueryIdentifier qid ) {
		unsatisfiedDependencies.remove( qid );
		if( unsatisfiedDependencies.isEmpty() ) {
			unsatisfiedDependencies.addAll( dependencies );
			return true;
		}
		return false;
	}
}
