package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.ingest.Ingest;
import evaluator.rubric.Fact;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The runner against a fake sandbox that writes the reports a real run would: copy, strip, install, run, read. */
class TestRunnerTest {
    private static final Path REPORTS = Path.of(System.getProperty("evaluator.root", ".")).resolve("test/fixtures/reports");

    /** Stands in for Docker: records each phase and script, and on TEST does what it is told to the copy. */
    static final class Fake implements Sandbox {
        final List<String> calls = new ArrayList<>();
        final List<Path> works = new ArrayList<>();
        BiConsumer<Path, Path> onTest = (work, out) -> {};
        int installExit = 0;
        boolean testTimesOut = false;

        @Override
        public Run run(Phase phase, Path work, Path out, String script, Duration timeout) {
            calls.add(phase + ": " + script);
            works.add(work);
            if (phase == Phase.INSTALL) return new Run(installExit, false, "");
            onTest.accept(work, out);
            return new Run(1, testTimesOut, "");
        }
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private static void report(Path out, String lang, String name, String fixture) {
        try {
            Files.createDirectories(out.resolve(lang));
            Files.copy(REPORTS.resolve(fixture), out.resolve(lang).resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path pythonRepo(Path repo) throws IOException {
        write(repo.resolve("tests/test_app.py"), "def test_a():\n    assert True\n");
        write(repo.resolve(".claude/settings.json"), "{\"hooks\": {}}");
        return repo;
    }

    /** pytest's report and coverage.py's, as a real run writes them: 0.75 each, cited; the checkout untouched, the copy gone. */
    @Test
    void aRunOnAStrippedCopyBecomesFacts(@TempDir Path repo) throws IOException {
        Fake fake = new Fake();
        fake.onTest = (work, out) -> {
            assertFalse(Files.exists(work.resolve(".claude")), "the copy is stripped before anything runs");
            report(out, "python", "junit.xml", "pytest.xml");
            report(out, "python", "coverage.xml", "cobertura.xml");
        };
        TestRunner.Result result = TestRunner.run(pythonRepo(repo), Ingest.read(repo), fake);
        assertEquals(List.of("QUALITY.tests.pass_rate repository:.", "QUALITY.tests.coverage repository:."), result.facts().stream().map(Fact::key).toList());
        assertEquals(List.of(0.75, 0.75), result.facts().stream().map(Fact::value).toList());
        assertEquals(List.of("python/junit.xml", "python/coverage.xml"), result.facts().get(0).evidence().sources());
        assertTrue(Files.exists(repo.resolve(".claude/settings.json")), "the submission's checkout is never touched");
        assertFalse(Files.exists(fake.works.get(0)), "the copy is removed afterwards");
        assertEquals(List.of(), result.notMeasured());
    }

    @Test
    void testsThatReachForTheNetworkFailTheRunAndSaySo(@TempDir Path repo) throws IOException {
        Fake fake = new Fake();
        fake.onTest = (work, out) -> report(out, "python", "junit.xml", "surefire.xml");
        List<Fact> facts = TestRunner.run(pythonRepo(repo), Ingest.read(repo), fake).facts();
        assertEquals(List.of("QUALITY.tests.network_attempts repository:."), facts.stream().map(Fact::key).toList());
        assertEquals(List.of("shop.CartTest.fetchesExchangeRates"), facts.get(0).evidence().sources());
    }

    @Test
    void aFailedInstallATimeoutOrNoReportIsNotMeasured(@TempDir Path repo) throws IOException {
        pythonRepo(repo);
        write(repo.resolve("requirements.txt"), "requests==2.32.3 --hash=sha256:" + "a".repeat(64) + "\n");
        Fake failing = new Fake();
        failing.installExit = 1;
        assertEquals(List.of("Python: the install failed (exit 1)"), TestRunner.run(repo, Ingest.read(repo), failing).notMeasured());
        assertEquals(1, failing.calls.size(), "no test runs after a failed install");
        Fake slow = new Fake();
        slow.testTimesOut = true;
        assertEquals(List.of("Python: the tests ran past 10 minutes"), TestRunner.run(repo, Ingest.read(repo), slow).notMeasured());
        assertEquals(List.of("Python: the tests wrote no report"), TestRunner.run(repo, Ingest.read(repo), new Fake()).notMeasured());
    }

    @Test
    void aHostileReportIsRefusedAndALinkIsNotFollowed(@TempDir Path repo, @TempDir Path outside) throws IOException {
        Fake fake = new Fake();
        fake.onTest = (work, out) -> report(out, "python", "junit.xml", "xxe.xml");
        assertTrue(TestRunner.run(pythonRepo(repo), Ingest.read(repo), fake).notMeasured().get(0).startsWith("Python: a report was refused"));
        write(outside.resolve("secret.xml"), "<testsuite><testcase name=\"forged\"/></testsuite>");
        Fake linking = new Fake();
        linking.onTest = (work, out) -> {
            try {
                Files.createDirectories(out.resolve("python"));
                Files.createSymbolicLink(out.resolve("python/junit.xml"), outside.resolve("secret.xml"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        assertEquals(List.of("Python: the tests wrote no report"), TestRunner.run(repo, Ingest.read(repo), linking).notMeasured());
        assertTrue(Files.exists(outside.resolve("secret.xml")), "removing the run's link leaves its target alone");
    }

    @Test
    void theCopyHasNoLinkAndNoGit(@TempDir Path repo, @TempDir Path work, @TempDir Path outside) throws IOException {
        write(repo.resolve("a.py"), "x = 1\n");
        write(repo.resolve(".git/config"), "[core]\n");
        Files.createSymbolicLink(repo.resolve("link.py"), outside);
        TestRunner.copy(repo, work);
        assertTrue(Files.exists(work.resolve("a.py")));
        assertFalse(Files.exists(work.resolve(".git")));
        assertFalse(Files.exists(work.resolve("link.py")));
    }
}
