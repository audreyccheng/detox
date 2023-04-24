package shield.client;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Wrapper for transaction representation at the client
 *
 * @author ncrooks
 * @author ncrooks
 */
/**
 * @author ncrooks
 *
 */

/**
 * @author ncrooks
 *
 */
public final class ClientTransaction {

  /**
   * Transaction can either be executing or idle
   *
   * @author ncrooks
   */
  enum TxState {
    NONE, // transaction has not yet started
    START, // transaction has started but no request has yet been executed
    ONGOING, // executing request and waiting for reply
    COMMIT, // transaction is about to commit
    ABORT, // transaction is about to abort
    START_AND_COMMIT, // transaction is about to commit but no request has been
    // executed
    OK, KO
  }

  /**
   * Index of the next statement that should be executed
   */
  private int nextStatementToExecute;

  /**
   * List of statements contained in this transaction
   */
  private final ArrayList<Statement> statements;

  private volatile TxState txState;

  private final HashSet<String> readKeys;
  private final HashSet<String> writeKeys;

  public ClientTransaction() {
    this.nextStatementToExecute = 0;
    this.statements = new ArrayList<Statement>();
    this.txState = TxState.NONE;
    this.readKeys = new HashSet<String>();
    this.writeKeys = new HashSet<String>();
  }

  public void reset() {
    txState = TxState.NONE;
    statements.clear();
    nextStatementToExecute = 0;
    readKeys.clear();
    writeKeys.clear();
  }

  public void resetExecution() {
    txState = TxState.NONE;
    nextStatementToExecute = 0;
    readKeys.clear();
    writeKeys.clear();
  }

  public void markStart(int maxOpCount) {
    if (txState!=TxState.NONE) System.out.println(txState);
    assert (txState == TxState.NONE);
    this.txState = TxState.START;
  }

  /**
   * Add read to the trx if not exceeding max transaction length
   *
   * @param key
   * @throws Exception
   */
  public void addRead(String key) throws DatabaseAbortException {
    System.out.println("[Client] Adding Read Key " + key);
    Statement st = Statement.read(key);
    statements.add(st);
    if (!readKeys.add(key)) {
      System.err.println("No duplicate reads for trx");
      reset();
      throw new DatabaseAbortException("Duplicate read. Must abort");
    }
  }

  /**
   * Add delete to the trx if not exceeding max transaction length
   *
   * @param key
   * @throws Exception
   */
  public void addDelete(String key) throws DatabaseAbortException {
     System.out.println("[Client] Adding Delete Key " + key);
     Statement st = Statement.delete(key);
    statements.add(st);
    if (!writeKeys.add(key)) {
      System.err.println("No duplicate writes for trx");
      reset();
      throw new DatabaseAbortException("Duplicate writes. Must abort");
    }
  }

   /**
   * Add delete to the trx if not exceeding max transaction length
   *
   * @param key
   * @throws Exception
   */

  public void addReadForUpdate(String key) throws DatabaseAbortException {
     System.out.println("[Client] Adding Read For Update Key " + key);
     Statement st = Statement.readForUpdate(key);
    statements.add(st);
    if (!readKeys.add(key)) {
      System.err.println("No duplicate writes for trx");
      reset();
      throw new DatabaseAbortException("Duplicate writes. Must abort");
    }
  }

  /**
   * Add write to the trx if not exceeding max transaction length
   *
   * @param key
   * @param value
   * @throws Exception
   */
  public void addWrite(String key, byte[] value) throws DatabaseAbortException {
     System.out.println("[Client] Adding Write Key " + key);
      Statement st = Statement.write(key, value);
    statements.add(st);
    if (!writeKeys.add(key)) {
      System.err.println("No duplicate writes for trx");
      reset();
      throw new DatabaseAbortException("Duplicate write. Must abort");
    }
  }

  /**
   * Reset transaction when commit/abort abortion have successfully finished
   * executing
   */
  public void markFinished() {
    reset();
  }

  /**
   * Mark transaction as waiting for a sent request
   */
  public void markOngoing() {
    txState = TxState.ONGOING;
  }

  /**
   * Marks the last request as successful or not
   *
   * @param success
   */
  public void markOpResult(boolean success) {
    txState = success ? TxState.OK : TxState.KO;
  }

  /**
   * Mark transaction as committed or as "START_AND_COMMIT" if transactions had
   * not yet started
   */
  public void markCommit() {
    if (txState == TxState.START) {
      txState = TxState.START_AND_COMMIT;
    } else {
      txState = TxState.COMMIT;
    }
  }

  /**
   * Mark transaction as aborted
   */
  public void markAbort() {
    txState = TxState.ABORT;
  }

  /**
   * Checks that the .start() method has been called on the transaction
   *
   * @return
   */
  public boolean hasStarted() {
    int ret = txState.compareTo(TxState.NONE);
    return ret > 0;
  }

  /**
   * Checks whether the last executed request was successful
   *
   * @return
   */
  public boolean hasError() {
    return txState == TxState.KO;
  }

  /**
   * Determines whether the transaction has already sent operations to the proxy
   *
   * @return
   */
  public boolean isFirstRequest() {
    return txState == TxState.START || txState == TxState.START_AND_COMMIT;
  }

  /**
   * Checks whether there is a pending request for this transaction
   *
   * @return
   */
  public boolean hasOngoingReq() {
    return txState == TxState.ONGOING;
  }

  /**
   * Checks whether this transaction is ready to commit (.commit()) method must
   * have been called
   *
   * @return
   */
  public boolean isCommit() {
    return txState == TxState.COMMIT || txState == TxState.START_AND_COMMIT;
  }

  /**
   * Checks whether this transaction is ready to abort (.abort()) method must
   * have been called
   *
   * @return
   */
  public boolean isAbort() {
    return txState == TxState.ABORT;
  }


  /**
   * Number of operations in this transaction
   *
   * @return
   */
  public int getLength() {
    return statements.size();
  }

  /**
   * Directly add a list of statements (this will only be used in benchmarking
   * when transactions are pre-created?)
   *
   * @param statements
   */
  public void addStatements(ArrayList<Statement> statements) {
    this.statements.addAll(statements);
  }

  public ArrayList<Statement> getStatements() {
    return new ArrayList<Statement>(statements);
  }

  /**
   * Return the next statement that has not yet executed
   *
   * @return the next statement
   */
  public Statement executeNextStatement() {
    Statement st = null;
    if (nextStatementToExecute != statements.size()) {
      st = statements.get(nextStatementToExecute++);
    }
    return st;
  }

  public long getNbWrites() {
    return writeKeys.size();
  }

  public long getNbReads() {
    return readKeys.size();
  }

}
