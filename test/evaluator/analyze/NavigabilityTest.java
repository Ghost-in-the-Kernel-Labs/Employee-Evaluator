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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NavigabilityTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private static List<Fact> checklist(Path repo) throws IOException {
        Inventory inventory = Ingest.read(repo);
        Complexity.Report report = Complexity.measure(repo, inventory);
        return Navigability.facts(repo, inventory, report, Dependencies.graph(report, inventory));
    }

    private static Map<String, Object> values(List<Fact> facts) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Fact f : facts) values.put(f.kind(), f.value());
        return values;
    }

    /**
     * Worked by hand: CLAUDE.md names five paths, two gone (one missing, one out of the repository): 0.6;
     * three modules, billing and shipping importing each other: 1 − 2/3; one camelCase function among snake
     * ones: functions 3 of 4, files 3 of 3, 6 of 7. Checklist 100 × (0.25 + 0.21 + 0.067 + 0.171) = 70.
     */
    @Test
    void aSmallRepositoryAsWorkedByHand(@TempDir Path repo) throws IOException {
        write(repo.resolve("CLAUDE.md"), "# For an agent\n\nStart at `src/billing/invoice.py`, then `src/billing/missing.py`.\n"
                + "The guide is `docs/guide.md`; shipping is [here](src/shipping/). Never `../outside.txt`. Run `pytest`.\n");
        write(repo.resolve("docs/guide.md"), "# Guide\n");
        write(repo.resolve("src/billing/invoice.py"), "from shipping.rates import ship_cost\n\n\ndef price_invoice(x):\n    return ship_cost(x) + 1\n");
        write(repo.resolve("src/shipping/rates.py"), "from billing.invoice import price_invoice\n\n\ndef ship_cost(x):\n    return x * 2\n\n\ndef totalAmount(x):\n    return price_invoice(x)\n");
        write(repo.resolve("src/report/summary.py"), "def summary(rows):\n    return len(rows)\n");
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("NAVIGABILITY.checklist.agent_docs", 1);
        expected.put("NAVIGABILITY.checklist.doc_paths_named", 5);
        expected.put("NAVIGABILITY.checklist.doc_paths_missing", 2);
        expected.put("NAVIGABILITY.checklist.module_cycles", 2);
        expected.put("NAVIGABILITY.checklist.naming_consistency", 0.857);
        expected.put("NAVIGABILITY.checklist", 70);
        List<Fact> facts = checklist(repo);
        assertEquals(expected, values(facts));
        assertEquals(List.of("src/billing/missing.py", "../outside.txt"), facts.get(2).evidence().sources());
        assertEquals(List.of("src/billing", "src/shipping"), facts.get(3).evidence().sources());
    }

    /** No instructions for agents: their two parts give nothing; the rest is still measured. */
    @Test
    void noAgentDocs(@TempDir Path repo) throws IOException {
        write(repo.resolve("app.py"), "def run():\n    return 1\n");
        Map<String, Object> values = values(checklist(repo));
        assertEquals(0, values.get("NAVIGABILITY.checklist.agent_docs"));
        assertEquals(40, values.get("NAVIGABILITY.checklist"), "boundaries 0.2 and naming 0.2, nothing else");
    }

    /** TODO.md 1.8's check: this repository, its CLAUDE.md and AGENTS.md held fresh by its own doc test. */
    @Test
    void thisRepositoryScoresAbove80() throws IOException {
        Map<String, Object> values = values(checklist(ROOT));
        assertTrue((Integer) values.get("NAVIGABILITY.checklist") > 80, "checklist " + values);
        assertEquals(0, values.get("NAVIGABILITY.checklist.module_cycles"), "the kernel's modules import in one direction (E-004)");
    }

    @Test
    void aPathIsReadFromSpansAndLinksButNotFromCommandsOrUrls() {
        assertEquals("docs/SCORE-CARD.md", DocPaths.path("docs/SCORE-CARD.md"));
        assertEquals("README.md", DocPaths.path("./README.md"));
        assertEquals("analyze/", DocPaths.path("analyze/"));
        for (String notAPath : List.of("gradle test --offline", "https://github.com/x", "*.java", "-PwriteGoldens", "pytest", "$HOME/x", "/etc/passwd", "~/.ssh")) {
            assertNull(DocPaths.path(notAPath), notAPath);
        }
    }

    @Test
    void namesAndModules() {
        assertEquals("snake", Naming.style("price_invoice"));
        assertEquals("camel", Naming.style("totalAmount"));
        assertEquals("pascal", Naming.style("ScoreCard"));
        assertEquals("kebab", Naming.style("score-card"));
        assertEquals("lower", Naming.style("total"));
        assertEquals("analyze", Navigability.module("analyze/evaluator/analyze/Tokens.java"));
        assertEquals("src/billing", Navigability.module("src/billing/invoice.py"));
        assertEquals("java/src/main/java/shop", Navigability.module("java/src/main/java/shop/Cart.java"));
        assertEquals(".", Navigability.module("setup.py"));
    }

    /** A name that fits no style is "other", and is never the style the rest are held to. */
    @Test
    void aNameOfNoStyleIsOtherAndNeverDominant() {
        assertEquals("other", Naming.style("Foo_bar"));
        assertEquals("other", Naming.style("a-B"));
        // Foo_bar and Baz_qux are other (2), fooBar camel (1): the dominant style is camel's, so 1 of 3, not 2 of 3.
        assertEquals(List.of(1, 3), Arrays.stream(Naming.fit(List.of("Foo_bar", "Baz_qux", "fooBar"))).boxed().toList());
        // Lowercase words count with any style: total and index (2), then snake and pascal one each (dominant 1): 3 of 4.
        assertEquals(List.of(3, 4), Arrays.stream(Naming.fit(List.of("total", "index", "make_it", "Make"))).boxed().toList());
    }

    /**
     * A constructor carries its class's name and is left out; a method does not. Cart.java: the constructor Cart
     * (left out), addItem and removeItem (camel, 2 of 2); the file Cart (pascal, 1 of 1): 3 of 3.
     */
    @Test
    void aConstructorIsLeftOutOfTheNamesAndTheSummaryCountsThem(@TempDir Path repo) throws IOException {
        write(repo.resolve("Cart.java"), "class Cart {\n    Cart() {}\n    void addItem() {}\n    void removeItem() {}\n}\n");
        Inventory inventory = Ingest.read(repo);
        Complexity.Report report = Complexity.measure(repo, inventory);
        assertEquals("3 of 3 names in their language's style (functions and files)", Naming.summary(inventory, report));
        assertEquals(1.0, Naming.consistency(inventory, report));
    }

    /** A file that is only container names has its own name as the module's last part: `src/main` is in `src`, `main` is at the root. */
    @Test
    void aFileNamedLikeAContainerIsStillAFile() {
        assertEquals("src", Navigability.module("src/main"));
        assertEquals(".", Navigability.module("main"));
        assertEquals("src/lib", Navigability.module("src/lib/main"), "a folder of containers only is the module, its file left out");
    }

    /** A doc of exactly the size cap is read; one byte more is not (E-013: the cap keeps a hostile doc from being slurped). */
    @Test
    void aDocOfExactlyTheCapIsRead(@TempDir Path repo) throws IOException {
        String path = "`docs/a.md`";
        write(repo.resolve("CLAUDE.md"), path + " ".repeat(DocPaths.MAX_DOC - path.length()));
        assertEquals(DocPaths.MAX_DOC, Files.size(repo.resolve("CLAUDE.md")));
        assertEquals(List.of("docs/a.md"), DocPaths.named(repo, List.of("CLAUDE.md")));
        write(repo.resolve("CLAUDE.md"), path + " ".repeat(DocPaths.MAX_DOC - path.length() + 1));
        assertEquals(List.of(), DocPaths.named(repo, List.of("CLAUDE.md")));
    }
}
