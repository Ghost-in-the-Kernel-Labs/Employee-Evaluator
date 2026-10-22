package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.ingest.Ingest;
import evaluator.ingest.Inventory;
import evaluator.rubric.Fact;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class CouplingTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    private static Map<String, Object> values(List<Fact> facts, String kind) {
        Map<String, Object> values = new TreeMap<>();
        for (Fact f : facts) if (f.kind().equals(kind) && !f.value().equals(0)) values.put(f.subject().id(), f.value());
        return values;
    }

    /** The imports fixture's edges (DependenciesTest), counted from each end by hand; files at 0 left out. */
    @Test
    void fanInAndFanOutAsCountedByHand() {
        Path fixture = ROOT.resolve("test/fixtures/imports");
        Inventory inventory = Ingest.read(fixture);
        List<Fact> facts = Coupling.facts(Dependencies.graph(Complexity.measure(fixture, inventory), inventory));
        assertEquals(Map.of("java/src/main/java/shop/Cart.java", 1, "java/src/main/java/shop/Checkout.java", 1, "py/app/__init__.py", 1, "py/app/main.py", 2,
                "ts/src/index.ts", 4, "ts/src/view.tsx", 1), values(facts, "MAINTAINABILITY.fan_out"));
        assertEquals(Map.of("java/src/main/java/shop/Cart.java", 1, "java/src/main/java/shop/money/Price.java", 1, "py/app/util.py", 2, "py/app/config.py", 1,
                "ts/src/view.tsx", 1, "ts/src/types.ts", 2, "ts/src/polyfill.ts", 1, "ts/src/lib.ts", 1), values(facts, "MAINTAINABILITY.fan_in"));
        Fact cycles = facts.get(facts.size() - 1);
        assertEquals(0, cycles.value());
        assertEquals(List.of("(no import cycle)"), cycles.evidence().sources());
        Fact typesIn = facts.stream().filter(f -> f.key().equals("MAINTAINABILITY.fan_in file:ts/src/types.ts")).findFirst().orElseThrow();
        assertEquals(List.of("ts/src/index.ts", "ts/src/view.tsx"), typesIn.evidence().sources());
    }

    /** a and b import each other, c imports a: a and b are in a cycle, c is not. */
    @Test
    void filesInACycleAreCounted() {
        Map<String, Set<String>> edges = Map.of("a", Set.of("b"), "b", Set.of("a"), "c", Set.of("a"));
        assertEquals(List.of("a", "b"), Coupling.cyclic(edges));
        List<Fact> facts = Coupling.facts(new Dependencies.Graph(edges, List.of()));
        assertEquals(2, facts.get(facts.size() - 1).value());
    }
}
