package shield.benchmarks.taobench;

public class TaoBenchConstants {
    /**
     * List of all possible transactions
     */
    public enum Transactions {
        READ,
        SCAN,
        WRITE,
        READTRANSACTION,
        WRITETRANSACTION,
    }

    // Names of the various tables and number of columns
    public static String kObjectsTable = "objects";
    public static int kObjectsCols  = 3;
    public static String kEdgesTable = "edges";
    public static int kEdgesCol = 5;

}