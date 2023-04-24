package chronocache.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.lang.StringBuilder;
import chronocache.core.qry.Query;

public class TPCWRelationVersionTable implements RelationVersionTable {
	public static final String tableNames[] = new String[] {
		"address",
		"author",
		"cc_xacts",
		"country",
		"customer",
		"item",
		"order_line",
		"orders",
		"shopping_cart",
		"shopping_cart_line",
	};

	public TPCWRelationVersionTable() {
	}

	public List<String> getSchemaCreationStrings() {
		StringBuilder sb = new StringBuilder();
		sb.append( "CREATE TABLE relation_versions( name text, version counter, " );
		sb.append( "PRIMARY KEY( name ) )" );

		List<String> schemaStrs = new LinkedList<>();
		schemaStrs.add( sb.toString() );

		for( String tableName : tableNames ) {
			sb = new StringBuilder();
			sb.append( "UPDATE relation_versions SET version = version + 1 WHERE " );
			sb.append( "name = '" );
			sb.append( tableName );
			sb.append( "'" );
			schemaStrs.add( sb.toString() );
		}

		return schemaStrs;
	}

	public List<String> getSchemaDestructionStrings() { 
		StringBuilder sb = new StringBuilder();
		sb.append( "DROP TABLE IF EXISTS relation_versions" );
		List<String> schemaStrs = new LinkedList<>();
		schemaStrs.add( sb.toString() );
		return schemaStrs;
	}

	public String getRelationVersionQuery( String tableName ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "SELECT version FROM relation_versions WHERE name = '" );
		sb.append( tableName );
		sb.append( "'" );
		return sb.toString();
	}

	public String updateRelationVersionQuery( Query query ) {

		Iterator<String> tableNamesIter = query.getTables().iterator();
		if( !tableNamesIter.hasNext() ) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append( "UPDATE relation_versions SET " );
		sb.append( "version = version + 1 WHERE " );
		sb.append( "name IN ( " );

		sb.append( "'" );
		sb.append( tableNamesIter.next() );
		sb.append( "'" );

		while( tableNamesIter.hasNext() ) {
			sb.append( ", '" );
			sb.append( tableNamesIter.next() );
			sb.append( "'" );
		}

		sb.append( " )" );
		return sb.toString();
	}

	public String[] getTableNames() {
		return tableNames;
	}

}
