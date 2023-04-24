package shield.config;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.StatisticsCollector;
import shield.proxy.data.async.IAsyncBackingStore;
import shield.proxy.data.async.IAsyncBackingStore.BackingStoreType;
import shield.proxy.oram.enc.MaskAlgorithm;
import shield.proxy.oram.enc.MaskAlgorithmType;
import shield.proxy.trx.concurrency.TransactionManager;
import shield.proxy.trx.concurrency.TransactionManager.CCManagerType;
import shield.util.Logging;

import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

/**
 * All configuration variables necessary to setup the proxy or servers should be placed here and
 * loadable from JSON The loadProperty() function is called once at the initialization of each
 * block
 *
 * @author ncrooks
 */

public class NodeConfiguration extends Configuration {

  /**
   * Guard flag: determines whether this block has been initialized No work should be done until
   * executed
   */
  private boolean isInitialised = false;

  /**
   * Logger for this block
   */
  public Logging logger = null;

  public Logging.Level LOGGING_LEVEL = Logging.Level.CRITICAL;

  public Logging.Level fromStringToLogging(String logging) {
    if (logging.equals("critical")) {
      return Logging.Level.CRITICAL;
    } else if (logging.equals("fine")) {
      return Logging.Level.FINE;
    } else if (logging.equals("info")) {
      return Logging.Level.INFO;
    } else if (logging.equals("warning")) {
      return Logging.Level.WARNING;
    } else if (logging.equals("none")) {
      return Logging.Level.NONE;
    } else {
      return Logging.Level.CRITICAL;
    }
  }

  /**
   * Unique Node Id
   */
  public long NODE_UID = System.currentTimeMillis();

  public int REQ_THREADS_PER_BM_THREAD = 3;
  /**
   * Size of the worker thread pool (TODO(natacha): find identical)
   */
  public int N_WORKER_THREADS = Runtime.getRuntime().availableProcessors() + 1;

  /**
   * Size of the thread pool in charge of receiving connections
   */
  public int N_RECEIVER_NET_THREADS = 1;
  /**
   * * Size of the thread pool in charge of sending data
   */
  public int N_SENDER_NET_THREADS = 1;

  /**
   * Proxy address
   */
  public String PROXY_IP_ADDRESS = "localhost";

  /**
   * Proxy Listening Port
   */
  public int PROXY_LISTENING_PORT = 20414;

  /**
   * This node's host
   */
  public String NODE_IP_ADDRESS = "localhost";

  /**
   * This node's port number
   */
  public int NODE_LISTENING_PORT = new Random().nextInt(20000) + 2000;


  /**
   * Remote store (if applicable) ip address
   */
  public String REMOTE_STORE_IP_ADDRESS = "localhost";

  /**
   * Remote store (if applicable) listening port
   */
  public int REMOTE_STORE_LISTENING_PORT = new Random().nextInt(20000) + 2000;

  /**
   * True if garbage collection is activated in the concurrency control manager
   */
  public boolean GC_ACTIVATED = false;

  /**
   * Number of concurrent garbage collection threads
   */
  public int GC_THREADS = 4;

  /**
   * True if return commit only when a transaction has been durably flushed (ak:
   */
  public boolean COMMIT_ON_DURABLE = true;

  public boolean READ_FOR_UPDATE = true;


  /**
   * If this option is turned on, duplicates are allowed in strides, there is no caching
   * and multiple versions are written out the ORAM. THIS OPTION IS FOR BENCHMARKING ONLY
   * THE RESULTS RETURNED WILL NOT BE SERIALIZABLE!
   */
  public boolean ALLOW_DUPLICATES = false;

  /* ORAM configuration parameters */
  /**
   * Number of threads in Parallel ORAM
   */
  public int ORAM_THREADS = 8;

  /**
   * Maximum number of real blocks that can be stored
   */
  public int ORAM_MAX_BLOCKS = 1000;

  // TODO(natacha): do these actually need to be fixed?
  /**
   * Size of the ORAM key
   */
  public int ORAM_KEY_SIZE = 16;

  /**
   * Size of the ORAM value
   */
  public int ORAM_VALUE_SIZE = 100;

  /**
   * TODO(fill in)
   */
  public int ORAM_NONCE_LEN = 1;

