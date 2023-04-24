package shield.proxy.trx.concurrency;

import java.util.SortedSet;
import java.util.TreeSet;
import shield.network.messages.Msg.Statement.Type;

/**
 * Instance of a TSO version object. All new write creates a TSO version. Each version contains a
 * unique id (the timestamp of the transaction that created it), and a set of read markers. This
 * denotes the set of transactions that read this version. This is used to determine whether writer
 * transactions should abort.
 *
 * Versions also contain pointers to previous and past version.
 *
 * Versions can be real or dummy. Dummy versions also have a set of transactions that are currently
 * blocking on the dummy to be created.
 *
 * @author ncrooks
 */
public class Version {



  enum VersionType {
    // Regular version
      NORMAL,
    // Dummy version. Write (or delete) is pending
      DUMMY,
    // Version represents a delete
      TOMBSTONE
  }
  /**
   * ID of the transaction who created that version
   */
  private final long versionId;

  /**
   * Transaction responsible for creating this version
   */
  private final Operation op;

  /**
   * Previous version in the chain
   */
  private Version prevVersion = null;

  /**
   * Next version in the chain
   */
  private Version nextVersion = null;

  /**
   * Type of the version: normal, dummy (operation pending),
   * or tombstone
   */
  private VersionType versionType;


  /**
   * List of transactions that are currently waiting on this version to be un-dummmied
   */
  private final SortedSet<Operation> blockedOperations;

  /**
   * Transactions that have read this version
   */
  // private final SortedSet<Transaction> readTransactions;
  private long readTransaction;


  public Version(Operation op) {
    this.versionId = op != null ? op.getTrx().getTimestamp() : 0;
    this.op = op;
    if (op == null) this.versionType = VersionType.NORMAL;
    else {
      if (op.getType() == Type.DUMMY
          || op.getType() == Type.READ_FOR_UPDATE) {
        this.versionType = VersionType.DUMMY;
      } else if (op.getType() == Type.DELETE) {
        this.versionType = VersionType.TOMBSTONE;
      } else
        this.versionType = VersionType.NORMAL;
    }
    this.blockedOperations = new TreeSet<Operation>();
    this.readTransaction = 0;
  }


  /**
   * Returns true if the transaction that created this version has aborted or will abort.
   */
  public boolean isZombie() {
    if (op != null) {
      return getTrx().isAborting();
    } else {
      return false;
    }
  }

  public void addBlockedOperation(Operation op) {
    System.out.println("Op blocked on " + op.getKey() + " TrxId " + op.getTrx().getTimestamp() + "  by Tx " + this.getVersionId() + " " + this.getBlockedOperations().size());
    blockedOperations.add(op);
  }

  /**
   * Marks a dummy version as a real
   */
  public void markReal(boolean delete) {
    assert(versionType == VersionType.DUMMY);
    versionType = delete? VersionType.TOMBSTONE:
        VersionType.NORMAL;
  }

  /**
   * Returns previous version in the chain
   */
  public Version getPrevious() {
    return prevVersion;
  }

  /**
   * Returns next version in the chain
   */
  public Version getNext() {
    return nextVersion;
  }

  public void setNext(Version version) {
    nextVersion = version;
  }

  public void setPrev(Version version) {
    prevVersion = version;
  }

  /**
   * Returns version id of the transaction that wrote this write
   */
  public long getVersionId() {
    return versionId;
  }

  /**
   * Returns whether version is currently a dummy write
   */
  public boolean isDummy() {
    return versionType==VersionType.DUMMY;
  }

  public boolean isTombstone() {
    return versionType == VersionType.DUMMY.TOMBSTONE;
  }

  /**
   * Adds a read marker to this version for this particular timestamp
   */
  public void addReadTimestamp(Transaction t) {
    // System.out.println("Read Timestamp Size " + readTransactions.size());
    assert (t.getTimestamp() >= versionId);
    // readTransactions.add(t);
    readTransaction = readTransaction < t.getTimestamp()? t.getTimestamp(): readTransaction;
  }

  /**
   * Determines whether a write operation should abort. A write operation should abort when trying
   * to read this version if it has a timestamp that is smaller than any transaction in the @link
   * {readTransactions} set. Otherwise, this would create an anti-dependency (rw), and potentially a
   * cycle.
   */
  public boolean shouldWriteAbort(Transaction t) {
    boolean shouldAbort = false;
    shouldAbort = t.getTimestamp() < readTransaction;
    if (shouldAbort) System.out.println("Abort. Write is " + t.getTimestamp() + " but Read is " + readTransaction);
    return shouldAbort;
  }

  public SortedSet<Operation> getBlockedOperations() {
    return blockedOperations;
  }

  public Transaction getTrx() {
    if (op == null) {
      return null;
    } else {
      return op.getTrx();
    }
  }

  public Operation getOperation() {
    return op;
  }


  public VersionType getType() {
    return versionType;
  }

}
