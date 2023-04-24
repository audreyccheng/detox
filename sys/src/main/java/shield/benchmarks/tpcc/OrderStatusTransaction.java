package shield.benchmarks.tpcc;

import java.util.List;
import shield.benchmarks.tpcc.utils.TPCCConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

public class OrderStatusTransaction extends BenchmarkTransaction{

  private int wid;

  private int cid;

  private int did;

  private boolean accessByLastName;

  private String customerName;

  private TPCCExperimentConfiguration config;

  private TPCCGenerator generator;
  private long txn_id;

  public OrderStatusTransaction(TPCCGenerator generator,
      int wid, int cid, int did, boolean accessByLastName, String customerName, long txn_id) {
    this.wid = wid;
    this.cid = cid;
    this.did = did;
    this.accessByLastName = accessByLastName;
    this.customerName = customerName;
    this.generator = generator;
    this.config = generator.getConfig();
    this.client = generator.getClient();
    this.txn_id = txn_id;
  }




  @Override
  public boolean tryRun() {
    try {
      List<byte[]> results;
      OrderByCustomerKey obcKey;
      byte[] obcVal;
      int oid;
      OrderKey oKey;
      byte[] orderVal;
      int olCnt = 15;
      OrderLineKey olKey;
      byte[] orderLineVal;
      CustomerKey cKey;
      byte[] customerVal;
      CustomerByNameKey cByNameKey;

      Table orderByCustTable = client.getTable(TPCCConstants.kOrderByCustomerTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);
      Table custTable = client.getTable(TPCCConstants.kCustomerTable);
      Table custByNameTable = client.getTable(TPCCConstants.kCustomerByNameTable);

      client.startTransaction();

      if (accessByLastName) {
        cByNameKey = new CustomerByNameKey(wid, did, customerName);
        results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kCustomerByNameTable, cByNameKey.str(),
                TPCCConstants.Transactions.ORDER_STATUS.ordinal(), this.txn_id);
        customerVal = results.get(0);
        String customers = (String) custByNameTable.getColumn(0, customerVal);
        int nbCustomers = generator.getNbElements(customers);
        // Use the id of the n/2 customer (as specified in clause)
        cid = Integer.parseInt(generator.getElementAtIndex(customers, nbCustomers / 2));
      }
      // TRICK: use <W_ID, D_ID, C_ID> to form a "secondary index table"
      // It stores the most recent order from each user

      obcKey = new OrderByCustomerKey(wid,did,cid);
      results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kOrderByCustomerTable, obcKey.str(),
              TPCCConstants.Transactions.ORDER_STATUS.ordinal(), this.txn_id);
      obcVal = results.get(0);

      if (obcVal.length == 0) {
        System.out.println("No order for this customer");
        client.commitTransaction();
        return true;
      }

      // Get Order Count
      oid =(Integer)  orderByCustTable.getColumn(0,obcVal);
      oKey = new OrderKey(wid,did,oid);
      ((RedisPostgresClient) client).read(TPCCConstants.kOrderTable, oKey.str(), TPCCConstants.Transactions.ORDER_STATUS.ordinal(), this.txn_id);
      // Check Order Line
      for (int i = 0 ; i <olCnt ; i++) {
        olKey = new OrderLineKey(wid,did,oid,i);
        ((RedisPostgresClient) client).read(TPCCConstants.kOrderLineTable, olKey.str(),
                TPCCConstants.Transactions.ORDER_STATUS.ordinal(), this.txn_id);
      }



      // We reorder the access to customer table to the end
      // Delivery transaction has a data dependency of "Order->OrderLine->Customer",
      // and it reads/writes all these three tables.
      // If we put access to customer table at the beginning in order status,
      // we may encounter deadlock.
      cKey = new CustomerKey(wid,did,cid);
      ((RedisPostgresClient) client).read(TPCCConstants.kCustomerTable, cKey.str(),
              TPCCConstants.Transactions.ORDER_STATUS.ordinal(), this.txn_id);
      results = client.commitTransaction();

      orderVal = results.get(0);
      olCnt = (Integer) orderTable.getColumn(6,orderVal);
//      System.out.println("Order Count " + olCnt);
      return true;
    } catch (DatabaseAbortException e) {
      return false;
    }
  }
}
