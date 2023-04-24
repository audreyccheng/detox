package chronocache.core.future;

/**
 * A class used to add one layer of indirection to a result
 * @author bjglasbe
 *
 * @param <T>
 */
public class ResultBox<T> {

	private T result;
	
	public ResultBox(){
		result = null;
	}
	/**
	 * Set the Result
	 * @param result
	 */
	public void setResult(T result){
		this.result = result;
	}
	
	/**
	 * Get the Result
	 * @return
	 */
	public T getResult(){
		return result;
	}
}
