package shield.benchmarks.tpcc;

import static shield.benchmarks.utils.Generator.generatePortNumber;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.json.simple.parser.ParseException;
import shield.benchmarks.tpcc.utils.TPCCConstants;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.Generator;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;
import shield.util.CallStack;

public class TPCCLoader {

  public static void main(String[] args) throws IOException, ParseException,
      DatabaseAbortException, InterruptedException {
    String expConfigFile;
    TPCCExperimentConfiguration tpccConfig;

    if (args.length != 1) {
      System.out.println(args.length);
      System.out.println(args[0]);
      System.out.println(args[1]);
      System.err.println(
          "Incorrect number of arguments: expected <clientConfigFile.json expConfigFile.json>");
    }
    // Contains the experiment parameters
    expConfigFile = args[0];
    System.err.println(expConfigFile);
    tpccConfig = new TPCCExperimentConfiguration(expConfigFile);

    System.out.println("Number of loader threads " + tpccConfig.NB_LOADER_THREADS);

    if (tpccConfig.MUST_LOAD_KEYS) {
      System.out.println("Begin loading data");
      loadData(tpccConfig, expConfigFile);
    }

    System.out.println("Data loaded");
    System.exit(0);

  }

  private static void loadData(TPCCExperimentConfiguration tpccConfig, String expConfigFile)
      throws InterruptedException, IOException, ParseException {

    // First load data
    int ranges = tpccConfig.NB_WAREHOUSES / tpccConfig.NB_LOADER_THREADS;
    System.out.println(tpccConfig.NB_WAREHOUSES + " " + tpccConfig.NB_LOADER_THREADS
        + " " + ranges);

    int warehousesToLoad = ranges > 0 ? ranges : 1;
    List<Thread> threads = new LinkedList<Thread>();

    // Pre initialise set of ports to avoid risk of duplicates
    Set<Integer> ports = new HashSet<>();
    while (ports.size() < tpccConfig.NB_WAREHOUSES * 1000) {
      ports.add(generatePortNumber());
    }


    System.out.println("Loading [WAREHOUSES]");
    Iterator<Integer> it = ports.iterator();
    for (int i = 0; i < tpccConfig.NB_WAREHOUSES; i += warehousesToLoad) {
      System.out.println(i + " " + tpccConfig.NB_WAREHOUSES + " " + warehousesToLoad);
      final int j = i;
      // The warehouse function spans off 4 new threads
      List<Integer> subPorts = new LinkedList<>();
      for (int h = 0; h < 1000; h++) {
        subPorts.add(it.next());
      }
      Thread t = new Thread() {
        public void run() {
          try {
            int endWarehouse =
                ((j + warehousesToLoad) > tpccConfig.NB_WAREHOUSES) ? tpccConfig.NB_WAREHOUSES :
                    (j + warehousesToLoad);
            loadWarehouses(j, endWarehouse, subPorts, expConfigFile);
          } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.err.println("Loading failed");
            System.exit(-1);
          }
        }
      };
      threads.add(t);
      t.start();
    }

    // Second, load items
    // Pre initialise set of ports to avoid risk of duplicates


    System.out.println("Loading [ITEMS]");
    HashSet<Integer> oldPorts = new HashSet(ports);
    ports = new HashSet<>();
    while (ports.size() < tpccConfig.NB_LOADER_THREADS * 50) {
      int port = generatePortNumber();
      if (!oldPorts.contains(port)) {
        ports.add(port);
      }
    }

    it = ports.iterator();
    ranges = tpccConfig.NB_ITEMS / tpccConfig.NB_LOADER_THREADS;
    int itemsToLoad = ranges > 0 ? ranges : 1;
    for (int i = 0; i < tpccConfig.NB_ITEMS; i += itemsToLoad) {
      final int j = i;
      int port = it.next();
      Thread t = new Thread() {
        public void run() {
          try {
            int endItems =
                ((j + itemsToLoad) > tpccConfig.NB_ITEMS) ? tpccConfig.NB_ITEMS :
                    (j + itemsToLoad);
            loadItemTable(j, endItems, port, expConfigFile);
          } catch (Exception e) {
            System.err.println(e);
            System.err.println("Loading failed");
            System.exit(-1);
          }
        }
      };
      threads.add(t);
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }

