package shield.benchmarks.tpcc;

import java.util.List;
import shield.benchmarks.tpcc.utils.TPCCConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

public class DeliveryTransaction extends BenchmarkTransaction {

  private int wid;

  private int carrierId;

  private int did;

  private TPCCGenerator generator;
  private TPCCExperimentConfiguration config;
  private long txn_id;

  public DeliveryTransaction(TPCCGenerator generator,
       int wid, int carrierId, int did, long txn_id) {
    this.client = generator.getClient();
    this.config = generator.getConfig();
    this.generator = generator;
    this.wid = wid;
    this.carrierId = carrierId;
    this.did = did;
    this.generator = generator;
    this.config = generator.getConfig();
    this.client = generator.getClient();
    this.txn_id = txn_id;
  }

  @Override
  public int run() {
    int nbAborts = 0;
    for (did = 0; did < config.NB_DISTRICTS; did++) {
      boolean succeed = false;
      while (!succeed) {
        succeed = tryRun();
        if (!succeed) nbAborts++;
      }
    }
    return nbAborts;
  }

  public boolean tryRun2() {

    try {

      List<byte[]> results;
      byte[] earliestNo;
      byte[] no;
      EarliestNewOrderKey enoKey;
      NewOrderKey noKey;
      Integer noId;
      OrderKey oKey;
      byte[] order;
      Integer cid;
      Integer olCnt = 15;
      byte[] olRow;
      CustomerKey cKey;
      byte[] custRow;
      Integer delCount;
      OrderLineKey olKey;
      int total = 0;
      String date;

      Table earliestNewOrderTable = client.getTable(TPCCConstants.kEarliestNewOrderTable);
      Table newOrderTable = client.getTable(TPCCConstants.kNewOrderTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);
      Table customerTable = client.getTable(TPCCConstants.kCustomerTable);

      client.startTransaction();
      // Read earliest table to find the order to deliver
      enoKey =
          new EarliestNewOrderKey(wid, did);

      results = ((RedisPostgresClient) client).readForUpdateAndExecute(TPCCConstants.kEarliestNewOrderTable, enoKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      earliestNo = results.get(0);
      if (earliestNo.length == 0) {
//        System.out.println("[Delivery] No earliest order");
        client.commitTransaction();
        return true;
      }

      noId = (Integer) earliestNewOrderTable.getColumn(2, earliestNo);
      noKey = new NewOrderKey(wid, did, noId);
      results = ((RedisPostgresClient) client).readForUpdateAndExecute(TPCCConstants.kNewOrderTable, noKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      // results = client.readAndExecute(TPCCConstants.kNewOrderTable, noKey.str());
      no = results.get(0);

      if (no.length == 0) {
        // if it is not ordered, stop
        // CHECK: are we still counting this as part of a successful delivery?
//        System.out.println("Item is not ordered");
        client.commitTransaction();
        return true;
      }

      // Update the earliest new order table
      noId = noId + 1;
//      System.out.println("[Delivery] Earliest New Order is Now " + wid + " " + did + " " + noId);
      earliestNewOrderTable.updateColumn(2, noId, no);
      ((RedisPostgresClient) client).update(TPCCConstants.kEarliestNewOrderTable, enoKey.str(), no,
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);

      // Delete a record (for now, write the empty value)
      ((RedisPostgresClient) client).delete(TPCCConstants.kNewOrderTable, noKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);

      // Read and update order
      oKey = new OrderKey(wid, did, noId);
      ((RedisPostgresClient) client).readForUpdate(TPCCConstants.kOrderTable,
        oKey.str(), TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      // client.read(TPCCConstants.kOrderTable, oKey.str());

      for (int i = 0 ; i < olCnt ; i++) {
        olKey = new OrderLineKey(wid, did, noId, i);
        ((RedisPostgresClient) client).readForUpdate(TPCCConstants.kOrderLineTable, olKey.str(),
                TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
        //client.read(TPCCConstants.kOrderLineTable, olKey.str());
      }

      results = client.execute();
      order = results.get(0);
      cid = (Integer) orderTable.getColumn(3, order);
      olCnt = (Integer) orderTable.getColumn(6, order);

//      System.out.println("[Delivery] Order count is " + olCnt);

      assert (olCnt>= 5 && olCnt <=15 );

      olRow = null;
      for (int i = 0 ; i < olCnt; i++) {
        try {
          olKey = new OrderLineKey(wid, did, noId, i);
          olRow = results.get(i + 1);
          total += (Integer) orderLineTable.getColumn(8, olRow);
          // Now update the delivery to the current time
          date = generator.getTime();
          orderLineTable.updateColumn(6, date, olRow);
          ((RedisPostgresClient) client).update(TPCCConstants.kOrderLineTable, olKey.str(), olRow,
                  TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
        } catch (Exception e) {
           System.err.println("Order Count " + i);
           System.err.println(results.get(0));
           System.err.println("Prev Order " + orderLineTable.printColumn(results.get(i)));
           client.abortTransaction();
           return false;
        }

     }

      orderTable.updateColumn(5, carrierId, order);
      ((RedisPostgresClient) client).update(TPCCConstants.kOrderTable, oKey.str(), order,
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);

      // Read and update customer table
      cKey = new CustomerKey(wid, did, cid);
//      System.out.println("Updating Customer " + wid + " " + cid);
      results = ((RedisPostgresClient) client).readForUpdateAndExecute(TPCCConstants.kCustomerTable, cKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      // results = client.readAndExecute(TPCCConstants.kCustomerTable, cKey.str());
      custRow = results.get(0);
      delCount = (Integer) customerTable.getColumn(19, custRow);
      customerTable.updateColumn(16, total, custRow);
      customerTable.updateColumn(19, 1 + delCount, custRow);
      ((RedisPostgresClient) client).update(TPCCConstants.kCustomerTable, cKey.str(), custRow,
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      client.commitTransaction();
      return true;
    } catch (DatabaseAbortException e) {
//      System.out.println("Returning False");
      return false;
    }
  }

  public boolean tryRun() {
    try {

      List<byte[]> results;
      byte[] earliestNo;
      byte[] no;
      EarliestNewOrderKey enoKey;
      NewOrderKey noKey;
      Integer noId;
      OrderKey oKey;
      byte[] order;
      Integer cid;
      Integer olCnt;
      byte[] olRow;
      CustomerKey cKey;
      byte[] custRow;
      Integer delCount;
      OrderLineKey olKey;
      int total = 0;
      String date;

      Table earliestNewOrderTable = client.getTable(TPCCConstants.kEarliestNewOrderTable);
      Table newOrderTable = client.getTable(TPCCConstants.kNewOrderTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);
      Table customerTable = client.getTable(TPCCConstants.kCustomerTable);

      client.startTransaction();
      // Read earliest table to find the order to deliver
      enoKey =
          new EarliestNewOrderKey(wid, did);


      results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kEarliestNewOrderTable, enoKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      earliestNo = results.get(0);
      if (earliestNo.length == 0) {
//        System.out.println("[Delivery] No earliest order");
        client.commitTransaction();
        return true;
      }

      noId = (Integer) earliestNewOrderTable.getColumn(2, earliestNo);

      noKey = new NewOrderKey(wid, did, noId);
      results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kNewOrderTable, noKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      no = results.get(0);

      if (no.length == 0) {
        // if it is not ordered, stop
        // CHECK: are we still counting this as part of a successful delivery?
//        System.out.println("Item is not ordered");
        client.commitTransaction();
        return true;
      }

      // Update the earliest new order table
      noId = noId + 1;
//      System.out.println("[Delivery] Earliest New Order is Now " + wid + " " + did + " " + noId);
      earliestNewOrderTable.updateColumn(2, noId, no);
      ((RedisPostgresClient) client).update(TPCCConstants.kEarliestNewOrderTable, enoKey.str(), no,
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);

      // Delete a record (for now, write the empty value)
      ((RedisPostgresClient) client).delete(TPCCConstants.kNewOrderTable, noKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);

      // Read and update order
      oKey = new OrderKey(wid, did, noId);
      results = ((RedisPostgresClient) client).readForUpdateAndExecute(TPCCConstants.kOrderTable,
          oKey.str(), TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      order = results.get(0);
      cid = (Integer) orderTable.getColumn(3, order);
      olCnt = (Integer) orderTable.getColumn(6, order);

      assert (olCnt>= 5 && olCnt <=15 );

      // Update Carrier id
      orderTable.updateColumn(5, carrierId, order);
      ((RedisPostgresClient) client).update(TPCCConstants.kOrderTable, oKey.str(), order,
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);

      for (int i = 0; i < olCnt; i++) {
        olKey = new OrderLineKey(wid, did, noId, i);
        // Compute amount
        results = ((RedisPostgresClient) client).readForUpdateAndExecute(TPCCConstants.kOrderLineTable, olKey.str(),
                TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
        olRow = results.get(0);
        total += (Integer) orderLineTable.getColumn(8, olRow);
        // Now update the delivery to the current time
        date = generator.getTime();
        orderLineTable.updateColumn(6, date, olRow);
        ((RedisPostgresClient) client).update(TPCCConstants.kOrderLineTable, olKey.str(), olRow,
                TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      }

      // Read and update customer table
      cKey = new CustomerKey(wid, did, cid);
      results = ((RedisPostgresClient) client).readForUpdateAndExecute(TPCCConstants.kCustomerTable, cKey.str(),
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
     // results = client.readAndExecute(TPCCConstants.kCustomerTable, cKey.str());
      custRow = results.get(0);
      delCount = (Integer) customerTable.getColumn(19, custRow);
      customerTable.updateColumn(16, total, custRow);
      customerTable.updateColumn(19, 1 + delCount, custRow);
      ((RedisPostgresClient) client).update(TPCCConstants.kCustomerTable, cKey.str(), custRow,
              TPCCConstants.Transactions.DELIVERY.ordinal(), this.txn_id);
      client.commitTransaction();
      return true;
    } catch (DatabaseAbortException e) {
//      System.out.println("Returning False");
      return false;
    } catch (Exception e) {
        try {
          client.abortTransaction();
        } catch (Exception ee) {
          System.exit(-1);
        }
    }
    return false;
  }

  /*
  @Override
  public boolean tryRun() {
    try {

      List<byte[]> results;
      byte[] earliestNo;
      byte[] no;
      EarliestNewOrderKey enoKey;
      NewOrderKey noKey;
      Integer noId;
      OrderKey oKey;
      byte[] order;
      Integer cid;
      Integer olCnt;
      byte[] olRow;
      CustomerKey cKey;
      byte[] custRow;
      Integer delCount;
      OrderLineKey olKey;
      int total = 0;
      String date;

      Table earliestNewOrderTable = client.getTable(TPCCConstants.kEarliestNewOrderTable);
      Table newOrderTable = client.getTable(TPCCConstants.kNewOrderTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);
      Table customerTable = client.getTable(TPCCConstants.kCustomerTable);

      client.startTransaction();
      // Read earliest table to find the order to deliver
      enoKey =
          new EarliestNewOrderKey(wid, did);

      results = client.readAndExecute(TPCCConstants.kEarliestNewOrderTable, enoKey.str());
      earliestNo = results.get(0);
      if (earliestNo.length == 0) {
        System.out.println("[Delivery] No earliest order");
        client.commitTransaction();
        return true;
      }

      noId = (Integer) earliestNewOrderTable.getColumn(2, earliestNo);
      noKey = new NewOrderKey(wid, did, noId);
      results = client.readAndExecute(TPCCConstants.kNewOrderTable, noKey.str());
      no = results.get(0);

      if (no.length == 0) {
        // if it is not ordered, stop
        // CHECK: are we still counting this as part of a successful delivery?
        System.out.println("Item is not ordered");
        client.commitTransaction();
        return true;
      }

      // Update the earliest new order table
      noId = noId + 1;
      System.out.println("[Delivery] Earliest New Order is Now " + wid + " " + did + " " + noId);
      earliestNewOrderTable.updateColumn(2, noId, no);
      client.write(TPCCConstants.kEarliestNewOrderTable, enoKey.str(), no);

      // Delete a record (for now, write the empty value)
      client.delete(TPCCConstants.kNewOrderTable, noKey.str());

      // Read and update order
      oKey = new OrderKey(wid, did, noId);
      results = client.readAndExecute(TPCCConstants.kOrderTable,
          oKey.str());
      order = results.get(0);
      cid = (Integer) orderTable.getColumn(3, order);
      olCnt = (Integer) orderTable.getColumn(6, order);

      assert (olCnt>= 5 && olCnt <=15 );

      // Update Carrier id
      orderTable.updateColumn(5, carrierId, order);
      client.write(TPCCConstants.kOrderTable, oKey.str(), order);


      System.out.println("Read and update customer table");
      // Read and update customer table
      cKey = new CustomerKey(wid, did, cid);
      client.read(TPCCConstants.kCustomerTable, cKey.str());
      for (int i = 0; i < olCnt; i++) {
        olKey = new OrderLineKey(wid, did, noId, i);
        // Compute amount
        client.read(TPCCConstants.kOrderLineTable, olKey.str());
      }
      results = client.execute();

      System.out.println("results " + results.size() + " olcnt " + olCnt);
      custRow = results.get(0);

      for (int i = 0 ; i < olCnt; i++) {
        olRow = results.get(i+1);
        olKey = new OrderLineKey(wid, did, noId, i);
        total += (Integer) orderLineTable.getColumn(8, olRow);
        // Now update the delivery to the current time
        date = generator.getTime();
        orderLineTable.updateColumn(6, date, olRow);
        client.write(TPCCConstants.kOrderLineTable, olKey.str(), olRow);
      }

      // Read and update customer table
      delCount = (Integer) customerTable.getColumn(19, custRow);
      customerTable.updateColumn(16, total, custRow);
      customerTable.updateColumn(19, 1 + delCount, custRow);
      client.write(TPCCConstants.kCustomerTable, cKey.str(), custRow);
      client.commitTransaction();
      return true;
    } catch (DatabaseAbortException e) {
      System.out.println("Returning False");
      return false;
    }
  } */
}
