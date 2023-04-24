package shield.proxy.oram.recover;

import com.google.common.primitives.Longs;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

public class RecoveryEncrypter {

  private final HMac hmac;

  public RecoveryEncrypter(byte[] clientKey) {
    hmac = new HMac(new SHA256Digest());
    hmac.init(new KeyParameter(clientKey));
  }

  private static void updateInt(HMac hmac, int i) {
    hmac.update((byte) ((i >> 24) & 0xFF));
    hmac.update((byte) ((i >> 16) & 0xFF));
    hmac.update((byte) ((i >> 8) & 0xFF));
    hmac.update((byte) (i & 0xFF));
  }

  public void mask(byte[] data, Long seedKey) {
    mask(data, Longs.toByteArray(seedKey));
  }

  public void mask(byte[] data, byte[] seed) {
    int i;
    byte[] dataPadded = new byte[data.length + (hmac.getMacSize() - data.length % hmac.getMacSize())];
    System.arraycopy(data, 0, dataPadded, 0, data.length);
    byte[] mask = new byte[dataPadded.length];
    for (i = 0; i + hmac.getMacSize() < mask.length; i += hmac.getMacSize()) {
      hmac.update(seed, 0, seed.length);
      updateInt(hmac, i);
      hmac.doFinal(mask, i);
    }
    for (int j = 0; j < data.length; ++j) {
      dataPadded[j] ^= mask[j];
    }
    System.arraycopy(dataPadded, 0, data, 0, data.length);
  }
}
