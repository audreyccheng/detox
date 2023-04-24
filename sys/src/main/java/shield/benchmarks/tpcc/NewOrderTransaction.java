package shield.benchmarks.tpcc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.xml.crypto.Data;
import shield.benchmarks.tpcc.utils.TPCCConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.RedisPostgresClient;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

public class NewOrderTransaction extends BenchmarkTransaction{

  private final TPCCGenerator generator;
  private final TPCCExperimentConfiguration config;

  private final int wid;

  private final int did;

  private final int cid;

  private final int olCnt;

  private final Set<Integer> iids;

  private final List<Integer> swids;

  private final List<Integer> olQuantities;
  private long txn_id;

  public NewOrderTransaction(TPCCGenerator generator, int wid, int did, int cid, int olCnt,
      Set<Integer> iids, List<Integer> swids, List<Integer> olQuantities, long txn_id) {
    this.wid = wid;
    this.did = did;
    this.cid = cid;
    this.olCnt = olCnt;
    this.iids = iids;
    this.swids = swids;
    this.olQuantities = olQuantities;
    this.generator = generator;
    this.config = generator.getConfig();
    this.client = generator.getClient();
    this.txn_id = txn_id;
  }


  public boolean tryRun2() {

    try {
      WarehouseKey wKey;
      DistrictKey dKey;
      CustomerKey cKey;
      NewOrderKey newOrderKey;
      OrderByCustomerKey orderByCustomerKey;
      ItemKey itemKey;
      OrderKey orderKey;
      OrderLineKey orderLineKey;
      StockKey stockKey;

      String wName;
      String dName;
      int wTax;
      int dTax;
      int dNextOid;
      List<byte[]> results;
      byte[] wRow;
      byte[] dRow;
      byte[] cRow;
      byte[] oRow;
      byte[] result;
      int cDiscount;
      String cLastName;
      String cCredit;
      int itemId;
      int olQuantity;
      int supplyWid;
      int price;
      boolean allLocal;
      int sQuantity;
      String distInfo;
      int stock;
      int amount = 0;
      String sData;
      String iData;
      int sYtd;
      int remoteCount;

      allLocal = true;
      for (int i = 0; i < olCnt; ++i) {
        if (swids.get(i) != wid) {
          allLocal = false;
          break;
        }
      }

      Table warehouseTable = client.getTable(TPCCConstants.kWarehouseTable);
      Table districtTable = client.getTable(TPCCConstants.kDistrictTable);
      Table customerTable = client.getTable(TPCCConstants.kCustomerTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table newOrderTable = client.getTable(TPCCConstants.kNewOrderTable);
      Table orderByCustTable = client.getTable(TPCCConstants.kOrderByCustomerTable);
      Table itemTable = client.getTable(TPCCConstants.kItemTable);
      Table stockTable = client.getTable(TPCCConstants.kStockTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);

      client.startTransaction();
      wKey = new WarehouseKey(wid);
      dKey = new DistrictKey(wid, did);
      cKey = new CustomerKey(wid, did, cid);

      ((RedisPostgresClient) client).read(TPCCConstants.kWarehouseTable, wKey.str(),
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
      ((RedisPostgresClient) client).readForUpdate(TPCCConstants.kDistrictTable, dKey.str(),
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kCustomerTable, cKey.str(),
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
      wRow = results.get(0);
      dRow = results.get(1);
      cRow = results.get(2);

      wTax = (Integer) warehouseTable.getColumn(7, wRow);
      dTax = (Integer) districtTable.getColumn(8, dRow);
      dNextOid = (Integer) districtTable.getColumn(10, dRow);
//      System.out.println("[NewOrder] Wid: " + wid + " " + did + " " + dNextOid);

      cDiscount = (Integer) customerTable.getColumn(15, cRow);
      cLastName = (String) customerTable.getColumn(5, cRow);
      cCredit = (String) customerTable.getColumn(13, cRow);

      districtTable.updateColumn(10, dNextOid + 1, dRow);
      ((RedisPostgresClient) client).update(TPCCConstants.kDistrictTable, dKey.str(), dRow,
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

      // Write new order

      newOrderKey = new NewOrderKey(wid, did, dNextOid);
      oRow = newOrderTable.createNewRow(config.PAD_COLUMNS);
      newOrderTable.updateColumn(0, dNextOid, oRow);
      newOrderTable.updateColumn(1, did, oRow);
      newOrderTable.updateColumn(2, wid, oRow);
      ((RedisPostgresClient) client).write(TPCCConstants.kNewOrderTable, newOrderKey.str(), oRow,
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

      // Write order - update secondary index
      oRow = orderByCustTable.createNewRow(config.PAD_COLUMNS);
      orderByCustomerKey = new OrderByCustomerKey(wid, did, cid);
      orderByCustTable.updateColumn(0, dNextOid, oRow);
      ((RedisPostgresClient) client).write(TPCCConstants.kOrderByCustomerTable, orderByCustomerKey.str(),
          oRow, TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

      // Write order
      oRow = orderTable.createNewRow(config.PAD_COLUMNS);
      orderKey = new OrderKey(wid, did, dNextOid);
      orderTable.updateColumn(0, dNextOid, oRow);
      orderTable.updateColumn(1, did, oRow);
      orderTable.updateColumn(2, wid, oRow);
      orderTable.updateColumn(3, cid, oRow);
      orderTable.updateColumn(4, generator.getTime(), oRow);
      orderTable.updateColumn(5, 0, oRow);
      orderTable.updateColumn(6, olCnt, oRow);
      orderTable.updateColumn(7, allLocal ? 1 : 0, oRow);
      ((RedisPostgresClient) client).write(TPCCConstants.kOrderTable, orderKey.str(), oRow,
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

//      System.out.println("New Order " + orderTable.printColumn(oRow));

      itemId = 0;

      // First get item info and stock info
      for (Integer iid: iids) {
        itemKey = new ItemKey(iid);
        ((RedisPostgresClient) client).read(TPCCConstants.kItemTable, itemKey.str(),
                TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
      }
      itemId = 0;
      for (Integer iid:iids) {
        supplyWid = swids.get(itemId);
        stockKey = new StockKey(supplyWid,iid);
        ((RedisPostgresClient) client).readForUpdate(TPCCConstants.kStockTable, stockKey.str(),
                TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
        itemId++;
      }
      results = client.execute();

      //Write stock and order line
      itemId = 0;
      for (Integer iid:iids) {
        olQuantity = olQuantities.get(itemId);
        supplyWid = swids.get(itemId);
        // Result from item table
        oRow = results.get(itemId);

        if (oRow.length == 0) {
//          System.out.println("Item not found, rolling back " + iid);
          client.abortTransaction();
          return true;
        }
        price = (Integer) itemTable.getColumn(3,oRow);
        iData = (String) itemTable.getColumn(4, oRow);

        // Result from stock table
        stockKey = new StockKey(supplyWid,iid);
        oRow = results.get(iids.size() + itemId);
        sQuantity = (Integer) stockTable.getColumn(2,oRow);
        distInfo = (String) stockTable.getColumn(3+ did, oRow);
        sData = (String) stockTable.getColumn(16, oRow);
        sYtd = (Integer) stockTable.getColumn(13, oRow);
        remoteCount = (Integer) stockTable.getColumn(15, oRow);
        sYtd+=olQuantity;
        stock = sQuantity - olQuantity;
        if (stock<0) {
          stock+=91;
        } else {
          stock  = sQuantity- olQuantity;
        }
        stockTable.updateColumn(2,stock,oRow);
        stockTable.updateColumn(13,sYtd,oRow);
        stockTable.updateColumn(14,1,oRow);
        stockTable.updateColumn(15, wid != supplyWid? remoteCount+1:remoteCount, oRow);
        ((RedisPostgresClient) client).update(TPCCConstants.kStockTable, stockKey.str(), oRow,
                TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

        orderLineKey = new OrderLineKey(wid,did,dNextOid,itemId);
        oRow = orderLineTable.createNewRow(config.PAD_COLUMNS);
        orderLineTable.updateColumn(0, dNextOid,oRow);
        orderLineTable.updateColumn(1, did, oRow);
        orderLineTable.updateColumn(2, wid, oRow);
        orderLineTable.updateColumn(3, itemId, oRow);
        orderLineTable.updateColumn(4, iid, oRow);
        orderLineTable.updateColumn(5, supplyWid, oRow);
        // ol_deliver_g set to null
        orderLineTable.updateColumn(6, "", oRow);
        orderLineTable.updateColumn(7, olQuantity, oRow);
        amount += (int) ((double) olQuantity * (double) price);
        orderLineTable.updateColumn(8, olQuantity*price, oRow);
        ((RedisPostgresClient) client).write(TPCCConstants.kOrderLineTable, orderLineKey.str(),
            oRow, TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
        itemId++;
      }
      client.commitTransaction();
      amount = (int) (amount *(1.0 - (double) cDiscount / 10000.0)
          * (1.0 + (double) dTax / 10000.0 + (double) wTax / 10000.0));
      return true;
    } catch (DatabaseAbortException e) {
      return false;
    }
  }


  public boolean tryRun() {
    try {

      WarehouseKey wKey;
      DistrictKey dKey;
      CustomerKey cKey;
      NewOrderKey newOrderKey;
      OrderByCustomerKey orderByCustomerKey;
      ItemKey itemKey;
      OrderKey orderKey;
      OrderLineKey orderLineKey;
      StockKey stockKey;

      String wName;
      String dName;
      int wTax;
      int dTax;
      int dNextOid;
      List<byte[]> results;
      byte[] wRow;
      byte[] dRow;
      byte[] cRow;
      byte[] oRow;
      int cDiscount;
      String cLastName;
      String cCredit;
      int itemId;
      int olQuantity;
      int supplyWid;
      int price;
      boolean allLocal;
      int sQuantity;
      String distInfo;
      int stock;
      int amount = 0;
      String sData;
      String iData;
      int sYtd;
      int remoteCount;

      allLocal = true;
      for (int i = 0; i < olCnt; ++i) {
        if (swids.get(i) != wid) {
          allLocal = false;
          break;
        }
      }

      Table warehouseTable = client.getTable(TPCCConstants.kWarehouseTable);
      Table districtTable = client.getTable(TPCCConstants.kDistrictTable);
      Table customerTable = client.getTable(TPCCConstants.kCustomerTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table newOrderTable = client.getTable(TPCCConstants.kNewOrderTable);
      Table orderByCustTable = client.getTable(TPCCConstants.kOrderByCustomerTable);
      Table itemTable = client.getTable(TPCCConstants.kItemTable);
      Table stockTable = client.getTable(TPCCConstants.kStockTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);

      client.startTransaction();
      wKey = new WarehouseKey(wid);
      dKey = new DistrictKey(wid, did);
      cKey = new CustomerKey(wid, did, cid);
      ((RedisPostgresClient) client).read(TPCCConstants.kWarehouseTable, wKey.str(), TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
      ((RedisPostgresClient) client).readForUpdate(TPCCConstants.kDistrictTable, dKey.str(),
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kCustomerTable, cKey.str(),
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
      wRow = results.get(0);
      dRow = results.get(1);
      cRow = results.get(2);

      wTax = (Integer) warehouseTable.getColumn(7, wRow);
      dTax = (Integer) districtTable.getColumn(8, dRow);
      dNextOid = (Integer) districtTable.getColumn(10, dRow);
//      System.out.println("[NewOrder] Wid: " + wid + " " + did + " " + dNextOid);

      cDiscount = (Integer) customerTable.getColumn(15, cRow);
      cLastName = (String) customerTable.getColumn(5, cRow);
      cCredit = (String) customerTable.getColumn(13, cRow);

      districtTable.updateColumn(10, dNextOid + 1, dRow);
      ((RedisPostgresClient) client).update(TPCCConstants.kDistrictTable, dKey.str(), dRow,
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

      // Write new order

      newOrderKey = new NewOrderKey(wid, did, dNextOid);
      oRow = newOrderTable.createNewRow(config.PAD_COLUMNS);
      newOrderTable.updateColumn(0, dNextOid, oRow);
      newOrderTable.updateColumn(1, did, oRow);
      newOrderTable.updateColumn(2, wid, oRow);
      ((RedisPostgresClient) client).write(TPCCConstants.kNewOrderTable, newOrderKey.str(), oRow,
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

      // Write order - update secondary index
      oRow = orderByCustTable.createNewRow(config.PAD_COLUMNS);
      orderByCustomerKey = new OrderByCustomerKey(wid, did, cid);
      orderByCustTable.updateColumn(0, dNextOid, oRow);
      ((RedisPostgresClient) client).write(TPCCConstants.kOrderByCustomerTable, orderByCustomerKey.str(),
          oRow, TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

      // Write order
      oRow = orderTable.createNewRow(config.PAD_COLUMNS);
      orderKey = new OrderKey(wid, did, dNextOid);
      orderTable.updateColumn(0, dNextOid, oRow);
      orderTable.updateColumn(1, did, oRow);
      orderTable.updateColumn(2, wid, oRow);
      orderTable.updateColumn(3, cid, oRow);
      orderTable.updateColumn(4, generator.getTime(), oRow);
      orderTable.updateColumn(5, 0, oRow);
      orderTable.updateColumn(6, olCnt, oRow);
      orderTable.updateColumn(7, allLocal ? 1 : 0, oRow);
      ((RedisPostgresClient) client).write(TPCCConstants.kOrderTable, orderKey.str(), oRow,
              TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

//      System.out.println("NewOrder " + orderTable.printColumn(oRow));

      // Write order line
      itemId = 0;
      for (Integer iid : iids) {

        olQuantity = olQuantities.get(itemId);
        supplyWid = swids.get(itemId);

        itemKey = new ItemKey(iid);
        results = ((RedisPostgresClient) client).readAndExecute(TPCCConstants.kItemTable, itemKey.str(),
                TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
        oRow = results.get(0);
        if (oRow.length == 0) {
          System.out.println("Item not found, rolling back " + iid);
          client.abortTransaction();
          return true;
        }
        price = (Integer) itemTable.getColumn(3, oRow);
        iData = (String) itemTable.getColumn(4, oRow);
        stockKey = new StockKey(supplyWid, iid);
        results = ((RedisPostgresClient) client).readForUpdateAndExecute(TPCCConstants.kStockTable, stockKey.str(),
                TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
        //   results = client.readAndExecute(TPCCConstants.kStockTable, stockKey.str());
        oRow = results.get(0);
        sQuantity = (Integer) stockTable.getColumn(2, oRow);
        distInfo = (String) stockTable.getColumn(3 + did, oRow);
        sData = (String) stockTable.getColumn(16, oRow);
        sYtd = (Integer) stockTable.getColumn(13, oRow);
        remoteCount = (Integer) stockTable.getColumn(15, oRow);
        sYtd += olQuantity;
        stock = sQuantity - olQuantity;
        if (stock < 0) {
          stock += 91;
        } else {
          stock = sQuantity - olQuantity;
        }
        stockTable.updateColumn(2, stock, oRow);
        stockTable.updateColumn(13, sYtd, oRow);
        stockTable.updateColumn(14, 1, oRow);
        stockTable.updateColumn(15, wid != supplyWid ? remoteCount + 1 : remoteCount, oRow);
        ((RedisPostgresClient) client).update(TPCCConstants.kStockTable, stockKey.str(), oRow,
                TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);

        orderLineKey = new OrderLineKey(wid, did, dNextOid, itemId);
        oRow = orderLineTable.createNewRow(config.PAD_COLUMNS);
        orderLineTable.updateColumn(0, dNextOid, oRow);
        orderLineTable.updateColumn(1, did, oRow);
        orderLineTable.updateColumn(2, wid, oRow);
        orderLineTable.updateColumn(3, itemId, oRow);
        orderLineTable.updateColumn(4, iid, oRow);
        orderLineTable.updateColumn(5, supplyWid, oRow);
        // ol_deliver_g set to null
        orderLineTable.updateColumn(6, "", oRow);
        orderLineTable.updateColumn(7, olQuantity, oRow);
        amount += (int) ((double) olQuantity * (double) price);
        orderLineTable.updateColumn(8, olQuantity * price, oRow);
        ((RedisPostgresClient) client).writeAndExecute(TPCCConstants.kOrderLineTable, orderLineKey.str(),
            oRow, TPCCConstants.Transactions.NEW_ORDER.ordinal(), this.txn_id);
        itemId++;
      }
      amount = (int) (amount * (1.0 - (double) cDiscount / 10000.0)
          * (1.0 + (double) dTax / 10000.0 + (double) wTax / 10000.0));

      client.commitTransaction();
      return true;
    } catch (DatabaseAbortException e) {
      return false;
    } catch (Exception e) {
      // TODO(natacha): remove
      System.err.println(e.getMessage());
      System.err.println(e.getStackTrace());
      System.exit(-1);
      try {
        client.abortTransaction();
      } catch (DatabaseAbortException e1) {
        System.exit(-1);
      }
    }
    return false;
  }

/*
  @Override
  public boolean tryRun() {
    try {

      WarehouseKey wKey;
      DistrictKey dKey;
      CustomerKey cKey;
      NewOrderKey newOrderKey;
      OrderByCustomerKey orderByCustomerKey;
      ItemKey itemKey;
      OrderKey orderKey;
      OrderLineKey orderLineKey;
      StockKey stockKey;

      String wName;
      String dName;
      int wTax;
      int dTax;
      int dNextOid;
      List<byte[]> results;
      byte[] wRow;
      byte[] dRow;
      byte[] cRow;
      byte[] oRow;
      int cDiscount;
      String cLastName;
      String cCredit;
      int itemId;
      int olQuantity;
      int supplyWid;
      int price;
      boolean allLocal;
      int sQuantity;
      String distInfo;
      int stock;
      int amount = 0;
      String sData;
      String iData;
      int sYtd;
      int remoteCount;
      ArrayList<Integer> prices = new ArrayList<>();

      allLocal = true;
      for (int i = 0; i < olCnt ; ++i) {
        if (swids.get(i) != wid) {
          allLocal = false;
          break;
        }
      }

      Table warehouseTable = client.getTable(TPCCConstants.kWarehouseTable);
      Table districtTable = client.getTable(TPCCConstants.kDistrictTable);
      Table customerTable = client.getTable(TPCCConstants.kCustomerTable);
      Table orderTable = client.getTable(TPCCConstants.kOrderTable);
      Table newOrderTable = client.getTable(TPCCConstants.kNewOrderTable);
      Table orderByCustTable = client.getTable(TPCCConstants.kOrderByCustomerTable);
      Table itemTable = client.getTable(TPCCConstants.kItemTable);
      Table stockTable = client.getTable(TPCCConstants.kStockTable);
      Table orderLineTable = client.getTable(TPCCConstants.kOrderLineTable);

      client.startTransaction();
      wKey = new WarehouseKey(wid);
      dKey = new DistrictKey(wid, did);
      cKey = new CustomerKey(wid,did,cid);
      client.read(TPCCConstants.kWarehouseTable,wKey.str());
      client.read(TPCCConstants.kDistrictTable, dKey.str());
      results = client.readAndExecute(TPCCConstants.kCustomerTable, cKey.str());
      wRow = results.get(0);
      dRow = results.get(1);
      cRow = results.get(2);

      wTax = (Integer) warehouseTable.getColumn(7, wRow);
      dTax = (Integer) districtTable.getColumn(8, dRow);
      dNextOid = (Integer) districtTable.getColumn(10, dRow);
      cDiscount = (Integer) customerTable.getColumn(15,cRow);
      cLastName = (String) customerTable.getColumn(5, cRow);
      cCredit = (String) customerTable.getColumn(13,cRow);

      districtTable.updateColumn(10, dNextOid + 1, dRow);
      client.write(TPCCConstants.kDistrictTable, dKey.str(), dRow);

      // Write new order
      System.out.println("[NewOrder] Wid: " + wid + " " + did + " " + dNextOid);

      newOrderKey = new NewOrderKey(wid,did,dNextOid);
      oRow = newOrderTable.createNewRow(config.PAD_COLUMNS);
      newOrderTable.updateColumn(0, dNextOid, oRow);
      newOrderTable.updateColumn(1,did, oRow);
      newOrderTable.updateColumn(2, wid, oRow);
      client.write(TPCCConstants.kNewOrderTable,  newOrderKey.str(), oRow);

      // Write order - update secondary index
      oRow = orderByCustTable.createNewRow(config.PAD_COLUMNS);
      orderByCustomerKey = new OrderByCustomerKey(wid,did,cid);
      orderByCustTable.updateColumn(0, dNextOid, oRow);
      client.write(TPCCConstants.kOrderByCustomerTable, orderByCustomerKey.str(),
          oRow);

      // Write order
      oRow = orderTable.createNewRow(config.PAD_COLUMNS);
      orderKey = new OrderKey(wid,did,dNextOid);
      orderTable.updateColumn(0,dNextOid, oRow);
      orderTable.updateColumn(1,did,oRow);
      orderTable.updateColumn(2,wid, oRow);
      orderTable.updateColumn(3,cid,oRow);
      orderTable.updateColumn(4,generator.getTime(), oRow);
      orderTable.updateColumn(5,0,oRow);
      orderTable.updateColumn(6, olCnt, oRow);
      orderTable.updateColumn(7, allLocal? 1: 0, oRow);
      client.write(TPCCConstants.kOrderTable, orderKey.str(), oRow);

      // Write order line
      itemId = 0;

      for (Integer iid: iids) {
        itemKey = new ItemKey(iid);
        System.out.println("Item " + iid);
        client.read(TPCCConstants.kItemTable, itemKey.str());
        itemId++;
      }

      results = client.execute();
      itemId = 0;

      System.out.println("Received Reads");

      for (Integer iid: iids) {
        oRow = results.get(itemId);
        if (oRow.length == 0) {
          System.out.println("Item not found, rolling back " + iid);
          client.abortTransaction();
          return true;
        }
        price = (Integer) itemTable.getColumn(3,oRow);
        prices.add(price);
        iData = (String) itemTable.getColumn(4, oRow);
        olQuantity = olQuantities.get(itemId);
        supplyWid  = swids.get(itemId);
        stockKey = new StockKey(supplyWid,iid);
        client.read(TPCCConstants.kStockTable, stockKey.str());
        itemId++;
      }

      results = client.execute();
      itemId = 0;
      for (Integer iid: iids) {
        oRow = results.get(itemId);
        sQuantity = (Integer) stockTable.getColumn(2,oRow);
        distInfo = (String) stockTable.getColumn(3+ did, oRow);
        sData = (String) stockTable.getColumn(16, oRow);
        sYtd = (Integer) stockTable.getColumn(13, oRow);
        remoteCount = (Integer) stockTable.getColumn(15, oRow);
        olQuantity = olQuantities.get(itemId);
        supplyWid  = swids.get(itemId);
        sYtd+=olQuantity;
        stock = sQuantity - olQuantity;
        if (stock<0) {
          stock+=91;
        } else {
          stock  = sQuantity- olQuantity;
        }
        stockTable.updateColumn(2,stock,oRow);
        stockTable.updateColumn(13,sYtd,oRow);
        stockTable.updateColumn(14,1,oRow);
        stockTable.updateColumn(15, wid != supplyWid? remoteCount+1:remoteCount, oRow);
        stockKey = new StockKey(supplyWid,iid);
        client.write(TPCCConstants.kStockTable, stockKey.str(), oRow);
        orderLineKey = new OrderLineKey(wid,did,dNextOid,itemId);
        oRow = orderLineTable.createNewRow(config.PAD_COLUMNS);
        orderLineTable.updateColumn(0, dNextOid,oRow);
        orderLineTable.updateColumn(1, did, oRow);
        orderLineTable.updateColumn(2, wid, oRow);
        orderLineTable.updateColumn(3, itemId, oRow);
        orderLineTable.updateColumn(4, iid, oRow);
        orderLineTable.updateColumn(5, supplyWid, oRow);
        // ol_deliver_g set to null
        orderLineTable.updateColumn(6, "", oRow);
        orderLineTable.updateColumn(7, olQuantity, oRow);
        amount += (int) ((double) olQuantity * (double) prices.get(itemId));
        orderLineTable.updateColumn(8, olQuantity*prices.get(itemId), oRow);
        client.write(TPCCConstants.kOrderLineTable, orderLineKey.str(),
            oRow);
        itemId++;
      }
      amount = (int) (amount *(1.0 - (double) cDiscount / 10000.0)
            * (1.0 + (double) dTax / 10000.0 + (double) wTax / 10000.0));




      client.commitTransaction();
      return true;
    } catch (DatabaseAbortException e) {
      return false;
    }
  } */
}
