package chronocache.core.parser;

import java.lang.StringBuilder;
import java.util.LinkedList;
import java.util.List;
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
 * Walks the parse tree and constructs a new query based on the given constants
 **/
public class AntlrQueryConstructor extends AntlrListener {
	private StringBuilder queryBuilder;
	private StringBuilder conditionBuilder;
	private StringBuilder expressionBufferBuilder;
	private StringBuffer columnBuilder;
	private int constantPosition;
	private List<String> newConstants;
	private List<String> uncontainedConditions;
	private List<String> containedConditions;
	private int lastPosition;
	private boolean addToCondition;
	private boolean treatProvidedConstantsAsColumnsToBeQualified;
	private int expressionDepth;
	private boolean hasConstantInExpressionTree;


	// Some large prime out of range of other constants ot avoid hash collisions
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private static String SPACE = String.valueOf(' ');

	public AntlrQueryConstructor(List<String> newConstants) {
		super();
		queryBuilder = new StringBuilder();
		conditionBuilder = new StringBuilder();
		expressionBufferBuilder = new StringBuilder();
		constantPosition = 0;
		this.newConstants = newConstants;
		lastPosition = 0;
		addToCondition = false;
		expressionDepth = 0;
		hasConstantInExpressionTree = false;
		containedConditions = new LinkedList<>();
		uncontainedConditions = new LinkedList<>();
	}

	public AntlrQueryConstructor(List<String> newConstants, boolean treatProvidedConstantsAsColumnsToBeQualified) {
		this(newConstants);
		this.treatProvidedConstantsAsColumnsToBeQualified = treatProvidedConstantsAsColumnsToBeQualified;
	}


	/**
	 * Returns the constructed query, or null if an incorrect number of
	 * constants was provided
	 **/
	public String getConstructedQuery() {
		return queryBuilder.toString();
	}

	/**
	 * Returns the WHERE clause of a query
	 **/
	public String getConditionClause() {
		return conditionBuilder.toString().trim();
	}

