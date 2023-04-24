package shield.client.schema;

import static shield.util.Utility.toBytes;

public class ColumnInfo {

  private final String name;
  private final Class<?> type;
  private final int size;

  public ColumnInfo(String colName, Class<?> columnType, int colSize) {

    this.name = colName;
    this.type = columnType;

    if (type.equals(String.class)) {
      this.size = colSize;
    } else if (type.equals(Long.class)) {
      this.size = toBytes(Long.SIZE);
    } else if (type.equals(Integer.class)) {
      this.size = toBytes(Integer.SIZE);
    } else {
      throw new RuntimeException("Unsupported column type");
    }

  }

  public ColumnInfo(String colName, Class<?> columnType) {
      this(colName, columnType, 0);
  }

  public String getName() {
    return name;
  }

  public int getSize() {
    return size;
  }

  public Class<?> getType() {
    return type;
  }
}




