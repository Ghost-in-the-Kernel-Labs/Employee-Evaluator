package evaluator.analyze;

import evaluator.ingest.Inventory;
import evaluator.ingest.Quarantine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A submission's own tests, run (E-037): the repository copied (no link, no `.git`) and stripped
 * (`Quarantine.strip`), each language's plan installed then run in the sandbox, its reports read as
 * hostile files, and the outcomes summed into the `QUALITY.tests` facts. The submission's checkout is
 * never touched, and both temporary folders are removed whatever happens.
 */
// Decision: E-037
public final class TestRunner {
    /** The tool every fact from a run names as its evidence. */
    public static final String TOOL = "evaluator analyze tests-run 1 (sandbox " + DockerSandbox.IMAGE + ")";
    static final Duration INSTALL_LIMIT = Duration.ofMinutes(10);
    static final Duration TEST_LIMIT = Duration.ofMinutes(10);
    private static final Map<String, String> OUT = Map.of("Python", "python", "Node", "node", "Java", "java");

    /** The facts of the run, and each language that was not measured, with why. */
    public record Result(List<evaluator.rubric.Fact> facts, List<String> notMeasured) {}

    private TestRunner() {}

    /** Runs the tests of the repository at repo, its inventory given, in the sandbox. */
    public static Result run(Path repo, Inventory inventory, Sandbox sandbox) {
        Path work = null, out = null;
        try {
            work = Files.createTempDirectory("evaluator-work");
            out = Files.createTempDirectory("evaluator-out");
            Workspace.copy(repo.toAbsolutePath().normalize(), work);
            Quarantine.strip(work);
            return runPlans(work, out, TestPlans.of(work, inventory), sandbox);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Workspace.delete(work);
            Workspace.delete(out);
        }
    }

    /** Each plan installed and run; the outcomes summed, and what kept a language unmeasured said. */
    static Result runPlans(Path work, Path out, List<TestPlans.Plan> plans, Sandbox sandbox) throws IOException {
        Reports.Outcome sum = new Reports.Outcome(0, 0, 0, 0, List.of());
        long covered = 0, valid = 0;
        List<String> reports = new ArrayList<>(), notMeasured = new ArrayList<>();
        for (TestPlans.Plan plan : plans) {
            String why = plan.notMeasured() != null ? plan.notMeasured() : runOne(plan, work, out, sandbox);
            if (why != null) {
                notMeasured.add(plan.language() + ": " + why);
                continue;
            }
            try {
                Path dir = out.resolve(OUT.get(plan.language()));
                Reports.Outcome outcome = ReportFiles.outcome(dir, reports);
                if (outcome.total() == 0) {
                    notMeasured.add(plan.language() + ": the tests wrote no report");
                    continue;
                }
                sum = ReportFiles.add(sum, outcome);
                Optional<Reports.Lines> lines = ReportFiles.coverage(dir, reports);
                if (lines.isPresent()) {
                    covered += lines.get().covered();
                    valid += lines.get().valid();
                }
            } catch (Reports.Refused e) {
                notMeasured.add(plan.language() + ": a report was refused (" + e.getMessage() + ")");
            }
        }
        Optional<Reports.Lines> coverage = valid > 0 ? Optional.of(new Reports.Lines(covered, valid)) : Optional.empty();
        return new Result(TestRuns.facts(sum, coverage, TOOL, reports), notMeasured);
    }

    /** One plan's install, then its tests; why the run cannot be measured, or null when it can. */
    private static String runOne(TestPlans.Plan plan, Path work, Path out, Sandbox sandbox) throws IOException {
        Files.createDirectories(out.resolve(OUT.get(plan.language())));
        if (plan.install() != null) {
            Sandbox.Run install = sandbox.run(Sandbox.Phase.INSTALL, work, out, plan.install(), INSTALL_LIMIT);
            if (install.timedOut()) return "the install ran past " + INSTALL_LIMIT.toMinutes() + " minutes";
            if (install.exit() != 0) return "the install failed (exit " + install.exit() + ")";
        }
        Sandbox.Run test = sandbox.run(Sandbox.Phase.TEST, work, out, plan.test(), TEST_LIMIT);
        return test.timedOut() ? "the tests ran past " + TEST_LIMIT.toMinutes() + " minutes" : null;
    }
}
