package shield.benchmarks.utils;

import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import shield.benchmarks.ycsb.utils.Utils;

/**
 * Utility class for generating uniformly random booleans, bytes, strings, integers, longs
 *
 * @author ncrooks
 */
public class Generator {

  private static Random random = new Random(Utils.hash(System.currentTimeMillis()));

  private static char[] characters =
      {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
          'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};


  /**
   * Generates random boolean with uniform probability
   *
   * @return the generated boolean
   */
  public static boolean generateBoolean() {

    return random.nextBoolean();
  }

  /**
   * Generates random byte array of size i with uniform probability
   *
   * @param i - the size of the string
   * @return the generated string
   */
  public static byte[] generateBytes(int i) {

    int size = i;
    byte[] bytes = new byte[size];
    new Random().nextBytes(bytes);
    return bytes;

  }

  /**
   * Generates random byte string of size i with uniform probability
   */
  public static ByteString generateByteString(int length) {

    char[] text = new char[length];
    for (int i = 0; i < length; i++) {
      text[i] = characters[(random.nextInt(characters.length))];
    }
    return ByteString.copyFrom(new String(text).getBytes());
  }

  /**
   * Generates random integer [i, j) with uniform probability
   */
  public static int generateInt(int i, int j) {
    if (i == j) {
      return i; // 1; // random.nextInt(1);
    }
    return random.nextInt(j - i) + i;
  }

  /**
   * Generate long with uniform probability
   */
  public static Long generateLong() {

    return random.nextLong();
  }

  /**
   * Generates long with uniform probability bounded by [i, j)
   */
  public static long generateLong(long i, long j) {

    long ret = random.nextLong();
    while (ret < i || ret >= j) {
      ret = random.nextLong();
    }
    return ret;
  }

  /**
   * Generates random port number
   */
  public static int generatePortNumber() {

    int offset = 10000;
    int port = random.nextInt(65355 - offset) + offset;
    return port;
  }

  /**
   * Generates random string of length n with uniform probability
   */
  public static String generateString(int length, Random ran) {

    char[] text = new char[length];
    for (int i = 0; i < length; i++) {
      text[i] = characters[(ran.nextInt(characters.length))];
    }
    return new String(text);
  }

  public static String generateString(int length) {
    return generateString(length,random);
  }



  /**
   * Generates random UUID with uniform probability as byte[]
   */
  public static byte[] getUuidAsBytes() {

    int size = 16;
    byte[] bytes = new byte[size];
    new Random().nextInt(25);
    return bytes;
  }

  /**
   * Generates random UUID with uniform probability as char[]
   */
  public static char[] getUUIDasChar() {

    char[] alph = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
        'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    char[] uuid = new char[16];
    for (int i = 0; i < 16; i++) {
      uuid[i] = alph[random.nextInt(25)];
    }
    return uuid;
  }


  // Code adapted from oltpbench
  public static int getGaussian( int min,  int max) {
    int value = -1;
    int range_size = (max-min)+1;
    while (value < 0 || value >= range_size) {
      double gaussian = (random.nextGaussian() + 2.0) / 4.0;
      value = (int)Math.round(gaussian * range_size);
    }
    return (value + min);
  }


  public static int getDiscrete(List<Integer> values, List<Double> weights) {
    double prob = 0.0;
    double randDouble = random.nextDouble();
    int val = 0;
    int index = 0;
    while (prob < randDouble) {
      val = values.get(index);
      prob += weights.get(index);
      index += 1;
    }
    return val;
  }


}
