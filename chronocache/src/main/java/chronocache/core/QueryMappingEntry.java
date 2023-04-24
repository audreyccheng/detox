package chronocache.core;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.lang.StringBuilder;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

public class QueryMappingEntry {

	private QueryIdentifier dependencyQueryId;
	private Query queryShell;
	private Multimap<Integer, String> mappings;

	public QueryMappingEntry( QueryIdentifier dependencyQueryId, Query queryShell, Multimap<Integer, String> mappings ) {
		this.dependencyQueryId = dependencyQueryId;
		this.queryShell = queryShell;
		this.mappings = mappings;
	}

	public QueryIdentifier getDependencyQueryId() {
		return dependencyQueryId;
	}

	public Multimap<Integer, String> getQueryMappings() {
		return mappings;
	}

	public Query getQueryShell() {
		return queryShell;
	}

	public QueryMappingEntry increaseMappingOffset( int offset ) {
		Multimap<Integer, String> locMappings = HashMultimap.create();
		for( Map.Entry<Integer, String> mappingEntry : mappings.entries() ) {
			locMappings.put( mappingEntry.getKey() + offset, mappingEntry.getValue() );
		}
		return new QueryMappingEntry( dependencyQueryId, queryShell, locMappings );
	}

	public QueryMappingEntry createEntryAccountingForAliases( Map<String,List<String>> changedAliases ) {
		Multimap<Integer, String> locMappings = HashMultimap.create();
		for( Map.Entry<Integer, String> mappingEntry : mappings.entries() ) {
			String mappingCol = mappingEntry.getValue();
			if( changedAliases == null ) {
				// Straight copy
				locMappings.put( mappingEntry.getKey(), mappingEntry.getValue() );
			} else {
				List<String> newAliases = changedAliases.get( mappingCol );
				if( newAliases.isEmpty() ) {
					locMappings.put( mappingEntry.getKey(), mappingCol );
				} else {
					if( newAliases.size() == 1 ) {
						locMappings.put( mappingEntry.getKey(), newAliases.iterator().next() );
					} else {
						// FIXME: The same column can be aliased more than once.
						// In this case, we should differentiate which aliased version we should use, but we don't support this yet.
						throw new IllegalArgumentException( "More than one alias candidate and we haven't supported this yet!" );
					}
				}
			}
		}
		return new QueryMappingEntry( dependencyQueryId, queryShell, locMappings );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "Query Mapping Entry for: " );
		sb.append( queryShell.getId() );
		sb.append( " from " );
		sb.append( dependencyQueryId );
		sb.append( " has " );
		sb.append( mappings.size() );
		sb.append( " mappings: [" );
		for( Map.Entry<Integer, String> entry : mappings.entries() ) {
			sb.append( " (" );
			sb.append( entry.getKey() );
			sb.append( "," );
			sb.append( entry.getValue() );
			sb.append( ")" );
		}
		sb.append( " ]" );
		
		return sb.toString();
	}

	public int size() {
		return mappings.keySet().size();
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (int) dependencyQueryId.getId();
		hash = 31 * hash + (int) queryShell.getId().getId();
		hash = 31 * hash + mappings.hashCode();
		return hash;
	}
}
