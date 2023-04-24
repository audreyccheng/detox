package shield.client.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import shield.BaseNode;

/**
 * Local client data-structure: represents a table. Every column has a type and an index in the
 * "row" byte array. Used to easy update invidual columns, and get individual columns
 *
 * Supported types are Integer, Long, String. To add a new type, modify the convertinging functions
 * in the {@code Column} class.
 *
 * @author ncrooks
 */
public class Table {

  private final String name;
  private final Map<String, Integer> columnIndex;
  private final Map<Integer, Column> columnInfo;
  private final BaseNode node;
  private int currentRowSize;
  private int currentNbCols;


  public Table(String name, BaseNode node) {
    this.name = name;
    this.columnInfo = new HashMap<Integer, Column>();
    this.columnIndex = new HashMap<String, Integer>();
    this.node = node;
    this.currentRowSize = 0;
    this.currentNbCols = 0;
  }


  public String printColumn(byte[] value) {
    StringBuilder b = new StringBuilder();
    for (int i = 0 ; i < currentNbCols ; i++) {
      b.append(getColumn(i, value));
      b.append("//");
    }
    return b.toString();
  }

  /**
   * Ret
   *
   * @param value - the underlying byte array
   */
  public Object getColumn(String column, byte[] value) {
    Integer index = columnIndex.get(column);
    if (index== null) {
      throw new RuntimeException("Unrecognised column name");
    }
    return getColumn(index, value);
  }

  public Object getColumn(Integer column, byte[] value, long key) {
    Object result = null;
    Column col = columnInfo.get(column);
    if (col == null) {
      throw new RuntimeException("Unrecognised column name");
    }
    try {
      result = col.getColData(value);
    } catch (Exception e) {
      System.err.println("Data size " + value.length);
      System.err.println(Arrays.toString(value));
      throw new RuntimeException();
    }
    return  result;
  }
  public Object getColumn(Integer column, byte[] value) {
    Object result = null;
    Column col = columnInfo.get(column);
    if (col == null) {
      throw new RuntimeException("Unrecognised column name");
    }
    try {
      result = col.getColData(value);
    } catch (Exception e) {
      System.err.println("Data size " + value.length);
      System.err.println(Arrays.toString(value));
      throw new RuntimeException();
    }
      return  result;
  }

  /**
   * Updates the particular column (by converting it to a byte array) and updating the appropriate
   * position in the backing array.
   *
   * @param column - the index of the column
   */
  public void updateColumn(Integer column, Object value, byte[] colData) {
    Column col = columnInfo.get(column);
    if (col == null) {
      throw new RuntimeException("Unrecognised column name");
    }
    col.setColumn(value, colData);
  }


  /**
   * Updates the particular column (by converting it to a byte array) and updating the appropriate
   * position in the backing array.
   *
   * @param column - the name of the column
   */
  public void updateColumn(String column, Object value, byte[] colData) {
    Integer index = columnIndex.get(column);
    if (index == null) {
      throw new RuntimeException("Unrecognised column");
    }
    updateColumn(index, value, colData);
  }

  public String getTableName() {
    return name;
  }

  @Override
  public boolean equals(Object other) {
    Table oth = (Table) other;
    return oth.name.equals(name);
  }

  /**
   * Returns an empty byte array of the appropriate size: either of the predefined {@code
   * ORAM_VALUE_SIZE} or of the current actual size of the row
   *
   * @param fixedSize - if should create fixed size rows
   */
  public byte[] createNewRow(boolean fixedSize) {
    assert (node.getConfig().ORAM_VALUE_SIZE >= currentRowSize);
    int size = fixedSize ? node.getConfig().ORAM_VALUE_SIZE : currentRowSize;
    return new byte[size];
  }

  /**
   * Adds a column of the appropriate type at the end of the current columns
   */
  public void addColumn(ColumnInfo c) {
    int size = c.getSize() > 0? c.getSize(): node.getConfig().MAX_COLUMN_SIZE;
    Column col = new Column(c.getType(), currentRowSize, size);
    Integer index = currentNbCols++;
    columnIndex.put(c.getName(), index);
    columnInfo.put(index, col);
    int colSize = col.getColSize();
    currentRowSize += colSize;
  }


  public int getRowSize() {
    return currentRowSize;
  }
}

class StringLength<T extends Integer> {
}
