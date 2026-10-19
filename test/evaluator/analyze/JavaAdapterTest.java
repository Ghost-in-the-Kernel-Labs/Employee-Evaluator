package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.ingest.Ingest;
import evaluator.ingest.Inventory;
import evaluator.rubric.Fact;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JavaAdapterTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final Path FIXTURE = ROOT.resolve("test/fixtures/java-complexity");
    private static final Path GOLDEN = ROOT.resolve("test/goldens/java-complexity/complexity.json");

    private static Complexity.Report report() {
        return Complexity.measure(FIXTURE, Ingest.read(FIXTURE));
    }

    /** Each function's cyclomatic and cognitive complexity, worked by hand (the fixture's README). */
    @Test
    void eachFunctionAsWorkedByHand() {
        Map<String, List<Integer>> expected = new LinkedHashMap<>();
        expected.put("Examples.Examples", List.of(2, 1));
        expected.put("Examples.sumOfPrimes", List.of(4, 7));
        expected.put("Examples.getWords", List.of(4, 1));
        expected.put("Examples.grade", List.of(3, 3));
        expected.put("Examples.logic", List.of(6, 4));
        expected.put("Examples.nested", List.of(4, 6));
        expected.put("Examples.tryIt", List.of(3, 3));
        expected.put("Examples.run", List.of(1, 0));
        expected.put("Examples.run#2", List.of(1, 0));
        expected.put("Examples.later", List.of(2, 2));
        expected.put("Examples.fact", List.of(2, 2));
        expected.put("Examples.kind", List.of(3, 1));
        expected.put("Examples.anon", List.of(1, 0));
        expected.put("Examples.<anonymous>.run", List.of(2, 1));
        expected.put("Examples.Inner.go", List.of(2, 1));
        Map<String, List<Integer>> measured = new LinkedHashMap<>();
        for (Measure m : report().functions().get("src/Examples.java")) measured.put(m.function(), List.of(m.cyclomatic(), m.cognitive()));
        assertEquals(expected, measured);
    }

    @Test
    void aFileThatDoesNotParseIsSaidNotGuessed() {
        assertEquals(Map.of("src/Broken.java", "does not parse: line 2"), report().unread());
        assertNull(report().functions().get("src/Broken.java"));
    }

    @Test
    void theFixturesComplexityIsItsGolden() throws IOException {
        String json = Complexity.toJson(report());
        if (Boolean.getBoolean("evaluator.writeGoldens")) Files.writeString(GOLDEN, json);
        assertEquals(Files.readString(GOLDEN), json, "the complexity changed: if that is intended, `gradle test -PwriteGoldens` and review the diff (E-018)");
    }

    @Test
    void twoFactsPerFunctionCitingFileAndLine() {
        Inventory inventory = Ingest.read(FIXTURE);
        List<Fact> facts = Complexity.facts(report(), inventory);
        assertEquals(30, facts.size());
        Fact first = facts.get(0);
        assertEquals("COMPLEXITY.cyclomatic function:src/Examples.java:Examples.Examples", first.key());
        assertEquals(List.of("src/Examples.java:6"), first.evidence().sources());
        assertTrue(first.evidence().tool().startsWith("evaluator analyze java 1"));
        assertEquals("COMPLEXITY.cognitive", facts.get(1).kind());
    }

    @Test
    void aLanguageNoAdapterReadsIsNotMeasured() {
        Path smallApp = ROOT.resolve("test/fixtures/small-app");
        Complexity.Report report = Complexity.measure(smallApp, Ingest.read(smallApp));
        assertEquals(List.of("lib/Report.java"), List.copyOf(report.functions().keySet()));
    }
}
