package evaluator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import evaluator.github.Authorship;
import evaluator.github.GitHubApi;
import evaluator.rubric.Fact;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelineTest {
    private static List<String> lines(ByteArrayOutputStream notes) {
        return notes.toString(StandardCharsets.UTF_8).lines().toList();
    }

    /** A folder with no .git directory has no history to read, and says so on the notes (E-013: a .git file is not followed either). */
    @Test
    void aFolderWithoutGitSaysItsHistoryWasNotRead(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a.txt"), "x\n");
        ByteArrayOutputStream notes = new ByteArrayOutputStream();
        Pipeline.facts(dir, new Pipeline.Options(null, null, null), new PrintStream(notes));
        assertEquals(List.of("history not read: no .git directory"), lines(notes));
    }

    /** A language whose tests cannot be measured is told, one note each, before the history's note. */
    @Test
    void whatTheSandboxCouldNotMeasureIsSaid(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a.test.js"), "test('x', () => {});\n");
        ByteArrayOutputStream notes = new ByteArrayOutputStream();
        Pipeline.facts(dir, new Pipeline.Options((phase, work, out, script, timeout) -> {
            throw new AssertionError("a plan that is not measured never reaches the sandbox");
        }, null, null), new PrintStream(notes));
        assertEquals(List.of("tests not measured: Node: no package.json at the root", "history not read: no .git directory"), lines(notes));
    }

    /** The owner/repo split on its first slash, the applicant as given: the paths GitHub is asked, and the facts they make. */
    @Test
    void githubIsAskedForTheOwnerAndRepositoryNamed() throws IOException {
        List<String> asked = new ArrayList<>();
        Map<String, Object> commit = Map.of("sha", "abc", "author", Map.of("login", "octo"),
                "commit", Map.of("author", Map.of("date", "2026-10-01T00:00:00Z"), "verification", Map.of("verified", true)));
        GitHubApi api = path -> {
            asked.add(path);
            return path.contains("/commits") ? List.of(commit) : List.of();
        };
        List<Fact> facts = Pipeline.github(api, new Pipeline.Options(null, "ghost/app", "octo"));
        assertFalse(facts.isEmpty());
        assertEquals(Authorship.facts(path -> path.contains("/commits") ? List.of(commit) : List.of(), "ghost", "app", "octo"), facts);
        assertEquals("/repos/ghost/app/commits?per_page=100&page=1", asked.get(0));
    }
}
