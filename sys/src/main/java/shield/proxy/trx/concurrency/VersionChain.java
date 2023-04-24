package shield.proxy.trx.concurrency;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import shield.proxy.trx.concurrency.Transaction.TxState;

/**
 * Chain of TSO versions
 *
 * To maximise concurrency, all versions are equipped with individual reader/writer locks. To
 * prevent deadlocks, these locks must always be acquired starting by the version with the highest
 * timestamp first
 *
 * @author ncrooks
 */
public class VersionChain {


  public static long kBaseTimestamp = 0;

  /**
   * Oldest version the chain. This is always a place-holder version with ts @link{kBaseTimestamp}.
   * Hence, value can never be null.
   */
  private Version oldestVersion;

  /**
   * Latest version in the chain
   */
  private Version latestVersion;

  /**
   * Ongoing dummy versions
   */
  private HashMap<Long, Version> currentDummyVersions;

  /**
   * Big fat lock protecting the version chain. TODO(natacha): remove and make use of the individual
   * version locks
   */
  private Lock lock;

  private int versionCount;


  public VersionChain() {
    oldestVersion = new Version(null);
    latestVersion = oldestVersion;
    lock = new ReentrantLock();
    versionCount = 0;
    currentDummyVersions = new HashMap<>();
  }

  /**
   * Creates a place-holder for a real write. This dummy write causes subsequent reads to block and
   * queue
   */
  public Version dummyWrite(long timestamp) {
    throw new RuntimeException("Unimplemented");
  }

  public long getVersionCount() {
    return versionCount;
  }

