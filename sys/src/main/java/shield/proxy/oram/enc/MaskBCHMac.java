package shield.proxy.oram.enc;

import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

public class MaskBCHMac extends MaskBCSha256 {

  private final HMac hmac;

  public MaskBCHMac(byte[] clientKey) {
    this.hmac = new HMac(digest);
    hmac.init(new KeyParameter(clientKey));
  }

  private static void updateInt(HMac hmac, int i) {
    hmac.update((byte) ((i >> 24) & 0xFF));
    hmac.update((byte) ((i >> 16) & 0xFF));
    hmac.update((byte) ((i >> 8) & 0xFF));
    hmac.update((byte) (i & 0xFF));
  }

  @Override
  public void reset() {
    hmac.reset();
  }

  @Override
  public void computeMask(byte[] mask, byte[] clientKey, byte[] nonce) {
    int i;
    assert (mask.length % hmac.getMacSize() == 0);

    for (i = 0; i + hmac.getMacSize() < mask.length; i += hmac.getMacSize()) {
      hmac.update(clientKey, 0, clientKey.length);
      hmac.update(nonce, 0, nonce.length);
      updateInt(hmac, i);
      hmac.doFinal(mask, i);
    }
    /*
    hmac.update(nonce, 0, nonce.length);
    updateInt(hmac, i);
    System.out.println("Index i "+ i + " " + mask.length);
    hmac.doFinal(mask, i); */
  }
}
