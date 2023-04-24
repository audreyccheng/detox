package shield.benchmarks.tpcc.utils;


public class TPCCConstants {

  /**
  * List of all possible transactions
  */
 public enum Transactions {
        DELIVERY,
        NEW_ORDER,
        ORDER_STATUS,
        PAYMENT,
        STOCK_LEVEL,
        LOAD
  }

// Names of the various tables and number of columns
public static String kWarehouseTable = "warehouse";
public static int kWarehouseCols = 9;
public static String kDistrictTable = "district";
public static int kDistrictCols = 11;
public static String kCustomerTable = "customer";
public static int kCustomerCols = 21;
public static String kCustomerByNameTable = "customerbyname";
public static int kCustomerByNameCols = 1;
public static String kHistoryTable = "history";
public static int kHistoryCols = 8;
public static String kNewOrderTable = "neworder";
public static int kNewOrderCols = 3;
public static String kEarliestNewOrderTable = "earliestneworder";
public static int kEarliestNewOrderCols = 3;
public static String kOrderTable = "orders";
public static int kOrderCols = 8;
public static String kOrderByCustomerTable = "orderbycust";
public static int kOrderByCustomerCols = 1;
public static String kOrderLineTable = "orderline";
public static int kOrderLineCols = 10;
public static String kItemTable = "item";
public static int kItemCols = 5;
public static String kStockTable = "stock";
public static int kStockCols = 17;
public static String kItemStatsTable = "itemstats";
public static int kItemStatsCols = 1;









}
