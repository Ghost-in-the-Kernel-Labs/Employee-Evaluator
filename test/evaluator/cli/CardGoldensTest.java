package evaluator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.Goldens;
import evaluator.analyze.RecordedSandbox;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * SC-09, TODO.md 1.9's check: every fixture repository's card, as JSON and as Markdown, is a golden
 * (test/goldens/cards/), compared byte for byte; a refactor changes none (E-018). The test-run fixtures'
 * tests are run in the recorded sandbox, so their Quality is scored without Docker.
 */
class CardGoldensTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final String DATE = "2026-10-22";
    static final List<String> FIXTURES = List.of("small-app", "imports", "duplication", "java-complexity", "python-complexity",
            "typescript-complexity", "test-runs/node-pass", "test-runs/py-pass", "test-runs/py-fail", "test-runs/py-network");

    private static String card(String fixture, boolean markdown) {
        Path repo = ROOT.resolve("test/fixtures/" + fixture);
        RecordedSandbox sandbox = fixture.startsWith("test-runs/") ? new RecordedSandbox(ROOT.resolve("test/goldens/" + fixture)) : null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Main.Command command = new Main.Command(repo, markdown, false, new Pipeline.Options(sandbox, null, null), DATE);
        assertEquals(0, Main.score(command, new PrintStream(out), new PrintStream(new ByteArrayOutputStream())), fixture);
        return out.toString(StandardCharsets.UTF_8);
    }

    /** Each fixture's two renderings against their goldens, every difference named at once; with -PshowGoldens, shown; with -PwriteGoldens, written (Goldens). */
    @Test
    void everyFixturesCardIsItsGolden() throws IOException {
        Goldens.Batch goldens = new Goldens.Batch();
        for (String fixture : FIXTURES) {
            for (boolean markdown : List.of(false, true)) {
                Path golden = ROOT.resolve("test/goldens/cards/" + fixture.replace('/', '-') + (markdown ? ".md" : ".json"));
                goldens.check(golden, card(fixture, markdown));
            }
        }
        goldens.assertUnchanged("the card changed: if that is intended, `gradle test -PshowGoldens`, read the diff, then `gradle test -PwriteGoldens` (E-018, E-077)");
    }
}
