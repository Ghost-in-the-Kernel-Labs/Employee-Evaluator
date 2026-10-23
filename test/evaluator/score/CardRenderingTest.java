package evaluator.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.Produced;
import evaluator.rubric.Ratios;
import evaluator.rubric.Refutation;
import evaluator.rubric.Score;
import evaluator.rubric.ScoreCard;
import evaluator.rubric.Subject;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The card's JSON and Markdown renderings (split from CardsTest). */
class CardRenderingTest {
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
                  "ratios": {"qualityPerThousandLines": null, "sizeBand": null, "maintainabilityAtComplexity": null},
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

    /** A rank and a review render as objects in the metrics' order, the review's text escaped (phase 2 fills them; 2.6, 2.8). */
    @Test
    void aRankAndAReviewInTheJson() {
        ScoreCard card = new ScoreCard(Map.of(Metric.NAVIGABILITY, new Score(70, List.of("n")), Metric.SIZE, new Score(40, List.of("s"))),
                new Ratios(null, null, null), null, "unknown", Map.of(Metric.NAVIGABILITY, 12, Metric.SIZE, 3),
                EnumSet.complementOf(EnumSet.of(Metric.NAVIGABILITY, Metric.SIZE)), List.of(),
                Map.of(Metric.NAVIGABILITY, "A \"clear\" map", Metric.SIZE, "Small"), PRODUCED);
        String json = CardJson.render(card);
        assertTrue(json.contains("\n  \"corpusRank\": {\"size\": 3, \"navigability\": 12},\n"), json);
        assertTrue(json.contains("\n  \"review\": {\"size\": \"Small\", \"navigability\": \"A \\\"clear\\\" map\"},\n"), json);
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
                - Maintainability at Complexity: not measured
                - Provenance tier: not measured
                - Tooling bucket: unknown
                - Refuted: `SIZE.x file:a\\|'b'` by Gh\\*st
                - Produced: kernel 0.1, 2026-10-21, rubric `abc123`
                """, CardMarkdown.render(card));
    }

    /** A score behind thousands of functions names three in the table and counts the rest. */
    @Test
    void theMarkdownCountsTheFactsPastThree() {
        assertEquals("`a`, `b`, `c` and 2 more (the JSON lists all)", CardMarkdown.facts(new Score(50, List.of("a", "b", "c", "d", "e"))));
        assertEquals("`a`, `b`, `c`", CardMarkdown.facts(new Score(50, List.of("a", "b", "c"))));
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
