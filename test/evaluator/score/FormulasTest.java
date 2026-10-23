package evaluator.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.Produced;
import evaluator.rubric.ProvenanceTier;
import evaluator.rubric.Score;
import evaluator.rubric.ScoreCard;
import evaluator.rubric.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The formulas, each worked by hand: a metric's facts in, its score out (split from CardsTest). */
class FormulasTest {
    private static final Produced PRODUCED = new Produced("0.1", "2026-10-21", "abc123");
    private static final Evidence TOOL = new Evidence("a tool 1", List.of("src/a.py"));

    private static Fact fact(String kind, Object value) {
        return Fact.measured(kind, Subject.repository(), value, TOOL);
    }

    /** E-046 worked by hand: shape 70; 6 signed, 2 pushed, 1 claimed, 1 foreign of 10: 0.6 × 70 + 0.4 × 80 = 74. */
    @Test
    void provenanceIsTheShapeAndWhatGitHubBacks() {
        List<Fact> facts = List.of(fact("PROVENANCE.history.shape", 70), fact("PROVENANCE.authorship.signed", 6), fact("PROVENANCE.authorship.pushed", 2),
                fact("PROVENANCE.authorship.claimed", 1), fact("PROVENANCE.authorship.foreign", 1), fact("PROVENANCE.authorship.unmatched", 0));
        ScoreCard card = Cards.card(facts, List.of(), Formulas.phaseOne(), PRODUCED);
        assertEquals(74, card.scores().get(Metric.PROVENANCE).value());
        assertEquals(6, card.scores().get(Metric.PROVENANCE).facts().size(), "the shape and the five counts");
        assertEquals(ProvenanceTier.PARTIAL, card.provenanceTier());
        List<Fact> allSigned = List.of(fact("PROVENANCE.history.shape", 70), fact("PROVENANCE.authorship.signed", 10));
        assertEquals(82, Cards.card(allSigned, List.of(), Formulas.phaseOne(), PRODUCED).scores().get(Metric.PROVENANCE).value());
    }

    /**
     * E-070 worked by hand against the baseline, Suite Builder at 32762a6 with 84,971 source lines: Size = 100 × lines ÷ 84,971,
     * rounded. 21,243 lines: 100 × 21,243 ÷ 84,971 = 25.0003, so 25. 42,486: 50.0006, 50. 100,000: 117.69, 118. Twice the baseline,
     * 169,942: exactly 200 (E-071: no ceiling). 424 lines: 0.499, 0; 425: 0.5002, 1. An empty project reads 0.
     */
    @Test
    void sizeIsLinesAgainstTheBaseline() {
        int[][] cases = {{0, 0}, {424, 0}, {425, 1}, {21_243, 25}, {42_486, 50}, {84_971, 100}, {100_000, 118}, {169_942, 200}, {849_710, 1000}};
        for (int[] c : cases) assertEquals(new Score(c[1], List.of("SIZE.source_lines repository:.")), Formulas.size(List.of(fact("SIZE.source_lines", c[0]))), c[0] + " lines");
        assertNull(Formulas.size(List.of(fact("SIZE.source_files", 12))), "no line count, no score");
        assertEquals(84_971, Formulas.BASELINE_SOURCE_LINES, "Suite Builder's measure at the baseline commit");
        assertEquals("32762a6110ebd73246ca2413b1d54db947aee179", Formulas.BASELINE_COMMIT);
    }

    /** The rubric names what Size is counted against, so a card's hash changes with the baseline (E-070). */
    @Test
    void theRubricNamesTheBaseline() {
        assertTrue(Formulas.RUBRIC.contains("size E-070 against 32762a6110ebd73246ca2413b1d54db947aee179 (84971 source lines)"));
        assertTrue(Formulas.RUBRIC.contains(Formulas.BASELINE_COMMIT) && Formulas.RUBRIC.contains("(" + Formulas.BASELINE_SOURCE_LINES + " source lines)"));
        assertTrue(Formulas.RUBRIC.startsWith("phase-1 rubric 3:"));
        assertTrue(Formulas.RUBRIC.contains("complexity E-074 against 32762a6110ebd73246ca2413b1d54db947aee179 (13361 cognitive, depth 28)"));
    }

