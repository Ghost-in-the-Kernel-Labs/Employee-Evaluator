package evaluator.score;

import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.Score;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The formulas the kernel scores with, by metric (E-045). A metric with no formula here is not measured
 * (SC-07), never 0: each one's formula is a decision of its own, made with Ghost, before it scores.
 */
// Decision: E-045
public final class Formulas {
    private Formulas() {}

    /** The kinds of commit GitHub's own record backs as the applicant's (E-043): signed, or pushed by them. */
    static final List<String> VERIFIED = List.of("signed", "pushed");
    /** Every kind a commit is classed as, so their counts sum to the commits read. */
    static final List<String> COMMIT_KINDS = List.of("signed", "pushed", "claimed", "foreign", "unmatched");
    /** The highest Provenance the history's shape earns alone: Partial, never Verified, with nothing GitHub backs. */
    static final int HISTORY_ALONE_CAP = 79;

    /** The dependency depth that reads 100 on Complexity's depth part: a chain of 25 links and over. */
    static final int FULL_DEPTH = 25;

    /**
     * Phase 1's formulas: AI navigability is its checklist (E-044), already 0–100; Provenance (E-046);
     * Size (E-048); Complexity (E-050); Maintainability (E-051).
     */
    public static Map<Metric, Formula> phaseOne() {
        return Map.of(Metric.NAVIGABILITY, Formulas.repositoryFact("NAVIGABILITY.checklist"), Metric.PROVENANCE, Formulas::provenance,
                Metric.SIZE, Formulas::size, Metric.COMPLEXITY, Formulas::complexity, Metric.MAINTAINABILITY, Maintainability::score);
    }

    /**
     * Complexity, the tools' half: 0.6 × the total cognitive complexity of every function on Size's log
     * scale (100 and under read 0, 100,000 and over 100) + 0.4 × the dependency depth over 25 links. Per
     * function averages are left out: they say more of style than of the problem. No depth read, no score.
     */
    // Decision: E-050
    static Score complexity(List<Fact> facts) {
        Fact depth = find(facts, "COMPLEXITY.dependency_depth");
        if (depth == null) return null;
        List<String> keys = new ArrayList<>(List.of(depth.key()));
        double cognitive = 0;
        for (Fact f : facts) {
            if (!f.kind().equals("COMPLEXITY.cognitive")) continue;
            cognitive += ((Number) f.value()).doubleValue();
            keys.add(f.key());
        }
        double depthPart = 100.0 * Math.min(((Number) depth.value()).intValue(), FULL_DEPTH) / FULL_DEPTH;
        return new Score((int) Math.round(0.6 * logScale(cognitive) + 0.4 * depthPart), keys);
    }

    /** A count on a log scale: 100 and under read 0, each tenfold step a third, 100,000 and over read 100. */
    static double logScale(double n) {
        return n <= 100 ? 0 : Math.min(100, 100 * (Math.log10(n) - 2) / 3);
    }

    /**
     * Size: source lines on a log scale, vendored and generated code already set aside (E-026): 100 lines
     * and under read 0, 1,000 read 33, 10,000 read 67, 100,000 and over read 100. Each tenfold step counts alike.
     */
    // Decision: E-048
    static Score size(List<Fact> facts) {
        Fact lines = find(facts, "SIZE.source_lines");
        if (lines == null) return null;
        return new Score((int) Math.round(logScale(((Number) lines.value()).doubleValue())), List.of(lines.key()));
    }

    /**
     * Provenance: 0.6 × the history's shape + 0.4 × 100 × the share of commits GitHub backs as the
     * applicant's (signed or pushed by them). With no authorship read, the shape alone, capped at 79, so a
     * history nothing on GitHub backs is never Verified. No shape, no score.
     */
    // Decision: E-046
    static Score provenance(List<Fact> facts) {
        Fact shape = find(facts, "PROVENANCE.history.shape");
        if (shape == null) return null;
        int history = ((Number) shape.value()).intValue();
        List<String> keys = new ArrayList<>(List.of(shape.key()));
        int verified = 0, all = 0;
        for (String kind : COMMIT_KINDS) {
            Fact count = find(facts, "PROVENANCE.authorship." + kind);
            if (count == null) continue;
            int n = ((Number) count.value()).intValue();
            keys.add(count.key());
            all += n;
            if (VERIFIED.contains(kind)) verified += n;
        }
        if (all == 0) return new Score(Math.min(history, HISTORY_ALONE_CAP), List.of(shape.key()));
        return new Score((int) Math.round(0.6 * history + 0.4 * 100.0 * verified / all), keys);
    }

    /** The repository-wide fact of this kind, or null. */
    static Fact find(List<Fact> facts, String kind) {
        for (Fact f : facts) if (f.kind().equals(kind) && f.subject().kind() == Subject.Kind.REPOSITORY) return f;
        return null;
    }

    /** A formula that is one repository-wide fact already on 0–100, taken as it is. */
    static Formula repositoryFact(String kind) {
        return facts -> {
            Fact f = find(facts, kind);
            return f == null ? null : new Score(((Number) f.value()).intValue(), List.of(f.key()));
        };
    }
}
