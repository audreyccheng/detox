package chronocache.core;

import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.PlSqlParser;
import chronocache.core.parser.AntlrQueryMetadata;
import chronocache.core.parser.SelectListAliasAdjuster;

import chronocache.db.DB;
import chronocache.db.DBException;

import java.lang.IllegalArgumentException;
import java.lang.StringBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Multimap;


import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A vectorizer that combines arbitrary queries with exploitable patterns using lateral unions.
 */
public class LateralUnionVectorizer extends QueryVectorizer {

	/**
	 * Describes an OrderBy condition's direction
	 */
	public enum OrderDirection {
		DEFAULT,
		ASC,
		DESC
	}

	/**
	 * For a given condition we should order by, describes the "field we should order by"'s position in the query result's output result set, the direction,
	 * and the original column name of the orderBy condition. */
	private class OrderTypeInformation {

		private int startPosition;
		private OrderDirection direction;
		private String origColName;

		public OrderTypeInformation( int startPosition, OrderDirection direction, String origColName ) {
			this.startPosition = startPosition;
			this.direction = direction;
			this.origColName = origColName;
		}

		/**
		 * Describes the position of this orderBy field in query's result set.
		 * For example if the output result set has columns (a,b,c,d) and we are ordering by field b, then the position is 1.
		 */
		public int getStartPosition() {
			return startPosition;
		}

		/**
		 * Push the start position for this condition to the right by "amount".
		 * Used when we merge two queries together via a lateral union to increment the offset of the order by field.
		 */
		public void increaseStartPosition( int amount ) {
			startPosition += amount;
		}

		/**
		 * Get the direction of the order by condition.
		 */
		public OrderDirection getOrderDirection() {
			return direction;
		}

		/**
		 * Get the original name of the field we are ordering by.
		 * It has likely been renamed by u* in the union, but this returns what it originally was.
		 */
		public String getOrigColName() {
			return origColName;
		}

		@Override
		public String toString() {
			return String.valueOf( startPosition ) + " - " + direction;
		}

	}

	/**
	 * A wrapper around zipper merge results.
	 */
	public class ZipperMergeResult {
		String zipperedQueryText;
		List<OrderTypeInformation> orderTypeInfos;
		List<String> ckFields;
		int totalNumberOfColumns;
		Map<QueryIdentifier, Map<String,List<String>>> queryMappingAliasAdjustments;
		Set<String> tables;

		public ZipperMergeResult(
			String zipperedQueryText,
			List<OrderTypeInformation> orderTypeInfos,
			List<String> ckFields,
			int totalNumberOfColumns,
			Map<QueryIdentifier, Map<String,List<String>>> queryMappingAliasAdjustments,
			Set<String> tables
		) {
			this.zipperedQueryText = zipperedQueryText;
			this.orderTypeInfos = orderTypeInfos;
			this.ckFields = ckFields;
			this.totalNumberOfColumns = totalNumberOfColumns;
			this.queryMappingAliasAdjustments = queryMappingAliasAdjustments;
			this.tables = tables;
		}

		/**
		 * Get the combined text from the zipper merge.
		 */
		public String getZipperedQueryText() {
			return zipperedQueryText;
		}

		/**
		 * Get combined order type information from the zipper merged queries
		 */
		public List<OrderTypeInformation> getOrderTypeInfos() {
			return orderTypeInfos;
		}

		/**
		 * Get candidate key fields.
		 */
		public List<String> getCkFields() {
			return ckFields;
		}

		/**
		 * Get the number of columns in the zippered text
		 */
		public int getNumColumns() {
			return totalNumberOfColumns;
		}

		/**
		 * Get any alias adjustments for queries in the zippered text
		 */
		public Map<QueryIdentifier, Map<String, List<String>>> getQueryMappingAliasAdjustments() {
			return queryMappingAliasAdjustments;
		}

		/**
		 * Get the table names in the zippered text
		 */
		public Set<String> getTables() {
			return tables;
		}
	}

	/**
	 * A wrapper around vectorization results for a given level in the topological dependency graph.
	 */
	private class VectorizedLevelResult {

		private String vectorizedString;
		private int numberOfColumns;
		private List<OrderTypeInformation> orderTypeInfos;
		private List<String> orderByConditions;
		private Set<String> tables;

		public VectorizedLevelResult(
			String vectorizedString,
			int numberOfColumns,
			List<OrderTypeInformation> orderTypeInfos,
			Set<String> tables
		) {
			this.vectorizedString = vectorizedString;
			this.numberOfColumns = numberOfColumns;
			this.orderTypeInfos = orderTypeInfos;
			this.tables = tables;
		}

		/**
		 * Get the number of selected columns for the queries that were vectorized at this level.
		 * (and consequently all levels below it)
		 */
		public int getNumberOfColumns() {
			return numberOfColumns;
		}

		/**
		 * Get the result of vectorizing the text for this level (and all levels below it).
		 */
		public String getVectorizedQueryText() {
			return vectorizedString;
		}

		/**
		 * Get order by information (position, direction, etc) for this level and all levels below it.
		 */
		public List<OrderTypeInformation> getOrderTypeInfos() {
			return orderTypeInfos;
		}

		/**
		 * Get the accessed tables by this level and all levels below it.
		 */
		public Set<String> getTables() {
			return tables;
		}

	}

	private class DecodedRowLevelResult {
		private Map<QueryIdentifier,Map<String,Object>> decodedRowForQueries;
		private int level;

		public DecodedRowLevelResult( Map<QueryIdentifier, Map<String,Object>> decodedRowForQueries, int level ) {
			this.decodedRowForQueries = decodedRowForQueries;
			this.level = level;
		}

		public Map<QueryIdentifier,Map<String,Object>> getDecodedRowForQueries() {
			return decodedRowForQueries;
		}

		public int getLevel() {
			return level;
		}
	}

	private class RowLevelInformation {
		private int level;
		private int baseUOffset;

		public RowLevelInformation( int level, int baseUOffset ) {
			this.level = level;
			this.baseUOffset = baseUOffset;
		}

		public int getLevel() {
			return level;
		}

		public int getBaseUOffset() {
			return baseUOffset;
		}
	}

	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	// FIXME: Should also have some kind of Parse tree check just to be sure
	private Map<QueryIdentifier, Integer> queryToColumnNumberMap;

	public LateralUnionVectorizer() {
		super();
		this.queryToColumnNumberMap = new ConcurrentHashMap<>();
	}


	/**
	 * Given a vectorizable, vectorize it using lateral unions.
	 */
	@Override public QueryVectorizerResult vectorize( long clientId, Vectorizable vectorizableQueries, DB db ) {
		DependencyGraph dependencyGraph = vectorizableQueries.getVectorizationDependencies();
		logger.trace( "Trying to vectorize: {}", dependencyGraph );
		return vectorize( clientId, dependencyGraph, db );
	}

	/**
	 * Determine the number of fields/columns returned by the query
	 */
	public int getNumberOfSelectedCols( long clientId, Query q, DB db ) throws DBException {
		//Should really check if there is a limit already and adjust it --- but we don't put
		// limits on the outer queries so we can always pack this limit.

		Integer numColsFromMap = queryToColumnNumberMap.get( q.getId() );
		if( numColsFromMap != null ) {
			logger.debug( "Found {} in queryToColumnNumberMap, num cols: {}", q.getId(), numColsFromMap );
			return numColsFromMap;
		}

		String queryString = q.getQueryString();

		queryString = queryString.replaceAll( " LIMIT \\d+$", "" );
		String limitedQueryString = queryString + " LIMIT 0";

		logger.trace( "Checking number of cols for query: {}", queryString );

		QueryResult result = db.querySpeculativelyToGetNumRows( clientId, limitedQueryString );
		int numCols = result.getNumColumns();
		logger.trace( "Got numCols: {}", numCols );

		logger.trace( "Putting [{}:{}] into the column map", q.getId(), numCols );
		queryToColumnNumberMap.put( q.getId(), numCols );

		return numCols;
	}

	/**
	 * Given a "simple" order by condition, determine what column we are ordering over.
	 */
	private String getColumnFromOrderBy( String orderByExpression ) {
		//XXX For now, we assume that the order by is "simple"
		String tmpOrderBy = orderByExpression;
		tmpOrderBy = tmpOrderBy.replaceFirst( ", DESC", "" );
		tmpOrderBy = tmpOrderBy.replaceFirst( ", ASC", "" );
		tmpOrderBy = tmpOrderBy.replaceFirst( " DESC", "" );
		tmpOrderBy = tmpOrderBy.replaceFirst( " ASC", "" );
	
		return tmpOrderBy;
	}

	/**
	 * Given a "simple" order by condition, determine which direction we are ordering by.
	 */
	private OrderDirection getOrderDirection( String orderByExpression ) {
		if( orderByExpression.contains( "DESC" ) ) {
			return OrderDirection.DESC;
		} else if( orderByExpression.contains( "ASC" ) ) {
			return OrderDirection.ASC;
		}
		return OrderDirection.DEFAULT;
	}

