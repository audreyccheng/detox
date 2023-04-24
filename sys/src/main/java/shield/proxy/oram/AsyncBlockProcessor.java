package shield.proxy.oram;

import java.util.*;

import shield.network.messages.Msg;
import shield.proxy.data.async.AsyncDataRequest;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.oram.recover.LogEntry;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncBlockProcessor implements IBlockProcessor {

  private final IAsyncBackingStore store;

  private final Random rng;

  private final ForkJoinPool pool;

  private final Map<Long, BlockProcessingTask> physicalTasks;

  private final Map<Long, BlockProcessingTask> realTasks;

  private final List<IBlockReadListener> blockReadListeners;

  private AtomicLong taskIds;

  private AsyncRingOram ringOram;

  private final Map<LogEntry, LogEntryTask> logEntryTasks;

  private final Map<Long, Lock> realLocks;


  public AsyncBlockProcessor(IAsyncBackingStore store, Random rng, ForkJoinPool pool) {
    this.store = store;
    this.rng = rng;
    this.pool = pool;
    this.physicalTasks = new ConcurrentHashMap<>();
    this.realTasks = new ConcurrentHashMap<>();
    this.logEntryTasks = new ConcurrentHashMap<>();
    this.blockReadListeners = new ArrayList<>();
    this.ringOram = null;
    this.taskIds = new AtomicLong();
    this.realLocks = new ConcurrentHashMap<>();
  }

  void setRingOram(AsyncRingOram oram) {
    ringOram = oram;
  }

  ForkJoinPool getPool() {
    return pool;
  }

  @Override
  public void readBlock(Long physicalKey, Long physicalRecoveryKey, Block b, LogEntry recoveryDep) {
    BlockProcessingTask task = new BlockProcessingTask(taskIds.incrementAndGet(), physicalKey, physicalRecoveryKey, b, true);
    ringOram.addTask();

    LogEntryTask predLogEntry = null;
    if (recoveryDep != null) {
      predLogEntry = logEntryTasks.get(recoveryDep);
    }
    if (predLogEntry != null) {
      predLogEntry.lock.lock();
    }
    BlockProcessingTask predPhysical = physicalTasks.get(physicalKey);
    if (predPhysical != null) {
      predPhysical.lock.lock();
    }
    task.lock.lock();

    if (predLogEntry != null) {
      if (logEntryTasks.get(recoveryDep) != null) {
        predLogEntry.succ.add(task);
        task.incrementPred();
      }
    }

    if (predPhysical != null) {
      assert !predPhysical.isRead;
      if (physicalTasks.get(physicalKey) != null) {
        if (task.id <= predPhysical.id) {
          System.err.printf("%d %d\n", task.id, predPhysical.id);
        }
        assert task.id > predPhysical.id;
        predPhysical.succ.add(task);
        task.incrementPred();
      }
    }
    boolean canExecute = task.pred == 0;
    physicalTasks.put(physicalKey, task);
    if (!b.isDummy) {
      Lock l = realLocks.get(b.getKey());
      if (l == null) {
        l = new ReentrantLock();
        realLocks.put(b.getKey(), l);
      }
      if (!b.isStale()){
        l.lock();
        realTasks.put(b.getKey(), task);
        l.unlock();
      }
    }
    if (canExecute) {
      pool.execute(task);
    }

    task.lock.unlock();
    if (predPhysical != null) {
      predPhysical.lock.unlock();
    }
    if (predLogEntry != null) {
      predLogEntry.lock.unlock();
    }
  }

  @Override
  public void readBlockInBatch(List<Long> key, List<Block> b) {
    throw new RuntimeException("Unimplemented in Async Mode");
  }

  @Override
  public void writeBlock(Long physicalKey, Long physicalRecoveryKey, Block b) {
    BlockProcessingTask task = new BlockProcessingTask(taskIds.incrementAndGet(), physicalKey, physicalRecoveryKey, b, false);
    ringOram.addTask();

    BlockProcessingTask predPhysical = physicalTasks.get(physicalKey);
    BlockProcessingTask predReal = null;
    if (!b.isDummy) {
      predReal = realTasks.get(b.getKey());
    }
    if (predPhysical != null) {
      predPhysical.lock.lock();
    }
    if (predReal != null) {
      predReal.lock.lock();
    }
    task.lock.lock();

    if (predPhysical != null) {
      if (physicalTasks.get(physicalKey) != null) {
        assert task.id > predPhysical.id;
        assert predPhysical.isRead || predPhysical.b.isDummy;
        predPhysical.succ.add(task);
        task.incrementPred();
      }
    }
    if (predReal != null && predReal != predPhysical) {
      if (realTasks.get(b.getKey()) != null) {
        assert task.id > predReal.id;
        assert predReal.isRead;
        predReal.succ.add(task);
        task.incrementPred();
      }
    }
    if (predReal == null) {
      assert b.getValue() != null;
    }
    boolean canExecute = task.pred == 0;
    physicalTasks.put(physicalKey, task);
    if (canExecute) {
      pool.execute(task);
    }
    task.lock.unlock();

    if (predReal != null) {
      predReal.lock.unlock();
    }
    if (predPhysical != null) {
      predPhysical.lock.unlock();
    }

    long end = System.nanoTime();

  }

  public void writeLogEntry(LogEntry logEntry, Long key, LogEntry prevLogEntry) {
    LogEntryTask task = new LogEntryTask(logEntry, key);

    LogEntryTask predLogEntry = null;
    if (prevLogEntry != null) {
      predLogEntry = logEntryTasks.get(prevLogEntry);
    }

    if (predLogEntry != null) {
      predLogEntry.lock.lock();
    }
    task.lock.lock();

    if (predLogEntry != null) {
      if (logEntryTasks.get(prevLogEntry) != null) {
        predLogEntry.succ.add(task);
        task.incrementPred();
      }
    }

    boolean canExecute = task.pred == 0;
    logEntryTasks.put(logEntry, task);
    if (canExecute) {
      pool.execute(task);
    }
    pool.execute(task);

    task.lock.unlock();
    if (predLogEntry != null) {
      predLogEntry.lock.unlock();
    }
  }

  @Override
  public Write writeBlockInBatch(Long key, Block b) {
    throw new RuntimeException("Unimplemented in async mode");
  }

  @Override
  public void flushWrites(Queue<Write> writes) {
    throw new RuntimeException("Unimplemented in async mode");
  }

  @Override
  public void handleRequestResponse(Msg.DataMessageResp msgResp) {
    store.onHandleRequestResponse(msgResp);
  }

  public void addBlockReadListener(IBlockReadListener blockReadListener) {
    blockReadListeners.add(blockReadListener);
  }

  private void notifyBlockReadListeners(Block b) {
    for (int i = 0; i < blockReadListeners.size(); ++i) {
      blockReadListeners.get(i).onBlockRead(b);
    }
  }

  private abstract class DepTask extends RecursiveAction {
    final ReentrantLock lock;
    final List<DepTask> succ;
    int pred;

    public DepTask() {
      this.succ = new ArrayList<>();
      this.lock = new ReentrantLock();
      this.pred = 0;
    }

    void incrementPred() {
      pred++;
    }

  }
  private class LogEntryTask extends DepTask {

    private final LogEntry entry;
    private final Long key;


    LogEntryTask(LogEntry entry, Long key) {
      this.entry = entry;
      this.key = key;

    }

    @Override
    protected void compute() {
      try {
        byte[] data = entry.serialize();
        ringOram.recoveryEncrypter.get().mask(data, key);
        store.write(new Write(key, data, Type.WRITE), new AsyncDataRequest() {
          @Override
          public void onDataRequestCompleted() {
            onFinished();
          }
        });
      } catch (Error e) {
        System.err.println("###" + toString());
        e.printStackTrace(System.err);
        System.exit(1);
      } catch (Exception e) {
        System.err.println("###" + toString());
        e.printStackTrace(System.err);
        System.exit(1);
      }
    }

    private void onFinished() {
      lock.lock();
      for (int i = 0; i < succ.size(); ++i) {
        DepTask succTask = succ.get(i);
        if (succTask != null) {
          succTask.lock.lock();
          succTask.pred--;
          boolean canExecute = succTask.pred == 0;
          succTask.lock.unlock();
          if (canExecute) {
            succTask.fork();
          }
        }
      }
      succ.clear();
      logEntryTasks.remove(entry);
      lock.unlock();
    }

  }

  private class BlockProcessingTask extends DepTask {


    final Long physicalKey;
    final Long key;
    final Block b;
    final boolean isRead;
    final long id;

    boolean stale;
    private String cachedString;

    BlockProcessingTask(long id, Long physicalKey, Long key, Block b, boolean isRead) {
      this.id = id;
      this.physicalKey = physicalKey;
      this.key = key;
      this.b = b;
      this.isRead = isRead;
      this.stale = b.isStale();
    }

    @Override
    protected void compute() {

      final BlockProcessingTask t = this;
      try {
        if (isRead) {
          AsyncDataRequest req = new AsyncDataRequest() {
            @Override
            public void onDataRequestCompleted() {
              if (!b.isDummy) {
                byte[] value = getValues().get(0);
                assert value != null && value.length != 0;
                if (value.length != ringOram.getValueSize()) {
                  System.err.println(value.length + " " + ringOram.getValueSize());
                  assert(false);
                }
                synchronized (b) {
                  b.decryptAndSetValue(value.clone());
                }
                notifyBlockReadListeners(b);
              }
              onFinished();
            }
          };
          store.read(key, req);
        } else {
          AsyncDataRequest req = new AsyncDataRequest() {
            @Override
            public void onDataRequestCompleted() {
              onFinished();
            }
          };
          byte[] value = b.encryptAndClearValue(rng);
          assert value.length == ringOram.getValueSize();
          store.write(new Write(key, value.clone(), Type.WRITE), req);
        }
      } catch (Error e) {
        System.err.println("###" + toString());
        e.printStackTrace(System.err);
        System.exit(1);
      } catch (Exception e) {
        System.err.println("###" + toString());
        e.printStackTrace(System.err);
        System.exit(1);
      }
    }

    private void onFinished() {
      lock.lock();
      for (int i = 0; i < succ.size(); ++i) {
        DepTask succTask = succ.get(i);
        if (succTask != null) {
          succTask.lock.lock();
          succTask.pred--;
          boolean canExecute = succTask.pred == 0;
          succTask.lock.unlock();
          if (canExecute) {
            succTask.fork();
          }
        }
      }
      succ.clear();
      BlockProcessingTask mostRecentPhysicalTask = physicalTasks.get(physicalKey);
      if (mostRecentPhysicalTask == this) {
        physicalTasks.remove(physicalKey);
      }
      if (!b.isDummy && isRead) {
        Lock l = realLocks.get(b.getKey());
        l.lock();
        BlockProcessingTask mostRecentRealTask = realTasks.get(b.getKey());
        if (mostRecentRealTask == this) {
          realTasks.remove(b.getKey());
        }
        l.unlock();
      }
      lock.unlock();

      ringOram.finishedTask();
    }

    @Override
    public String toString() {
      return key.toString();
    }
  }

}
