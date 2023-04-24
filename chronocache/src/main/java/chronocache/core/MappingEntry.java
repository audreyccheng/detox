package chronocache.core;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;

import java.lang.StringBuilder;

/*
 * A class representing a single mapping for when QueryMappingEntry's are not granular enough
 * head and tail refer to either end of a directed edge where edges are directed in the direction of
 * data flow
 */
public class MappingEntry {

	private QueryIdentifier head; // The query from which the data is going to
	private QueryIdentifier tail; // The query from which the data is coming from
	private int headConstantPosition; // Constant position that the data is going to
	private String tailColumnName; // Column name that the data is coming out of

	public MappingEntry(
		QueryIdentifier head,
		QueryIdentifier tail,
		int headConstantPosition,
		String tailColumnName
	) {
		this.head = head;
		this.tail = tail;
		this.headConstantPosition = headConstantPosition;
		this.tailColumnName = tailColumnName;
	}

	public QueryIdentifier getHeadQuery() {
		return head;
	}

	public QueryIdentifier getTailQuery() {
		return tail;
	}

	public int getConstantPosition() {
		return headConstantPosition;
	}

	public String getColumnName() {
		return tailColumnName;
	}

	@Override
	public String toString() {
		StringBuilder mapping = new StringBuilder();
		mapping.append( tail.getId() ).append( ' ' ).append( tailColumnName );
		mapping.append( " -> " );
		mapping.append( headConstantPosition ).append( ' ' ).append( head.getId() );
		return mapping.toString();
	}
}
