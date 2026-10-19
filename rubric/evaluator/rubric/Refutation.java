package evaluator.rubric;

/** A fact a person refuted, by its key, and who refuted it. */
public record Refutation(String fact, String by) {}
