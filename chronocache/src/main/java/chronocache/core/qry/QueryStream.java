package chronocache.core.qry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joda.time.DateTime;

/**
 * A streaming list of executed queries
 * @author bjglasbe
 *
 */
public class QueryStream {

	private ConcurrentLinkedQueue<ExecutedQuery> qryStream;
	private ConcurrentLinkedQueue<ExecutedQuery> qryTail;

	public QueryStream(){
		qryStream = new ConcurrentLinkedQueue<ExecutedQuery>();
		qryTail = new ConcurrentLinkedQueue<ExecutedQuery>();
	}
	
	/** 
	 * Add query to list of streaming queries
	 * TODO add concurrency controls
	 */
	public void addQueryToStream(ExecutedQuery q){
		qryStream.add(q);
	}
	
	/**
	 * Get the query at the front of the stream
	 * @return first element
	 */
	public ExecutedQuery peek(){
		return qryStream.peek();
	}
	
	/**
	 * Return the first element from the list after
	 * removing it. Move the element into the query
	 * stream's tail.
	 * @return first element
	 */
	public ExecutedQuery pop(){
		qryTail.add(qryStream.peek());
		return qryStream.poll();
	}

	/**
	 * Add query to tail of streaming queries
	 */
	public void addQueryToTail(ExecutedQuery q){
		qryTail.add(q);
	}

	/**
	 * Get the query at the end of the tail.
	 */
	public ExecutedQuery tailPeek(){
		return qryTail.peek();
	}

	/**
	 * Return query at the end of the tail and remove it.
	 */
	public ExecutedQuery tailPop(){
		return qryTail.poll();
	}

	/**
	 * Return query at the end of the tail and remove it.
	 */
	public void popFromTailBefore(DateTime cutoff){
		while(tailPeek().getExecutionTime().isBefore(cutoff)){
			tailPop();
		}
	}

	/**
	 * Return queries from the stream that occurred before the 
	 * specified endpoint
	 * @param endPoint
	 * @return querySlice
	 */
	public List<ExecutedQuery> getReadOnlyQuerySlice(DateTime endPoint){
		List<ExecutedQuery> querySlice = new LinkedList<ExecutedQuery>();
		Iterator<ExecutedQuery> it = qryStream.iterator();
		while(it.hasNext()){
			ExecutedQuery qryToTest = it.next();
			if(qryToTest.getExecutionTime().isBefore(endPoint)){
				querySlice.add(qryToTest);
			} else {
				break;
			}
		}
		return querySlice;
	}

	/**
	 * Return queries from the stream's tail.
	 */
	public List<ExecutedQuery> getReadOnlyTailSlice(DateTime endPoint){
		List<ExecutedQuery> querySlice = new LinkedList<ExecutedQuery>();
		Iterator<ExecutedQuery> it = qryTail.iterator();
		while(it.hasNext()){
			ExecutedQuery qryToTest = it.next();
			if(qryToTest.getExecutionTime().isBefore(endPoint)){
				querySlice.add(qryToTest);
			} else {
				break;
			}
		}
		return querySlice;
	}
	
	/**
	 * Get the number of elements in the stream
	 * O(N) OPERATION DO NOT USE EXCEPT FOR TESTING
	 */
	public int getQueryStreamSize(){
		return qryStream.size();
	}

	/**
	 * Also O(N)
	 */
	public int getQueryStreamTailSize(){
		return qryTail.size();
	}
}
