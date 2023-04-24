package shield.benchmarks.smallbank;

import java.util.List;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.RedisPostgresClient;
import shield.client.schema.Table;

/**
 * AmalgamateTransaction represents moving all the funds from one customer to another.
 * It reads the balances for both accounts of customer N1, then sets both to zero, and finally
 * increases the checking balance for N2 by the sum of N1’s previous balances
 */
public class SendPaymentTransaction extends BenchmarkTransaction{

  private Integer srcCust;

  private Integer destCust;

  private Integer amount;
  private long txn_id;

  public SendPaymentTransaction(SmallBankGenerator generator, int custId1, int custId2, int amount, long txn_id) {
    this.txn_id = txn_id;
    this.srcCust= custId1;
    this.destCust= custId2;
    this.amount = amount;
     this.client = generator.getClient(); }



  @Override
  public boolean tryRun() {
    try {

      List<byte[]> results;
      byte[] src;
      byte[] dest;
      Integer srcCC;
      Integer destCC;

      Table checkingsTable= client.getTable(SmallBankConstants.kCheckingsTable);
      client.startTransaction();
      // Get Account
        ((RedisPostgresClient) client).read(SmallBankConstants.kAccountsTable, srcCust.toString(), SmallBankTransactionType.SEND_PAYMENT.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kAccountsTable, destCust.toString(), SmallBankTransactionType.SEND_PAYMENT.ordinal(), this.txn_id);

      if (results.get(0).length == 0 || results.get(1).length == 0) {
          // Invalid customer ids
        client.commitTransaction();
        return true;
      }
      ((RedisPostgresClient) client).read(SmallBankConstants.kCheckingsTable, srcCust.toString(), SmallBankTransactionType.SEND_PAYMENT.ordinal(), this.txn_id);
      results = ((RedisPostgresClient) client).readAndExecute(SmallBankConstants.kCheckingsTable, destCust.toString(), SmallBankTransactionType.SEND_PAYMENT.ordinal(), this.txn_id);
      src = results.get(0);
      dest = results.get(1);

      srcCC = (Integer) checkingsTable.getColumn("C_BAL", src);
      destCC = (Integer) checkingsTable.getColumn("C_BAL", dest);

      if (srcCC < amount) {
        // Insufficient money
//        System.out.println("Aborting, insuffient money");
        client.commitTransaction();
        return true;
      }

      checkingsTable.updateColumn("C_BAL", srcCC - amount, src);
      checkingsTable.updateColumn("C_BAL", destCC + amount, dest);
      ((RedisPostgresClient) client).write(SmallBankConstants.kCheckingsTable, srcCust.toString(), src, SmallBankTransactionType.SEND_PAYMENT.ordinal(), this.txn_id);
      ((RedisPostgresClient) client).write(SmallBankConstants.kCheckingsTable, destCust.toString(), dest, SmallBankTransactionType.SEND_PAYMENT.ordinal(), this.txn_id);

      client.commitTransaction();

      return true;

    } catch (DatabaseAbortException e) {
      return false;
    }

  }
}
