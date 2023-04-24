package shield.client;

import static shield.network.messages.Msg.Message.Type.ClientReqMessage;
import static shield.network.messages.Msg.Message.Type.ClientRespMessage;
import static shield.util.Logging.Level.CRITICAL;
import static shield.util.Logging.Level.INFO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.crypto.Data;
import org.json.simple.parser.ParseException;
import shield.client.schema.Table;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Message.Type;
import shield.util.Logging;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import shield.util.Utility;

/**
 * Represents a client. Interfaces to library code. Clients are single threaded and can only have
 * one ongoing transaction at a time.
 *
 * TODO(Natacha): Code needs to be refactored to reduce code duplication
 *
 * @author ncrooks
 */
public final class SQLClient extends ClientBase {


  /**
   * Stores a list of tables that have already been created
   */
  private Set<String> tableNames;

  /**
   * MySQL Connection
   */
  private Connection connection;

  /**
   * List of statemetns that have not yet been executed
   */
  private LinkedList<PreparedStatement> pendingStatements
      = new LinkedList<>();


  public SQLClient() throws InterruptedException, IOException, ParseException, SQLException {
    super();
    initClient();
  }

  public SQLClient(String configFileName)
      throws InterruptedException, ParseException, IOException, SQLException {
    super(configFileName);
    initClient();
  }

  public SQLClient(String configFileName, int port, int uid)
      throws InterruptedException, ParseException, IOException, SQLException {
    super(configFileName, port, uid);
    initClient();
  }


  public SQLClient(String configFileName, String address, int port, int uid)
      throws InterruptedException, ParseException, IOException, SQLException {
    super(configFileName, address, port, uid);
    initClient();
  }

  private void initClient() {
    System.out.println("Initialising Clients");
    tableNames = new HashSet<String>();
    String jdbcUrl = "jdbc:mysql://" + config.RDS_HOSTNAME + ":" +
        config.RDS_PORT + "/" + config.RDS_DB_NAME + "?user=" + config.RDS_USERNAME + "&password="
        + config.RDS_PASSWORD;
    // Load the JDBC driver
    try {
      System.out.println("Loading driver...");
      Class.forName("com.mysql.cj.jdbc.Driver");
      System.out.println("Driver loaded!");
      System.out.println("Connection " + jdbcUrl);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Cannot find the driver in the classpath!", e);
    }
    try {
      connection = DriverManager.getConnection(jdbcUrl);
    } catch (SQLException e) {
      System.err.println(jdbcUrl);
      System.err.println(e);
      System.err.println(e.getErrorCode());
      System.err.println(e.getMessage());
      System.exit(-1);
    }
    databaseSchema = new HashMap<String, Table>();
  }