	/**
	 * Given the metadata for a query, figure out what column position "affectedCol" is in.
	 * Returns -1 if the field is not found.
	 * E.g., if we are returning columns (a,b,c,d) the position for c is 2.
	 */
	private int selectedColsContains( AntlrQueryMetadata metadata, String affectedCol ) {
		// Check the metadata (true column names)
		List<String> selectedCols = metadata.getSelectedColumnsInQuery();
		logger.trace( "Found selectedCols: {}, looking for affectedCol {}", selectedCols, affectedCol );
		int i = 0;
		for( String column : selectedCols ) {
			if( affectedCol.equals( column ) ) {
				logger.trace( "{} equals {}", affectedCol, column );
				return i;
			}
			i++;
		}
		// We may have aliased the column, check it as well.
		Integer pos = metadata.getAliasesToPositionMap().get( affectedCol );
		if( pos != null ) {
			return pos;
		}

		// Can't find the column
		return -1;
	}


	/**
	 * Adds the select clause for the lateralUnion rewrite.
	 * returns the number of orderByKeys selected in the top-level select list.
	 */
	private int addSelectClause(
		StringBuilder queryTextBuilder,
		List<OrderTypeInformation> orderTypeInfosForThisLevel,
		List<String> ckFields,
		int level,
		List<OrderDirection> ordKeyDirs
	) {

		logger.debug( "Adding select clause at level: {}", level );
		logger.debug( "Have orderTypeInformation: {}", orderTypeInfosForThisLevel );
		int nonTypeLUMetadataKeyNumber = 0;
		queryTextBuilder.append( "SELECT " );


		// Go over our order by conditions, make sure they selected out of the text for this level.
		// This will also include the "type" field.
		for( OrderTypeInformation orderTypeInfo : orderTypeInfosForThisLevel ) {

			logger.debug( "Adding orderTypeInfo for {}", orderTypeInfo.getOrigColName() );

			// We are ordering only by things visible to P
			// Will work if we are ordering by column names
			// XXX What about if we order by SUM() or something?
			if( nonTypeLUMetadataKeyNumber > 0 ) {
				queryTextBuilder.append(", ");
			}
			queryTextBuilder.append( "P" );
			queryTextBuilder.append( level );
			queryTextBuilder.append( "." );
			String colName = orderTypeInfo.getOrigColName();
			OrderDirection direction = orderTypeInfo.getOrderDirection();
			ordKeyDirs.add( direction );
			queryTextBuilder.append( colName );
			queryTextBuilder.append( " AS ordKey" );
			queryTextBuilder.append( nonTypeLUMetadataKeyNumber );
			nonTypeLUMetadataKeyNumber++;
		}

		// Induce a candidate key over the query results for the text at this level by using the row number.

		for( String ckField : ckFields ) {
			if( nonTypeLUMetadataKeyNumber > 0 ) {
				queryTextBuilder.append( ", " );
			}
			// If this was a zipper merge level, then all of their <RN_i> form a CK.
			// Otherwise, just RN0 is sufficient.
			queryTextBuilder.append( "P" );
			queryTextBuilder.append( level );
			queryTextBuilder.append( "." );
			queryTextBuilder.append( ckField );
			queryTextBuilder.append( " AS ordKey" );
			queryTextBuilder.append( nonTypeLUMetadataKeyNumber );
			nonTypeLUMetadataKeyNumber++;
		}

		// Select all the fields from the union'd text.
		queryTextBuilder.append( ", U.* " );
		return nonTypeLUMetadataKeyNumber;
	}

	private void addPTableClause(
		StringBuilder queryTextBuilder,
		String queryTextForThisLevel,
		int level
	) {
		// Create a table P<level>, that holds the results for the text at this level
		queryTextBuilder.append( "FROM ( " );
		queryTextBuilder.append( queryTextForThisLevel );

		queryTextBuilder.append( " ) P" );
		queryTextBuilder.append( level );
	}

	/**
	 * Adds the UTable Clause for this lateralUnion rewrite.
	 * Returns the total number of fields in U.
	 */
	private int addUTableClause(
		StringBuilder queryTextBuilder,
		VectorizedLevelResult resultFromLowerLevels,
		int colsForOuterQuery,
		int level
	) {
		// The union half for P. Type is 0 indicating this is a field for P. Select all fields for P
		queryTextBuilder.append( "SELECT 0, P" );
		queryTextBuilder.append( level );
		queryTextBuilder.append( ".*" );
		// Then pack NULLs for each item in the select list for the text for the levels below us.
		for( int i = 0; i < resultFromLowerLevels.getNumberOfColumns(); i++ ) {
			queryTextBuilder.append( ", NULL" );
		}

		// We select this from any table with 1 row in it, so that we get the above row once.
		// Here, we use ( VALUES ( 1 ) ) to induce the table on the fly. Not all DBs support this, so we
		// should use a pre-generated table with one row.
		queryTextBuilder.append( " FROM DTOneRow " );

		// Now we create the other half of the union.
		queryTextBuilder.append( "UNION ALL " );

		// Type is 1 indicating that this row is coming from the lower levels of the queries we unioned with.
		queryTextBuilder.append( "SELECT 1" );

		// Pack nulls for each of the columns that P selects.
		for( int i = 0; i < colsForOuterQuery; i++ ) {
			queryTextBuilder.append(", NULL" );
		}

		// The lower levels are going to be wrapped in a table I<level>, so select everything from I<level> (I<level>.*).
		queryTextBuilder.append( ", I" );
		queryTextBuilder.append( level );

		// Now pack the lower levels text into I<level>
		queryTextBuilder.append( ".* FROM ( " );
		queryTextBuilder.append( resultFromLowerLevels.getVectorizedQueryText() );
		queryTextBuilder.append( ") I" );
		queryTextBuilder.append( level );

		// Rename this entire union result to U. U won't be visible at the higher levels, so we don't have to qualify it.
		queryTextBuilder.append( ") U( type" );
		queryTextBuilder.append( level );

		// Figure out how many fields we have in total ( upper level # of field + # fields for all lower levels )
		int totalNumberOfFields = resultFromLowerLevels.getNumberOfColumns() + colsForOuterQuery;

		// Pack these into U with appropriate names so we can reference them easily.
		for( int i = 0; i < totalNumberOfFields; i++ ) {
			queryTextBuilder.append( ", u" );
			queryTextBuilder.append( i );
		}
		return totalNumberOfFields;
	}

	/**
	 * Add the orderBy fields to the queryTextBuilder when doing a LateralUnion rewrite.
	 * returns the new OrderTypeInformation list for this entire layer and updates the orderByConditions list.
	 */
	private List<OrderTypeInformation> addOrderByInformation(
		StringBuilder queryTextBuilder,
		int nonTypeLUMetadataKeyNumber,
		List<OrderDirection> ordKeyDirs,
		int colsForOuterQuery,
		VectorizedLevelResult resultFromLowerLevels,
		int level,
		LateralUnionVectorizationPlan plan
	) {
		// Now we need to be sure that we order everything properly
		queryTextBuilder.append( " ) ORDER BY " );

		// Pack all of the metadata/ordering conditions for the top level
		// (AKA any ordering keys for the text at this level, the candidate key, then the type (1 or 0)
		Iterator<OrderDirection> ordKeyDirIterator = ordKeyDirs.iterator();
		List<OrderTypeInformation> thisLevelOrderTypeInfos = new LinkedList<>();
		logger.trace( "At level: {}, we have nonTypeLUMetadataKeyNumber: {}", level, nonTypeLUMetadataKeyNumber );
		for( int i = 0; i < nonTypeLUMetadataKeyNumber; i++ ) {

			if( i > 0 ) {
				queryTextBuilder.append( ", " );
			}
			queryTextBuilder.append( "ordKey" );
			queryTextBuilder.append( i );


			if( ordKeyDirIterator.hasNext() ) {
				OrderDirection direction = ordKeyDirIterator.next();
				thisLevelOrderTypeInfos.add( new OrderTypeInformation( i, direction, "ordKey" + String.valueOf( i ) ) );
				if( direction == OrderDirection.ASC ) {
					queryTextBuilder.append( " ASC" );
				} else if( direction == OrderDirection.DESC ) {
					queryTextBuilder.append( " DESC" );
				}
			} else {
				thisLevelOrderTypeInfos.add( new OrderTypeInformation( i, OrderDirection.DEFAULT, "ordKey" + String.valueOf( i ) ) );
			}
		}
		queryTextBuilder.append(", type" );
		queryTextBuilder.append( level );
		thisLevelOrderTypeInfos.add( new OrderTypeInformation( nonTypeLUMetadataKeyNumber, OrderDirection.DEFAULT, "type" + String.valueOf( level ) ) );

		// Pack all of the ordering conditions for the lower levels.
		List<OrderTypeInformation> orderTypeInfos = resultFromLowerLevels.getOrderTypeInfos();
		logger.trace( "Got orderTypeInfos: {}", orderTypeInfos );

		// Because we packed all the columns for the text at this level in, we need to offset the order by field's positioning accordingly
		for( OrderTypeInformation orderTypeInfo : orderTypeInfos ) {
			int startPosition = orderTypeInfo.getStartPosition();
			startPosition = startPosition + colsForOuterQuery;
			queryTextBuilder.append( ", u" );
			queryTextBuilder.append( startPosition );
			OrderDirection direction = orderTypeInfo.getOrderDirection();
			if( direction == OrderDirection.ASC ) {
				queryTextBuilder.append( " ASC" );
			} else if( direction == OrderDirection.DESC ) {
				queryTextBuilder.append( " DESC" );
			}
		}
		

		// Now we pack our order by information with the lower level's order by information so we can pass it up to the next level.
		List<OrderTypeInformation> newOrderTypeInfos = new LinkedList<>();

		int ordNum = 0;
		// Add our orderBy conditions
		logger.trace( "OT infos for this level: {}", thisLevelOrderTypeInfos );
		newOrderTypeInfos.addAll( thisLevelOrderTypeInfos );
		ordNum = thisLevelOrderTypeInfos.size();
		logger.trace( "ordNum: {}", thisLevelOrderTypeInfos.size() );

		for( OrderTypeInformation orderInfoEntry : orderTypeInfos ) {
			// offset by another 1 for the type
			orderInfoEntry.increaseStartPosition( colsForOuterQuery + nonTypeLUMetadataKeyNumber + 1 );
			newOrderTypeInfos.add( orderInfoEntry );
		}
		logger.trace( "newOrderTypeInfos: {}", newOrderTypeInfos );

		plan.setNumColsAtTopologicalLevel( level, colsForOuterQuery );
		logger.trace( "Setting order keys at level {} to be {}", level, nonTypeLUMetadataKeyNumber );
		plan.setNumNonTypeLUMetadataKeysAtTopologicalLevel( level, nonTypeLUMetadataKeyNumber );
		return newOrderTypeInfos;
	}


