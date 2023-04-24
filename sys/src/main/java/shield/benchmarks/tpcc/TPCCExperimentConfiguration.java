
package shield.benchmarks.tpcc;

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

public class TPCCExperimentConfiguration extends Configuration {

  public int THREADS = 1;

  public int REQ_THREADS_PER_BM_THREAD = 3;

  /**
   * Size of values in bytes
   */
  public int VALUE_SIZE = 1000;

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
   * Number of loader threads
   */
  public int NB_LOADER_THREADS = 16;

  public ClientType CLIENT_TYPE = ClientType.SHIELD;

  /**
   * True if must pad columns
   */

  public boolean PAD_COLUMNS = true;
  /**
   * True if must load keys
   */
  public boolean MUST_LOAD_KEYS = true;

  /**
   * Probability to execute delivery trx
   */
  public double PROB_TRX_DELIVERY = 4.0;
  /**
   * Probability to execute new order transaction
   */
  public double PROB_TRX_NEW_ORDER = 44.0;
  /**
   * Probability to execute order status transaction
   */
  public double PROB_TRX_ORDER_STATUS = 4.0;
  /**
   * Probability to execute stock level transaction
   */
  public double PROB_TRX_STOCK_LEVEL = 4.0;
  /**
   * Probability to execute payment transaction
   */
  public double PROB_TRX_PAYMENT = 44.0;
  /**
   * Number of items per warehouse
   */
  public int NB_ITEMS = 100000;
  /**
   * Number of warehouses
   */
  public int NB_WAREHOUSES = 10;

  /**
   * Percent of accesses by last name in payment transaction
   */
  public int PERCENT_ACCESS_LAST_NAME = 0;

  /**
   * Number of districts NB: THIS CANNOT BE CHANGE (CURRENTLY)
   * as it will make the schema incorrect
   */

  public int NB_DISTRICTS = 10;
  /**
   * Number of customers
   */
  public int NB_CUSTOMERS = 3000 ;

  /**
   * Max number of customers with the same name (necessary
   * to make sure that max row size is not greater than configured)
   */
  public int MAX_CUSTOMERS_NAME = 44;

  /**
   * Number of initial orders on init (TODO: check)
   */
  public int INIT_NEW_ORDER_NB = 3000; // NB_CUSTOMERS;

  public boolean USE_THINK_TIME = true;

  public int THINK_TIME = 10;

  public TPCCExperimentConfiguration(String configFileName)
      throws IOException, ParseException {
    loadProperties(configFileName);
  }

  public TPCCExperimentConfiguration() {
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
    CLIENT_TYPE = ClientUtils.fromStringToClientType(getPropString(prop, "client_type", ""));

    THREADS = getPropInt(prop, "threads", THREADS);
    REQ_THREADS_PER_BM_THREAD = getPropInt(prop, "req_threads_per_bm_thread", REQ_THREADS_PER_BM_THREAD);

    RAMP_UP = getPropInt(prop, "ramp_up", RAMP_UP);
    RAMP_DOWN = getPropInt(prop, "ramp_down", RAMP_DOWN);
    EXP_LENGTH = getPropInt(prop, "exp_length", EXP_LENGTH);
    RUN_NAME = getPropString(prop, "run_name", RUN_NAME);
    EXP_DIR = getPropString(prop, "exp_dir", EXP_DIR);
    KEY_FILE_NAME = getPropString(prop, "key_file_name", KEY_FILE_NAME);
    NB_LOADER_THREADS = getPropInt(prop, "nb_loader_threads", NB_LOADER_THREADS);
    PAD_COLUMNS = getPropBool(prop, "pad_columns", PAD_COLUMNS);
    PROB_TRX_DELIVERY = getPropDouble(prop, "prob_trx_delivery", PROB_TRX_DELIVERY);
    PROB_TRX_NEW_ORDER =getPropDouble(prop, "prob_trx_new_order", PROB_TRX_NEW_ORDER);
    PROB_TRX_PAYMENT = getPropDouble(prop, "prob_trx_payment", PROB_TRX_PAYMENT);
    PROB_TRX_ORDER_STATUS =getPropDouble(prop, "prob_trx_order_status", PROB_TRX_ORDER_STATUS);
    PROB_TRX_STOCK_LEVEL = getPropDouble(prop, "prob_trx_stock_level", PROB_TRX_STOCK_LEVEL);
    NB_WAREHOUSES = getPropInt(prop, "nb_warehouses", NB_WAREHOUSES);
    NB_CUSTOMERS = getPropInt(prop, "nb_customers", NB_CUSTOMERS);
    NB_ITEMS = getPropInt(prop, "nb_items", NB_ITEMS);
    PERCENT_ACCESS_LAST_NAME = getPropInt(prop, "percent_access_last_name", PERCENT_ACCESS_LAST_NAME);
    INIT_NEW_ORDER_NB = getPropInt(prop, "init_new_order_nb", INIT_NEW_ORDER_NB);
    USE_THINK_TIME = getPropBool(prop, "use_think_time", USE_THINK_TIME);
    THINK_TIME = getPropInt(prop, "think_time", THINK_TIME);
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