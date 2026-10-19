package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.ingest.Ingest;
import evaluator.ingest.Inventory;
import evaluator.rubric.Fact;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DuplicationTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final Path FIXTURE = ROOT.resolve("test/fixtures/duplication");

    private static Complexity.Report report() {
        return Complexity.measure(FIXTURE, Ingest.read(FIXTURE));
    }

    /** price_order is 78 tokens (worked line by line; line 8 alone is 14), all of them in its renamed copy: both whole functions, nothing else. */
    @Test
    void aRenamedCopyIsCaughtAndANearMissIsNot() {
        Fact fact = Duplication.fact(report()).orElseThrow();
        assertEquals(List.of("src/invoices.py:4-15", "src/orders.py:1-12"), fact.evidence().sources());
        int priceOrder = Tokens.of(read("src/orders.py").split("\n\n\n")[0], true).tokens().size();
        assertEquals(78, priceOrder);
        int total = 0;
        for (Duplication.Unit u : Duplication.units(report())) total += u.tokens().size();
        assertEquals(Math.round(1000.0 * 2 * priceOrder / total) / 1000.0, fact.value());
    }

    @Test
    void namesAndLiteralsAreNormalizedKeywordsAndCommentsAreNot() {
        assertEquals(List.of("if", "$", ">", "0", ":", "return", "\"\""), Tokens.of("if total > 1000:  # big\n    return 'x'", true).tokens());
        Tokens.List2 js = Tokens.of("while (busy) /* spin */ log(`a\nb`); // done", false);
        assertEquals(List.of("while", "(", "$", ")", "$", "(", "\"\"", ")", ";"), js.tokens());
        assertEquals(List.of(1, 1, 1, 1, 1, 1, 1, 2, 2), js.lines());
    }

    @Test
    void oneRunRepeatedInOneFunctionCountsOnlyWhenTheCopiesDoNotOverlap() {
        String once = "x = a + b\n".repeat(12);
        List<Duplication.Unit> overlapping = List.of(unit("x = a + b\n".repeat(13)));
        Duplication.mark(overlapping);
        assertEquals(0, count(overlapping.get(0)));
        List<Duplication.Unit> twice = List.of(unit(once + "y = 1\n" + once));
        Duplication.mark(twice);
        assertEquals(120, count(twice.get(0)));
    }

    @Test
    void aHugeRepetitiveFileCostsItsLength() {
        List<Duplication.Unit> big = List.of(unit("x = a + b\n".repeat(50_000)));
        long started = System.nanoTime();
        Duplication.mark(big);
        assertEquals(250_000, count(big.get(0)));
        assertEquals(true, System.nanoTime() - started < 5_000_000_000L, "linear, not quadratic");
    }

    /** Every Maintainability fact of the fixture, one per line: the golden of TODO.md 1.4. */
    @Test
    void theFixturesMaintainabilityFactsAreTheirGolden() throws IOException {
        Inventory inventory = Ingest.read(FIXTURE);
        Complexity.Report report = Complexity.measure(FIXTURE, inventory);
        List<Fact> facts = new ArrayList<>();
        TestFiles.ratio(inventory).ifPresent(facts::add);
        facts.addAll(Coupling.facts(Dependencies.graph(report, inventory)));
        Duplication.fact(report).ifPresent(facts::add);
        StringBuilder text = new StringBuilder();
        for (Fact f : facts) text.append(f.key()).append(" = ").append(f.value()).append("  ").append(f.evidence().sources()).append('\n');
        Path golden = ROOT.resolve("test/goldens/duplication/maintainability.txt");
        if (Boolean.getBoolean("evaluator.writeGoldens")) Files.writeString(golden, text.toString());
        assertEquals(Files.readString(golden), text.toString(), "the facts changed: if that is intended, `gradle test -PwriteGoldens` and review the diff (E-018)");
    }

    private static Duplication.Unit unit(String text) {
        Tokens.List2 t = Tokens.of(text, true);
        return new Duplication.Unit("a.py", 1, t.tokens(), t.lines(), new boolean[t.tokens().size()]);
    }

    private static int count(Duplication.Unit u) {
        int n = 0;
        for (boolean b : u.duplicated()) if (b) n++;
        return n;
    }

    private static String read(String path) {
        try {
            return Files.readString(FIXTURE.resolve(path));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