	/**
	 * Vectorize the queries at a given level.
	 * The idea is that we zipperMerge all queries at this level, and then lateral union our results with
	 * the results from any of the lower levels.
	 */
	private VectorizedLevelResult vectorizeLevel(
		long clientId,
		Integer level,
		Multimap<Integer, QueryIdentifier> topologicalDepthToQueryIdMap,
		LateralUnionVectorizationPlan plan,
		Integer maxDepth,
		DB db,
		Map<QueryIdentifier, Map<String, List<String>>> queryMappingAliasAdjustmentsFromHigherLevels
	) {

		/* The goal is to construct a query that looks like:
		 * SELECT <order fields for queries at this level>, <candidate key for queries at this level (RN) >, type<level>, U.*
		 * FROM ( <zipper merged text for queries at this level> ) P<level>, LATERAL ( 
		 *  SELECT 0, P.*, < NULL for each column selected by vectorized text from lower level > FROM <table with one row>
		 *  UNION
		 *  SELECT 1, < NULL for each column in zipper merged text for queries at this level>, I<level>.* FROM (
		 *  <vectorized text for lower levels> ) I<level> ) U( type, u1, u2, ... )
		 * ORDER BY <order fields for queries at this level>, <candidate key/RN for this level>, <type>, <ordering keys for lower levels>
		 *
		 * Doing so gives us a result set that looks like:
		 * < order fields for queries at this level >, < candidate key/RN >, <type>, <cols for zipper merged text at this level >, < cols for lower levels >
		 * 
		 * We generate this query recursively, with the text at the lowest level being just the flat zipper merged text.
		 */

		logger.trace( "At level: {}, maxDepth : {}", level, maxDepth );
		assert level <= maxDepth;

		Collection<QueryIdentifier> queryIdsAtThisLevel = topologicalDepthToQueryIdMap.get( level );
		logger.trace( "Found {} queries at this level.", queryIdsAtThisLevel.size() );
		assert !queryIdsAtThisLevel.isEmpty();
		
		// Zipper merge the queries at this level, pull out the metadata.
		
		long zipperStart = System.nanoTime()/1000;
		ZipperMergeResult result = zipperMerge( clientId, queryIdsAtThisLevel, plan, db, queryMappingAliasAdjustmentsFromHigherLevels );
		long zipperEnd = System.nanoTime()/1000;
		logger.trace( "Time to zipper merge: {}", zipperEnd-zipperStart );

		String queryTextForThisLevel = result.getZipperedQueryText();
		List<OrderTypeInformation> orderTypeInfosForThisLevel = result.getOrderTypeInfos();

		logger.trace( "Text for queries at level: {}", queryTextForThisLevel );

		// We need to know how many columns this level's zippered query's return, so that we when union our results with
		// the lower results we pack with the right number of nulls.
		// This may require doing a database query, but afterwards we will cache the results in this object and not
		// ask again for the same query shell.
		int colsForOuterQuery = result.getNumColumns();
		logger.trace( "Determined that: {} selects {} cols", queryTextForThisLevel, colsForOuterQuery );
		assert colsForOuterQuery >= 1;

		Map<QueryIdentifier, Map<String, List<String>>> queryMappingAliasAdjustmentsForThisLevel = result.getQueryMappingAliasAdjustments();

		// If we are the bottom most query, then we don't need to union with anyone, just return our vectorizedLevelResult.
		if( level == maxDepth ) {

			plan.setNumColsAtTopologicalLevel( level, colsForOuterQuery );
			plan.setNumNonTypeLUMetadataKeysAtTopologicalLevel( level, 0 ); // We didn't select any additional order keys

			VectorizedLevelResult levelResult = new VectorizedLevelResult( queryTextForThisLevel, colsForOuterQuery, orderTypeInfosForThisLevel, result.getTables() );

			logger.trace( "vectorized query at level {}: \"{}\" has {} fields", level, levelResult.getVectorizedQueryText(), levelResult.getNumberOfColumns() );
			return levelResult;
		}

		queryMappingAliasAdjustmentsForThisLevel.putAll( queryMappingAliasAdjustmentsFromHigherLevels );
		
		// Otherwise, recursively vectorize the levels below us.
		VectorizedLevelResult resultFromLowerLevels = vectorizeLevel( clientId, level+1, topologicalDepthToQueryIdMap, plan, maxDepth, db, queryMappingAliasAdjustmentsForThisLevel );

		StringBuilder queryTextBuilder = new StringBuilder();

		List<OrderDirection> ordKeyDirs = new LinkedList<OrderDirection>();
		long selectAddStart = System.nanoTime()/1000;
		int nonTypeLUMetadataKeyNumber = addSelectClause( queryTextBuilder, orderTypeInfosForThisLevel, result.getCkFields(), level, ordKeyDirs );
		long selectAddEnd = System.nanoTime()/1000;
		logger.debug( "Time to add Select Fields: {}", selectAddEnd-selectAddStart );

		// Now we need to lateral outer union our text with the text from the below levels.
		// P largely serves as a ordering reference for our select list and as fodder for the union.
		// We lateral P with the union of our query (P) and the vectorizedText from the lower levels.
		long pAddStart = System.nanoTime()/1000;
		addPTableClause( queryTextBuilder, queryTextForThisLevel, level );
		long pAddEnd = System.nanoTime()/1000;
		logger.debug( "Time to add P Table: {}", pAddEnd-pAddStart );

		queryTextBuilder.append( ", LATERAL ( " );

		long uAddStart = System.nanoTime()/1000;
		int totalNumberOfFields = addUTableClause( queryTextBuilder, resultFromLowerLevels, colsForOuterQuery, level );
		long uAddEnd = System.nanoTime()/1000;
		logger.debug( "Time to add U Table: {}", uAddEnd-uAddStart );

		long ordAddStart= System.nanoTime()/1000;
		List<OrderTypeInformation> newOrderTypeInfos = addOrderByInformation( queryTextBuilder, nonTypeLUMetadataKeyNumber, ordKeyDirs, colsForOuterQuery, resultFromLowerLevels, level, plan );
		long ordAddEnd = System.nanoTime()/1000;
		logger.debug( "Time to add ordByKeys: {}", ordAddEnd - ordAddStart );

		plan.setNumColsAtTopologicalLevel( level, colsForOuterQuery );
		logger.trace( "Setting order keys at level {} to be {}", level, nonTypeLUMetadataKeyNumber );
		plan.setNumNonTypeLUMetadataKeysAtTopologicalLevel( level, nonTypeLUMetadataKeyNumber );

		Set<String> allTables = result.getTables();
		allTables.addAll( resultFromLowerLevels.getTables() );

		// Should have all of U plus the necessary ordering fields, plus 1 for new field
		VectorizedLevelResult levelResult = new VectorizedLevelResult( queryTextBuilder.toString(), totalNumberOfFields + nonTypeLUMetadataKeyNumber+1, newOrderTypeInfos, allTables );

		logger.trace( "vectorized query at level {}: \"{}\" has {} fields", level, levelResult.getVectorizedQueryText(), levelResult.getNumberOfColumns() );

		return levelResult;
	}