    /**
     * E-074 worked by hand against the baseline, Suite Builder at 32762a6 with a total cognitive complexity of 13,361 and a dependency
     * depth of 28: Complexity = 0.6 × (100 × cognitive ÷ 13,361) + 0.4 × (100 × depth ÷ 28), rounded. The baseline itself: 0.6 × 100
     * + 0.4 × 100 = 100. Twice it (26,722 and 56): 200. Cognitive 6,681 (50.004) and depth 7 (25): 30.002 + 10 = 40.002, 40.
     * Cognitive 13,361 and depth 0: 60 + 0 = 60. Cognitive 0 and depth 14: 0 + 20 = 20. Cognitive 111: 0.6 × 0.8308 = 0.4985, 0; 112:
     * 0.5030, 1. Depth 1 alone: 0.4 × 3.5714 = 1.43, 1; depth 28 alone: 40. An empty project (depth 0, no function) reads 0. Each
     * function's fact is listed behind the score.
     */
    @Test
    void complexityIsTotalCognitiveAndDepthAgainstTheBaseline() {
        assertEquals(13_361, Formulas.BASELINE_COGNITIVE, "Suite Builder's total cognitive complexity at the baseline commit");
        assertEquals(28, Formulas.BASELINE_DEPTH, "Suite Builder's dependency depth at the baseline commit");
        int[][] cases = {{0, 0, 0}, {13_361, 28, 100}, {26_722, 56, 200}, {6_681, 7, 40}, {13_361, 0, 60}, {0, 14, 20}, {111, 0, 0}, {112, 0, 1},
                {0, 1, 1}, {0, 28, 40}, {133_610, 280, 1000}};
        for (int[] c : cases) assertEquals(c[2], Formulas.complexity(List.of(fact("COMPLEXITY.dependency_depth", c[1]), function("f", c[0]))).value(), c[0] + " cognitive, depth " + c[1]);
        assertEquals(0, Formulas.complexity(List.of(fact("COMPLEXITY.dependency_depth", 0))).value(), "an empty project");
        List<Fact> split = List.of(fact("COMPLEXITY.dependency_depth", 28), function("a", 4_000), function("b", 9_000), function("c", 361),
                Fact.measured("COMPLEXITY.cyclomatic", new Subject(Subject.Kind.FUNCTION, "a"), 99, TOOL));
        Score baseline = Formulas.complexity(split);
        assertEquals(100, baseline.value(), "functions summed: the baseline reads 100 against itself");
        assertEquals(List.of("COMPLEXITY.dependency_depth repository:.", "COMPLEXITY.cognitive function:a", "COMPLEXITY.cognitive function:b", "COMPLEXITY.cognitive function:c"), baseline.facts());
        assertEquals(40, Formulas.complexity(List.of(fact("COMPLEXITY.dependency_depth", 28))).value(), "no function: only the depth part");
        assertNull(Formulas.complexity(List.of(function("a", 400))), "no depth read, no score");
    }

    private static Fact function(String name, int cognitive) {
        return Fact.measured("COMPLEXITY.cognitive", new Subject(Subject.Kind.FUNCTION, name), cognitive, TOOL);
    }

    /** E-052: 0.6 × 0.9 + 0.4 × 0.5 = 0.74; one alone when only it was read. */
    @Test
    void qualityIsPassRateAndCoverage() {
        Fact pass = fact("QUALITY.tests.pass_rate", 0.9), coverage = fact("QUALITY.tests.coverage", 0.5);
        assertEquals(new Score(74, List.of(pass.key(), coverage.key())), Formulas.quality(List.of(pass, coverage)));
        assertEquals(90, Formulas.quality(List.of(pass)).value());
        assertEquals(50, Formulas.quality(List.of(coverage, fact("MAINTAINABILITY.test_to_code_ratio", 0.4))).value());
    }

    /** No test file is a measurement, 0; tests that could not be run are not. */
    @Test
    void noTestsReadZeroButUnrunTestsAreNotMeasured() {
        Fact none = fact("MAINTAINABILITY.test_to_code_ratio", 0);
        assertEquals(new Score(0, List.of(none.key())), Formulas.quality(List.of(none)));
        assertNull(Formulas.quality(List.of(fact("MAINTAINABILITY.test_to_code_ratio", 0.6), fact("QUALITY.tests.network_attempts", 2))), "tests there, no run read");
        assertNull(Formulas.quality(List.of()), "no code, no ratio: nothing to say");
    }

    /** With nothing from GitHub, the shape alone, never Verified: a history can be written by anyone. */
    @Test
    void provenanceFromTheHistoryAloneIsCappedBelowVerified() {
        ScoreCard high = Cards.card(List.of(fact("PROVENANCE.history.shape", 95)), List.of(), Formulas.phaseOne(), PRODUCED);
        assertEquals(new Score(79, List.of("PROVENANCE.history.shape repository:.")), high.scores().get(Metric.PROVENANCE));
        assertEquals(ProvenanceTier.PARTIAL, high.provenanceTier());
        assertEquals(40, Cards.card(List.of(fact("PROVENANCE.history.shape", 40), fact("PROVENANCE.authorship.signed", 0)), List.of(), Formulas.phaseOne(), PRODUCED)
                .scores().get(Metric.PROVENANCE).value(), "no commit read on GitHub reads as no authorship");
        assertNull(Formulas.provenance(List.of(fact("PROVENANCE.authorship.signed", 5))), "no shape, no score");
    }

    /** The rubric's hash is the first 12 hex digits of its SHA-256; it changes with the rubric, as every card's golden does. */
    @Test
    void theRubricsHash() {
        assertEquals("4d7287ec37e6", Formulas.rubricHash());
    }
}
