package shield.proxy.oram.enc;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.NoSuchAlgorithmException;

public class MaskAlgorithmFactory {

  public static MaskAlgorithm getAlgorithm(MaskAlgorithmType type, byte[] clientKey) throws NoSuchAlgorithmException {
    switch (type) {
      case JAVA_SHA1:
        return new MaskMessageDigest("SHA-1");
      case JAVA_SHA256:
        return new MaskMessageDigest("SHA-256");
      case APACHE_SHA256:
        return new MaskMessageDigest(DigestUtils.getSha256Digest());
      case BC_SHA256:
        return new MaskBCSha256();
      case BC_HMAC:
        return new MaskBCHMac(clientKey);
      default:
        System.err.printf("Unsupported MaskAlgorithmType: %s\n", type.toString());
        System.exit(-1);
    }
    return null;
  }

}
