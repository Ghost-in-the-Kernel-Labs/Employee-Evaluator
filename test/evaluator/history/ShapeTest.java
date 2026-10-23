package evaluator.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.history.Commits.Change;
import evaluator.history.Commits.Commit;
import evaluator.rubric.Fact;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShapeTest {
    private static final Instant DAY0 = Instant.parse("2026-09-01T10:00:00Z");
    private static final Predicate<String> TESTS = p -> p.startsWith("tests/");

    private static Instant at(int day, int hour) {
        return DAY0.plus(Duration.ofDays(day)).plus(Duration.ofHours(hour - 10));
    }

    private static Map<String, Object> values(Path repo) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Fact f : Shape.facts(Commits.read(repo), Commits.paths(repo), TESTS)) values.put(f.kind().substring("PROVENANCE.history.".length()), f.value());
        return values;
    }

    /**
     * Five commits worked by hand: sizes 13, 11, 2, 4, 2 (mean 6.4, deviation 4.673, variation 0.730);
     * days 0, 2 and 5 (3 active, 5 apart); one fix; of two commits touching tests one touches code;
     * one Claude trailer; CLAUDE.md kept in two commits (Claude's evidence: the trailer and the file). Parts: spread 3/14, cadence 1, variation 0.730,
     * messy 1, colocation 0.5, fingerprints 1; shape 100 × (0.054 + 0.2 + 0.146 + 0.15 + 0.05 + 0.1) = 70.
     */
    @Test
    void aSmallHistoryAsWorkedByHand(@TempDir Path dir) throws Exception {
        GitFixture h = new GitFixture()
                .commit(at(0, 10), "Start the calculator", Map.of("src/calc.py", GitFixture.lines("calc", 10), "CLAUDE.md", GitFixture.lines("note", 3)))
                .commit(at(0, 15), "Add mean, with its test\n\nCo-Authored-By: Claude <noreply@anthropic.com>",
                        Map.of("src/calc.py", GitFixture.lines("calc", 15), "tests/test_calc.py", GitFixture.lines("test", 6)));
        h.commit(at(2, 11), "fix typo in mean", Map.of("src/calc.py", h.file("src/calc.py").replace("calc 3\n", "calc three\n")))
                .commit(at(5, 9), "Add tests for edge cases", Map.of("tests/test_calc.py", GitFixture.lines("test", 10)))
                .commit(at(5, 12), "Update CLAUDE.md", Map.of("CLAUDE.md", GitFixture.lines("note", 5)));
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("commits", 5);
        expected.put("active_days", 3);
        expected.put("spread_days", 5L);
        expected.put("diff_variation", 0.73);
        expected.put("messy_commits", 1);
        expected.put("test_colocation", 0.5);
        expected.put("agent_trailers", 1);
        expected.put("agent_files", 1);
        expected.put("agent_tool.claude", 2);
        expected.put("shape", 70);
        assertEquals(expected, values(h.write(dir)));
    }

    /** 300 even commits in an hour, faking everything but time (a test beside each, an agent's trailer on each): under 30. */
    @Test
    void aReplayedHistoryScoresUnder30(@TempDir Path dir) throws Exception {
        GitFixture h = new GitFixture();
        for (int i = 0; i < 300; i++) {
            h.commit(DAY0.plusSeconds(12L * i), "Add part " + i + "\n\nCo-Authored-By: Claude <noreply@anthropic.com>",
                    Map.of("src/part" + i + ".py", GitFixture.lines("code", 20), "tests/test_part" + i + ".py", GitFixture.lines("test", 5)));
        }
        Map<String, Object> values = values(h.write(dir));
        assertEquals(300, values.get("commits"));
        assertEquals(1, values.get("active_days"));
        assertEquals(0.0, values.get("diff_variation"));
        assertTrue((Integer) values.get("shape") < 30, "shape " + values.get("shape"));
    }

    /** Six weeks of work: sizes from 1 line to 400, a fix and a revert now and then, tests beside code, an agent at times: over 70. */
    @Test
    void weeksOfVariedWorkScoreOver70(@TempDir Path dir) throws Exception {
        int[] sizes = {2, 400, 30, 1, 120, 8, 60};
        GitFixture h = new GitFixture();
        for (int i = 0; i < 40; i++) {
            String message = i % 10 == 9 ? "fix the off-by-one in part " + i : i == 25 ? "Revert \"Add part 24\"\n\nThis reverts commit 0000000." : "Add part " + i;
            if (i % 4 == 0) message += "\n\nCo-Authored-By: Claude <noreply@anthropic.com>";
            Map<String, String> writes = new LinkedHashMap<>();
            writes.put("src/part" + i + ".py", GitFixture.lines("code", sizes[i % sizes.length]));
            if (i % 3 == 0) writes.put("tests/test_part" + i + ".py", GitFixture.lines("test", 4));
            h.commit(DAY0.plus(Duration.ofHours(i * 25L + (i % 5) * 3)), message, writes);
        }
        Map<String, Object> values = values(h.write(dir));
        assertTrue((Integer) values.get("active_days") >= 35, "active days " + values.get("active_days"));
        assertTrue((Integer) values.get("shape") > 70, "shape " + values.get("shape"));
    }

    /** No commit, no fact: an empty history is not measured (SC-07). */
    @Test
    void noCommitsNoFacts() {
        assertEquals(List.of(), Shape.facts(List.of(), List.of(), TESTS));
    }

    @Test
    void anAgentTrailerIsAnAgentsAndMessinessIsReadFromTheFirstWord() {
        assertTrue(Shape.agentTrailer("x\n\nCo-authored-by: GitHub Copilot <copilot@github.com>"));
        assertFalse(Shape.agentTrailer("x\n\nCo-Authored-By: Pat <pat@example.invalid>"));
        Commit fix = new Commit("a", DAY0, "Fixed the build", List.of());
        Commit prefix = new Commit("b", DAY0, "Prefix the names", List.of());
        assertEquals(List.of("a"), Shape.read(List.of(fix, prefix), List.of(), TESTS).messy());
    }

    /**
     * A submission's .git/config and .gitattributes ask git to run commands; none runs. Plain `git status`
     * on such a repository runs its fsmonitor (checked by hand, J-027), so the status call here has teeth.
     */
    @Test
    void gitRunsNothingTheRepositoryAsksFor(@TempDir Path dir, @TempDir Path marks) throws Exception {
        Path repo = new GitFixture().commit(DAY0, "one", Map.of("a.py", "x = 1\n")).commit(DAY0.plusSeconds(60), "two", Map.of("a.py", "x = 2\n")).write(dir);
        String touch = "sh -c 'touch " + marks + "/$0' ";
        Files.writeString(repo.resolve(".git/config"), Files.readString(repo.resolve(".git/config"))
                + "[core]\n\tfsmonitor = " + touch + "fsmonitor\n\tpager = " + touch + "pager\n\thooksPath = .githooks\n"
                + "[diff]\n\texternal = " + touch + "external\n[diff \"evil\"]\n\ttextconv = " + touch + "textconv\n");
        Files.writeString(repo.resolve(".gitattributes"), "*.py diff=evil\n");
        Files.createDirectories(repo.resolve(".githooks"));
        for (String hook : List.of("post-checkout", "pre-commit", "reference-transaction")) {
            Path h = Files.writeString(repo.resolve(".githooks/" + hook), "#!/bin/sh\ntouch " + marks + "/" + hook + "\n");
            h.toFile().setExecutable(true);
        }
        assertEquals(2, Commits.read(repo).size());
        assertEquals(List.of("a.py"), Commits.paths(repo));
        Git.run(repo, List.of("status", "--short"));
        try (var left = Files.list(marks)) {
            assertEquals(List.of(), left.map(p -> p.getFileName().toString()).toList(), "nothing the repository asked for ran");
        }
    }

    @Test
    void theOverridesAreOnEveryCall() {
        List<String> command = Git.builder(Path.of("/r"), List.of("log")).command();
        assertTrue(command.containsAll(List.of("core.hooksPath=/dev/null", "core.fsmonitor=false", "diff.external=", "protocol.allow=never")));
        assertEquals("/dev/null", Git.builder(Path.of("/r"), List.of("log")).environment().get("GIT_CONFIG_GLOBAL"));
    }

    private static Commit commit(String hash, int day, String message, Change... changes) {
        return new Commit(hash, at(day, 10), message, List.of(changes));
    }

    private static Change change(String path, int lines) {
        return new Change(path, lines, 0);
    }

    private static Fact fact(List<Fact> facts, String kind) {
        return facts.stream().filter(f -> f.kind().equals("PROVENANCE.history." + kind)).findFirst().orElseThrow();
    }

    /** Cadence: 15 over commits per active day. 40 commits on 2 days is 20 a day, so 15/20 = 0.75 (not 15 x 20, not 15/(40 x 2)). */
    @Test
    void cadenceIsFifteenOverCommitsPerActiveDay() {
        List<Commit> commits = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) commits.add(commit("c" + i, i / 20, "Add part " + i));
        assertEquals(0.75, Shape.read(commits, List.of(), TESTS).parts().get("cadence"));
    }

    /** Messy: messy commits over a twentieth of all. 100 commits with 2 fixes: 2 / 5 = 0.4 (not 2 x 5, not 2 / (0.05 / 100)). */
    @Test
    void messyIsTheShareOfATwentiethOfTheCommits() {
        List<Commit> commits = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) commits.add(commit("c" + i, 0, i < 2 ? "fix part " + i : "Add part " + i));
        assertEquals(0.4, Shape.read(commits, List.of(), TESTS).parts().get("messy"));
    }

    /** A commit that changed no line (a merge) is no size: sizes 10 and 10 and a commit of none do not vary. */
    @Test
    void aCommitOfNoLinesIsLeftOutOfTheVariation() {
        assertEquals(0.0, Shape.variation(List.of(commit("a", 0, "a", change("x", 10)), commit("b", 0, "b", change("y", 10)), commit("c", 0, "c"))));
    }

    /** Two sizes are enough: 10 and 30 have mean 20, deviation 10, variation 0.5; one size, none. */
    @Test
    void twoSizesVaryAndOneDoesNot() {
        assertEquals(0.5, Shape.variation(List.of(commit("a", 0, "a", change("x", 10)), commit("b", 0, "b", change("y", 30)))));
        assertEquals(0.0, Shape.variation(List.of(commit("a", 0, "a", change("x", 10)))));
    }

    /** Of commits touching tests, the share touching code too: two beside code, one alone: 2/3. A README beside a test is no code. */
    @Test
    void colocationCountsOnlyCodeBesideATest() {
        Commit beside1 = commit("a", 0, "a", change("tests/a", 1), change("src/a", 1));
        Commit beside2 = commit("b", 0, "b", change("tests/b", 1), change("src/b", 1));
        Commit alone = commit("c", 0, "c", change("tests/c", 1));
        assertEquals(2.0 / 3, Shape.colocation(List.of(beside1, beside2, alone), TESTS), 1e-12);
        assertEquals(0.0, Shape.colocation(List.of(commit("d", 0, "d", change("tests/d", 1), change("README.md", 1))), TESTS));
        assertEquals(0.0, Shape.colocation(List.of(commit("e", 0, "e", change("src/e", 1))), TESTS));
    }

    /** Fingerprints: a tenth of commits with a trailer is full, else in proportion; an agent file is half, kept up in two commits full. */
    @Test
    void fingerprintsByTrailersAndByAgentFile() {
        assertEquals(0.0, Shape.fingerprints(0, 10, 0, 5));
        assertEquals(1.0, Shape.fingerprints(1, 10, 0, 0));
        assertEquals(0.5, Shape.fingerprints(1, 20, 0, 0));
        assertEquals(0.5, Shape.fingerprints(0, 10, 1, 1));
        assertEquals(1.0, Shape.fingerprints(0, 10, 1, 2));
        assertEquals(1.0, Shape.fingerprints(0, 10, 1, 3));
        assertEquals(0.5, Shape.fingerprints(1, 20, 1, 1), "the larger of the two");
    }

    /** The commits touching an agent file are counted from the changes: CLAUDE.md in 1 of 3 commits is 0.5, in 2 of 3 full. */
    @Test
    void anAgentFileKeptUpIsReadFromTheCommitsThatTouchedIt() {
        Commit a = commit("a", 0, "a", change("src/a", 1), change("CLAUDE.md", 1));
        Commit b = commit("b", 1, "b", change("src/b", 1));
        Commit c = commit("c", 2, "c", change("src/c", 1));
        List<String> head = List.of("CLAUDE.md", "src/a", "src/b", "src/c");
        assertEquals(0.5, Shape.read(List.of(a, b, c), head, TESTS).parts().get("fingerprints"));
        Commit c2 = commit("c", 2, "c", change("CLAUDE.md", 1));
        assertEquals(1.0, Shape.read(List.of(a, b, c2), head, TESTS).parts().get("fingerprints"));
    }

    /** The evidence of the trailer and agent-file facts: the hashes found, or "(none)" when there are none. */
    @Test
    void theEvidenceNamesWhatWasFoundOrNone() {
        List<Fact> none = Shape.facts(List.of(commit("a", 0, "Add part")), List.of("src/a"), TESTS);
        assertEquals(List.of("(none)"), fact(none, "agent_trailers").evidence().sources());
        assertEquals(List.of("(none)"), fact(none, "agent_files").evidence().sources());
        assertEquals(List.of("(none)"), fact(none, "messy_commits").evidence().sources());
        List<Fact> found = Shape.facts(List.of(commit("a", 0, "Add part\n\nCo-Authored-By: Claude <noreply@anthropic.com>"), commit("b", 1, "fix it")),
                List.of("CLAUDE.md"), TESTS);
        assertEquals(List.of("a"), fact(found, "agent_trailers").evidence().sources());
        assertEquals(List.of("CLAUDE.md"), fact(found, "agent_files").evidence().sources());
        assertEquals(List.of("b"), fact(found, "messy_commits").evidence().sources());
    }
}
