package shield.tests;

import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Message.Builder;
import shield.util.Logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

public class TestClient extends BaseNode {

  TestClient(String configFileName)
      throws InterruptedException, IOException, ParseException {
    super(configFileName);
  }

  public TestClient() throws InterruptedException {
    super();
  }

  @Override
  public void handleMsg(Message msg) {

    Msg.Message.Type ty = msg.getMessageType();
    switch (ty) {
      case GoodbyeMessage:
        logOut("Msg: Goodbye Message received at the client!");
        break;
      case HelloMessage:
        logErr("Msg: Hello Message received at the client!");
        break;
      default:
        logErr("Incorrect Type Message " + ty, Logging.Level.CRITICAL);

    }
  }

  public static void main(String[] args)
      throws InterruptedException, IOException {

    TestClient client = new TestClient();
    System.out
        .println("Client Port is " + client.getConfig().NODE_LISTENING_PORT);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String s = in.readLine();
    int server_port = Integer.parseInt(s);
    System.out.println("Waiting for server to connect");
    Thread.sleep(10000);
    System.out.println("Starting to connect");
    InetSocketAddress addr = new InetSocketAddress("localhost", server_port);

    Builder msgB = Msg.Message.newBuilder();
    msgB.setMessageType(Msg.Message.Type.HelloMessage);
    client.sendMsg(msgB.build(), addr);
    msgB.setMessageType(Msg.Message.Type.GoodbyeMessage);
    client.sendMsg(msgB.build(), addr);
  }

}
