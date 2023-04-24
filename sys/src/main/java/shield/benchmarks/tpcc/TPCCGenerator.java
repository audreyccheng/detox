package shield.benchmarks.tpcc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import shield.benchmarks.tpcc.utils.TPCCConstants;
import shield.benchmarks.tpcc.utils.TPCCConstants.Transactions;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.benchmarks.utils.TrxStats;
import shield.client.RedisPostgresClient;
import shield.client.schema.ColumnInfo;

public class TPCCGenerator {

  private final TPCCExperimentConfiguration config;

  private final char[] TPCC_ALPHANUM =  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
  private final int C_255_ = 128;
  private final int C_1023_ = 512;
  private final int C_8191_ = 4096;
  private Integer  nextHistoryKey = 0;
  private final RedisPostgresClient client;
  public final static String[] kCustomerSyllables = {
    "BAR", "OUGHT", "ABLE", "PRI", "PRES", "ESE", "ANTI", "CALLY", "ATION", "EING"
  };
  private static long txn_id = 0;

  public String[] kCustomerNames;

  private final HashMap<Transactions, TrxStats> trxStats;


  public TPCCGenerator(RedisPostgresClient client, TPCCExperimentConfiguration config) {
    this.config = config;
    this.client = client;
    this.trxStats = new HashMap<>();
    this.kCustomerNames = null;
    for (TPCCConstants.Transactions tType: TPCCConstants.Transactions.values()) {
      trxStats.put(tType, new TrxStats());
    }
    createTPCCTables(client);
    generateCustomerNames();
  }

  private void generateCustomerNames() {
    kCustomerNames = new String[config.NB_CUSTOMERS];
    // Seed it to ensure deterministic.
    Random r = new Random(0);
    int customers = config.NB_CUSTOMERS/3;
    int nextCust = 0;
    for (int i = 0 ; i < customers ; i++) {
      kCustomerNames[nextCust++] = genNameFromNumber(i);
      System.out.println("Customer " + kCustomerNames[nextCust-1]);
    }
    for (int i = customers ; i < config.NB_CUSTOMERS; i++) {
      kCustomerNames[nextCust++] = genNameFromNumber(NURand(C_255_,0,999));
      System.out.println("Customer " + kCustomerNames[nextCust-1]);
    }
  }

  private String genNameFromNumber(int number) {
    StringBuilder b = new StringBuilder();
    System.out.println(number + " " + number/100 + " "
        + number/10 %10 + " " + number % 10);
    assert(number>=0 && number <= 999);
    b.append(kCustomerSyllables[number/100]);
    b.append(kCustomerSyllables[number/10 % 10]);
    b.append(kCustomerSyllables[number % 10]);
    return b.toString();
  }

  public  NewOrderTransaction GenerateNewOrderInput() {

    int wid;
    int did;
    int cid;
    int olCnt;
    SortedSet<Integer> iids= new TreeSet<>();
    List<Integer> swids = new LinkedList<>();
    List<Integer> olQuantities = new LinkedList<>();

    wid = Generator.generateInt(0, config.NB_WAREHOUSES - 1);
    did = Generator.generateInt(0, config.NB_DISTRICTS - 1);

    // TODO(Natacha): it seems to be that tpc-c oltp uses different
    // constants for C_ID_C, we need to check that they are actually
    // the same and conform to the spec)
    cid = NURand(1023, 0, config.NB_CUSTOMERS- 1);
    olCnt = Generator.generateInt(5, 15);

    boolean rollback = Generator.generateInt(0,100) % 100 == 0;

    // generate a list of item id
    // currently we don't allow repeated items in an order
    // TODO(natacha): check that this is standard TPC-C
    while (olCnt > iids.size()) {
      int iid = NURand(8191, 0, config.NB_ITEMS - 1);
      iids.add(iid);
    }

   if (rollback) {
      // Generating an invalid item id so that the item
      // won't be found, causing the transaction to abort
     //  iids.remove(iids.last());
     // iids.add(config.NB_ITEMS);
   }

    // Generate item warehouse and quantity
    for (int i = 0; i < olCnt; i++) {
      olQuantities.add(Generator.generateInt(1, 10));
      int swid;
      // 10% of remote transactions, so 10 * 1/100 = 10%
      int olSupplyWidRand = Generator.generateInt(0,100);
      // Unless we partition by wid, everything will be
      // remote
      if (olSupplyWidRand!= 0) {
        // use local warehouse
        swid = wid;
      } else {
        // use remote warehouse
        do {
          swid = Generator.generateInt(0,config.NB_WAREHOUSES - 1);
        } while (swid == wid && config.NB_WAREHOUSES > 1);
      }
      swids.add(swid);

    }
    return new NewOrderTransaction(this, wid,did,cid,olCnt,iids,swids,olQuantities,txn_id++);

  }

