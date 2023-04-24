package chronocache.util;

import java.util.concurrent.Semaphore;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectPool<T> {

	private List<T> objects;
	private Semaphore lock;
	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	public ObjectPool( List<T> startingObjects ) {
		objects = startingObjects;
		lock = new Semaphore( objects.size() );
	}

	public T borrow(){
		try {
			T obj;
			lock.acquire();
			synchronized( objects ) {
				obj = objects.remove(0);
			}
			return obj;
		} catch( InterruptedException e ) {
			logger.error( "Interrupted while waiting for sempahore: {}", e.getMessage() );
 		}
		return null;
	}

	public void returnObj( T obj ) {
		synchronized( objects ) {
			objects.add( obj );
		}
		lock.release();
	}
}