	/**
	 * vectorize the queries contained in the dependency graph.
	 */
	public QueryVectorizerResult vectorize( long clientId, DependencyGraph dependencyGraph, DB db ) {

		if( !dependencyGraph.isVectorizable() ) {
			logger.warn( "DependencyGraph is not vectorizable!" );
			return new QueryVectorizerResult( null, null, null );
		}

		QueryVectorizationPlan plan = dependencyGraph.getVectorizationPlan();

		logger.debug( "Going to vectorize dependencyGraph." );

		assert plan.size() >= 1;
		if( plan.size() == 1 ) {
			LateralUnionVectorizationPlan luPlan = new LateralUnionVectorizationPlan( plan );
			QueryIdentifier qid = luPlan.iterator().next().getQueryId();
			Query q = luPlan.getQueryShell( qid );

			int numberOfCols = -1;
			try {
				numberOfCols = getNumberOfSelectedCols( clientId, q, db );
			} catch( DBException e ) {
				logger.error( "Could not get # of columns for {}", q.getQueryString() );
				throw new IllegalArgumentException( e );
			}

			luPlan.setNumColsAtTopologicalLevel( 0, numberOfCols );

			QueryVectorizerResult result = new QueryVectorizerResult( luPlan, getSingleQueryBaseText( plan, dependencyGraph ), q.getTables() );

			return result;
		}
		LateralUnionVectorizationPlan luPlan = new LateralUnionVectorizationPlan( plan );

		StringBuilder vectorizedQueryTextBuilder = new StringBuilder();
		Multimap<Integer, QueryIdentifier> topologicalDepthToQueryIdMap = luPlan.getTopologicalDepthToQueryIdMap();
		logger.trace( "topologicalDepthMap has {} entries.", topologicalDepthToQueryIdMap.size() );

		// Analogous
		Integer maxDepth = luPlan.getMaxTopologicalHeight();
		long allVecStart = System.nanoTime()/1000;
		VectorizedLevelResult levelResult = vectorizeLevel( clientId, 0, topologicalDepthToQueryIdMap, luPlan, maxDepth, db, new HashMap<QueryIdentifier, Map<String, List<String>>>() );
		long allVecEnd = System.nanoTime()/1000;
		
		logger.trace( "Time to vectorize all levels and get text: {}", allVecEnd-allVecStart );

		return new QueryVectorizerResult( luPlan, levelResult.getVectorizedQueryText(), levelResult.getTables() );
	}

	/**
	 * Add row numbers to the queryString in the order specified by order conditions.
	 * Call the row number column rn<queryNumber>
	 */
	public String addRowNumberColToQueryString( String queryString, List<String> orderByConditions, Map<String, List<String>> colNameAliasMappings, int queryNumber ) {
		StringBuilder queryTextBuilder = new StringBuilder();
		queryTextBuilder.append( "SELECT *, ROW_NUMBER() OVER (" );
		// Strictly speaking, we need to ensure the order of the RN's ONLY for zippered query texts. However, it may be useful in
		// the future to know that the rows are ordered by RNs, so we may as well do it in general since it is unlikely to add further
		// overhead (as the underlying result set is in the same order).
		if( !orderByConditions.isEmpty() ) {
			queryTextBuilder.append( "ORDER BY " );
			int i = 0;
			for( String orderByCondition : orderByConditions ) {
				// FIXME This is dangerous if someone names their table "B" or something
				// Should be a replace of table name rather than a string replace 
				String orderByCondDup = new String( orderByCondition );
				for( Map.Entry<String, List<String>> entry : colNameAliasMappings.entrySet() ) {
					String replaceKey = entry.getKey();
					List<String> replaceVals = entry.getValue();
					for( String replaceVal : replaceVals ) {
						logger.trace( "Going to change orderby alias in row number {} -> {} in {}", replaceKey, replaceVal, orderByCondDup );
						orderByCondDup = orderByCondDup.replaceFirst( replaceKey + "(?!_)", replaceVal );
					}
				}
				if( i > 0 ) {
					queryTextBuilder.append( ", " );
				}
				queryTextBuilder.append( orderByCondDup );
				i++;
			}
		}
		queryTextBuilder.append( ") AS rn" );
		queryTextBuilder.append( queryNumber );
		queryTextBuilder.append( " FROM ( " );
		queryTextBuilder.append( queryString );
		//throwaway table name k, doesn't matter what we call it b/c it won't be visible to anyone
		queryTextBuilder.append( " ) k" );
		return queryTextBuilder.toString();
		
	}

	/**
	 * Generate half of the zipperMerged text for queryString1, queryString2 with queryNumbers 1 and 2.
	 * This half corresponds to joinType (LEFT/RIGHT)
	 */
	public void generateZipperMergeTextHalf( String queryString1, String queryString2, int queryNumber1, int queryNumber2, String joinType, StringBuilder queryTextBuilder ) {
		queryTextBuilder.append( "SELECT * FROM ( " );
		queryTextBuilder.append( queryString1 );
		queryTextBuilder.append( " ) z" );
		queryTextBuilder.append( queryNumber1 );
		queryTextBuilder.append( " " );
		queryTextBuilder.append( joinType );
		queryTextBuilder.append( " ( " );
		queryTextBuilder.append( queryString2 );
		queryTextBuilder.append( " ) z" );
		queryTextBuilder.append( queryNumber2 );
		queryTextBuilder.append( " ON " );
		for( int i = 0; i < queryNumber2; i++ ) {
			//Every other query is a wrapper query after the first query, so it has no rn of its own
			if( i % 2 != 0 || i == 0 ) {
				if( i > 0 ) {
					queryTextBuilder.append( " OR " );
				}
				queryTextBuilder.append( "z" );
				queryTextBuilder.append( queryNumber1 );
				queryTextBuilder.append( ".rn" );
				queryTextBuilder.append( i );
				queryTextBuilder.append( " = " );
				queryTextBuilder.append( "z" );
				queryTextBuilder.append( queryNumber2 );
				queryTextBuilder.append( ".rn" );
				queryTextBuilder.append( queryNumber2 );
			}
		}

	}

	/**
	 * Take two query strings and zipperMerge them together.
	 */
	public String zipperMergeTwoQueries(
			String queryString1,
			String queryString2,
			List<String> query2OrderConditions,
			Map<String, List<String>> colNameAliasMappingsForQuery2,
			int queryNumber1,
			int queryNumber2
	) {
		String q2StringWithRowNumber = addRowNumberColToQueryString( queryString2, query2OrderConditions, colNameAliasMappingsForQuery2, queryNumber2 );
		StringBuilder queryTextBuilder = new StringBuilder();
		generateZipperMergeTextHalf( queryString1, q2StringWithRowNumber, queryNumber1, queryNumber2, "LEFT JOIN", queryTextBuilder );
		queryTextBuilder.append( " UNION " );
		generateZipperMergeTextHalf( queryString1, q2StringWithRowNumber, queryNumber1, queryNumber2, "RIGHT JOIN", queryTextBuilder );

		return queryTextBuilder.toString();
	}

	/**
	  * build a map from constant placement to topologicalDepth
	  */
	public Map<Integer, Integer> getPlaceToToplogicalLevelMap( Map<Integer, QueryColumn> placeToQueryColumnMap, Multimap<Integer, QueryIdentifier> topologicalDepthToQueryIdMap ) {
		Map<Integer,Integer> placeToTopologicalLevelMap = new HashMap<>();
		for( Map.Entry<Integer, QueryColumn> placeToColumnEntry : placeToQueryColumnMap.entrySet() ) {
			QueryIdentifier queryIdToFind = placeToColumnEntry.getValue().getQueryId();
			for( Map.Entry<Integer, QueryIdentifier> levelEntry : topologicalDepthToQueryIdMap.entries() ) {
				if( levelEntry.getValue().equals( queryIdToFind ) ) {
					placeToTopologicalLevelMap.put( placeToColumnEntry.getKey(), levelEntry.getKey() );
				}
			}
		}
		return placeToTopologicalLevelMap;
	}

	public Collection<QueryMappingEntry> accountForNewAliases(
		Collection<QueryMappingEntry> originalQueryMappings,
		Map<QueryIdentifier, Map<String, List<String>>> changedAliasesFromHigherLevels
	) {
		List<QueryMappingEntry> queryMappingsAccountingForNewAliases = new LinkedList<>();
		for( QueryMappingEntry entry : originalQueryMappings ) {
			Map<String, List<String>> colNamesToAdjust = changedAliasesFromHigherLevels.get( entry.getDependencyQueryId() );
			// Make new object so we don't change the original mappings
			QueryMappingEntry fixedEntry = entry.createEntryAccountingForAliases( colNamesToAdjust );
			queryMappingsAccountingForNewAliases.add( fixedEntry );
		}

		return queryMappingsAccountingForNewAliases;

	}

