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
                .rejects("a wait read as the small tier", () -> tier("### 9.1 A\n\nCheck: waits on Ghost.\n", "small"));
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
