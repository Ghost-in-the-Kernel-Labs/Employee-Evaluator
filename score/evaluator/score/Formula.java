package evaluator.score;

import evaluator.rubric.Fact;
import evaluator.rubric.Score;
import java.util.List;

/**
 * How a metric's score, 0–100, is made from the card's facts, naming the facts it read; null when none it
 * reads is there. It is given every fact, not only its metric's: Maintainability reads functions' complexity.
 */
@FunctionalInterface
public interface Formula {
    Score score(List<Fact> facts);
}