	/**
	 * Given a set of query mappings, a query containing the unqualified mappings, and a plan for the vectorization, return a query string
	 * with the properly qualified mappings.
	 */
	public String qualifyColumnMappings(
		Collection<QueryMappingEntry> queryMappings,
		String unqualifiedQuery,
		QueryVectorizationPlan plan,
		Map<QueryIdentifier, Map<String, List<String>>> changedAliasesFromHigherLevels,
		int numColumnsInQuery
	) {

		// No need to qualify anything if we have no mappings. 
		if( queryMappings.isEmpty() ) {
			return unqualifiedQuery;
		}

		Collection<QueryMappingEntry> queryMappingsAccountingForNewAliases = accountForNewAliases( queryMappings, changedAliasesFromHigherLevels );

		logger.trace( "Going to qualify column Mappings." );
		FlattenedQueryMappings flattenedMappings = flattenQueryMappings( queryMappingsAccountingForNewAliases );
		logger.trace( "flattened qmappings." );
		Map<Integer, QueryColumn> placeToQueryColumnMap = flattenedMappings.getPlaceToQueryColumnMappings();
		logger.trace( "got place to query mappings: {}", placeToQueryColumnMap );
		Multimap<Integer, QueryIdentifier> topologicalDepthToQueryIdMap = plan.getTopologicalDepthToQueryIdMap();
		logger.trace( "got topological depth");

		Map<Integer, Integer> placeToTopologicalLevelMap = getPlaceToToplogicalLevelMap( placeToQueryColumnMap, topologicalDepthToQueryIdMap );
		logger.trace( "did merge.");

		String rewrittenQuery = unqualifiedQuery.replaceAll( "QUALIFYME0\\.", "" );
		for( int i = 0; i < numColumnsInQuery; i++ ) {
			logger.trace( "Looking for level for place: {}", i );
			Integer level = placeToTopologicalLevelMap.get( i );

			if( level == null ) {
				continue;
			}
			logger.trace( "Got level {} for place: {}", level, i );
			rewrittenQuery = rewrittenQuery.replaceFirst( "QUALIFYME1\\.[a-zA_Z0-9_]+", "P" + String.valueOf( level ) + "." + placeToQueryColumnMap.get( i ).getColumnName() );
		}
		
		return rewrittenQuery;
		
	}


	/**
	 * Given a set of query mappings with their positions in the query, increase those positions by currentMappingOffset
	 */
	public Collection<QueryMappingEntry> adjustMappingOffsets( Collection<QueryMappingEntry> queryMappings, int currentMappingOffset ) {
		List<QueryMappingEntry> newMappings = new LinkedList<>();
		for( QueryMappingEntry mappingEntry : queryMappings ) {
			newMappings.add( mappingEntry.increaseMappingOffset( currentMappingOffset ) );
		}
		logger.trace( "Unadjusted QMES: {}", queryMappings );
		logger.trace( "Adjusted QMES: {}", newMappings );

		return newMappings;

	}

	/**
	 * Given a query, extract the order conditions and record them in a list of OrderTypeInformation.
	 * This will throw an exception if you try to order by a field that isn't in your select list (because then the field is not in U,
	 * so we will have problems when we try to order it in the lateral joined query. XXX We should handle this by adding your field to the
	 * the select list, but right now we don't.
	 */
	public List<OrderTypeInformation> getOrderTypeInfosFromOrigQuery( Query query ) {
			// N.B., this will actually contain subquery items as well, but we are ordered so we'll hit
			// the top level select list elements first. This is strictly speaking bad, but it should never happen...
			logger.trace( "Checking the ordering conditions!" );
			AntlrQueryMetadata metadata = query.getQueryMetadata();
			logger.trace( "Obtained metadata!" );

			List<String> orderByConditions = metadata.getOrderByConditions();
			logger.trace( "Got orderByConditions!" );
			List<OrderTypeInformation> orderTypeInfos = new LinkedList<OrderTypeInformation>();

			for( String orderByCondition: orderByConditions ) {
				logger.trace( "Got orderByCondition: {}", orderByCondition );
				String affectedCol = getColumnFromOrderBy( orderByCondition );
				logger.trace( "Got affectedCol: {}", affectedCol );
				OrderDirection direction = getOrderDirection( orderByCondition );
				logger.trace( "Got order by direction: {}", direction );
				logger.trace( "Found {} column in orderByCondition {}", affectedCol, orderByCondition );
				int colPosition = selectedColsContains( metadata, affectedCol );
				logger.trace( "Found {} in position: {}", affectedCol, colPosition );
				if( colPosition == -1 ) {
					throw new IllegalArgumentException( "Can't vectorize queries with order bys on columns not in their select list." );
				}

				OrderTypeInformation orderTypeInfo = new OrderTypeInformation( colPosition, direction, affectedCol );
				orderTypeInfos.add( orderTypeInfo );

			}

			return orderTypeInfos;

	}

	public int computeTotalNewMappings( Collection<QueryMappingEntry> queryMappings ) {
		int totMappings = 0;
		for( QueryMappingEntry qme : queryMappings ) {
			totMappings += qme.size();
		}
		return totMappings;
	}

	public Map<String, List<String>> buildColumnAliasesMap( Query query ) {
		Map<String, List<String>> aliasesMap = new HashMap<>();
		Iterator<String> selectedColumns = query.getQueryMetadata().getSelectedColumnsInQuery().iterator();
		Map<Integer, String> positionToAliasesMap = query.getQueryMetadata().getPositionToAliasesMap();
		int i = 0;
		while( selectedColumns.hasNext() ) {
			String selectedCol = selectedColumns.next();
			String aliasName = positionToAliasesMap.get( i );
			if( aliasName != null ) {
				List<String> aliasesForThisName = aliasesMap.get( selectedCol );
				if( aliasesForThisName == null ) {
					aliasesForThisName = new LinkedList<>();
				}
				aliasesForThisName.add( aliasName );
				aliasesMap.put( selectedCol, aliasesForThisName );
			}
			i++;
		}
		return aliasesMap;
	}

