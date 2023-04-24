package shield.benchmarks.micro;

import org.apache.commons.crypto.cipher.CryptoCipher;
import org.apache.commons.crypto.cipher.CryptoCipherFactory;
import org.apache.commons.crypto.cipher.CryptoCipherFactory.CipherProvider;
import org.apache.commons.crypto.utils.Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;

/**
 * @author ncrooks
 *
 * Microbenchmark to measure encryption/decryption over increasing block sizes Uses OpenSSL under
 * the hood via the apache.commons.crypto lib. I can currently only get this working with OpenSSL.
 */

public class EncryptionBenchmark {

  public static void main(String[] args) throws Exception {

    long start = 0;
    long end = 0;
    double time = 0.0;
    Random ran = new Random();
    int max_size = 10000000; // 10Mb
    byte[] input;
    byte[] output;
    byte[] decoded;

    System.out.println("AES");
    System.out.println("---------------");
    start = System.nanoTime();
    final SecretKeySpec key =
        new SecretKeySpec(getUTF8Bytes("1234567890123456"), "AES");

    Properties properties = new Properties();
     properties.setProperty(CryptoCipherFactory.CLASSES_KEY,
     CipherProvider.OPENSSL.getClassName());
    //  final String transform = "AES/CBC/NoPadding";
    // final String transform = "AES/CBC/PKCS5Padding";
     final String transform = "AES/CTR/NoPadding";
    // final String transform = "RSA/ECB/PKCS1Padding";
    // final String transform = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    CryptoCipher encipher = Utils.getCipherInstance(transform, properties);

    for (int size = 1; size < max_size; size = size * 10) {

      assert (size * 160 < Integer.MAX_VALUE);
      input = new byte[size];
      output = new byte[size * 160];
      decoded = new byte[size];
      ran.nextBytes(input);

     // start = System.nanoTime();
      final IvParameterSpec iv =
        new IvParameterSpec(getUTF8Bytes("1234567890123456"));
      /* end = System.nanoTime();
      time = (end - start) / 1000000.0;
      System.out.println("Initialization cost: " + time + " ms "); */
     encipher.init(Cipher.ENCRYPT_MODE, key, iv);
      int updateBytes = encipher.update(input, 0, input.length, output, 0);
      int finalBytes = encipher.doFinal(input, 0, 0, output, updateBytes);
      end = System.nanoTime();
      encipher.close();
      time = (end - start) / 1000000.0;
      System.out
          .println("Size: " + size + " bytes - Encryption " + time + " ms ");

      // decrypt
   //   properties.setProperty(CryptoCipherFactory.CLASSES_KEY,
   //       CipherProvider.OPENSSL.getClassName());
      CryptoCipher decipher = Utils.getCipherInstance(transform, properties);

      start = System.currentTimeMillis();
      decipher.init(Cipher.DECRYPT_MODE, key, iv);
      decipher.doFinal(output, 0, updateBytes + finalBytes, decoded, 0);
      end = System.currentTimeMillis();
      time = (end - start) / 100000.0;
      System.out
          .println("Size: " + size + " bytes - Decryption " + time + " ms ");
      System.out.println("Success " + isEqual(input, decoded));
    }
  }

  static boolean isEqual(byte[] input, byte[] output) {
    int size = input.length;
    for (int i = 0; i < size; i++) {
      if (input[i] != output[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Converts String to UTF8 bytes
   *
   * @param input the input string
   * @return UTF8 bytes
   */
  private static byte[] getUTF8Bytes(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }

}