  private void createTable(String tableName) {
    //String deleteTable = ("DROP TABLE " + config.RDS_TABLE_NAME);
    String createTable = (
        "CREATE TABLE " + tableName +
            "(id BIGINT not null, " +
            "data VARCHAR(" + config.ORAM_VALUE_SIZE + ")," +
            "PRIMARY KEY (id))");
    System.out.println(createTable);
    tableNames.add(tableName);
    try {
      java.sql.Statement stmt = connection.createStatement();
      stmt.executeUpdate(createTable);
      connection.commit();
    } catch (SQLException e) {
      System.err.println(e.getMessage());
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#registerClient()
   */
  @Override
  public synchronized void registerClient() throws DatabaseAbortException {
    try {
      connection.setAutoCommit(false);
      // connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      // connection.setAutoCommit(false);
      connection.commit();
    } catch (SQLException e) {
      e.printStackTrace();
      System.exit(-1);
    }

    createTable(config.RDS_TABLE_NAME);

  }


  public void startTransaction() throws DatabaseAbortException {
  }

  public void createReadStatement(String table, String key) throws SQLException {

    if (!tableNames.contains(table)) {
      // Table may not have been created yet, so create it
      createTable(table);
    }

    String query = "SELECT data FROM " + table + " WHERE id = ?";
    String row = table + key;
    Long id = Utility.hashPersistent(row);
    PreparedStatement prepStatement = connection.prepareStatement(query);
    prepStatement.setLong(1, id);
    pendingStatements.add(prepStatement);
  }

  public void createUpdateStatement(String table, String key, byte[] value) throws SQLException {
    if (!tableNames.contains(table)) {
      // Table may not have been created yet, so create it
      createTable(table);
    }
    String row = table + key;
    Long id = Utility.hashPersistent(row);
    String update = " UPDATE " +
        table +
        " SET data=? where id=?";
    PreparedStatement preparedStatement = connection.prepareStatement(update);
    preparedStatement.setBytes(1, value);
    preparedStatement.setLong(2, id);
//      preparedStatement.closeOnCompletion();
    pendingStatements.add(preparedStatement);

  }

  public void createWriteStatement(String table, String key, byte[] value) throws SQLException {
    if (!tableNames.contains(table)) {
      // Table may not have been created yet, so create it
      createTable(table);
    }
    String row = table + key;
    Long id = Utility.hashPersistent(row);
    String update = "INSERT INTO  " +
        table +
        "(id, data) VALUES(?,?) ON DUPLICATE KEY UPDATE data=?";
    PreparedStatement preparedStatement = connection.prepareStatement(update);
    preparedStatement.setLong(1, id);
    preparedStatement.setBytes(2, value);
    preparedStatement.setBytes(3, value);
//      preparedStatement.closeOnCompletion();
    pendingStatements.add(preparedStatement);
  }

  public void createDeleteStatement(String table, String key) throws SQLException {
    if (!tableNames.contains(table)) {
      // Table may not have been created yet, so create it
      createTable(table);
    }
    String query = "DELETE  FROM " + table + " WHERE id = ?";
    String row = table + key;
    Long id = Utility.hashPersistent(row);
    PreparedStatement prepStatement = connection.prepareStatement(query);
    //     prepStatement.closeOnCompletion();
    prepStatement.setLong(1, id);
    pendingStatements.add(prepStatement);
  }

  public void createReadForUpdateStatement(String table, String key) throws SQLException {
    if (!tableNames.contains(table)) {
      // Table may not have been created yet, so create it
      createTable(table);
    }
    String query = "SELECT data FROM " + table + " WHERE id = ? FOR UPDATE";
    String row = table + key;
    Long id = Utility.hashPersistent(row);
    PreparedStatement prepStatement = connection.prepareStatement(query);
    //    prepStatement.closeOnCompletion();
    prepStatement.setLong(1, id);
    pendingStatements.add(prepStatement);
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#readAndExecute(java.lang.String)
   */
  @Override
  public synchronized List<byte[]> readAndExecute(String table, String row)
      throws DatabaseAbortException {

    try {
      // Generate and send any non-executed statements
      createReadStatement(table, row);
      List<byte[]> results = executeOps();
      return results;
    } catch (SQLException e) {
      handleError(e);
    }
    return null;
  }

  public void reset() {
    pendingStatements.clear();
    pendingStatements.clear();
    pendingStatements.clear();
    pendingStatements.clear();
  }

  public void handleError(SQLException e)
      throws DatabaseAbortException {
    // Return read values
    try {
      connection.rollback();
      e.printStackTrace();
      reset();
    } catch (SQLException e1) {
      //TODO(natacha) handle property
      e1.printStackTrace();
      ;
    }
    throw new DatabaseAbortException();

  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#read(java.lang.String)
   */
  @Override
  public synchronized void read(String tableName, String row) throws DatabaseAbortException {
    try {
      createReadStatement(tableName, row);
    } catch (SQLException e) {
      handleError(e);
    }

  }

  @Override
  public synchronized List<byte[]> execute() throws DatabaseAbortException {
    try {
      List<byte[]> results = executeOps();
      return results;
    } catch (SQLException e) {
      handleError(e);
    }
    return null;
  }

  @Override
  public synchronized List<byte[]> readForUpdateAndExecute(String table, String row)
      throws DatabaseAbortException {
    try {

      createReadForUpdateStatement(table, row);
      List<byte[]> results = executeOps();
      return results;
    } catch (SQLException e) {
      handleError(e);
    }
    return null;
  }

  @Override
  public synchronized void readForUpdate(String tableName, String row)
      throws DatabaseAbortException {
    try {
      createReadForUpdateStatement(tableName, row);
    } catch (SQLException e) {
      handleError(e);
    }
  }

  @Override
  public synchronized void delete(String tableName, String row) throws DatabaseAbortException {
    try {
      createDeleteStatement(tableName, row);
    } catch (SQLException e) {
      handleError(e);
    }
  }

  @Override
  public synchronized List<byte[]> deleteAndExecute(String table, String row)
      throws DatabaseAbortException {
    try {

      createDeleteStatement(table, row);
      List<byte[]> results = executeOps();
      return results;
    } catch (SQLException e) {
      handleError(e);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#write(java.lang.String, byte[])
   */
  @Override
  public void write(String table, String row, byte[] value) throws DatabaseAbortException {
    try {
      createWriteStatement(table, row, value);
    } catch (SQLException e) {
      handleError(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#update(java.lang.String, byte[])
   */
  @Override
  public void update(String table, String row, byte[] value) throws DatabaseAbortException {
    try {
      createUpdateStatement(table, row, value);
    } catch (SQLException e) {
      handleError(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#updateAndExecute(java.lang.String, byte[])
   */
  @Override
  public synchronized List<byte[]> updateAndExecute(String table,
      String row, byte[] value)
      throws DatabaseAbortException {

    try {
      createUpdateStatement(table, row, value);
      List<byte[]> results = executeOps();
      return results;
    } catch (SQLException e) {
      handleError(e);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#writeAndExecute(java.lang.String, byte[])
   */
  @Override
  public synchronized List<byte[]> writeAndExecute(String table,
      String row, byte[] value)
      throws DatabaseAbortException {

    try {
      createWriteStatement(table, row, value);
      List<byte[]> results = executeOps();
      return results;
    } catch (SQLException e) {
      handleError(e);
    }
    return null;
  }


  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#commitTransaction()
   */
  @Override
  public synchronized List<byte[]> commitTransaction()
      throws DatabaseAbortException {
    try {
      List<byte[]> results = executeOps();
      connection.commit();
      return results;
    } catch (SQLException e) {
      handleError(e);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#abortTransaction()
   */
  @Override
  public synchronized void abortTransaction() throws DatabaseAbortException {
    try {
      connection.rollback();
    } catch (SQLException e) {
      handleError(e);
    }

  }


  /**
   * Sends any non-executed statement. Assume that the calling function is calling semaphore.wait()
   * to wait for replies.
   *
   * NB: sending multiple operations at once will allow the operations to be executed concurrently
   * at the server. This means, however, that we cannot send more than one operation with the same
   * key per batch of operations We check this here (AK: only non conflicting operations can be
   * sent).
   */
  public synchronized LinkedList<byte[]> executeOps() throws DatabaseAbortException, SQLException {

    LinkedList<byte[]> readResults = new LinkedList<byte[]>();
    byte[] current;
    boolean hasResultSet;

    for (PreparedStatement stat : pendingStatements) {
      hasResultSet = stat.execute();
      if (hasResultSet) {
        try (ResultSet resultSet = stat.getResultSet()) {
          if (resultSet.next()) {
            current = resultSet.getBytes(1);
          } else {
            current = new byte[0];
          }
        }
        readResults.add(current);
      }
      stat.close();
    }

    pendingStatements.clear();

    return readResults;

  }

  /**
   * Executes a pre-generated transaction (the transaction already contains statements)
   *
   * @param trx - the transaction
   */
  public List<byte[]> executeTransaction(ClientTransaction trx)
      throws DatabaseAbortException {
    throw new RuntimeException("Unimplemented");
  }


  /*
   * (non-Javadoc)
   *
   * @see shield.BaseNode#handleMsg(shield.network.messages.Msg.Message)
   */
  @Override
  public void handleMsg(Message msg) {
    throw new RuntimeException("Unimplemented");
  }


}
