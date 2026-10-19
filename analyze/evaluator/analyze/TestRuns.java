package evaluator.analyze;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A test run's facts (E-037). `QUALITY.tests.pass_rate` is passed over run (skipped left out), and
 * `QUALITY.tests.coverage` the share of lines run; both to three places. A run whose tests reached for
 * the network failed as a run: it gets `QUALITY.tests.network_attempts`, naming those tests, and no
 * pass rate, since the failures were the sandbox's, not the code's. With no test run, no fact (SC-07).
 */
// Decision: E-037
public final class TestRuns {
    private TestRuns() {}

    /** The facts of one run, citing the reports they were read from. */
    public static List<Fact> facts(Reports.Outcome outcome, Optional<Double> coverage, String tool, List<String> reports) {
        List<Fact> facts = new ArrayList<>();
        Evidence read = new Evidence(tool, reports.isEmpty() ? List.of("(no report)") : reports);
        if (!outcome.networkAttempts().isEmpty()) {
            facts.add(Fact.measured("QUALITY.tests.network_attempts", Subject.repository(), outcome.networkAttempts().size(),
                    new Evidence(tool, outcome.networkAttempts())));
        } else if (outcome.total() - outcome.skipped() > 0) {
            facts.add(Fact.measured("QUALITY.tests.pass_rate", Subject.repository(), round((double) outcome.passed() / (outcome.total() - outcome.skipped())), read));
        }
        coverage.ifPresent(c -> facts.add(Fact.measured("QUALITY.tests.coverage", Subject.repository(), round(c), read)));
        return facts;
    }

    private static double round(double share) {
        return Math.round(1000.0 * share) / 1000.0;
    }
}
