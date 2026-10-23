package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class TarjanTest {
    /** A part is numbered when it is closed: b, then c (a's imports, in order), then a. A cycle shares one number. */
    @Test
    void partsAreNumberedInTheOrderTheyAreClosed() {
        assertEquals(Map.of("b", 0, "c", 1, "a", 2), Tarjan.parts(Map.of("a", Set.of("b", "c"))));
        assertEquals(Map.of("z", 0, "a", 1, "b", 1), Tarjan.parts(Map.of("a", Set.of("b", "z"), "b", Set.of("a"))));
    }

    /** Every part after all it points to, found from any start: 0 → 1 → 2 is done as 2, 1, 0, though 0 is tried first. */
    @Test
    void orderPutsWhatAPartPointsToFirst() {
        Map<Integer, Set<Integer>> chain = new TreeMap<>();
        chain.put(0, Set.of(1));
        chain.put(1, Set.of(2));
        chain.put(2, Set.of());
        assertEquals(List.of(2, 1, 0), Tarjan.order(chain));
        Map<Integer, Set<Integer>> diamond = new TreeMap<>();
        diamond.put(0, new java.util.TreeSet<>(Set.of(1, 2)));
        diamond.put(1, Set.of(3));
        diamond.put(2, Set.of(3));
        diamond.put(3, Set.of());
        assertEquals(List.of(3, 1, 2, 0), Tarjan.order(diamond));
    }
}