  /**
   * Number of real blocks per bucket
   */
  public int ORAM_Z = 8;

  /**
   * Number of dummy blocks per bucket
   */
  public int ORAM_S = 13;

  /**
   * Number of rounds in between running EvictPath
   */
  public int ORAM_A = 8;

  public boolean ORAM_BUFFER_SYNCOPS = false;

  public MaskAlgorithmType ORAM_MASK_ALGORITHM = MaskAlgorithmType.BC_HMAC;

  public boolean ORAM_WRITE_WITHOUT_READ = false;

  public boolean ORAM_WRITE_END_BATCH = false;

  public boolean ORAM_PAD_BATCHES = true;

  public boolean ORAM_ENCRYPT_BLOCKS = true;

  public boolean ORAM_DURABLE = false;
  
  public int ORAM_DURABLE_CHECKPOINT_FREQ = 5;
  public int ORAM_DURABLE_CHECKPOINT_POSITION_MAP = 0;
  public int ORAM_DURABLE_CHECKPOINT_VALID_MAP = 1;
  public int ORAM_DURABLE_CHECKPOINT_STALE_MAP = 2;
  public int ORAM_DURABLE_CHECKPOINT_PERMUTATION_MAP = 3;
  public int ORAM_DURABLE_CHECKPOINT_EARLY_RESHUFFLE_MAP = 4;
  public int ORAM_DURABLE_MAX_DATA_SIZE = 1024;
  public int ORAM_DURABLE_MAX_STASH_SIZE = 500;

  public int MAX_COLUMN_SIZE = 100;

  public byte[] CLIENT_KEY;

  /**
   * Type of CC Manager to instantiate
   */
  public TransactionManager.CCManagerType CC_MANAGER_TYPE =
      TransactionManager.CCManagerType.BATCH;

  public TransactionManager.CCManagerType fromStringToCC(String ccType) {
    if (ccType.equals("nobatch")) {
      return TransactionManager.CCManagerType.NOBATCH;
    } else {
      return TransactionManager.CCManagerType.BATCH;
    }
  }

  /**
   * Type of backing key value store to instantiate locally
   */

  public IAsyncBackingStore.BackingStoreType BACKING_STORE_TYPE =
      IAsyncBackingStore.BackingStoreType.NORAM_HASHMAP;

  /**
   * Type of backing store to instantiate remotely;
   */
  public IAsyncBackingStore.BackingStoreType REMOTE_BACKING_STORE_TYPE =
      IAsyncBackingStore.BackingStoreType.NORAM_HASHMAP;


  public IAsyncBackingStore.BackingStoreType fromStringToDataStore(
      String backingStoreType) {
    System.out.println("Storage: " + backingStoreType);
    if (backingStoreType.equals("noram_hashmap")) {
      return IAsyncBackingStore.BackingStoreType.NORAM_HASHMAP;
    } else if (backingStoreType.equals("noram_mapdb")) {
      return IAsyncBackingStore.BackingStoreType.NORAM_MAPDB;
    } else if (backingStoreType.equals("noram_server")) {
      return IAsyncBackingStore.BackingStoreType.NORAM_SERVER;
    } else if (backingStoreType.equals("noram_dummy")) {
      return IAsyncBackingStore.BackingStoreType.NORAM_DUMMY;
    } else if (backingStoreType.equals("oram_par_hashmap")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_PAR_HASHMAP;
    } else if (backingStoreType.equals("oram_par_mapdb")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_PAR_MAPDB;
    } else if (backingStoreType.equals("oram_par_server")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_PAR_SERVER;
    } else if (backingStoreType.equals("oram_seq_hashmap")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_SEQ_HASHMAP;
    } else if (backingStoreType.equals("oram_seq_mapdb")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_SEQ_MAPDB;
    } else if (backingStoreType.equals("oram_seq_server")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_SEQ_SERVER;
    } else if (backingStoreType.equals("oram_seq_dummy")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_SEQ_DUMMY;
    } else if (backingStoreType.equals("oram_par_dummy")) {
      return IAsyncBackingStore.BackingStoreType.ORAM_PAR_DUMMY;
    }
    return BackingStoreType.NORAM_HASHMAP;
  }

  /**
   * Maximum number of minibatches/strides per batch. (This should be no smaller than the longest
   * transaction)
   */
  public int MAX_NB_STRIDE = 4;