	/**
	 * Given a set of queries to merge at a particular level, zipper merge them.
	 */
	public ZipperMergeResult zipperMerge(
		long clientId,
		Collection<QueryIdentifier> queriesToMerge,
		QueryVectorizationPlan plan,
		DB db,
		Map<QueryIdentifier, Map<String,List<String>>> changedAliasesFromHigherLevels
	) {
		logger.debug( "Doing zipper merge!" );
		assert !queriesToMerge.isEmpty();

		DependencyGraph dependencyGraph = plan.getDependencyGraph();
		Iterator<QueryIdentifier> queryIdIterator = queriesToMerge.iterator();

		// Pass through the query with a rowNumber column, nothing to merge together.
		if( queriesToMerge.size() == 1 ) {
			logger.trace( "Zipper merging a single query!" );
			QueryIdentifier queryId = queryIdIterator.next();
			Query query = plan.getQueryShell( queryId );

			int numCols = -1;
			try {
				logger.trace( "Going to get number of selected cols!" );
				numCols = getNumberOfSelectedCols( clientId, query, db );
			} catch( DBException e ) {
				logger.trace( "Could not determine number of columns for \"{}\"", query.getQueryString() );
				throw new IllegalArgumentException( e );
			}
			Collection<QueryMappingEntry> queryMappings = dependencyGraph.getQueryDependencies( queryId );
			int numConstants = query.getQueryMetadata().getConstantsFromQuery().size();
			String baseQueryText = dependencyGraph.getBaseQueryText( query.getId() );
			if( baseQueryText == null ) {
				baseQueryText = query.getQueryString();
			}

			String rewrittenQuery = rewriteQueryWithColumnMappings( queryMappings, baseQueryText, numConstants );

			logger.trace( "Done rewriting query, need to qualify columnMappings." );

			rewrittenQuery = qualifyColumnMappings( queryMappings, rewrittenQuery, plan, changedAliasesFromHigherLevels, numConstants );

			logger.trace( "Done qualifying mappings, add row number..." );

			rewrittenQuery = addRowNumberColToQueryString( rewrittenQuery, query.getQueryMetadata().getOrderByConditions(), buildColumnAliasesMap( query ), 0 );
			numCols++;
			logger.trace( "After adding queryString, we think that {} has {} cols.", rewrittenQuery, numCols );
	
			// Here, we know that we are going to pass through the original query with an additional rn0.
			// If we are going to order by rn0, it will be determined by a higher level function.
			// So the only additional ordering requirements are those of the underlying query, not any
			// we have introduced. How do we figure out what position the ordering field is in?
			// Let's assume that it must be in the select list to be ordered, and that it will be
			// at the top level of the query, since inner level order bys are clobbered by outer query
			// order semantics. Thus, we can get the order by positions by checking their position in
			// the select list of the original query.

			List<OrderTypeInformation> orderTypeInfos = getOrderTypeInfosFromOrigQuery( query );
			logger.trace( "Got orderTypeInfos from query." );


			List<String> ckFields = new LinkedList<String>();
			ckFields.add( "rn0" );
			Set<String> tables = new HashSet<>( query.getTables() );
			logger.trace( "Returning everything!" );
			return new ZipperMergeResult( rewrittenQuery, orderTypeInfos, ckFields, numCols, new HashMap<QueryIdentifier, Map<String, List<String>>>(), tables );
		}

		// OK, we actually need to merge stuff
		logger.trace( "Found multiple queries at this level, need to zipper merge them together." );

		List<QueryMappingEntry> combinedQueryMappings = new LinkedList<QueryMappingEntry>();
		Set<String> allTableNames = new HashSet<>();
		Map<QueryIdentifier, Map<String,List<String>>> queryMappingAliasAdjustments = new HashMap<>();

		QueryIdentifier firstQueryId = queryIdIterator.next();
		Query firstQuery = plan.getQueryShell( firstQueryId );

		// We want to check if any of our column names collide. If so, we need
		// to add/adjust their alias.
		AntlrQueryMetadata firstQueryMetadata = firstQuery.getQueryMetadata();
		List<String> firstQuerySelectedCols = firstQueryMetadata.getSelectedColumnsInQuery();
		Map<Integer,String> firstQueryAliasMap = firstQueryMetadata.getPositionToAliasesMap();
		int numConstants = firstQueryMetadata.getConstantsFromQuery().size();
		String firstQueryString = dependencyGraph.getBaseQueryText( firstQuery.getId() );
		if( firstQueryString == null ) { 
			firstQueryString = firstQuery.getQueryString();
		}
		String curQueryString = addRowNumberColToQueryString( firstQueryString, firstQuery.getQueryMetadata().getOrderByConditions(), buildColumnAliasesMap( firstQuery ), 0 );

		combinedQueryMappings.addAll( dependencyGraph.getQueryDependencies( firstQueryId ) );
		allTableNames.addAll( firstQuery.getTables() );
		int currentMappingOffset = firstQueryMetadata.getConstantsFromQuery().size(); //Start after the end of our constants.

		logger.trace( "Set up for first query, have total mappings: {}", currentMappingOffset );
		logger.trace( "First query: {}", curQueryString );
		logger.trace( "First queryId: {}", firstQueryId );

		int queryNumber = 0;

		List<String> trueColumnNames = new LinkedList<>();
		int selectedColPosition = 0;
		for( String selectedColName : firstQuerySelectedCols ) {
			String aliasName = firstQueryAliasMap.get( selectedColPosition );
			// Add the alias if we have one, otherwise add the colName
			if( aliasName == null ) {
				trueColumnNames.add( selectedColName );
			} else {
				trueColumnNames.add( aliasName );
			}
			selectedColPosition++;
		}

		logger.trace( "Past setting up true col names!" );

		while( queryIdIterator.hasNext() ) {
			long zipperInternalStart = System.nanoTime()/1000;

			QueryIdentifier nextQueryId = queryIdIterator.next();
			logger.trace( "Got nextQueryId: {}", nextQueryId );
			Query nextQuery = plan.getQueryShell( nextQueryId );
			logger.trace( "Got next query: {}", nextQuery );
			AntlrQueryMetadata nextQueryMetadata = nextQuery.getQueryMetadata();
			logger.trace( "Got query metadata: {}", nextQueryMetadata );
			List<String> nextQuerySelectedCols = nextQueryMetadata.getSelectedColumnsInQuery();
			Map<Integer,String> nextQueryAliasMap = nextQueryMetadata.getPositionToAliasesMap();
			String nextQueryString = dependencyGraph.getBaseQueryText( nextQuery.getId() );
			if( nextQueryString == null ) {
				nextQueryString = nextQuery.getQueryString();
			}
			Map<String, List<String>> colNamesToAdjust = new HashMap<>();

			allTableNames.addAll( nextQuery.getTables() );
			numConstants += nextQueryMetadata.getConstantsFromQuery().size();
			selectedColPosition = 0;

			List<String> aliasAdjustments = new LinkedList<>();
			boolean needToAdjust = false;

			logger.trace( "Iterating over the cols and seeing if we have a collision!" );
			for( String nextQuerySelectedCol : nextQuerySelectedCols ) {
				String aliasName = nextQueryAliasMap.get( selectedColPosition );
				String colName = (aliasName == null) ? nextQuerySelectedCol : aliasName;
				String origColName = colName;
				

				//Determine a unique aliasName if we have one already
				while( trueColumnNames.contains( colName ) ) {
					colName = colName + "_0";
				}
				if( !origColName.equals( colName ) ) {
					logger.trace( "We have a collision: {} -> {}", origColName, colName );
					// Adjust the text to use our new columnName
					aliasAdjustments.add( colName );
					// Record this as a mapping adjustment
					List<String> aliasesForThisColName = colNamesToAdjust.get( origColName );
					if( aliasesForThisColName == null ) {
						aliasesForThisColName = new LinkedList<>();
					}
					aliasesForThisColName.add( colName );
					colNamesToAdjust.put( origColName, aliasesForThisColName );
					needToAdjust = true;
				} else {
					aliasAdjustments.add( null );
				}

				// Record this columnName so we check for collisions with later queries.
				trueColumnNames.add( colName );
			}

			logger.trace( "Current trueColumnNames: {}", trueColumnNames );

			// Now that we have colNamesToAdjust, we need to kick this down to everyone at a level below us so they know to change their mappings from us.
			// The best way to do this is add to a map that is keyed on our queryId

			if( needToAdjust ) {
				logger.trace( "Going to make colAdjustments: {}", aliasAdjustments );
				logger.trace( "Next query tokens: {}", nextQuery.getQueryTokens() );

				long timeToAdjustSelectListStart = System.nanoTime()/1000;
				TokenSource tokenSource = new ListTokenSource( nextQuery.getQueryTokens() );
				CommonTokenStream tokenStream = new CommonTokenStream( tokenSource );
				PlSqlParser parser = new PlSqlParser( tokenStream );
				SelectListAliasAdjuster adjuster = new SelectListAliasAdjuster( tokenStream, aliasAdjustments );

				long timeToAdjustSelectListEnd = System.nanoTime()/1000;
				logger.debug( "time to adjust select list: {}", timeToAdjustSelectListEnd-timeToAdjustSelectListStart );
				logger.trace( "Going to walk!" );
				ParseTreeWalker.DEFAULT.walk( adjuster, parser.dml_compilation_unit() );
				logger.trace( "Done walk!" );
				nextQueryString = adjuster.getText();
				logger.trace( "Revised query string: {}", nextQueryString );
			}

			// We care only about ordering by RN. The only reason we order by all RNs is that some
			// queries may have more rows than others. This ensures that the rows will be totally ordered:
			// - Assume the left query has some pertinent ordering, and that the right query does as well.
			// - The left query is keyed by row number, which implies the ordering.
			// - The right query is given a row number, which implies its ordering.
			// - By joining on the row numbers we thus zipper them together in an appropriate order.
			// - To ensure output ordering, we must order by the joint RN, and that is all that matters.

			curQueryString = zipperMergeTwoQueries( curQueryString, nextQueryString, nextQuery.getQueryMetadata().getOrderByConditions(), colNamesToAdjust, queryNumber, queryNumber+1 ); 

			logger.trace( "Zipper merged the two queries together: {}", curQueryString );

			Collection<QueryMappingEntry> queryMappings = dependencyGraph.getQueryDependencies( nextQueryId );
			int numberOfAdditionalConstants = nextQueryMetadata.getConstantsFromQuery().size();
			logger.trace( "numberOfAdditionalConstants: {}", numberOfAdditionalConstants );
			logger.trace( "AdditionalMappings: {}", queryMappings );
			logger.trace( "start CurrentMappingOffset: {}", currentMappingOffset );

			// Add their query mappings on
			long combinedQueryMappingTimeStart = System.nanoTime()/1000;
			combinedQueryMappings.addAll( adjustMappingOffsets( queryMappings, currentMappingOffset ) );
			long combinedQueryMappingTimeEnd = System.nanoTime()/1000;
			logger.debug( "Time to add mappings: {}", combinedQueryMappingTimeEnd-combinedQueryMappingTimeStart );

			logger.trace( "Prior to duplication, we have {} as our mappings", combinedQueryMappings );
			logger.trace( "Current mapping offset: {}, num additional constants: {}", currentMappingOffset, numberOfAdditionalConstants );
			currentMappingOffset += numberOfAdditionalConstants;

			// duplicate them because we are doing a left join union right join
			combinedQueryMappingTimeStart = System.nanoTime()/1000;
			combinedQueryMappings.addAll( adjustMappingOffsets( combinedQueryMappings, currentMappingOffset ) );
			combinedQueryMappingTimeEnd = System.nanoTime()/1000;
			logger.debug( "Time to dup mappings: {}", combinedQueryMappingTimeEnd-combinedQueryMappingTimeStart );

			logger.trace( "After duplication, we have {} as our mappings", combinedQueryMappings );

			currentMappingOffset *= 2;
			numConstants *= 2;

			logger.trace( "end CurrentMappingOffset: {}", currentMappingOffset );

			// Need to kick these aliases to lower topological levels
			queryMappingAliasAdjustments.put( nextQueryId, colNamesToAdjust );
			
			queryNumber += 2;

			long zipperInternalEnd = System.nanoTime()/1000;
			logger.debug( "Internal Zipper merge loop time: {}", zipperInternalEnd-zipperInternalStart );

		}

		logger.debug( "Zipper merged all of the queries together: {}", curQueryString );
		
		// Enforce ordering by row numbers
		StringBuilder orderBuilder = new StringBuilder();
		orderBuilder.append( curQueryString );
		orderBuilder.append( " ORDER BY " );

		for( int i = 0; i < queryNumber; i ++ ) {
			// Every second i after the first two is a renamed table for syntactic convenience,
			// and has no rn
			if( i % 2 == 0 && i != 0 ) {
				continue;
			}
			if( i > 0 ) {
				orderBuilder.append( ", " );
			}
			orderBuilder.append( "rn" );
			orderBuilder.append( i );
		}


		logger.trace( "Added the order by clause: {}", orderBuilder.toString() );

		// Great, we've zippered together all of these queries, time to adjust the parameter mappings

		String rewrittenQuery = rewriteQueryWithColumnMappings( combinedQueryMappings, orderBuilder.toString(), numConstants );

		rewrittenQuery = qualifyColumnMappings( combinedQueryMappings, rewrittenQuery, plan, changedAliasesFromHigherLevels, numConstants );
		logger.trace( "Done replacing the column names: {}", rewrittenQuery );

		// We need to throw the ordering conditions and their positions up to the higher levels.
		// The only ordering that matters is the rns, in ASC order.
		// We know that the column order for this zippered query is:
		// < - QO's cols ->, RN0, <- Q1's cols ->, RN1, ..., RN_n

		int curPos = 0;
		int queryToMergeNumber = 0;

		List<OrderTypeInformation> orderTypeInfos = new LinkedList<OrderTypeInformation>();

		int rnNumber = 0;
		// Compute the number of columns the zippered query has
		for( QueryIdentifier queryId : queriesToMerge ) {

			// Any instance of the query will do
			int numberOfCols = -1;
			try {
				numberOfCols = getNumberOfSelectedCols( clientId, plan.getQueryShell( queryId ), db );
			} catch( DBException e ) {
				logger.error( "Could not find number of columns for query: {}", plan.getQueryShell( queryId ).getQueryString() );
				return null;
			}
			curPos += numberOfCols;
			OrderTypeInformation orderTypeInfo = new OrderTypeInformation( curPos, OrderDirection.DEFAULT,"rn" + String.valueOf( rnNumber ) );
			curPos++; // skip past this column
			if( queryToMergeNumber == 0 ) {
				rnNumber++;
			} else {
				rnNumber += 2;
			}
			queryToMergeNumber++;
			orderTypeInfos.add( orderTypeInfo );
		}

		logger.trace( "Determined ordering conditions for basequery: {}", orderTypeInfos );
		logger.trace( "After zipping, we think that {} has {} columns.", rewrittenQuery, curPos );
		

		rnNumber = 0;
		List<String> ckFields = new LinkedList<>();
		for( int i = 0; i < queriesToMerge.size(); i++ ) {
			ckFields.add( "rn" + String.valueOf( rnNumber ) );
			if( i == 0 ) {
				rnNumber++;
			} else {
				rnNumber += 2;
			}
		}

		logger.trace( "ckFields: {}", ckFields );

		return new ZipperMergeResult( rewrittenQuery, orderTypeInfos, ckFields, curPos, queryMappingAliasAdjustments, allTableNames );

	}

