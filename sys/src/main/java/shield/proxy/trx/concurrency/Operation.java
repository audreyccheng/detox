package shield.proxy.trx.concurrency;

import com.google.protobuf.ByteString;
import shield.network.messages.Msg;
import shield.network.messages.Msg.Statement.Type;

/**
 * Wrapper for individual read/write operations within a transaction
 *
 * @author ncrooks
 */
public final class Operation implements Comparable<Operation> {

  /**
   * Reference to the raw client statement from which this request originates
   */
  private final Msg.Statement statement;
  /**
   * Reference to the transaction in which this operation is executing
   */
  private final Transaction trx;
  private final int opId;
  /**
   * Place-holder for the read value if this statement was a read
   */
  private byte[] readValue;

  /**
   * Was this transaction successfully executed
   */
  private boolean wasSuccessful;
  /**
   * Version
   */
  private Version version;

  public Operation(Transaction trx, Msg.Statement stat, int opId) {
    this.statement = stat;
    this.trx = trx;
    this.readValue = null;
    this.wasSuccessful = true;
    this.opId = opId;
    this.version = null;
  }

  public long getClientId() {
    return trx.getClientId();
  }

  public Msg.Statement getStatement() {
    return statement;
  }

  public byte[] getReadValue() {
    return readValue;
  }

  public void setReadValue(byte[] value) {
    this.readValue = value;
  }

  public Version getVersion() {
    return version;
  }

  public void setVersion(Version version) {
    this.version = version;
  }

  public boolean wasSuccessful() {
    return wasSuccessful;
  }

  public void markError() {
    this.wasSuccessful = false;
  }

  public boolean isRead() {
    return statement.getOpType() == Msg.Statement.Type.READ;
  }

  public boolean isWrite() {
    return statement.getOpType() == Msg.Statement.Type.WRITE;
  }

  public boolean isReadForUpdate() {
    return statement.getOpType() == Msg.Statement.Type.READ_FOR_UPDATE;
  }

  public boolean isDummy() {
    return statement.getOpType() == Msg.Statement.Type.DUMMY;
  }

  public Transaction getTrx() {
    return trx;
  }

  public Long getKey() {
    return new Long(statement.getKey());
  }

  @Override
  public String toString() {
    String rt;
    rt = "Op:" + trx.getTimestamp() + "/" + opId + "/" + statement.getOpType()
        + " " + wasSuccessful;
    return rt;
  }

  public boolean equals(Object obj) {
    Operation otherOp;
    if (obj instanceof Operation) {
      otherOp = (Operation) obj;
      return otherOp.getTrx().equals(getTrx()) && otherOp.opId == opId
          && otherOp.getStatement().equals(getStatement());
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(Operation arg0) {
    int comp = arg0.getTrx().compareTo(getTrx());
    if (comp != 0) {
      return comp;
    } else {
      if (opId == arg0.opId) {
        return 0;
      } else if (opId < arg0.opId) {
        return -1;
      } else {
        return 1;
      }
    }

  }

  public byte[] getWriteValue() {
    ByteString val = statement.getValue();
    if (val != null) {
      return val.toByteArray();
    } else {
      return null;
    }
  }

  public boolean hasExecuted() {
    return version != null;
  }


  public int getOpId() {
    return opId;
  }


  public Type getType() {
    return statement.getOpType();
  }

  public boolean isDelete() {
    return statement.getOpType() == Type.DELETE;
  }
}
