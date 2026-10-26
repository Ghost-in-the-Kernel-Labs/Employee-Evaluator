package evaluator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    @Test
    void noArgumentsPrintsUsageAndRefuses() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of(), new PrintStream(new ByteArrayOutputStream()), new PrintStream(err)));
        assertTrue(err.toString().contains("usage: evaluator score"));
    }

    @Test
    void whatWasTypedIsRead() {
        Main.Command c = Main.parse(List.of("score", "repo", "--markdown", "--no-run", "--github", "ghost/app", "--applicant", "ghost", "--date", "2026-10-22"));
        assertEquals(Path.of("repo"), c.path());
        assertTrue(c.markdown());
        assertNull(c.options().sandbox(), "--no-run: no sandbox");
        assertEquals("ghost/app", c.options().github());
        assertEquals("2026-10-22", c.date());
        assertTrue(Main.parse(List.of("score", "repo")).options().sandbox() != null, "tests run unless told not to");
        assertFalse(c.facts(), "the card unless --facts");
    }

    @Test
    void factsAreAskedForWithTheirOwnFlag() {
        assertTrue(Main.parse(List.of("score", "repo", "--no-run", "--facts")).facts());
        assertTrue(Main.parse(List.of("score", "--facts", "repo")).facts(), "the flag may come first");
    }

    @Test
    void whatCannotBeReadIsRefused() {
        for (List<String> args : List.of(List.of("score"), List.of("grade", "repo"), List.of("score", "a", "b"), List.of("score", "repo", "--github", "ghost/app"),
                List.of("score", "repo", "--github", "../x", "--applicant", "g"), List.of("score", "repo", "--github", "ghost/..", "--applicant", "g"), List.of("score", "repo", "--date"), List.of("score", "repo", "--frobnicate"),
                List.of("score", "repo", "--facts", "--markdown"), List.of("score", "repo", "--date", "2026-10-22", "--facts"))) {
            assertThrows(IllegalArgumentException.class, () -> Main.parse(args), args.toString());
        }
    }

    /** A bad date is refused with the usage, exit 1, never a crash (an Opus judge found it, docs/judges/trial-2.md). */
    @Test
    void aBadDateIsRefusedNotACrash() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of("score", "repo", "--date", "yesterday"), new PrintStream(new ByteArrayOutputStream()), new PrintStream(err)));
        assertTrue(err.toString().contains("--date takes yyyy-mm-dd, not yesterday") && err.toString().contains("usage:"), err.toString());
    }

    /** A wrong use of --facts is refused as the others are: the usage and exit 1, nothing on stdout. */
    @Test
    void factsWithACardsFlagIsRefusedWithTheUsage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of("score", "repo", "--facts", "--markdown"), new PrintStream(out), new PrintStream(err)));
        assertTrue(err.toString().contains("--facts prints no card") && err.toString().contains("usage:"), err.toString());
        assertEquals("", out.toString());
    }

    /**
     * --facts on a folder of one file, worked by hand: a.py is 4 lines (SIZE.source_lines 4); f has one `if` at nesting 0, cognitive 1; one file,
     * no import, so depth 0, no cycle, fan-out 0, no unused import; no test file, so a test-to-code ratio of 0; no clone, duplication 0. Parts:
     * duplication 1, tests 0, cycles 1, coupling 1, functions 1, unused 1; weighted 0.25 + 0 + 0.15 + 0.10 + 0.15 + 0.10 = 0.75. No card is printed.
     */
    @Test
    void factsPrintTheValuesBehindTheCard(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.py"), "def f(x):\n    if x:\n        return 1\n    return 2\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(0, Main.run(List.of("score", dir.toString(), "--no-run", "--facts"), new PrintStream(out), new PrintStream(err)));
        String printed = out.toString(StandardCharsets.UTF_8);
        for (String line : List.of("COMPLEXITY.cognitive_total 1", "COMPLEXITY.dependency_depth 0", "COMPLEXITY.functions 1", "MAINTAINABILITY.files 1",
                "MAINTAINABILITY.part.duplication 1", "MAINTAINABILITY.part.tests 0", "MAINTAINABILITY.weighted 0.75", "SIZE.source_lines 4")) {
            assertTrue(printed.lines().anyMatch(line::equals), line + "\n" + printed);
        }
        assertEquals(printed.lines().sorted().toList(), printed.lines().toList(), "sorted by name");
        assertFalse(printed.contains("{") || printed.contains("|"), "a card is not printed");
    }

    @Test
    void aPathThatIsNoFolderIsRefused() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of("score", ROOT.resolve("no-such-folder").toString(), "--no-run"), new PrintStream(new ByteArrayOutputStream()), new PrintStream(err)));
        assertTrue(err.toString().contains("not a folder"));
    }

    /** What the pipeline cannot read is told as one line and exit 1, with nothing printed as a card: a .git that is no repository. */
    @Test
    void whatCouldNotBeReadIsRefusedWithItsReason(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.txt"), "x\n");
        Files.createDirectory(dir.resolve(".git"));
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of("score", dir.toString(), "--no-run"), new PrintStream(out), new PrintStream(err)));
        assertTrue(err.toString().startsWith("evaluator score: git log failed"), err.toString());
        assertEquals("", out.toString());
    }

    /** The whole pipeline on this repository, tests unrun: every metric but Ambition and Quality's run scores. */
    @Test
    void thisRepositorysCard() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(0, Main.run(List.of("score", ROOT.toString(), "--no-run", "--markdown"), new PrintStream(out), new PrintStream(err)));
        String card = out.toString(StandardCharsets.UTF_8);
        for (String scored : List.of("| Complexity | ", "| Size | ", "| AI navigability | ", "| Maintainability | ", "| Provenance | ")) {
            assertTrue(card.contains(scored) && !card.contains(scored + "not measured"), scored + "\n" + card);
        }
        assertTrue(card.contains("| Ambition | not measured"), card);
        assertTrue(card.contains("- Tooling bucket: Claude"), card);
    }

    private static final Path JUDGED = ROOT.resolve("test/fixtures/judged");

    private static String facts(String... extra) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        List<String> args = new java.util.ArrayList<>(List.of("score", JUDGED.resolve("app").toString(), "--no-run", "--facts"));
        args.addAll(List.of(extra));
        assertEquals(0, Main.run(args, new PrintStream(out), new PrintStream(err)), err.toString());
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void assertLines(String printed, List<String> lines) {
        for (String line : lines) assertTrue(printed.lines().anyMatch(line::equals), line + "\n" + printed);
    }

    /**
     * TODO.md 2.2j on test/fixtures/judged, worked by hand (the judgment: src and docs/tools own code, src/gen generated, tests own tests,
     * tests/data fixture data, scripts own tooling, docs docs; extra/ in no folder). Lines, non-blank: util 2, calc 7, build 6, helper 5,
     * test_helper 2, and set aside table 6, test_calc 5, sample 1, release 3, example 1.
     * Counted: util, calc, build (own code) and helper, test_helper (no folder, so the kernel's rule counts them): 22 lines in 5 files in
     * 3 folders (src, docs/tools, extra); 5 set aside. Cognitive: calc.classify 1, helper.label 1, build 1, the rest 0: total 3 over 6 functions.
     * Depth: helper to calc to util is 2 links; build to util is 1 (release to table to calc to util, 3, is not counted). Tests part: by class,
     * test_calc (5) own tests and test_helper (2) a test by the kernel's name rule: 7 over the code, util 2 + calc 7 + build 6 + helper 5 = 20:
     * 0.35. Parts: duplication 1, cycles 1, coupling 1, functions 1, unused 1, tests 0.35: weighted 0.25 + 0.25 x 0.35 + 0.15 + 0.10 + 0.15
     * + 0.10 = 0.8375.
     */
    @Test
    void judgedCountsOnlyTheFoldersTheJudgeCallsOwnCode() {
        assertLines(facts("--judged", JUDGED.resolve("judgment.json").toString()), List.of("SIZE.source_lines 22", "SIZE.source_files 5", "SIZE.source_folders 3",
                "SIZE.set_aside_files 5", "COMPLEXITY.cognitive_total 3", "COMPLEXITY.functions 6", "COMPLEXITY.dependency_depth 2",
                "MAINTAINABILITY.files 5", "MAINTAINABILITY.test_to_code_ratio 0.35", "MAINTAINABILITY.part.tests 0.35", "MAINTAINABILITY.part.duplication 1",
                "MAINTAINABILITY.part.cycles 1", "MAINTAINABILITY.part.coupling 1", "MAINTAINABILITY.part.functions 1", "MAINTAINABILITY.part.unused 1",
                "MAINTAINABILITY.weighted 0.8375"));
    }

    /**
     * The same fixture without --judged is the kernel's own count, worked by hand: all ten files, 38 lines in 8 folders; cognitive 1 + 3
     * (src/gen/table: an if and a nested if, 1 + 2) + 1 + 1 = 6 over 10 functions; depth release, table, calc, util = 3; tests by the path
     * rule: test_calc 5, sample 1 (a tests folder), test_helper 2 = 8 over the 30 lines of the rest = 0.267; weighted
     * 0.25 + 0.25 x 0.267 + 0.5 = 0.81675.
     */
    @Test
    void withoutJudgedTheFixtureIsTheKernelsOwnCount() {
        assertLines(facts(), List.of("SIZE.source_lines 38", "SIZE.source_files 10", "SIZE.source_folders 8", "SIZE.set_aside_files 0",
                "COMPLEXITY.cognitive_total 6", "COMPLEXITY.functions 10", "COMPLEXITY.dependency_depth 3", "MAINTAINABILITY.files 10",
                "MAINTAINABILITY.test_to_code_ratio 0.267", "MAINTAINABILITY.weighted 0.81675"));
    }

    /** The navigability checklist reads the whole inventory with or without a judgment: the judge scores it itself. */
    @Test
    void theNavigabilityChecklistIsNotNarrowedByAJudgment() {
        List<String> plain = facts().lines().filter(l -> l.startsWith("NAVIGABILITY")).toList();
        assertFalse(plain.isEmpty());
        assertEquals(plain, facts("--judged", JUDGED.resolve("judgment.json").toString()).lines().filter(l -> l.startsWith("NAVIGABILITY")).toList());
    }

    /** --judged takes a value; a file that is missing, a folder or the wrong shape is refused with the usage and a reason, nothing on stdout. */
    @Test
    void judgedIsReadAndItsFileChecked(@TempDir Path dir) throws Exception {
        Main.Command c = Main.parse(List.of("score", "repo", "--no-run", "--judged", JUDGED.resolve("judgment.json").toString()));
        assertEquals("synthetic-opus-1", c.options().judgment().models().get(1));
        assertNull(Main.parse(List.of("score", "repo", "--no-run")).options().judgment());
        assertThrows(IllegalArgumentException.class, () -> Main.parse(List.of("score", "repo", "--judged")));
        Path bare = dir.resolve("bare.json");
        Files.writeString(bare, "{\"kind\": \"built\", \"folders\": []}");
        for (String file : List.of(dir.resolve("none.json").toString(), dir.toString(), bare.toString())) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
            assertEquals(1, Main.run(List.of("score", "repo", "--judged", file), new PrintStream(out), new PrintStream(err)));
            assertTrue(err.toString().contains("evaluator: --judged") && err.toString().contains("usage:"), err.toString());
            assertEquals("", out.toString());
        }
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Main.run(List.of("score", "repo", "--judged", bare.toString()), new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));
        assertTrue(err.toString().contains("evaluator: --judged " + bare + ": this is a bare judgment answer"), err.toString());
        ByteArrayOutputStream missing = new ByteArrayOutputStream();
        Main.run(List.of("score", "repo", "--judged", dir.resolve("none.json").toString()), new PrintStream(new ByteArrayOutputStream()), new PrintStream(missing));
        assertTrue(missing.toString().contains("evaluator: --judged: cannot read " + dir.resolve("none.json")), missing.toString());
    }
}
