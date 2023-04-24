package chronocache.core;

public class FDQExecutionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FDQExecutionException(String message, Throwable e) {
		super(message, e);
	}

	public FDQExecutionException(Exception e) {
		super(e);
	}

}