  public PaymentTransaction GeneratePaymentInput() {

    int cWid;
    int cDid;
    int hAmount;
    boolean scanByLastName;
    int cid;
    String cName = null;
    int wid = Generator.generateInt(0, config.NB_WAREHOUSES - 1);
    int did = Generator.generateInt(0, config.NB_DISTRICTS - 1);

    scanByLastName =
        Generator.generateInt(0,100) < config.PERCENT_ACCESS_LAST_NAME? true: false;

    if (!scanByLastName) {
      cid = NURand(1023, 0, config.NB_CUSTOMERS - 1);
    } else {
      // Generate a random name among the ones that were geernate
      cid = -1 ;
      cName = kCustomerNames[NURand(255,0,config.NB_CUSTOMERS/3)];
    }

   //TODO(natacha): check, not sure i understand
  int r = Generator.generateInt(0,100) ;
  if (r < 85) {
    cWid = wid;
    cDid = did;
    } else {
    // use remote warehouse
    do {
      cWid = Generator.generateInt(0, config.NB_WAREHOUSES - 1);
    } while (cWid == wid &&
        // if there is a single warehouse,
        // not possible to choose another one
        config.NB_WAREHOUSES > 1);
    cDid = Generator.generateInt(0, config.NB_DISTRICTS - 1);
  }
   hAmount = Generator.generateInt(1, 500000);  // multiplied by 100

    return new PaymentTransaction(this,cWid, cDid, cid, wid, did, hAmount,
        scanByLastName, cName, txn_id++);
  }

  public OrderStatusTransaction GenerateOrderStatusInput() {
    // due to lack of support for scan queries,
    // currently we omit the case of selecting customer by last name
  int wid = Generator.generateInt(0, config.NB_WAREHOUSES - 1);
  int did = Generator.generateInt(0, config.NB_DISTRICTS - 1);
  int cid = -1;
  String cName = null;
  boolean scanByLastName =
        Generator.generateInt(0,100) < config.PERCENT_ACCESS_LAST_NAME? true: false;

    if (!scanByLastName) {
      cid = NURand(1023, 0, config.NB_CUSTOMERS - 1);
    } else {
      // Generate a random name among the ones that were geernate
      cid = -1 ;
      cName = kCustomerNames[NURand(255,0,config.NB_CUSTOMERS/3)];
    }
    return new OrderStatusTransaction(this, wid,cid,did,scanByLastName, cName,txn_id++);
  }

  public DeliveryTransaction GenerateDeliveryInput() {
  int wid = Generator.generateInt(0, config.NB_WAREHOUSES- 1);
  int carrierId = Generator.generateInt(1, 10);
  return new DeliveryTransaction(this, wid,carrierId,0,txn_id++);
  }

  StockLevelTransaction GenerateStockLevelInput() {

  int wid = Generator.generateInt(0, config.NB_WAREHOUSES- 1);
  int did = Generator.generateInt(0, config.NB_DISTRICTS- 1);

  return new StockLevelTransaction(this, wid,did,txn_id++);
  }

