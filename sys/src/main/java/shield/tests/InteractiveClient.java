package shield.tests;

import org.json.simple.parser.ParseException;
import shield.client.Client;
import shield.client.DatabaseAbortException;
import shield.util.Logging;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Simulates a client which reads commands from the console (stdin). Used for testing. The proxy and
 * (optionally) the remote data store must already be started when starting this client.
 *
 * @author ncrooks
 */
public class InteractiveClient {

  public static void main(String[] args) throws InterruptedException,
      IOException, DatabaseAbortException, ParseException {

    String expConfigFile;
    Client client;
    Scanner s; // read from stdin

    if (args.length != 1) {
      System.err.println(
          "Incorrect number of arguments: expected <expConfigFile.json>");
    }

    expConfigFile = args[0];
    s = new Scanner(System.in);

    // Create Client with appropriate parameters
    client = new Client(expConfigFile);
    client.registerClient();

    System.out.println("Client Registered");

    while (true) {
      parseAndExecuteCommand(client, s);
    }


  }

  /**
   * Parses from command line, and executes corresponding command
   */
  private static void parseAndExecuteCommand(Client client, Scanner s) {

    String nextCommand;
    String[] commandSplit;

    nextCommand = s.nextLine();
    nextCommand.toLowerCase();
    commandSplit = nextCommand.split(" ");

    if (commandSplit.length == 0) {
      System.out.println("Empty Client");
      return;
    }

    switch (commandSplit[0]) {
      case "start":
        doStart(client, commandSplit);
        break;
      case "read":
        doRead(client, commandSplit);
        break;
      case "write":
        doWrite(client, commandSplit);
        break;
      case "commit":
        doCommit(client, commandSplit);
        break;
      case "abort":
        doAbort(client, commandSplit);
        break;
      case "quit":
        s.close();
        System.exit(0);
      default:
        System.err.println(
            "Unexpected command. Supported <start> <read key> <write key value> <commit> <abort> <quit>");
    }

  }

  /**
   * Aborts a transaction
   */
  private static void doAbort(Client client, String[] commandSplit) {
    try {
      client.abortTransaction();
    } catch (DatabaseAbortException e) {
      client.logErr(e.getMessage(), Logging.Level.CRITICAL);
      System.out.println("Abort OK");
    }
  }

  /**
   * Commits ongoing transaction
   */
  private static void doCommit(Client client, String[] commandSplit) {
    try {
      client.commitTransaction();
      System.out.println("Commit OK");
    } catch (DatabaseAbortException e) {
      System.out.println("Transaction Aborted");
    }

  }

  /**
   * Executes write operation
   */
  private static void doWrite(Client client, String[] commandSplit) {

    String table;
    String key;
    byte[] value;

    try {
      if (commandSplit.length != 4) {
        client.logErr("Incorrect Write Format. Expected <write key value>");
      } else {
        table = commandSplit[1];
        key = commandSplit[2];
        value = commandSplit[3].getBytes();
        client.writeAndExecute(table, key, value);
      }
    } catch (DatabaseAbortException e) {
      client.logErr(e.getMessage(), Logging.Level.CRITICAL);
    }
  }

  /**
   * Executes read operation
   */
  private static void doRead(Client client, String[] commandSplit) {
    String table;
    String key;
    try {
      if (commandSplit.length != 3) {
        client.logErr("Incorrect Write Format. Expected <write key value>");
      } else {
        table = commandSplit[1];
        key = commandSplit[2];
        List<byte[]> res = client.readAndExecute(table, key);
        System.out.println("Result: " + new String(res.get(0)));
      }
    } catch (DatabaseAbortException e) {
      client.logErr(e.getMessage(), Logging.Level.CRITICAL);
    }

  }

  /**
   * Executes start operation
   */
  private static void doStart(Client client, String[] commandSplit) {
    try {
      client.startTransaction();
    } catch (Exception e) {
      client.logErr(e.getMessage());
    }
  }

}
