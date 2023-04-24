package shield.client;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import shield.network.messages.Msg;
import shield.network.messages.Msg.Statement.Type;
import shield.util.Utility;

/**
 * Wrapper over individual operations in a transaction
 *
 * @author ncrooks
 */

public final class Statement {

  enum OpType {
    READ, WRITE, READ_FOR_UPDATE, DELETE, DUMMY
  }

  private final String key;
  private final byte[] value;
  private final OpType ty;

  private Statement(String key, byte[] value, OpType ty) {
    assert (key != null);
    this.key = key;
    this.value = value;
    this.ty = ty;
    if (ty == OpType.WRITE) {
      assert (value != null);
    } else {
      assert (value == null);
    }
  }

  /**
   * Creates a read operation
   *
   * @return the corresponding statement
   */
  public static Statement read(String key) {
    Statement st = new Statement(key, null, OpType.READ);
    return st;
  }

  /**
   * Creates a delete operation
   *
   * @return the corresponding statement
   */
  public static Statement delete(String key) {
    Statement st = new Statement(key, null, OpType.DELETE);
    return st;
  }

 /**
   * Creates a read for update operation
   *
   * @return the corresponding statement
   */
  public static Statement readForUpdate(String key) {
    Statement st = new Statement(key, null, OpType.READ_FOR_UPDATE);
    return st;
  }

  public static Statement dummy(String key) {
    Statement st = new Statement(key, null, OpType.DUMMY);
    return st;
  }

  /**
   * Creates a write operation
   *
   * @return the corresponding statement
   */
  public static Statement write(String key, byte[] value) {
    Statement st = new Statement(key, value, OpType.WRITE);
    return st;
  }

  public boolean isRead() {
    return ty == OpType.READ;
  }

  public boolean isWrite() {
    return ty == OpType.WRITE;
  }

  public boolean isDelete() {
    return ty == OpType.DELETE;
  }

  public boolean isDummy() {
    return ty == OpType.DUMMY;
  }

  public boolean isReadForUpdate() {
    return ty == OpType.READ_FOR_UPDATE;
  }

  public String getKey() {
    return key;
  }

  public byte[] getValue() {
    return value;
  }


  /**
   * Generate intermediate protobuffer structure
   *
   * @return builder
   */
  public Msg.Statement.Builder toProto() {
    Msg.Statement.Builder b = Msg.Statement.newBuilder();
    assert (key != null);
    System.out.println(key + " " + Utility.hashPersistent(key));
    b.setKey(Utility.hashPersistent(key));
    if (isWrite()) {
      b.setOpType(Msg.Statement.Type.WRITE);
      b.setValue(ByteString.copyFrom(value));
    } else if (isRead()) {
      b.setOpType(Msg.Statement.Type.READ);
    } else if (isDelete()) {
      b.setOpType(Msg.Statement.Type.DELETE);
    } else if (isDummy()) {
      b.setOpType(Type.DUMMY);
    } else {
      b.setOpType(Type.READ_FOR_UPDATE);
    }
    return b;
  }


}
