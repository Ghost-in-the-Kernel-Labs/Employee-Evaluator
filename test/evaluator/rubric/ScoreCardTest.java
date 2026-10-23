package evaluator.rubric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
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
        assertEquals("AMBITION must be scored or not measured, and is neither",
                assertThrows(IllegalArgumentException.class, () -> card(sizeOnly(), allBut(Metric.SIZE, Metric.AMBITION), null, Map.of())).getMessage());
        assertEquals("SIZE must be scored or not measured, not both",
                assertThrows(IllegalArgumentException.class, () -> card(sizeOnly(), EnumSet.allOf(Metric.class), null, Map.of())).getMessage());
    }

    @Test
    void aScoreIsNotNegativeAndTracedToAFact() {
        new Score(0, List.of("a"));
        new Score(100, List.of("a"));
        new Score(200, List.of("a"));
        assertThrows(IllegalArgumentException.class, () -> new Score(-1, List.of("a")));
        assertThrows(IllegalArgumentException.class, () -> new Score(50, List.of()));
    }

    /** E-071: a counted metric (Size, Complexity or Maintainability, scored against the baseline) may pass 100; every other metric keeps 0–100. */
    @Test
    void onlyACountedMetricMayPassAHundred() {
        assertEquals(List.of(Metric.COMPLEXITY, Metric.SIZE, Metric.MAINTAINABILITY), Arrays.stream(Metric.values()).filter(m -> m.counted).toList());
        card(Map.of(Metric.SIZE, new Score(200, List.of("SIZE.source_lines repository:."))), allBut(Metric.SIZE), null, Map.of());
        card(Map.of(Metric.SIZE, new Score(100, List.of("a"))), allBut(Metric.SIZE), null, Map.of());
        card(Map.of(Metric.COMPLEXITY, new Score(200, List.of("COMPLEXITY.dependency_depth repository:."))), allBut(Metric.COMPLEXITY), null, Map.of());
        card(Map.of(Metric.MAINTAINABILITY, new Score(112, List.of("MAINTAINABILITY.duplication repository:."))), allBut(Metric.MAINTAINABILITY), null, Map.of());
        for (Metric m : Metric.values()) {
            if (m.counted || m == Metric.PROVENANCE) continue; // Provenance carries a tier, so it is held on its own below
            assertEquals(m + " is scored 101, but only a counted metric may pass 100 (E-071)",
                    assertThrows(IllegalArgumentException.class, () -> card(Map.of(m, new Score(101, List.of("a"))), allBut(m), null, Map.of())).getMessage());
            card(Map.of(m, new Score(100, List.of("a"))), allBut(m), null, Map.of());
        }
        assertEquals("PROVENANCE is scored 101, but only a counted metric may pass 100 (E-071)",
                assertThrows(IllegalArgumentException.class, () -> card(Map.of(Metric.PROVENANCE, new Score(101, List.of("a"))), allBut(Metric.PROVENANCE), ProvenanceTier.VERIFIED, Map.of())).getMessage());
        card(Map.of(Metric.PROVENANCE, new Score(100, List.of("a"))), allBut(Metric.PROVENANCE), ProvenanceTier.VERIFIED, Map.of());
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
