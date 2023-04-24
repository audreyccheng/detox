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
public class DepositCheckingTransaction extends BenchmarkTransaction{

  private Integer custId;
  private Integer amount;
  private long txn_id;

  public DepositCheckingTransaction(SmallBankGenerator generator, int custId, int amount, long txn_id) {
    this.txn_id = txn_id;
    this.custId= custId;
    this.amount = amount;
    this.client = generator.getClient(); }



  @Override
  public boolean tryRun() {
  try {

//      System.out.println("Deposit Checking Transaction");

      List<byte[]> results;
      byte[] rowCheckingsCus1;
      Integer balCC1;

      Table checkingsTable = client.getTable(SmallBankConstants.kCheckingsTable);

      client.startTransaction();
      // Get Account
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kAccountsTable, custId.toString(), SmallBankTransactionType.DEPOSIT_CHECKING.ordinal(), this.txn_id);

      if (results.get(0).equals("")) {
          // Invalid customer ids
        client.commitTransaction();
        return true;
      }
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kCheckingsTable, custId.toString(), SmallBankTransactionType.DEPOSIT_CHECKING.ordinal(), this.txn_id);
      rowCheckingsCus1 = results.get(0);
      balCC1 = (Integer) checkingsTable.getColumn("C_BAL", rowCheckingsCus1);
      checkingsTable.updateColumn("C_BAL", balCC1 - amount, rowCheckingsCus1);
      ((RedisPostgresClient) client).write(SmallBankConstants.kCheckingsTable,custId.toString(), rowCheckingsCus1, SmallBankTransactionType.DEPOSIT_CHECKING.ordinal(), this.txn_id);
      client.commitTransaction();
      return true;

    } catch (DatabaseAbortException e) {
      return false;
    }
  }
}
