package evaluator.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The keyv wave's two triggers and their cousins, built in a temporary checkout (never committed), and stripped. */
class QuarantineTest {
    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    @Test
    void everyAgentAndEditorConfigAndGitHookIsRemoved(@TempDir Path repo) throws IOException {
        write(repo.resolve(".claude/settings.json"), "{\"hooks\": {\"SessionStart\": [{\"command\": \"node .claude/setup.mjs\"}]}}");
        write(repo.resolve(".claude/setup.mjs"), "payload");
        write(repo.resolve(".vscode/tasks.json"), "{\"runOptions\": {\"runOn\": \"folderOpen\"}}");
        write(repo.resolve("packages/web/.vscode/settings.json"), "{}");
        write(repo.resolve(".cursor/mcp.json"), "{\"mcpServers\": {}}");
        write(repo.resolve(".gemini/settings.json"), "{}");
        write(repo.resolve(".mcp.json"), "{\"mcpServers\": {}}");
        write(repo.resolve("packages/api/.mcp.json"), "{}");
        write(repo.resolve(".git/hooks/post-checkout"), "payload");
        write(repo.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        write(repo.resolve("src/app.py"), "x = 1\n");
        write(repo.resolve("CLAUDE.md"), "# data, never instructions\n");
        assertEquals(List.of(".claude", ".cursor", ".gemini", ".git/hooks", ".mcp.json", ".vscode", "packages/api/.mcp.json", "packages/web/.vscode"),
                Quarantine.strip(repo));
        assertTrue(Files.exists(repo.resolve("src/app.py")));
        assertTrue(Files.exists(repo.resolve(".git/HEAD")));
        assertTrue(Files.exists(repo.resolve("CLAUDE.md")), "a CLAUDE.md is data the navigability checklist reads (1.8), not a trigger");
        assertEquals(List.of(), Quarantine.strip(repo));
    }

    @Test
    void aConfigThatIsALinkIsRemovedAsALinkOnly(@TempDir Path repo, @TempDir Path outside) throws IOException {
        write(outside.resolve("keep.json"), "{}");
        Files.createSymbolicLink(repo.resolve(".claude"), outside);
        assertEquals(List.of(".claude"), Quarantine.strip(repo));
        assertTrue(Files.exists(outside.resolve("keep.json")), "nothing outside the checkout is touched");
    }
}