  private void createTPCCTables(RedisPostgresClient client) {

      client.createTable(TPCCConstants.kWarehouseTable,
          new ColumnInfo("W_ID", Integer.class),
          new ColumnInfo("W_NAME", String.class, 10),
          new ColumnInfo("W_STREET_1", String.class, 20),
          new ColumnInfo("W_STREET_2", String.class, 20),
          new ColumnInfo("W_CITY", String.class, 20),
          new ColumnInfo("W_STATE", String.class, 2),
          new ColumnInfo("W_ZIP", String.class, 9),
          new ColumnInfo("W_TAX", Integer.class),
          new ColumnInfo("W_YTD", Integer.class));

      client.createTable(TPCCConstants.kDistrictTable,
          new ColumnInfo("D_ID", Integer.class),
          new ColumnInfo("D_W_ID", Integer.class),
          new ColumnInfo("D_NAME", String.class, 10),
          new ColumnInfo("D_STREET_1", String.class, 20),
          new ColumnInfo("D_STREET_2", String.class, 20),
          new ColumnInfo("D_CITY", String.class, 20),
          new ColumnInfo("D_STATE", String.class, 2),
          new ColumnInfo("D_ZIP", String.class, 9),
          new ColumnInfo("D_TAX", Integer.class),
          new ColumnInfo("D_YTD", Integer.class),
          new ColumnInfo("D_NEXT_O_ID", Integer.class)
          );

      client.createTable(TPCCConstants.kCustomerTable,
          new ColumnInfo("C_ID", Integer.class),
          new ColumnInfo("C_D_ID", Integer.class),
          new ColumnInfo("C_W_ID", Integer.class),
          new ColumnInfo("C_FIRST", String.class, 16),
          new ColumnInfo("C_MIDDLE", String.class, 2),
          new ColumnInfo("C_LAST", String.class, 16),
          new ColumnInfo("C_STREET_1", String.class, 20),
          new ColumnInfo("C_STREET_2", String.class, 20),
          new ColumnInfo("C_CITY", String.class, 20),
          new ColumnInfo("C_STATE", String.class, 2),
          new ColumnInfo("C_ZIP", String.class, 9),
          new ColumnInfo("C_PHONE", String.class, 16),
          new ColumnInfo("C_SINCE", String.class, 20),
          new ColumnInfo("C_CREDIT", String.class, 2),
          new ColumnInfo("C_CREDIT_LIM", Integer.class),
          new ColumnInfo("C_DISCOUNT", Integer.class),
          new ColumnInfo("C_BALANCE", Integer.class),
          new ColumnInfo("C_YTD_PAYMENT", Integer.class),
          new ColumnInfo("C_PAYMENT_CNT", Integer.class),
          new ColumnInfo("C_DELIVERY_CNT", Integer.class),
          new ColumnInfo("C_DATA", String.class, 500));

      client.createTable(TPCCConstants.kHistoryTable,
          new ColumnInfo("H_C_ID", Integer.class),
          new ColumnInfo("H_C_D_ID", Integer.class),
          new ColumnInfo("H_C_W_ID", Integer.class),
          new ColumnInfo("H_D_ID", Integer.class),
          new ColumnInfo("H_W_ID", Integer.class),
          new ColumnInfo("H_DATE", String.class, 20),
          new ColumnInfo("H_AMOUNT", Integer.class),
          new ColumnInfo("H_DATA", String.class, 24));

      client.createTable(TPCCConstants.kNewOrderTable,
          new ColumnInfo("NO_O_ID", Integer.class),
          new ColumnInfo("NO_D_ID", Integer.class),
          new ColumnInfo("NO_W_ID", Integer.class));

      client.createTable(TPCCConstants.kOrderTable,
          new ColumnInfo("O_ID", Integer.class),
          new ColumnInfo("O_D_ID", Integer.class),
          new ColumnInfo("O_W_ID", Integer.class),
          new ColumnInfo("O_C_ID", Integer.class),
          new ColumnInfo("O_ENTRY_D", String.class, 20),
          new ColumnInfo("O_CARRIER_ID", Integer.class),
          new ColumnInfo("O_OL_CNT", Integer.class),
          new ColumnInfo("O_ALL_LOCAL", Integer.class));

      client.createTable(TPCCConstants.kOrderLineTable,
          new ColumnInfo("OL_O_ID", Integer.class),
          new ColumnInfo("OL_D_ID", Integer.class),
          new ColumnInfo("OL_W_ID", Integer.class),
          new ColumnInfo("OL_NUMBER", Integer.class),
          new ColumnInfo("OL_I_ID", Integer.class),
          new ColumnInfo("OL_SUPPLY_W_ID", Integer.class),
          new ColumnInfo("OL_DELIVERY_D", String.class, 20),
          new ColumnInfo("OL_QUANTITY", Integer.class),
          new ColumnInfo("OL_AMOUNT", Integer.class),
          new ColumnInfo("OL_DIST_INFO", String.class, 24));

      client.createTable(TPCCConstants.kItemTable,
          new ColumnInfo("I_ID", Integer.class),
          new ColumnInfo("I_IM_ID",Integer.class),
          new ColumnInfo("I_NAME", String.class, 24),
          new ColumnInfo("I_PRICE", Integer.class),
          new ColumnInfo("I_DATA", String.class, 50));

      client.createTable(TPCCConstants.kStockTable,
          new ColumnInfo("S_I_ID", Integer.class),
          new ColumnInfo("S_W_ID", Integer.class),
          new ColumnInfo("S_QUANTITY", Integer.class),
          new ColumnInfo("S_DIST_01", String.class, 24),
          new ColumnInfo("S_DIST_02", String.class, 24),
          new ColumnInfo("S_DIST_03", String.class, 24),
          new ColumnInfo("S_DIST_04", String.class, 24),
          new ColumnInfo("S_DIST_05", String.class, 24),
          new ColumnInfo("S_DIST_06", String.class, 24),
          new ColumnInfo("S_DIST_07", String.class, 24),
          new ColumnInfo("S_DIST_08", String.class, 24),
          new ColumnInfo("S_DIST_09", String.class, 24),
          new ColumnInfo("S_DIST_10", String.class, 24),
          new ColumnInfo("S_YTD", Integer.class),
          new ColumnInfo("S_ORDER_CNT", Integer.class),
          new ColumnInfo("S_REMOTE_CNT", Integer.class),
          new ColumnInfo("S_DATA", String.class, 50));

      client.createTable(TPCCConstants.kEarliestNewOrderTable,
          new ColumnInfo("?", Integer.class),
          new ColumnInfo("?", Integer.class),
          new ColumnInfo("?", Integer.class));

      client.createTable(TPCCConstants.kOrderByCustomerTable,
          new ColumnInfo("?", Integer.class));

      client.createTable(TPCCConstants.kCustomerByNameTable,
          new ColumnInfo("?",String.class,704));

  }

