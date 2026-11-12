import evaluator.rubric.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The token posture's step 4 (E-092, E-095): what each worker cost, beside the score its item was given, one JSON line each
 * in docs/WORKERS.jsonl, read by `gradle scoreItems --args=--tune` to say which placements were right. A line holds `item`,
 * `score` (points, or null when unplaced), `tier` (small, mid or strong), `kind` (work: the item built; review: its result
 * graded; score: an unplaced item scored), `tokens`, `toolCalls`, `escalated` (true, false, or absent when not known),
 * `date`, and `source` (where the numbers came from: the worker's report). The coordinator adds the line when a worker
 * hands back (`docs/how/hand-out-a-task.md`). Ported from Suite Builder's scripts/score-tune.mjs (its G-357).
 */
final class WorkerLog {
    /** The log, from the root. */
    static final String PATH = "docs/WORKERS.jsonl";
    /** Tool calls a worker of a tier may make before it stops and reports (E-092, `docs/how/hand-out-a-task.md`). */
    static final Map<String, Integer> TOOL_CALL_LIMITS = Map.of("small", 40, "mid", 120);
    /** A mid-tier worker that finished, not escalated, within this share of its limit might have gone a tier lower. */
    static final double LOWER_SHARE = 0.25;
    static final List<String> KINDS = List.of("work", "review", "score");
    static final List<String> TIERS = List.of("small", "mid", "strong");

    /** One worker's line. `escalated` is null when not known; `item` null when the line names none. */
    record Line(String item, Double score, String tier, String kind, double tokens, double toolCalls, Boolean escalated) {}

    /** The lines of a .jsonl text, blank lines skipped; one that is not a line names its number. */
    @SuppressWarnings("unchecked")
    static List<Line> parse(String text) {
        List<Line> lines = new ArrayList<>();
        String[] rows = text.split("\n");
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isBlank()) continue;
            Object parsed;
            try {
                parsed = Json.parse(rows[i]);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(PATH + " line " + (i + 1) + ": not JSON");
            }
            if (!(parsed instanceof Map<?, ?> m) || !(m.get("kind") instanceof String kind && KINDS.contains(kind)) || !(m.get("tier") instanceof String tier && TIERS.contains(tier))
                    || !(m.get("tokens") instanceof Double tokens) || !(m.get("toolCalls") instanceof Double calls)) {
                throw new IllegalArgumentException(PATH + " line " + (i + 1) + ": needs a kind (" + String.join(", ", KINDS) + "), a tier (" + String.join(", ", TIERS) + "), tokens and toolCalls");
            }
            Map<String, Object> r = (Map<String, Object>) m;
            lines.add(new Line(r.get("item") instanceof String s ? s : null, r.get("score") instanceof Double d ? d : null, (String) r.get("tier"), (String) r.get("kind"), tokens, calls, r.get("escalated") instanceof Boolean b ? b : null));
        }
        return lines;
    }

    /** The middle of a list (the mean of the two middle ones for an even count). */
    static double median(List<Double> numbers) {
        List<Double> s = numbers.stream().sorted().toList();
        int mid = s.size() / 2;
        return s.size() % 2 == 1 ? s.get(mid) : (s.get(mid - 1) + s.get(mid)) / 2;
    }

    /**
     * What the log says, as lines: the cost by tier and kind; each worker that escalated below the strong tier (its item
     * scored too low); each mid-tier worker that finished far under its limit (it might go a tier lower); what grading cost
     * against the work it graded.
     */
    static List<String> report(List<Line> lines) {
        if (lines.isEmpty()) return List.of("no workers recorded yet: add a line to " + PATH + " for each worker that hands back");
        List<String> out = new ArrayList<>(List.of("tier\tkind\tn\tmedian tokens\tmedian tool calls\tescalated"));
        for (String tier : TIERS) {
            for (String kind : KINDS) {
                List<Line> rows = lines.stream().filter(l -> l.tier().equals(tier) && l.kind().equals(kind)).toList();
                if (rows.isEmpty()) continue;
                out.add(tier + "\t" + kind + "\t" + rows.size() + "\t" + Math.round(median(rows.stream().map(Line::tokens).toList())) + "\t"
                        + Math.round(median(rows.stream().map(Line::toolCalls).toList())) + "\t" + rows.stream().filter(l -> Boolean.TRUE.equals(l.escalated())).count());
            }
        }
        for (Line l : lines) {
            if (!l.kind().equals("work")) continue;
            if (Boolean.TRUE.equals(l.escalated()) && !l.tier().equals("strong")) out.add(named(l) + ": escalated from " + l.tier() + ": scored too low");
            int limit = TOOL_CALL_LIMITS.getOrDefault(l.tier(), 0);
            if (Boolean.FALSE.equals(l.escalated()) && l.tier().equals("mid") && l.toolCalls() <= LOWER_SHARE * limit) {
                out.add(named(l) + ": finished in " + Math.round(l.toolCalls()) + " of " + limit + " tool calls at mid: might go a tier lower");
            }
        }
        out.add(grading(lines));
        return out;
    }

    private static String named(Line l) {
        return "item " + (l.item() == null ? "?" : l.item()) + (l.score() == null ? "" : " (scored " + Math.round(l.score()) + ")");
    }

    /** A review's tokens against the work's for the same item: the median share, and its range. */
    private static String grading(List<Line> lines) {
        List<Double> ratios = new ArrayList<>();
        for (Line review : lines) {
            if (!review.kind().equals("review") || review.item() == null) continue;
            double built = lines.stream().filter(w -> w.kind().equals("work") && review.item().equals(w.item())).mapToDouble(Line::tokens).sum();
            if (built > 0) ratios.add(review.tokens() / built);
        }
        if (ratios.isEmpty()) return "grading cost: no review line has a work line for the same item yet";
        return "grading cost " + Math.round(median(ratios) * 100) + "% of the work's tokens (median of " + ratios.size() + ", "
                + Math.round(ratios.stream().min(Double::compare).get() * 100) + "% to " + Math.round(ratios.stream().max(Double::compare).get() * 100) + "%)";
    }
}
