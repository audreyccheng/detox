package shield.benchmarks.micro;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shield.config.Configuration;

import java.io.FileReader;
import java.io.IOException;

public class BackingStoreMicrobenchmarkConfiguration extends Configuration {


  public int KEY_SIZE = 16;
  public int VALUE_SIZE = 1024;
  public int TOTAL_KEYS = 100;
  public MicrobenchmarkType MICROBENCHMARK_TYPE =
      MicrobenchmarkType.FIXED_OPERATION_SET;
  public WorkloadType WORKLOAD_TYPE = WorkloadType.UNIFORM;
  public double WORKLOAD_ZIPFIAN_CONSTANT = 0.99;
  public BackingStoreType BACKING_STORE_TYPE = BackingStoreType.HASH_MAP;
  public int BACKING_STORE_RING_ORAM_N = 65536;
  public int BACKING_STORE_RING_ORAM_Z = 4;
  public int BACKING_STORE_RING_ORAM_S = 3;
  public int BACKING_STORE_RING_ORAM_A = 3;
  public int BACKING_STORE_PARALLEL_RING_ORAM_THREADS = 2;
  public int BACKING_STORE_BATCH_SIZE = 1;
  public double READ_RATIO = 0.5;
  public long RANDOM_SEED = 0;
  public int STORE_LATENCY = 0;
  public int TIMED_RAMP_UP_TIME = 60;
  public String OUTPUT_DIR = "";
  public int BACKING_STORE_RING_ORAM_L = -1;

  /**
   * Length of experiment in, e.g., operations (seconds, etc.) depending on MicrobenchmarkType
   */
  public int EXPERIMENT_LENGTH = 100000;

  public boolean BACKING_STORE_RING_ORAM_ENCRYPT_BLOCKS = true;

  public boolean WRITE_DEFAULT_VALUE = false;

  public boolean BACKING_STORE_RING_ORAM_READ_PATH_ALL_REAL = false;

  @Override
  public void loadProperties(String fileName)
      throws IOException, ParseException {
    FileReader reader = new FileReader(fileName);
    if (fileName == "") {
      System.err.println("Empty property file name.");
    }
    JSONParser jsonParser = new JSONParser();
    JSONObject prop = (JSONObject) jsonParser.parse(reader);

    KEY_SIZE = getPropInt(prop, "key_size", KEY_SIZE);
    VALUE_SIZE = getPropInt(prop, "value_size", VALUE_SIZE);
    TOTAL_KEYS = getPropInt(prop, "total_keys", TOTAL_KEYS);
    MICROBENCHMARK_TYPE = MicrobenchmarkType.valueOf(getPropString(prop,
        "microbenchmark_type", MICROBENCHMARK_TYPE.toString()).toUpperCase());
    WORKLOAD_TYPE = WorkloadType
        .valueOf(getPropString(prop, "workload_type", WORKLOAD_TYPE.toString())
            .toUpperCase());
    WORKLOAD_ZIPFIAN_CONSTANT = getPropDouble(prop, "workload_zipfian_constant",
        WORKLOAD_ZIPFIAN_CONSTANT);
    BACKING_STORE_TYPE = BackingStoreType.valueOf(
        getPropString(prop, "backing_store_type", BACKING_STORE_TYPE.toString())
            .toUpperCase());
    BACKING_STORE_BATCH_SIZE =
        getPropInt(prop, "backing_store_batch_size", BACKING_STORE_BATCH_SIZE);
    BACKING_STORE_RING_ORAM_N = getPropInt(prop, "backing_store_ring_oram_n",
        BACKING_STORE_RING_ORAM_N);
    BACKING_STORE_RING_ORAM_Z = getPropInt(prop, "backing_store_ring_oram_z",
        BACKING_STORE_RING_ORAM_Z);
    BACKING_STORE_RING_ORAM_S = getPropInt(prop, "backing_store_ring_oram_s",
        BACKING_STORE_RING_ORAM_S);
    BACKING_STORE_RING_ORAM_A = getPropInt(prop, "backing_store_ring_oram_a",
        BACKING_STORE_RING_ORAM_A);
    BACKING_STORE_PARALLEL_RING_ORAM_THREADS =
        getPropInt(prop, "backing_store_parallel_ring_oram_threads",
            BACKING_STORE_PARALLEL_RING_ORAM_THREADS);
    READ_RATIO = getPropDouble(prop, "read_ratio", READ_RATIO);
    EXPERIMENT_LENGTH =
        getPropInt(prop, "experiment_length", EXPERIMENT_LENGTH);
    RANDOM_SEED = getPropLong(prop, "random_seed", RANDOM_SEED);
    STORE_LATENCY = getPropInt(prop, "store_latency", STORE_LATENCY);
    TIMED_RAMP_UP_TIME =
        getPropInt(prop, "timed_ramp_up_time", TIMED_RAMP_UP_TIME);
    BACKING_STORE_RING_ORAM_ENCRYPT_BLOCKS = Boolean
        .valueOf(getPropString(prop, "backing_store_ring_oram_encrypt_blocks",
            Boolean.toString(BACKING_STORE_RING_ORAM_ENCRYPT_BLOCKS))
            .toLowerCase());
    WRITE_DEFAULT_VALUE =
        Boolean.valueOf(getPropString(prop, "write_default_value",
            Boolean.toString(WRITE_DEFAULT_VALUE)).toLowerCase());
    OUTPUT_DIR = getPropString(prop, "output_dir", OUTPUT_DIR);
    BACKING_STORE_RING_ORAM_READ_PATH_ALL_REAL = Boolean
        .valueOf(getPropString(prop, "backing_store_ring_oram_read_path_all_real",
            Boolean.toString(BACKING_STORE_RING_ORAM_READ_PATH_ALL_REAL)
                .toLowerCase()));
    BACKING_STORE_RING_ORAM_L = getPropInt(prop, "backing_store_ring_oram_l", BACKING_STORE_RING_ORAM_L);
    isInitialised = true;
  }

