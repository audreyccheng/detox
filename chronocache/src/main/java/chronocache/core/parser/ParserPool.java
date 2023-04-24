package chronocache.core.parser;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.util.ObjectPool;
import chronocache.core.Parameters;

public class ParserPool {

	private static ParserPool instance = new ParserPool();

	private Logger logger = LoggerFactory.getLogger( ParserPool.class );
	private ObjectPool<AntlrParser> parserPool;

	private ParserPool() {
		List<AntlrParser> poolContents = new LinkedList<>();
		String initialQuery = "SELECT 2 FROM tab";
		for( int i = 0; i < Parameters.PARSER_POOL_SIZE; i++ ) {
			//Create parser and parse object to initialize internal ANTLR structs
			//First parse is slowest, so get it out of the way during boot
			AntlrParser p = new AntlrParser();
			p.buildParseTree( initialQuery );
			poolContents.add( p );
		}
		parserPool = new ObjectPool<>( poolContents );

	}

	public static ParserPool getInstance() {
		return instance;
	}

	public AntlrParser getParser() {
		logger.trace("Trying to get a parser...");
		AntlrParser parser = parserPool.borrow();
		logger.trace("Parser acquired...");
		return parser;
	}

	public void returnParser( AntlrParser p ) {
		logger.trace("Returning parser...");
		parserPool.returnObj( p );
	}

}
