package evaluator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.analyze.RecordedSandbox;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /** Each fixture's two renderings against their goldens, every difference named at once; with -PwriteGoldens, written. */
    @Test
    void everyFixturesCardIsItsGolden() throws IOException {
        List<String> changed = new ArrayList<>();
        for (String fixture : FIXTURES) {
            for (boolean markdown : List.of(false, true)) {
                Path golden = ROOT.resolve("test/goldens/cards/" + fixture.replace('/', '-') + (markdown ? ".md" : ".json"));
                String card = card(fixture, markdown);
                if (Boolean.getBoolean("evaluator.writeGoldens")) {
                    Files.createDirectories(golden.getParent());
                    Files.writeString(golden, card);
                } else if (!Files.exists(golden) || !Files.readString(golden).equals(card)) {
                    changed.add(ROOT.relativize(golden).toString());
                }
            }
        }
        assertEquals(List.of(), changed, "a card changed: if that is intended, `gradle test -PwriteGoldens` and review the diff (E-018)");
    }
}
