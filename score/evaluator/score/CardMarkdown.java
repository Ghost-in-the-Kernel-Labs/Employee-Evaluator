package evaluator.score;

import evaluator.rubric.Metric;
import evaluator.rubric.Ratios;
import evaluator.rubric.Refutation;
import evaluator.rubric.Score;
import evaluator.rubric.ScoreCard;
import java.util.stream.Collectors;

/**
 * A card as Markdown, for a person: the seven in a table, each score with the facts behind it, then the
 * ratios, the tier and what made it. A metric no fact fed reads "not measured" (SC-07); there is no total
 * (SC-01). Every text from a submission is escaped, so it renders as text and never as markup.
 */
public final class CardMarkdown {
    private CardMarkdown() {}

    /** The card's Markdown rendering. */
    public static String render(ScoreCard card) {
        StringBuilder out = new StringBuilder("# Score card\n\n| Metric | Score | Facts behind it |\n| --- | --- | --- |\n");
        for (Metric metric : Metric.values()) {
            Score s = card.scores().get(metric);
            String facts = s == null ? "" : s.facts().stream().map(CardMarkdown::code).collect(Collectors.joining(", "));
            out.append("| ").append(metric.title).append(" | ").append(s == null ? "not measured" : String.valueOf(s.value()))
                    .append(" | ").append(facts).append(" |\n");
        }
        Ratios r = card.ratios();
        out.append("\n- Quality per thousand lines: ").append(ratio(r.qualityPerThousandLines())).append('\n');
        out.append("- Maintainability per point of Complexity: ").append(ratio(r.maintainabilityPerComplexity())).append('\n');
        out.append("- Provenance tier: ").append(card.provenanceTier() == null ? "not measured" : card.provenanceTier().title).append('\n');
        out.append("- Tooling bucket: ").append(escape(card.toolingBucket())).append('\n');
        out.append("- Refuted: ").append(card.refuted().isEmpty() ? "none" : card.refuted().stream().map(CardMarkdown::refutation).collect(Collectors.joining("; "))).append('\n');
        out.append("- Produced: kernel ").append(escape(card.produced().kernelVersion())).append(", ").append(escape(card.produced().date()))
                .append(", rubric ").append(code(card.produced().rubricHash())).append('\n');
        return out.toString();
    }

    private static String ratio(Double value) {
        return value == null ? "not measured" : String.valueOf(value);
    }

    private static String refutation(Refutation r) {
        return code(r.fact()) + " by " + escape(r.by());
    }

    /** A code span that a backtick, a pipe or a newline in the text cannot break out of. */
    static String code(String text) {
        return "`" + text.replace("`", "'").replace("|", "\\|").replaceAll("[\\r\\n]", " ") + "`";
    }

    /** Plain text with Markdown's active characters escaped. */
    static String escape(String text) {
        return text.replaceAll("([\\\\`*_\\[\\]<>|#!])", "\\\\$1").replaceAll("[\\r\\n]", " ");
    }
}
