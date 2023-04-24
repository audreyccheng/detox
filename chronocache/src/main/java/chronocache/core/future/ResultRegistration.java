package chronocache.core.future;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Result Registration class used to have a thread wait until a result is ready
 * @author bjglasbe
 *
 * @param <T>
 */
public class ResultRegistration<T> {

	private ReentrantLock lock; 
	private boolean ready;
	private Condition readyCV; 
	private ResultBox<T> resultBox;
	
	/**
	 * Create a registration record - provide a lock for us to create cond vars on,
	 * and a place to put the result
	 * @param sem
	 * @param result
	 */
	public ResultRegistration( ReentrantLock lock, ResultBox<T> resultBox ){
		this.lock = lock;
		this.ready = false;
		this.readyCV = lock.newCondition();
		this.resultBox = resultBox;
	}
	
	/**
	 * Block the current thread until someone increments the semaphore,
	 * acknowledging that the result is ready
	 */
	public void waitUntilResultReady() {
		lock.lock();
		while( !ready ) {
			try {
				readyCV.await();
				break;
			} catch (InterruptedException e) {
			} 
		}
		lock.unlock();
	}
	
	/**
	 * Set the result and wake up the threads waiting for it
	 * @param result
	 */
	public void setResult(T result){
		lock.lock();
		ready = true;
		resultBox.setResult(result);
		readyCV.signalAll();
		lock.unlock();
	}
	
	/**
	 * Get the result
	 * @return
	 */
	public T getResult(){
		lock.lock();
		T result = resultBox.getResult();
		lock.unlock();
		return result;
	}
}
