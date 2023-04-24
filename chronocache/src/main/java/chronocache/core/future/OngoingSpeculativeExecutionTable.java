package chronocache.core.future;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import chronocache.core.VersionVector;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains a list of ongoing speculative executions in the Engine
 * Before performing an execution, we should look it up in here to see
 * if there is an instance of that query already running
 * 
 * @author bjglasbe
 *
 */
public class OngoingSpeculativeExecutionTable {

	private Logger logger = LoggerFactory.getLogger(OngoingSpeculativeExecutionTable.class);

	private class ExecutingQueryEntry {
		public String queryString;
		public VersionVector version;
		public boolean isWriteQuery;
		public ExecutingQueryEntry( String queryString, VersionVector version, boolean isWriteQuery ) {
			this.queryString = queryString;
			this.version = version;
			this.isWriteQuery = isWriteQuery;
		}

		@Override
			public int hashCode() {
				int hash = 1;
				hash = hash + 17 * queryString.hashCode();
				hash = hash + 31 * version.hashCode();
				logger.trace( "{} has hash {}", toString(), hash );
				return hash;
			}

		@Override
		public boolean equals( Object o ) {
			if( o instanceof ExecutingQueryEntry ) {
				ExecutingQueryEntry other = (ExecutingQueryEntry) o;
				if( this.queryString.equals( other.queryString ) &&
				 	this.version.equals( other.version ) ) {
					return true;
				}
			}
			return false;
		}
		@Override
		public String toString() {
			return queryString + "@" + version;
		}

		public String getQueryString() {
			return queryString;
		}

		public boolean isWriteQuery() {
			return isWriteQuery;
		}

	}

	private ConcurrentHashMap<Long, ConcurrentLinkedQueue<ExecutingQueryEntry>> clientIdsExecutingSpecQueries;
	private ConcurrentHashMap<ExecutingQueryEntry, ConcurrentLinkedQueue<ResultRegistration<QueryResult>>> threadsWaitingForResults;
	
	public OngoingSpeculativeExecutionTable() {
		clientIdsExecutingSpecQueries = new ConcurrentHashMap<>();
		threadsWaitingForResults = new ConcurrentHashMap<>();
	}

	public boolean isWriteQuery( String queryString ) {
		return queryString.startsWith( "INSERT" ) || queryString.startsWith( "UPDATE" ) || queryString.startsWith( "DELETE" );
	}
	
	public ConcurrentLinkedQueue<ExecutingQueryEntry> getOrCreateClientQueue( long clientId ) {
		ConcurrentLinkedQueue<ExecutingQueryEntry> queue = clientIdsExecutingSpecQueries.get( clientId );
		if( queue == null ) {
			queue = new ConcurrentLinkedQueue<ExecutingQueryEntry>();
			ConcurrentLinkedQueue<ExecutingQueryEntry> queue2 = clientIdsExecutingSpecQueries.putIfAbsent( clientId, queue );
			if( queue2 != null ) {
				return queue2;
			}
		}
		return queue;
	}

	public ConcurrentLinkedQueue<ResultRegistration<QueryResult>> getOrCreateExecutingQueryQueue( ExecutingQueryEntry entry ) {
		ConcurrentLinkedQueue<ResultRegistration<QueryResult>> queue = threadsWaitingForResults.get( entry );
		if( queue == null ) {
			queue = new ConcurrentLinkedQueue<ResultRegistration<QueryResult>>();
			ConcurrentLinkedQueue<ResultRegistration<QueryResult>> queue2 = threadsWaitingForResults.putIfAbsent( entry, queue );
			if( queue2 != null ) {
				return queue2;
			}
		}
		return queue;
	}


	/**
	 * Drain the prediction queue for a client.
	 * The iterator is weakly consistent, so it is only guaranteed to see things in the queue at the time of
	 * of the iterator's creation. Anything after this point is not guaranteed.
	 */
	public void drainPredictionQueue( ConcurrentLinkedQueue<ExecutingQueryEntry> queue ) {
		Iterator<ExecutingQueryEntry> queueIterator = queue.iterator();
		logger.trace( "Draining the prediction queue." );
		while( queueIterator.hasNext() ) {
			ExecutingQueryEntry queryEntry = queueIterator.next();

			// We are the only write, skip us
			if( !queryEntry.isWriteQuery() ) {
				ConcurrentLinkedQueue<ResultRegistration<QueryResult>> allWaiters = threadsWaitingForResults.get( queryEntry );
				// Technically, we need to wait only for the entry that is our client, but everyone
				// should drain around the same time
				if( allWaiters != null ) {
					Iterator<ResultRegistration<QueryResult>> waitersIterator = allWaiters.iterator();
					while( waitersIterator.hasNext() ) {
						ResultRegistration<QueryResult> reg = waitersIterator.next();
						logger.trace( "Waiting on reg: {} to drain", reg );
						reg.waitUntilResultReady();
					}
				}
			}
		}
		logger.trace( "Done draining." );
	}

	/**
	 * Check if the prediction queue for this client has a write.
	 * Iterator is weakly consistent, so we may not observe things added to the queue later.
	 */
	public ExecutingQueryEntry queueHasWrite( ConcurrentLinkedQueue<ExecutingQueryEntry> queue ) {
		Iterator<ExecutingQueryEntry> queueIterator = queue.iterator();
		while( queueIterator.hasNext() ) {
			ExecutingQueryEntry queryEntry = queueIterator.next();
			if( queryEntry.isWriteQuery() ) {
				return queryEntry;
			}
		}
		return null;
	}

