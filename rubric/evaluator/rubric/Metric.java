package evaluator.rubric;

/**
 * The seven metrics of docs/SCORE-CARD.md, each scored 0–100 and ranked on its own. A fact's kind
 * names the metric it feeds before its first dot (`SIZE.source_lines` feeds Size).
 */
// Decision: E-011
public enum Metric {
    QUALITY("Quality"),
    COMPLEXITY("Complexity"),
    SIZE("Size"),
    AMBITION("Ambition"),
    NAVIGABILITY("AI navigability"),
    MAINTAINABILITY("Maintainability"),
    PROVENANCE("Provenance");

    /** The metric's name as the card and the documents write it. */
    public final String title;

    Metric(String title) {
        this.title = title;
    }

    /** The metric a fact's kind feeds; a kind that names no metric is refused. */
    public static Metric ofKind(String kind) {
        int dot = kind.indexOf('.');
        if (dot <= 0 || dot == kind.length() - 1) throw new IllegalArgumentException("a fact's kind is METRIC.what, not " + kind);
        try {
            return valueOf(kind.substring(0, dot));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("a fact's kind names no metric: " + kind, e);
        }
    }
}
