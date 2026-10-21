package evaluator.score;

import evaluator.rubric.Fact;
import evaluator.rubric.Score;
import java.util.List;

/** How one metric's facts become its score, 0–100, naming the facts it read; null when none it reads is there. */
@FunctionalInterface
public interface Formula {
    Score score(List<Fact> facts);
}
