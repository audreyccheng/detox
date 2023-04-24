package shield.util;

/**
 * Utility functions for converting between unsigned integer and longs and java types.
 *
 * Maps unsigned 16 bit integers to int and unsigned 32 bit integers to long.
 *
 * Relies on network byte order for the byte representation.
 **/
public class UnsignedTypes {

  final public static long minusOne = 4294967295l;

  // the following correspond to c unsigned byte sizes
  final public static int uint64Size = 8;
  final public static int uint32Size = 4;
  final public static int uint16Size = 2;

  // the following translate the unsigned c sizes into java objects required
  // for the similar encoding
  final public static int intsize = uint16Size;
  final public static int longsize = uint32Size;
  final public static int longlongsize = uint32Size;

  /**
   * Input: 2 bytes
   *
   * Output: java int (4 byte) representation of that int. Effectively a 16 bit unsigned int
   **/
  public static int bytesToInt(byte[] buf) {
    if (buf.length != 2) {
      throw new RuntimeException("invalid buffer length");
    }
    return bytesToInt(buf, 0);
  }

  public static int bytesToInt(byte[] buf, int offset) {
    if (buf.length - offset < 2) {
      throw new RuntimeException("Invalid buffer length");
    }
    char anUnsignedInt = 0;

    int firstByte = 0;
    int secondByte = 0;

    firstByte = (0x000000FF & ((int) buf[offset]));
    secondByte = (0x000000FF & ((int) buf[offset + 1]));
    anUnsignedInt = (char) (firstByte << 8 | secondByte);

    return anUnsignedInt;
  }

  /**
   * input: java int outputs: byte represention of an unsigned 16 bit integer
   **/
  public static byte[] intToBytes(int val) {
    byte[] buf = new byte[2];
    intToBytes(val, buf, 0);
    return buf;
  }

  public static void intToBytes(int val, byte[] buf, int offset) {
    buf[offset + 0] = (byte) ((val & 0xFF00) >> 8);
    buf[offset + 1] = (byte) (val & 0x00FF);
  }

  /**
   * Input: 4 bytes
   *
   * Output: a java long (8 bytes) representation of the unsigned 32 bit long
   **/
  public static long bytesToLong(byte[] buf) {
    if (buf.length != 4) {
      throw new RuntimeException("invalid byte array length");
    }
    return bytesToLong(buf, 0);
  }

  public static long bytesToLong(byte[] buf, int offset) {
    if (buf.length - offset < 4) {
      throw new RuntimeException("invalid byte array length");
    }

    long anUnsignedInt = 0;

    int firstByte = 0;
    int secondByte = 0;
    int thirdByte = 0;
    int fourthByte = 0;

    firstByte = (0x000000FF & ((int) buf[offset + 0]));
    secondByte = (0x000000FF & ((int) buf[offset + 1]));
    thirdByte = (0x000000FF & ((int) buf[offset + 2]));
    fourthByte = (0x000000FF & ((int) buf[offset + 3]));
    anUnsignedInt = ((long) (firstByte << 24 | secondByte << 16 | thirdByte << 8
        | fourthByte)) & 0xFFFFFFFFL;

    return anUnsignedInt;
  }

  /**
   * input: java long output: byte representation of an unsigned 32 bit long
   **/
  public static byte[] longToBytes(long val) {
    byte[] buf = new byte[4];
    longToBytes(val, buf, 0);
    return buf;
  }

  public static void longToBytes(long val, byte[] buf, int offset) {
    buf[offset + 0] = (byte) ((val & 0xFF000000L) >> 24);
    buf[offset + 1] = (byte) ((val & 0x00FF0000L) >> 16);
    buf[offset + 2] = (byte) ((val & 0x0000FF00L) >> 8);
    buf[offset + 3] = (byte) (val & 0x000000FFL);
  }

  /**
   * Input: 8 bytes
   *
   * Output: a java long (8 bytes) representation of the unsigned 64 bit long
   **/

  public static long bytesToLongLong(byte[] buf) {
    if (buf.length != 8) {
      throw new RuntimeException("invalid byte array length");
    }
    return bytesToLongLong(buf, 0);
  }

  public static long bytesToLongLong(byte[] buf, int offset) {
    if (buf.length - offset < 8) {
      throw new RuntimeException("invalid byte array length");
    }

    long anUnsignedInt = 0;

    long firstByte = 0;
    long secondByte = 0;
    long thirdByte = 0;
    long fourthByte = 0;
    long fifthByte = 0;
    long sixthByte = 0;
    long seventhByte = 0;
    long eightByte = 0;

    firstByte = (0x00000000000000FFL & ((long) buf[offset + 0]));
    secondByte = (0x00000000000000FFL & ((long) buf[offset + 1]));
    thirdByte = (0x00000000000000FFL & ((long) buf[offset + 2]));
    fourthByte = (0x00000000000000FFL & ((long) buf[offset + 3]));
    fifthByte = (0x00000000000000FFL & ((long) buf[offset + 4]));
    sixthByte = (0x00000000000000FFL & ((long) buf[offset + 5]));
    seventhByte = (0x00000000000000FFL & ((long) buf[offset + 6]));
    eightByte = (0x00000000000000FFL & ((long) buf[offset + 7]));
    anUnsignedInt = ((long) (firstByte << 56 | secondByte << 48
        | thirdByte << 40 | fourthByte << 32 | fifthByte << 24 | sixthByte << 16
        | seventhByte << 8 | eightByte)) & 0xFFFFFFFFFFFFFFFFL;

    return anUnsignedInt;
  }

  /**
   * input: java long output: byte representation of an unsigned 32 bit long
   **/
  public static byte[] longlongToBytes(long val) {
    byte[] buf = new byte[8];
    longlongToBytes(val, buf, 0);
    return buf;
  }

  public static void longlongToBytes(long val, byte[] buf, int offset) {
    buf[offset + 0] = (byte) ((val & 0xFF00000000000000L) >> 56);
    buf[offset + 1] = (byte) ((val & 0x00FF000000000000L) >> 48);
    buf[offset + 2] = (byte) ((val & 0x0000FF0000000000L) >> 40);
    buf[offset + 3] = (byte) ((val & 0x000000FF00000000L) >> 32);
    buf[offset + 4] = (byte) ((val & 0x00000000FF000000L) >> 24);
    buf[offset + 5] = (byte) ((val & 0x0000000000FF0000L) >> 16);
    buf[offset + 6] = (byte) ((val & 0x000000000000FF00L) >> 8);
    buf[offset + 7] = (byte) (val & 0x00000000000000FFL);
  }

  public static String bytesToString(byte[] buf) {
    String tmp = "";
    for (int i = 0; i < buf.length; i++) {
      tmp += buf[i] + " ";
      if (i % 16 == 15) {
        tmp += "\n";
      }
    }
    return tmp;
  }

  static final byte[] HEX_CHAR_TABLE =
      {(byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5',
          (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a',
          (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'};

  public static String bytesToHexString(byte[] raw) {
    byte[] hex = new byte[2 * raw.length];
    int index = 0;

    for (byte b : raw) {
      int v = b & 0xFF;
      hex[index++] = HEX_CHAR_TABLE[v >>> 4];
      hex[index++] = HEX_CHAR_TABLE[v & 0xF];
    }
    try {
      return new String(hex, "ASCII");
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
