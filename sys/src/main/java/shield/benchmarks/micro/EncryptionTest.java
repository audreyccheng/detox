package shield.benchmarks.micro;

import com.google.common.primitives.Ints;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.prng.X931SecureRandomBuilder;
import org.bouncycastle.pqc.math.linearalgebra.Permutation;
import shield.benchmarks.utils.Generator;
import shield.util.Utility;

public class EncryptionTest {


  /**
   * Constructs a mask using the nonce and XORs it with the value to encrypt it.
   * <p>
   * Most importantly, if the nonce has not changed: mask(mask(value)) == value.
   *
   * @param value: The value to encrypt.
   * @return: The encrypted value.
   */
  public static long maskUsingJavaSecurity(byte[] value) throws NoSuchAlgorithmException {
    byte[] clientKey = new byte[32];
    byte[] nonce = new byte[1];
    long time = 0;
    long begin = 0 ;

    long seed = 421;
    for (int i = 0; i < 8; ++i) {
      clientKey[7 - i] = (byte) (seed & 0x000000FF);
      seed >>= 8;
    }
    // SecureRandom rng = new SecureRandom();
    Random rng = new Random(0);
    rng.nextBytes(nonce);
    assert value != null;
    //apply mask to value in-place
    begin = System.nanoTime();
    byte[] mask = new byte[value.length];
    MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
    int i = 0;
    for (; i + sha1.getDigestLength() < value.length; i += sha1.getDigestLength()) {
     sha1.update(clientKey);
     sha1.update(nonce);
     sha1.update(Ints.toByteArray(i));
     System.arraycopy(sha1.digest(), 0, mask, i, sha1.getDigestLength());
    }
     sha1.update(clientKey);
     sha1.update(nonce);
     sha1.update(Ints.toByteArray(i));
     System.arraycopy(sha1.digest(), 0, mask, i, mask.length - i);
      assert mask.length == value.length;
      for (int j = 0; j < value.length; ++j) {
         value[j] ^= mask[j];
      }
    time = System.nanoTime() - begin;
    return time;
    }



    public static long maskUsingApacheCodec(byte[] value) throws NoSuchAlgorithmException {
    byte[] clientKey = new byte[32];
    byte[] nonce = new byte[1];
    long time = 0;
    long begin = 0 ;

    long seed = 421;
    for (int i = 0; i < 8; ++i) {
      clientKey[7 - i] = (byte) (seed & 0x000000FF);
      seed >>= 8;
    }
    SecureRandom rng = SecureRandom.getInstance("SHA1PRNG");
    rng.nextBytes(nonce);
    assert value != null;
    //apply mask to value in-place
    begin = System.nanoTime();
    byte[] mask = new byte[value.length];
    MessageDigest sha1 = DigestUtils.getSha256Digest();
    // sha1.reset();
   int i = 0;
    for (; i + sha1.getDigestLength() < value.length; i += sha1.getDigestLength()) {
      sha1.update(clientKey);
      sha1.update(nonce);
      sha1.update(Ints.toByteArray(i));
      System.arraycopy(sha1.digest(), 0, mask, i, sha1.getDigestLength());
    }
     sha1.update(clientKey);
     sha1.update(nonce);
     sha1.update(Ints.toByteArray(i));
     System.arraycopy(sha1.digest(), 0, mask, i, mask.length - i);
      for (int j = 0; j < value.length; ++j) {
         value[j] ^= mask[j];
      }
    time = System.nanoTime() - begin;
    return time;

    }

public static long maskUsingBouncyCastle(byte[] value) throws NoSuchAlgorithmException {
    byte[] clientKey = new byte[32];
    byte[] nonce = new byte[1];
    long time = 0;
    long begin = 0 ;

    long seed = 421;
    for (int i = 0; i < 32; ++i) {
      clientKey[31 - i] = (byte) (seed & 0x000000FF);
      seed >>= 8;
    }
    SecureRandom rng = new SecureRandom();
    rng.nextBytes(nonce);
    assert value != null;
    //apply mask to value in-place
    begin = System.nanoTime();
    byte[] mask = new byte[value.length];
    org.bouncycastle.crypto.Digest sha1 = new SHA256Digest();
   int i = 0;
    for (i =0 ; i + sha1.getDigestSize() < value.length; i += sha1.getDigestSize()) {
      sha1.update(clientKey, 0, clientKey.length);
      sha1.update(nonce, 0, nonce.length);
      sha1.update(Ints.toByteArray(i), 0, Ints.BYTES);
      sha1.doFinal(mask,i);
    }
      sha1.update(clientKey, 0, clientKey.length);
      sha1.update(nonce, 0, nonce.length);
      sha1.update(Ints.toByteArray(i), 0, Ints.BYTES);
      sha1.doFinal(mask, mask.length -i);
      for (int j = 0; j < value.length; ++j) {
         value[j] ^= mask[j];
      }
    time = System.nanoTime() - begin;
    return time;

    }


