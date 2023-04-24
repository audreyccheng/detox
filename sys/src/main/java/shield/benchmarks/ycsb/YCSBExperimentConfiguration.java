package shield.benchmarks.ycsb;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.ClientUtils.ClientType;
import shield.config.Configuration;

/**
 * All configuration variables necessary to setup an experiment should be placed here and loadable
 * from JSON The loadProperty() function is called once at the initialization of each block
 *
 * @author ncrooks
 */

public class YCSBExperimentConfiguration extends Configuration {

  public enum WorkloadType {
    UNIFORM, LATEST, ZIPFIAN, HOTSPOT
  }

  private WorkloadType stringToWorkloadType(String data) {
    if (data.equals("uniform")) {
      return WorkloadType.UNIFORM;
    } else if (data.equals("latest")) {
      return WorkloadType.LATEST;
    } else if (data.equals("latest")) {
      return WorkloadType.ZIPFIAN;
    } else if (data.equals("hotspot")) {
      return WorkloadType.HOTSPOT;
    }
    return WorkloadType.UNIFORM;
  }

  public enum ReadWriteType {
    READONLY, READHEAVY, WRITEHEAVY
  }

  private ReadWriteType stringToReadWriteType(String data) {
    if (data.equals("readonly")) {
      return ReadWriteType.READONLY;
    } else if (data.equals("readheavy")) {
      return ReadWriteType.READHEAVY;
    } else if (data.equals("writeheavy")) {
      return ReadWriteType.WRITEHEAVY;
    }
    return ReadWriteType.READONLY;
  }

  /**
   * Size of keys in bytes
   */
  public int KEY_SIZE = 16;
  /**
   * Size of values in bytes
   */
  public int VALUE_SIZE = 1;

  /**
   * Default number of keys
   */
  public int NB_KEYS = 1000000;

  /**
   * Default number of values
   */
  public int NB_VALUES = 1;

  /**
   * Object access pattern: zipfian, uniform, read-latest, etc.
   */
  public WorkloadType WORKLOAD_TYPE = WorkloadType.UNIFORM;

  /**
   * Zipfian parameter (if workload is zipfian)
   */
  public double WORKLOAD_ZIPFIAN_PARAM = 0.99;

  /**
   * Percentage of hot items (if workload is hotspot)
   */
  public double WORKLOAD_HOTSPOT_FRAC = 0.2;

  /**
   * Number of operations per transactions
   */
  public int TRX_SIZE = 10;

  /**
   * For non read-only transactions, ratio of reads vs writes in the transaction
   */
  public double READ_RATIO = 0.5;

  /**
   * Number of transactions to "pre-create"
   */
  public int NB_TRANSACTIONS = 10000;

  /**
   * Warm-up period before which results start being collected
   */
  public int RAMP_UP = 15;

  /**
   * Ramp-down period during which results are no longer collected
   */
  public int RAMP_DOWN = 15;

  /**
   * Total experiment duration (including ramp up, ramp down)
   */
  public int EXP_LENGTH = 90;

  /**
   * Name of this run (used to determine where collected data will be outputted)
   */
  public String RUN_NAME = "";

  /**
   * Experiment dir
   */
  public String EXP_DIR = "";

  /**
   * Name of the file in which the keys are stored. If the file name remains "" after load,
   */
  public String KEY_FILE_NAME = "";

  /**
   * Number of threads to run for a client instance
   */
  public int NB_CLIENT_THREADS = 16;

  /**
   * Number of loader threads
   */
  public int NB_LOADER_THREADS = 16;

  /**
   * True if must generate data
   */
  public boolean MUST_GENERATE_KEYS = true;

  /**
   * True if must load keys
   */
  public boolean MUST_LOAD_KEYS = true;

  public ClientType CLIENT_TYPE = ClientType.SHIELD;

  public YCSBExperimentConfiguration(String configFileName)
      throws IOException, ParseException {
    loadProperties(configFileName);
  }

  public YCSBExperimentConfiguration() {
    loadProperties();
  }


  /**
   * Loads the constant values from JSON file
   */
  public void loadProperties(String fileName)
      throws IOException, ParseException {

    isInitialised = true;

    FileReader reader = new FileReader(fileName);
    if (fileName == "") {
      System.err.println("Empty Property File, Intentional?");
    }
    JSONParser jsonParser = new JSONParser();
    JSONObject prop = (JSONObject) jsonParser.parse(reader);

    KEY_SIZE = getPropInt(prop, "key_size", KEY_SIZE);
    VALUE_SIZE = getPropInt(prop, "value_size", VALUE_SIZE);
    NB_KEYS = getPropInt(prop, "nb_keys", NB_KEYS);
    NB_VALUES = getPropInt(prop, "nb_values", NB_VALUES);
    WORKLOAD_TYPE =
        stringToWorkloadType(getPropString(prop, "workload_type", ""));
    WORKLOAD_ZIPFIAN_PARAM =
        getPropDouble(prop, "workload_zipfian_param", WORKLOAD_ZIPFIAN_PARAM);
    WORKLOAD_HOTSPOT_FRAC =
        getPropDouble(prop, "workload_hotspot_frac", WORKLOAD_HOTSPOT_FRAC);
    TRX_SIZE = getPropInt(prop, "trx_size", TRX_SIZE);
    READ_RATIO = getPropDouble(prop, "read_ratio", READ_RATIO);
    NB_TRANSACTIONS = getPropInt(prop, "nb_transactions", NB_TRANSACTIONS);
    RAMP_UP = getPropInt(prop, "ramp_up", RAMP_UP);
    RAMP_DOWN = getPropInt(prop, "ramp_down", RAMP_DOWN);
    CLIENT_TYPE = ClientUtils.fromStringToClientType(getPropString(prop, "client_type", ""));
    EXP_LENGTH = getPropInt(prop, "exp_length", EXP_LENGTH);
    RUN_NAME = getPropString(prop, "run_name", RUN_NAME);
    EXP_DIR = getPropString(prop, "exp_dir", EXP_DIR);
    KEY_FILE_NAME = getPropString(prop, "key_file_name", KEY_FILE_NAME);
    NB_CLIENT_THREADS = getPropInt(prop, "nb_client_threads", NB_CLIENT_THREADS);
    NB_LOADER_THREADS = getPropInt(prop, "nb_loader_threads", NB_LOADER_THREADS);
    MUST_LOAD_KEYS = getPropBool(prop, "must_load_keys", MUST_LOAD_KEYS);
  }

  /**
   * This is a test method which initializes constants to default values without the need to pass in
   * a configuration file
   *
   * @return true if initialization successful
   */
  public void loadProperties() {
    isInitialised = true;
  }

}
