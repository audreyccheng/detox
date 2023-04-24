package shield.network;

import java.net.InetSocketAddress;

public interface NetworkQueue {

  abstract public void addWork(byte[] msg);
}
