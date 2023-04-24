package shield.benchmarks.smallbank;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

/**
 * AmalgamateTransaction represents moving all the funds from one customer to another.
 * It reads the balances for both accounts of customer N1, then sets both to zero, and finally
 * increases the checking balance for N2 by the sum of N1’s previous balances
 */
public class AmalgamateTransaction extends BenchmarkTransaction{

  private long txn_id;
  private Integer custId1;
  private Integer custId2;

  public AmalgamateTransaction(SmallBankGenerator generator, int custId1, int custId2, long txn_id) {
    this.txn_id = txn_id;
    this.custId1= custId1;
    this.custId2 = custId2;
    this.client = generator.getClient(); }



  @Override
  public boolean tryRun() {
    try {

//      System.out.println("Amalgamate");

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
      ((RedisPostgresClient) client).read(SmallBankConstants.kAccountsTable, custId1.toString(), SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kAccountsTable, custId2.toString(), SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);

      if (results.get(0).equals("") || results.get(1).equals("")) {
          // Invalid customer ids
        client.commitTransaction();
        return true;
      }
      ((RedisPostgresClient) client).read(SmallBankConstants.kCheckingsTable, custId1.toString(), SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);
      ((RedisPostgresClient) client).read(SmallBankConstants.kSavingsTable, custId1.toString(), SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kCheckingsTable, custId2.toString(), SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);
      rowCheckingsCus1 = results.get(0);
      rowSavingsCus1 = results.get(1);
      rowCheckingsCus2 = results.get(2);

      balCC1 = (Integer) checkingsTable.getColumn("C_BAL", rowCheckingsCus1);
      balSC1= (Integer) savingsTable.getColumn("S_BAL", rowSavingsCus1);
      balCC2 = (Integer) checkingsTable.getColumn("C_BAL", rowCheckingsCus2);
      total = balSC1 + balCC1;
      balCC2+=total;
      balCC1 = 0;
      balSC1 = 0;

      checkingsTable.updateColumn("C_BAL", balCC1, rowCheckingsCus1);
      savingsTable.updateColumn("S_BAL", balSC1, rowSavingsCus1);
      checkingsTable.updateColumn("C_BAL", balCC2, rowCheckingsCus2);
      ((RedisPostgresClient) client).write(SmallBankConstants.kCheckingsTable, custId1.toString(), rowCheckingsCus1, SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);
      ((RedisPostgresClient) client).write(SmallBankConstants.kSavingsTable, custId1.toString(), rowSavingsCus1, SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);
      ((RedisPostgresClient) client).write(SmallBankConstants.kCheckingsTable, custId2.toString(), rowCheckingsCus2, SmallBankTransactionType.AMALGAMATE.ordinal(), this.txn_id);

      client.commitTransaction();

      return true;

    } catch (DatabaseAbortException e) {
      return false;
    }

  }
}
