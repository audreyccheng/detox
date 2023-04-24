package shield.proxy.client;

import shield.network.messages.Msg;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.ClientMessageResp.RespType;
import shield.network.messages.Msg.Message;
import shield.proxy.Proxy;
import shield.proxy.trx.concurrency.Operation;
import shield.proxy.trx.concurrency.Transaction;
import shield.util.Logging;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages all the client handlers. Its primary purpose is to multiplex based on the
 * client id of the various requests/operations that circulate in the system
 *
 * @author ncrooks
 */
public class ClientManager {

  /**
   * All currently registered clients
   */
  private final ConcurrentHashMap<Long, ClientHandler> registeredClients;

  /**
   * Back-pointer to hosting proxy
   */
  private final Proxy proxy;

  public ClientManager(Proxy proxy) {
    this.proxy = proxy;
    // TODO: remove magic numbers
    this.registeredClients =
        new ConcurrentHashMap<Long, ClientHandler>(2000, 0.75f);
  }


  public void handleMsg(Msg.ClientMessageReq msg) {
    if (msg.hasRegister()) {
      handleRegisterClient(msg);
    } else {
      handleTransactionalRequest(msg);
    }
  }


  /**
   * Multiplexes client request to the appropriate client handler
   */
  private void handleTransactionalRequest(ClientMessageReq msg) {

    Long clientId = msg.getClientId();
    ClientHandler ch = registeredClients.get(clientId);
    if (ch == null) {
      proxy.logErr("Client Id " + clientId + " was not found",
          Logging.Level.CRITICAL);
    } else {
      ch.handleTransactionalRequest(msg);
    }
  }


  private void handleRegisterClient(ClientMessageReq msg) {

    ClientHandler old;
    ClientMessageResp.Builder clientMsg;
    Msg.Message.Builder respMsg;

    Long clientId = msg.getClientId();
    String host = msg.getClientHost();
    int port = msg.getClientPort();

    ClientHandler ch = new ClientHandler(clientId, host, port, proxy);
    old = registeredClients.put(clientId, ch);

    if (old != null) {
      proxy.logErr("Cannot register the same client twice. " + "Client "
          + clientId + " already exists", Logging.Level.CRITICAL);
      System.exit(-1);
    }

    System.out.println("Registering Client " + clientId + " Nb Of Clients: "
        + registeredClients.size());

    clientMsg = ClientMessageResp.newBuilder();
    clientMsg.setRespType(RespType.REGISTER);
    clientMsg.setIsError(false);
    respMsg = Message.newBuilder();
    respMsg.setClientRespMsg(clientMsg);
    respMsg.setMessageType(Msg.Message.Type.ClientRespMessage);
    proxy.sendMsg(respMsg.build(), ch.getClientAddress(), true);

  }


  public void onTransactionStarted(Transaction t, boolean success) {

    ClientHandler ch = registeredClients.get(t.getClientId());
    if (ch == null) {
      proxy.logErr("Client Id " + t.getClientId() + " does not exist");
    } else {
      ch.onTransactionStarted(t, success);
    }
  }

  /**
   * Callback for one of the ongoing operations. If all operations have successfully executed,
   * returns control to the client or commits the transaction if msg requested that
   */
  public void onOperationExecuted(Operation op) {
    ClientHandler ch = registeredClients.get(op.getTrx().getClientId());
    if (ch == null) {
      proxy
          .logErr("Client Id " + op.getTrx().getClientId() + " does not exist");
    } else {
      ch.onOperationExecuted(op);
    }

  }


  /**
   * Transaction was successfully committed
   */
  public void onTransactionCommitted(Transaction t) {
    ClientHandler ch = registeredClients.get(t.getClientId());
    if (ch == null) {
      proxy.logErr("Client Id " + t.getClientId() + " does not exist");
    } else {
      ch.onTransactionCommitted(t);
    }

  }

  /**
   * Transaction aborted
   */
  public void onTransactionAborted(Transaction t) {
    ClientHandler ch = registeredClients.get(t.getClientId());
    if (ch == null) {
      proxy.logErr("Client Id " + t.getClientId() + " does not exist");
    } else {
      ch.onTransactionAborted(t);
    }
  }


  /**
   * Called when a cascading rollback causes this transaction to rollback
   */
  public void onRollback(Transaction t) {
    ClientHandler ch = registeredClients.get(t.getClientId());
    if (ch == null) {
      proxy.logErr("Client Id " + t.getClientId() + " does not exist");
    } else {
      ch.onRollback(t);
    }
  }
}
