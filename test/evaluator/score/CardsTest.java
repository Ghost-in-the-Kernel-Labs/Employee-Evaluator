package evaluator.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void phaseOneScoresNavigabilityAndSaysTheRestWereNotMeasured() {
        ScoreCard card = Cards.card(List.of(fact("NAVIGABILITY.checklist", 99), fact("SIZE.source_lines", 6000)), List.of(), Formulas.phaseOne(), PRODUCED);
        assertEquals(Map.of(Metric.NAVIGABILITY, new Score(99, List.of("NAVIGABILITY.checklist repository:."))), card.scores());
        assertEquals(EnumSet.complementOf(EnumSet.of(Metric.NAVIGABILITY)), card.notMeasured());
        assertNull(card.provenanceTier());
        assertNull(card.ratios().qualityPerThousandLines());
    }

    /** DESIGN.md's example: a 6,000-line clean solution beats a 40,000-line mess on Quality per thousand lines. */
    @Test
    void theRatiosAreComputedFromTheScores() {
        ScoreCard clean = Cards.card(List.of(fact("QUALITY.score", 80), fact("SIZE.source_lines", 6000), fact("MAINTAINABILITY.score", 75), fact("COMPLEXITY.score", 50)), List.of(), plain(), PRODUCED);
        ScoreCard mess = Cards.card(List.of(fact("QUALITY.score", 40), fact("SIZE.source_lines", 40000)), List.of(), plain(), PRODUCED);
        assertEquals(13.33, clean.ratios().qualityPerThousandLines());
        assertEquals(1.5, clean.ratios().maintainabilityPerComplexity());
        assertEquals(1.0, mess.ratios().qualityPerThousandLines());
        assertNull(mess.ratios().maintainabilityPerComplexity(), "no Maintainability, no Complexity: no ratio");
        assertNull(Cards.card(List.of(fact("MAINTAINABILITY.score", 75), fact("COMPLEXITY.score", 0)), List.of(), plain(), PRODUCED).ratios().maintainabilityPerComplexity());
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

    /** SC-09's form, worked by hand: the same card is always these bytes; a path from a submission stays text. */
    @Test
    void theJsonRendering() {
        Fact nav = Fact.measured("NAVIGABILITY.checklist", Subject.repository(), 70, TOOL);
        ScoreCard card = Cards.card(List.of(nav), List.of(new Refutation("SIZE.source_lines file:a\"b", "Ghost")), Formulas.phaseOne(), PRODUCED);
        assertEquals("""
                {
                  "scores": {
                    "navigability": {"value": 70, "facts": ["NAVIGABILITY.checklist repository:."]}
                  },
                  "ratios": {"qualityPerThousandLines": null, "maintainabilityPerComplexity": null},
                  "provenanceTier": null,
                  "toolingBucket": "unknown",
                  "corpusRank": {},
                  "notMeasured": ["quality", "complexity", "size", "ambition", "maintainability", "provenance"],
                  "refuted": [{"fact": "SIZE.source_lines file:a\\"b", "by": "Ghost"}],
                  "review": {},
                  "produced": {"kernelVersion": "0.1", "date": "2026-10-21", "rubricHash": "abc123"}
                }
                """, CardJson.render(card));
        assertEquals(CardJson.render(card), CardJson.render(Cards.card(List.of(nav), card.refuted(), Formulas.phaseOne(), PRODUCED)));
    }

    /** The Markdown, worked by hand: "not measured" where no fact fed, a refuter's name and a fact's key escaped. */
    @Test
    void theMarkdownRendering() {
        ScoreCard card = Cards.card(List.of(fact("NAVIGABILITY.checklist", 70)), List.of(new Refutation("SIZE.x file:a|`b`", "Gh*st")), Formulas.phaseOne(), PRODUCED);
        assertEquals("""
                # Score card

                | Metric | Score | Facts behind it |
                | --- | --- | --- |
                | Quality | not measured |  |
                | Complexity | not measured |  |
                | Size | not measured |  |
                | Ambition | not measured |  |
                | AI navigability | 70 | `NAVIGABILITY.checklist repository:.` |
                | Maintainability | not measured |  |
                | Provenance | not measured |  |

                - Quality per thousand lines: not measured
                - Maintainability per point of Complexity: not measured
                - Provenance tier: not measured
                - Tooling bucket: unknown
                - Refuted: `SIZE.x file:a\\|'b'` by Gh\\*st
                - Produced: kernel 0.1, 2026-10-21, rubric `abc123`
                """, CardMarkdown.render(card));
    }

    /** SC-01: no rendering adds the seven up. */
    @Test
    void noRenderingCarriesAComposite() {
        ScoreCard card = Cards.card(List.of(fact("QUALITY.score", 80), fact("SIZE.score", 60)), List.of(), plain(), PRODUCED);
        for (String text : List.of(CardJson.render(card), CardMarkdown.render(card))) {
            for (String word : List.of("overall", "total", "composite", "average", "140", "70")) assertFalse(text.toLowerCase().contains(word), word + " in " + text);
        }
    }
}
