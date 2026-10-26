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
    /** How many of a score's facts the table names before it counts the rest. */
    static final int SHOWN = 3;

    private CardMarkdown() {}

    /** The card's Markdown rendering. */
    public static String render(ScoreCard card) {
        return render(card, null);
    }

    /** The card's Markdown rendering, with a line naming the judgment's prompt and models (TODO.md 2.2j) when there is one. */
    public static String render(ScoreCard card, Judged judged) {
        StringBuilder out = new StringBuilder("# Score card\n\n| Metric | Score | Facts behind it |\n| --- | --- | --- |\n");
        for (Metric metric : Metric.values()) {
            Score s = card.scores().get(metric);
            String facts = s == null ? "" : facts(s);
            out.append("| ").append(metric.title).append(" | ").append(s == null ? "not measured" : String.valueOf(s.value()))
                    .append(" | ").append(facts).append(" |\n");
        }
        Ratios r = card.ratios();
        out.append("\n- Quality per thousand lines: ").append(ratio(r.qualityPerThousandLines()))
                .append(r.qualityPerThousandLines() == null ? "" : " (ranked among " + r.sizeBand() + ")").append('\n');
        out.append("- Maintainability at Complexity: ").append(ratio(r.maintainabilityAtComplexity())).append('\n');
        out.append("- Provenance tier: ").append(card.provenanceTier() == null ? "not measured" : card.provenanceTier().title).append('\n');
        out.append("- Tooling bucket: ").append(escape(card.toolingBucket())).append('\n');
        out.append("- Refuted: ").append(card.refuted().isEmpty() ? "none" : card.refuted().stream().map(CardMarkdown::refutation).collect(Collectors.joining("; "))).append('\n');
        out.append("- Produced: kernel ").append(escape(card.produced().kernelVersion())).append(", ").append(escape(card.produced().date()))
                .append(", rubric ").append(code(card.produced().rubricHash())).append('\n');
        if (judged != null) {
            out.append("- Judged: prompt ").append(code(judged.promptHash())).append(", models ")
                    .append(judged.models().stream().map(CardMarkdown::code).collect(Collectors.joining(", "))).append('\n');
        }
        return out.toString();
    }

    /** The facts behind a score, by key; past the first few, how many more the JSON lists. */
    static String facts(Score s) {
        String shown = s.facts().stream().limit(SHOWN).map(CardMarkdown::code).collect(Collectors.joining(", "));
        int more = s.facts().size() - SHOWN;
        return more > 0 ? shown + " and " + more + " more (the JSON lists all)" : shown;
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
