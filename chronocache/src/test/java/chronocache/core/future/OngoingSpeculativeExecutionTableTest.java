package chronocache.core.future;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import chronocache.core.VersionVector;
import chronocache.core.VersionVectorFactory;
import chronocache.core.WorkloadType;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryResult;

public class OngoingSpeculativeExecutionTableTest {

	private String queryString = "SELECT 1";
	private ArrayList<Long> versions1 = new ArrayList<Long>() {{
		add( 12L );
		add( 3L );
	}};
	private ArrayList<Long> versions2 = new ArrayList<Long>() {{
		add( 15L );
		add( 3L );
	}};

	private VersionVector versionVector1 = new VersionVector( versions1 );
	private VersionVector versionVector2 = new VersionVector( versions2 );
	private QueryResult expResults1 = new QueryResult( new LinkedList<Map<String,Object>>(), versionVector1 );
	private QueryResult expResults2 = new QueryResult( new LinkedList<Map<String,Object>>(), versionVector2 );

	private class WaitingThread implements Runnable {

		private OngoingSpeculativeExecutionTable oset;
		private String queryToWaitFor;
		private VersionVector versionToWaitFor;
		
		public WaitingThread( OngoingSpeculativeExecutionTable oset, String queryToWaitFor, VersionVector versionToWaitFor ) {
			this.oset = oset;
			this.queryToWaitFor = queryToWaitFor;
			this.versionToWaitFor = versionToWaitFor;
			
		}
		@Override
		public void run() {
			ResultBox<QueryResult> box = new ResultBox<>();
			ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
			assertThat( oset.registerOrBlockIfQueryCurrentlyExecuting( 0, queryToWaitFor, versionToWaitFor, reg ), equalTo( true ) );
			assertThat( box.getResult(), equalTo( expResults1 ) );
		}
		
	};
	/**
	 * Test that we don't wait if there is no query currently registered
	 */
	@Test
	public void testNoBlockIfQueryNotRegistered() {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), new ResultBox<>() );
		assertThat( oset.registerOrBlockIfQueryCurrentlyExecuting( 0, "SELECT 1", versionVector1, reg ), equalTo( false ) );
	}
	
	/**
	 * Test that we wait for results if there is a query registered
	 * @throws InterruptedException
	 */
	@Test(timeout=1000)
	public void testBlockIfQueryRegistered() throws InterruptedException {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		ResultBox<QueryResult> box = new ResultBox<>();
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, queryString, versionVector1, reg );
		Thread t = new Thread( new WaitingThread( oset, queryString, versionVector1 ) );
		t.start();
		Thread.sleep( 300 );
		oset.doneExecutingQuery( 0, queryString, versionVector1, expResults1 );
		t.join();
	}
	
	/**
	 * Test that we can block and unblock multiple threads
	 * @throws InterruptedException
	 */
	@Test(timeout=1000)
	public void testBlockMultipleThreads() throws InterruptedException {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		ResultBox<QueryResult> box = new ResultBox<>();
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, queryString, versionVector1, reg );
		Thread t = new Thread( new WaitingThread( oset, queryString, versionVector1 ) );
		Thread t2 = new Thread( new WaitingThread( oset, queryString, versionVector1 ) );
		t.start();
		t2.start();
		Thread.sleep( 300 );
		oset.doneExecutingQuery( 0, queryString, versionVector1, expResults1 );
		t.join();
		t2.join();
	}

	@Test(timeout=2000)
	public void testDifferentVersionsDontUnblockEachOther() throws InterruptedException {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		ResultBox<QueryResult> box = new ResultBox<>();
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, queryString, versionVector1, reg );
		Thread t = new Thread( new WaitingThread( oset, queryString, versionVector1 ) );
		Thread t2 = new Thread( new WaitingThread( oset, queryString, versionVector1 ) );
		t.start();
		t2.start();
		Thread.sleep( 300 );
		//If this unblocks, the expResults will fail, causing the thread to assert
		oset.doneExecutingQuery( 0, queryString, versionVector2, expResults2 );
		Thread.sleep( 300 );
		oset.doneExecutingQuery( 0, queryString, versionVector1, expResults1 );
		t.join();
		t2.join();
	}

	@Test
	public void testWaitForDBVersion() throws InterruptedException {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();

		VersionVectorFactory vvf = new VersionVectorFactory( WorkloadType.TPCW );
		String query = "SELECT i_id, i_title, a_fname, a_lname FROM item, " +
			"author, order_line WHERE ol_o_id > 25723730 AND i_id = ol_i_id AND " +
			"i_a_id = a_id AND i_subject = 'MYSTERY' GROUP BY i_id, i_title, " +
			"a_fname, a_lname ORDER BY SUM(ol_qty) DESC LIMIT 50";
		VersionVector version = vvf.createDBVersionVector();
		ResultBox<QueryResult> box = new ResultBox<>();
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, query, version, reg );
		Thread t = new Thread( new WaitingThread( oset, query, version ) );
		t.start();
		Thread.sleep( 300 );
		oset.doneExecutingQuery( 0, query, version, expResults1 );
		t.join();
	}

	@Test
	public void testDoubleReadQuery() {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		String readQuery = "SELECT * FROM T";
		ResultBox<QueryResult> box = new ResultBox<>();
		ResultRegistration<QueryResult> readReg = new ResultRegistration<>( new ReentrantLock(), box );
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, readQuery, versionVector1, readReg );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, "SELECT * FROM T2", versionVector1, reg );
	}

	@Test
	public void testNoBlockIfOtherClientExecutingWriteQuery() {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		String insertQuery = "INSERT INTO T VALUES( 1 )";
		ResultBox<QueryResult> box = new ResultBox<>();
		ResultRegistration<QueryResult> writeReg = new ResultRegistration<>( new ReentrantLock(), box );
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 1, insertQuery, versionVector1, writeReg );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, "SELECT * FROM T", versionVector1, reg );
	}

	@Test
	public void testWriterDrainsReaderQueue() {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		String insertQuery = "INSERT INTO T VALUES( 1 )";
		ResultBox<QueryResult> box = new ResultBox<>();
		ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
		ResultRegistration<QueryResult> writeReg = new ResultRegistration<>( new ReentrantLock(), box );
		oset.registerOrBlockIfQueryCurrentlyExecuting( 0, "SELECT * FROM T", versionVector1, reg );
		Thread t = new Thread( new WaitingThread( oset, insertQuery, versionVector1 ) );
		oset.doneExecutingQuery( 0, "SELECT * FROM T", versionVector1, expResults1 );
		oset.doneExecutingQuery( 0, insertQuery, versionVector1, expResults1 );
	}

	private class WaitingReadThreadLauncher implements Runnable {

		private OngoingSpeculativeExecutionTable oset;
		private String queryToWaitFor;
		private VersionVector versionToWaitFor;
		private int numberToLaunch;
		
		public WaitingReadThreadLauncher( OngoingSpeculativeExecutionTable oset, String queryToWaitFor, VersionVector versionToWaitFor, int numberToLaunch ) {
			this.oset = oset;
			this.queryToWaitFor = queryToWaitFor;
			this.versionToWaitFor = versionToWaitFor;
			this.numberToLaunch = numberToLaunch;
			
		}
		@Override
		public void run() {
			for( int i = 0; i < numberToLaunch; i++ ) {
				ResultBox<QueryResult> box = new ResultBox<>();
				ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
				// We will either win the race and execute, in which case they must drain, or
				// we lose the race and get an exception.
				boolean shouldWait = oset.registerOrBlockIfQueryCurrentlyExecuting( 0, queryToWaitFor, versionToWaitFor, reg );
				oset.doneExecutingQuery( 0, queryToWaitFor, versionToWaitFor, expResults1 );
			}
		}
		
	};

	@Test
	public void testReaderWriterRace() throws InterruptedException {
		OngoingSpeculativeExecutionTable oset = new OngoingSpeculativeExecutionTable();
		String readQuery = "SELECT * FROM T";
		String insertQuery = "INSERT INTO T VALUES( 1 )";
		Thread t = new Thread( new WaitingReadThreadLauncher( oset, readQuery, versionVector1, 10000 ) );
		t.start();
		for( int i = 0; i < 10000; i++ ) {
			ResultBox<QueryResult> box = new ResultBox<>();
			ResultRegistration<QueryResult> reg = new ResultRegistration<>( new ReentrantLock(), box );
			oset.registerOrBlockIfQueryCurrentlyExecuting( 0, insertQuery, versionVector1, reg );
			oset.doneExecutingQuery( 0, insertQuery, versionVector1, expResults1 );
		}
		t.join();


	}

}
