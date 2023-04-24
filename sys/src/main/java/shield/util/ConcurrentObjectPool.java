package shield.util;

/**
 * @author ncrooks
 *
 * Pool of precreated objects with objective to maximise memory reuse and minimise the effect of the
 * garbage collector.
 *
 * New objects are obtained from the pool using getNewObject call, and returned to the pool using
 * returnObject() method.
 */

public abstract class ConcurrentObjectPool<T> {

  /**
   * Returns uninitialised object of type T If there is a free object in the pool then this is
   * returned. Otherwise a new object is created.
   */
  public abstract T getNewObject();

  /**
   * Returns object to the pool. If returning object to the pool would push pool above maximum size,
   * then object is discarded otherwise reinitialised and placed in pool
   */
  public abstract void returnObject(T object);

  /**
   * Returns number of available free objects
   */
  abstract int getFreeObjects();

  /**
   * Reinitialises an object to its default values so can safely be reused.
   */

  abstract void reinitialise(T t);

}