  /**
   * Default nb of operations per stride
   */
  public int STRIDE_SIZE = 10000;

  /**
   * Max nb of writes
   */
  public int WRITES_SIZE = 1000;

  /**
   * Maximum number of "waiting" batches that have not yet been scheduled
   */
  public int MAX_PENDING_BATCHES = 5;

  /**
   * Time between two successive strides
   */
  public int TIME_BETWEEN_STRIDE = 10000;

  /**
   * Minimum time between last stride in a batch and finalising that batch This is to ensure that
   * transactions have time to send the commit request
   */
  public int FINALISE_BATCH_BUFFER_T = 100;


  /**
   * Use think time in experiments
   */
  public boolean USE_THINK_TIME = false;


  /*
   * ============================ Options specific to MapDB
   * ===========================
   */
  /**
   * Backing file of the database (if any)
   */
  public String DB_FILE_NAME = "db" + NODE_UID;

  /**
   * If true, opens the dabase in-memory (This memory is not managed by the java garbage collector,
   * but undergoes serialization overhead)
   */
  public boolean DB_IN_MEMORY = false;

  /**
   * MapDB requires all datastructures to be named: this is
   */
  public String KV_NAME = "kvStore";

  /**
   * Initial allocation size for MapDB
   */
  public int INITIAL_ALLOCATION_SIZE = 10 * 1024 * 1024 * 1024;


  /*
   * ============================= Options specific to DynamoDB
   */
  /**
   * Mandatory table for DynamoDB Transaction Library
   */
  public String AWS_TRX_TABLE_NAME = "Transactions";
  /**
   * Mandatory table for DynamoDB Transaction Library
   */
  public String AWS_TRX_IMAGES_TABLE_NAME = "TransactionsImages";
  /**
   * AWS access key
   */
  public String AWS_ACCESS_KEY = "";
  /**
   * AWS secret key
   */
  public String AWS_SECRET_KEY = "";

  /**
   * Current System does not support tables. Create a single dynamoDB table to store the full kv
   */
  public String AWS_BASE_TABLE_NAME = "SHIELD";

  /**
   * Currently use the item id (unique key) for all items as primary key
   */
  public String AWS_BASE_TABLE_HASH_KEY = "ItemId";
  /**
   * Key that points to the payload
   */
  public String AWS_BASE_TABLE_VALUE_KEY = "Value";

  /**
   * Read capacity units for DynamoDB (in 1kB reads)
   */
  public long AWS_RCUS = 1000;

  /**
   * Write capacity units for DynamoDB (in 1kb writes)
   */
  public long AWS_WCUS = 1000;

  /**
   * Maximum batch size for DynamoDB:
   * has to be smaller than 25 and smaller than 16MB
   */
  public int AWS_BATCH_SIZE = 25;

  /**
   * Number of dynamo clients to create
   */
  public int AWS_DYN_CLIENT_NB = 12;



  /**
   * Dynamo Access point
   */
  public String AWS_ACCESS_POINT = "http://dynamodb.us-east-2.amazonaws.com";

  /**
   * Dynamo Region
   */
  public String AWS_ACCESS_REGION = "us-east-2";

  /**
   * True if must create this table
   */
  public boolean AWS_CREATE_TABLE = true;

  public boolean USE_PROXY = true;


  public boolean USE_BACKOFF = false;

  // =======================================================
  public String RDS_DB_NAME = "test";

  public String RDS_USERNAME = "nacrooks";

  public String RDS_PASSWORD =  "lorenzo";

   //public String RDS_HOSTNAME = "testmysql.ccr4lwwrpfus.us-east-2.rds.amazonaws.com";

  public String RDS_HOSTNAME = "172.31.37.253";

  //public String RDS_HOSTNAME = "test.ccr4lwwrpfus.us-east-2.rds.amazonaws.com";

  public String RDS_PORT = "3306";

  public String RDS_TABLE_NAME = "mytable";

  // =======================================================
  public String REDIS_HOSTNAME = "localhost";
  public String REDIS_PORT = "6379";
  public boolean REDIS_ENABLED = true;
  public boolean REDIS_PREFETCH = true;
  public long LATENCY = 0;

