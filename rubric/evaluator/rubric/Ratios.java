package evaluator.rubric;

/**
 * The two efficiency ratios Size and Complexity rank through (E-047): Quality per thousand lines, ranked
 * only among cards in the same size band (SC-10), and Maintainability at Complexity, high only when both
 * are. Each is absent (null) when a part it needs was not measured; the band is absent with no line count.
 */
// Invariant: SC-04
public record Ratios(Double qualityPerThousandLines, String sizeBand, Double maintainabilityAtComplexity) {}