	/**
	 * Once we know that this row belongs to a particular level, we can divy up the fields among the
	 * queries at that level.
	 */
	public DecodedRowLevelResult decodeRowForQueriesAtLevel(
		Collection<QueryIdentifier> queriesAtThisLevel,
		Map<String, Object> row,
		LateralUnionVectorizationPlan plan,
		int baseUOffset,
		int level
	) {

		logger.debug( "Decoding row {} for {} at level {}", row, queriesAtThisLevel, level );
		logger.debug( "Initial offset for this row is: {}", baseUOffset );
		Map<QueryIdentifier, Map<String,Object>> decodedRows = new HashMap<>();

		// If the level is not zero (or the bottom), then we are shifted by baseUOffset plus how many md keys we have
		if( level > 0 && level != plan.getMaxTopologicalHeight() ) {
			int numNonTypeLUMetadataKeysAtThisLevel = plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( level );
			logger.trace( "We think that there are {} mdKeys at level {}", numNonTypeLUMetadataKeysAtThisLevel, level );
			baseUOffset += numNonTypeLUMetadataKeysAtThisLevel + 1;
		}

		logger.trace( "Base U offset: {}.", baseUOffset );

		// There could be multiple queries at this level if they are zipped.
		for( QueryIdentifier queryId : queriesAtThisLevel ) {
			logger.trace( "Pulling out results for queryId: {}", queryId );

			Query queryAtThisLevel = plan.getQueryShell( queryId );
			AntlrQueryMetadata metadata = queryAtThisLevel.getQueryMetadata();
			List<String> queryColumns = metadata.getSelectedColumnsInQuery();
			Map<Integer, String> queryAliasMap = metadata.getPositionToAliasesMap();

			// TODO: this is duplicated, fix later
			List<String> trueColumnNames = new LinkedList<>();
			int selectedColPosition = 0;
			for( String selectedColName : queryColumns ) {
				String aliasName = queryAliasMap.get( selectedColPosition );
				// Add the alias if we have one, otherwise add the colName
				if( aliasName == null ) {
					trueColumnNames.add( selectedColName );
				} else {
					trueColumnNames.add( aliasName );
				}
				selectedColPosition++;
			}

			queryColumns = trueColumnNames;

			logger.trace( "Found cols {} for this query.", queryColumns );
			
			// These are ordered, but contain columns selected by sub queries as well.
			logger.trace( "Looking for query in column to number map: {}", queryId );
			assert queryToColumnNumberMap.containsKey( queryId );
			logger.trace( "Found {} in queryToColumn map.", queryId );

			// Figure out how many cols this actually has
			int numColsForThisQuery = queryToColumnNumberMap.get( queryId );
			logger.trace( "Pulled out {} cols for {}", numColsForThisQuery, queryId );
			logger.trace( "Columns: {}", queryColumns );


			Iterator<String> queryColumnIterator = queryColumns.iterator();
			Map<String, Object> thisQuerysResultSet = new HashMap<>();

			// Now we've stopped at the right place. Figure out what the original
			// column is, and retrieve that field from u<i>.
			boolean isNullEntry = false;

			for( int i = 0; i < numColsForThisQuery; i++ ) {
				logger.trace( "Looping on {}", i );
				String origColName = queryColumnIterator.next();
				logger.trace( "origColName: {}", origColName );
				String curColName = "u" + String.valueOf( i + baseUOffset );
				logger.trace( "Looking for column name: {}", curColName );
				logger.trace( "Assert row {} contains {}", row, curColName );
				assert row.containsKey( curColName );
				Object val = row.get( curColName );
				if( val == null ) {
					isNullEntry = true;
					break;
				}
				logger.trace( "thisQuerysResultSet: {}", thisQuerysResultSet );
				thisQuerysResultSet.put( origColName, val );
				logger.trace( "thisQuerysResultSet after: {}", thisQuerysResultSet );
			}
			logger.trace( "Found result row {} for query: {}", thisQuerysResultSet, queryId );
			if( !isNullEntry ) {
				decodedRows.put( queryId, thisQuerysResultSet );
			} else {
				decodedRows.put( queryId, null );
			}

			// How many cols we have plus our RN
			baseUOffset += numColsForThisQuery + 1;
		}
		logger.trace( "END BaseUOffset: {}", baseUOffset );

		DecodedRowLevelResult decodedRowLevelResult = new DecodedRowLevelResult( decodedRows, level );

		return decodedRowLevelResult;
	}

	/**
	 * To figure out which query is in charge of this row, we progressively find typeN fields and
	 * check if there is a zero in that field. If so, it belongs to level N. Otherwise, we check N+1
	 * until we hit the bottom level.
	 */
	public RowLevelInformation determineWhichLevelIsInChargeOfRow( Map<String, Object> row, LateralUnionVectorizationPlan plan, int maxDepth ) {

		logger.trace( "Going to determine which level is in charge of row: {}", row );
		Object o = row.get( "type0" );
		assert o != null;
		
		if( (Integer) o == 0 ) {
			// This is a level 0 query
			logger.trace( "Determined this row is for level 0." );
			RowLevelInformation rowLevelInfo = new RowLevelInformation( 0, 0 );
			return rowLevelInfo;
		}

		// Unfortunately, the typeN fields where N > 0 aren't named in U, so we need to count columns
		// to figure where it is.
		int baseUOffset = 0;

		for( int level = 0; level < maxDepth; level++ ) {
			int numNonTypeLUMetadataKeysAtThisLevel = plan.getNumNonTypeLUMetadataKeysAtTopologicalLevel( level );

			// If we aren't at level 0 (and we can't be at max depth) then figure out where our type
			// field is ( its the offset + our orderKeys ).
			if( level > 0 ) {
				int offset = baseUOffset + numNonTypeLUMetadataKeysAtThisLevel;
				logger.trace( "Looking for typeN in field: u{}", offset );
				o = row.get( "u" + String.valueOf( offset ) );
				assert o != null;
				if( (Integer) o == 0 ) {
					logger.trace( "Determined this row is for level {}", level );
					logger.trace( "baseUOffset of this row is: {}", baseUOffset );
					return new RowLevelInformation( level, baseUOffset );
				}
				baseUOffset += numNonTypeLUMetadataKeysAtThisLevel + 1; // offset + mdKeys + 1
			}

			// If we got a 1 for type, then it means this result corresponds to some further inner query.
			// Add the # of cols for this level and keep going. 
			int numColsAtThisLevel = plan.getNumColsAtTopologicalLevel( level );
			logger.trace( "We thinking there are {} cols at topological level {}", numColsAtThisLevel, level );
			baseUOffset += numColsAtThisLevel;
		}

		logger.trace( "Determined this row is for level {}", maxDepth );
		logger.trace( "baseUOffset of this row is: {}", baseUOffset );
		return new RowLevelInformation( maxDepth, baseUOffset );
	}

