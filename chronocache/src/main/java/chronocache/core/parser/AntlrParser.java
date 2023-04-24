package chronocache.core.parser;

import java.util.List;

import com.google.common.collect.ImmutableSet;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.hashers.PlSqlHasher;
import chronocache.core.parser.AntlrQueryConstructor;

/**
 * A wrapper around operations on queries such as constructing their hash
 * and replacing constants
 **/
public class AntlrParser {

	public class ExtractedConditions {
		private List<String> containedConditions;
		private List<String> uncontainedConditions;

		public ExtractedConditions( List<String> containedConditions, List<String> uncontainedConditions ) {
			this.containedConditions = containedConditions;
			this.uncontainedConditions = uncontainedConditions;
		}

		public List<String> getContainedConditions() {
			return containedConditions;
		}

		public List<String> getUncontainedConditions() {
			return uncontainedConditions;
		}
	}

	public class ParseResult {
		private ParseTree parseTree;
		private List<Token> lexedTokens;

		public ParseResult( ParseTree parseTree, List<Token> lexedTokens ) {
			this.parseTree = parseTree;
			this.lexedTokens = lexedTokens;
		}

		public ParseTree getParseTree() {
			return parseTree;
		}

		public List<Token> getLexedTokens() {
			return lexedTokens;
		}
	}

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public AntlrParser() {}

	/**
	 * Constructs a parse tree of a given string representing a sql statement
	 * this parse tree is constructed using the antlr generated lexer and parser
	 * @Param sql
	 **/
	public ParseResult buildParseTree( String sql ) throws ParseCancellationException {
		log.trace( "building parse tree of: {}", sql );

		CharStream stream = new ANTLRInputStream( sql );
		CaseChangingCharStream upperStream = new CaseChangingCharStream( stream, true );
		PlSqlLexer lexer  = new PlSqlLexer( upperStream );
		CommonTokenStream tokenStream = new CommonTokenStream( lexer );
		PlSqlParser parser = new PlSqlParser( tokenStream );
		parser.setErrorHandler( new BailErrorStrategy() );

		ParseTree tree = parser.dml_compilation_unit();

		// Reset tokenStream position, extract all tokens
		tokenStream.reset();
		List<Token>tokens = tokenStream.getTokens();

		ParseResult result = new ParseResult( tree, tokens );
		return result;
	}

	public AntlrQueryMetadata getQueryMetadata( ParseTree tree ) {
		log.trace( "getting query metadata of: {}", tree.getText() );
		ParseTreeWalker walker = new ParseTreeWalker();

		AntlrQueryMetadata queryMetadata = new AntlrQueryMetadata();
		walker.walk(queryMetadata, tree);

		return queryMetadata;
	}

	/**
	 * Walks the parse tree and constructs a structure which holds the hash of
	 * the parse tree, as well as the ignored hash of the parse tree. This hash
	 * will be the same for queries that have the same query shell. That is, they
	 * differ only in constants. The ignored hash is the hash of all the contents
	 * within qsh_ignore
	 * @Param tree
	 **/
	public PlSqlHasher getQueryHasher(ParseTree tree) {
		log.trace("getting query hash of: {}", tree.getText());
		ParseTreeWalker walker = new ParseTreeWalker();

		PlSqlHasher hasher = new PlSqlHasher();
		walker.walk(hasher, tree);
		return hasher;
	}

	/**
	 * Walks the parse tree and constructs a hash of the parse tree. This hash
	 * will be the same for queries that have the same query shell. That is, they
	 * differ only in constants
	 * @Param tree
	 **/
	public long getQueryHash(ParseTree tree) {
		PlSqlHasher hasher = getQueryHasher(tree);
		return hasher.getHash();
	}

	/**
	 * Walks the parse tree and replaces the constants with the updatedConstants
	 * to generate a new query. If there are an incorrect number of constants, the
	 * returned string is null when useVerbatim is false
	 * @Param tree, updatedConstants
	 **/
	public String replaceQueryShellConstants(ParseTree tree, List<String> updatedConstants, boolean useVerbatim) {
		log.trace("replacing query shell constants of {} with {}", tree.getText(), updatedConstants);
		ParseTreeWalker walker = new ParseTreeWalker();

		AntlrQueryConstructor constructor = new AntlrQueryConstructor(updatedConstants, useVerbatim);
		walker.walk(constructor, tree);

		String constructedQuery = constructor.getConstructedQuery();

		log.trace("replacing query shell constants of {} with {} and got {}",
				tree.getText(), updatedConstants, constructedQuery);

		if (constructedQuery == null) {
			log.warn( "Could not replace constants in \"{}\" with {} constants",
					tree.getText(), updatedConstants.size());
		}

		return constructedQuery;
	}


	/**
	 * Determine what the join conditions should be.
	 *
	 * We walk the parse tree, and check if within a given relationalExpression if there is a constant.
	 * if there are no constants within a relational expression, then we don't extract it as a
	 * join condition
	 * @Param tree, updatedConstants
	 **/
	public ExtractedConditions getCTEJoinConditions(ParseTree tree, List<String> updatedConstants, boolean useVerbatim) {
		ParseTreeWalker walker = new ParseTreeWalker();

		AntlrQueryConstructor constructor = new AntlrQueryConstructor( updatedConstants, useVerbatim );
		walker.walk( constructor, tree );

		String constructedQuery = constructor.getConditionClause();

		log.trace( "Contained Conditions: {}", constructor.getContainedConditions() );
		log.trace( "Uncontained Conditions: {}", constructor.getUncontainedConditions() );

		if (constructedQuery == null) {
			log.warn( "Could not replace constants in \"{}\" with {} constants",
					tree.getText(), updatedConstants.size());
		}
		ExtractedConditions ec = new ExtractedConditions( constructor.getContainedConditions(), constructor.getUncontainedConditions() );

		return ec;
	}
}
