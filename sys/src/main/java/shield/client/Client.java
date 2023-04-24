package shield.client;

import java.util.*;

import org.json.simple.parser.ParseException;
import shield.client.schema.Table;
import shield.network.messages.Msg;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Message.Type;
import shield.util.Logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import static shield.network.messages.Msg.Message.Type.ClientReqMessage;
import static shield.network.messages.Msg.Message.Type.ClientRespMessage;
import static shield.util.Logging.Level.CRITICAL;
import static shield.util.Logging.Level.INFO;


/**
 * Represents a client. Interfaces to library code. Clients are single threaded and can only have
 * one ongoing transaction at a time.
 *
 * TODO(Natacha): Code needs to be refactored to reduce code duplication
 *
 * @author ncrooks
 */
public final class Client extends ClientBase {

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

  /**
   * Client is registed
   */
  private volatile boolean isRegistered = false;

  /**
   * Address of the proxy
   */
  private InetSocketAddress proxyAddress;

  private InetSocketAddress currentAddress;

  int trxPieces = 0;



  public Client() throws InterruptedException, IOException, ParseException {
    super();
    initClient();
  }

  public Client(String configFileName) throws InterruptedException, ParseException, IOException {
    super(configFileName);
    initClient();
  }

  public Client(String configFileName, Map<Long, ReadWriteLock> keyLocks) throws InterruptedException, ParseException, IOException {
    super(configFileName, keyLocks);
    initClient();
  }

  public Client(String configFileName, int port, int uid) throws InterruptedException, ParseException, IOException {
    super(configFileName, port, uid);
    initClient();
  }


  public Client(String configFileName, String address, int port, int uid) throws InterruptedException, ParseException, IOException {
    super(configFileName, address, port, uid);
    initClient();
  }

  private void initClient() {
   clientTransaction = new ClientTransaction();
    readValue = null;
    currentAddress = new InetSocketAddress(config.NODE_IP_ADDRESS,
        config.NODE_LISTENING_PORT);
    proxyAddress = new InetSocketAddress(config.PROXY_IP_ADDRESS,
        config.PROXY_LISTENING_PORT);
    databaseSchema = new HashMap<String, Table>();
  }



  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#registerClient()
   */
  @Override
  public synchronized void registerClient() throws DatabaseAbortException {

    try {

      logOut("Register Client " + this.getBlockId() + " " + currentAddress,
          Logging.Level.INFO);

      // First send message
      ClientMessageReq.Builder clientMsgB = ClientMessageReq.newBuilder();
      clientMsgB.setRegister(true);
      clientMsgB.setClientHost(currentAddress.getHostName());
      clientMsgB.setClientPort(currentAddress.getPort());
      clientMsgB.setClientId(this.getBlockId());
      Msg.Message.Builder msgB = Msg.Message.newBuilder();
      msgB.setClientReqMsg(clientMsgB);
      msgB.setMessageType(ClientReqMessage);
      sendMsg(msgB.build(), proxyAddress, true);

      // Wait for reply
      while (!isRegistered) {
        wait();
      }

      // Check that reply was successful
      if (errorMsg != null) {
        logErr(errorMsg, CRITICAL);
        System.exit(-1);
      } else {
        logOut("Client registered " + currentAddress.getPort(), INFO);
      }
    } catch (InterruptedException e) {
      logErr("Client Interrupted", CRITICAL);
    } catch (Exception e ) {
      e.printStackTrace();;
      System.exit(-1);
    }
  }


  public void startTransaction() throws DatabaseAbortException {
    trxPieces = 0;
    clientTransaction.markStart(0);
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
      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      String key = table + row;

      // Generate and send any non-executed statements
      clientTransaction.addRead(key);
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

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#read(java.lang.String)
   */
  @Override
  public synchronized  void read(String tableName, String row) throws DatabaseAbortException {
    if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }
    String key = tableName + row;
    clientTransaction.addRead(key);
  }

