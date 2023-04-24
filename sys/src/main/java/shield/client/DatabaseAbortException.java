package shield.client;

public class DatabaseAbortException extends Exception {

  public DatabaseAbortException() {

  }

  public DatabaseAbortException(String message) {
    super(message);
  }

}
