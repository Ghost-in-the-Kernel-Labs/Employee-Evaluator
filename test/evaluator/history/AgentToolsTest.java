package evaluator.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import evaluator.history.Commits.Commit;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentToolsTest {
    private static Commit commit(String hash, String message) {
        return new Commit(hash, Instant.parse("2026-10-01T10:00:00Z"), message, List.of());
    }

    /** E-053: a commit counts once per tool it names; Anthropic is Claude; AGENTS.md names none; a person is no tool. */
    @Test
    void eachToolWithItsEvidence() {
        List<Commit> commits = List.of(commit("a", "x\n\nCo-Authored-By: Claude <noreply@anthropic.com>"),
                commit("b", "y\n\nCo-authored-by: GitHub Copilot <copilot@github.com>\nCo-Authored-By: Claude Opus <noreply@anthropic.com>"),
                commit("c", "z\n\nCo-Authored-By: Pat <pat@example.invalid>"), commit("d", "Mentions Claude in the subject only"));
        Map<String, List<String>> found = AgentTools.found(commits, List.of("CLAUDE.md", "AGENTS.md", ".cursor/rules/style.mdc", "src/Main.java"));
        assertEquals(Map.of("claude", List.of("commit a", "commit b", "CLAUDE.md"), "copilot", List.of("commit b"), "cursor", List.of(".cursor/rules/style.mdc")), found);
    }

    @Test
    void namesAndFiles() {
        assertEquals(Set.of("codex"), AgentTools.named("x\n\nCo-Authored-By: ChatGPT <bot@openai.com>"));
        assertEquals(Set.of(), AgentTools.named("Co-Authored-By: Pat <pat@example.invalid>"));
        assertEquals("gemini", AgentTools.fileTool("docs/GEMINI.md"));
        assertEquals("copilot", AgentTools.fileTool(".github/copilot-instructions.md"));
        assertNull(AgentTools.fileTool("AGENTS.md"), "read by several tools, so it names none");
    }
}