  public String POSTGRES_HOSTNAME = "";
  public String POSTGRES_PORT = "5432";
  public String POSTGRES_USERNAME = "admin";
  public String POSTGRES_PASSWORD = "password";
  public String POSTGRES_DB_NAME = "benchmark";

  /*
   * =========================================================
   *
   * Statistics Collectors
   *
   */

  /**
   * Folder in which statistics data will be collected
   */
  public String EXP_DIR = "";

  public boolean LOG_ABORT_TYPES = false;
  public StatisticsCollector statsTSOAborts = null;
  public StatisticsCollector statsLateOpAbort = null;
  public StatisticsCollector statsBatchFullAbort = null;

  public boolean LOG_STORAGE_TIME = false;
  public StatisticsCollector statsStorageTime = null;
  public boolean LOG_STORAGE_SIZE = false;
  public StatisticsCollector statsStorageSize = null;

  public boolean LOG_WRITES_SAVED = false;
  public StatisticsCollector statsWritesSaved= null;
  public boolean LOG_DUPLICATES_SAVED = false;
  public StatisticsCollector statsDuplicatesSaved = null;
  public boolean LOG_CACHED_SAVED = false;
  public StatisticsCollector statsCachedSaved = null;
  public boolean LOG_TOTAL_OPERATIONS = false;
  public StatisticsCollector statsTotalOperations = null;
  public boolean LOG_DURABILITY_WRITE_TIME = false;
  public StatisticsCollector statsDurabilityWriteTime = null;
  public boolean LOG_DURABILITY_RECOVER_TIME = false;
  public StatisticsCollector statsDurabilityRecoverTime = null;
  public boolean LOG_DURABILITY_SERIALIZATION_TIME = false;
  public StatisticsCollector statsDurabilitySerializationTime = null;
  public boolean LOG_DURABILITY_CLONE_TIME = false;
  public StatisticsCollector statsDurabilityCloneTime = null;
  public boolean LOG_DURABILITY_MASK_TIME = false;
  public StatisticsCollector statsDurabilityMaskTime = null;
  public boolean LOG_DURABILITY_NETWORKING_TIME = false;
  public StatisticsCollector statsDurabilityNetworkingTime = null;

  public boolean LOG_DURABILITY_POSITION_MAP_SIZE = false;
  public boolean LOG_DURABILITY_VALID_MAP_SIZE = false;
  public boolean LOG_DURABILITY_STALE_MAP_SIZE = false;
  public boolean LOG_DURABILITY_EARLY_RESHUFFLE_MAP_SIZE = false;
  public boolean LOG_DURABILITY_PERMUTATION_MAP_SIZE = false;
  public boolean LOG_DURABILITY_STASH_SIZE = false;

  public StatisticsCollector statsDurabilityPositionMapSize = null;
  public StatisticsCollector statsDurabilityValidMapSize = null;
  public StatisticsCollector statsDurabilityStaleMapSize = null;
  public StatisticsCollector statsDurabilityEarlyReshuffleMapSize = null;
  public StatisticsCollector statsDurabilityPermutationMapSize = null;
  public StatisticsCollector statsDurabilityStashSize = null;

  public boolean LOG_DURABILITY_DESERIALIZATION_TIME = false;
  public boolean LOG_DURABILITY_UNMASK_TIME = false;
  public boolean LOG_DURABILITY_NETWORKING_READ_TIME = false;
  public StatisticsCollector statsDurabilityDeserializationTime = null;
  public StatisticsCollector statsDurabilityUnmaskTime = null;
  public StatisticsCollector statsDurabilityNetworkingReadTime = null;

  public boolean LOG_STORAGE_ACCESSES_PER_BATCH = false;
  public StatisticsCollector statsStorageReadsPerBatch = null;
  public StatisticsCollector statsStorageWritesPerBatch = null;

  public NodeConfiguration(String configFileName)
      throws IOException, ParseException {
    this.configFileName = configFileName;
    if (configFileName != null) {
      loadProperties(configFileName);
    } else {
      loadProperties();
    }
  }

  public NodeConfiguration() {
    loadProperties();
  }

