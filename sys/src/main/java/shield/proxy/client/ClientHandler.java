package shield.proxy.client;

import com.google.protobuf.ByteString;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import shield.network.messages.Msg;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.ClientMessageResp.RespType;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Statement;
import shield.network.messages.Msg.Statement.Type;
import shield.proxy.Proxy;
import shield.proxy.trx.concurrency.Operation;
import shield.proxy.trx.concurrency.TransactionManager;
import shield.proxy.trx.concurrency.Transaction;
import shield.proxy.trx.concurrency.Transaction.TxState;
import shield.util.CallStack;
import shield.util.Logging;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler {


  enum ClientState {
    IDLE, // No operations executing
    OPS, // Operations are executing
    ABORTING, // Transaction is doomed to abort but no commit/abort message has been received
    FINISHING // A commit/abort request has been received for this trx
  }

  private ClientState clientState;

  /**
   * Unique client id
   */
  private final long clientId;

  /**
   * Socket address on which the client will be listening
   */
  private final InetSocketAddress clientAddress;


  /**
   * Ongoing request that is being processed. If the request is null, then there is no ongoing
   * request being processed
   */
  private ClientMessageReq ongoingReq;

  /**
   * Operations left to process for ongoingReq
   */
  private AtomicInteger pendingOperations;

  /**
   * True if an operation failed during the execution of the trx
   */
  private boolean opFailed = false;

  /**
   * Currently executing transaction
   */
  private Transaction ongoingTransaction;

  /**
   * Back pointer to the proxy
   */
  private final Proxy proxy;

  /**
   * Back pointer to the transactional Manager
   */
  private final TransactionManager trxManager;

  /**
   * True if user initiated abort is ongoing
   */
  private boolean userAbort = false;

  /**
   * Debug: key for measuring execution time
   */
  private int statMeasurementKey = 0;

  /**
   * True if the current transaction aborted (due to batch/dependent aborts) but there was
   * no pending operation from the client. This implies that the next operation that
   * executes should send the abort notification (intuitively, this is buffering the abort
   * notification)
   */
  private boolean bufferedAbort = false;

  private long startTrx = 0;

  /**
   * Must be acquired when executing modifications related to client state.
   */
  private ReentrantLock clientStateLock;

  public ClientHandler(long clientId, String host, int listeningPort,
      Proxy proxy) {

    proxy.logOut("New Client: " + clientId + " " + host + " " + listeningPort,
        Logging.Level.INFO);
    this.clientId = clientId;
    this.clientAddress = new InetSocketAddress(host, listeningPort);
    this.proxy = proxy;
    this.trxManager = proxy.getTrxManager();
    assert (trxManager != null);
    this.ongoingReq = null;
    this.pendingOperations = new AtomicInteger(0);
    this.clientState = ClientState.IDLE;
    this.clientStateLock = new ReentrantLock();
  }


  /**
   * Handles incoming messages for this client. Messages contain operations and, optionally, a flag
   * to either start, commit or abort the transaction
   */
    public void handleTransactionalRequest(ClientMessageReq clientReq) {
    clientStateLock.lock();
    ongoingReq = clientReq;
    if (bufferedAbort || clientState == ClientState.ABORTING) {
      clientState = ClientState.FINISHING;
      if (bufferedAbort) onTransactionAborted(ongoingTransaction);
    } else {
      if (clientReq.hasToStart()) {
        handleTransactionalStartReq(clientReq);
      } else if (clientReq.hasToAbort()) {
        assert (ongoingTransaction != null);
        handleTransactionalAbortReq(clientReq, ongoingTransaction);
      } else {
        assert (ongoingTransaction != null);
        handleTransactionalOpsReq(clientReq, ongoingTransaction);
      }
    }
    clientStateLock.unlock();
  }


  /**
   * Starts a transaction. The transaction has successfully started once the corresponding callback
   * has been called
   */
  private void handleTransactionalStartReq(ClientMessageReq clientReq) {
    assert(ongoingTransaction == null);
    // System.out.println("[Transaction] PRESTARTED " + clientId + " ");
    Transaction t = new Transaction(clientId);
    this.ongoingTransaction = t;
    this.startTrx = System.nanoTime();
    clientStateLock.lock();
    this.ongoingReq = clientReq;
    this.clientState = ClientState.OPS;
    clientStateLock.unlock();
    proxy.executeAsync(() -> trxManager.startTransaction(this.clientId, t),
        proxy.getDefaultExpHandler());
  }

  /**
   * Handles all operations to execute. As these statements have no control flow dependencies, these
   * requests are executed in parallel. They have been successfully (or unsuccessfully) executed
   * once the corresponding number of OperationExecuted callbacks have been called
   */
  private void handleTransactionalOpsReq(ClientMessageReq clientReq, Transaction trx) {

    List<Statement> stats;

    clientStateLock.lock();
    this.ongoingReq = clientReq;
    this.clientState = ClientState.OPS;
    clientStateLock.unlock();

    stats = clientReq.getOperationsList();

    if (stats.size() > 0) {
      pendingOperations.set(stats.size());
      for (Statement stat : stats) {
        final Operation op = trx.addOperation(stat);
          proxy.executeAsync(() -> {
          trxManager.executeOperation(op);
          }, proxy.getDefaultExpHandler());
      }
    } else if (clientReq.hasToCommit()) {
      handleTransactionalCommit(clientReq, trx);
    } else {
      clientStateLock.lock();
      this.clientState= ClientState.OPS;
      ClientMessageResp.Builder clientMsg = ClientMessageResp.newBuilder();
      clientMsg.setIsError(false);
      clientMsg.setRespType(RespType.OPERATION);
      ongoingReq = null;
      clientStateLock.unlock();;
      sendMsg(clientMsg);
    }
  }

  /**
   * Attempts to commit a transaction. If this is successful, will result in a
   * onTransactionCommitted callback. Otherwise it will result in a onTransactionAborted callback
   */
  private  void handleTransactionalCommit(ClientMessageReq clientReq, Transaction trx) {
    clientStateLock.lock();
    clientState = ClientState.FINISHING;
     // System.out.println("[Transaction] COMMIT " + clientId + " " + trx.getTimestamp());
    if (!bufferedAbort) {
      proxy.executeAsync(() -> trxManager.commitTransaction(trx), proxy.getDefaultExpHandler());
    } else {
      doAbort(trx);
    }
      clientStateLock.unlock();
  }

  private void doAbort(Transaction trx) {
      // trxManager.abortTransaction(trx);
      proxy.executeAsync(() -> trxManager.abortTransaction(trx), proxy.getDefaultExpHandler());
  }


  /**
   * Aborts a transaction. Abort is completed once an OnTransactionAborted callback has been
   * called;
   */
  private void handleTransactionalAbortReq(ClientMessageReq clientReq,
      Transaction trx) {
    clientStateLock.lock();
    clientState = ClientState.FINISHING;
    ongoingReq = clientReq;
    userAbort = true;
    doAbort(trx);
    clientStateLock.unlock();
  }

  /**
   * Call back for transaction start
   */
  public void onTransactionStarted(Transaction t, boolean success) {

    ClientMessageResp.Builder clientMsg;

    // System.out.println("[Transaction] STARTED " + clientId + " " + t.getTimestamp() + " " + success + " " + trxManager.getCurrentTs());
    if (t.getTrxState() != TxState.ABORTED) {
      if (success) {
        if (this.ongoingReq.getOperationsCount() > 0) {
          handleTransactionalOpsReq(this.ongoingReq, t);
        } else {
          // there are no pending operations. Return control to
          // client after cleanup
          clientStateLock.lock();
          this.ongoingReq = null;
          clientStateLock.unlock();
          clientMsg = ClientMessageResp.newBuilder();
          clientMsg.setRespType(RespType.OPERATION);
          sendMsg(clientMsg);
        }
      } else {
        // Simply retry executing the transaction
        // System.out.println("Retrying to start transaction " + clientId);
        proxy.executeAsync(() -> trxManager.startTransaction(this.clientId, t),
        proxy.getDefaultExpHandler());

        /* t.setTrxState(TxState.ABORTED);
        assert(t==ongoingTransaction);
        System.out.println("[Transaction] Aborted on start " + clientId + " " + t.getTrxState());
        cleanupOnTrxFinish();
        clientMsg = ClientMessageResp.newBuilder();
        clientMsg.setIsError(true);
        clientMsg.setRespType(RespType.OPERATION);
        sendMsg(clientMsg); */

       }
    }
  }

  /**
   * Callback for one of the ongoing operations. If all operations have successfully executed,
   * returns control to the client or commits the transaction if msg requested that
   */
  public void onOperationExecuted(Operation op) {
     //System.out.println("Operation Executed " + op + " " + clientId + " " + pendingOperations.get() + " " + op.getTrx().getTimestamp() + " " + op.getTrx().getTrxState() + " " + CallStack.dumpTrace());
    if (op.getTrx().getTrxState()!= TxState.ABORTED) {
      // TODO(natacha): not 100% sure this fixes the bug
   if (pendingOperations.get() <= 0) {
       System.err.println("Operation Executed " + op + " " + clientId + " " + pendingOperations.get() + " " + op.getTrx().getTimestamp() + " " + op.getTrx().getTrxState());
       assert(false);
    }
       if (pendingOperations.decrementAndGet() == 0) {
        // Al operations have executed
        onFinishPendingOps(op.getTrx());
      } else {
        // Do nothing, wait until finished
      }
    }
    }

  /**
   * This function is called once all the pending operations have successfully finished executing
   */
  private void onFinishPendingOps(Transaction trx) {

    LinkedList<Operation> operations;
    Operation currentOp;
    ClientMessageResp.Builder clientMsg = ClientMessageResp.newBuilder();

    /* assert (trx.getTrxState() != TxState.ABORTED);
    assert(trx.getTrxState() != TxState.COMMITTED); */

    opFailed = trx.containsFailedOp();
    int nbExOps = trx.getOpCount() - ongoingReq.getOperationsCount();
    clientStateLock.lock();
    clientState = ClientState.FINISHING;
    if (!opFailed && !bufferedAbort) {
      if (ongoingReq.hasToCommit()) {
        assert (trx != null);
        clientStateLock.unlock();
        handleTransactionalCommit(ongoingReq, trx);
      } else {
        clientState = ClientState.IDLE;
        ongoingReq = null;
        clientStateLock.unlock();
        // send reply back
        clientMsg.setIsError(false);
        operations = trx.getOperations();
        for (int i = nbExOps; i < trx.getOpCount(); i++) {
          currentOp = operations.get(i);
          if (currentOp.getStatement().getOpType() == Statement.Type.READ
              || currentOp.getStatement().getOpType() == Type.READ_FOR_UPDATE) {
            if (currentOp.getReadValue() == null) {
              clientMsg.addReadValues(ByteString.copyFrom("".getBytes()));
            } else {
              clientMsg.addReadValues(ByteString.copyFrom(currentOp.getReadValue()));
            }
          }
        }
        clientMsg.setRespType(RespType.OPERATION);
        sendMsg(clientMsg);
      }
    } else {
      // some of the operations failed, cleanup transaction
      clientStateLock.unlock();
      // assert (trx.getTrxState() != TxState.ABORTED);
      doAbort(trx);
    }
  }


  /**
   * Transaction was successfully committed
   */
  public void onTransactionCommitted(Transaction trx) {

    // assert (trx.getTrxState() == TxState.COMMITTED);

    // System.out.println("[Transaction] COMMITTED " + clientId + " " + trx.getTimestamp());

    ClientMessageResp.Builder clientMsg = ClientMessageResp.newBuilder();
    int nbExOps =
        trx.getOpCount() - ongoingReq.getOperationsCount();
    LinkedList<Operation> operations;
    Operation currentOp;

    // send reply
    clientMsg.setIsError(false);
    clientMsg.setRespType(RespType.OPERATION);
    operations = trx.getOperations();
    for (int i = nbExOps; i < trx.getOpCount(); i++) {
      currentOp = operations.get(i);
      if (currentOp.getReadValue() != null) {
        clientMsg.addReadValues(ByteString.copyFrom(currentOp.getReadValue()));
      } else {
        clientMsg.addReadValues(ByteString.copyFrom("".getBytes()));
      }
    }
    // cleanup
    cleanupOnTrxFinish();
    sendMsg(clientMsg);
  }

  /**
   * Reinitialises data-structures on transaction abort or commit
   */
  public void cleanupOnTrxFinish() {
     assert(ongoingTransaction.getTrxState() == TxState.COMMITTED
        || ongoingTransaction.getTrxState() == TxState.ABORTED);
    double elapsed =((double)(System.nanoTime()  - startTrx)/1000000);
    clientStateLock.lock();
    this.ongoingReq = null;
    this.clientState = ClientState.IDLE;
    clientStateLock.unlock();
    this.ongoingTransaction = null;
    this.opFailed = false;
    this.userAbort = false;
    this.bufferedAbort = false;
    this.pendingOperations.set(0);
    this.startTrx = 0;
  }


  /**
   * Triggered when a transaction cascades rollback
   */
  public void onRollback(Transaction trx) {
    clientStateLock.lock();
    switch (clientState) {
      case OPS:
        // Case 1: there are currently executing operations.
        // We wait until they have finished to abort the transaction
        System.out.println("Trx " + trx.getTimestamp() + " has been buffered " );
        bufferedAbort = true;
        break;
      case IDLE:
        // Case 2: there are currently no executing operations, and the
        // client is not expecting a reply. As a result, we have to
        // abort the transaction, and buffer the notification
        // Falling through
        clientState = ClientState.ABORTING;
      case FINISHING:
        // Case 3: the database has already started a commit or an abort operation.
        // We abort the transaction and send a reply
        doAbort(trx);
        break;
    }
    clientStateLock.unlock();
  }


  /**
   * Transaction aborted callback. Need to determine whether should buffer the abort response or
   * not.
   */
  public void onTransactionAborted(Transaction trx) {


    // assert (trx.getTrxState() == TxState.ABORTED);

    clientStateLock.lock();

    // boolean shouldSendAbort = clientState != ClientState.IDLE;
    boolean shouldSendAbort = clientState == ClientState.OPS || clientState == ClientState.FINISHING;
    System.out.println("[Transaction] ABORTED " + clientId + " " + trx.getTimestamp() + " "
         + shouldSendAbort + " " + clientState);

    if (shouldSendAbort) {
      // The client is expecting a reply and therefore we can 
      // reply with abort
      ClientMessageResp.Builder clientMsg = ClientMessageResp.newBuilder();

      // cleanup
      cleanupOnTrxFinish();

      // send reply
      if (userAbort) {
        // this was a user abort
        clientMsg.setIsError(false);
      } else {
        clientMsg.setIsError(true);
      }
      clientMsg.setRespType(RespType.OPERATION);
      sendMsg(clientMsg);
    } else {
      bufferedAbort = true;
      // The client is not expecting a reply. This means that the transaction
      // had not finished executing and we will  necessarily receive a statement
      // at a later time, so buffer the abort message
    }
    clientStateLock.unlock();
  }

  public InetSocketAddress getClientAddress() {
    return clientAddress;
  }

  /**
   * Sends a response back to the client
   */
  public void sendMsg(ClientMessageResp.Builder clientMsg) {
    // System.out.println("Sending reply ");
    try {
      Msg.Message.Builder msg = Message.newBuilder();
      msg.setClientRespMsg(clientMsg.build());
      msg.setMessageType(Msg.Message.Type.ClientRespMessage);
      proxy.sendMsg(msg.build(), clientAddress);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  @Override
  public boolean equals(Object other) {
    ClientHandler otherCh;
    if (other instanceof ClientHandler) {
      otherCh = (ClientHandler) other;
      return otherCh.clientId == clientId;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return (int) clientId;
  }


}
