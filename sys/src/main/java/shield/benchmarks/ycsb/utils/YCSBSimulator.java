package shield.benchmarks.ycsb.utils;

import shield.benchmarks.utils.Generator;
import shield.client.ClientTransaction;
import shield.client.DatabaseAbortException;
import shield.benchmarks.ycsb.YCSBExperimentConfiguration;
import shield.util.Pair;

import java.io.*;
import java.util.*;

/**
 * Workload Generator for YCSB
 *
 * @author ncrooks
 */
public class YCSBSimulator {

  /**
   * Configuration for this experiment
   */
  private final YCSBExperimentConfiguration config;
  /**
   * Generated keys
   */
  private LinkedList<String> keys;
  /**
   * Generated values
   */
  private ArrayList<byte[]> values;
  /**
   * Generated transactions
   */
  private ArrayList<ClientTransaction> genTransactions;

  /**
   * Random integer generator according to the chosen workload type
   */
  private NumberGenerator ycsbRandom;

  /**
   * Uniform random integer generator
   */
  private final Random random;

  /**
   * Special seeded random generator to generate same set
   * of keys for every loader
   */
  private final Random keyGenerator;

  public YCSBSimulator(YCSBExperimentConfiguration config) {

    assert (config.isInitialised());

    this.config = config;
    keys = new LinkedList<String>();
    values = new ArrayList<byte[]>(config.NB_VALUES);
    genTransactions = new ArrayList<ClientTransaction>(config.NB_TRANSACTIONS);
    ycsbRandom = null; // initialized in setupExperiment
    random = new Random(System.currentTimeMillis());
    keyGenerator = new Random(0);
  }

  /**
   * Setups up the experiment according to the parameters specified by the
   */
  public void setupExperiment() throws DatabaseAbortException {
    // First generate or load keys
    setupGenerator();
    generateKeys();
    generateValues();
  }

  /**
   * Setups experiment as in the method above, but loads the input keys from a file. The method will
   * fail if the said file does not exist.
   */
  public void setupExperimentFromFile(String fileName)
      throws FileNotFoundException, IOException, DatabaseAbortException {
    // First generate or load keys
    setupGenerator();
    loadKeysFromFile(fileName);
    generateValues();
  }


  /**
   * @return the pre-generated transactions. Any future changes to this set will be reflected in
   * here
   */
  public ArrayList<ClientTransaction> getGeneratedTransactions() {
    return genTransactions;
  }

  public LinkedList<String> getKeys() {
    return keys;
  }


  /**
   * Generate a configurable number of keys, of specified size in the config file TODO(natacha)
   * parallellize
   */
  public void generateKeys() {
    System.out.println("Generating keys ...");
    int i = 0;
    String k = null;
    while (i++ < config.NB_KEYS) {
      k = shield.benchmarks.utils.Generator.generateString(config.KEY_SIZE, keyGenerator);
      assert (k != null);
      keys.add(k);
    }
  }

  /**
   * Generates keys and outputs them directly to a file. For consistency, this does NOT populate the
   * keys datastructure. Any existing file is overwritten
   */
  public void dumpKeysToFile(String fileName) throws IOException {

    File fi = new File(fileName);
    System.err.println("File address is " + fileName);
    if (!fi.exists() && !fi.isDirectory()) {
      fi.createNewFile();
    }
    FileWriter file = new FileWriter(fileName);
    BufferedWriter testWriter = new BufferedWriter(file);
    for (String k : keys) {
      assert (k != null);
      testWriter.write(k);
      if (!k.equals(keys.getLast())) {
        testWriter.newLine();
      }
      testWriter.flush();
    }
    testWriter.close();
  }

  /**
   * Occasionally, the dump key to file fails to print out a newline and generates a file in an
   * incompatible format
   *
   * @return true if the file contains a single key per line
   */
  public boolean checkValidFileFormat(String fileName) throws IOException {
    FileReader file = new FileReader(fileName);
    BufferedReader testReader = new BufferedReader(file);
    String key;
    int i = 0;
    key = testReader.readLine();
    while (i++ < config.NB_KEYS) {
      key = testReader.readLine();
      if (key == null) {
        System.err.println("Incorrect file format, null key " + i);
        return false;
      }
      if (key.split(" ").length != 1) {
        System.err.println("Incorrect file format " + key);
        return false;
      }
    }
    return true;
  }

