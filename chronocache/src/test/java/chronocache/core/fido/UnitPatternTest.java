package chronocache.core.fido;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

import chronocache.core.fido.UnitPattern;

public class UnitPatternTest {
  @Test
  public void testWithinThresholdDistance() {

    UnitPattern u0 = new UnitPattern(Arrays.asList("a", "b", "c"),
                                     Arrays.asList("d", "e", "f"));

    assertTrue(u0.withinThresholdDistance(Arrays.asList("a", "b", "c"),
                                          Arrays.asList("d", "e", "f"), 1.0));
    assertTrue(u0.withinThresholdDistance(Arrays.asList("a", "b", "c"),
                                          Arrays.asList("d", "e", "f"), 0.5));

    assertTrue(u0.withinThresholdDistance(Arrays.asList("a", "x", "c"),
                                          Arrays.asList("d", "y", "f"), 0.6));
    assertFalse(u0.withinThresholdDistance(Arrays.asList("a", "x", "c"),
                                          Arrays.asList("d", "y", "f"), 0.7));
  }
}
