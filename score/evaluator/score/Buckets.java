package evaluator.score;

import evaluator.rubric.Fact;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The tooling bucket a card is ranked within (E-053): the agent tool holding more than half the evidence
 * the history found (`PROVENANCE.history.agent_tool.*`), "mixed" when none does, "none detected" when the
 * history was read and named none, "unknown" when no history was read.
 */
// Decision: E-053
public final class Buckets {
    static final String PREFIX = "PROVENANCE.history.agent_tool.";
    /** Each tool's name as the card writes it. */
    static final Map<String, String> TITLES = Map.of("claude", "Claude", "copilot", "Copilot", "cursor", "Cursor", "codex", "Codex",
            "gemini", "Gemini", "devin", "Devin", "aider", "Aider", "windsurf", "Windsurf");

    private Buckets() {}

    /** The bucket the card's facts put it in. */
    public static String of(List<Fact> facts) {
        if (Formulas.find(facts, "PROVENANCE.history.shape") == null) return Cards.NO_BUCKET;
        Map<String, Integer> evidence = new TreeMap<>();
        for (Fact f : facts) if (f.kind().startsWith(PREFIX)) evidence.put(f.kind().substring(PREFIX.length()), ((Number) f.value()).intValue());
        int all = evidence.values().stream().mapToInt(Integer::intValue).sum();
        if (all == 0) return "none detected";
        for (var e : evidence.entrySet()) if (2 * e.getValue() > all) return TITLES.getOrDefault(e.getKey(), e.getKey());
        return "mixed";
    }
}
