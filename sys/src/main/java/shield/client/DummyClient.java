package shield.client;

import static shield.network.messages.Msg.Message.Type.ClientReqMessage;
import static shield.network.messages.Msg.Message.Type.ClientRespMessage;
import static shield.util.Logging.Level.CRITICAL;
import static shield.util.Logging.Level.INFO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.simple.parser.ParseException;
import shield.client.schema.Table;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Message.Type;
import shield.util.Logging;
import shield.util.Utility;


/**
 * Represents a client. Interfaces to library code. Clients are single threaded and can only have
 * one ongoing transaction at a time. This client should only be used for unit testing and does not
 * actually contact the database. All reads and writes are served from a local hashmap
 *
 * TODO(Natacha): Code needs to be refactored to reduce code duplication
 *
 * @author ncrooks
 */
public final class DummyClient extends ClientBase {

  /**
   * Place-holder transaction for this client
   */
  private ClientTransaction clientTransaction;

  /**
   * Place-holder for returned value
   */
  private List<byte[]> readValue;

  /**
   * Place-holder for error string
   */
  private String errorMsg;

  private HashMap<Long, byte[]> localDB;


  public DummyClient() throws InterruptedException, IOException, ParseException {
    super();
    initClient();
  }

  public DummyClient(String configFileName)
      throws InterruptedException, ParseException, IOException {
    super(configFileName);
    initClient();
  }

  public DummyClient(String configFileName, int port, int uid)
      throws InterruptedException, ParseException, IOException {
    super(configFileName, port, uid);
    initClient();
  }


  public DummyClient(String configFileName, String address, int port, int uid)
      throws InterruptedException, ParseException, IOException {
    super(configFileName, address, port, uid);
    initClient();
  }

  private void initClient() {
    clientTransaction = new ClientTransaction();
    readValue = null;
    databaseSchema = new HashMap<String, Table>();
    localDB = new HashMap<Long, byte[]>();
  }


  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#registerClient()
   */
  @Override
  public synchronized void registerClient() throws DatabaseAbortException {

    System.out.println("[Client] Registered");
  }


  public void startTransaction() throws DatabaseAbortException {
    clientTransaction.markStart(0);
    System.out.println("[Client] Started");
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#readAndExecute(java.lang.String)
   */
  @Override
  public synchronized List<byte[]> readAndExecute(String table, String row)
      throws DatabaseAbortException {

    if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }

    String key = table + row;

    // Generate and send any non-executed statements
    clientTransaction.addRead(key);
    executeOps();

    // check if executing these statements was successful
    if (clientTransaction.hasError()) {
      clientTransaction.markFinished();
      throw new DatabaseAbortException(errorMsg);
    } else {

    }

    // Return read values
    return readValue;
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#read(java.lang.String)
   */
  @Override
  public synchronized void read(String tableName, String row) throws DatabaseAbortException {
    if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }
    String key = tableName + "-" + row;
    clientTransaction.addRead(key);
  }

  @Override
  public synchronized List<byte[]> execute()
      throws DatabaseAbortException {
    try {
      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      executeOps();

      // Wait for operations to finish executing
      while (clientTransaction.hasOngoingReq()) {
        wait();
      }
      // check if executing these statements was successful
      if (clientTransaction.hasError()) {
        clientTransaction.markFinished();
        throw new DatabaseAbortException(errorMsg);
      } else {

      }
    } catch (InterruptedException e) {
      logErr("Client was interrupted", CRITICAL);
      System.exit(-1);
    }

    // Return read values
    return readValue;
  }

  @Override
  public synchronized List<byte[]> readForUpdateAndExecute(String table, String row)
      throws DatabaseAbortException {
    try {
      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      String key = table + row;

      // Generate and send any non-executed statements
      clientTransaction.addReadForUpdate(key);
      executeOps();

      // Wait for operations to finish executing
      while (clientTransaction.hasOngoingReq()) {
        wait();
      }
      // check if executing these statements was successful
      if (clientTransaction.hasError()) {
        clientTransaction.markFinished();
        throw new DatabaseAbortException(errorMsg);
      } else {

      }
    } catch (InterruptedException e) {
      logErr("Client was interrupted", CRITICAL);
      System.exit(-1);
    }

    // Return read values
    return readValue;
  }

