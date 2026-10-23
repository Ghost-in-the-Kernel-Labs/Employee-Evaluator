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
    /**
     * The rubric's name and version: every formula and its decision. A card carries its hash, so two cards
     * scored by different rubrics are told apart; a formula changed gives it a new version.
     */
    static final String RUBRIC = "phase-1 rubric 3: navigability E-044, provenance E-046, ratios E-047, size E-070 against "
            + "32762a6110ebd73246ca2413b1d54db947aee179 (84971 source lines), complexity E-074 against "
            + "32762a6110ebd73246ca2413b1d54db947aee179 (13361 cognitive, depth 28), maintainability E-051, quality E-052, bucket E-053";

    /**
     * The baseline Size is counted against (E-070): Suite Builder at this commit (main, 2026-10-23), read by this kernel
     * (`evaluator score --no-run`, nothing of its own run) as 84,971 source lines in 916 files, 122 more set aside (E-026).
     */
    static final String BASELINE_COMMIT = "32762a6110ebd73246ca2413b1d54db947aee179";
    /** `SIZE.source_lines` of the baseline commit: the repository that reads Size 100. */
    static final long BASELINE_SOURCE_LINES = 84_971;

    private Formulas() {}

    /** The rubric's hash, the first 12 hex digits of its SHA-256. */
    public static String rubricHash() {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(RUBRIC.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("every JDK has SHA-256", e);
        }
    }

    /** The kinds of commit GitHub's own record backs as the applicant's (E-043): signed, or pushed by them. */
    static final List<String> VERIFIED = List.of("signed", "pushed");
    /** Every kind a commit is classed as, so their counts sum to the commits read. */
    static final List<String> COMMIT_KINDS = List.of("signed", "pushed", "claimed", "foreign", "unmatched");
    /** The highest Provenance the history's shape earns alone: Partial, never Verified, with nothing GitHub backs. */
    static final int HISTORY_ALONE_CAP = 79;

    /** `COMPLEXITY.cognitive` summed over every function of the baseline commit: the total that reads 100 on Complexity's cognitive part. */
    static final long BASELINE_COGNITIVE = 13_361;
    /** `COMPLEXITY.dependency_depth` of the baseline commit: the chain of links that reads 100 on Complexity's depth part. */
    static final int BASELINE_DEPTH = 28;

    /**
     * Phase 1's formulas: AI navigability is its checklist (E-044), already 0–100; Provenance (E-046);
     * Size (E-070, against the baseline); Complexity (E-074, against the baseline); Maintainability (E-051); Quality (E-052).
     */
    public static Map<Metric, Formula> phaseOne() {
        return Map.of(Metric.NAVIGABILITY, Formulas.repositoryFact("NAVIGABILITY.checklist"), Metric.PROVENANCE, Formulas::provenance,
                Metric.SIZE, Formulas::size, Metric.COMPLEXITY, Formulas::complexity, Metric.MAINTAINABILITY, Maintainability::score,
                Metric.QUALITY, Formulas::quality);
    }

    /**
     * Quality, the tools' half: 0.6 × the pass rate + 0.4 × the coverage of the submission's own tests, run
     * in the sandbox (E-037); one alone when only it was read. No test file at all reads 0: that was looked
     * for and found. Tests there but not run (no lockfile, a run that reached for the network) are not measured.
     */
    // Decision: E-052
    static Score quality(List<Fact> facts) {
        Fact pass = find(facts, "QUALITY.tests.pass_rate"), coverage = find(facts, "QUALITY.tests.coverage");
        if (pass != null && coverage != null) return new Score((int) Math.round(100 * (0.6 * share(pass) + 0.4 * share(coverage))), List.of(pass.key(), coverage.key()));
        Fact one = pass != null ? pass : coverage;
        if (one != null) return new Score((int) Math.round(100 * share(one)), List.of(one.key()));
        Fact tests = find(facts, "MAINTAINABILITY.test_to_code_ratio");
        return tests != null && share(tests) == 0 ? new Score(0, List.of(tests.key())) : null;
    }

    private static double share(Fact f) {
        return ((Number) f.value()).doubleValue();
    }

    /**
     * Complexity, the tools' half: 0.6 × (100 × the total cognitive complexity of every function ÷ the baseline's 13,361) + 0.4 × (100 ×
     * the dependency depth ÷ the baseline's 28), rounded (Suite Builder at 32762a6 reads 100). Linear and with no ceiling (E-071): an
     * empty project reads 0, twice the baseline's measures 200. Per-function averages are left out: they say more of style than of
     * the problem. No depth read, no score.
     */
    // Decision: E-074
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
        double cognitivePart = 100.0 * cognitive / BASELINE_COGNITIVE;
        double depthPart = 100.0 * ((Number) depth.value()).doubleValue() / BASELINE_DEPTH;
        return new Score((int) Math.round(0.6 * cognitivePart + 0.4 * depthPart), keys);
    }

    /**
     * Size: 100 × the repository's source lines ÷ the baseline's 84,971 (Suite Builder at 32762a6), rounded, vendored and
     * generated code already set aside (E-026). Linear: an empty project reads 0, the baseline 100, twice its lines 200;
     * there is no ceiling (E-071). No line count, no score.
     */
    // Decision: E-070
    static Score size(List<Fact> facts) {
        Fact lines = find(facts, "SIZE.source_lines");
        if (lines == null) return null;
        return new Score((int) Math.round(100.0 * ((Number) lines.value()).doubleValue() / BASELINE_SOURCE_LINES), List.of(lines.key()));
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
