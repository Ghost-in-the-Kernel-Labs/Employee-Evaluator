package evaluator.history;

import evaluator.history.Commits.Change;
import evaluator.history.Commits.Commit;
import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provenance layer 3, the history's shape (docs/DESIGN.md, E-042): how a repository came to be, read from
 * its commits. Spread over days, a cadence a person keeps, diffs of every size, the messy middle (reverts,
 * fixes), tests landing beside code, an agent's fingerprints; each a fact, and together `shape`, 0–100.
 * A replayed history (hundreds of even commits in an hour) scores low; weeks of varied work, high.
 */
// Decision: E-042
public final class Shape {
    /** The tool every history fact names as its evidence. */
    public static final String TOOL = "evaluator history 1 (git log)";
    private static final Pattern MESSY = Pattern.compile("(?i)^(revert\\b|fix(es|ed)?\\b|fixup!|squash!|typo|oops|wip\\b)");
    private static final Pattern TRAILER = Pattern.compile("(?im)^co-authored-by:(.*)$");
    private static final Pattern AGENT = Pattern.compile("(?i)\\b(claude|copilot|cursor|codex|gemini|devin|aider|windsurf|chatgpt|openai|anthropic)\\b");
    private static final List<String> AGENT_FILES = List.of("claude.md", "agents.md", "gemini.md", ".cursorrules", "copilot-instructions.md");
    /** The shape's parts and their weights, summing to 1. */
    static final Map<String, Double> WEIGHTS = Map.of("spread", 0.25, "cadence", 0.20, "variation", 0.20, "messy", 0.15, "colocation", 0.10, "fingerprints", 0.10);

    /** The measures read from a history, and the parts of the shape made of them, each 0–1. */
    record Reading(int commits, int activeDays, long spreadDays, double variation, List<String> messy, double colocation,
                   List<String> trailers, List<String> agentFiles, int agentFileCommits, Map<String, Double> parts) {
        /** The shape: the weighted parts, 0–100. */
        int shape() {
            double sum = 0;
            for (var part : parts.entrySet()) sum += WEIGHTS.get(part.getKey()) * part.getValue();
            return (int) Math.round(100 * sum);
        }
    }

    private Shape() {}

    /** The history's facts, or none for a history with no commit (SC-07). */
    public static List<Fact> facts(List<Commit> commits, List<String> pathsAtHead, Predicate<String> isTest) {
        if (commits.isEmpty()) return List.of();
        Reading r = read(commits, pathsAtHead, isTest);
        List<String> ends = List.of(commits.get(0).hash(), commits.get(commits.size() - 1).hash());
        List<Fact> facts = new ArrayList<>();
        facts.add(fact("commits", r.commits(), ends));
        facts.add(fact("active_days", r.activeDays(), ends));
        facts.add(fact("spread_days", r.spreadDays(), ends));
        facts.add(fact("diff_variation", r.variation(), ends));
        facts.add(fact("messy_commits", r.messy().size(), r.messy().isEmpty() ? List.of("(none)") : r.messy()));
        facts.add(fact("test_colocation", r.colocation(), ends));
        facts.add(fact("agent_trailers", r.trailers().size(), r.trailers().isEmpty() ? List.of("(none)") : r.trailers()));
        facts.add(fact("agent_files", r.agentFiles().size(), r.agentFiles().isEmpty() ? List.of("(none)") : r.agentFiles()));
        List<String> parts = new ArrayList<>();
        r.parts().forEach((name, value) -> parts.add(name + " " + round(value)));
        facts.add(fact("shape", r.shape(), parts));
        return facts;
    }

    /** Every measure of the history, and the shape's parts. */
    static Reading read(List<Commit> commits, List<String> pathsAtHead, Predicate<String> isTest) {
        TreeSet<String> days = new TreeSet<>();
        for (Commit c : commits) days.add(c.at().atZone(ZoneOffset.UTC).toLocalDate().toString());
        long spread = Duration.between(commits.get(0).at(), commits.get(commits.size() - 1).at()).toDays();
        List<String> messy = commits.stream().filter(c -> MESSY.matcher(c.message()).find() || c.message().contains("This reverts commit")).map(Commit::hash).toList();
        List<String> trailers = new ArrayList<>();
        for (Commit c : commits) if (agentTrailer(c.message())) trailers.add(c.hash());
        List<String> agentFiles = pathsAtHead.stream().filter(Shape::agentFile).toList();
        int agentFileCommits = (int) commits.stream().filter(c -> c.changes().stream().anyMatch(ch -> agentFile(ch.path()))).count();
        double variation = variation(commits), colocation = colocation(commits, isTest);
        Map<String, Double> parts = new LinkedHashMap<>();
        parts.put("spread", Math.min(1, days.size() / 14.0));
        parts.put("cadence", Math.min(1, 15.0 / ((double) commits.size() / days.size())));
        parts.put("variation", Math.min(1, variation));
        parts.put("messy", Math.min(1, messy.size() / Math.max(1, 0.05 * commits.size())));
        parts.put("colocation", colocation);
        parts.put("fingerprints", fingerprints(trailers.size(), commits.size(), agentFiles.size(), agentFileCommits));
        return new Reading(commits.size(), days.size(), spread, round(variation), messy, round(colocation), trailers, agentFiles, agentFileCommits, parts);
    }

    /** How far commits' sizes vary: their standard deviation over their mean (0 when fewer than two changed lines). */
    static double variation(List<Commit> commits) {
        double[] sizes = commits.stream().mapToInt(Commit::size).filter(s -> s > 0).asDoubleStream().toArray();
        if (sizes.length < 2) return 0;
        double mean = java.util.Arrays.stream(sizes).average().orElse(0);
        double variance = java.util.Arrays.stream(sizes).map(s -> (s - mean) * (s - mean)).sum() / sizes.length;
        return Math.sqrt(variance) / mean;
    }

    /** Of the commits that touch a test, the share that touch code beside it; 0 with no test touched. */
    static double colocation(List<Commit> commits, Predicate<String> isTest) {
        int withTests = 0, beside = 0;
        for (Commit c : commits) {
            boolean test = c.changes().stream().map(Change::path).anyMatch(isTest);
            boolean code = c.changes().stream().map(Change::path).anyMatch(p -> !isTest.test(p) && !p.endsWith(".md"));
            if (test) withTests++;
            if (test && code) beside++;
        }
        return withTests == 0 ? 0 : (double) beside / withTests;
    }

    /** An agent's hand: 1 when a tenth of commits carry an agent's trailer or an agent file is kept up; otherwise in proportion. */
    static double fingerprints(int trailers, int commits, int agentFiles, int agentFileCommits) {
        double byTrailers = Math.min(1, 10.0 * trailers / commits);
        double byFiles = agentFiles == 0 ? 0 : agentFileCommits >= 2 ? 1 : 0.5;
        return Math.max(byTrailers, byFiles);
    }

    /** Whether a message carries a Co-Authored-By trailer naming an agent. */
    static boolean agentTrailer(String message) {
        Matcher trailer = TRAILER.matcher(message);
        while (trailer.find()) if (AGENT.matcher(trailer.group(1)).find()) return true;
        return false;
    }

    private static boolean agentFile(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return AGENT_FILES.contains(name) || path.startsWith(".cursor/rules/");
    }

    private static Fact fact(String kind, Number value, List<String> sources) {
        return Fact.measured("PROVENANCE.history." + kind, Subject.repository(), value, new Evidence(TOOL, sources));
    }

    private static double round(double value) {
        return Math.round(1000 * value) / 1000.0;
    }
}