  /**
   * Stores next history key. Because of ORAM limitations, only
   * store a subset of the most recent history entries
   *
   * @return
   */
  public Integer GetNextHistoryKey() {
    Integer res = nextHistoryKey;
    nextHistoryKey+= config.NB_CUSTOMERS;
    nextHistoryKey = nextHistoryKey % 60000;
    return res;
  }



  public int NURand(int A, int x, int y) {
    int C = 0;
    if (A == 255) {
      C = C_255_;
    } else if (A == 1023) {
      C = C_1023_;
    } else if (A == 8191) {
      C = C_8191_;
    } else {
      C = A;
      // assert(false);
    }

    return (((Generator.generateInt(0, A) | Generator.generateInt(x, y)) + C) % (y - x + 1)) + x;
  }

/**
 * Adds to a list of elements. Assertion: element does not contain
 * any "-" characters (assumes this holds). Each element in a list
 * is separated by -
 * @throws Exception if element contains a "-"
 * @return  a list of the new element appended
 */

 public String addToList(byte[] currentList, String element) throws Exception {
    if (element.contains("-")) throw new Exception();
    else {
      StringBuilder b = new StringBuilder();
      b.append(currentList);
      b.append("-");
      b.append(element);
      return b.toString();
    }
 }

  /**
   * Returns the current count of elements in a list
   * @return
   */
 public int getNbElements(byte[] currentList) {
  return getNbElements(new String(currentList));
 }

  /**
   * Returns the current count of elements in a list
   * @return
   */
 public int getNbElements(String currentList) {
  return currentList.split("-").length;
 }

 /**
   * @return the element at index i of the list
   * @throws IndexOutOfBoundsException if i is > number of elements in list
   */
 public String getElementAtIndex(String currentList, int i) throws IndexOutOfBoundsException {
   String[] elements = currentList.split("-");
   if (elements.length < i) {
      return elements[i];
   } else {
      throw new IndexOutOfBoundsException();
   }
 }

