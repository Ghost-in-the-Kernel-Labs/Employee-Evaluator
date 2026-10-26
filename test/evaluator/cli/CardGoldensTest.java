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
            "typescript-complexity", "test-runs/node-pass", "test-runs/py-pass", "test-runs/py-fail", "test-runs/py-network", "judged/app");

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

    /**
     * TODO.md 2.2j: the judged card of test/fixtures/judged/app is a golden, as JSON and as Markdown, and so are the facts behind it. Worked by hand
     * (MainTest.judgedCountsOnlyTheFoldersTheJudgeCallsOwnCode): Size 22 over the baseline's 84,971 lines is 0.026, rounds to 0; Complexity
     * 0.6 x 100 x 3 / 13,361 + 0.4 x 100 x 2 / 28 = 0.013 + 2.857 = 2.87, rounds to 3; Maintainability 100 x 0.8375 / 0.895670 = 93.51, rounds to 94; the
     * ratio Maintainability at Complexity 94 x 3 / 100 = 2.82. The card names the prompt hash and the two models of test/fixtures/judged/judgment.json.
     */
    @Test
    void theJudgedCardIsItsGolden() throws IOException {
        Path repo = ROOT.resolve("test/fixtures/judged/app");
        String judgment = ROOT.resolve("test/fixtures/judged/judgment.json").toString();
        Goldens.Batch goldens = new Goldens.Batch();
        for (String form : List.of("--json", "--markdown", "--facts")) {
            List<String> args = new java.util.ArrayList<>(List.of("score", repo.toString(), "--no-run", "--judged", judgment));
            if (form.equals("--markdown")) args.add(form);
            if (form.equals("--facts")) args.add(form);
            else args.addAll(List.of("--date", DATE));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertEquals(0, Main.run(args, new PrintStream(out), new PrintStream(new ByteArrayOutputStream())), form);
            String extension = form.equals("--json") ? ".json" : form.equals("--markdown") ? ".md" : ".txt";
            goldens.check(ROOT.resolve("test/goldens/cards/judged-app.judged" + extension), out.toString(StandardCharsets.UTF_8));
        }
        goldens.assertUnchanged("the judged card changed: `gradle test -PshowGoldens`, read the diff, then `gradle test -PwriteGoldens` (E-018, E-077)");
    }
}