  /**
   * Loads the constant values from JSON file. If add a new constant in this config file, should
   * also add the ability to parse it in this file
   */
  public void loadProperties(String fileName)
      throws IOException, ParseException {

    FileReader reader = new FileReader(fileName);
    if (fileName == "") {
      System.err.println("Empty Property File, Intentional?");
    }
    JSONParser jsonParser = new JSONParser();
    JSONObject prop = (JSONObject) jsonParser.parse(reader);

    logger = Logging.getLogger(Logging.Level.FINE);
    // TODO: add parsing code here

    isInitialised = true;

    LOGGING_LEVEL =
        fromStringToLogging(getPropString(prop, "logging_level", ""));
    NODE_UID = getPropLong(prop, "node_uid", NODE_UID);
    REQ_THREADS_PER_BM_THREAD = getPropInt(prop, "req_threads_per_bm_thread", REQ_THREADS_PER_BM_THREAD);
    N_WORKER_THREADS = getPropInt(prop, "n_worker_threads", N_WORKER_THREADS);
    N_RECEIVER_NET_THREADS =
        getPropInt(prop, "n_receiver_net_threads", N_RECEIVER_NET_THREADS);
    N_SENDER_NET_THREADS =
        getPropInt(prop, "n_sender_net_threads", N_SENDER_NET_THREADS);
    N_RECEIVER_NET_THREADS =
        getPropInt(prop, "n_receiver_net_threads", N_RECEIVER_NET_THREADS);
    PROXY_IP_ADDRESS =
        getPropString(prop, "proxy_ip_address", PROXY_IP_ADDRESS);
    PROXY_LISTENING_PORT =
        getPropInt(prop, "proxy_listening_port", PROXY_LISTENING_PORT);
    NODE_IP_ADDRESS = getPropString(prop, "node_ip_address", NODE_IP_ADDRESS);
    NODE_LISTENING_PORT =
        getPropInt(prop, "node_listening_port", NODE_LISTENING_PORT);
    MAX_NB_STRIDE = getPropInt(prop, "max_nb_stride", MAX_NB_STRIDE);
    STRIDE_SIZE = getPropInt(prop, "stride_size", STRIDE_SIZE);
    WRITES_SIZE = getPropInt(prop, "writes_size", WRITES_SIZE);
    MAX_PENDING_BATCHES =
        getPropInt(prop, "max_pending_batches", MAX_PENDING_BATCHES);
    TIME_BETWEEN_STRIDE =
        getPropInt(prop, "min_between_stride", TIME_BETWEEN_STRIDE);
    FINALISE_BATCH_BUFFER_T =
        getPropInt(prop, "finalise_batch_buffer_t", FINALISE_BATCH_BUFFER_T);
    DB_FILE_NAME = getPropString(prop, "db_file_path", DB_FILE_NAME);
    KV_NAME = getPropString(prop, "kv_name", KV_NAME);
    DB_IN_MEMORY = getPropBool(prop, "db_in_memory", DB_IN_MEMORY);
    CC_MANAGER_TYPE =
        fromStringToCC(getPropString(prop, "cc_manager_type", ""));
    BACKING_STORE_TYPE =
        fromStringToDataStore(getPropString(prop, "backing_store_type", ""));
    REMOTE_BACKING_STORE_TYPE =
        fromStringToDataStore(getPropString(prop, "remote_backing_store_type", ""));
    REMOTE_STORE_IP_ADDRESS =
        getPropString(prop, "remote_store_ip_address", REMOTE_STORE_IP_ADDRESS);
    REMOTE_STORE_LISTENING_PORT = getPropInt(prop,
        "remote_store_listening_port", REMOTE_STORE_LISTENING_PORT);
    GC_ACTIVATED = getPropBool(prop, "gc_activated", GC_ACTIVATED);
    if (CC_MANAGER_TYPE == CCManagerType.BATCH) GC_ACTIVATED = false;
    GC_THREADS = getPropInt(prop, "gc_threads", GC_THREADS);
     COMMIT_ON_DURABLE =
     //   getPropBool(prop, "commit_on_durable", COMMIT_ON_DURABLE);
         true;
    MAX_COLUMN_SIZE =
        getPropInt(prop, "max_column_size", MAX_COLUMN_SIZE);
    ALLOW_DUPLICATES =  getPropBool(prop, "allow_duplicates", ALLOW_DUPLICATES);
    READ_FOR_UPDATE = getPropBool(prop, "read_for_update", READ_FOR_UPDATE);

    // ORAM properties
    ORAM_THREADS = getPropInt(prop, "oram_threads", ORAM_THREADS);
    ORAM_MAX_BLOCKS = getPropInt(prop, "oram_max_blocks", ORAM_MAX_BLOCKS);
    ORAM_KEY_SIZE = getPropInt(prop, "oram_key_size", ORAM_KEY_SIZE);
    ORAM_VALUE_SIZE = getPropInt(prop, "oram_value_size", ORAM_VALUE_SIZE);
    ORAM_NONCE_LEN = getPropInt(prop, "oram_nonce_len", ORAM_NONCE_LEN);
    CLIENT_KEY = getPropString(prop, "oram_client_key", "").getBytes();
    ORAM_Z = getPropInt(prop, "oram_z", ORAM_Z);
    ORAM_S = getPropInt(prop, "oram_s", ORAM_S);
    ORAM_A = getPropInt(prop, "oram_a", ORAM_A);
    ORAM_MASK_ALGORITHM = MaskAlgorithmType.valueOf(getPropString(prop, "oram_mask_algorithm", ORAM_MASK_ALGORITHM.toString().toLowerCase()).toUpperCase());
    ORAM_WRITE_WITHOUT_READ = getPropBool(prop, "oram_write_without_read", ORAM_WRITE_WITHOUT_READ);
    ORAM_WRITE_END_BATCH = getPropBool(prop, "oram_write_end_batch", ORAM_WRITE_END_BATCH);
    ORAM_PAD_BATCHES = getPropBool(prop, "oram_pad_batches", ORAM_PAD_BATCHES);
    ORAM_ENCRYPT_BLOCKS = getPropBool(prop, "oram_encrypt_blocks", ORAM_ENCRYPT_BLOCKS);
    ORAM_BUFFER_SYNCOPS = getPropBool(prop, "oram_buffer_syncops", ORAM_BUFFER_SYNCOPS);

    ORAM_DURABLE = getPropBool(prop, "oram_durable", ORAM_DURABLE);
    ORAM_DURABLE_CHECKPOINT_FREQ = getPropInt(prop, "oram_durable_checkpoint_freq", ORAM_DURABLE_CHECKPOINT_FREQ);
    ORAM_DURABLE_CHECKPOINT_POSITION_MAP = getPropInt(prop, "oram_durable_checkpoint_position_map", ORAM_DURABLE_CHECKPOINT_POSITION_MAP);
    ORAM_DURABLE_CHECKPOINT_VALID_MAP = getPropInt(prop, "oram_durable_checkpoint_valid_map", ORAM_DURABLE_CHECKPOINT_VALID_MAP);
    ORAM_DURABLE_CHECKPOINT_STALE_MAP = getPropInt(prop, "oram_durable_checkpoint_stale_map", ORAM_DURABLE_CHECKPOINT_STALE_MAP);
    ORAM_DURABLE_CHECKPOINT_PERMUTATION_MAP = getPropInt(prop, "oram_durable_checkpoint_permutation_map",ORAM_DURABLE_CHECKPOINT_PERMUTATION_MAP);
    ORAM_DURABLE_CHECKPOINT_EARLY_RESHUFFLE_MAP =getPropInt(prop, "oram_durable_checkpoint_early_reshuffle_map", ORAM_DURABLE_CHECKPOINT_EARLY_RESHUFFLE_MAP);
    ORAM_DURABLE_MAX_DATA_SIZE = getPropInt(prop, "oram_durable_max_data_size", ORAM_DURABLE_MAX_DATA_SIZE);
    ORAM_DURABLE_MAX_STASH_SIZE = getPropInt(prop, "oram_durable_max_stash_size", ORAM_DURABLE_MAX_STASH_SIZE);

    AWS_TRX_TABLE_NAME =
        getPropString(prop, "aws_trx_table_name", AWS_TRX_TABLE_NAME);
    AWS_TRX_IMAGES_TABLE_NAME = getPropString(prop, "aws_trx_images_table_name",
        AWS_TRX_IMAGES_TABLE_NAME);
    AWS_ACCESS_KEY = getPropString(prop, "aws_access_key", AWS_ACCESS_KEY);
    AWS_SECRET_KEY = getPropString(prop, "aws_secret_key", AWS_SECRET_KEY);

    AWS_BASE_TABLE_NAME = getPropString(prop, "aws_base_table_name", AWS_BASE_TABLE_NAME);
    AWS_BASE_TABLE_HASH_KEY = getPropString(prop, "aws_base_table_hash_key", AWS_BASE_TABLE_HASH_KEY);
    AWS_BASE_TABLE_VALUE_KEY =
        getPropString(prop, "aws_base_table_value_key", AWS_BASE_TABLE_VALUE_KEY);

    AWS_RCUS = getPropLong(prop, "aws_rcus", AWS_RCUS);
    AWS_WCUS = getPropLong(prop, "aws_wcus", AWS_WCUS);

    AWS_ACCESS_REGION = getPropString(prop, "aws_access_region", AWS_ACCESS_REGION);
    AWS_ACCESS_POINT =
        "http://dynamodb." + AWS_ACCESS_REGION + ".amazonaws.com";

    RDS_PASSWORD = getPropString(prop, "rds_password", RDS_PASSWORD);
    RDS_USERNAME = getPropString(prop, "rds_username", RDS_USERNAME);
    RDS_HOSTNAME = getPropString(prop, "rds_hostname", RDS_HOSTNAME);
    RDS_DB_NAME = getPropString(prop, "rds_db_name", RDS_DB_NAME);
    RDS_TABLE_NAME = getPropString(prop, "rds_table_name", RDS_TABLE_NAME);
    RDS_PORT = getPropString(prop, "rds_port", RDS_PORT);

    // REDIS-POSTGRES CLIENT
    REDIS_HOSTNAME = getPropString(prop, "redis_hostname", REDIS_HOSTNAME);
    REDIS_PORT = getPropString(prop, "redis_post", REDIS_PORT);
    REDIS_ENABLED = getPropBool(prop, "redis_enabled", REDIS_ENABLED);
    REDIS_PREFETCH = getPropBool(prop, "redis_prefetch", REDIS_PREFETCH);
    LATENCY = getPropLong(prop, "latency", LATENCY);

    POSTGRES_HOSTNAME = getPropString(prop, "postgres_hostname", POSTGRES_HOSTNAME);
    POSTGRES_PORT = getPropString(prop, "postgres_port", POSTGRES_PORT);
    POSTGRES_USERNAME = getPropString(prop, "postgres_username", POSTGRES_USERNAME);
    POSTGRES_PASSWORD = getPropString(prop, "postgres_password", POSTGRES_PASSWORD);
    POSTGRES_DB_NAME = getPropString(prop, "postgres_db_name", POSTGRES_DB_NAME);

    USE_PROXY = getPropBool(prop, "useproxy", USE_PROXY);
    USE_BACKOFF = getPropBool(prop, "use_backoff", USE_BACKOFF);
    // Load statistics collectors
    loadStats(prop);

  }


