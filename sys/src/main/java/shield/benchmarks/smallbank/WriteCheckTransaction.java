package shield.benchmarks.smallbank;

import java.util.List;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

/**
 * Represents writing a check against an account. Its operation is evaluate
 * the sum of savings and checking balances for the given customer. If the sum is less than
 * V, it decreases the checking balance by V + 1 (reflecting a penalty of $1 for overdrawing),
 * otherwise it decreases the checking balance by V
 */
public class WriteCheckTransaction extends BenchmarkTransaction{

  private Integer custId1;
  private Integer amount;
  private long txn_id;

  public WriteCheckTransaction(SmallBankGenerator generator, int custId1, int amount, long txn_id) {
    this.txn_id = txn_id;
    this.custId1= custId1;
    this.amount = amount;
    this.client = generator.getClient();}



  @Override
  public boolean tryRun() {
    try {

//      System.out.println("Write Check Transaction");

      List<byte[]> results;
      byte[] rowSavingsCus1;
      byte[] rowCheckingsCus1;
      byte[] rowCheckingsCus2;
      Integer balCC2;
      Integer balCC1;
      Integer balSC1;
      Integer total;

      Table checkingsTable = client.getTable(SmallBankConstants.kCheckingsTable);
      Table savingsTable = client.getTable(SmallBankConstants.kSavingsTable);
      client.startTransaction();
      // Get Account
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kAccountsTable, custId1.toString(), SmallBankTransactionType.WRITE_CHECK.ordinal(), this.txn_id);
      if (results.get(0).length == 0) {
          // Invalid customer ids
//        System.out.println("Incorrect Customer Id " + custId1.toString());
          client.commitTransaction();
      }
      ((RedisPostgresClient) client).read(SmallBankConstants.kCheckingsTable, custId1.toString(), SmallBankTransactionType.WRITE_CHECK.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kSavingsTable, custId1.toString(), SmallBankTransactionType.WRITE_CHECK.ordinal(), this.txn_id);
      rowCheckingsCus1 = results.get(0);
      rowSavingsCus1 = results.get(1);

      balCC1 = (Integer) checkingsTable.getColumn("C_BAL", rowCheckingsCus1);
      balSC1= (Integer) savingsTable.getColumn("S_BAL", rowSavingsCus1);
      total = balSC1 + balCC1;
      if (total < amount) {
        // Pay a penalty fee
        balCC1 = balCC1 - (amount + 1);
      } else {
        balCC1 = balCC1 - amount;
      }

      checkingsTable.updateColumn("C_BAL", balCC1, rowCheckingsCus1);

      ((RedisPostgresClient) client).write(SmallBankConstants.kCheckingsTable, custId1.toString(), rowCheckingsCus1, SmallBankTransactionType.WRITE_CHECK.ordinal(), this.txn_id);

      client.commitTransaction();

      return true;

    } catch (DatabaseAbortException e) {
      return false;
    }

  }
}
