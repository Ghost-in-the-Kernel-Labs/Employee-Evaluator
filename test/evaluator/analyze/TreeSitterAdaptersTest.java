package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.Goldens;
import evaluator.ingest.Ingest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The Python, TypeScript and JavaScript adapters on their fixtures: each function's values worked by hand, as the Java fixture's are. */
class TreeSitterAdaptersTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    private static Complexity.Report report(String fixture) {
        Path root = ROOT.resolve("test/fixtures/" + fixture);
        return Complexity.measure(root, Ingest.read(root));
    }

    private static Map<String, List<Integer>> measured(Complexity.Report report, String path) {
        Map<String, List<Integer>> measured = new LinkedHashMap<>();
        for (Measure m : report.functions().get(path)) measured.put(m.function(), List.of(m.cyclomatic(), m.cognitive()));
        return measured;
    }

    private static void golden(String fixture, Complexity.Report report) throws IOException {
        Path golden = ROOT.resolve("test/goldens/" + fixture + "/complexity.json");
        String json = Complexity.toJson(report);
        Goldens.check(golden, json, "the complexity changed: if that is intended, `gradle test -PshowGoldens`, read the diff, then `gradle test -PwriteGoldens` (E-018, E-077)");
    }

    /** As the Java fixture, but no labelled continue (sum_of_primes is 6), and a comprehension's if is +1 flat. */
    @Test
    void pythonAsWorkedByHand() {
        Map<String, List<Integer>> expected = new LinkedHashMap<>();
        expected.put("sum_of_primes", List.of(4, 6));
        expected.put("get_words", List.of(4, 1));
        expected.put("grade", List.of(3, 3));
        expected.put("logic", List.of(6, 4));
        expected.put("nested", List.of(4, 6));
        expected.put("try_it", List.of(3, 3));
        expected.put("run", List.of(1, 0));
        expected.put("later", List.of(2, 2));
        expected.put("fact", List.of(2, 2));
        expected.put("evens", List.of(3, 1));
        expected.put("outer", List.of(2, 0));
        expected.put("outer.inner", List.of(2, 1));
        expected.put("Shape.area", List.of(2, 1));
        expected.put("Shape.area#2", List.of(1, 0));
        assertEquals(expected, measured(report("python-complexity"), "shapes.py"));
    }

    /** As the Java fixture; a named arrow function is its own function, a callback counts one level deeper. */
    @Test
    void typescriptAsWorkedByHand() {
        Map<String, List<Integer>> expected = new LinkedHashMap<>();
        expected.put("sumOfPrimes", List.of(4, 7));
        expected.put("getWords", List.of(4, 1));
        expected.put("grade", List.of(3, 3));
        expected.put("logic", List.of(6, 4));
        expected.put("Store.nested", List.of(4, 6));
        expected.put("Store.load", List.of(3, 3));
        expected.put("later", List.of(2, 2));
        expected.put("fact", List.of(2, 2));
        expected.put("countdown", List.of(2, 1));
        assertEquals(expected, measured(report("typescript-complexity"), "src/examples.ts"));
        assertEquals(Map.of("Badge", List.of(2, 1)), measured(report("typescript-complexity"), "src/view.tsx"));
    }

    /**
     * E-058: TypeScript's examples without their types give TypeScript's values. onRequest: if, catch and ||
     * (?? is no decision) make cyclomatic 4; if, catch and the || run make cognitive 3. total: one ternary.
     */
    @Test
    void javascriptAsWorkedByHand() {
        Complexity.Report report = report("javascript-complexity");
        assertEquals(measured(report("typescript-complexity"), "src/examples.ts"), measured(report, "src/examples.js"));
        assertEquals(Map.of("onRequest", List.of(4, 3)), measured(report, "src/handler.mjs"));
        assertEquals(Map.of("total", List.of(2, 1)), measured(report, "src/legacy.cjs"));
        assertEquals(Map.of("Badge", List.of(2, 1)), measured(report, "src/view.jsx"));
        assertEquals(Map.of("src/broken.js", "does not parse: line 1"), report.unread());
    }

    /** ESM imports and requires, their unused names, and where they lead with JavaScript's endings. */
    @Test
    void javascriptImportsAndRequires() {
        Complexity.Report report = report("javascript-complexity");
        assertEquals(List.of(new Import("./examples.js", List.of(), 1, List.of()), new Import("./examples.js", List.of(), 2, List.of("neverUsed"))),
                report.imports().get("src/handler.mjs"));
        assertEquals(List.of(new Import("./examples", List.of(), 1, List.of()), new Import("path", List.of(), 2, List.of("path")), new Import("./view", List.of(), 3, List.of())),
                report.imports().get("src/legacy.cjs"));
        java.util.Set<String> files = java.util.Set.of("src/examples.js", "src/view.jsx", "src/legacy.cjs");
        JavaScriptAdapter js = new JavaScriptAdapter();
        assertEquals(List.of("src/examples.js"), js.resolve(new Import("./examples", List.of(), 1, List.of()), "src/legacy.cjs", files));
        assertEquals(List.of("src/view.jsx"), js.resolve(new Import("./view", List.of(), 3, List.of()), "src/legacy.cjs", files));
        assertEquals(List.of(), js.resolve(new Import("path", List.of(), 2, List.of()), "src/legacy.cjs", files));
    }

    @Test
    void aFileThatDoesNotParseIsSaidNotGuessed() {
        assertEquals(Map.of("broken.py", "does not parse: line 1"), report("python-complexity").unread());
        assertEquals(Map.of("src/broken.ts", "does not parse: line 1"), report("typescript-complexity").unread());
    }

    @Test
    void theFixturesComplexityIsItsGolden() throws IOException {
        golden("python-complexity", report("python-complexity"));
        golden("typescript-complexity", report("typescript-complexity"));
        golden("javascript-complexity", report("javascript-complexity"));
    }

    @Test
    void textThatIsNotAsciiKeepsNamesWhole() {
        Parsed parsed = new PythonAdapter().parse("é.py", "# déjà vu\ndef naïve(x):\n    return x\n");
        Measure naive = parsed.functions().get(0);
        assertEquals(List.of("naïve", 2, 1, 0, 3), List.of(naive.function(), naive.line(), naive.cyclomatic(), naive.cognitive(), naive.endLine()));
        assertEquals("def naïve(x):\n    return x", naive.text());
    }
}