  /**
   * This is a test method which initializes constants to default values without the need to pass in
   * a configuration file
   *
   * @return true if initialization successful
   */
  public void loadProperties() {

    logger = Logging.getLogger(Logging.Level.FINE);
    isInitialised = true;

  }

  /**
   * Loads all statistics collectors and benchmarking knobs.
   *
   * @param prop - the json file
   */
  private void loadStats(JSONObject prop) {
    EXP_DIR = getPropString(prop, "log_folder", "") + "/";
    JSONObject logging = (JSONObject) prop.get("logging");
    if (logging == null) {
      return;
    }

    boolean savedWork = getPropBool(logging, "logging_reduced_work", false);
    LOG_WRITES_SAVED = savedWork;
    LOG_DUPLICATES_SAVED = savedWork;
    LOG_CACHED_SAVED = savedWork;
    LOG_TOTAL_OPERATIONS = savedWork;
    statsWritesSaved =  savedWork? new StatisticsCollector(EXP_DIR + "log_writessaved.txt"):null;
    statsCachedSaved = savedWork? new StatisticsCollector(EXP_DIR + "log_cachedsaved.txt"):null;
    statsDuplicatesSaved = savedWork? new StatisticsCollector(EXP_DIR + "log_duplicatessaved.txt"):null;
    statsTotalOperations = savedWork? new StatisticsCollector(EXP_DIR + "log_totoperations.txt"):null;
    boolean durability = getPropBool(logging, "logging_durability", true);
    LOG_DURABILITY_WRITE_TIME = durability;
    LOG_DURABILITY_RECOVER_TIME = durability;
    LOG_DURABILITY_SERIALIZATION_TIME = durability;
    LOG_DURABILITY_CLONE_TIME = durability;
    LOG_DURABILITY_MASK_TIME = durability;
    LOG_DURABILITY_NETWORKING_TIME = durability;

    LOG_DURABILITY_DESERIALIZATION_TIME = durability;
    LOG_DURABILITY_UNMASK_TIME = durability;
    LOG_DURABILITY_NETWORKING_READ_TIME = durability;

    LOG_DURABILITY_POSITION_MAP_SIZE = durability;
    LOG_DURABILITY_VALID_MAP_SIZE = durability;
    LOG_DURABILITY_STALE_MAP_SIZE = durability;
    LOG_DURABILITY_EARLY_RESHUFFLE_MAP_SIZE = durability;
    LOG_DURABILITY_PERMUTATION_MAP_SIZE = durability;
    LOG_DURABILITY_STASH_SIZE = durability;

    statsDurabilityWriteTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_write_time.txt") : null;
    statsDurabilityRecoverTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_recover_time.txt") : null;
    statsDurabilitySerializationTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_serialization_time.txt") : null;
    statsDurabilityCloneTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_clone_time.txt") : null;
    statsDurabilityMaskTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_mask_time.txt") : null;
    statsDurabilityNetworkingTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_networking_time.txt") : null;

    statsDurabilityDeserializationTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_deserialization_time.txt") : null;
    statsDurabilityUnmaskTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_unmask_time.txt") : null;
    statsDurabilityNetworkingReadTime = durability ? new StatisticsCollector(EXP_DIR + "log_durability_networking_read_time.txt") : null;

    statsDurabilityPositionMapSize = durability ? new StatisticsCollector(EXP_DIR + "log_durability_position_map_size.txt") : null;
    statsDurabilityValidMapSize = durability ? new StatisticsCollector(EXP_DIR + "log_durability_valid_map_size.txt") : null;
    statsDurabilityStaleMapSize = durability ? new StatisticsCollector(EXP_DIR + "log_durability_stale_map_size.txt") : null;
    statsDurabilityEarlyReshuffleMapSize = durability ? new StatisticsCollector(EXP_DIR + "log_durability_early_reshuffle_map_size.txt") : null;
    statsDurabilityPermutationMapSize = durability ? new StatisticsCollector(EXP_DIR + "log_durability_permutation_map_size.txt") : null;
    statsDurabilityStashSize = durability ? new StatisticsCollector(EXP_DIR + "log_durability_stash_size.txt") : null;

    boolean storageTime = getPropBool(logging, "logging_storage_time", false);
    LOG_STORAGE_TIME = storageTime;
    statsStorageTime = storageTime? new StatisticsCollector(EXP_DIR + "log_storage_time.txt"): null;
    LOG_STORAGE_SIZE = storageTime;
    statsStorageSize = storageTime? new StatisticsCollector(EXP_DIR + "log_storage_size.txt"): null;

    boolean logAborts = getPropBool(logging, "logging_abort_types",false);
    LOG_ABORT_TYPES = logAborts;
    statsTSOAborts = logAborts? new StatisticsCollector(EXP_DIR + "logging_tso_aborts.txt"):null;
    statsBatchFullAbort = logAborts? new StatisticsCollector(EXP_DIR + "logging_batch_full.txt"): null;
    statsLateOpAbort = logAborts? new StatisticsCollector(EXP_DIR + "logging_batch_finished.txt"):null;

    LOG_STORAGE_ACCESSES_PER_BATCH = getPropBool(logging, "logging_storage_accesses_per_batch", LOG_STORAGE_ACCESSES_PER_BATCH);
    statsStorageReadsPerBatch = LOG_STORAGE_ACCESSES_PER_BATCH ? new StatisticsCollector(EXP_DIR + "log_storage_reads_per_batch.txt") : null;
    statsStorageWritesPerBatch = LOG_STORAGE_ACCESSES_PER_BATCH ? new StatisticsCollector(EXP_DIR + "log_storage_reads_per_batch.txt") : null;
  }

  public boolean isInitialised() {
    return isInitialised == true;
  }
}
