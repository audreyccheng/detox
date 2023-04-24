package chronocache.core.parser;

import java.lang.StringBuilder;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class extends the plsqlBaseListener and serves as the basis of walking
 * the parse tree. By maintaining a stack of the important context of each node,
 * the class is able to determine whether each node should be considered a constant,
 * or not. By extending this class it should be easy to perform tasks like query
 * rewriting or hashing of queries, without re-implementing the logic of parsing
 */
public class AntlrListener extends PlSqlParserBaseListener {
	protected Stack<PlSqlTypes> context;

	private Logger log = LoggerFactory.getLogger(this.getClass());

	// additive symbols
	private static char NEGATIVE_SIGN = '-';
	private static char PLUS_SIGN = '+';

	private static ImmutableSet<PlSqlTypes> UNARY_TYPES =
		ImmutableSet.of(PlSqlTypes.UNARY_EXPRESSION);

	private static ImmutableSet<PlSqlTypes> TABLE_TYPES =
		ImmutableSet.of(PlSqlTypes.TABLE_VIEW_NAME);
	private static ImmutableSet<PlSqlTypes> ATOM_TYPES = ImmutableSet.of(PlSqlTypes.ATOM);
	private static ImmutableSet<PlSqlTypes> GURANTEED_CONST_ATOM_TYPES = ImmutableSet.of(PlSqlTypes.QUOTED_STRING);
	private static ImmutableSet<PlSqlTypes> QSH_IGNORE_TYPES =
		ImmutableSet.of(PlSqlTypes.QSH_IGNORE);
	private static ImmutableSet<PlSqlTypes> NON_CONST_ATOM_TYPES =
		ImmutableSet.of(PlSqlTypes.REGULAR_ID, PlSqlTypes.STANDARD_FUNCTION,
				PlSqlTypes.GENERAL_ELEMENT);
	private static ImmutableSet<PlSqlTypes> POSSIBLE_CONST_TYPES =
		ImmutableSet.of(PlSqlTypes.WHERE_CLAUSE, PlSqlTypes.HAVING_CLAUSE,
				PlSqlTypes.VALUES_CLAUSE, PlSqlTypes.JOIN_ON_PART);
	private static ImmutableSet<PlSqlTypes> ID_TYPES = ImmutableSet.of(PlSqlTypes.REGULAR_ID);

	public AntlrListener() {
		context = new Stack<PlSqlTypes>();
	}

