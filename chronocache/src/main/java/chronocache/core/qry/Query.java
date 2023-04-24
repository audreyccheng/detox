package chronocache.core.qry;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import chronocache.core.hashers.PlSqlHasher;
import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.AntlrQueryMetadata;
import chronocache.core.parser.AntlrQueryType;
import chronocache.core.parser.ParserPool;


/**
 * A representation of a query
 *
 * @author bjglasbe
 *
 */
public class Query {

	private String queryString;
	private List<Token> queryTokens;
	private ParseTree parseTree;
	private AntlrQueryMetadata queryMetadata;

	private QueryIdentifier id;
	private String cacheKey;

	private Logger log = LoggerFactory.getLogger( this.getClass() );

	/**
	 * Construct a query object using solely the queryString
	 */
	public Query( String queryString ) {
		this.queryString = queryString;
	
		AntlrParser parser = new AntlrParser();
		try {
			AntlrParser.ParseResult result = parser.buildParseTree( queryString );
			this.parseTree = result.getParseTree();
			this.queryTokens = result.getLexedTokens();
			this.queryMetadata = parser.getQueryMetadata( parseTree );

			setQueryIdentifiers( parser, parseTree, queryMetadata );
		} catch( ParseCancellationException e ) {
			log.error( "Could not parse query {}", queryString );
		}
	}


	/**
	 * Construct a query object using the provided parser
	 */
	public Query( String queryString, AntlrParser parser ) {
		this.queryString = queryString;
		AntlrParser.ParseResult result = parser.buildParseTree( queryString );
		this.parseTree = result.getParseTree();
		this.queryTokens = result.getLexedTokens();
		this.queryMetadata = parser.getQueryMetadata( parseTree );
		setQueryIdentifiers( parser, parseTree, queryMetadata );
	}

	/**
	 * Construct a query object based on the string and a parseTree
	 */
	public Query( String queryString, AntlrParser.ParseResult result ) {
		this.queryString = queryString;
		this.parseTree = result.getParseTree();
		this.queryTokens = result.getLexedTokens();
		AntlrParser parser = new AntlrParser();
		try {
			this.queryMetadata = parser.getQueryMetadata( parseTree );
			setQueryIdentifiers( parser, parseTree, queryMetadata );
		} catch( ParseCancellationException e ) {
			log.error( "Could not parse query {}, reason {}", queryString, e.getMessage() );
		}
	}

	/**
	 * Construct a query object based on the string and a parseTree, using
	 * the attached parser
	 */
	public Query( String queryString, AntlrParser.ParseResult result, AntlrParser parser ) {
		this.queryString = queryString;
		this.parseTree = result.getParseTree();
		this.queryTokens = result.getLexedTokens();
		try {
			this.queryMetadata = parser.getQueryMetadata( parseTree );
			setQueryIdentifiers( parser, parseTree, queryMetadata );
		} catch( ParseCancellationException e ) {
			log.error( "Could not get query metadata for {}, {}", queryString, e.getMessage() );
		}
	}

	/**
	 * Construct a query based on the string, parseTree and metadata
	 */
	public Query( String queryString, AntlrParser.ParseResult result,
				  AntlrQueryMetadata queryMetadata ) {
		this.queryString = queryString;
		this.parseTree = result.getParseTree();
		this.queryTokens = result.getLexedTokens();
		this.queryMetadata = queryMetadata;
		AntlrParser parser = new AntlrParser();

		setQueryIdentifiers( parser, parseTree, queryMetadata );
		ParserPool.getInstance().returnParser( parser );
	}

	/**
	 * Construct a query based on the string, parseTree and metadata, using the
	 * attached parser
	 */
	public Query( String queryString, AntlrParser.ParseResult result,
				  AntlrQueryMetadata queryMetadata, AntlrParser parser ) {
		this.queryString = queryString;
		this.parseTree = result.getParseTree();
		this.queryTokens = result.getLexedTokens();
		this.queryMetadata = queryMetadata;
		setQueryIdentifiers( parser, parseTree, queryMetadata );
	}


	/**
	 * Set query identifiers for the given query using the provided parser
	 */
	public void setQueryIdentifiers( AntlrParser parser, ParseTree parseTree, AntlrQueryMetadata queryMetadata ){
		PlSqlHasher hasher = parser.getQueryHasher( parseTree );
		this.id = new QueryIdentifier( hasher.getHash() );
		long subkey = id.getId();
		this.cacheKey = String.valueOf( queryString.hashCode() );
	}

	public Query(Query q) {
		this.queryString = q.queryString;
		this.parseTree = q.parseTree;
		this.queryMetadata = q.queryMetadata;
		this.queryTokens = q.queryTokens;
		this.id = q.id;
        this.cacheKey = q.cacheKey;
	}

	public String getQueryString() {
		return this.queryString;
	}

	/**
	 * Get the identifier for this query
	 * @return
	 */
	public QueryIdentifier getId() {
		return this.id;
	}

	/**
	 * Get the key we use to look up this query's results in the cache
	 * @return
	 */
	public String getCacheKey(){
		return cacheKey;
	}

	/**
	 * Get the constants provided for this query
	 * NB: (HACK) If this is not a select query, there will be nothing here
	 * TODO: For ease of use there should be a constant extractor here
	 * @return
	 */
	public List<String> getParams() {
		return queryMetadata.getConstantsFromQuery();
	}

	public Set<String> getTables() {
		return queryMetadata.getTablesInQuery();
	}

	/**
	 * Get the parse tree for this query
	 * @return
	 */
	public ParseTree getParseTree() {
		return parseTree;
	}

	/**
	 * Return a boolean indicating whether this is read or write query
	 * @return
	 */
	public boolean isReadQuery(){
		return (queryMetadata.getQueryType()  == AntlrQueryType.SELECT);
	}

	/**
	 * Return the query's metadata
	 */
	public AntlrQueryMetadata getQueryMetadata() {
		return queryMetadata;
	}

	public List<Token> getQueryTokens() {
		return queryTokens;
	}
}
