package shield.proxy.data.sync;

import com.google.protobuf.ByteString;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import shield.BaseNode;
import shield.config.NodeConfiguration;
import shield.network.messages.Msg.DataMessageResp;
import shield.proxy.trx.data.Write;
import shield.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class around the MapDB back-end: has two options 1) in memory: data will not be persisted
 * if the system crashes 2) disk-based: data is backed by a memory-mapped file
 *
 * TODO(natacha): converting from ByteString to byte[] before accessing the key creates an
 * unnecessary copy
 *
 * @author ncrooks
 */
public class SyncMapDBBackingStore implements ISyncBackingStore {

  private final HTreeMap<Long, byte[]> kvStore;

  private final DB db;

  private final NodeConfiguration config;

  private int accessCount = 0;

  public SyncMapDBBackingStore(BaseNode node) {
    this(node.getConfig());
  }

  public SyncMapDBBackingStore(NodeConfiguration config) {
    this.config = config;
    if (config.DB_IN_MEMORY) {
      // TODO: check if memorySharedHashMap with large heap is better than
      // directDB map with smaller heap
      // Option 1
     db = DBMaker.memoryDirectDB()
          .concurrencyScale(config.N_WORKER_THREADS)
          .allocateStartSize(config.INITIAL_ALLOCATION_SIZE)
          .allocateIncrement(1 * 1024 * 1024 * 1024)
          .make();
      kvStore = db.hashMap(config.KV_NAME)
          .keySerializer(Serializer.LONG)
          .valueSerializer(Serializer.BYTE_ARRAY).create();

      // Option 2
     /*  kvStore =
        DBMaker.memoryShardedHashMap(config.N_WORKER_THREADS)
        .keySerializer(Serializer.LONG)
        .valueSerializer(Serializer.BYTE_ARRAY).create();
       db = null; */
    } else {
      System.out.println("Creating Map DB File Based System");
      System.out.println("File Name " + config.DB_FILE_NAME);
      db = DBMaker
          .fileDB(config.DB_FILE_NAME)
          .checksumHeaderBypass()
          .fileMmapEnableIfSupported()
          .concurrencyScale(config.N_WORKER_THREADS)
          .closeOnJvmShutdown()
          .make();
      kvStore =
          db.hashMap(config.KV_NAME)
              .keySerializer(Serializer.LONG)
              .valueSerializer(Serializer.BYTE_ARRAY).createOrOpen();
    }
  }

  public byte[] read(Long key) {
    long begin = 0;
    if (config.LOG_STORAGE_TIME) {
      begin = System.nanoTime();
    }
    byte[] res = kvStore.get(key);
    if (config.LOG_STORAGE_TIME) {
      long end = System.nanoTime();
      config.statsStorageTime.addPoint("READ "  + (end-begin));
    }

    if (config.LOG_STORAGE_SIZE && accessCount++ % 500 ==0) {
      config.statsStorageSize.addPoint(kvStore.size());
    }
    return res;
  }

  /**
   * Write back to database.
   */
  public void write(Write write) {
    long begin = 0;
    if (config.LOG_STORAGE_TIME) {
      begin = System.nanoTime();
       config.statsStorageTime.addBegin();
    }
    if (write.isDelete()) {
      kvStore.remove(write.getKey());
    if (config.LOG_STORAGE_TIME) {
      long end = System.nanoTime();
      config.statsStorageTime.addPoint("DELETE "  + (end-begin) + " 0 ");
    }
    } else {
      kvStore.put(write.getKey(), write.getValue());
    if (config.LOG_STORAGE_TIME) {
      long end = System.nanoTime();
      config.statsStorageTime.addPoint("WRITE "  + (end-begin) + " " + write.getValue().length);
    }
   }


    if (config.LOG_STORAGE_SIZE && accessCount++ % 500 ==0) {
      config.statsStorageSize.addPoint(kvStore.size());
    }

  }


  @Override
  public List<byte[]> read(List<Long> key) {
    List<byte[]> results = new ArrayList<byte[]>();
    for (Long k : key) {
      results.add(read(k));
    }

    /*
    ystem
    key.parallelStream().forEach(k -> vals.put(k, read(k)));
    for (Long k: key) {
      results.add(vals.get(key));
    } */

    return results;
  }

  @Override
  public void onHandleRequestResponse(DataMessageResp msgResp) {
    throw new RuntimeException("Unimplememnted");
  }

  @Override
  public void write(Queue<Write> writes) {
    // writes.parallelStream().forEach(w -> write(w));
    for (Write write : writes) {
      write(write);
    }
  }

  public void deleteDB() {
    db.close();
  }


}
