package shield.benchmarks.smallbank;

public class SmallBankConstants {

  /**
  * List of all possible transactions
  */
 public enum Transactions {
        AMALGAMATE,
        BALANCE,
        DEPOSIT_CHECKING,
        SEND_PAYMENT,
        TRANSACT_SAVINGS,
        WRITE_CHECK
  }

// Names of the various tables and number of columns
  public static String kAccountsTable = "accounts";
  public static int kAccountsCols  = 3;
  public static String kAccountsNameTable = "accountsname";
  public static int kAccountsName = 2;
  public static String kSavingsTable = "savings";
  public static int kSavingsCols = 3;
  public static String kCheckingsTable = "checkings";
  public static int kCheckingsCols = 3;


}