    System.out.println("Finished Items");
  }

  private static void loadItemTable(int start, int end, int uid, String expConfigFile)
      throws InterruptedException, ParseException, IOException, DatabaseAbortException, SQLException {

    TPCCExperimentConfiguration config = new TPCCExperimentConfiguration(expConfigFile);
    RedisPostgresClient client = (RedisPostgresClient) ClientUtils.createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

    client.registerClient();
    TPCCGenerator generator = new TPCCGenerator(client, config);

    System.out.println("Loading Items: " + start + " to " + end + " Port " + uid);

    ItemKey iKey;
    Table itemTable = client.getTable(TPCCConstants.kItemTable);
    boolean success;
    byte[] row;

    while (start < end) {
      success = false;
      int oldStart = start;
      while (!success) {
        start = oldStart;
        try {
          client.startTransaction();
          for (int ij = 0; ij < 10; ij++) {
            System.out.println(start + " " + ij + " " + end);
            start = oldStart + ij;
            if (start >= end) {
              break;
            }
            int iid = start;
            row = itemTable.createNewRow(config.PAD_COLUMNS);
            System.out.println("[Item] " + iid);
            iKey = new ItemKey(iid);
            itemTable.updateColumn(0, iid, row); //I_ID
            itemTable.updateColumn(1, Generator.generateInt(1, 1000), row); // IMG_ID
            String name = generator.RandString(14, 24, false);
            itemTable.updateColumn(2, name, row);
            // Item price multipled by 100
            // TPC spec requires 10% of item contains ORIGINAL substring.
            // Ignore this as is only used to determine screen output
            itemTable.updateColumn(3, Generator.generateInt(100, 10000), row);
            String dat = generator.RandString(26, 50, false);
            itemTable.updateColumn(4, dat, row);
            ((RedisPostgresClient) client).write(TPCCConstants.kItemTable, iKey.str(), row,
                    TPCCConstants.Transactions.LOAD.ordinal(), 0);
          }
          client.commitTransaction();
          success = true;
        } catch (DatabaseAbortException e) {
          System.out.println("ERROR");
          success = false;
        }
      }
    }
  }


  private static void loadWarehouses(int start, int end, List<Integer> uid, String expConfigFile)
      throws InterruptedException,
      IOException, DatabaseAbortException, ParseException, SQLException {

    LinkedList<Thread> threads = new LinkedList<>();
    int portIndex = 0;

    TPCCExperimentConfiguration config = new TPCCExperimentConfiguration(expConfigFile);
    RedisPostgresClient client = (RedisPostgresClient) ClientUtils
            .createClient(config.CLIENT_TYPE, expConfigFile, uid.get(portIndex), uid.get(portIndex++));

    System.out.println("Loading Warehouses: " + start + " to " + end + " " + portIndex);

    assert (end <= config.NB_WAREHOUSES);

    client.registerClient();

    TPCCGenerator generator = new TPCCGenerator(client, config);

    System.out.println("Client registered " + start + " " + end);

    boolean success = false;
    byte[] row;
    WarehouseKey wKey;
    DistrictKey dKey;
    EarliestNewOrderKey eolKey;

    Table warehouseTable = client.getTable(TPCCConstants.kWarehouseTable);
    Table districtTable = client.getTable(TPCCConstants.kDistrictTable);
    Table customerTable = client.getTable(TPCCConstants.kCustomerTable);
    Table customerByNameTable = client.getTable(TPCCConstants.kCustomerByNameTable);
    Table historyTable = client.getTable(TPCCConstants.kHistoryTable);
    Table stockTable = client.getTable(TPCCConstants.kStockTable);
    Table orderTable = client.getTable(TPCCConstants.kOrderTable);
    Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);
    Table orderByCustTable = client.getTable(TPCCConstants.kOrderByCustomerTable);
    Table earliestNewOrderTable = client.getTable(TPCCConstants.kEarliestNewOrderTable);
    Table newOrderTable = client.getTable(TPCCConstants.kNewOrderTable);

    for (int wid_ = start; wid_ < end; wid_++) {

      final int wid = wid_;
      System.out.println("Load Warehouse " + wid);

      wKey = new WarehouseKey(wid);
      System.out.println("[Warehouse]" + wid);
      row = warehouseTable.createNewRow(config.PAD_COLUMNS);
      warehouseTable.updateColumn(0, wid, row);
      String name = generator.RandString(6, 10, false);
      warehouseTable.updateColumn(1, name, row);
      String street1 = generator.RandString(10, 20, false);
      warehouseTable.updateColumn(2, street1, row);
      String street2 = generator.RandString(10, 20, false);
      warehouseTable.updateColumn(3, street2, row);
      String city = generator.RandString(10, 20, false);
      warehouseTable.updateColumn(4, city, row);
      String state = generator.RandString(2, 2, false);
      warehouseTable.updateColumn(5, state, row);
      String zip = generator.RandZipCode();
      warehouseTable.updateColumn(6, zip, row);
      int tax = Generator.generateInt(0, 2000); // tax, multiplied by 10000 to get int
      warehouseTable.updateColumn(7, tax, row);
      warehouseTable.updateColumn(8, 3000000, row); // W_YTD, all money multipled by 100 to get int

      while (!success) {
        try {
          System.out.println("[Wid]: " + wid);
          client.startTransaction();
          ((RedisPostgresClient) client).write(TPCCConstants.kWarehouseTable, wKey.str(), row,
                  TPCCConstants.Transactions.LOAD.ordinal(), 0);
          System.out.println("Commit Transaction");
          client.commitTransaction();
          success = true;
          System.out.println("Successful");
        } catch (DatabaseAbortException e) {
          System.out.println("Warehouse Aborted");
          System.out.println(e.getMessage());
          success = false;
        } catch (Exception e) {
          e.printStackTrace();;
          System.exit(-1);
        }
      }



      // Next, identity the next delivery id and load district
      success = false;
      System.out.println("DISTRICT TABLE " + wid);
      while (!success) {
        try {
          client.startTransaction();
          for (int did = 0; did < config.NB_DISTRICTS; did++) {
            System.out.println("[District] " + wid + " " + did);
            dKey = new DistrictKey(wid, did);
            row = districtTable.createNewRow(config.PAD_COLUMNS);
            districtTable.updateColumn(0, did, row);
            districtTable.updateColumn(1, wid, row);
            name = generator.RandString(6, 10, false);
            districtTable.updateColumn(2, name, row);
            street1 = generator.RandString(10, 20, false);
            districtTable.updateColumn(3, street1, row);
            street2 = generator.RandString(10, 20, false);
            districtTable.updateColumn(4, street2, row);
            city = generator.RandString(10, 20, false);
            districtTable.updateColumn(5, city, row);
            state = generator.RandString(2, 2, false);
            districtTable.updateColumn(6, state, row);
            zip = generator.RandZipCode();
            districtTable.updateColumn(7, zip, row);
            tax = Generator.generateInt(0, 2000); // tax, multiplied by 10000 to get int
            districtTable.updateColumn(8, tax, row);
            districtTable
                .updateColumn(9, 3000000, row); // W_YTD, all money multipled by 100 to get int
            districtTable.updateColumn(10, config.INIT_NEW_ORDER_NB, row);
            System.out.println("District Write " + wid + " " + did + " " + Arrays.toString(row));
            ((RedisPostgresClient) client).write(TPCCConstants.kDistrictTable, dKey.str(), row,
                    TPCCConstants.Transactions.LOAD.ordinal(), 0);
          }

          client.commitTransaction();
          success = true;
        } catch (DatabaseAbortException e) {
          System.out.println(e.getMessage());
          success = false;
        } catch (Exception e) {
          e.printStackTrace();;
          System.exit(-1);
        }
      }

      System.out.println("CUSTOMER/HISTORY TABLE " + wid);

      final int step = 300;
      for (int gcid = 0; gcid < config.NB_CUSTOMERS; gcid += step) {
        final int pt = portIndex++;
        final int gcid_ = gcid;
        System.out.println("Doing customers " + gcid + " " + (gcid + step));
        Thread customerThread = new Thread() {
          public void run() {
            // Write to Order Line/ New Order / Order By Customer
            RedisPostgresClient client = null;
            try {
              System.out.println("Customer Client " + uid.get(pt) + " registering");
               client = (RedisPostgresClient) ClientUtils
                  .createClient(config.CLIENT_TYPE, expConfigFile, uid.get(pt),
                      uid.get(pt));
              client.registerClient();
              System.out.println("Customer Client " + uid.get(pt) + " registered ");
            } catch (Exception e) {
              e.printStackTrace();
              System.exit(-1);
            }
            for (int cid = gcid_; cid < gcid_ + step; cid++) {
              for (int did = 0; did < config.NB_DISTRICTS; did++) {
                final int d = did;
                final int c = cid;

                boolean success = false;
                while (!success) {
                  try {
                    System.out.println("[Customer] " + wid + " " + d + " " + c);
                    CustomerKey cKey = new CustomerKey(wid, d, c);
                    client.startTransaction();
                    byte[] row = customerTable.createNewRow(config.PAD_COLUMNS);
                    customerTable.updateColumn(0, c, row);
                    customerTable.updateColumn(1, d, row);
                    customerTable.updateColumn(2, wid, row);
                    String first = generator.RandString(8, 16, false);
                    customerTable.updateColumn(3, first, row);
                    customerTable.updateColumn(4, "OE", row); // middle name
                    String last = generator.kCustomerNames[cid];
                   // String last = generator.RandString(9, 16, false);
                    customerTable.updateColumn(5, last, row);
                    String street1 = generator.RandString(10, 20, false);
                    customerTable.updateColumn(6, street1, row);
                    String street2 = generator.RandString(10, 20, false);
                    customerTable.updateColumn(7, street2, row);
                    String city = generator.RandString(10, 20, false);
                    customerTable.updateColumn(8, city, row);
                    String state = generator.RandString(2, 2, false);
                    customerTable.updateColumn(9, state, row);
                    String zip = generator.RandZipCode();
                    customerTable.updateColumn(10, zip, row);
                    String phone = generator.RandString(16, 16, false);
                    customerTable.updateColumn(11, phone, row);
                    String now = generator.getTime();
                    customerTable.updateColumn(12, now, row);
                    int r = Generator.generateInt(0, 10);
                    if (r == 0) {
                      customerTable.updateColumn(13, "BC", row); // credit
                    } else {
                      customerTable.updateColumn(13, "GC", row); // credit
                    }
                    customerTable.updateColumn(14, 5000000, row); // Credit
                    r = Generator.generateInt(0, 5000);
                    customerTable
                        .updateColumn(15, r, row); // discount, multipled by 1000 for integer
                    customerTable.updateColumn(16, -1000, row); // balance, multipled by 100
                    customerTable.updateColumn(17, 1000, row); // ytd payment, multipled by 100
                    customerTable.updateColumn(18, 1, row); // payment cnt
                    customerTable.updateColumn(19, 0, row); // delivery cnt
                    String data = generator.RandString(300, 500, false);
                    customerTable.updateColumn(20, data, row);
                    ((RedisPostgresClient) client).write(TPCCConstants.kCustomerTable, cKey.str(), row,
                            TPCCConstants.Transactions.LOAD.ordinal(), 0);

                    // Now need to update customer last name
                    CustomerByNameKey key = new CustomerByNameKey(wid,did,last);
                    List<byte[]> results =
                            ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kCustomerByNameTable, key.str(),
                                    TPCCConstants.Transactions.LOAD.ordinal(), 0);
                    String update = null;
                    row = results.get(0);
                    try {
                      update = generator.addToList(row, last);
                      System.out.println("[CUSTOMERLASTNAME] " +  generator.getNbElements(row));
                    } catch (Exception e) {
                      // Ignore: customer last name generated from three syllables only
                    }
                    row = row.length == 0? customerByNameTable.createNewRow(config.PAD_COLUMNS):row;
                    customerByNameTable.updateColumn(0,update,row);
                    ((RedisPostgresClient) client).write(TPCCConstants.kCustomerByNameTable, key.str(),row,
                            TPCCConstants.Transactions.LOAD.ordinal(), 0);

                    // Current we don't implement autoprimary key. generate unique key
                    row = historyTable.createNewRow(config.PAD_COLUMNS);
                    now = generator.getTime();
                    HistoryKey hKey = new HistoryKey(generator.GetNextHistoryKey());
                    historyTable.updateColumn(0, c, row);
                    historyTable.updateColumn(1, d, row);
                    historyTable.updateColumn(2, wid, row);
                    historyTable.updateColumn(3, d, row);
                    historyTable.updateColumn(4, wid, row);
                    historyTable.updateColumn(5, now, row);
                    historyTable.updateColumn(6, 1000, row); // h_amount
                    String hisData = generator.RandString(12, 24, false);
                    historyTable.updateColumn(7, hisData, row);
                    ((RedisPostgresClient) client).write(TPCCConstants.kHistoryTable, hKey.str(), row,
                            TPCCConstants.Transactions.LOAD.ordinal(), 0);
                    client.commitTransaction();
                    success = true;


                  } catch (DatabaseAbortException e) {
                    System.out.println(e.getMessage());
                    success = false;
                  } catch (Exception e) {
                    e.printStackTrace();;
                    System.exit(-1);
                  }
                }
                success = false;
              }
            }
          }
        };
        customerThread.start();
        ;
        threads.add(customerThread);
      }

      // Write to Stock Table
      System.out.println("STOCK TABLE " + wid);

      final int port = portIndex++;
      Thread stockThread = new Thread() {
        public void run() {
          int j = 0;
          int ij;
          RedisPostgresClient client = null;
          try {
            client = (RedisPostgresClient) ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid.get(port), uid.get(port));
            client.registerClient();
            System.out.println("Stock Client " + uid.get(port));
          } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
          }
          while (j < config.NB_ITEMS)

          {
            boolean success = false;
            while (!success) {
              try {
                ij = 0;
                client.startTransaction();
                while (ij < 20) {
                  int iid = j + ij;
                  if (iid >= config.NB_ITEMS) {
                    break;
                  }
                  System.out.println("[Stock] " + wid + " " + iid);
                  StockKey sKey = new StockKey(wid, iid);
                  byte[] row = stockTable.createNewRow(config.PAD_COLUMNS);
                  stockTable.updateColumn(0, iid, row); // s_i_id
                  stockTable.updateColumn(1, wid, row); // s_w_id
                  stockTable.updateColumn(2,
                      Generator.generateInt(10, 100), row); // s_quantity

                  for (int sDistLoop = 0; sDistLoop < 10; sDistLoop++) {
                    String sDist = generator.RandString(24, 24, false);
                    stockTable.updateColumn(3 + sDistLoop, sDist, row);
                  }

                  stockTable.updateColumn(13, 0, row); // s_ytd (multipled by 100, init = 0)
                  stockTable.updateColumn(14, 0, row); // s_order_cnt
                  stockTable.updateColumn(15, 0, row); // s_remote_cnt

                  // ignore the 10% ORIGINAL substring in tpc-c spec.
                  // it is onlky used to determine a screen-output in new-ord
                  // and we are omitting the real calculation of screen-output
                  String sData = generator.RandString(26, 50, false);
                  stockTable.updateColumn(16, sData, row); // s_data
                  ((RedisPostgresClient) client).write(TPCCConstants.kStockTable, sKey.str(), row,
                          TPCCConstants.Transactions.LOAD.ordinal(), 0);
                  ij++;
                }
                client.commitTransaction();
                success = true;
              } catch (DatabaseAbortException e) {
                System.out.println(e.getMessage());
                success = false;
              } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
              }
            }
            j += 20;
          }
        }
      };
      stockThread.start();
      threads.add(stockThread);

      // Write to Earliest New Order Table
      // We use this table as a walk around of the scan in delivery
      System.out.println("EARLIEST NEW ORDER TABLE " + wid);
      success = false;
      while (!success) {
        try {
          client.startTransaction();
          for (int j = 0; j < config.NB_DISTRICTS; j++) {
            eolKey = new EarliestNewOrderKey(wid, j);
            System.out.println("[EarliestNewOrder] " + wid + " " + j);
            row = earliestNewOrderTable.createNewRow(config.PAD_COLUMNS);
            earliestNewOrderTable.updateColumn(0, wid, row);
            earliestNewOrderTable.updateColumn(1, j, row);
            earliestNewOrderTable.updateColumn(2, (int) (config.INIT_NEW_ORDER_NB * 0.7), row);
            ((RedisPostgresClient) client).write(TPCCConstants.kEarliestNewOrderTable, eolKey.str(), row,
                    TPCCConstants.Transactions.LOAD.ordinal(), 0);
          }
          client.commitTransaction();
          success = true;
        } catch (DatabaseAbortException e) {
          System.out.println(e.getMessage());
          success = false;
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(-1);
        }
      }

      System.out.println("ORDERLINE/NEWORDER/ORDERBYCUSTOMER  TABLE" + wid);
      List<Thread> orderLines = new LinkedList<>();
      for (int did = 0; did < config.NB_DISTRICTS; did++) {
        final int d = did;
        final int p = portIndex++;
        Thread orderLineThread = new Thread() {
          public void run() {
            // Write to Order Line/ New Order / Order By Customer
            RedisPostgresClient client = null;
            try {
              client = (RedisPostgresClient) ClientUtils
                  .createClient(config.CLIENT_TYPE, expConfigFile, uid.get(p), uid.get(p));
              System.out.println("ORderByCustomer " + uid.get(p));
              client.registerClient();
            } catch (Exception e) {
              e.printStackTrace();
              System.exit(-1);
            }
            for (int oid = 0; oid < config.INIT_NEW_ORDER_NB; oid++) {
              boolean success = false;
              System.out.println("[OrderLine/NewOrder] wid " + wid + " did " + d + " oid " + oid);
              while (!success) {
                try {
                  client.startTransaction();
                  // TPC spec requires cid to be a permutation in each district.
                  // We use a special permutation, which is not permuted at all.
                  // It won't have a significant effect anyways
                  int cid = oid % config.NB_CUSTOMERS;
                  int olCnt = Generator.generateInt(5, 15);
                  int oCarrierId
                      = (oid < (config.INIT_NEW_ORDER_NB * 0.7)) ?
                      Generator.generateInt(1, 10) : 0;
                  int custOrders = 0;

                  // Write order - update scondary index.
                  // We use this as a walk-around to the scan in order-status
                  OrderByCustomerKey obcKey = new OrderByCustomerKey(wid, d, cid);
                  byte[] row = orderByCustTable.createNewRow(config.PAD_COLUMNS);
                  orderByCustTable.updateColumn(0, custOrders, row);
                  ((RedisPostgresClient) client).write(TPCCConstants.kOrderByCustomerTable, obcKey.str(), row,
                          TPCCConstants.Transactions.LOAD.ordinal(), 0);

                  // Write order
                  OrderKey oKey = new OrderKey(wid, d, oid);
                  row = orderTable.createNewRow(config.PAD_COLUMNS);
                  orderTable.updateColumn(0, oid, row);
                  orderTable.updateColumn(1, d, row);
                  orderTable.updateColumn(2, wid, row);
                  orderTable.updateColumn(3, cid, row);
                  orderTable.updateColumn(4, generator.getTime(), row);
                  orderTable.updateColumn(5, oCarrierId, row);
                  orderTable.updateColumn(6, olCnt, row);
                  orderTable.updateColumn(7, 1, row); // o_all_local
                  ((RedisPostgresClient) client).write(TPCCConstants.kOrderTable, oKey.str(), row,
                          TPCCConstants.Transactions.LOAD.ordinal(), 0);

                  // Write order-line
                  for (int olid = 0; olid < olCnt; olid++) {
                    int iid = Generator.generateInt(0, config.NB_ITEMS);
                    int olQuantity = 5;
                    int amount = (oid < config.INIT_NEW_ORDER_NB * 0.7) ? 0
                        : Generator.generateInt(0, 999999); // multipled by 100
                    OrderLineKey olKey = new OrderLineKey(wid, d, oid, olid);
                    System.out.println(
                        "OrderLine " + wid + " " + d + " " + oid + " " + olid + " Key orderline"
                            + olKey.str());
                    row = orderLineTable.createNewRow(config.PAD_COLUMNS);
                    orderLineTable.updateColumn(0, oid, row);
                    orderLineTable.updateColumn(1, d, row);
                    orderLineTable.updateColumn(2, wid, row); // ol_w_id
                    orderLineTable.updateColumn(3, olid, row);
                    orderLineTable.updateColumn(4, iid, row);
                    orderLineTable.updateColumn(5, wid, row); // ol_supply_w_id
                    // ol_delivery_d is a date string, we ignore this.
                    // delivery txn also ignores update of this column
                    orderLineTable.updateColumn(6, "", row);
                    // ol_quantity, spec define this to be 5 for all init orders
                    orderLineTable.updateColumn(7, olQuantity, row);
                    orderLineTable.updateColumn(8, amount, row);
                    String olDist = generator.RandString(24, 24, false);
                    orderLineTable.updateColumn(9, olDist, row);
                    ((RedisPostgresClient) client).write(TPCCConstants.kOrderLineTable, olKey.str(), row,
                            TPCCConstants.Transactions.LOAD.ordinal(), 0);
                  }

                  // Write New Order Table
                  if (oid >= config.INIT_NEW_ORDER_NB * 0.7) {
                    NewOrderKey noKey = new NewOrderKey(wid, d, oid);
                    row = newOrderTable.createNewRow(config.PAD_COLUMNS);
                    newOrderTable.updateColumn(0, oid, row);
                    newOrderTable.updateColumn(1, d, row);
                    newOrderTable.updateColumn(2, wid, row);
                    ((RedisPostgresClient) client).write(TPCCConstants.kNewOrderTable, noKey.str(), row,
                            TPCCConstants.Transactions.LOAD.ordinal(), 0);
                  }

                  client.commitTransaction();
                  success = true;
                } catch (DatabaseAbortException e) {
                  System.err.println(e.getMessage());
                  success = false;
                } catch (Exception e) {
                  e.printStackTrace();
                  System.exit(-1);
                }
              }
            }
          }
        };
        orderLineThread.start();
        threads.add(orderLineThread);
      }

      for (Thread t : threads) {
        t.join();
      }

      System.out.println("Finished Warehouses: " + start + " " + end);
    }

    client.requestExecutor.shutdown();
    while (!client.requestExecutor.isTerminated()) {}
  }

}
