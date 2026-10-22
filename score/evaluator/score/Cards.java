package evaluator.score;

import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.ProvenanceTier;
import evaluator.rubric.Produced;
import evaluator.rubric.Ratios;
import evaluator.rubric.Refutation;
import evaluator.rubric.Score;
import evaluator.rubric.ScoreCard;
import evaluator.rubric.State;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facts into a card (docs/SCORE-CARD.md, "The card"): the refuted set aside, each metric's facts through
 * its formula, the ratios from the scores, the tier from Provenance. It reads no code and makes no fact.
 */
public final class Cards {
    /** The tooling bucket until its rule is decided (TODO.md 1.9). */
    static final String NO_BUCKET = "unknown";

    private Cards() {}

    /** The card for these facts, these refutations and these formulas, made by `produced`. */
    public static ScoreCard card(List<Fact> facts, List<Refutation> refutations, Map<Metric, Formula> formulas, Produced produced) {
        List<Fact> live = live(facts, refutations);
        Map<Metric, Score> scores = new EnumMap<>(Metric.class);
        Set<Metric> notMeasured = EnumSet.noneOf(Metric.class);
        for (Metric metric : Metric.values()) {
            Formula formula = formulas.get(metric);
            Score score = formula == null ? null : formula.score(live);
            if (score == null) notMeasured.add(metric);
            else scores.put(metric, score);
        }
        Score provenance = scores.get(Metric.PROVENANCE);
        return new ScoreCard(scores, ratios(scores, live), provenance == null ? null : ProvenanceTier.of(provenance.value()),
                NO_BUCKET, Map.of(), notMeasured, refutations, Map.of(), produced);
    }

    /**
     * The facts that may score: each key once, none refuted. A fact marked refuted with no refutation
     * saying by whom is refused, since the card must say who.
     */
    // Invariant: SC-03
    static List<Fact> live(List<Fact> facts, List<Refutation> refutations) {
        Set<String> refuted = new HashSet<>(), seen = new HashSet<>();
        for (Refutation r : refutations) refuted.add(r.fact());
        List<Fact> live = new ArrayList<>();
        for (Fact f : facts) {
            if (!seen.add(f.key())) throw new IllegalArgumentException("two facts are known by " + f.key() + " (SC-06)");
            if (f.state() == State.REFUTED && !refuted.contains(f.key())) throw new IllegalArgumentException(f.key() + " is refuted, by no one named (SC-03)");
            if (f.scores() && !refuted.contains(f.key())) live.add(f);
        }
        return live;
    }

    /**
     * Quality per thousand source lines, with the size band it ranks in; and Maintainability at Complexity,
     * their product over 100. Each null when a part is missing (Quality per lines also with no line).
     */
    // Decision: E-047
    static Ratios ratios(Map<Metric, Score> scores, List<Fact> live) {
        Fact linesFact = Formulas.find(live, "SIZE.source_lines");
        Double lines = linesFact == null ? null : ((Number) linesFact.value()).doubleValue();
        Score quality = scores.get(Metric.QUALITY), maintainability = scores.get(Metric.MAINTAINABILITY), complexity = scores.get(Metric.COMPLEXITY);
        Double perLines = quality == null || lines == null || lines == 0 ? null : round(quality.value() / (lines / 1000));
        Double atComplexity = maintainability == null || complexity == null ? null : round(maintainability.value() * complexity.value() / 100.0);
        return new Ratios(perLines, lines == null ? null : band(lines), atComplexity);
    }

    /** The size bands Quality per thousand lines ranks within (SC-10): a card ranks only among its own band's. */
    static final double[] BAND_FLOORS = {0, 2_000, 10_000, 50_000};
    static final String[] BANDS = {"under 2k lines", "2k to 10k lines", "10k to 50k lines", "50k lines and over"};

    /** The band a line count falls in. */
    // Invariant: SC-10
    static String band(double lines) {
        int i = BAND_FLOORS.length - 1;
        while (lines < BAND_FLOORS[i]) i--;
        return BANDS[i];
    }

    static double round(double value) {
        return Math.round(100 * value) / 100.0;
    }
}
