package chronocache.core.parser;

import java.lang.StringBuilder;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectListAliasAdjuster extends PlSqlParserBaseListener {
	private List<String> aliasesToModify;
	private int selectListPosition;
	private int subqueryDepth;
	private TokenStreamRewriter rewriter;
	private boolean shouldModifyNextAlias;
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public SelectListAliasAdjuster( CommonTokenStream tokens, List<String> aliasesToModify ) {
		logger.trace( "Creating selectListAliasAdjuster: {}, {}", tokens.getText(), aliasesToModify );
		rewriter = new TokenStreamRewriter( tokens );
		this.aliasesToModify = aliasesToModify;
		this.selectListPosition = 0;
		this.subqueryDepth = 0;
		this.shouldModifyNextAlias = false;
	}

	@Override public void enterSelected_element(@NotNull PlSqlParser.Selected_elementContext ctx) {
		logger.trace( "selected element!" );
		// We care only about items in the top level select list
		if( subqueryDepth > 1 ) {
			super.enterSelected_element( ctx );
			return;
		}

		String newAliasName = aliasesToModify.get( selectListPosition );
		logger.trace( "ENTER SELECT LIST ALIAS NAME: {}", newAliasName );

		if( newAliasName != null ) {
			int childCount = ctx.getChildCount();

			boolean hasAlias = false;
			for( int i = 0; i < childCount; i++ ) {
				ParseTree childRootedTree = ctx.getChild(i);
				Object subsequentParseRule = childRootedTree.getPayload();
				if( subsequentParseRule instanceof PlSqlParser.Column_aliasContext ) {
					hasAlias = true;
				}
			}

			if( hasAlias ) {
				logger.trace( "Setting modify next alias!" );
				shouldModifyNextAlias = true;
			} else {
				//Otherwise, I need to add my alias in here...
				logger.trace( "Need to add alias." );
				StringBuilder sb = new StringBuilder();
				sb.append( " AS " );
				sb.append( newAliasName );
				rewriter.insertAfter( ctx.getStop(), sb.toString() );
			}

		}
		
	}

	@Override public void exitSelected_element(@NotNull PlSqlParser.Selected_elementContext ctx) {
		// We care only about items in the top level select list
		if( subqueryDepth == 1 ) {
			logger.trace( "incrementing select list pos" );
			selectListPosition++;	
		}
		super.exitSelected_element( ctx );
	}

	@Override public void enterSubquery(PlSqlParser.SubqueryContext ctx) {
		logger.trace( "Incrementing subquery Depth." );
		subqueryDepth++;
		super.enterSubquery( ctx );
	}

	@Override public void exitSubquery(PlSqlParser.SubqueryContext ctx) {
		logger.trace( "Decrementing subquery Depth." );
		subqueryDepth--;
		super.exitSubquery( ctx );
	}

	@Override public void enterColumn_alias(@NotNull PlSqlParser.Column_aliasContext ctx) {
		if( shouldModifyNextAlias ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "AS " );
			sb.append( aliasesToModify.get( selectListPosition ) );
			rewriter.replace( ctx.getStart(), ctx.getStop(), sb.toString() );
			shouldModifyNextAlias = false;
		} else {
			super.enterColumn_alias( ctx );
		}
	}


	@Override public void enterSelect_list_elements(PlSqlParser.Select_list_elementsContext ctx) {
		logger.trace( "Entering select list elements: {}", ctx.getText() );
		super.enterSelect_list_elements( ctx );
	}

	public String getText() {
		return rewriter.getText();
	}

}