	/**
	 * Given a result row for the lateral union vectorizer, figure out which level the row belongs
	 * to and assign the right fields to the relevant queryIds.
	 */
	public DecodedRowLevelResult decodeRow( Map<String, Object> row, LateralUnionVectorizationPlan plan ) {

		// analogous
		Integer maxDepth = plan.getMaxTopologicalHeight();
		logger.trace( "Going to decode row: {}", row );

		Map<QueryIdentifier, Map<String,Object>> decodedRowForQueries = new HashMap<>();
		int baseUOffset = 0;

		RowLevelInformation rowLevelInfo = determineWhichLevelIsInChargeOfRow( row, plan, maxDepth );
		DecodedRowLevelResult decodedRowLevelResult = decodeRowForQueriesAtLevel( plan.getTopologicalDepthToQueryIdMap().get( rowLevelInfo.getLevel() ), row, plan, rowLevelInfo.getBaseUOffset(), rowLevelInfo.getLevel() );

		logger.trace( "Decoded Row Level Result: {}", decodedRowLevelResult.getDecodedRowForQueries() );

		return decodedRowLevelResult;
	}

	/**
	 * Split a combined result set from the lateral union vectorizer into result sets for the original queries.
	 */
	public ArrayList<List<QueryResult>> splitApartResultSet( QueryResult combinedResults, QueryVectorizationPlan plan ) {

		/* Split apart a result set that was obtained via lateral union vectorization into individual query result sets.
		 * 
		 * e.g.
		 *
		 ordkey0 | type0 | u0 | u1 | u2 | u3 | u4 | u5 
		---------+-------+----+----+----+----+----+----
		       1 |     0 |  1 |  1 |    |    |    |     <- Belongs to level 0
			   2 |     0 |  2 |  2 |    |    |    |     <- Belongs to level 0
			   2 |     1 |    |    |  1 |  1 | f  |  1  <- Belongs to level 1
			   3 |     0 |  3 |  3 |    |    |    |   
			   3 |     1 |    |    |  2 |  1 | h  |  1
			   3 |     1 |    |    |  1 |  2 | f  |  2
		 *
		 * We can determine which level a row belongs to by finding the typeN fields. If a typeN
		 * field is 0, then the row belongs to the Nth level. Otherwise, we check typeN+1. If we
		 * hit the bottom level, then the row belongs to the bottom level.
         *
         * Once we know what level a row belongs to, we figure out how the original query(ies) map
		 * their fields to u's fields. We pull out those fields, distribute them to the original queries
 		 * and store them.
		 *
		 * We also need to consider which rows belong to which result sets. The logic is that when
		 * are you processing a row at level L, any ongoing result sets you are buliding for levels
		 * L2 > L are done and you should start building a new one. For example, row 3 above belongs
		 * to level 1 and is the only row in the result set for the query instance(s) parameterized by
		 * the results in row 2. We would flush it when we get to row 4, since we go down to level 0,
		 * thus flushing the ongoing result set in level 1. By contrast, row 5 and 6 are part of the
		 * same result set for the query(ies) at level 1, because there isn't a row at a higher level
		 * between them. Once we hit the end of the combined result set, we flush all rows out.
		 */

		assert ( plan instanceof LateralUnionVectorizationPlan );
		List<Map<String,Object>> resultSet = combinedResults.getSelectResult();
		logger.trace( "Splitting apart result Set: {}", resultSet );

		Map<QueryIdentifier, List<List<Map<String,Object>>>> resultSetsForQueries = new HashMap<>();
		Map<QueryIdentifier, List<Map<String,Object>>> ongoingResultSets = new HashMap<>();

		// Pack everything into a queryResultArray
		ArrayList<List<QueryResult>> queryResultArray = new ArrayList<>( plan.size() );
		for( int i = 0; i < plan.size(); i++ ) {
			queryResultArray.add( new LinkedList<QueryResult>() );
		}

		if( plan.size() == 1 ) {
			logger.debug( "Only vectorized one query, returning result set as is!" );
			queryResultArray.get( 0 ).add( combinedResults );
			return queryResultArray;
		}
	

		// Initialize ongoingResultSets
		for( QueryIdentifier queryId : plan.getOrderedQueries() ) {
			List<List<Map<String,Object>>> resultSetList = new LinkedList<>();
			resultSetsForQueries.put( queryId, resultSetList );
			ongoingResultSets.put( queryId, new LinkedList<Map<String,Object>>() );

		}

		logger.trace( "Initialized ongoing result sets." );

		int lastRowLevel = -1;
		for( Map<String, Object> rowInResultSet : resultSet ) {

			logger.trace( "Decoding row: {}", rowInResultSet );
			// Decode row, figure out what level it belongs to
			DecodedRowLevelResult decodedRowLevelResult = decodeRow( rowInResultSet, (LateralUnionVectorizationPlan) plan );
			Map<QueryIdentifier, Map<String, Object>> queryRows = decodedRowLevelResult.getDecodedRowForQueries();
			logger.trace( "Done decoding row: {}", queryRows );
			int rowLevel = decodedRowLevelResult.getLevel();

			// Flush everything with a row level higher than this row level.
			if( lastRowLevel >= rowLevel ) {
				for( int i = rowLevel+1; i <= plan.getMaxTopologicalHeight(); i++ ) {
					Collection<QueryIdentifier> queryIds = plan.getTopologicalDepthToQueryIdMap().get( i );
					for( QueryIdentifier queryId : queryIds ) {
						List<Map<String,Object>> ongoingResultSet = ongoingResultSets.get( queryId );
						resultSetsForQueries.get( queryId ).add( ongoingResultSet );
						ongoingResultSets.put( queryId, new LinkedList<Map<String,Object>>() );
					}
				}
			}

			// add us to the appropriate ongoing result set
			for( Map.Entry<QueryIdentifier, Map<String,Object>> queryRowEntry : queryRows.entrySet() ) {
				Map<String,Object> queryRow = queryRowEntry.getValue();
				assert( queryRow != null );
				if( queryRow != null ) {
					List<Map<String,Object>> queryOngoingResultSet = ongoingResultSets.get( queryRowEntry.getKey() );
					logger.trace( "Going to add row: {} to result set for queryId: {}", queryRow, queryRowEntry.getKey() );
					queryOngoingResultSet.add( queryRow );
					logger.trace( "Current result set for {} is {}", queryRowEntry.getKey(), queryOngoingResultSet );
				}
			}
			lastRowLevel = rowLevel;
		}
		logger.trace( "Out of rows, flush everything down." );

		// Out of rows, flush anything we have built up in the ongoing result sets.
		// N.B. There's an interesting effect here: if we end on a query at level L, we get empty sets
		// for everything at level L2 > L. This is expected, because the inner query would return a row
		// if something matched the instance of the query at level L!
		for( Map.Entry<QueryIdentifier, List<Map<String,Object>>> ongoingResultSetEntry : ongoingResultSets.entrySet() ) {
			List<Map<String,Object>> queryOngoingResultSet = ongoingResultSetEntry.getValue();
			QueryIdentifier queryId = ongoingResultSetEntry.getKey();

			resultSetsForQueries.get( queryId ).add( queryOngoingResultSet );
		}

		int i = 0;
		Iterator<QueryIdentifier> orderedQueryIdIterator = plan.getOrderedQueries().iterator();
		while( orderedQueryIdIterator.hasNext() ) {
			QueryIdentifier queryId = orderedQueryIdIterator.next();
			List<List<Map<String,Object>>> resultSetsForQuery = resultSetsForQueries.get( queryId );
			for( List<Map<String,Object>> resultSetForQuery : resultSetsForQuery ) {
				QueryResult qResult = new QueryResult( resultSetForQuery, combinedResults.getResultVersion() );
				queryResultArray.get( i ).add( qResult );
			}
			i++;
		}

		logger.trace( "Done splitting up query results: {}", queryResultArray );
		return queryResultArray;
	}

}
