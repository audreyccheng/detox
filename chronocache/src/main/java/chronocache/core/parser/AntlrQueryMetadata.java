package chronocache.core.parser;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.parser.AntlrListener;

/**
 * Walks the parse tree to track query metadata like the query type, constants
 * in the query, and the tables used in the query
 **/

public class AntlrQueryMetadata extends AntlrListener {
	private StringBuilder constantBuilder;
	private StringBuilder tableBuilder;
	private StringBuilder aliasBuilder;
	private StringBuilder selectedColumnBuilder;
	private StringBuilder conditionalColumnBuilder;
	private StringBuilder orderByBuilder;
	private List<String> extractedConstants;
	private Set<String> tables;
	private List<String> aliases;
	private Map<String, Integer> aliasesToPositionMap;
	private Map<Integer, String> positionToAliasesMap;
	private List<String> selectedColumns;
	private List<String> uncontainedConditionColumns;
	private List<String> columnsInThisExpressionTree;
	private List<String> orderByConditions;
	private int expressionDepth;
	private boolean hasConstantInExpressionTree;
	private boolean isSimpleVectorizable;
	private boolean hasUnion;
	private AntlrQueryType queryType;
	private int lastPosition;
	private int selectedColPosition;

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public AntlrQueryMetadata() {
		constantBuilder = new StringBuilder();
		orderByBuilder = new StringBuilder();
		extractedConstants = new LinkedList<>();
		tables = new HashSet<>();
		aliases = new LinkedList<>();
		aliasesToPositionMap = new HashMap<>();
		positionToAliasesMap = new HashMap<>();
		selectedColumns = new LinkedList<>();
		uncontainedConditionColumns = new LinkedList<>();
		columnsInThisExpressionTree = new LinkedList<>();
		orderByConditions = new LinkedList<>();
		expressionDepth = 0;
		hasConstantInExpressionTree = false;
		isSimpleVectorizable = true;
		hasUnion = false;
		queryType = null;
		lastPosition = 0;
		selectedColPosition = 0;
	}

	/**
	 * Returns an ordered list of the constants in this query
	 */
	public List<String> getConstantsFromQuery() {
		return extractedConstants;
	}

	/**
	 * Check if this query can be vectorized by the simple vectorizer (i.e it is a flat select project with only cartesian products).
	 */
	public boolean isSimpleVectorizable() {
		return isSimpleVectorizable;
	}

	public boolean hasUnion() {
		return hasUnion;
	}

	/**
	 * Returns the set of tables that are used in this query
	 */
	public Set<String> getTablesInQuery() {
		return tables;
	}

	/**
	 * Returns the set of column aliases used in this query
	 */
	public List<String> getAliasesInQuery() {
		return aliases;
	}

	/**
	 * Returns the ordered set of columns that are used in this query
	 */
	public List<String> getSelectedColumnsInQuery() {
		return selectedColumns;
	}

	/**
	 * Returns the ordered set of order by conditions that are used in this query.
	 */
	public List<String> getOrderByConditions() {
		return orderByConditions;
	}

	// Returns the ordered set of columns used in this query's conditionals
	public List<String> getUncontainedConditionalColumnsInQuery() {
		return uncontainedConditionColumns;
	}

	/**
	 * Returns the type of the query: SELECT, INSERT, UPDATE, DELETE
	 */
	public AntlrQueryType getQueryType() {
		return queryType;
	}


	/**
	 * Updates the query type to type if it has not already been set
	 * Only ever want the query type to change once, so that nested selects
	 * don't change the type to select
	 * @Parm type
	 */
	private void setQueryTypeIfNull(AntlrQueryType type) {
		if (queryType == null) {
			log.trace("Setting query type to {}", type);
			queryType = type;
		}
	}

