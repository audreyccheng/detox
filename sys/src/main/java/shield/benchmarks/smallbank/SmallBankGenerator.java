package shield.benchmarks.smallbank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import shield.benchmarks.smallbank.SmallBankConstants.Transactions;

import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.benchmarks.utils.TrxStats;
import shield.client.RedisPostgresClient;
import shield.client.schema.ColumnInfo;

public class SmallBankGenerator {

  private final SmallBankExperimentConfiguration config;

  private final RedisPostgresClient client;
  private final HashMap<Transactions, TrxStats> trxStats;
  private final char[] ALPHANUM =  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
  private final int nbAccounts;
  private static long txn_id = 0;

  public SmallBankGenerator(RedisPostgresClient client, SmallBankExperimentConfiguration config) {
    this.config = config;
    this.client = client;
    this.trxStats = new HashMap<>();
    for (SmallBankConstants.Transactions tType: SmallBankConstants.Transactions.values()) {
      trxStats.put(tType, new TrxStats());
    }
    createSmallbankTables(client);
    this.nbAccounts = config.HOTSPOT_USE_FIXED_SIZE
        ? config.HOTSPOT_FIXED_SIZE:
            (int) (config.NB_ACCOUNTS);
//        (int) (config.NB_ACCOUNTS/config.HOTSPOT_PERCENTAGE);
  }

  public RedisPostgresClient getClient() {
    return client;
  }

  private void createSmallbankTables(RedisPostgresClient client) {

      client.createTable(SmallBankConstants.kAccountsTable,
          new ColumnInfo("A_CUST_ID", Integer.class),
          new ColumnInfo("A_CUST_NAME", String.class, config.NAME_SIZE),
          new ColumnInfo("A_DATA", String.class, config.VAR_DATA_SIZE));
      client.createTable(SmallBankConstants.kSavingsTable,
          new ColumnInfo("S_CUST_ID", Integer.class),
          new ColumnInfo("S_BAL", Integer.class),
          new ColumnInfo("S_DATA", String.class, config.VAR_DATA_SIZE)
          );
      client.createTable(SmallBankConstants.kCheckingsTable,
          new ColumnInfo("C_CUST_ID", Integer.class),
          new ColumnInfo("C_BAL", Integer.class),
          new ColumnInfo("C_DATA", String.class, config.VAR_DATA_SIZE));

  }

  public void runNextTransaction() {
    int x = Generator.generateInt(0,100);
    int nbAborts;
    long begin = System.currentTimeMillis();
    long end = 0;
    BenchmarkTransaction trx;
    if (x < config.PROB_TRX_AMALGAMATE) {
      trx = GenerateAmalgamateInput();
//      System.out.println("[" + Transactions.AMALGAMATE+ "] Begin");
      nbAborts = trx.run();
      end = System.currentTimeMillis();
      trxStats.get(Transactions.AMALGAMATE).addTransaction(nbAborts, end-begin);
//      System.out.println("[" + Transactions.AMALGAMATE + "] End");
    }
    else if (x < config.PROB_TRX_AMALGAMATE+ config.PROB_TRX_TRANSACT_SAVINGS) {
      trx = GenerateTransactSavingsInput();
//      System.out.println("[" + Transactions.TRANSACT_SAVINGS+ "] Begin");
      nbAborts = trx.run();
      end = System.currentTimeMillis();
//       System.out.println("[" + Transactions.TRANSACT_SAVINGS+ "] End");
      trxStats.get(Transactions.TRANSACT_SAVINGS).addTransaction(nbAborts, end-begin);
    } else if (x < config.PROB_TRX_AMALGAMATE + config.PROB_TRX_TRANSACT_SAVINGS+
        config.PROB_TRX_SEND_PAYMENT) {
      trx = GenerateSendPaymentInput();
//      System.out.println("[" + Transactions.SEND_PAYMENT + "] Begin");
      nbAborts = trx.run();
      end = System.currentTimeMillis();
//       System.out.println("[" + Transactions.SEND_PAYMENT+ "] End");
      trxStats.get(Transactions.SEND_PAYMENT).addTransaction(nbAborts, end-begin);
    } else if (x < config.PROB_TRX_AMALGAMATE + config.PROB_TRX_TRANSACT_SAVINGS+
        config.PROB_TRX_SEND_PAYMENT + config.PROB_TRX_BALANCE){
      trx = GenerateBalanceInput();
//      System.out.println("[" + Transactions.BALANCE+ "] Begin");
      nbAborts = trx.run();
      end = System.currentTimeMillis();
//       System.out.println("[" + Transactions.BALANCE+ "] End");
      trxStats.get(Transactions.BALANCE).addTransaction(nbAborts, end-begin);
    } else if (x < config.PROB_TRX_AMALGAMATE + config.PROB_TRX_TRANSACT_SAVINGS+
        config.PROB_TRX_SEND_PAYMENT + config.PROB_TRX_BALANCE +
        config.PROB_TRX_DEPOSIT_CHECKING){
      trx = GenerateDepositCheckingInput();
//      System.out.println("[" + Transactions.DEPOSIT_CHECKING+ "] Begin");
      nbAborts = trx.run();
      end = System.currentTimeMillis();
//       System.out.println("[" + Transactions.DEPOSIT_CHECKING+ "] End");
      trxStats.get(Transactions.DEPOSIT_CHECKING).addTransaction(nbAborts,end-begin);
    } else {
      trx = GenerateWriteCheckTransaction();
//      System.out.println("[" + Transactions.WRITE_CHECK+ "] Begin");
      nbAborts = trx.run();
      end = System.currentTimeMillis();
//       System.out.println("[" + Transactions.WRITE_CHECK+ "] End");
      trxStats.get(Transactions.WRITE_CHECK).addTransaction(nbAborts,end-begin);
    }
  }

