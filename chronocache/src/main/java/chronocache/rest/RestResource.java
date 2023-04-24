package chronocache.rest;

import java.io.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.qry.QueryResult;
import chronocache.db.DB;
import chronocache.db.DBFactory;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Path("/query")
public class RestResource {

	private static Logger logger = LoggerFactory.getLogger( RestResource.class );
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final DB db = new DBFactory().getDBInstance( DBFactory.DBType.REAL_DB );

	@POST
	public Response resetCacheStats() {
		db.resetCacheStats();
		return Response.ok().build();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getCacheStats() {
		int hits = db.getCacheHits();
		int miss = db.getCacheMiss();
		return Response.ok(String.format("Hits: %d, Misses: %d", hits, miss) ).build();
	}

	@POST
	@Path("/{terminalID}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response query( String queryString, @PathParam("terminalID") long terminalID ) throws IOException {

		logger.info( "Got query string {} from clientId {}", queryString, terminalID );

		QueryResult result;
		try {
			ObjectNode node = mapper.readValue( queryString, ObjectNode.class );
			String query = node.get( "query" ).getValueAsText();
			long start = System.nanoTime() / 1000;
			result = db.query( terminalID, query );
			long end = System.nanoTime() / 1000;
			logger.info( "Query execution time for query \"{}\": {}", query, end - start );
		} catch (Exception e) {
			logger.error( "Encountered problem: {}", e.getMessage() );
			logger.error( "Problem stacktrace {}", e.getStackTrace() );
			logger.error( "Error {}", e );
			return Response.serverError().entity( "ERROR: " + e.getMessage() ).build();
		}
		if( result.isSelect() ) {
			logger.info( "Returning rest ok" );
			return Response.ok(result.getSelectResultWithCachingInfo()).build();
		} else {
			logger.info( "Returning rest ok" );
			// then it means it is an update result - just an integer
			return Response.ok( Integer.toString( result.getUpdateResult() ) ).build();
		}
	}
}