  /**
   * @return the element at index i of the list
   * @throws IndexOutOfBoundsException if i is > number of elements in list
   */
 public String getElementAtIndex(byte[] currentList, int i) throws IndexOutOfBoundsException {
    return getElementAtIndex(new String(currentList), i);
 }

    public HashMap<Transactions, TrxStats> getTrxStats() {
        return trxStats;
    }

  public char RandCharNum(boolean num_only) {
    int x = Generator.generateInt(0,num_only? 10: 61);
    return TPCC_ALPHANUM[x];
  }

  /**
   * Generates a random string of size between min and max, and optinally consisting
   * of numbers only
   *
   * @param num_only
   * @return
   */
  public String RandString(int min, int max, boolean num_only) {
    StringBuffer bf = new StringBuffer();
    int len = Generator.generateInt(min, max);
    for (int i = 0; i < len; ++i) {
      bf.append(RandCharNum(num_only));
    }
    return bf.toString();
  }

  public String RandZipCode() {
    StringBuffer bf;
    String s = RandString(4, 5, true);
    return s.concat("11111");
  }

  public String getTime() {
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    return dateFormat.format(date);
  }

  public RedisPostgresClient getClient() {
        return client;
    }

  public TPCCExperimentConfiguration getConfig() {
    return config;
  }

  public void runNextTransaction() {
      long begin = System.currentTimeMillis();
      int x = Generator.generateInt(0,100);
      int nbAborts;
      long end = 0;
      BenchmarkTransaction trx;
      if (x < config.PROB_TRX_PAYMENT) {
        trx = GeneratePaymentInput();
//        System.out.println("[" + Transactions.PAYMENT + "] Begin");
        nbAborts = trx.run();
        end = System.currentTimeMillis();
         trxStats.get(Transactions.PAYMENT).addTransaction(nbAborts, end-begin);
//        System.out.println("[" + Transactions.PAYMENT + "] End");
        }
      else if (x <= config.PROB_TRX_PAYMENT + config.PROB_TRX_NEW_ORDER) {
        trx = GenerateNewOrderInput();
//        System.out.println("[" + Transactions.NEW_ORDER+ "] Begin");
         nbAborts = trx.run();
        end = System.currentTimeMillis();
//         System.out.println("[" + Transactions.NEW_ORDER+ "] End");
         trxStats.get(Transactions.NEW_ORDER).addTransaction(nbAborts,end-begin);
       } else if (x <= config.PROB_TRX_PAYMENT + config.PROB_TRX_NEW_ORDER +
          config.PROB_TRX_DELIVERY) {
        trx = GenerateDeliveryInput();
//        System.out.println("[" + Transactions.DELIVERY+ "] Begin");
         nbAborts = trx.run();
        end = System.currentTimeMillis();
//         System.out.println("[" + Transactions.DELIVERY+ "] End");
         trxStats.get(Transactions.DELIVERY).addTransaction(nbAborts,end-begin);
       } else if (x <= config.PROB_TRX_PAYMENT + config.PROB_TRX_NEW_ORDER +
          config.PROB_TRX_DELIVERY + config.PROB_TRX_ORDER_STATUS){
        trx = GenerateOrderStatusInput();
//        System.out.println("[" + Transactions.ORDER_STATUS+ "] Begin");
         nbAborts = trx.run();
        end = System.currentTimeMillis();
//         System.out.println("[" + Transactions.ORDER_STATUS+ "] End");
         trxStats.get(Transactions.ORDER_STATUS).addTransaction(nbAborts,end-begin);
       } else {
        trx = GenerateStockLevelInput();
//        System.out.println("[" + Transactions.STOCK_LEVEL+ "] Begin");
        nbAborts = trx.run();
        end = System.currentTimeMillis();
//        System.out.println("[" + Transactions.STOCK_LEVEL+ "] End");
        trxStats.get(Transactions.STOCK_LEVEL).addTransaction(nbAborts,end-begin);
      }
//      System.out.println("Executed in: " + (end-begin));
  }

  public void printStats() {
    trxStats.forEach((tType,stat) -> System.out.println("[STAT] " + tType + " " +  stat.getStats()));
  }


}
