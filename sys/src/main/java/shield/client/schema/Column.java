package shield.client.schema;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import shield.util.Utility;

/**
 * Represents a column in a table structure. A column has a type, a size (if the column is a string,
 * it defaults to maxColSize), and an index
 *
 * @author ncrooks
 */
public class Column {


  private final Class<?> colType;
  private final int colSize;
  private final int colIndex;

  public Column(Class<?> type, int colIndex, int maxColSize) {
    this.colType = type;
    this.colIndex = colIndex;
    this.colSize =  maxColSize;
  }




  public int getColSize() {
    return colSize;
  }

  public int getColIndex() {
    return colIndex;
  }

  /**
   * Updates the appropriate column entry
   */
  public void setColumn(Object newColValue, byte[] value) {
    byte[] newColBytes;
    try {
      if (colType.equals(String.class)) {
        String newColValueString = (String) newColValue;
        if (newColValueString.length() > colSize) {
          throw new RuntimeException("String too long");
        }
        newColBytes = new byte[colSize];
        System.arraycopy(newColValueString.getBytes(), 0, newColBytes, 0, newColValueString.length());
      } else if (colType.equals(Long.class)) {
        Long newColValueLong = (Long) newColValue;
        newColBytes = Longs.toByteArray(newColValueLong);
        assert (newColBytes.equals(colSize));
      } else if (colType.equals(Integer.class)) {
        Integer newColValueInteger = (Integer) newColValue;
        newColBytes = Ints.toByteArray(newColValueInteger);
        assert (newColBytes.length == (colSize));
      } else {
        throw new RuntimeException("Unsupported column type");
      }
      System.arraycopy(newColBytes, 0, value, colIndex, colSize);
    } catch (ClassCastException e) {
      throw new RuntimeException("Mismatch in column types. Try to write an"
          + colType);
    }
  }

  /**
   * Returns the appropriate value (cast to the appropriate object type) from the underlying byte
   * structure
   *
   * @param value - the underlying value
   */
  public Object getColData(byte[] value) {
    byte[] col = new byte[colSize];
    System.arraycopy(value, colIndex, col, 0, colSize);
    if (colType.equals(String.class)) {
      return new String(col);
    } else if (colType.equals(Long.class)) {
      return Longs.fromByteArray(col);
    } else if (colType.equals(Integer.class)) {
      return Ints.fromByteArray(col);
    } else {
      throw new RuntimeException("Unsupported column type");
    }
  }

}