  public AmalgamateTransaction GenerateAmalgamateInput() {
    int custId1 = generateCustomerAccount();
    int custId2 = custId1;
    while (custId2==custId1) {
      custId2 = generateCustomerAccount();
    }
    return new AmalgamateTransaction(this,custId1, custId2, txn_id++);
  }

  public BalanceTransaction GenerateBalanceInput() {
    int custId1 = generateCustomerAccount();
    return new BalanceTransaction(this, custId1, txn_id++);
  }

  public DepositCheckingTransaction GenerateDepositCheckingInput() {
    int custId = generateCustomerAccount();
    return new DepositCheckingTransaction(this, custId, config.PARAM_DEPOSIT_CHECKING_AMOUNT, txn_id++);
  }

  public SendPaymentTransaction GenerateSendPaymentInput() {
    int custId1 = generateCustomerAccount();
    int custId2 = custId1;
    while (custId2==custId1) {
      custId2 = generateCustomerAccount();
    }
    return new SendPaymentTransaction(this,custId1, custId2, config.PARAM_SEND_PAYMENT_AMOUNT, txn_id++);
  }

  public TransactSavingsTransaction GenerateTransactSavingsInput() {
    int custId = generateCustomerAccount();
    return new TransactSavingsTransaction(this, custId, config.PARAM_TRANSACT_SAVINGS_AMOUNT, txn_id++);
  }

  public WriteCheckTransaction GenerateWriteCheckTransaction() {
    int custId = generateCustomerAccount();
    return new WriteCheckTransaction(this,custId, config.PARAM_WRITE_CHECK_AMOUNT, txn_id++);
  }


  public HashMap<Transactions, TrxStats> getTrxStats() {
    return trxStats;
  }

  public char RandCharNum(boolean numOnly) {
    int x = Generator.generateInt(0,numOnly?10:26);
    return ALPHANUM[x];
  }

  /**
   * Generates a random string of size between min and max, and optinally consisting
   * of numbers only
   *
   * @param num_only
   * @return
   */
  public String RandString(int min, int max, boolean num_only) {
    StringBuffer bf = new StringBuffer();
    int len = Generator.generateInt(min, max);
    for (int i = 0; i < len; ++i) {
      bf.append(RandCharNum(num_only));
    }
    return bf.toString();
  }


  public int generateCustomerAccount() {

      int clientId;
      int ran;

//      ran = Generator.generateInt(0,100);
//      boolean useHotSpot = Generator.generateInt(0, 100)
//          < config.PROB_ACCOUNT_HOTSPOT;
//      if (useHotSpot) {
//          ran = Generator.generateInt(0, nbAccounts);
//          clientId = ran;
//      } else {
          ran =  Generator.generateInt(0,config.NB_ACCOUNTS);
          clientId = ran;
//      }
//      System.out.println("Account to read from is " + clientId);
      return clientId;
  }

  public void printStats() {
    trxStats.forEach((tType,stat) -> System.out.println("[STAT] " + tType + " " +  stat.getStats()));
  }


}
