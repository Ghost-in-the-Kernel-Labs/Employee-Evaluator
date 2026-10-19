package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import evaluator.ingest.Ingest;
import evaluator.rubric.Fact;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The run fixtures' facts, worked by hand from what each fixture's tests do: in the fast loop from the
 * reports a real run wrote (test/goldens/test-runs/), and with `-Pdocker` from a run in the real sandbox,
 * which must give the same facts.
 */
class TestRunnerFixturesTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    /**
     * py-pass: 3 of 3, coverage 6 of 6 lines of app/; py-fail: 2 of 3, `return 0` never run, 5 of 6;
     * py-network: a test reached for `.invalid`, no code but tests so no coverage; node-pass: 2 of 2,
     * `twice`'s body never run, 4 of 5.
     */
    private static final Map<String, Map<String, Object>> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("py-pass", Map.of("QUALITY.tests.pass_rate", 1.0, "QUALITY.tests.coverage", 1.0));
        EXPECTED.put("py-fail", Map.of("QUALITY.tests.pass_rate", 0.667, "QUALITY.tests.coverage", 0.833));
        EXPECTED.put("py-network", Map.of("QUALITY.tests.network_attempts", 1));
        EXPECTED.put("node-pass", Map.of("QUALITY.tests.pass_rate", 1.0, "QUALITY.tests.coverage", 0.8));
    }

    /** Writes the recorded reports where the real run wrote them, as the sandbox would. */
    static final class Recorded implements Sandbox {
        private final Path reports;

        Recorded(Path reports) {
            this.reports = reports;
        }

        @Override
        public Run run(Phase phase, Path work, Path out, String script, Duration timeout) {
            try (Stream<Path> files = Files.walk(reports)) {
                for (Path f : files.filter(Files::isRegularFile).toList()) {
                    Path target = out.resolve(reports.relativize(f).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(f, target);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new Run(0, false, "");
        }
    }

    private static Map<String, Object> facts(String fixture, Sandbox sandbox) {
        Path repo = ROOT.resolve("test/fixtures/test-runs/" + fixture);
        TestRunner.Result result = TestRunner.run(repo, Ingest.read(repo), sandbox);
        assertEquals(List.of(), result.notMeasured(), fixture);
        Map<String, Object> facts = new LinkedHashMap<>();
        for (Fact f : result.facts()) facts.put(f.kind(), f.value());
        return facts;
    }

    @Test
    void theRecordedRunsGiveTheFactsWorkedByHand() {
        EXPECTED.forEach((fixture, expected) -> assertEquals(expected, facts(fixture, new Recorded(ROOT.resolve("test/goldens/test-runs/" + fixture))), fixture));
    }

    @Test
    void theNetworkAttemptIsNamed() {
        Path repo = ROOT.resolve("test/fixtures/test-runs/py-network");
        Fact fact = TestRunner.run(repo, Ingest.read(repo), new Recorded(ROOT.resolve("test/goldens/test-runs/py-network"))).facts().get(0);
        assertEquals(List.of("tests.test_rates.test_fetches_rates"), fact.evidence().sources());
    }

    /** On a machine with Docker and sandbox/setup.sh run: the real sandbox gives the same facts. */
    @Test
    void theRealSandboxGivesTheSameFacts() {
        assumeTrue(Boolean.getBoolean("evaluator.docker"), "the real sandbox runs with gradle test -Pdocker");
        EXPECTED.forEach((fixture, expected) -> assertEquals(expected, facts(fixture, new DockerSandbox()), fixture));
    }
}