	/**
	 * Appends a new constant to the query provided there exists a new constant
	 * to append. If there are no more constants to append, an exception is thrown
	 * @Param node
	 **/
	private void generateNewConstant(TerminalNode node) {
		if( constantPosition == newConstants.size() || constantPosition == -1 ) {
			log.error( "ran out of constants: {}", newConstants );
			throw new IllegalArgumentException( "Ran out of constants while replacing constants with column Names!" );
		} else if( constantPosition != -1 ) {
			log.trace( "adding new constant: {}", newConstants.get( constantPosition ) );
			//Delimit strings, since they don't come back from result sets like so
			String constant = newConstants.get( constantPosition );
			if( node.getSymbol().getType() == PlSqlLexer.CHAR_STRING ) {
				if( constant != null ) {
					constant = "'" + constant + "'";
				}
			}
			if( contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) &&
				contextContains( ImmutableSet.of( PlSqlTypes.WHERE_CLAUSE ) )
			) {
				if( constant != null ) {
					expressionBufferBuilder.append( constant );
				} else {
					expressionBufferBuilder.append( node.getText() );
				}
			} else {
				if( constant != null ) {
					queryBuilder.append( constant );
				} else {
					queryBuilder.append( node.getText() );
				}
			}
			constantPosition++;
		}
	}

	/**
	 * Appends a new column in lieu of a constant provided there exists a new column
	 * to append.
	 */
	private void generateNewColumn( TerminalNode node ) {
		if( constantPosition == newConstants.size() || constantPosition == -1 ) {
			log.error( "ran out of columns: {}", newConstants );
			throw new IllegalArgumentException( "Ran out of columns while replacing constants with column Names!" );
		} else if( constantPosition != -1 ) {
			String constant = newConstants.get( constantPosition );
			if(	contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) &&
				contextContains( ImmutableSet.of( PlSqlTypes.WHERE_CLAUSE ) )
			) {
				if( constant != null ) {
					expressionBufferBuilder.append( "QUALIFYME1." + constant );
				} else {
					expressionBufferBuilder.append( node.getText() );
				}
			} else {
				if( constant != null ) {
					queryBuilder.append( constant );
				} else {
					queryBuilder.append( node.getText() );
				}
			}
			constantPosition++;
		}
	}

	/**
	 * Appends white space to the query as necessary. This is determined by checking
	 * if there was whitespace between the previous token and the current node's
	 * token
	 * @Param node
	 **/
	private void appendSpace(TerminalNode node) {
		Token token = node.getSymbol();
		int tokenStart = token.getStartIndex();
		int tokenStop = token.getStopIndex();
		int whitespaceAmount = tokenStart - lastPosition - 1;
		if (whitespaceAmount > 0) {
			if( contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) &&
				contextContains( ImmutableSet.of( PlSqlTypes.WHERE_CLAUSE ) ) 
			){
				expressionBufferBuilder.append( Strings.repeat(SPACE, whitespaceAmount) );
			} else {
				queryBuilder.append(Strings.repeat(SPACE, whitespaceAmount));
			}

		}
		lastPosition = tokenStop;
	}


	/**
	 * Updates the constructed query with the given node. If the node is a constant
	 * node, it will replace it with the new constant, if it is an additive operation,
	 * nothing is appended, otherwise, the nodes token is used as part of the new query
	 * @Param node
	 **/
	private void buildQueryFromNode(TerminalNode node) {
		if (node.getSymbol().getType() == Token.EOF) {
			return;
		}
		appendSpace(node);
		if (super.shouldConsiderLeafConstant()) {
			if( contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) &&
				contextContains( ImmutableSet.of( PlSqlTypes.WHERE_CLAUSE ) )
			) {
				hasConstantInExpressionTree = true;
			}
			if (treatProvidedConstantsAsColumnsToBeQualified) {
				generateNewColumn(node);
			} else {
				generateNewConstant(node);
			}
		} else if (super.isNodeAdditive(node)) {
			log.trace("additive op node: {}", node.getText());
		} else if (super.isLeafColumnName()) {
			log.trace( "leaf column name: {}", node.getText() );

			if( contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) &&
				contextContains( ImmutableSet.of( PlSqlTypes.WHERE_CLAUSE ) ) ) {
				if( treatProvidedConstantsAsColumnsToBeQualified ) {
					expressionBufferBuilder.append( "QUALIFYME0." + node.getText() );
				} else {
					expressionBufferBuilder.append( node.getText() );
				}

			} else {
				queryBuilder.append( node.getText() );
			}
		} else {
			log.trace("regular node: {}", node.getText());
			if( contextContains( ImmutableSet.of( PlSqlTypes.RELATIONAL_EXPRESSION ) ) &&
				contextContains( ImmutableSet.of( PlSqlTypes.WHERE_CLAUSE ) ) ) {
				expressionBufferBuilder.append( node.getText() );
			} else {
				queryBuilder.append( node.getText() );
			}
		}
	}

	
	/**
	 * Visits every node, and updates the constructed query with the node if it is a leaf
	 * @param node
	 */
	@Override public void visitTerminal(@NotNull TerminalNode node) {
		if (node.getChildCount() == 0) {
			log.trace("leaf node: {}", node.getText());
			buildQueryFromNode(node);
		}
	}

	@Override public void enterWhere_clause(@NotNull PlSqlParser.Where_clauseContext ctx) {
		super.enterWhere_clause(ctx);
		addToCondition = true;
	}

	@Override public void exitWhere_clause(@NotNull PlSqlParser.Where_clauseContext ctx) {
		super.exitWhere_clause(ctx);
		addToCondition = false;
	}

	@Override public void enterRelational_expression(@NotNull PlSqlParser.Relational_expressionContext ctx) {
		super.enterRelational_expression(ctx);
		expressionDepth++;
	}

	@Override public void exitRelational_expression(@NotNull PlSqlParser.Relational_expressionContext ctx) {
		super.exitRelational_expression(ctx);
		expressionDepth--;
		if( expressionDepth == 0 ) {
			String expr = expressionBufferBuilder.toString();
			if( hasConstantInExpressionTree ) {
				if( addToCondition ) {
					conditionBuilder.append( expr );
				}
				queryBuilder.append( expr );
				uncontainedConditions.add( expr );
			} else {
				expr = expr.replaceAll( "QUALIFYME[0-9]*\\.", "");
				queryBuilder.append( expr );
				if( contextContains( ImmutableSet.of( PlSqlTypes.WHERE_CLAUSE ) ) ) {;
					log.debug( "Found contained condition: {}", expr );
					containedConditions.add( expr );
				}
			}

			expressionBufferBuilder = new StringBuilder();
			hasConstantInExpressionTree = false;
		}
	}

	public List<String> getContainedConditions() {
		return containedConditions;
	}

	public List<String> getUncontainedConditions() {
		return uncontainedConditions;
	}

}
