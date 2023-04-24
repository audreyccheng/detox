package chronocache.core;

import java.util.List;

import chronocache.core.qry.Query;

public interface RelationVersionTable {

	public List<String> getSchemaCreationStrings();

	public List<String> getSchemaDestructionStrings();

	public String getRelationVersionQuery( String tableName );

	public String updateRelationVersionQuery( Query query );

	public String[] getTableNames();
}
