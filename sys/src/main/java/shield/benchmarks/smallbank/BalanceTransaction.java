package shield.benchmarks.smallbank;

import java.util.List;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

/**
 * a parameterized transaction that represents calculating the total balance for
 * a customer with name N. It returns the sum of savings and checking balances for the specified
 * customer
 */
public class BalanceTransaction extends BenchmarkTransaction{

  private Integer custId;
  private long txn_id;

  public BalanceTransaction(SmallBankGenerator generator, int custId, long txn_id) {
    this.txn_id = txn_id;
    this.custId= custId;
    this.client = generator.getClient();
  }

  @Override
  public boolean tryRun() {
  try {

//      System.out.println("Balance Transaction");

      List<byte[]> results;
      byte[] rowSavingsCus1;
      byte[] rowCheckingsCus1;
      Integer balCC1;
      Integer balSC1;
      Integer total;

      Table checkingsTable = client.getTable(SmallBankConstants.kCheckingsTable);
      Table savingsTable = client.getTable(SmallBankConstants.kSavingsTable);


      client.startTransaction();
      // Get Account
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kAccountsTable, custId.toString(), SmallBankTransactionType.BALANCE.ordinal(), this.txn_id);

      if (results.get(0).equals("")) {
          // Invalid customer ids
        client.commitTransaction();
        return true;
      }
      ((RedisPostgresClient) client).read(SmallBankConstants.kCheckingsTable, custId.toString(), SmallBankTransactionType.BALANCE.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kSavingsTable, custId.toString(), SmallBankTransactionType.BALANCE.ordinal(), this.txn_id);
//      ((RedisPostgresClient) client).readForUpdate(SmallBankConstants.kCheckingsTable, custId.toString(), SmallBankTransactionType.BALANCE.ordinal(), this.txn_id);
//      results = ((RedisPostgresClient) client).readForUpdateAndExecute(SmallBankConstants.kSavingsTable, custId.toString(), SmallBankTransactionType.BALANCE.ordinal(), this.txn_id);

      rowCheckingsCus1 = results.get(0);
      rowSavingsCus1 = results.get(1);

      balCC1 = null;
      balSC1 = null;
      try {
          balCC1 = (Integer) checkingsTable.getColumn("C_BAL", rowCheckingsCus1);
          balSC1 = (Integer) savingsTable.getColumn("S_BAL", rowSavingsCus1);
      } catch(RuntimeException e) {
          System.out.println("BROKEN CUSTOMER ID IS: " + custId.toString());
      }
      total = balSC1 + balCC1;

      client.commitTransaction();
      return true;

    } catch (DatabaseAbortException e) {
      return false;
    }
  }
}
