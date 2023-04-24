package chronocache.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.qry.ExecutedQuery;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryStream;

/**
 * The algorithm we use to construct the markov network
 * Should be executed in a thread
 */
public class MarkovConstructor implements Runnable {

	private QueryStream qryStream;
	private Duration delta;
	private MarkovGraph graph;
	private AtomicBoolean stop;
	private Logger log;
	private long qryStreamId;
	protected long clientId;

	private static final AtomicInteger qStreamIdGen = new AtomicInteger(1);

	public MarkovConstructor( long clientId, QueryStream qryStream, Duration delta ) {

		this.clientId = clientId;
		this.qryStream = qryStream;
		this.delta = delta;
		this.graph = new MarkovGraph( clientId );
		this.stop = new AtomicBoolean(false);
		this.log = LoggerFactory.getLogger(this.getClass());
		this.qryStreamId = qStreamIdGen.getAndIncrement();
		this.log.debug("Creating query stream, id {}, with duration {}", qryStreamId, delta );
	}

	/**
	 * Threaded call to actually run the algorithm
	 * Should run forever until we hit some stop condition (yet to be determined)
	 */
	public void run() {
		log.trace( "QueryStream {} - starting to run", qryStreamId );
		while( !stop.get() ){
			ExecutedQuery qry = qryStream.peek();
			if( qry == null ) {
				try {
					log.trace( "Query was null, Query Stream {} sleeping...", qryStreamId );
					Thread.sleep(delta.plus(new Duration(200)).getMillis());
				} catch (InterruptedException e) {
					// Yeah whatever
				}
				continue;
			}
			log.trace( "Waiting to process QID {} in query stream {}", qry.getId().getId(), qryStreamId );

			DateTime intervalEndPoint = qry.getExecutionTime().plus(delta);
			if( intervalEndPoint.isAfter(DateTime.now()) ){
				log.trace("Not enough time has passed, query stream {} sleeping...", qryStreamId );
				//Wait a while longer before we compute for this interval
				//Sleep, rerun
				try {
					Thread.sleep(delta.plus(new Duration(200)).getMillis());
				} catch (InterruptedException e) {
					// Yeah whatever
				}
				log.trace("Query Stream {} woke up, retrying...", qryStreamId);
				continue;
			}
			processExecutedQuery();
		}
		log.trace("Stopping...");
	}
	
	/**
	 * Once we know that enough time has passed, call this to add edges in the markov model
	 * between the queries
	 */
	private void processExecutedQuery() {
		//Pop off first and get list of queries in interval
		ExecutedQuery qry = qryStream.pop();

		// Pre-run queries for profile, don't use.
		if( qry.getId().getId() == -8265232266087599347L ) {
			log.warn( "Dropping query {} from queryStream, pre-run query!", qry.getQueryString() );
			return;
		}
		log.trace("Processing QID {} in query stream {}", qry.getId().getId(), qryStreamId );
		// Maintenance of the query stream's tail
		qryStream.popFromTailBefore( qry.getExecutionTime().minus( delta ) );		

		List<ExecutedQuery> slice = qryStream.getReadOnlyQuerySlice( qry.getExecutionTime().plus( delta ) );

		// Maintenance of the query graph
		log.trace("query stream {}: Increasing counters for node and markov graph...", qryStreamId );
		graph.maintainTransitionsAndMappings(qry, slice);
	}

	public MarkovGraph getGraph() {
		return graph;
	}

	/**
	 * Have the thread terminate safely as soon as possible
	 */
	public void stop() {
		log.trace("Markov constructor stop called.");
		this.stop.set(true);

		// Print graph statistics
		graph.printGraph();
		log.trace("=================================================");
		graph.transformGraph(Parameters.PRUNE_THRESHOLD).print();
		log.trace("=================================================");
		graph.printParameterMappings();
		log.trace("=================================================");
	}

	public Duration getDelta() {
		return delta;
	}
}
