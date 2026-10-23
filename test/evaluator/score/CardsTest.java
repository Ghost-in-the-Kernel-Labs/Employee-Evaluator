package evaluator.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.Produced;
import evaluator.rubric.ProvenanceTier;
import evaluator.rubric.Refutation;
import evaluator.rubric.Score;
import evaluator.rubric.ScoreCard;
import evaluator.rubric.State;
import evaluator.rubric.Subject;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardsTest {
    private static final Produced PRODUCED = new Produced("0.1", "2026-10-21", "abc123");
    private static final Evidence TOOL = new Evidence("a tool 1", List.of("src/a.py"));

    private static Fact fact(String kind, Object value) {
        return Fact.measured(kind, Subject.repository(), value, TOOL);
    }

    /** Each metric scored by its one repository fact of `METRIC.score`, for tests of the card's own rules. */
    private static Map<Metric, Formula> plain() {
        Map<Metric, Formula> formulas = new java.util.EnumMap<>(Metric.class);
        for (Metric m : Metric.values()) formulas.put(m, Formulas.repositoryFact(m.name() + ".score"));
        return formulas;
    }

    @Test
    void aMetricWhoseFactsAreAbsentIsNotMeasured() {
        ScoreCard card = Cards.card(List.of(fact("NAVIGABILITY.checklist", 99), fact("SIZE.source_files", 60)), List.of(), Formulas.phaseOne(), PRODUCED);
        assertEquals(Map.of(Metric.NAVIGABILITY, new Score(99, List.of("NAVIGABILITY.checklist repository:."))), card.scores());
        assertEquals(EnumSet.complementOf(EnumSet.of(Metric.NAVIGABILITY)), card.notMeasured(), "no history read, no line count: Provenance and Size not measured");
        assertNull(card.provenanceTier());
        assertNull(card.ratios().qualityPerThousandLines());
    }

    /** DESIGN.md's example, E-047: a 6,000-line clean solution beats a 40,000-line mess; each carries its size band. */
    @Test
    void theRatiosAreComputedFromTheScores() {
        ScoreCard clean = Cards.card(List.of(fact("QUALITY.score", 80), fact("SIZE.source_lines", 6000), fact("MAINTAINABILITY.score", 75), fact("COMPLEXITY.score", 50)), List.of(), plain(), PRODUCED);
        ScoreCard mess = Cards.card(List.of(fact("QUALITY.score", 40), fact("SIZE.source_lines", 40000)), List.of(), plain(), PRODUCED);
        assertEquals(13.33, clean.ratios().qualityPerThousandLines());
        assertEquals("2k to 10k lines", clean.ratios().sizeBand());
        assertTrue(CardMarkdown.render(clean).contains("- Quality per thousand lines: 13.33 (ranked among 2k to 10k lines)\n"));
        ScoreCard noQuality = Cards.card(List.of(fact("SIZE.source_lines", 6000)), List.of(), plain(), PRODUCED);
        assertTrue(CardMarkdown.render(noQuality).contains("- Quality per thousand lines: not measured\n"), "no ratio, so no band to rank in");
        assertEquals(37.5, clean.ratios().maintainabilityAtComplexity(), "75 × 50 / 100");
        assertEquals(1.0, mess.ratios().qualityPerThousandLines());
        assertEquals("10k to 50k lines", mess.ratios().sizeBand());
        assertNull(mess.ratios().maintainabilityAtComplexity(), "no Maintainability, no Complexity: no ratio");
        assertEquals(0.0, Cards.card(List.of(fact("MAINTAINABILITY.score", 75), fact("COMPLEXITY.score", 0)), List.of(), plain(), PRODUCED).ratios().maintainabilityAtComplexity());
    }

    /** E-047: hard and maintainable beats easy and maintainable, and hard and tangled. */
    @Test
    void maintainabilityAtComplexityRewardsOnlyBoth() {
        double hardClean = ratio(80, 80), easyClean = ratio(90, 20), hardTangled = ratio(20, 90);
        assertTrue(hardClean > easyClean && hardClean > hardTangled, hardClean + " " + easyClean + " " + hardTangled);
    }

    private static double ratio(int maintainability, int complexity) {
        return Cards.card(List.of(fact("MAINTAINABILITY.score", maintainability), fact("COMPLEXITY.score", complexity)), List.of(), plain(), PRODUCED).ratios().maintainabilityAtComplexity();
    }

    /** SC-10: the bands' edges; a toy never ranks beside a 6,000-line project. */
    @Test
    void sizeBands() {
        assertEquals("under 2k lines", Cards.band(0));
        assertEquals("under 2k lines", Cards.band(1999));
        assertEquals("2k to 10k lines", Cards.band(2000));
        assertEquals("10k to 50k lines", Cards.band(10000));
        assertEquals("50k lines and over", Cards.band(50000));
        assertNull(Cards.card(List.of(fact("QUALITY.score", 80)), List.of(), plain(), PRODUCED).ratios().sizeBand(), "no line count, no band");
    }

    /** E-053: more than half the evidence names the bucket; no majority is mixed; read and none is none detected. */
    @Test
    void theToolingBucket() {
        Fact shape = fact("PROVENANCE.history.shape", 60);
        assertEquals("Claude", Buckets.of(List.of(shape, fact("PROVENANCE.history.agent_tool.claude", 3), fact("PROVENANCE.history.agent_tool.cursor", 2))));
        assertEquals("mixed", Buckets.of(List.of(shape, fact("PROVENANCE.history.agent_tool.claude", 2), fact("PROVENANCE.history.agent_tool.cursor", 2))));
        assertEquals("none detected", Buckets.of(List.of(shape)));
        assertEquals("unknown", Buckets.of(List.of(fact("PROVENANCE.history.agent_tool.claude", 3))), "no history read");
        assertEquals("Claude", Cards.card(List.of(shape, fact("PROVENANCE.history.agent_tool.claude", 1)), List.of(), Formulas.phaseOne(), PRODUCED).toolingBucket());
    }

    @Test
    void theTierIsProvenances() {
        assertEquals(ProvenanceTier.PARTIAL, Cards.card(List.of(fact("PROVENANCE.score", 65)), List.of(), plain(), PRODUCED).provenanceTier());
    }

    /** SC-03: a refuted fact never scores, and the card says who refuted it. */
    @Test
    void aRefutedFactNeverScores() {
        Fact quality = fact("QUALITY.score", 90);
        List<Refutation> by = List.of(new Refutation(quality.key(), "Ghost"));
        ScoreCard card = Cards.card(List.of(quality), by, plain(), PRODUCED);
        assertFalse(card.scores().containsKey(Metric.QUALITY));
        assertEquals(by, card.refuted());
        ScoreCard marked = Cards.card(List.of(quality.withState(State.REFUTED)), by, plain(), PRODUCED);
        assertFalse(marked.scores().containsKey(Metric.QUALITY));
        assertThrows(IllegalArgumentException.class, () -> Cards.card(List.of(quality.withState(State.REFUTED)), List.of(), plain(), PRODUCED));
    }

    /** SC-06: one fact per kind and subject. */
    @Test
    void twoFactsWithOneKeyAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Cards.card(List.of(fact("SIZE.source_lines", 1), fact("SIZE.source_lines", 2)), List.of(), plain(), PRODUCED));
    }
}
