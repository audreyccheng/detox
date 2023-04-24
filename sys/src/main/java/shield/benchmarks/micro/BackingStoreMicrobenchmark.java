package shield.benchmarks.micro;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import shield.benchmarks.ycsb.utils.*;
import shield.proxy.data.sync.*;
import shield.proxy.data.async.*;
import shield.proxy.oram.*;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.proxy.trx.data.Write;
import shield.proxy.trx.data.Write.Type;
import shield.util.Pair;
import shield.util.Utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BackingStoreMicrobenchmark {

  private final String configFileName;

  private final BackingStoreMicrobenchmarkConfiguration config;

  private ISyncBackingStore backingStore;

  private BaseRingOram oram;

  private List<Long> keys;

  private List<byte[]> values;

  private IntGenerator keySelector;

  private JSONObject output;

  private Random rand;

  private long opsExecuted;

  private List<byte[]> defaultValues;

  public BackingStoreMicrobenchmark(String configFileName,
      BackingStoreMicrobenchmarkConfiguration config) {
    this.configFileName = configFileName;
    this.config = config;
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.printf("Incorrect number of args %d != 1\n", args.length);
      System.err.printf("Usage: java %s <config_file_path>\n",
          BackingStoreMicrobenchmark.class.getName());
      System.err.println(
          "    config_file_path: path to backing store experiment config file");
      System.exit(1);
    }
    BackingStoreMicrobenchmarkConfiguration config =
        new BackingStoreMicrobenchmarkConfiguration();
    try {
      config.loadProperties(args[0]);
    } catch (IOException e) {
      System.err.printf("Failed to read config file %s\n", args[0]);
      e.printStackTrace(System.err);
      System.exit(1);
    } catch (ParseException e) {
      System.err.printf("Failed to parse configuration from config file %s\n",
          args[0]);
      e.printStackTrace(System.err);
      System.exit(1);
    }

    BackingStoreMicrobenchmark benchmark =
        new BackingStoreMicrobenchmark(args[0], config);
    benchmark.run();
  }

  public void run() {
    setup();
    long start = System.currentTimeMillis();
    execute();
    long end = System.currentTimeMillis();
    output.put("number_of_operations", opsExecuted);
    output.put("total_elapsed_millis", end - start);
    export();
    // hack to clean up backing stores
    if (oram instanceof AsyncRingOram) {
      ((AsyncRingOram) oram).shutdown();
    }
  }

  private void setup() {
    throw new RuntimeException("Temporarily not implemented.");
    /*System.out.println("Setting up microbenchmark.");
    // this is currently a hack - I need to figure out why Soumya made this a
    // static field
    ORAMConfig.clientKey = new byte[8];
    long seed = config.RANDOM_SEED;
    for (int i = 0; i < 8; ++i) {
      ORAMConfig.clientKey[7 - i] = (byte) (seed & 0x000000FF);
      seed >>= 8;
    }
    Utils.seed(config.RANDOM_SEED);
    rand = new Random(config.RANDOM_SEED);

    System.out.println("Initializing backing store.");
    switch (config.BACKING_STORE_TYPE) {
      case HASH_MAP:
        backingStore = new SyncMapBackingStore(new HashMap<>(config.TOTAL_KEYS * 2, 0.9f));
        break;
      case SEQUENTIAL_RING_ORAM:
        ISyncBackingStore oramBackingStore = new LatencyMapBackingStore(new ConcurrentHashMap<>(4 * config.BACKING_STORE_RING_ORAM_N
            / config.BACKING_STORE_RING_ORAM_A * (config.BACKING_STORE_RING_ORAM_Z + config.BACKING_STORE_RING_ORAM_S),
            0.9f), config.STORE_LATENCY);
        if (config.BACKING_STORE_RING_ORAM_L == -1) {
          oram = SyncRingOram.create(config.BACKING_STORE_RING_ORAM_N, config.BACKING_STORE_RING_ORAM_Z,
              config.BACKING_STORE_RING_ORAM_S, config.BACKING_STORE_RING_ORAM_A, rand, oramBackingStore,
              config.BACKING_STORE_RING_ORAM_ENCRYPT_BLOCKS, config.VALUE_SIZE, MaskAlgorithmType.BC_HMAC, new byte[1], false, 1, false, 0, false, 0, 0, false, false);
        } else {
          oram = SyncRingOram.create(
              config.BACKING_STORE_RING_ORAM_L,
              rand,
              config.VALUE_SIZE, false);
        }
        backingStore = (SyncRingOram) oram;
        break;
      case PARALLEL_RING_ORAM:
        IAsyncBackingStore asyncOramBackingStore = new AsyncSyncBackingStore(new LatencyMapBackingStore(new ConcurrentHashMap<>(
          4 * config.BACKING_STORE_RING_ORAM_N
              / config.BACKING_STORE_RING_ORAM_A * (config.BACKING_STORE_RING_ORAM_Z + config.BACKING_STORE_RING_ORAM_S),
          0.9f), config.STORE_LATENCY));
        if (config.BACKING_STORE_RING_ORAM_L == -1) {
          oram = AsyncRingOram.create(config.BACKING_STORE_RING_ORAM_N, config.BACKING_STORE_RING_ORAM_Z,
              config.BACKING_STORE_RING_ORAM_S, config.BACKING_STORE_RING_ORAM_A, rand, asyncOramBackingStore,
              config.BACKING_STORE_RING_ORAM_ENCRYPT_BLOCKS, config.BACKING_STORE_PARALLEL_RING_ORAM_THREADS,
              config.VALUE_SIZE, MaskAlgorithmType.BC_HMAC, new byte[1], false, 1, false, 0, false, 0, 0, false);
        } else {
          oram = AsyncRingOram.create(
              config.BACKING_STORE_RING_ORAM_L,
              rand, config.BACKING_STORE_RING_ORAM_ENCRYPT_BLOCKS,
              config.VALUE_SIZE, false);
        }
        backingStore = new SyncAsyncBackingStore((AsyncRingOram) oram);
        break;
    }

    switch (config.WORKLOAD_TYPE) {
      case UNIFORM:
        keySelector = new UniformIntGenerator(0, config.TOTAL_KEYS - 1);
        break;
      case ZIPFIAN:
        keySelector = new ZipfianIntGenerator(config.TOTAL_KEYS,
            config.WORKLOAD_ZIPFIAN_CONSTANT);
        break;
    }

    if (config.BACKING_STORE_TYPE
        == BackingStoreMicrobenchmarkConfiguration.BackingStoreType.SEQUENTIAL_RING_ORAM ||
        config.BACKING_STORE_TYPE
            == BackingStoreMicrobenchmarkConfiguration.BackingStoreType.PARALLEL_RING_ORAM) {
      if (config.BACKING_STORE_RING_ORAM_READ_PATH_ALL_REAL) {
        oram.enableReadPathAllReal();
      }
      oram.enableRecordStashSize();
    }
    output = new JSONObject();

    System.out.println("Generating keys.");
    keys = new ArrayList<>();
    byte[] initialValue = new byte[config.VALUE_SIZE];
    Queue<Write> initialWrites = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < config.TOTAL_KEYS; ++i) {
      byte[] keyBytes = new byte[config.KEY_SIZE];
      rand.nextBytes(keyBytes);
      Long key = Utility.hashPersistent(new String(keyBytes));
      keys.add(key);
      initialWrites.add(new Write(key, initialValue, Type.WRITE));
    }
    backingStore.write(initialWrites);

    if (config.MICROBENCHMARK_TYPE
        == BackingStoreMicrobenchmarkConfiguration.MicrobenchmarkType.FIXED_OPERATION_SET) {
      System.out.println("Generating values.");
      values = generateValues(config.EXPERIMENT_LENGTH);
    } else {
      defaultValues = generateValues(config.BACKING_STORE_BATCH_SIZE);
    }*/
  }

  private List<byte[]> generateValues(int n) {
    List<byte[]> values = new ArrayList<>(n);
    for (int i = 0; i < n; ++i) {
      byte[] value = new byte[config.VALUE_SIZE];
      if (!config.WRITE_DEFAULT_VALUE) {
        rand.nextBytes(value);
      }
      values.add(value);
    }
    return values;
  }

  private void execute() {
    System.out.println("Starting to execute benchmark.");
    switch (config.MICROBENCHMARK_TYPE) {
      case FIXED_OPERATION_SET:
        executeFixedOperationSet();
        break;
      case TIMED:
        executeTimed();
        break;
    }
    System.out.println("Finished executing benchmark.");
  }

  private void executeFixedOperationSet() {
    int batches = (config.EXPERIMENT_LENGTH
        + (config.EXPERIMENT_LENGTH % config.BACKING_STORE_BATCH_SIZE))
        / config.BACKING_STORE_BATCH_SIZE;
    for (int i = 0; i < batches; ++i) {
      List<byte[]> batchValues = this.values.subList(
          i * config.BACKING_STORE_BATCH_SIZE,
          Math.min(values.size(), (i + 1) * config.BACKING_STORE_BATCH_SIZE));
      executeBatch(batchValues, true);
    }
  }

  private void executeTimed() {
    long start = System.currentTimeMillis();
    long now;
    long startRecord = 0;
    boolean recordOps = false;
    JSONObject stashSizeRecordings = new JSONObject();
    JSONArray stashSizeRecordingsAccesses = new JSONArray();
    JSONArray stashSizeRecordingsSizes = new JSONArray();
    stashSizeRecordings.put("accesses", stashSizeRecordingsAccesses);
    stashSizeRecordings.put("sizes", stashSizeRecordingsSizes);
    for (now = System.currentTimeMillis(); (now - start) / 1000 < config.EXPERIMENT_LENGTH;
        now = System.currentTimeMillis()) {
      if (!recordOps && (now - start) / 1000 > config.TIMED_RAMP_UP_TIME) {
        recordOps = true;
        startRecord = System.currentTimeMillis();
      }
      List<byte[]> batchValues = config.WRITE_DEFAULT_VALUE ? defaultValues
          : generateValues(config.BACKING_STORE_BATCH_SIZE);
      executeBatch(batchValues, recordOps);
      System.out
          .printf("\r[%d:%02d][%.2f ops/s]", (now - start) / 1000 / 60, ((now - start) / 1000) % 60,
              1000.0 * opsExecuted / (now - start));
      if (oram != null) {
        Pair<Long, Long> stashSizeRecording = null;
        while ((stashSizeRecording = oram.getStashSizeRecording()) != null) {
          stashSizeRecordingsAccesses.add(stashSizeRecording.getLeft());
          stashSizeRecordingsSizes.add(stashSizeRecording.getRight());
        }
      }
    }
    System.out.println();
    if (config.BACKING_STORE_TYPE == BackingStoreMicrobenchmarkConfiguration.BackingStoreType.SEQUENTIAL_RING_ORAM ||
        config.BACKING_STORE_TYPE == BackingStoreMicrobenchmarkConfiguration.BackingStoreType.PARALLEL_RING_ORAM) {
      output.put("stash_sizes", stashSizeRecordings);
    }
    if (config.BACKING_STORE_RING_ORAM_READ_PATH_ALL_REAL) {
      output.put("num_buckets_skipped_on_read_path", oram.getTotalBucketsSkipped());
    }
    output.put("recorded_elapsed_millis", now - startRecord);
  }

  private void executeBatch(List<byte[]> values, boolean recordOps) {
    int n = values.size();
    boolean readBatch = rand.nextDouble() < config.READ_RATIO;
    if (readBatch) {
      List<Long> batchKeys = new ArrayList<>();
      for (int j = 0; j < config.BACKING_STORE_BATCH_SIZE && j < n; ++j) {
        batchKeys.add(keys.get(keySelector.nextValue()));
      }
      backingStore.read(batchKeys);
    } else {
      Queue<Write> batchWrites =
          new ConcurrentLinkedQueue<Write>();
      for (int j = 0; j < config.BACKING_STORE_BATCH_SIZE && j < n; ++j) {
        batchWrites.add(new Write(keys.get(keySelector.nextValue()),
            values.get(j), Type.WRITE));
      }
      backingStore.write(batchWrites);
    }
    if (recordOps) {
      opsExecuted += values.size();
    }
  }

  private void export() {
    System.out.println("Exporting stats collected during benchmark.");

    File configFile = new File(configFileName);
    String outputFileName = configFile.getName();
    if (outputFileName.contains(".")) {
      outputFileName =
          outputFileName.substring(0, outputFileName.lastIndexOf('.'))
              + ".out.json";
      outputFileName =
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-"))
              + outputFileName;
    }
    try (FileWriter writer = new FileWriter(config.OUTPUT_DIR + File.separator + outputFileName)) {
      config.addToJson(output);
      output.writeJSONString(writer);
      writer.flush();
    } catch (IOException e) {
      System.err.printf("Unable to open output file %s\n", outputFileName);
      e.printStackTrace(System.err);
    }
  }
}
