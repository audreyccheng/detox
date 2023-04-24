package shield.util;

import java.util.Collection;
import java.util.stream.Collectors;

public class StringUtils {

  /**
   * Generic method for printing out contents of a collection (assuming that the element in the
   * collection has a sensible toString() method
   *
   * @return the printed string
   */
  public static String join(Collection<?> collection, String delimiter) {
    return (String) collection.stream().map(Object::toString)
        .collect(Collectors.joining(delimiter));
  }
}
