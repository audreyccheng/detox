package shield.proxy.oram;

import shield.proxy.oram.enc.MaskAlgorithm;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockMask {

  byte[] nonce;
  private final boolean encrypt;
  private final byte[] clientKey;
  private final ThreadLocal<MaskAlgorithm> alg;
  final ThreadLocal<byte[]> mask;

  public BlockMask(boolean encrypt, int nonceLength, ThreadLocal<byte[]> maskByteArray, ThreadLocal<MaskAlgorithm> alg, byte[] clientKey) {
    this.nonce = new byte[nonceLength];
    this.encrypt = encrypt;
    this.mask = maskByteArray;
    this.clientKey = clientKey;
    this.alg = alg;
  }

  /**
   * Constructs a mask using the nonce and XORs it with the value to encrypt it.
   * <p>
   * Most importantly, if the nonce has not changed: mask(mask(value)) == value.
   *
   * @param value: The value to encrypt.
   * @return: The encrypted value.
   */
  public void mask(byte[] value) {
    assert value != null;

    //apply mask to value in-place
    if (encrypt) {
      alg.get().reset();
      alg.get().computeMask(mask.get(), clientKey, nonce);
      xor(value, mask.get());
    }
  }

  public void unmask(byte[] value) {
    assert value != null;

    //apply mask to value in-place
    if (encrypt) {
      alg.get().reset();
      alg.get().computeMask(mask.get(), clientKey, nonce);
      xor(value, mask.get());
    }
  }

  public void generateNonce(Random rng) {
    if (encrypt) {
      rng.nextBytes(nonce);
    }
  }

  private static void xor(byte[] value, byte[] mask) {
    for (int j = 0; j < value.length; ++j) {
      value[j] ^= mask[j];
    }
  }

  byte[] getNonce() {
    return nonce;
  }

  public BlockMask copy() {
    BlockMask bm = new BlockMask(encrypt, nonce.length, mask, alg, clientKey);
    bm.nonce = nonce;
    return bm;
  }
}