	/**
	 * Returns true if a leaf which is some form of atom is considered constant.
	 * This done by ensuring that the context is not a regular_id, standard_function,
	 * or general_element
	 */
	private boolean shouldConsiderAtomConstant() {
		log.trace("checking atom");
		if (contextContains(GURANTEED_CONST_ATOM_TYPES)) {
			return true;
		} else if (contextContains(NON_CONST_ATOM_TYPES)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns true if a node is considered additive. A node is additive if it
	 * is a unary '+' or '-' symbol.
	 * @param node
	 */
	public boolean isNodeAdditive(TerminalNode node) {
		if (node.getText().length() != 1) {
			return false;
		}
		char nodeChar = node.getText().charAt(0);
		if (nodeChar == NEGATIVE_SIGN || nodeChar == PLUS_SIGN) {
			if (contextContains(UNARY_TYPES)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if a leaf can be considered constant. This is done by ensuring
	 * the leaf is an acceptable atom type, and the leaf is in the context of a
	 * where, having, values or join_on clause
	 */
	public boolean shouldConsiderLeafConstant() {
		log.trace("context: {}", context);

		// only want atom's
		if (!contextContains(ATOM_TYPES)) {
			return false;
		}

		if (contextContains(POSSIBLE_CONST_TYPES)) {
			return shouldConsiderAtomConstant();
		}

		return false;
	}

	/**
	 * Returns true if a leaf is part of a table name
	 */
	public boolean isLeafTableName() {
		return contextContains(TABLE_TYPES);
	}

	/**
	 * Returns true if a leaf is a regular id in the context of a SELECT or WHERE
	 */
	public boolean isLeafColumnName() {
		return contextContains(ImmutableSet.of(PlSqlTypes.SELECTED_ELEMENT, PlSqlTypes.WHERE_CLAUSE)) && contextContains(ID_TYPES);
	}

    // Returns true if the leaf is in the contextof a SELECT
	public boolean isLeafSelected() {
	    return contextContains(ImmutableSet.of(PlSqlTypes.SELECTED_ELEMENT));
	}

	/**
	 * Returns true if this node is part of a WHERE clause
	 */
	public boolean isLeafInWhere() {
		return contextContains(ImmutableSet.of(PlSqlTypes.WHERE_CLAUSE));
	}

	/**
	 * Returns true if this node is part of an AS clause
	 */
	public boolean isLeafAlias() {
		return contextContains(ImmutableSet.of(PlSqlTypes.ALIAS)) && contextContains(ID_TYPES);
	}

	/**
	 * Returns true if the context stack contains any element of the toCheck set.
	 * @param toCheck
	 */
	public boolean contextContains(ImmutableSet<PlSqlTypes> toCheck) {
		for (PlSqlTypes stackElem : context) {
			if (toCheck.contains(stackElem)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Pushes toPush onto the context stack
	 * @param toPush
	 */
	private void pushContext(PlSqlTypes toPush) {
		log.trace("pushing {}, context: {}", toPush, context);
		context.push(toPush);
	}

	/**
	 * pops from the context stack and assert the type is as expected
	 * @param expected
	 */
	private void popContext(PlSqlTypes expected) {
		log.trace("popping context: {}, expect: {}", context, expected);
		assert !context.empty();

		PlSqlTypes top = context.peek();
		assert (expected.equals(top));

		context.pop();
	}

	/**
	 * Returns the top of the context stack
	 */
	public PlSqlTypes peekContext() {
		return context.peek();
	}

	// check specific node types
	@Override public void enterSelect_list_elements(@NotNull PlSqlParser.Select_list_elementsContext ctx) {
		pushContext(PlSqlTypes.SELECTED_ELEMENT);
	}

	@Override public void exitSelect_list_elements(@NotNull PlSqlParser.Select_list_elementsContext ctx) {
		popContext(PlSqlTypes.SELECTED_ELEMENT);
	}

	@Override public void enterWhere_clause(@NotNull PlSqlParser.Where_clauseContext ctx) {
		pushContext(PlSqlTypes.WHERE_CLAUSE);
	}

	@Override public void exitWhere_clause(@NotNull PlSqlParser.Where_clauseContext ctx) {
		popContext(PlSqlTypes.WHERE_CLAUSE);
	}

	@Override public void enterRelational_expression(@NotNull PlSqlParser.Relational_expressionContext ctx) {
		pushContext(PlSqlTypes.RELATIONAL_EXPRESSION);
	}

	@Override public void exitRelational_expression(@NotNull PlSqlParser.Relational_expressionContext ctx) {
		popContext(PlSqlTypes.RELATIONAL_EXPRESSION);
	}

	@Override public void enterAtom(@NotNull PlSqlParser.AtomContext ctx) {
		pushContext(PlSqlTypes.ATOM);
	}

	@Override public void exitAtom(@NotNull PlSqlParser.AtomContext ctx) {
		popContext(PlSqlTypes.ATOM);
	}

	@Override public void enterConstant(@NotNull PlSqlParser.ConstantContext ctx) {
		pushContext(PlSqlTypes.CONSTANT);
	}

	@Override public void exitConstant(@NotNull PlSqlParser.ConstantContext ctx) {
		popContext(PlSqlTypes.CONSTANT);
	}

	@Override public void enterRegular_id(@NotNull PlSqlParser.Regular_idContext ctx) {
		pushContext(PlSqlTypes.REGULAR_ID);
	}

	@Override public void exitRegular_id(@NotNull PlSqlParser.Regular_idContext ctx) {
		popContext(PlSqlTypes.REGULAR_ID);
	}

	@Override public void enterHaving_clause(@NotNull PlSqlParser.Having_clauseContext ctx) {
		pushContext(PlSqlTypes.HAVING_CLAUSE);
	}

	@Override public void exitHaving_clause(@NotNull PlSqlParser.Having_clauseContext ctx) {
		popContext(PlSqlTypes.HAVING_CLAUSE);
	}

	@Override public void enterStandard_function(@NotNull PlSqlParser.Standard_functionContext ctx) {
		pushContext(PlSqlTypes.STANDARD_FUNCTION);
	}

	@Override public void exitStandard_function(@NotNull PlSqlParser.Standard_functionContext ctx) {
		popContext(PlSqlTypes.STANDARD_FUNCTION);
	}

	@Override public void enterValues_clause(@NotNull PlSqlParser.Values_clauseContext ctx) {
		pushContext(PlSqlTypes.VALUES_CLAUSE);
	}

	@Override public void exitValues_clause(@NotNull PlSqlParser.Values_clauseContext ctx) {
		popContext(PlSqlTypes.VALUES_CLAUSE);
	}

	@Override public void enterJoin_on_part(@NotNull PlSqlParser.Join_on_partContext ctx) {
		pushContext(PlSqlTypes.JOIN_ON_PART);
	}

	@Override public void exitJoin_on_part(@NotNull PlSqlParser.Join_on_partContext ctx) {
		popContext(PlSqlTypes.JOIN_ON_PART);
	}

	@Override public void enterGeneral_element(@NotNull PlSqlParser.General_elementContext ctx) {
		pushContext(PlSqlTypes.GENERAL_ELEMENT);
	}

	@Override public void exitGeneral_element(@NotNull PlSqlParser.General_elementContext ctx) {
		popContext(PlSqlTypes.GENERAL_ELEMENT);
	}

	@Override public void enterUnary_expression(@NotNull PlSqlParser.Unary_expressionContext ctx) {
		pushContext(PlSqlTypes.UNARY_EXPRESSION);
	}

	@Override public void exitUnary_expression(@NotNull PlSqlParser.Unary_expressionContext ctx) {
		popContext(PlSqlTypes.UNARY_EXPRESSION);
	}

	@Override public void enterTableview_name(@NotNull PlSqlParser.Tableview_nameContext ctx) {
		pushContext(PlSqlTypes.TABLE_VIEW_NAME);
	}

	@Override public void exitTableview_name(@NotNull PlSqlParser.Tableview_nameContext ctx) {
		popContext(PlSqlTypes.TABLE_VIEW_NAME);
	}

	@Override public void enterColumn_alias(@NotNull PlSqlParser.Column_aliasContext ctx) {
		pushContext(PlSqlTypes.ALIAS);
	}

	@Override public void exitColumn_alias(@NotNull PlSqlParser.Column_aliasContext ctx) {
		popContext(PlSqlTypes.ALIAS);
	}

	// Types we care about hashing the specific type

	@Override public void enterNumeric(@NotNull PlSqlParser.NumericContext ctx) {
		pushContext(PlSqlTypes.NUMERIC);
	}

	@Override public void exitNumeric(@NotNull PlSqlParser.NumericContext ctx) {
		popContext(PlSqlTypes.NUMERIC);
	}

	@Override public void enterId_expression(@NotNull PlSqlParser.Id_expressionContext ctx) {
		pushContext(PlSqlTypes.ID_EXPRESSION);
	}

	@Override public void exitId_expression(@NotNull PlSqlParser.Id_expressionContext ctx) {
		popContext(PlSqlTypes.ID_EXPRESSION);
	}

	@Override public void enterQuoted_string(@NotNull PlSqlParser.Quoted_stringContext ctx) {
		pushContext(PlSqlTypes.QUOTED_STRING);
	}

	@Override public void exitQuoted_string(@NotNull PlSqlParser.Quoted_stringContext ctx) {
		popContext(PlSqlTypes.QUOTED_STRING);
	}

    @Override public void enterGroup_by_clause(@NotNull PlSqlParser.Group_by_clauseContext ctx ) {
		pushContext( PlSqlTypes.GROUP_BY_CLAUSE );
	}

    @Override public void exitGroup_by_clause(@NotNull PlSqlParser.Group_by_clauseContext ctx ) {
		popContext( PlSqlTypes.GROUP_BY_CLAUSE );
	}

	@Override public void enterJoin_clause(@NotNull PlSqlParser.Join_clauseContext ctx ) {
		pushContext( PlSqlTypes.EXPLICIT_JOIN );
	}

	@Override public void exitJoin_clause(@NotNull PlSqlParser.Join_clauseContext ctx ) {
		popContext( PlSqlTypes.EXPLICIT_JOIN );
	}

	@Override public void enterLimit_clause(@NotNull PlSqlParser.Limit_clauseContext ctx) {
		pushContext( PlSqlTypes.LIMIT );
	}

	@Override public void exitLimit_clause(@NotNull PlSqlParser.Limit_clauseContext ctx) {
		popContext( PlSqlTypes.LIMIT );
	}

	@Override public void enterOrder_by_clause(@NotNull PlSqlParser.Order_by_clauseContext ctx) {
		pushContext( PlSqlTypes.ORDER_BY );
	}

	@Override public void exitOrder_by_clause(@NotNull PlSqlParser.Order_by_clauseContext ctx) {
		popContext( PlSqlTypes.ORDER_BY );
	}

	@Override public void enterOrder_by_elements(@NotNull PlSqlParser.Order_by_elementsContext ctx) {
		pushContext( PlSqlTypes.ORDER_BY_ELEMENTS );
	}

	@Override public void exitOrder_by_elements(@NotNull PlSqlParser.Order_by_elementsContext ctx) {
		popContext( PlSqlTypes.ORDER_BY_ELEMENTS );
	}

	@Override public void enterSelect_statement(@NotNull PlSqlParser.Select_statementContext ctx) {
		pushContext( PlSqlTypes.SELECT );
	}

	@Override public void exitSelect_statement(@NotNull PlSqlParser.Select_statementContext ctx) {
		popContext( PlSqlTypes.SELECT );
	}

}
