package evaluator.score;

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
        StringBuilder out = new StringBuilder("{\n");
        out.append("  \"scores\": {");
        String scores = card.scores().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> "\n    " + quote(name(e.getKey())) + ": " + score(e.getValue())).collect(Collectors.joining(","));
        out.append(scores.isEmpty() ? "}" : scores + "\n  }").append(",\n");
        out.append("  \"ratios\": {\"qualityPerThousandLines\": ").append(card.ratios().qualityPerThousandLines())
                .append(", \"maintainabilityPerComplexity\": ").append(card.ratios().maintainabilityPerComplexity()).append("},\n");
        out.append("  \"provenanceTier\": ").append(card.provenanceTier() == null ? "null" : quote(card.provenanceTier().title)).append(",\n");
        out.append("  \"toolingBucket\": ").append(quote(card.toolingBucket())).append(",\n");
        out.append("  \"corpusRank\": ").append(object(card.corpusRank(), String::valueOf)).append(",\n");
        out.append("  \"notMeasured\": [").append(card.notMeasured().stream().sorted().map(m -> quote(name(m))).collect(Collectors.joining(", "))).append("],\n");
        out.append("  \"refuted\": [").append(card.refuted().stream().map(CardJson::refutation).collect(Collectors.joining(", "))).append("],\n");
        out.append("  \"review\": ").append(object(card.review(), CardJson::quote)).append(",\n");
        out.append("  \"produced\": {\"kernelVersion\": ").append(quote(card.produced().kernelVersion()))
                .append(", \"date\": ").append(quote(card.produced().date()))
                .append(", \"rubricHash\": ").append(quote(card.produced().rubricHash())).append("}\n}\n");
        return out.toString();
    }

    /** A metric's name in the card: its enum name in lower case. */
    static String name(Metric metric) {
        return metric.name().toLowerCase();
    }

    private static String score(Score s) {
        return "{\"value\": " + s.value() + ", \"facts\": [" + s.facts().stream().map(CardJson::quote).collect(Collectors.joining(", ")) + "]}";
    }

    private static String refutation(Refutation r) {
        return "{\"fact\": " + quote(r.fact()) + ", \"by\": " + quote(r.by()) + "}";
    }

    private static <V> String object(Map<Metric, V> values, java.util.function.Function<V, String> render) {
        List<String> parts = values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> quote(name(e.getKey())) + ": " + render.apply(e.getValue())).toList();
        return "{" + String.join(", ", parts) + "}";
    }

    /** A JSON string: quotes, backslashes and control characters escaped, so a path from a submission stays text. */
    static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
