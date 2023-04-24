package shield.proxy.oram.enc;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

public class MaskBCSha256 implements MaskAlgorithm {

  protected final Digest digest;

  public MaskBCSha256() {
    this.digest = new SHA256Digest();
  }

  private static void updateInt(Digest digest, int i) {
    digest.update((byte) ((i >> 24) & 0xFF));
    digest.update((byte) ((i >> 16) & 0xFF));
    digest.update((byte) ((i >> 8) & 0xFF));
    digest.update((byte) (i & 0xFF));
  }

  @Override
  public void reset() {
    digest.reset();
  }

  @Override
  public void computeMask(byte[] mask, byte[] clientKey, byte[] nonce) {
    int i;
    for (i = 0; i + digest.getDigestSize() < mask.length; i += digest.getDigestSize()) {
      digest.update(clientKey, 0, clientKey.length);
      digest.update(nonce, 0, nonce.length);
      updateInt(digest, i);
      digest.doFinal(mask, i);
    }
    digest.update(clientKey, 0, clientKey.length);
    digest.update(nonce, 0, nonce.length);
    updateInt(digest, i);
    digest.doFinal(mask, i);
  }
}