	/**
	 * Appends the constant node's text to the constant list
	 * @Parm node
	 */
	private void addConstant(TerminalNode node) {
		constantBuilder.append(node.getText());
		extractedConstants.add(constantBuilder.toString());
		log.trace("Adding constant {}", constantBuilder.toString());
		if( contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) ) {
			hasConstantInExpressionTree = true;
		}
		constantBuilder = new StringBuilder();
	}

	/**
	 * Appends the unary operands node to the next constant
	 * @Parm node
	 */
	private void addAdditive(TerminalNode node) {
		constantBuilder.append(node.getText());
	}

	/**
	 * Appends the node's text to the current table name
	 * @Parm node
	 */
	private void addTable(TerminalNode node) {
		tableBuilder.append(node.getText());
	}

	// Appends the node's text to the current alias name
	private void addAlias(TerminalNode node) {
		aliasBuilder.append(node.getText());
	}

	// Appends the node's text to the current column name
	private void addColumn(TerminalNode node) {
		if (super.isLeafSelected()) {
			selectedColumnBuilder.append(node.getText());
		} else {
			conditionalColumnBuilder.append(node.getText());
		}
	}

	/**
	 * Updates any state (table name, or constants) based on the current node's
	 * type
	 * @Parm node
	 */
	private void updateLeafNode(TerminalNode node) {
		// Quick exit
		if (node.getSymbol().getType() == Token.EOF) {
			return;
		}

		if( contextContains( ImmutableSet.of( PlSqlTypes.ORDER_BY_ELEMENTS ) ) ) {
			if( orderByBuilder.length() > 0 ) {
				appendSpace( node, orderByBuilder );
			}
			orderByBuilder.append( node.getText() );
		}

		// Depending on what the leaf is, add to the metadata
		if (super.shouldConsiderLeafConstant()) {
			addConstant(node);
		} else if (super.isNodeAdditive(node)) {
			addAdditive(node);
		} else if (super.isLeafTableName()) {
			addTable(node);
		} else if (super.isLeafColumnName()) {
			addColumn(node);
		} else if (super.isLeafAlias()) {
			addAlias(node);
		}

		// Update the position of the last token which is used to get exact
		// strings for clauses and sub-clauses
		lastPosition = node.getSymbol().getStopIndex();
	}

	/**
	 * Appends white space to the query as necessary. This is determined by checking
	 * if there was whitespace between the previous token and the current node's
	 * token
	 * @Param node
	 **/
	private void appendSpace(TerminalNode node, StringBuilder sb) {
		Token token = node.getSymbol();
		int tokenStart = token.getStartIndex();
		int tokenStop = token.getStopIndex();
		int whitespaceAmount = tokenStart - lastPosition - 1;
		if (whitespaceAmount > 0) {
			sb.append(Strings.repeat(" ", whitespaceAmount));
		}
		lastPosition = tokenStop;
	}

	/**
	 * Visits every node, and if it is a leaf use the node information to update
	 * query metadata
	 * @Param node
	 */
	@Override public void visitTerminal(@NotNull TerminalNode node) {
		if (node.getChildCount() == 0) {
			log.trace("leaf node: {}", node.getText());
			updateLeafNode(node);
		}
	}

	/**
	 * Upon entering a tableview_name, reset the table name buffer
	 * @Param ctx
	 */
	@Override public void enterTableview_name(@NotNull PlSqlParser.Tableview_nameContext ctx) {
		super.enterTableview_name(ctx);

		tableBuilder = new StringBuilder();
	}

	/**
	 * Upon exiting a tableview_name, add the table name to the set of tables
	 * @Param ctx
	 */
	@Override public void exitTableview_name(@NotNull PlSqlParser.Tableview_nameContext ctx) {
		super.exitTableview_name(ctx);

		tables.add(tableBuilder.toString());
		log.trace("Updating table {}", tableBuilder.toString());
	}

	// Upon entering an alias, reset the alias name buffer
	@Override public void enterColumn_alias(@NotNull PlSqlParser.Column_aliasContext ctx) {
		super.enterColumn_alias(ctx);

		aliasBuilder = new StringBuilder();
	}

	// Upon exiting an alias, clear the buffer and store the alias
	@Override public void exitColumn_alias(@NotNull PlSqlParser.Column_aliasContext ctx) {
		super.exitColumn_alias(ctx);
		String alias = aliasBuilder.toString();
		aliases.add( alias );
		// This is a constant selected as a column
		if( selectedColumns.size() <= selectedColPosition ) {
			selectedColumns.add( alias );
		}
		aliasesToPositionMap.put( alias, selectedColPosition );
		positionToAliasesMap.put( selectedColPosition, alias );
	}

	// Upon entering a regular_id, check if this is a column name
	@Override public void enterRegular_id(PlSqlParser.Regular_idContext ctx) {
		super.enterRegular_id(ctx);
		
		selectedColumnBuilder = new StringBuilder();
		conditionalColumnBuilder = new StringBuilder();
	}

	// Upon leaving a regular_id, add to column names if the identifier isn't a table/alias
	@Override public void exitRegular_id(PlSqlParser.Regular_idContext ctx) {
		super.exitRegular_id(ctx);

		String regularId = selectedColumnBuilder.toString();
		if (regularId.length() > 0 &&
			!tables.contains(regularId) &&
			!aliases.contains(regularId) &&
			!contextContains( ImmutableSet.of( PlSqlTypes.ORDER_BY ) )
		) {
			selectedColumns.add(regularId);
			log.trace("Updating selected columns {}", regularId);
		}


		// Then the column is in here
		regularId = conditionalColumnBuilder.toString();

		// If a column was already selected don't add it to the extra conditional columns
		if (regularId.length() > 0 && !tables.contains(regularId) && !aliases.contains(regularId) && !selectedColumns.contains(regularId) && !uncontainedConditionColumns.contains(regularId) ) {
			if( contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) ) {
				columnsInThisExpressionTree.add( regularId );
			}
			log.trace("Updating conditional columns {}", regularId);
		}
	}

	// Rules for entering specific statements
	@Override public void enterSelect_statement(@NotNull PlSqlParser.Select_statementContext ctx) {
		super.enterSelect_statement(ctx);
		setQueryTypeIfNull(AntlrQueryType.SELECT);
	}
	@Override public void enterUpdate_statement(@NotNull PlSqlParser.Update_statementContext ctx) {
		super.enterUpdate_statement(ctx);
		setQueryTypeIfNull(AntlrQueryType.UPDATE);
	}
	@Override public void enterDelete_statement(@NotNull PlSqlParser.Delete_statementContext ctx) {
		super.enterDelete_statement(ctx);
		setQueryTypeIfNull(AntlrQueryType.DELETE);
	}
	@Override public void enterInsert_statement(@NotNull PlSqlParser.Insert_statementContext ctx) {
		super.enterInsert_statement(ctx);
		setQueryTypeIfNull(AntlrQueryType.INSERT);
	}

	@Override public void enterRelational_expression(@NotNull PlSqlParser.Relational_expressionContext ctx) {
		super.enterRelational_expression(ctx);
		expressionDepth++;
	}

	@Override public void exitRelational_expression(@NotNull PlSqlParser.Relational_expressionContext ctx) {
		super.exitRelational_expression(ctx);
		expressionDepth--;
		if( expressionDepth == 0 ) {
			if( hasConstantInExpressionTree ) {
				for( String col : columnsInThisExpressionTree ) {
					uncontainedConditionColumns.add( col );
				}
			}
			columnsInThisExpressionTree.clear();
			hasConstantInExpressionTree = false;
		}
	}

    @Override public void enterGroup_by_clause(@NotNull PlSqlParser.Group_by_clauseContext ctx ) {
		super.enterGroup_by_clause( ctx );
		isSimpleVectorizable = false;
	}

    @Override public void enterSubquery_operation_part(@NotNull PlSqlParser.Subquery_operation_partContext ctx ) {
		super.enterSubquery_operation_part( ctx );
		hasUnion = true;
	}


	@Override public void enterJoin_clause(@NotNull PlSqlParser.Join_clauseContext ctx ) {
		super.enterJoin_clause( ctx );
		isSimpleVectorizable = false;
	}

	@Override public void enterStandard_function(@NotNull PlSqlParser.Standard_functionContext ctx ) {
		super.enterStandard_function( ctx );
		isSimpleVectorizable = false;
		
	}

	@Override public void enterLimit_clause(@NotNull PlSqlParser.Limit_clauseContext ctx) {
		super.enterLimit_clause( ctx );
		isSimpleVectorizable = false;
	}

	@Override public void enterOrder_by_clause(@NotNull PlSqlParser.Order_by_clauseContext ctx) {
		super.enterOrder_by_clause( ctx );
		isSimpleVectorizable = false;
	}

	@Override public void enterOrder_by_elements(PlSqlParser.Order_by_elementsContext ctx) {
		orderByBuilder = new StringBuilder();
		super.enterOrder_by_elements( ctx );
	}

	public int getQueryDepth( Stack<PlSqlTypes> context ) {
		int count = 0;
		for( PlSqlTypes type : context ) {
			if( type == PlSqlTypes.SELECT ) {
				count++;
			}
		}
		return count;
	}

	@Override public void exitOrder_by_elements(PlSqlParser.Order_by_elementsContext ctx) {
		//We care only about the top-level order-bys
		if( getQueryDepth( context ) == 1 ) {
			orderByConditions.add( orderByBuilder.toString().trim() );
		}
		super.exitOrder_by_elements( ctx );
	}


	@Override public void exitSelected_element(PlSqlParser.Selected_elementContext ctx) {
		super.exitSelected_element( ctx );
		selectedColPosition++;
	}

	public Map<String, Integer> getAliasesToPositionMap() {
		return aliasesToPositionMap;
	}

	public Map<Integer, String> getPositionToAliasesMap() {
		return positionToAliasesMap;
	}


}
