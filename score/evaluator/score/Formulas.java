package evaluator.score;

import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.Score;
import java.util.List;
import java.util.Map;

/**
 * The formulas the kernel scores with, by metric (E-045). A metric with no formula here is not measured
 * (SC-07), never 0: each one's formula is a decision of its own, made with Ghost, before it scores.
 */
// Decision: E-045
public final class Formulas {
    private Formulas() {}

    /** Phase 1's formulas: AI navigability is its checklist (E-044), already 0–100. */
    public static Map<Metric, Formula> phaseOne() {
        return Map.of(Metric.NAVIGABILITY, Formulas.repositoryFact("NAVIGABILITY.checklist"));
    }

    /** A formula that is one repository-wide fact already on 0–100, taken as it is. */
    static Formula repositoryFact(String kind) {
        return facts -> {
            for (Fact f : facts) {
                if (f.kind().equals(kind) && f.subject().kind() == evaluator.rubric.Subject.Kind.REPOSITORY) {
                    return new Score(((Number) f.value()).intValue(), List.of(f.key()));
                }
            }
            return null;
        };
    }
}
