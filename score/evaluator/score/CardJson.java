package evaluator.score;

import evaluator.rubric.Json;
import evaluator.rubric.Metric;
import evaluator.rubric.Refutation;
import evaluator.rubric.Score;
import evaluator.rubric.ScoreCard;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A card as JSON, the same card always the same bytes (SC-09): fields in docs/SCORE-CARD.md's order,
 * metrics in the enum's, two spaces of indent, a newline at the end. No field adds the scores up (SC-01).
 */
// Invariant: SC-09
public final class CardJson {
    private CardJson() {}

    /** The card's JSON rendering. */
    public static String render(ScoreCard card) {
        return render(card, null);
    }

    /**
     * The card's JSON rendering, naming the judgment it was measured through (TODO.md 2.2j) in `produced` when there is one; with
     * none, the same bytes as ever.
     */
    public static String render(ScoreCard card, Judged judged) {
        StringBuilder out = new StringBuilder("{\n");
        out.append("  \"scores\": {");
        String scores = card.scores().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> "\n    " + Json.quote(name(e.getKey())) + ": " + score(e.getValue())).collect(Collectors.joining(","));
        out.append(scores.isEmpty() ? "}" : scores + "\n  }").append(",\n");
        out.append("  \"ratios\": {\"qualityPerThousandLines\": ").append(card.ratios().qualityPerThousandLines())
                .append(", \"sizeBand\": ").append(card.ratios().sizeBand() == null ? "null" : Json.quote(card.ratios().sizeBand()))
                .append(", \"maintainabilityAtComplexity\": ").append(card.ratios().maintainabilityAtComplexity()).append("},\n");
        out.append("  \"provenanceTier\": ").append(card.provenanceTier() == null ? "null" : Json.quote(card.provenanceTier().title)).append(",\n");
        out.append("  \"toolingBucket\": ").append(Json.quote(card.toolingBucket())).append(",\n");
        out.append("  \"corpusRank\": ").append(object(card.corpusRank(), String::valueOf)).append(",\n");
        out.append("  \"notMeasured\": [").append(card.notMeasured().stream().sorted().map(m -> Json.quote(name(m))).collect(Collectors.joining(", "))).append("],\n");
        out.append("  \"refuted\": [").append(card.refuted().stream().map(CardJson::refutation).collect(Collectors.joining(", "))).append("],\n");
        out.append("  \"review\": ").append(object(card.review(), Json::quote)).append(",\n");
        out.append("  \"produced\": {\"kernelVersion\": ").append(Json.quote(card.produced().kernelVersion()))
                .append(", \"date\": ").append(Json.quote(card.produced().date()))
                .append(", \"rubricHash\": ").append(Json.quote(card.produced().rubricHash()))
                .append(judged == null ? "" : ", \"judged\": {\"promptHash\": " + Json.quote(judged.promptHash()) + ", \"models\": ["
                        + judged.models().stream().map(Json::quote).collect(Collectors.joining(", ")) + "]}")
                .append("}\n}\n");
        return out.toString();
    }

    /** A metric's name in the card: its enum name in lower case. */
    static String name(Metric metric) {
        return metric.name().toLowerCase();
    }

    private static String score(Score s) {
        return "{\"value\": " + s.value() + ", \"facts\": [" + s.facts().stream().map(Json::quote).collect(Collectors.joining(", ")) + "]}";
    }

    private static String refutation(Refutation r) {
        return "{\"fact\": " + Json.quote(r.fact()) + ", \"by\": " + Json.quote(r.by()) + "}";
    }

    private static <V> String object(Map<Metric, V> values, java.util.function.Function<V, String> render) {
        List<String> parts = values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> Json.quote(name(e.getKey())) + ": " + render.apply(e.getValue())).toList();
        return "{" + String.join(", ", parts) + "}";
    }
}
