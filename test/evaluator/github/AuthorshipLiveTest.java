package evaluator.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import evaluator.rubric.Fact;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * TODO.md 1.7's check, against GitHub itself: Suite Builder's commits told apart through the API, read only,
 * with the token from the environment. Runs when `EVALUATOR_GITHUB_TOKEN` is set; skipped otherwise. Its
 * history only grows, so it asks for what is known to be there: the merges GitHub signed, Claude's commits
 * as another account's, and Ghost's own pushes.
 */
class AuthorshipLiveTest {
    @Test
    void suiteBuilderThroughGitHubsApi() throws IOException {
        assumeTrue(System.getenv(HttpGitHub.TOKEN_VARIABLE) != null, HttpGitHub.TOKEN_VARIABLE + " is not set");
        List<Fact> facts = Authorship.facts(HttpGitHub.fromEnvironment(), "Ghost-in-the-Kernel-Labs", "suite-builder", "ghost-in-the-kernel");
        Fact signed = facts.get(0), pushed = facts.get(1), foreign = facts.get(3);
        assertEquals("PROVENANCE.authorship.signed", signed.kind());
        assertTrue(signed.evidence().sources().contains("8be3cda9e0cbaa35e324c7c7d273e98bf1949484"), "a pull-request merge GitHub signed");
        assertTrue(foreign.evidence().sources().contains("f1211017d6c5e61f425b51bd56686669aafc4991"), "a commit of Claude's account");
        assertTrue(((Number) pushed.value()).intValue() > 0, "Ghost's own pushes in GitHub's log");
    }
}