  /**
   * Loads keys from file, and ensures that all loaded keys are of the correct format
   */
  public void loadKeysFromFile(String fileName)
      throws FileNotFoundException, IOException {

    FileReader file = new FileReader(fileName);
    BufferedReader testReader = new BufferedReader(file);
    String key;

    key = testReader.readLine();
    while (key != null) {
      assert (!key.equals(null) && !key.equals(""));
      keys.add(key);
      key = testReader.readLine();
    }

    if (keys.size() != config.NB_KEYS
        || keys.get(0).length() != config.KEY_SIZE) {
      System.err.println("Inconsistent data with parameters");
    }

    testReader.close();
  }

  /**
   * Based on the configuration file, initializes the appropriate random generator. These generators
   * are directly taken from the YCSB benchmark
   */
  public void setupGenerator() {
    System.out.println("Setting up generator");
    switch (config.WORKLOAD_TYPE) {
      case UNIFORM:
        System.out.println("Uniform");
        ycsbRandom = new UniformIntegerGenerator(0, config.NB_KEYS - 1);
        break;
      case LATEST:
        ycsbRandom = new SkewedLatestGenerator(new CounterGenerator(0));
        break;
      case ZIPFIAN:
        ycsbRandom = new ZipfianGenerator(0, config.NB_KEYS - 1);
        break;
      case HOTSPOT:
        ycsbRandom = new HotspotIntegerGenerator(0, config.NB_KEYS - 1,
            config.WORKLOAD_HOTSPOT_FRAC, 1 - config.WORKLOAD_HOTSPOT_FRAC);
        break;
      default:
        throw new RuntimeException("Incorrect Workload Type");
    }
  }

  /**
   * Generate the required number of values of size config.VALUE_SIZE
   */
  public void generateValues() {
    System.out.println("Generating values ... ");
    for (int i = 0; i < config.NB_VALUES; i++) {
      values.add(Generator.generateBytes(config.VALUE_SIZE));
    }
  }

  public Pair<String, Boolean> generateOperation() {
    String key;
    int readRatio = (int) (config.READ_RATIO * 100);
    int gen = random.nextInt(100);
    key = keys.get(ycsbRandom.nextValue().intValue());
    if (gen < readRatio) {
      return new Pair<String, Boolean>(key, true);
    } else {
      return new Pair<String, Boolean>(key, false);
    }
  }

  public Set<String> generateOperations(int size) {
      HashSet<String> ks = new HashSet<>();
      while (ks.size()!=size) {
          ks.add(keys.get(ycsbRandom.nextValue().intValue()));
      }
      return ks;
  }



  /**
   * Generate a transaction of the specific size, number of reads, accessing data according to the
   * specified workload access pattern
   *
   * @return the generated transaction
   */
  public ClientTransaction generateTransaction() throws DatabaseAbortException {

    int gen = 0;
    int readRatio = (int) (config.READ_RATIO);

    String key = null;
    byte[] value = null;

    Set<String> readKeys = new HashSet<String>();
    Set<String> writeKeys = new HashSet<String>();

    ClientTransaction t = new ClientTransaction();

    for (int i = 0; i < config.TRX_SIZE; i++) {
      gen = random.nextInt(100);
      if (gen < readRatio) {
        key = keys.get(ycsbRandom.nextValue().intValue());
        while (readKeys.contains(key) || key == null) {
          key = keys.get(ycsbRandom.nextValue().intValue());
        }
        readKeys.add(key);
        t.addRead(key);
      } else {
        key = keys.get(ycsbRandom.nextValue().intValue());
        while (writeKeys.contains(key) || key == null) {
          key = keys.get(ycsbRandom.nextValue().intValue());
        }
        value = values.get(random.nextInt(config.NB_VALUES));
        t.addWrite(key, value);
        writeKeys.add(key);
      }

    }

    return t;
  }

  /**
   * Generate the pre-required number of transactions
   */
  public void generateTransactions() throws DatabaseAbortException {
    System.out.println("Generating transactions ... ");
    for (int i = 0; i < config.NB_TRANSACTIONS; i++) {
      genTransactions.add(generateTransaction());
    }
  }

  /**
   * Selects a random transaction from pre-generated transaction
   *
   * @return the randomly generated transaction
   */
  public ClientTransaction pickRandomTransaction() {
    int trxIndex = random.nextInt(config.NB_TRANSACTIONS);
    return genTransactions.get(trxIndex);
  }


  public byte[] getValue() {
    return values.get(0);
  }
}
