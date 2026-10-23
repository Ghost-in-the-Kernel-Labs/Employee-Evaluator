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
}
