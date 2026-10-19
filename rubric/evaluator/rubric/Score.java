package evaluator.rubric;

import java.util.List;

/** One metric's score, 0–100, with the facts behind it by key: a score nobody can trace to a fact is refused. */
// Decision: E-017
public record Score(int value, List<String> facts) {
    public Score {
        if (value < 0 || value > 100) throw new IllegalArgumentException("a score is 0–100, not " + value);
        facts = facts == null ? List.of() : List.copyOf(facts);
        if (facts.isEmpty()) throw new IllegalArgumentException("a score with no fact behind it is refused");
    }
}
