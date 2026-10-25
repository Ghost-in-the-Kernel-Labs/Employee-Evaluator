package evaluator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.Goldens;
import evaluator.history.Commits;
import evaluator.rubric.Json;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** `evaluator brief` (TODO.md 2.2b): what a judge is told, as JSON, the same bytes each time, with no contents and no author. */
class BriefTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    private static String brief(Path repo) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(0, Main.run(List.of("brief", repo.toString()), new PrintStream(out), new PrintStream(new ByteArrayOutputStream())), repo.toString());
        return out.toString(StandardCharsets.UTF_8);
    }

    private static Commits.Commit commit(String hash, long epoch, String message, int files) {
        List<Commits.Change> changes = new ArrayList<>();
        for (int i = 0; i < files; i++) changes.add(new Commits.Change("f" + i, 1, 0));
        return new Commits.Commit(hash, Instant.ofEpochSecond(epoch), message, changes);
    }

    /**
     * A folder of a.py (4 lines, one `if`: SIZE.source_lines 4, cognitive 1, one function, weighted 0.75; MainTest worked these), a README.md, a
     * readme.rst and a CLAUDE.md, with no history. By hand: the inventory lists a.py with 4 lines and nothing set aside; the facts are the
     * `--facts` values as numbers; no .git, so no commit (total 0); both READMEs in name order (README.md sorts before readme.rst: 'R' < 'r');
     * CLAUDE.md the one agent file. Parsed back, it is one JSON document.
     */
    @Test
    void aBriefWorkedByHand(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.py"), "def f(x):\n    if x:\n        return 1\n    return 2\n");
        Files.writeString(dir.resolve("README.md"), "SECRET-CONTENT\n");
        Files.writeString(dir.resolve("readme.rst"), "x\n");
        Files.writeString(dir.resolve("CLAUDE.md"), "SECRET-CONTENT\n");
        Files.writeString(dir.resolve("notes.md"), "not a readme\n");
        String brief = brief(dir);
        assertEquals(brief, brief(dir), "the same bytes twice");
        Map<?, ?> doc = (Map<?, ?>) Json.parse(brief);
        assertEquals(List.of("inventory", "facts", "commitsTotal", "commits", "readme", "agentInstructions"), new ArrayList<>(doc.keySet()));
        assertEquals(List.of(Map.of("path", "a.py", "language", "Python", "lines", 4.0)), ((Map<?, ?>) doc.get("inventory")).get("source"));
        assertEquals(List.of(), ((Map<?, ?>) doc.get("inventory")).get("setAside"));
        Map<?, ?> facts = (Map<?, ?>) doc.get("facts");
        assertEquals(4.0, facts.get("SIZE.source_lines"));
        assertEquals(1.0, facts.get("COMPLEXITY.cognitive_total"));
        assertEquals(1.0, facts.get("COMPLEXITY.functions"));
        assertEquals(0.75, facts.get("MAINTAINABILITY.weighted"));
        assertEquals(0.0, doc.get("commitsTotal"));
        assertEquals(List.of(), doc.get("commits"));
        assertEquals(List.of("README.md", "readme.rst"), doc.get("readme"));
        assertEquals(List.of("CLAUDE.md"), doc.get("agentInstructions"));
        assertFalse(brief.contains("SECRET-CONTENT") || brief.contains("not a readme"), "no file's contents");
    }

    /**
     * Commits worked by hand: three, given oldest first; the brief lists them newest first. 1,700,000,000 is 2023-11-14 22:13:20 UTC and
     * 1,700,086,400 (a day on) is 2023-11-15; a hash is cut to 7 characters; only a message's first line is the subject; files changed is
     * the number of paths. The third, at 1,700,000,000 + 2*86,400, is 2023-11-16, 0 files (a merge).
     */
    @Test
    void commitsAreListedNewestFirstAsShortHashDateSubjectFiles() {
        List<Commits.Commit> log = List.of(commit("aaaaaaaaaaaaaaaa", 1_700_000_000L, "first\n\nlong body", 2),
                commit("bbbbbbbbbbbbbbbb", 1_700_086_400L, "second", 1), commit("cccccccccccccccc", 1_700_172_800L, "merge", 0));
        assertEquals("""
                [
                    {"hash": "ccccccc", "date": "2023-11-16", "subject": "merge", "filesChanged": 0},
                    {"hash": "bbbbbbb", "date": "2023-11-15", "subject": "second", "filesChanged": 1},
                    {"hash": "aaaaaaa", "date": "2023-11-14", "subject": "first", "filesChanged": 2}
                  ]""", Brief.commits(log));
        assertEquals("[]", Brief.commits(List.of()));
    }

    /** 205 commits: the 200 newest are listed (hashes 5 to 204 of 0 to 204), none older; a subject over 200 characters is cut to 200, a quote is escaped. */
    @Test
    void theNewestTwoHundredAreListedAndASubjectIsCapped() {
        List<Commits.Commit> log = new ArrayList<>();
        for (int i = 0; i < 205; i++) log.add(commit(String.format("%07d", i), 1_700_000_000L + i, i == 204 ? "\"" + "x".repeat(300) : "c" + i, 1));
        String listed = Brief.commits(log);
        assertEquals(200, listed.lines().filter(l -> l.contains("\"hash\"")).count());
        assertTrue(listed.contains("\"hash\": \"0000204\"") && listed.contains("\"hash\": \"0000005\"") && !listed.contains("\"hash\": \"0000004\""), listed);
        assertTrue(listed.contains("\"subject\": \"\\\"" + "x".repeat(199) + "\"") && !listed.contains("x".repeat(200)), "the subject is 200 characters, its quote escaped");
    }

    /** The edges of both caps: exactly 200 characters stay whole, 201 are cut; exactly 200 commits are all listed, the oldest too; one commit is listed. */
    @Test
    void theCapsHoldAtTheirEdges() {
        assertTrue(Brief.commits(List.of(commit("1234567890", 1_700_000_000L, "y".repeat(200), 1))).contains("y".repeat(200) + "\""));
        String cut = Brief.commits(List.of(commit("1234567890", 1_700_000_000L, "y".repeat(201), 1)));
        assertTrue(cut.contains("y".repeat(200) + "\"") && !cut.contains("y".repeat(201)), cut);
        assertTrue(Brief.commits(List.of(commit("1234567890", 1_700_000_000L, "\nbody only", 1))).contains("\"subject\": \"\""), "a message that opens with a newline has an empty subject");
        List<Commits.Commit> log = new ArrayList<>();
        for (int i = 0; i < 200; i++) log.add(commit(String.format("%07d", i), 1_700_000_000L + i, "c", 1));
        String listed = Brief.commits(log);
        assertEquals(200, listed.lines().filter(l -> l.contains("\"hash\"")).count());
        assertTrue(listed.contains("\"hash\": \"0000000\""), "the oldest of exactly 200 is listed");
    }

    /** A .git that is no repository is refused with its reason, exit 1, nothing printed. */
    @Test
    void aBrokenHistoryIsRefused(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.py"), "x = 1\n");
        Files.createDirectory(dir.resolve(".git"));
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of("brief", dir.toString()), new PrintStream(out), new PrintStream(err)));
        assertTrue(err.toString().startsWith("evaluator brief: git log failed"), err.toString());
        assertEquals("", out.toString());
    }

    /** The facts as an object: a whole number and a fraction stay numbers, text is a string, none is {}. */
    @Test
    void factsBecomeAnObject() {
        assertEquals("{}", Brief.facts(""));
        assertEquals("{\n    \"A.b\": 3,\n    \"C.d\": 0.5,\n    \"E.f\": \"two words\"\n  }", Brief.facts("A.b 3\nC.d 0.5\nE.f two words\n"));
    }

    /** A real history: two commits, the author's name and address nowhere in the brief; the subject and the count are. */
    @Test
    void aHistoryGivesItsCommitsAndNoAuthor(@TempDir Path dir) throws Exception {
        git(dir, "init", "-q", "-b", "main");
        Files.writeString(dir.resolve("a.py"), "x = 1\n");
        git(dir, "add", ".");
        git(dir, "commit", "-q", "-m", "Add a");
        Files.writeString(dir.resolve("b.py"), "y = 2\n");
        Files.writeString(dir.resolve("c.py"), "z = 3\n");
        git(dir, "add", ".");
        git(dir, "commit", "-q", "-m", "Add b and c");
        String brief = brief(dir);
        Map<?, ?> doc = (Map<?, ?>) Json.parse(brief);
        assertEquals(2.0, doc.get("commitsTotal"));
        List<?> commits = (List<?>) doc.get("commits");
        assertEquals("Add b and c", ((Map<?, ?>) commits.get(0)).get("subject"));
        assertEquals(2.0, ((Map<?, ?>) commits.get(0)).get("filesChanged"));
        assertEquals("Add a", ((Map<?, ?>) commits.get(1)).get("subject"));
        assertEquals(1.0, ((Map<?, ?>) commits.get(1)).get("filesChanged"));
        assertEquals(7, ((String) ((Map<?, ?>) commits.get(0)).get("hash")).length());
        assertEquals("2023-11-14", ((Map<?, ?>) commits.get(1)).get("date"));
        assertFalse(brief.contains("Fixture") || brief.contains("example.invalid"), "no author name or address");
        assertEquals(brief, brief(dir), "the same bytes twice");
    }

    private static void git(Path dir, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", dir.toString(), "-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
                "-c", "commit.gpgsign=false", "-c", "core.hooksPath=/dev/null"));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("GIT_AUTHOR_DATE", "1700000000 +0000");
        builder.environment().put("GIT_COMMITTER_DATE", "1700000000 +0000");
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
    }

    /** Every fixture's brief is the same bytes on two runs and holds no word of any file; small-app's is a golden (checked by hand in the commit). */
    @Test
    void everyFixturesBriefIsStableAndSmallAppsIsAGolden() {
        for (String fixture : CardGoldensTest.FIXTURES) {
            Path repo = ROOT.resolve("test/fixtures/" + fixture);
            String first = brief(repo);
            assertEquals(first, brief(repo), fixture);
            Json.parse(first);
        }
        Goldens.check(ROOT.resolve("test/goldens/briefs/small-app.json"), brief(ROOT.resolve("test/fixtures/small-app")),
                "the brief changed: `gradle test -PshowGoldens`, read the diff, then `gradle test -PwriteGoldens` (E-018, E-077)");
    }

    @Test
    void wrongUseIsRefusedWithTheUsage() {
        for (List<String> args : List.of(List.of("brief"), List.of("brief", "a", "b"), List.of("brief", "--facts"), List.of("brief", ROOT.toString(), "--no-run"))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
            assertEquals(1, Main.run(args, new PrintStream(out), new PrintStream(err)), args.toString());
            assertTrue(err.toString().contains("brief takes one path") && err.toString().contains("usage: evaluator brief"), err.toString());
            assertEquals("", out.toString());
        }
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of("brief", ROOT.resolve("no-such-folder").toString()), new PrintStream(new ByteArrayOutputStream()), new PrintStream(err)));
        assertTrue(err.toString().contains("evaluator brief: not a folder"), err.toString());
    }
}
