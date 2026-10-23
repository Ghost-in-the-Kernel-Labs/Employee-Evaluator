package evaluator.rubric;

/**
 * The seven metrics of docs/SCORE-CARD.md, each ranked on its own and scored 0–100, except a counted one:
 * a counted metric is scored against the baseline repository (E-070) and has no ceiling (E-071). A fact's kind
 * names the metric it feeds before its first dot (`SIZE.source_lines` feeds Size).
 */
// Decision: E-011
public enum Metric {
    QUALITY("Quality"),
    COMPLEXITY("Complexity"),
    SIZE("Size", true),
    AMBITION("Ambition"),
    NAVIGABILITY("AI navigability"),
    MAINTAINABILITY("Maintainability"),
    PROVENANCE("Provenance");

    /** The metric's name as the card and the documents write it. */
    public final String title;

    /**
     * Whether the score is a count against the baseline, which may pass 100 (E-071). Size is the first; Complexity and
     * Maintainability's parts join it as TODO.md 1.13 moves them. Provenance, Quality and Navigability stay 0–100.
     */
    public final boolean counted;

    Metric(String title) {
        this(title, false);
    }

    Metric(String title, boolean counted) {
        this.title = title;
        this.counted = counted;
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
