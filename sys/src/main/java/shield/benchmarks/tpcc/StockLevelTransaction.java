package shield.benchmarks.tpcc;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import shield.benchmarks.tpcc.utils.TPCCConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

public class StockLevelTransaction extends BenchmarkTransaction {

  private int wid;

  private int did;
  private long txn_id;

  public StockLevelTransaction(TPCCGenerator generator, int wid, int did, long txn_id) {
    this.wid = wid;
    this.did = did;
    this.client = generator.getClient();
    this.txn_id = txn_id;
  }




  @Override
  public boolean tryRun() {
    try {

      List<byte[]> results;
      byte[] row;
      SortedSet<Integer> iids = new TreeSet<>();
      DistrictKey dKey;
      OrderLineKey orderLineKey;
      StockKey stockKey;
      int nextOid;
      int currentIid;
      int olCnt;
      OrderKey oKey;

      Table districtTable = client.getTable(TPCCConstants.kDistrictTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table stockTable = client.getTable(TPCCConstants.kStockTable);

      client.startTransaction();

      // Get next order id
      dKey = new DistrictKey(wid, did);
      results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kDistrictTable, dKey.str(),
              TPCCConstants.Transactions.STOCK_LEVEL.ordinal(), this.txn_id);
      row = results.get(0);
      nextOid = (Integer) districtTable.getColumn(10, row);
//      System.out.println("Next OID is " + nextOid);
      // Read the most 20 orders
      for (int i = 1; i <= 20 && (nextOid - i >= 0); i++) {
        // By spec, we should call all order lines within the most recent 20 orders.
        // We can either first get ol_cnt by reading order table, or just select each
        // order line until we hit a not found. here we choose the second approach as
        // it won't add an additional access to order table.
        oKey = new OrderKey(wid, did, nextOid - i);
        ((RedisPostgresClient) client).read(TPCCConstants.kOrderTable,
            oKey.str(), TPCCConstants.Transactions.STOCK_LEVEL.ordinal(), this.txn_id);

      }
      int i = 0 ;
      results = client.execute();
      for (byte[] order: results) {
        if (order.length != 0) {
          // olCnt = (Integer) orderTable.getColumn(6, order);
          olCnt = 5;
//          System.out.println("Order Count: wid " + wid + " " + did + " " + olCnt);
          int j = 0;
          do {
//            System.out.println("[OrderStatus] " + wid + " " + did + " " + (nextOid - i) + " " + j);
            orderLineKey = new OrderLineKey(wid, did, nextOid - i, j);
            ((RedisPostgresClient) client).read(TPCCConstants.kOrderLineTable, orderLineKey.str(),
                    TPCCConstants.Transactions.STOCK_LEVEL.ordinal(), this.txn_id);
          } while (j++ < olCnt); // 15 is max orderline
        } else {
//            System.out.println("Order did not exist " + wid + " " + did + " " + (nextOid -i));
        }
        i++;
      }

      results = client.execute();
      for (byte[] result : results) {
        if (result.length == 0) {
          // Order line is not valid;
//          System.out.println("Order Line is Not Valid");
        } else {
          // Order line is valid
          currentIid = (Integer) orderLineTable.getColumn(4, result);
          iids.add(currentIid);
        }
      }

//      System.out.println("Number of items: " + iids.size());

      for (Integer iid : iids) {
        stockKey = new StockKey(wid, iid);
        ((RedisPostgresClient) client).read(TPCCConstants.kStockTable, stockKey.str(),
                TPCCConstants.Transactions.STOCK_LEVEL.ordinal(), this.txn_id);
      }

      client.commitTransaction();

      return true;
    } catch (DatabaseAbortException e) {
      return false;
    }
  }
}
