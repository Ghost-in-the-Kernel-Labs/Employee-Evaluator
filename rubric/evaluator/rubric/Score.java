package evaluator.rubric;

import java.util.List;

/**
 * One metric's score with the facts behind it by key: a score nobody can trace to a fact is refused, nor is one below 0.
 * The ceiling of 100 belongs to the metric, not the number: a card refuses a score above 100 unless the metric is counted (E-071).
 */
// Decision: E-017
public record Score(int value, List<String> facts) {
    public Score {
        if (value < 0) throw new IllegalArgumentException("a score is 0 or more, not " + value);
        facts = facts == null ? List.of() : List.copyOf(facts);
        if (facts.isEmpty()) throw new IllegalArgumentException("a score with no fact behind it is refused");
    }
}
