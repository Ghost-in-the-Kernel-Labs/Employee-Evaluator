package evaluator.rubric;

/**
 * One thing measured or judged about a submission (docs/SCORE-CARD.md, "A fact"): its kind, what it
 * is about, its value, the evidence it was read from, its confidence and its state. Built only
 * through its constructor, which refuses a fact no person could check.
 */
// Decision: E-017
public record Fact(String kind, Subject subject, Object value, Evidence evidence, double confidence, State state) {
    // Invariant: SC-02
    public Fact {
        Metric.ofKind(kind);
        if (subject == null) throw new IllegalArgumentException(kind + ": a fact is about something");
        if (!(value instanceof Number || value instanceof String || value instanceof Boolean)) {
            throw new IllegalArgumentException(kind + ": a fact's value is a number, a text or a yes/no, not " + value);
        }
        if (evidence == null || evidence.isEmpty()) throw new IllegalArgumentException(kind + " of " + subject + " has no evidence (SC-02)");
        if (!(confidence >= 0 && confidence <= 1)) throw new IllegalArgumentException(kind + ": a confidence is 0–1, not " + confidence);
        if (state == null) throw new IllegalArgumentException(kind + ": a fact has a state");
    }

    /** A tool's measurement: confidence 1.0, inferred until a person says otherwise. */
    public static Fact measured(String kind, Subject subject, Object value, Evidence evidence) {
        return new Fact(kind, subject, value, evidence, 1.0, State.INFERRED);
    }

    /** An agent's judgment, its confidence never above its prompt's recorded precision; no calibration, no fact. */
    // Invariant: SC-05
    public static Fact judged(String kind, Subject subject, Object value, Evidence evidence, double claimed, Calibration calibration) {
        if (calibration == null) throw new IllegalArgumentException(kind + ": a prompt with no recorded precision produces no fact (SC-05)");
        if (!evidence.tool().contains(calibration.promptHash())) {
            throw new IllegalArgumentException(kind + ": the evidence names prompt " + evidence.tool() + ", calibrated was " + calibration.promptHash());
        }
        return new Fact(kind, subject, value, evidence, Math.min(claimed, calibration.precision()), State.INFERRED);
    }

    /** The metric this fact feeds. */
    public Metric metric() {
        return Metric.ofKind(kind);
    }

    /** What a fact is known by, so a rule's next version that infers it again is the same fact. */
    // Invariant: SC-06
    public String key() {
        return kind + " " + subject;
    }

    /** Whether the fact may feed a score: a refuted one never does. */
    // Invariant: SC-03
    public boolean scores() {
        return state != State.REFUTED;
    }

    /** The same fact with a person's word on it. */
    public Fact withState(State next) {
        return new Fact(kind, subject, value, evidence, confidence, next);
    }
}
