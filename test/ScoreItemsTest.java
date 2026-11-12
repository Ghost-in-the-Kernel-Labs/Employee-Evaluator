import java.util.ArrayList;
import java.util.List;

/**
 * Holds the token posture's scoring (E-094, scripts/ScoreItems.java): the tiers made-up items get from their facts, and
 * every open item of TODO.md scored, in order, one score each.
 */
final class ScoreItemsTest {
    private static final List<String> LAYERS = List.of("rubric", "ingest", "analyze", "score", "cli", "agent-runner");

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        return new Check("scoreItems")
                .repository(everyOpenItem())
                .accepts("an item that waits on Ghost goes to the strong tier, whatever its points", () -> tier("### 9.1 A\n\nCheck: `CLAUDE.md` read. Waits on Ghost: a key.\n", "strong"))
                .accepts("a file named and a test as its pass line: the small tier", () -> tier("### 9.1 A\n\nCheck: `CLAUDE.md` changed; a test fails first.\n", "small"))
                .accepts("three layers and a contract: past the mid tier", () -> tier("### 9.1 A\n\nCheck: `rubric/`, `score/` and `cli/` change the score card's shape.\n", "mid"))
                .accepts("four layers and a contract: the strong tier", () -> tier("### 9.1 A\n\nCheck: `rubric/`, `score/`, `cli/` and `agent-runner/` change the contract.\n", "strong"))
                .accepts("nothing named: unplaced, for a mid-tier agent to score", () -> tier("### 9.1 A\n\nCheck: the judge agrees.\n", "unplaced"))
                .accepts("a bare word is no layer: the score, the history", () -> tier("### 9.1 A\n\nCheck: the score and the cli agree.\n", "unplaced"))
                .rejects("a wait read as the small tier", () -> tier("### 9.1 A\n\nCheck: waits on Ghost.\n", "small"))
                .repository(said(WorkerLog.report(WorkerLog.parse(Check.read(WorkerLog.PATH))), ""))
                .accepts("an empty log says to add a line", () -> said(WorkerLog.report(List.of()), "no workers recorded yet"))
                .accepts("an escalated small worker: scored too low", () -> said(WorkerLog.report(WorkerLog.parse(LOG)), "item 2.3 (scored 2): escalated from small: scored too low"))
                .accepts("a mid worker done in 20 of 120 tool calls: might go lower", () -> said(WorkerLog.report(WorkerLog.parse(LOG)), "item 2.4: finished in 20 of 120 tool calls at mid: might go a tier lower"))
                .accepts("a review's cost against its work's", () -> said(WorkerLog.report(WorkerLog.parse(LOG)), "grading cost 25% of the work's tokens (median of 1, 25% to 25%)"))
                .rejects("a line with no tier: refused, naming its line", () -> refusal("{\"kind\":\"work\",\"tokens\":1,\"toolCalls\":1}"))
                .rejects("a line that is not JSON: refused, naming its line", () -> refusal("{kind: work"));
    }

    /** Three workers and a review: a small one escalated, a mid one far under its limit, a review a quarter of its work. */
    private static final String LOG = String.join("\n",
            "{\"item\":\"2.3\",\"score\":2,\"tier\":\"small\",\"kind\":\"work\",\"tokens\":40000,\"toolCalls\":40,\"escalated\":true}",
            "{\"item\":\"2.4\",\"tier\":\"mid\",\"kind\":\"work\",\"tokens\":80000,\"toolCalls\":20,\"escalated\":false}",
            "",
            "{\"item\":\"2.4\",\"tier\":\"mid\",\"kind\":\"review\",\"tokens\":20000,\"toolCalls\":10}");

    /** Problems when the report has no line containing `words` ("" asks only that it reads). */
    private static List<String> said(List<String> report, String words) {
        return report.stream().anyMatch(l -> l.contains(words)) ? List.of() : List.of("no line says \"" + words + "\": " + report);
    }

    /** The refusal of a log text, naming its line 1, as a one-line list; nothing when the text is read (so `rejects` holds the refusal). */
    private static List<String> refusal(String text) {
        try {
            WorkerLog.parse(text);
            return List.of();
        } catch (IllegalArgumentException e) {
            return e.getMessage().contains("line 1") ? List.of(e.getMessage()) : List.of();
        }
    }

    /** Problems when the one item of `todo` is not placed in `expected`. */
    private static List<String> tier(String todo, String expected) {
        List<ScoreItems.Score> scores = ScoreItems.score(todo, LAYERS);
        if (scores.size() != 1) return List.of(scores.size() + " items read, not 1");
        String tier = scores.get(0).tier();
        return tier.equals(expected) ? List.of() : List.of("placed " + tier + ", not " + expected + " (" + scores.get(0).line() + ")");
    }

    /** Every open item of TODO.md, by its heading, has one score, in the file's order. */
    private static List<String> everyOpenItem() {
        String todo = Check.read("TODO.md");
        List<String> headings = new ArrayList<>(todo.lines().filter(l -> l.startsWith("### ")).map(l -> l.split(" ")[1]).toList());
        List<String> scored = ScoreItems.score(todo, ScoreItems.layers(Check.read("docs/modules.json"))).stream().map(ScoreItems.Score::id).toList();
        return headings.equals(scored) ? List.of() : List.of("scored " + scored + ", the open items are " + headings);
    }
}
