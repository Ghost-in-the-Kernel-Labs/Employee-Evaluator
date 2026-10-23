package evaluator.rubric;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One submission's score card (docs/SCORE-CARD.md, "The card"): seven scores and no composite, the
 * ratios, the provenance tier, the tooling bucket, the corpus rank once the bucket is large enough,
 * the metrics not measured, the refuted facts, the review and what produced it.
 */
// Invariant: SC-01
public record ScoreCard(
        Map<Metric, Score> scores,
        Ratios ratios,
        ProvenanceTier provenanceTier,
        String toolingBucket,
        Map<Metric, Integer> corpusRank,
        Set<Metric> notMeasured,
        List<Refutation> refuted,
        Map<Metric, String> review,
        Produced produced) {

    public ScoreCard {
        scores = scores.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(scores));
        notMeasured = notMeasured.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(notMeasured));
        corpusRank = Map.copyOf(corpusRank);
        refuted = List.copyOf(refuted);
        review = Map.copyOf(review);
        everyMetricAccountedFor(scores, notMeasured);
        onlyCountedMayPassAHundred(scores);
        tierAgrees(scores.get(Metric.PROVENANCE), provenanceTier);
        for (Metric ranked : corpusRank.keySet()) {
            if (!scores.containsKey(ranked)) throw new IllegalArgumentException(ranked + " is ranked but has no score");
        }
    }

    /** Each of the seven is scored or "not measured", never both and never neither: an absence is not a zero. */
    // Invariant: SC-07
    private static void everyMetricAccountedFor(Map<Metric, Score> scores, Set<Metric> notMeasured) {
        for (Metric metric : Metric.values()) {
            boolean scored = scores.containsKey(metric), absent = notMeasured.contains(metric);
            if (scored == absent) throw new IllegalArgumentException(metric + " must be scored or not measured, " + (scored ? "not both" : "and is neither"));
        }
    }

    /** Each score is 0–100, but a counted metric's (scored against the baseline, E-070) has no ceiling (E-071). */
    // Decision: E-071
    private static void onlyCountedMayPassAHundred(Map<Metric, Score> scores) {
        scores.forEach((metric, score) -> {
            if (score.value() > 100 && !metric.counted) throw new IllegalArgumentException(metric + " is scored " + score.value() + ", but only a counted metric may pass 100 (E-071)");
        });
    }

    /** The tier is the provenance score's, and there is none when provenance was not measured. */
    private static void tierAgrees(Score provenance, ProvenanceTier tier) {
        ProvenanceTier expected = provenance == null ? null : ProvenanceTier.of(provenance.value());
        if (expected != tier) throw new IllegalArgumentException("the provenance tier is " + expected + ", not " + tier);
    }
}
