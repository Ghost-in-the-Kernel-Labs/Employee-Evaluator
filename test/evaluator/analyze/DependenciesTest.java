package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.Goldens;
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
        expected.put("java/src/main/java/shop/Checkout.java", Set.of("java/src/main/java/shop/Cart.java"));
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

    /** E-049: Checkout names Cart, its own package's, with no import line; List and String are not beside it. */
    @Test
    void aJavaClassUsedFromItsOwnPackageIsAnEdge() {
        Import own = report().imports().get("java/src/main/java/shop/Checkout.java").stream().filter(i -> !i.names().isEmpty()).findFirst().orElseThrow();
        assertEquals(new Import("shop.*", List.of("Cart", "List", "String"), 1, List.of()), own);
        assertEquals(List.of("java/src/main/java/shop/Cart.java"), JavaImports.siblings(own.names(), "java/src/main/java/shop/Checkout.java",
                Set.of("java/src/main/java/shop/Cart.java", "java/src/main/java/shop/money/Price.java", "java/src/main/java/other/List.java")));
        assertEquals(List.of("Cart.java"), JavaImports.siblings(List.of("Cart"), "Main.java", Set.of("Cart.java", "a/Cart.java")), "the default package: the root's own");
    }

    /** Two chains of two links, Java's (through its own package, E-049) and TypeScript's; the first in path order is cited. */
    @Test
    void theLongestChainIsTheFirstOfTheTwoLongest() {
        assertEquals(List.of("java/src/main/java/shop/Checkout.java", "java/src/main/java/shop/Cart.java", "java/src/main/java/shop/money/Price.java"),
                Dependencies.graph(report(), inventory()).longest());
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
        Goldens.check(golden, json, "the graph changed: if that is intended, `gradle test -PshowGoldens`, read the diff, then `gradle test -PwriteGoldens` (E-018, E-077)");
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

    private static Map<String, Set<String>> graphOf(String... edges) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (String edge : edges) {
            String[] parts = edge.split(">");
            map.computeIfAbsent(parts[0], k -> new java.util.TreeSet<>());
            if (parts.length > 1) for (String to : parts[1].split(",")) {
                map.get(parts[0]).add(to);
                map.computeIfAbsent(to, k -> new java.util.TreeSet<>());
            }
        }
        return map;
    }

    /** Two chains of one link: the first file in path order starts it, whichever part is found later. */
    @Test
    void equalChainsGoToTheFirstFile() {
        assertEquals(List.of("a", "b"), Dependencies.longest(graphOf("a>b", "c>d")));
        assertEquals(List.of("a", "b"), Dependencies.longest(graphOf("c>d", "a>b")));
    }

    /** From a, b and c are both one link further: b, the first in path order, is the step; with c → d beyond, c is, being longer. */
    @Test
    void aStepGoesToTheFirstOfEqualNextLinksAndToALongerOneOverIt() {
        assertEquals(List.of("a", "b"), Dependencies.longest(graphOf("a>b,c")));
        assertEquals(List.of("a", "c", "d"), Dependencies.longest(graphOf("a>b,c", "c>d")));
    }

    /**
     * a → c → b → a is one part, standing for its first file a, however the edges are listed (c also leads to z):
     * the chain is a, z. The parts of the edges, worked by hand: z is found first (0), the cycle after it (1).
     */
    @Test
    void aCycleStandsForItsFirstFile() {
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        edges.put("a", Set.of("c"));
        edges.put("b", Set.of("a"));
        edges.put("c", new java.util.TreeSet<>(Set.of("b", "z")));
        edges.put("z", Set.of());
        assertEquals(List.of("a", "z"), Dependencies.longest(edges));
    }

    /** With no chain the evidence says so and the depth is 0; with one it is the chain itself, and depth is its links. */
    @Test
    void theDepthFactCitesTheChainOrSaysThereIsNone() {
        Complexity.Report none = new Complexity.Report(Map.of(), Map.of(), Map.of());
        Fact empty = Dependencies.facts(new Dependencies.Graph(Map.of(), List.of()), none, "t").get(0);
        assertEquals(0, empty.value());
        assertEquals(List.of("(no file imports another)"), empty.evidence().sources());
        Fact chain = Dependencies.facts(new Dependencies.Graph(Map.of(), List.of("a", "b", "c")), none, "t").get(0);
        assertEquals(2, chain.value());
        assertEquals(List.of("a", "b", "c"), chain.evidence().sources());
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
