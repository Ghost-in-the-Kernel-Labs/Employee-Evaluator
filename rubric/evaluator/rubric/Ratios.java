package evaluator.rubric;

/**
 * The two efficiency ratios Size and Complexity rank through: Quality per thousand lines, and
 * Maintainability per unit of Complexity. Each is absent (null) when a metric it needs was not measured.
 */
// Invariant: SC-04
public record Ratios(Double qualityPerThousandLines, Double maintainabilityPerComplexity) {}
