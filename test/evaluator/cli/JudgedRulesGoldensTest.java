package evaluator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.Goldens;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * TODO.md 2.2k: the card's rules for the judged kind, each case a fixture with a hand-written judgment (test/fixtures/judged/cases/) and a synthetic
 * agreement record (test/fixtures/judged/records/, never one from a real run, E-014), and its card, its facts and its Markdown a golden through
 * Goldens (E-018, E-077). The records: `aaaa...1` has every field live (Ambition 25 of 30, Quality within one 29, kind 27, each at its mark), `bbbb...2`
 * has none (22, 28 and 26, each one under). The values are worked by hand in the notes of the golden files' review (docs/JOURNAL.md, J-122).
 */
class JudgedRulesGoldensTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final String DATE = "2026-10-22";
    static final List<String> CASES = List.of("built", "mixed", "own-work", "clone", "plan-only", "generated", "own-work-not-live", "generated-not-live");

    /** One run of the command on the judged fixture app; the exit status is checked, the stdout returned. */
    private static String run(String judgment, String form, int status) {
        List<String> args = new ArrayList<>(List.of("score", ROOT.resolve("test/fixtures/judged/app").toString(), "--no-run",
                "--judged", ROOT.resolve("test/fixtures/judged/cases/" + judgment + ".json").toString(),
                "--records", ROOT.resolve("test/fixtures/judged/records").toString()));
        if (form.equals("--facts")) args.add("--facts");
        else args.addAll(List.of("--date", DATE));
        if (form.equals("--markdown")) args.add("--markdown");
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(status, Main.run(args, new PrintStream(out), new PrintStream(err)), judgment + " " + form + " " + err);
        assertTrue(!err.toString(StandardCharsets.UTF_8).contains("agreement record"), judgment + " " + form + ": a record was found, so stderr does not say it was not");
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void everyKindsCardIsItsGolden() throws IOException {
        Goldens.Batch goldens = new Goldens.Batch();
        for (String name : CASES) {
            int status = name.equals("generated") ? 2 : 0;
            for (String form : List.of("--json", "--markdown", "--facts")) {
                String extension = form.equals("--json") ? ".json" : form.equals("--markdown") ? ".md" : ".txt";
                goldens.check(ROOT.resolve("test/goldens/cards/judged-kind-" + name + extension), run(name, form, status));
            }
        }
        goldens.assertUnchanged("a judged-kind card changed: `gradle test -PshowGoldens`, read the diff, then `gradle test -PwriteGoldens` (E-018, E-077)");
    }

    /** The existing judged golden is unmoved by the rules because its prompt has no record: the default folder holds none (the real records folder stays empty, E-014). */
    @Test
    void aPromptWithNoRecordChangesNothing() {
        ByteArrayOutputStream plain = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        List<String> args = List.of("score", ROOT.resolve("test/fixtures/judged/app").toString(), "--no-run", "--date", DATE, "--judged",
                ROOT.resolve("test/fixtures/judged/judgment.json").toString(), "--records", ROOT.resolve("test/fixtures/judged/records").toString());
        assertEquals(0, Main.run(args, new PrintStream(plain), new PrintStream(err)));
        assertTrue(err.toString().contains("no agreement record for prompt 0123456789abcdef"), err.toString());
        assertTrue(!plain.toString().contains("notes"), plain.toString());
    }
}
