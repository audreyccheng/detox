package shield.network;

import java.net.InetSocketAddress;

import shield.network.messages.Msg;

public interface NetworkSender {

  public void send(byte[] msg, InetSocketAddress rcpt, boolean flush);

  public void send(byte[] msg, InetSocketAddress rcpt);

  public void send(Msg.Message msg, InetSocketAddress rcpt, boolean flush);

  public void send(Msg.Message msg, InetSocketAddress rcpt);

}
