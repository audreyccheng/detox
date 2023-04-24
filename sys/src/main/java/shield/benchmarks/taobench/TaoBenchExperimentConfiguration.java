package shield.benchmarks.taobench;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.ClientUtils.ClientType;
import shield.config.Configuration;

/**
 * All configuration variables necessary to setup an experiment should be placed here and loadable
 * from JSON The loadProperty() function is called once at the initialization of each block
 *
 * @author ncrooks
 */

public class TaoBenchExperimentConfiguration extends Configuration {

    public int THREADS = 1;

    public int REQ_THREADS_PER_BM_THREAD = 3;

    public double PROB_TRX_READ= 58.0; //49.0; //  59.0; //
    public double PROB_TRX_READ_TXN = 83.0; // 89.0; // 84.0; //
    public double PROB_TRX_READ_SCAN = 100.0; // 97.0; //
    public double PROB_TRX_READ_1= 60.0; // 100.0 // 60.0
    public double PROB_TRX_WRITE = 100.0;
    public int NB_OBJECTS = 100000;
    public int BASE_SIZE = 10000;
    public List<Integer> DATA_SIZES = Arrays.asList(75);
    public List<Double> DATA_WEIGHTS = Arrays.asList(1.0);

    // TBU
    public List<Integer> TXN_SIZES_5 = Arrays.asList(10,15,20,25,30); //5,6,7,8,10,20); //2,3,4); //
    public List<Double> TXN_WEIGHTS_5 = Arrays.asList(0.20,0.20,0.20,0.20,0.20); //0.2,0.2,0.2,0.2,0.19,0.01); //0.4,0.35,0.25); //
    public List<Integer> TXN_SIZES_6 = Arrays.asList(50,51,52,53,54,55,56,57,58,59,60); //10,20,30,40,50,60); //2,3,4); //
    public List<Double> TXN_WEIGHTS_6 = Arrays.asList(0.05,0.05,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1); //0.2,0.2,0.1,0.1,0.2,0.2); //0.4,0.35,0.25); //
    public List<Integer> TXN_SIZES_7 = Arrays.asList(2,3,4,5,6,7,8,9,10,20);
    public List<Double> TXN_WEIGHTS_7 = Arrays.asList(0.15,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.05,0.05);

    /**
     * Warm-up period before which results start being collected
     */
    public int RAMP_UP = 60;

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

    /**
     * True if must pad columns
     */

    public boolean PAD_COLUMNS = true;
    /**
     * True if must load keys
     */
    public boolean MUST_LOAD_KEYS = true;

    /**
     * Probability to execute amalgamate trx
     */
    public double PROB_TRX_AMALGAMATE= 15.0;
    /**
     * Probability to execute new order transaction
     */
    public double PROB_TRX_BALANCE = 15.0;
    /**
     * Probability to execute order status transaction
     */
    public double PROB_TRX_DEPOSIT_CHECKING = 15.0;
    /**
     * Probability to execute stock level transaction
     */
    public double PROB_TRX_SEND_PAYMENT= 25.0;
    /**
     * Probability to execute payment transaction
     */
    public double PROB_TRX_TRANSACT_SAVINGS= 15.0;

    /**
     *Probability that access a hotspot account
     */
    public int PROB_ACCOUNT_HOTSPOT  = 90;

    /**
     * Number of accounts
     */
    public int NB_ACCOUNTS = 1000000;

    public boolean HOTSPOT_USE_FIXED_SIZE  = false;
    public double HOTSPOT_PERCENTAGE = 10; // [0% - 100%]
    public int HOTSPOT_FIXED_SIZE  = 100; // fixed number of tuples

    // Initial balance amount
    // We'll just make it really big so that they never run out of money
    public int MIN_BALANCE             = 10000;
    public int MAX_BALANCE             = 50000;
    public int PARAM_SEND_PAYMENT_AMOUNT = 5;
    public int PARAM_DEPOSIT_CHECKING_AMOUNT = 1;
    public int PARAM_TRANSACT_SAVINGS_AMOUNT = 20;
    public int PARAM_WRITE_CHECK_AMOUNT = 5;

    public boolean USE_THINK_TIME = true;
    public int THINK_TIME = 10;

    /**
     * Max size of the variable metadata column
     */
    public int VAR_DATA_SIZE = 93;
    public int NAME_SIZE=64;

    public ClientType CLIENT_TYPE = ClientType.SHIELD;

    public TaoBenchExperimentConfiguration(String configFileName)
            throws IOException, ParseException {
        loadProperties(configFileName);
    }

    public TaoBenchExperimentConfiguration() {
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

        NB_OBJECTS = getPropInt(prop, "nb_objects", NB_OBJECTS);
        BASE_SIZE = getPropInt(prop, "base_size", BASE_SIZE);

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
        CLIENT_TYPE = ClientUtils.fromStringToClientType(getPropString(prop, "client_type", ""));
        PROB_TRX_READ= getPropDouble(prop, "prob_trx_read", PROB_TRX_READ);
        PROB_TRX_AMALGAMATE= getPropDouble(prop, "prob_trx_amalgamate", PROB_TRX_AMALGAMATE);
        PROB_TRX_BALANCE=getPropDouble(prop, "prob_trx_balance", PROB_TRX_BALANCE);
        PROB_TRX_DEPOSIT_CHECKING= getPropDouble(prop, "prob_trx_deposit_checking", PROB_TRX_DEPOSIT_CHECKING);
        PROB_TRX_SEND_PAYMENT=getPropDouble(prop, "prob_trx_send_payment", PROB_TRX_SEND_PAYMENT);
        PROB_TRX_TRANSACT_SAVINGS= getPropDouble(prop, "prob_trx_transact_savings", PROB_TRX_TRANSACT_SAVINGS);
        NB_ACCOUNTS = getPropInt(prop, "nb_accounts", NB_ACCOUNTS);
        HOTSPOT_PERCENTAGE = getPropDouble(prop, "hotspot_percentage", HOTSPOT_PERCENTAGE);
        PROB_ACCOUNT_HOTSPOT = getPropInt(prop, "prob_account_hotspot", PROB_ACCOUNT_HOTSPOT);
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