  @Override
  public void loadProperties() {
    isInitialised = false;
  }

  public enum WorkloadType {
    UNIFORM, ZIPFIAN
  }

  public enum BackingStoreType {
    HASH_MAP, SEQUENTIAL_RING_ORAM, PARALLEL_RING_ORAM
  }

  public enum MicrobenchmarkType {
    FIXED_OPERATION_SET, TIMED
  }

  public void addToJson(JSONObject json) {
    JSONObject config = new JSONObject();
    config.put("key_size", KEY_SIZE);
    config.put("value_size", VALUE_SIZE);
    config.put("total_keys", TOTAL_KEYS);
    config.put("microbenchmark_type", MICROBENCHMARK_TYPE.toString());
    config.put("workload_type", WORKLOAD_TYPE.toString());
    config.put("workload_zipfian_constant", WORKLOAD_ZIPFIAN_CONSTANT);
    config.put("backing_store_type", BACKING_STORE_TYPE.toString());
    config.put("backing_store_batch_size", BACKING_STORE_BATCH_SIZE);
    config.put("backing_store_ring_oram_n", BACKING_STORE_RING_ORAM_N);
    config.put("backing_store_ring_oram_z", BACKING_STORE_RING_ORAM_Z);
    config.put("backing_store_ring_oram_s", BACKING_STORE_RING_ORAM_S);
    config.put("backing_store_ring_oram_a", BACKING_STORE_RING_ORAM_A);
    config
        .put("backing_store_parallel_ring_oram_threads", BACKING_STORE_PARALLEL_RING_ORAM_THREADS);
    config.put("read_ratio", READ_RATIO);
    config.put("experiment_length", EXPERIMENT_LENGTH);
    config.put("random_seed", RANDOM_SEED);
    config.put("store_latency", STORE_LATENCY);
    config.put("timed_ramp_up_time", TIMED_RAMP_UP_TIME);
    config.put("backing_store_ring_oram_encrypt_blocks",
        Boolean.toString(BACKING_STORE_RING_ORAM_ENCRYPT_BLOCKS));
    config.put("write_default_value", Boolean.toString(WRITE_DEFAULT_VALUE));
    config.put("output_dir", OUTPUT_DIR);
    config.put("backing_store_ring_oram_read_path_all_real", Boolean.toString(BACKING_STORE_RING_ORAM_READ_PATH_ALL_REAL));
    config.put("backing_store_ring_oram_l", BACKING_STORE_RING_ORAM_L);
    json.put("config", config);
  }
}