  @Override
  public synchronized void readForUpdate(String tableName, String row)
      throws DatabaseAbortException {
    if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }
    String key = tableName + "-" + row;
    clientTransaction.addReadForUpdate(key);
  }

  @Override
  public synchronized void delete(String tableName, String row) throws DatabaseAbortException {
    if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }
    String key = tableName + "-" + row;
    clientTransaction.addDelete(key);
  }

  @Override
  public synchronized List<byte[]> deleteAndExecute(String table, String row)
      throws DatabaseAbortException {

      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      String key = table + row;

      // Generate and send any non-executed statements
      clientTransaction.addDelete(key);
      executeOps();

      // check if executing these statements was successful
      if (clientTransaction.hasError()) {
        clientTransaction.markFinished();
        throw new DatabaseAbortException(errorMsg);
      } else {

      }

    // Return read values
    return readValue;
  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#write(java.lang.String, byte[])
   */
  @Override
  public void write(String table, String row, byte[] value) throws DatabaseAbortException {
    if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }
    String key = table + row;

    clientTransaction.addWrite(key, value);
  }

  @Override
  public void update(String table, String key, byte[] value) throws DatabaseAbortException {
    throw new RuntimeException("Unimplemented");
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
      String key = table + row;
      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      clientTransaction.addWrite(key, value);
      executeOps();

      // check if executing these statements was successful
      if (clientTransaction.hasError()) {
        clientTransaction.markFinished();
        throw new DatabaseAbortException(errorMsg);
      } else {

      }


    return readValue;

  }

  @Override
  public List<byte[]> updateAndExecute(String table, String key, byte[] value)
      throws DatabaseAbortException {
    throw new RuntimeException("Unimplemented");
  }


  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#commitTransaction()
   */
  @Override
  public synchronized List<byte[]> commitTransaction()
      throws DatabaseAbortException {

      System.out.println("[Client] Commited ");
      List<byte[]> values = new LinkedList<byte[]>();

      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      if (clientTransaction.isFirstRequest()
          && clientTransaction.getLength() == 0) {
        // Never actually sent any request, so need to bother the
        // transaction with this
        clientTransaction.markFinished();
      } else {
        clientTransaction.markCommit();
        executeOps();

        values = readValue;

        // check if executing these statements was successful
        if (clientTransaction.hasError()) {
          clientTransaction.markFinished();
          throw new DatabaseAbortException(errorMsg);
        } else {
          clientTransaction.markFinished();
        }


      }
      return values;

  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#abortTransaction()
   */
  @Override
  public synchronized void abortTransaction() throws DatabaseAbortException {

    System.out.println("[Client] Aborted ");

    if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }

    if (clientTransaction.isFirstRequest()
        && clientTransaction.getLength() == 0) {
      // Never actually sent any request, so need to bother the
      // transaction with this
      clientTransaction.markFinished();
    } else {

      clientTransaction.markAbort();
      executeOps();

      // check if executing these statements was successful
      if (clientTransaction.hasError()) {
        clientTransaction.markFinished();
        throw new DatabaseAbortException(errorMsg);
      } else {
        clientTransaction.markFinished();
      }


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
  public synchronized void executeOps() throws DatabaseAbortException {

    Set<String> addedKeys = new HashSet<String>();
    boolean newKey;
    Statement nextStatement;
    ClientMessageReq.Builder bClientMsg = ClientMessageReq.newBuilder();

    readValue = new ArrayList<>();
    bClientMsg.setClientId(getBlockId());

    // Determine whether this is the first operation that is sent
    // to the server for this transaction, or whether this
    // transaction should abort/commit
    if (clientTransaction.isFirstRequest()) {
      bClientMsg.setToStart(true);
    }
    if (clientTransaction.isCommit()) {
      bClientMsg.setToCommit(true);
    }
    if (clientTransaction.isAbort()) {
      bClientMsg.setToAbort(true);
    }

    // Mark transaction as having an ongoing statement
    clientTransaction.markOngoing();

    // Add all statements that have not yet executed yet if
    // the transaction is not about to abort (as these statements
    // will never be executed anyways
    nextStatement = clientTransaction.executeNextStatement();
    if (nextStatement != null && !clientTransaction.isAbort()) {
      while (nextStatement != null) {
        newKey = addedKeys.add((nextStatement.getKey()));
        if (!newKey) {
          clientTransaction.markFinished();
          throw new DatabaseAbortException("Cannot send conflicting statements as part"
              + "of same sequence of requests");
        }
        if (nextStatement.isWrite()) {
          localDB.put(Utility.hashPersistent(nextStatement.getKey()),
              nextStatement.getValue());
          System.out.println("[Client] Execute Write " + nextStatement.getKey());
        } else if (nextStatement.isDelete()) {
           localDB.remove(Utility.hashPersistent(nextStatement.getKey()));
          System.out.println("[Client] Execute Delete " + nextStatement.getKey());
        } else if (nextStatement.isDummy()) {
          // Do nothing
          System.out.println("[Client] Execute Dummy" + nextStatement.getKey());
        } else {
          // Read or read for update
          System.out.println("[Client] Execute Read (For Update?)" + nextStatement.getKey());
          byte[] val = localDB.get(Utility.hashPersistent(nextStatement.getKey()));
          if (val != null) {
            readValue.add(val);
          } else {
            readValue.add("".getBytes());
          }
        } nextStatement = clientTransaction.executeNextStatement();
      }

    } else {
      logOut("Nothing to execute", Logging.Level.FINE);
    }
  }

  /**
   * Executes a pre-generated transaction (the transaction already contains statements)
   *
   * @param trx - the transaction
   */
  public List<byte[]> executeTransaction(ClientTransaction trx)
      throws DatabaseAbortException {
    clientTransaction = trx;
    return commitTransaction();
  }


  /*
   * (non-Javadoc)
   *
   * @see shield.BaseNode#handleMsg(shield.network.messages.Msg.Message)
   */
  @Override
  public void handleMsg(Message msg) {

    Type ty;
    ClientMessageResp msgResp;

    ty = msg.getMessageType();

    if (ty != ClientRespMessage) {
      logErr("Incorrect message type received" + ty, CRITICAL);
      System.exit(-1);
    }

    msgResp = msg.getClientRespMsg();

    if (msgResp == null) {
      logErr("Incorrect message format, null inner message", CRITICAL);
    }

    switch (msgResp.getRespType()) {
      case REGISTER:
        handleRegisterClientReply(msgResp);
        break;
      case OPERATION:
        handleOperationReply(msgResp);
        break;
      default:
        logErr("Incorrect Message Type" + ty, CRITICAL);
        System.exit(-1);
    }
  }

  /**
   * Handles the reply for a register request
   *
   * @param rsp - the client message
   */
  public synchronized void handleRegisterClientReply(ClientMessageResp rsp) {

  }

  /**
   * Handles the reply for one or more sent out operations.
   *
   * @param rsp - the client message
   */
  public synchronized void handleOperationReply(ClientMessageResp rsp) {
    if (rsp.getIsError()) {
      System.out.println(rsp.getIsError());
      errorMsg = rsp.getErrorMsg();
      clientTransaction.markOpResult(false);
    } else {
      clientTransaction.markOpResult(true);
      readValue = rsp.getReadValuesList().stream().map(bs -> {
        byte[] value = new byte[bs.size()];
        bs.copyTo(value, 0);
        return value;
      }).collect(Collectors.toList());
    }
    notifyAll();
  }

}
