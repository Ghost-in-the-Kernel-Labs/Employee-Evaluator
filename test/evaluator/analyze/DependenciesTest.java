package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.ingest.Ingest;
import evaluator.ingest.Inventory;
import evaluator.rubric.Fact;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DependenciesTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final Path FIXTURE = ROOT.resolve("test/fixtures/imports");

    private static Inventory inventory() {
        return Ingest.read(FIXTURE);
    }

    private static Complexity.Report report() {
        return Complexity.measure(FIXTURE, inventory());
    }

    /** Every file's edges into the repository, worked by hand from the fixture's imports. */
    @Test
    void eachImportLeadsWhereItSays() {
        Map<String, Set<String>> expected = new LinkedHashMap<>();
        expected.put("java/src/main/java/shop/Cart.java", Set.of("java/src/main/java/shop/money/Price.java"));
        expected.put("java/src/main/java/shop/money/Price.java", Set.of());
        expected.put("py/app/__init__.py", Set.of("py/app/util.py"));
        expected.put("py/app/config.py", Set.of());
        expected.put("py/app/main.py", Set.of("py/app/util.py", "py/app/config.py"));
        expected.put("py/app/util.py", Set.of());
        expected.put("ts/src/index.ts", Set.of("ts/src/view.tsx", "ts/src/types.ts", "ts/src/polyfill.ts", "ts/src/lib.ts"));
        expected.put("ts/src/lib.ts", Set.of());
        expected.put("ts/src/polyfill.ts", Set.of());
        expected.put("ts/src/types.ts", Set.of());
        expected.put("ts/src/view.tsx", Set.of("ts/src/types.ts"));
        Map<String, Set<String>> edges = Dependencies.graph(report(), inventory()).edges();
        assertEquals(expected, edges.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue()))));
    }

    @Test
    void theLongestChainIsTheTypeScriptOne() {
        assertEquals(List.of("ts/src/index.ts", "ts/src/view.tsx", "ts/src/types.ts"), Dependencies.graph(report(), inventory()).longest());
    }

    /** Unused: Java's Map; Python's json and unused_helper (not in __init__.py, which re-exports); TypeScript's fs and Default. */
    @Test
    void unusedImportsAreFactsAgainstTheirFile() {
        List<Fact> facts = Dependencies.facts(Dependencies.graph(report(), inventory()), report(), "test");
        assertEquals(2, facts.get(0).value());
        assertEquals("COMPLEXITY.dependency_depth repository:.", facts.get(0).key());
        Map<String, Object> unused = new LinkedHashMap<>();
        for (Fact f : facts.subList(1, facts.size())) if (!f.value().equals(0)) unused.put(f.subject().id(), f.value());
        assertEquals(Map.of("java/src/main/java/shop/Cart.java", 1, "py/app/main.py", 2, "ts/src/index.ts", 2), unused);
        Fact cart = facts.stream().filter(f -> f.subject().id().equals("java/src/main/java/shop/Cart.java")).findFirst().orElseThrow();
        assertEquals(List.of("java/src/main/java/shop/Cart.java:5 Map"), cart.evidence().sources());
    }

    @Test
    void theGraphIsItsGolden() throws IOException {
        Path golden = ROOT.resolve("test/goldens/imports/dependencies.json");
        String json = Dependencies.toJson(Dependencies.graph(report(), inventory()), report());
        if (Boolean.getBoolean("evaluator.writeGoldens")) Files.writeString(golden, json);
        assertEquals(Files.readString(golden), json, "the graph changed: if that is intended, `gradle test -PwriteGoldens` and review the diff (E-018)");
    }

    /** A cycle is one link: a → b → a → c is a chain of two parts, so depth 1, never a loop. */
    @Test
    void aCycleIsOneLinkOfAChain() {
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        edges.put("a", Set.of("b"));
        edges.put("b", Set.of("a", "c"));
        edges.put("c", Set.of());
        assertEquals(List.of("a", "c"), Dependencies.longest(edges));
        assertEquals(List.of(), Dependencies.longest(Map.of("x", Set.of())));
    }

    @Test
    void aDeepChainDoesNotExhaustTheStack() {
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        for (int i = 0; i < 100_000; i++) edges.put("f%06d".formatted(i), Set.of("f%06d".formatted(i + 1)));
        assertEquals(100_001, Dependencies.longest(edges).size());
    }

    @Test
    void aPathOutOfTheRepositoryLeadsNowhere() {
        assertEquals(List.of(), TypeScriptImports.resolve(new Import("../../../etc/passwd", List.of(), 1, List.of()), "a.ts", Set.of("etc/passwd")));
        assertEquals(List.of(), PythonImports.resolve(new Import("...x", List.of(), 1, List.of()), "a/b.py", Set.of("x.py")));
    }
}
