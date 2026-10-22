package evaluator.rubric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScoreCardTest {
    private static final Produced PRODUCED = new Produced("0.1", "2026-10-19", "rubric-hash");

    private static ScoreCard card(Map<Metric, Score> scores, Set<Metric> notMeasured, ProvenanceTier tier, Map<Metric, Integer> rank) {
        return new ScoreCard(scores, new Ratios(null, null, null), tier, "unknown", rank, notMeasured, List.of(), Map.of(), PRODUCED);
    }

    private static Map<Metric, Score> sizeOnly() {
        return Map.of(Metric.SIZE, new Score(40, List.of("SIZE.source_lines repository:.")));
    }

    private static Set<Metric> allBut(Metric... scored) {
        Set<Metric> rest = EnumSet.allOf(Metric.class);
        rest.removeAll(List.of(scored));
        return rest;
    }

    @Test
    void everyMetricIsScoredOrNotMeasured() {
        ScoreCard card = card(sizeOnly(), allBut(Metric.SIZE), null, Map.of());
        assertEquals(6, card.notMeasured().size());
        assertThrows(IllegalArgumentException.class, () -> card(sizeOnly(), allBut(Metric.SIZE, Metric.AMBITION), null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> card(sizeOnly(), EnumSet.allOf(Metric.class), null, Map.of()));
    }

    @Test
    void aScoreIsZeroToAHundredAndTracedToAFact() {
        new Score(0, List.of("a"));
        new Score(100, List.of("a"));
        assertThrows(IllegalArgumentException.class, () -> new Score(101, List.of("a")));
        assertThrows(IllegalArgumentException.class, () -> new Score(-1, List.of("a")));
        assertThrows(IllegalArgumentException.class, () -> new Score(50, List.of()));
    }

    @Test
    void theTierIsTheProvenanceScores() {
        assertEquals(ProvenanceTier.VERIFIED, ProvenanceTier.of(80));
        assertEquals(ProvenanceTier.PARTIAL, ProvenanceTier.of(79));
        assertEquals(ProvenanceTier.PARTIAL, ProvenanceTier.of(50));
        assertEquals(ProvenanceTier.UNVERIFIED, ProvenanceTier.of(49));
        Map<Metric, Score> scores = new EnumMap<>(sizeOnly());
        scores.put(Metric.PROVENANCE, new Score(65, List.of("PROVENANCE.history.spread repository:.")));
        card(scores, allBut(Metric.SIZE, Metric.PROVENANCE), ProvenanceTier.PARTIAL, Map.of());
        assertThrows(IllegalArgumentException.class, () -> card(scores, allBut(Metric.SIZE, Metric.PROVENANCE), ProvenanceTier.VERIFIED, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> card(sizeOnly(), allBut(Metric.SIZE), ProvenanceTier.UNVERIFIED, Map.of()));
    }

    @Test
    void onlyAScoredMetricIsRanked() {
        card(sizeOnly(), allBut(Metric.SIZE), null, Map.of(Metric.SIZE, 70));
        assertThrows(IllegalArgumentException.class, () -> card(sizeOnly(), allBut(Metric.SIZE), null, Map.of(Metric.QUALITY, 70)));
    }
}
