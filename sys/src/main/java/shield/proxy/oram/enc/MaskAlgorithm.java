package shield.proxy.oram.enc;

public interface MaskAlgorithm {

  void reset();

  void computeMask(byte[] mask, byte[] clientKey, byte[] nonce);

}