  @Override
  public synchronized List<byte[]> execute() throws DatabaseAbortException {
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
  public synchronized  List<byte[]> readForUpdateAndExecute(String table, String row)
      throws DatabaseAbortException {
    try {
      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      String key = table + row;

      // Generate and send any non-executed statements
      if (config.READ_FOR_UPDATE) clientTransaction.addReadForUpdate(key);
          else clientTransaction.addRead(key);
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
  public synchronized  void readForUpdate(String tableName, String row) throws DatabaseAbortException {
  if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }
    String key = tableName + row;
   if (config.READ_FOR_UPDATE) clientTransaction.addReadForUpdate(key);
          else clientTransaction.addRead(key);
   }

  @Override
  public synchronized void delete(String tableName, String row) throws DatabaseAbortException {
  if (!clientTransaction.hasStarted()) {
      logErr("Transaction is not started", CRITICAL);
      System.exit(-1);
    }
    String key = tableName + row;
    clientTransaction.addDelete(key);
   }

  @Override
  public synchronized List<byte[]> deleteAndExecute(String table, String row) throws DatabaseAbortException {
    try {
      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      String key = table + row;

      // Generate and send any non-executed statements
      clientTransaction.addDelete(key);
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
    write(table,key,value);
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
      String key = table + row;
      if (!clientTransaction.hasStarted()) {
        logErr("Transaction is not started", CRITICAL);
        System.exit(-1);
      }

      clientTransaction.addWrite(key, value);
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
    }

    return readValue;

  }

  @Override
  public List<byte[]> updateAndExecute(String table, String key, byte[] value)
      throws DatabaseAbortException {
    return writeAndExecute(table,key,value);
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

        // Wait for operations to finish executing
        while (clientTransaction.hasOngoingReq()) {
          wait();
        }

        values = readValue;


        // check if executing these statements was successful
        if (clientTransaction.hasError()) {
          clientTransaction.markFinished();
          throw new DatabaseAbortException(errorMsg);
        } else {
          clientTransaction.markFinished();
        }

      System.out.println("Transaction Committed");

      }
      return values;
    } catch (InterruptedException e) {
      logErr("Client was interrupted", CRITICAL);
      System.exit(-1);
    }

    return readValue;

  }

  /*
   * (non-Javadoc)
   *
   * @see shield.client.ClientBase#abortTransaction()
   */
  @Override
  public synchronized void abortTransaction() throws DatabaseAbortException {
    try {

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

        // Wait for operations to finish executing
        while (clientTransaction.hasOngoingReq()) {
          wait();
        }

        // check if executing these statements was successful
        if (clientTransaction.hasError()) {
          clientTransaction.markFinished();
          throw new DatabaseAbortException(errorMsg);
        } else {
          clientTransaction.markFinished();
        }


      }

    } catch (InterruptedException e) {
      logErr("Client was interrupted", CRITICAL);
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
      }    bClientMsg.addOperations(nextStatement.toProto());
        nextStatement = clientTransaction.executeNextStatement();
      }

    } else {
      logOut("Nothing to execute ", Logging.Level.FINE);
    }

    try {
      // Send the message
      Message.Builder bMsg = Message.newBuilder();
      bMsg.setClientReqMsg(bClientMsg);
      bMsg.setMessageType(ClientReqMessage);
      sendMsg(bMsg.build(), proxyAddress);
    } catch (Exception e) {
      System.err.println(e.getMessage());
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

    logOut("Received Reg Client Reply", Logging.Level.FINE);

    if (rsp.getIsError()) {
      errorMsg = rsp.getErrorMsg();
      isRegistered = true;
    } else {
      isRegistered = true;
    }

    notifyAll();

  }

  /**
   * Handles the reply for one or more sent out operations.
   *
   * @param rsp - the client message
   */
  public synchronized void handleOperationReply(ClientMessageResp rsp) {
    if (rsp.getIsError()) {
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
