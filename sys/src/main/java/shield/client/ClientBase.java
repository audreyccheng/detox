package shield.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.client.schema.ColumnInfo;
import shield.client.schema.Table;


public abstract class ClientBase extends BaseNode {

  /**
   * List of tables currently contained by the database (NB: this table need not be consistent
   * across clients This is just a helper datastructure to identify the right byte-offset.
   * TODO(natacha): might be better to move to server?)
   */
  protected HashMap<String, Table> databaseSchema;


  public ClientBase() throws InterruptedException, IOException, ParseException {
    super();
    initClient();
  }

  public ClientBase(String configFileName) throws InterruptedException, ParseException, IOException {
    super(configFileName);
    initClient();
  }

  public ClientBase(String configFileName, Map<Long, ReadWriteLock> keyLocks) throws InterruptedException, ParseException, IOException {
    super(configFileName, keyLocks);
    initClient();
  }

  public ClientBase(String configFileName, int port, int uid) throws InterruptedException, ParseException, IOException {
    super(configFileName, port, uid);
    initClient();
  }


  public ClientBase(String configFileName, String address, int port, int uid) throws InterruptedException, ParseException, IOException {
    super(configFileName, address, port, uid);
    initClient();
  }

  private void initClient() {
    databaseSchema = new HashMap<String, Table>();
  }

  /**
   * Creates a table with the desired schema. NB: the columns are optional here. It is possible to
   * create an empty table and add columns later using the {@code addColumn} method
   *
   * @param name - Name of the table
   * @param columns - List of columns to be createa as pairs of name/type
   */
  public Table createTable(String name, ColumnInfo... columns) {
    Table table = new Table(name, this);
    Table oldTable = databaseSchema.put(name, table);
    if (oldTable != null) {
      throw new RuntimeException("Tried to add the same table twice");
    }
    for (ColumnInfo c : columns) {
      table.addColumn(c);

    }
    System.out.println(table.getRowSize());
    return table;
  }


  /**
   * Returns a reference to the table with the corresponding name. Throws an exception if this table
   * does not exist
   *
   * @param name - lookup table
   */
  public Table getTable(String name) {
    Table table = databaseSchema.get(name);
    if (table == null) {
      throw new RuntimeException("Table not found");
    }
    return table;
  }

  /**
   * This method registers the client with the proxy. This method should only be called once
   */
  public abstract void registerClient() throws DatabaseAbortException, InterruptedException;

  /**
   * Start the transaction. For efficiency, this start request will be batched with the first
   * read/write requests. This operation does not contact the proxy
   */
  public abstract void startTransaction() throws DatabaseAbortException;

  /**
   * Adds a read request to the transaction, and executes any operations that have not yet been
   * executed at the proxy.
   *
   * @return the list of all previous reads' return values
   * @throws DatabaseAbortException if the proxy aborts the transaction
   */
  public abstract List<byte[]> readAndExecute(String table, String key) throws DatabaseAbortException;

  /**
   * Adds a read operation to the transaction. Does not actually execute the read requests. Can be
   * used for batching independent operations.
   */
  public abstract void read(String table, String key) throws DatabaseAbortException;

  /**
   * Sends buffered operations
   * @return
   * @throws DatabaseAbortException
   */
  public abstract List<byte[]> execute() throws DatabaseAbortException;

  /**
   * Adds a read-for-update request to the transaction, and executes any operations that have not yet been
   * executed at the proxy.
   *
   * @return the list of all previous reads' return values
   * @throws DatabaseAbortException if the proxy aborts the transaction
   */
  public abstract List<byte[]> readForUpdateAndExecute(String table, String key) throws DatabaseAbortException;

  /**
   * Adds a read operation to the transaction. Does not actually execute the read requests. Can be
   * used for batching independent operations.
   */
  public abstract void readForUpdate(String table, String key) throws DatabaseAbortException;

  /**
   * Adds a delete operation to the transaction. Does not actually execute the delete
   * request. Used for batching independent operations
   * @param table
   * @param key
   */
  public abstract void delete(String table, String key) throws DatabaseAbortException;

  /**
   * Adds a delete request to the transaction, and executes any operations that have not yet been
   * executed at the proxy.
   *
   * @return the list of all previous reads' return values
   * @throws DatabaseAbortException if the proxy aborts the transaction
   */
   public abstract List<byte[]> deleteAndExecute(String table, String key) throws DatabaseAbortException;

  /**
   * Adds a write operation to the transaction. Does not actually execute the write request. Can be
   * used for batching independent operations.
   */
  public abstract void write(String table, String key, byte[] value) throws DatabaseAbortException;

  /**
   * Adds an update operation to the transaction (not guaranteed to take
   * effect if row does not exist). Does not actually execute the write request. Can be
   * used for batching independent operations.
   */
  public abstract void update(String table, String key, byte[] value) throws DatabaseAbortException;

  /**
   * Adds a write request to the transaction, and executes any operations that have not yet been
   * executed at the proxy.
   *
   * @return the list of all previous reads' return values
   * @throws DatabaseAbortException if the proxy aborts the transaction
   */
  public abstract List<byte[]> writeAndExecute(String table, String key, byte[] value)
      throws DatabaseAbortException;

  /**
   * Adds an update request to the transaction, and executes any operations that have not yet been
   * executed at the proxy.
   *
   * @return the list of all previous reads' return values
   * @throws DatabaseAbortException if the proxy aborts the transaction
   */
  public abstract List<byte[]> updateAndExecute(String table, String key, byte[] value)
      throws DatabaseAbortException;

  /**
   * @return the result of any not-previously executed read operations
   */
  public abstract List<byte[]> commitTransaction() throws DatabaseAbortException;

  /**
   * Aborts transaction. If the transaction was never sent to the proxy, simply skip that command
   * and reset the transaction. Resets datastructure for the next query
   */
  public abstract void abortTransaction() throws DatabaseAbortException;

  public abstract List<byte[]> executeTransaction(ClientTransaction ongoingTrx) throws DatabaseAbortException;
}