	public void blockOnWrite( ExecutingQueryEntry writeQueryEntry ) {
		ConcurrentLinkedQueue<ResultRegistration<QueryResult>> writeWaitingThreadQueue = getOrCreateExecutingQueryQueue( writeQueryEntry );
		ResultRegistration<QueryResult> writeReg = writeWaitingThreadQueue.peek();
		logger.trace( "Found writeReg: {}", writeReg );
		if( writeReg != null ) {
			logger.trace( "Waiting for writeReg!" );
			writeReg.waitUntilResultReady();
			logger.trace( "Writer was done! Trying again!" );
		}
	}

	/**
	 * Register for a query that is currently executing, or lead the execution against the database
	 * We assume that there can be only one write query ongoing at a time for a client, and that if that write query is ongoing it is the non-prediction query for that client.
	 * Any concurrent reads are from predictions and ought to be aborted to ensure client session safety.
	 * @param id
	 */
	public boolean registerOrBlockIfQueryCurrentlyExecuting( long clientId, String queryString, VersionVector version, ResultRegistration<QueryResult> reg ) {
		logger.trace( "Checking if {} is executing at version {}", queryString, version );
		boolean isWrite = isWriteQuery( queryString );
		ExecutingQueryEntry queryEntry = new ExecutingQueryEntry( queryString, version, isWrite );
		logger.trace( "Determined that {} is a write query: {}", queryString, isWrite );


		// Add myself to the queue for ongoing queries for this client.
		ConcurrentLinkedQueue<ExecutingQueryEntry> clientIdQueue = getOrCreateClientQueue( clientId );
		clientIdQueue.add( queryEntry );

		logger.trace( "Created client queue for client: {}", clientId );

		// If I am not a write, check if my client is doing a write.
		// If they are, then we need to abort this read
		if( !isWrite ) {
			boolean shouldWait = false;
			ResultRegistration<QueryResult> headRegistration = null;
			ConcurrentLinkedQueue<ResultRegistration<QueryResult>> waitingThreadQueue = null;
			while( true ) {
				logger.trace( "Not a write, checking if queue has writes." );
				ExecutingQueryEntry writeQueryEntry = queueHasWrite( clientIdQueue );
				if( writeQueryEntry != null ) {
					logger.trace( "Going to block on write and try again: {}", writeQueryEntry );
					blockOnWrite( writeQueryEntry );
					continue;
				}

				logger.trace( "queue does not have writes (yet)" );
				// If there are no writes yet, set up a register for us
				waitingThreadQueue = getOrCreateExecutingQueryQueue( queryEntry );
				waitingThreadQueue.add( reg );
				logger.trace( "Added myself to the registration queue.");
				
				// Other clients may race with us to lead this query execution.
				// The person at the head of the queue is going to execute the query
				headRegistration = waitingThreadQueue.peek();
				shouldWait = ( headRegistration != reg );
				logger.trace( "Should wait: {} reg {}", shouldWait, headRegistration );

				// If a write gets added while we were registering, abort
				logger.trace( "Checking if queue has writes (again)." );
				writeQueryEntry = queueHasWrite( clientIdQueue );
				if( writeQueryEntry != null ) {
					logger.trace( "Queue has writes. Throwing, but setting reg: {}", reg );
					clientIdQueue.remove( queryEntry );
					reg.setResult( null );
					waitingThreadQueue.remove( reg );
					blockOnWrite( writeQueryEntry );
					continue;
				}
				break;
			}

			logger.trace( "queue does not have writes (final check)" );

			// Otherwise, this query is in the clear
			if( shouldWait ) {
				headRegistration.waitUntilResultReady();
				clientIdQueue.remove( queryEntry );
				waitingThreadQueue.remove( reg );
				QueryResult result = headRegistration.getResult();
				reg.setResult( result );
			}
			return shouldWait;

		} 

		// If we are a write, then our entry in the clientIdQueue precludes others from adding to the threadsWaitingForResults queue once
		// they've observed it. However, they could put their name in the clientIdQueue, see there is no upcoming write, and then stall for a long
		// time before they finally add themselves to the queue. So the writer's iterator() does not guarantee that they see all read queries.
		// However, those queries will abort so it doesn't matter that we don't see them.

		drainPredictionQueue( clientIdQueue );

		ConcurrentLinkedQueue<ResultRegistration<QueryResult>> waitingThreadQueue = getOrCreateExecutingQueryQueue( queryEntry );
		logger.trace( "Adding write reg {} for query entry: {}", reg, queryEntry );
		waitingThreadQueue.add( reg );

		return false;
	}

	/**
	 * Set the result for the query, wake up everyone waiting for it, remove it
	 * from the currently executing query list
	 * @param id
	 */
	public void doneExecutingQuery( long clientId, String queryString, VersionVector version, QueryResult result ){

		ExecutingQueryEntry queryEntry = new ExecutingQueryEntry( queryString, version, isWriteQuery( queryString ) );
		logger.trace( "Marking {} as done!", queryEntry );
		ConcurrentLinkedQueue<ResultRegistration<QueryResult>> registrationsForQuery = threadsWaitingForResults.get( queryEntry );
		if( registrationsForQuery != null ) {
		
			// We are the head of the queue, because we are leading the execution
			logger.trace( "Removing entry from head." );
			ResultRegistration<QueryResult> ourEntry = registrationsForQuery.poll();
			logger.trace( "Set reg: {}", ourEntry );
			ourEntry.setResult( result );

			// Remove us from the client executing queue
			ConcurrentLinkedQueue<ExecutingQueryEntry> queryEntriesForClient = clientIdsExecutingSpecQueries.get( clientId );
			logger.trace( "Removing entry from executing queries." );
			queryEntriesForClient.remove( queryEntry );
		}
	}
}
