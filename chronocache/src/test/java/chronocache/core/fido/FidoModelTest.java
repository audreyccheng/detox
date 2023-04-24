package chronocache.core.fido;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;

import chronocache.core.fido.FidoIndex;
import chronocache.core.fido.FidoModel;
import chronocache.core.fido.UnitPattern;

public class FidoModelTest {

  private static void checkMatches(List<FidoIndex> indices,
                                   List<String> pattern,
                                   ImmutableMap<Integer, Integer> expected) {
    Multiset<Integer> actual = FidoModel.findNeighbours(pattern, indices);

    assertEquals(actual.elementSet().size(), expected.size());

    for (Map.Entry<Integer, Integer> entry : expected.entrySet()) {
      assertEquals((int)actual.count(entry.getKey()), (int)entry.getValue());
    }
  }

  private static void checkOrderedMatches(List<FidoIndex> indices,
                                          List<String> pattern,
                                          List<Integer> expected) {
    List<Integer> actual = FidoModel.getOrderedNeighbours(pattern, indices);
    assertEquals(actual.size(), expected.size());

    for (int i = 0; i < actual.size(); i++) {
      assertEquals((int)actual.get(i), (int)expected.get(i));
    }
  }

  private static void checkPrediction(FidoModel model, List<String> pattern,
                                      List<String> expected) {
      List<String> actual = model.makePrediction(pattern, expected.size());

      assertEquals(actual.size(), expected.size());

      for (int i = 0; i < expected.size(); i++) {
        assertEquals(actual.get(i), expected.get(i));
      }
  }

  @Test
  public void testFindNeighbours() {
    FidoIndex index0 = new FidoIndex();
    FidoIndex index1 = new FidoIndex();
    FidoIndex index2 = new FidoIndex();

    // Pattern 0: "a, b, c"
    assertTrue(index0.addIndexElement("a", 0));
    assertTrue(index1.addIndexElement("b", 0));
    assertTrue(index2.addIndexElement("c", 0));

    // Pattern 1: "d, b, c"
    assertTrue(index0.addIndexElement("d", 1));
    assertTrue(index1.addIndexElement("b", 1));
    assertTrue(index2.addIndexElement("e", 1));

    // Pattern 2: "a, f, c"
    assertTrue(index0.addIndexElement("a", 2));
    assertTrue(index1.addIndexElement("f", 2));
    assertTrue(index2.addIndexElement("c", 2));

    List<FidoIndex> indices = Arrays.asList(index0, index1, index2);

    List<String> noMatches = Arrays.asList("x", "y", "z");
    ImmutableMap<Integer, Integer> noMatchCount =
        ImmutableMap.of(); // no matches
    List<Integer> noMatchOrdered = Arrays.asList();
    checkMatches(indices, noMatches, noMatchCount);
    checkOrderedMatches(indices, noMatches, noMatchOrdered);

    List<String> matchesAll = Arrays.asList("a", "b", "c");
    ImmutableMap<Integer, Integer> matchesAllCount =
        ImmutableMap.<Integer, Integer>builder()
            .put(0, 3) /*a, b, c*/
            .put(1, 1) /* -, b, - */
            .put(2, 2) /* a, -, c*/
            .build();
    List<Integer> matchesAllOrdered = Arrays.asList(0, 2, 1);
    checkMatches(indices, matchesAll, matchesAllCount);
    checkOrderedMatches(indices, matchesAll, matchesAllOrdered);

    List<String> matches0And2 = Arrays.asList("a", "y", "c");
    ImmutableMap<Integer, Integer> matches0And2Count =
        ImmutableMap.<Integer, Integer>builder()
            .put(0, 2) /*a, -, c*/
            .put(2, 2) /* a, -, c*/
            .build();
    checkMatches(indices, matches0And2, matches0And2Count);
    // Cant' check here as things are tied...

    List<String> checkShuffled = Arrays.asList("b", "c", "a");
    checkMatches(indices, checkShuffled, noMatchCount);
    checkOrderedMatches(indices, checkShuffled, noMatchOrdered);

    List<String> match2 = Arrays.asList("x", "f", "y");
    ImmutableMap<Integer, Integer> match2Count =
        ImmutableMap.<Integer, Integer>builder()
            .put(2, 1) /*-, f, -*/
            .build();
    List<Integer> match2Ordered = Arrays.asList(2);
    checkMatches(indices, match2, match2Count);
    checkOrderedMatches(indices, match2, match2Ordered);
  }

  @Test
  public void testPrediction() {
    FidoIndex index0 = new FidoIndex();
    FidoIndex index1 = new FidoIndex();
    FidoIndex index2 = new FidoIndex();

    // Pattern 0: "a, b, c" -- "d, e, f"
    assertTrue(index0.addIndexElement("a", 0));
    assertTrue(index1.addIndexElement("b", 0));
    assertTrue(index2.addIndexElement("c", 0));

    UnitPattern u0 = new UnitPattern(Arrays.asList("a", "b", "c"),
            Arrays.asList("d", "e", "f"));

    // Pattern 1: "d, b, c" -- "d, e, g"
    assertTrue(index0.addIndexElement("d", 1));
    assertTrue(index1.addIndexElement("b", 1));
    assertTrue(index2.addIndexElement("e", 1));

    UnitPattern u1 = new UnitPattern(Arrays.asList("d", "b", "e"),
            Arrays.asList("d", "e", "g"));

    // Pattern 2: "a, f, c" -- "h, e, g"
    assertTrue(index0.addIndexElement("a", 2));
    assertTrue(index1.addIndexElement("f", 2));
    assertTrue(index2.addIndexElement("c", 2));

    UnitPattern u2 = new UnitPattern(Arrays.asList("a", "f", "c"),
            Arrays.asList("h", "e", "g"));

    List<FidoIndex> indices = Arrays.asList(index0, index1, index2);
    List<UnitPattern> unitPatterns = Arrays.asList(u0, u1, u2);

    // simple query map for simplicity
    Map<String, String> queryMap = new HashMap<String, String>();
    queryMap.put("a", "a");
    queryMap.put("b", "b");
    queryMap.put("c", "c");
    queryMap.put("d", "d");
    queryMap.put("e", "e");
    queryMap.put("f", "f");
    queryMap.put("g", "g");
    queryMap.put("h", "h");

    FidoModel model = new FidoModel(3, indices, unitPatterns, queryMap);

    List<String> noPrediction = Arrays.asList("x", "y", "z");
    List<String> noPredictionExpect = Arrays.asList();
    checkPrediction(model, noPrediction, noPredictionExpect);

    List<String> matchesAll = Arrays.asList("a", "b", "c");
    // expect u0, then u2, then u1
    // which means d, e, f, then, h, e, g, then d, e, f
    // when filter this to uniques get d, e, f, h, g
    List<String> matchesAllExpect = Arrays.asList("d", "e", "f", "h", "g");
    checkPrediction(model, matchesAll, matchesAllExpect);
    List<String> matchesAllExpectShort = Arrays.asList("d", "e", "f");
    checkPrediction(model, matchesAll, matchesAllExpectShort);

    List<String> match2 = Arrays.asList("x", "f", "y");
    // expect u2, which means only, h, e, g
    List<String> match2Expect = Arrays.asList("h", "e", "g");
    checkPrediction(model, match2, match2Expect);

    // Pattern 1: "a, b, c" -- "d, e, f"
    // Pattern 2: "d, b, c" -- "d, e, g"
    // Pattern 3: "a, f, c" -- "h, e, g"
  }
}
