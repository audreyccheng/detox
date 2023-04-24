package shield.proxy.oram.enc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MaskMessageDigest implements MaskAlgorithm {

  private final MessageDigest digest;

  public MaskMessageDigest(String alg) throws NoSuchAlgorithmException {
    this(MessageDigest.getInstance(alg));
  }

  public MaskMessageDigest(MessageDigest digest) {
    this.digest = digest;
  }

  private static void updateInt(MessageDigest digest, int i) {
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
    int i = 0;
    for (; i + digest.getDigestLength() < mask.length; i += digest.getDigestLength()) {
      digest.update(clientKey);
      digest.update(nonce);
      updateInt(digest, i);
      System.arraycopy(digest.digest(), 0, mask, i, digest.getDigestLength());
    }
    digest.update(clientKey);
    digest.update(nonce);
    updateInt(digest, i);
    System.arraycopy(digest.digest(), 0, mask, i, mask.length - i);
  }
}