  public void write(Operation op) {

    long timestamp;
    Version versionToOverwrite;
    Transaction t = op.getTrx();
    Version dummyVersion;

    try {
      lock.lock();

      dummyVersion = currentDummyVersions.get(t.getTimestamp());
      if (dummyVersion != null) {
        // This write is for a dummy version. Update it to
        // real version
        // System.out.println("[ReadFor] This was a dummy version " + op.getKey() + " Updating " + op.getTrx().getTimestamp() + " Blocked ops " + dummyVersion.getBlockedOperations());
        currentDummyVersions.remove(t.getTimestamp());
        dummyVersion.markReal(op.isDelete());
        op.setVersion(dummyVersion);
        versionCount++;
      } else {
        // The new version that will be inserted in the version chain
        Version newVersion = new Version(op);
        op.setVersion(newVersion);

        versionCount++;
        timestamp = t.getTimestamp();
        // Find the latest version with a timestamp that is smaller
        // than this transaction's timestamp
        versionToOverwrite = findVersion(timestamp);
        // Check whether a writer with a higher timestamp than the writer
        // transaction has read this value. If yes, then this transaction
        // must be aborted
        if (versionToOverwrite.shouldWriteAbort(t)) {
          op.markError();
        } else {
          // If not, then insert new version in between the version to
          // overwrite and the next version
          newVersion.setPrev(versionToOverwrite);
          newVersion.setNext(versionToOverwrite.getNext());
          if (versionToOverwrite.getNext() != null) {
            versionToOverwrite.getNext().setPrev(newVersion);
          }
          versionToOverwrite.setNext(newVersion);

          if (latestVersion.equals(versionToOverwrite)) {
            latestVersion = newVersion;
          }
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Identifies appropriate version to read. A version always exist
   *
   * @return true if read a real value
   */
  public boolean read(Operation op, boolean isForUpdate) {
    long timestamp;
    Version versionToRead = null;
    Transaction versionCreatorTrx;
    boolean aborted = true;
    Version newVersionForUpdate;
    boolean isDummy = false;

    try {
      lock.lock();
      timestamp = op.getTrx().getTimestamp();
      // Identify what version this transaction should read
      while (aborted) {
        aborted = false;
        isDummy = false;

        versionToRead = findVersion(timestamp);

        if (!isForUpdate || !versionToRead.shouldWriteAbort(op.getTrx())) {
          op.setVersion(versionToRead);

          versionCreatorTrx = versionToRead.getTrx();
          if (versionCreatorTrx != null) {
            versionCreatorTrx.lock();
            op.getTrx().lock();
            // Add transaction to dependencies
            // Read may fail if the transaction that read from
            // has aborted in the meantime. Retry
            if (!versionCreatorTrx.equals(op.getTrx())) {
              aborted = addDependency(op.getTrx(), versionCreatorTrx);
            } else {
              // Transaction just read a version that it itself wrote
            }
            // Mark that this transaction read this version
            if (!aborted) {
              versionToRead.addReadTimestamp(op.getTrx());
              if (versionToRead.isDummy()) {
                isDummy = true;
                versionToRead.addBlockedOperation(op);
              }
            }
            versionCreatorTrx.unlock();
            op.getTrx().unlock();
          } else {
            // else: this was initinial version
            StringBuilder b = new StringBuilder();
           b.append("Initial Version " + op.getKey() + " " + timestamp);
            Version v = latestVersion;
            while (v!=null) {
              b.append(" " + v.getVersionId());
              v = v.getPrevious();
            }
            System.err.println(b.toString());
          }
        } else {
            // only do the read if the write is not about to
            // fail, as otherwise can get two undo notifications
            aborted = false;
        }

      }

      // Now create the dummy write version
      // The version to read is necessarily the version that will be overwritten
      if (isForUpdate) {
        aborted = versionToRead.shouldWriteAbort(op.getTrx());
        if (aborted) {
          op.markError();
        } else {
          // If not, then insert new version in between the version to
          // overwrite and the next version
          newVersionForUpdate = new Version(op);
          //op.setVersion(newVersionForUpdate);
          newVersionForUpdate.setPrev(versionToRead);
          newVersionForUpdate.setNext(versionToRead.getNext());
          if (versionToRead.getNext() != null) {
            versionToRead.getNext().setPrev(newVersionForUpdate);
          }
          versionToRead.setNext(newVersionForUpdate);

          if (latestVersion.equals(versionToRead)) {
            latestVersion = newVersionForUpdate;
          }

          currentDummyVersions.put(op.getTrx().getTimestamp(), newVersionForUpdate);
        }
      }

    } finally {
      lock.unlock();
    }
    return isDummy;
  }


  /**
   * Adds a wr dependency from writeTrx to readTrx
   *
   * @param readTrx - Transaction that read the version
   * @param writeTrx - Transaction that wrote the version
   */
  public boolean addDependency(Transaction readTrx, Transaction writeTrx) {
    try {
      // writeTrx.lock();
      // readTrx.lock();
      assert (writeTrx.getTimestamp() <= readTrx.getTimestamp());
      if (writeTrx.isAborting()) {
        // Transaction aborted in the meantime
        // try again to minimise the risk of cascading aborts
        return true;
      } else {
        if (writeTrx.getTrxState() != TxState.COMMITTED) {
          writeTrx.addDependantTransaction(readTrx);
          readTrx.addDependingTransaction(writeTrx);
        } else {
          // Must be in committed state, at which point
          // no need to add it as a dependency
        }
        return false;
      }
    } finally {
      // writeTrx.unlock();
      // readTrx.unlock();
    }
  }

  /**
   * This function returns the latest version that has a timestamp smaller or equal to the specified
   * timestamp
   */
  private Version findVersion(long timestamp) {

    Version currentVersion;
    long currentTimestamp;

    assert (latestVersion != null);

    currentVersion = latestVersion;
    while (true) {
      // Previous version should never be null
      // as always initialise a version chain with a place
      // older
      assert (currentVersion != null);
      currentTimestamp = currentVersion.getVersionId();
      if (currentVersion.isZombie()) System.out.println("Trx " + timestamp + " Found Zombie " + currentTimestamp);
      if (currentTimestamp <= timestamp && !currentVersion.isZombie()) {
        return currentVersion;
      }
      currentVersion = currentVersion.getPrevious();
    }
  }

  public void delete(Operation op) {
    // Delete is exactly the same as write, will
    // create a tombstone operation
    write(op);
  }


  /**
   * Undo operation when a transaction aborts.
   *
   * If the operation is a write, also mark transactions as "WILL_ABORT". Marking a transaction as
   * WILL_ABORT ensures that the ongoing (or the next) operation will be marked as failed, causing
   * the transaction to
   */
  public SortedSet<Operation> undoOperation(Operation op) {
    SortedSet<Operation> blockedOps = new TreeSet<Operation>();
    try {
      lock.lock();
      Version version = op.getVersion();
      assert (version != null);
      if (op.isRead()) {
        // If it is a read operation, simply remove
        // its id from the read timestamps.
        // NB: we do not remove T from the list of
        // transactions that depend on this version's
        // creating transaction. It is not necessary but
        // may cause notifications to aborted transactions
      } else if (op.isReadForUpdate()) {
        version = currentDummyVersions.remove(op.getTrx().getTimestamp());
        if (version != null) {
          removeVersion(version, true);
          blockedOps.addAll(version.getBlockedOperations());
        }

      } else {
        // Mark all depending transactions as aborted.
        // Remove version from chain
        removeVersion(version, true);
      }
    } finally {
      lock.unlock();
    }
    return blockedOps;
  }

  private void removeVersion(Version version, boolean isAbort) {

    // Remove version from chain

    Version prevVersion = version.getPrevious();
    Version nextVersion = version.getNext();

    if (prevVersion != null) {
      prevVersion.setNext(nextVersion);
    }
    if (nextVersion != null) {
      nextVersion.setPrev(prevVersion);
    }

    if (latestVersion.equals(version)) {
      latestVersion = prevVersion;
    }

    // If we are removing this value because an abort, mark dependent
    // operations as aborted
   /* if (isAbort) {
      // Mark all transactions that read that value as aborted
      for (Transaction t : version.getReadTransactions()) {
        if (t.getTrxState() != TxState.ABORTED) {
          t.markRollbacked();
        }
      }
    } */

  }

  public Version getLast() {
    try {
      lock.lock();
      return latestVersion;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Garbage collect versions that will no longer be accessed: all versions that are < than the
   * timestamp, and not the oldest version in the system
   *
   * @return list of timestamps that were removed
   */
  public int truncateChain(Long key, long timestamp, TSOMetadata metadata) {
    // Get the first real version of the chain
    int removedTimestamps = 0;
    Version currentVersion = oldestVersion.getNext();
    if (currentVersion == null) {
      return removedTimestamps; // the chain is empty
    } else {
      // Now begin pruning
      // remove any version that has a lower timestamp and is not
      // the head of the chain
       currentVersion = currentVersion.getNext();
      long ts = currentVersion == null ? 0 : currentVersion.getVersionId();
      while (currentVersion != null && currentVersion.getVersionId() < timestamp) {
        assert (currentVersion.getVersionId() > 0);
        removedTimestamps++;
        System.out.println("GC Removing " + key + " " + currentVersion.getVersionId());
        metadata.removeDataFromCache(key, currentVersion.getVersionId());
        removeVersion(currentVersion, false);
        currentVersion = currentVersion.getNext();
      }
      // metadata.removeDataFromCache(key, 0);
      return removedTimestamps;
    }
  }


}