 public static long maskUsingHMac(byte[] value) throws NoSuchAlgorithmException {
  byte[] clientKey = new byte[32];
   byte[] nonce = new byte[1];
   long time = 0;
   long begin = 0 ;

   long seed = 421;
   for (int i = 0; i < 32; ++i) {
     clientKey[31 - i] = (byte) (seed & 0x000000FF);
     seed >>= 8;
   }

   Digest digest = new SHA256Digest();
   HMac sha1= new HMac(digest);
   KeyParameter param = new KeyParameter(clientKey);
   byte[] mask = new byte[value.length];
   int i = 0;
   sha1.init(param);

   begin = System.nanoTime();
    for (; i + sha1.getMacSize() < value.length; i += sha1.getMacSize()) {
      sha1.update(nonce, 0, nonce.length);
      sha1.update(Ints.toByteArray(i), 0, Ints.BYTES);
      sha1.doFinal(mask,i);
    }
      sha1.update(nonce, 0, nonce.length);
      sha1.update(Ints.toByteArray(i), 0, Ints.BYTES);
      sha1.doFinal(mask,mask.length -i);
      for (int j = 0; j < value.length; ++j) {
         value[j] ^= mask[j];
      }
    time = System.nanoTime() - begin;
    return time;
  }

  public static void main(String[] args ) throws NoSuchAlgorithmException {
    SecureRandom gen = new SecureRandom();
    System.out.println(gen.getAlgorithm());
    System.out.println(gen.getProvider());
    SecureRandom gen2 = SecureRandom.getInstance("NativePRNGNonBlocking");
    System.out.println(gen2.getProvider());
    System.out.println(gen2.getAlgorithm());
    SecureRandom gen3 = SecureRandom.getInstance("SHA1PRNG");
    System.out.println(gen3.getAlgorithm());

    gen3 = new X931SecureRandomBuilder().build(new AESFastEngine(),
        new KeyParameter(new SecureRandom().generateSeed(32)), false);
    System.out.println(gen3.getAlgorithm());

    Permutation p = new Permutation(296, gen3);
    p.getVector();



    byte[] val = Generator.generateBytes(100);
    long time = 0;
    double result = 0;
    for (int i = 0 ; i < 1000000 ; i++) {
      time+=maskUsingJavaSecurity(val);
    }
    result = ((double) (time / 1000000)) / 1000000;
    System.out.println("Java Sec " + result);
    time = 0;
    result = 0;
    for (int i = 0 ; i < 1000000 ; i++) {
       time+=maskUsingApacheCodec(val);
    }
    result = ((double) (time / 1000000)) / 1000000;
    System.out.println("Java Sec " + result);
    time = 0;
    result = 0;
    for (int i = 0 ; i < 1000000 ; i++) {
      time+=maskUsingBouncyCastle(val);
    }
    result = ((double) (time / 1000000)) / 1000000;
     System.out.println("Java Sec " + result);
    time = 0;
    result = 0;
    for (int i = 0 ; i < 1000000 ; i++) {
      time+=maskUsingHMac(val);
    }
    result = ((double) (time / 1000000)) / 1000000;
    System.out.println("Java Sec " + result);

  }
}

