package shield.proxy.oram;

import shield.proxy.Proxy;

public class ORAMConfig {

  public static int nonceLen = 10;

  // TODO(soumya): Import public key here.
  public static byte[] clientKey = new byte[0];

  public static void setStaticFields(Proxy p) {
    nonceLen = p.getConfig().ORAM_NONCE_LEN;
    clientKey = p.getConfig().CLIENT_KEY;
  }
}
