package evaluator.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.rubric.Fact;
import evaluator.rubric.Json;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorshipTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final Path FIXTURE = ROOT.resolve("test/fixtures/github/suite-builder");
    private static final String OWNER = "Ghost-in-the-Kernel-Labs", REPO = "suite-builder", APPLICANT = "ghost-in-the-kernel";

    /** GitHub's answers from the fixture's files, by path; anything not recorded is an error, as an unknown path is. */
    static final class Recorded implements GitHubApi {
        final List<String> asked = new ArrayList<>();

        @Override
        public Object get(String path) throws IOException {
            asked.add(path);
            String base = "/repos/" + OWNER + "/" + REPO;
            String file;
            if (path.startsWith(base + "/commits?per_page=100&page=")) file = "commits-page-" + path.substring(path.lastIndexOf('=') + 1);
            else if (path.startsWith(base + "/events?per_page=100&page=")) file = "events-page-" + path.substring(path.lastIndexOf('=') + 1);
            else if (path.startsWith(base + "/compare/")) file = "compare-" + path.substring((base + "/compare/").length());
            else throw new IOException("not recorded: " + path);
            Path answer = FIXTURE.resolve(file + ".json");
            if (!Files.exists(answer)) throw new IOException("GitHub answered 404 for " + path);
            return Json.parse(Files.readString(answer));
        }
    }

    private static Map<String, Object> values(List<Fact> facts) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Fact f : facts) values.put(f.kind().substring("PROVENANCE.authorship.".length()), f.value());
        return values;
    }

    /**
     * Seven real commits worked by hand: the two merges GitHub signed; two of Ghost's unsigned commits his push
     * brought (the compare), one his but pushed by another account (claimed); Claude's two (foreign). Ghost's
     * share 5 of 7; his oldest push 2026-09-29 09:00, the newest commit 2026-10-21 18:48: 22 days.
     */
    @Test
    void suiteBuildersCommitsToldApart() throws IOException {
        List<Fact> facts = Authorship.facts(new Recorded(), OWNER, REPO, APPLICANT);
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("signed", 2);
        expected.put("pushed", 2);
        expected.put("claimed", 1);
        expected.put("foreign", 2);
        expected.put("unmatched", 0);
        expected.put("applicant_share", 0.714);
        expected.put("push_window_days", 22L);
        assertEquals(expected, values(facts));
        assertEquals(List.of("8be3cda9e0cbaa35e324c7c7d273e98bf1949484", "6c35c565eed32df5b55344464254400453606dd0"), facts.get(0).evidence().sources());
        assertEquals(List.of("80a1f5bd8e08aee7e674486c66c1e29c5a69a973"), facts.get(2).evidence().sources());
    }

    @Test
    void onlyTheApplicantsPushesCountAndANewBranchsPushAsksNoCompare() throws IOException {
        Recorded api = new Recorded();
        Set<String> pushed = Authorship.pushes(api, "/repos/" + OWNER + "/" + REPO, APPLICANT).shas();
        assertEquals(Set.of("0050c4064847ec4bf52387d3ce76671e3a20837c", "7e21d31ccfeb63c1544821df7be9ca9012fc2129", "6b089e6ca5b7f870db1b7db5fe69b8ec25e1f83b"), pushed);
        assertEquals(1, api.asked.stream().filter(p -> p.contains("/compare/")).count(), "the new branch's push (before all zeros) is not compared");
    }

    @Test
    void aCommitNoAccountClaimsIsUnmatchedAndAccountsAreCaseBlind() {
        Map<String, Object> nobody = Map.of("sha", "a", "author", Map.of(), "commit", Map.of());
        assertEquals(Authorship.Kind.UNMATCHED, Authorship.classify(nobody, APPLICANT, Set.of()));
        Map<String, Object> shouted = Map.of("sha", "b", "author", Map.of("login", "Ghost-In-The-Kernel"), "commit", Map.of());
        assertEquals(Authorship.Kind.CLAIMED, Authorship.classify(shouted, APPLICANT, Set.of()));
        assertEquals(Authorship.Kind.PUSHED, Authorship.classify(shouted, APPLICANT, Set.of("b")));
    }

    @Test
    void aNameThatCouldSteerAUrlIsRefused() {
        for (String bad : List.of("../../user", "a/b", "x?per_page=1", "", "a b")) {
            assertThrows(IOException.class, () -> Authorship.facts(new Recorded(), bad, REPO, APPLICANT), bad);
        }
    }

    @Test
    void noCommitsNoFacts() throws IOException {
        GitHubApi empty = path -> List.of();
        assertEquals(List.of(), Authorship.facts(empty, OWNER, REPO, APPLICANT));
    }

    @Test
    void theAdapterNamesItsVariableAndNeverItsToken() throws IOException {
        if (System.getenv(HttpGitHub.TOKEN_VARIABLE) == null) {
            IOException missing = assertThrows(IOException.class, HttpGitHub::fromEnvironment);
            assertEquals(HttpGitHub.TOKEN_VARIABLE + " is not set", missing.getMessage());
        } else {
            HttpGitHub live = HttpGitHub.fromEnvironment();
            assertFalse(live.toString().contains(System.getenv(HttpGitHub.TOKEN_VARIABLE).strip()));
            IOException refused = assertThrows(IOException.class, () -> live.get("https://evil.example/x"));
            assertTrue(refused.getMessage().startsWith("not an API path"));
        }
    }
}
