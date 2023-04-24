package shield.tests;

import java.io.IOException;

import org.json.simple.parser.ParseException;

import shield.client.Client;
import shield.client.schema.ColumnInfo;
import shield.client.schema.Table;
import shield.util.Pair;

public class SchemaTest {

  public static void main(String[] args) throws InterruptedException, IOException, ParseException {

    Client client = new Client();

    Table firstTable = client.createTable("Hello", new ColumnInfo("Col1", Integer.class),
        new ColumnInfo("Col2", Long.class),
        new ColumnInfo("Col3", String.class ));

    byte[] array = firstTable.createNewRow(false);
    assert (array.length ==
        (Integer.SIZE + Long.SIZE + client.getConfig().MAX_COLUMN_SIZE));
    byte[] array2 = firstTable.createNewRow(true);
    assert (array2.length == client.getConfig().ORAM_VALUE_SIZE);

    firstTable.updateColumn("Col1", new Integer(1), array);
    firstTable.updateColumn("Col1", new Integer(2), array2);
    Integer res = (Integer) firstTable.getColumn("Col1", array);
    Integer res2 = (Integer) firstTable.getColumn("Col1", array2);
    assert (res.equals(1));
    assert (res2.equals(2));

    firstTable.updateColumn("Col3", new String("HelloWorld"), array);
    String res3 = (String) firstTable.getColumn("Col3", array);
    assert (res3.equals("HelloWorld"));

    System.out.println("Test passed");
    System.exit(0);
  }
}
