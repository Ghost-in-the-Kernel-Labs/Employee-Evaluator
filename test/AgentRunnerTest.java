import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Holds agent-runner/ (TODO.md 2.2a): `python3 -I -B -m unittest discover` runs in it, and a failing Python test, or none
 * found, fails the build. Python is outside the Java module map (docs/DESIGN.md, "Step 2.2 sliced"), so this is its gate.
 * PENDING and green when `python3` is absent, as CalibrationSet is before 30 labels; the pinned image has it (E-081).
 * The child sees no environment variable that names a key (E-013): the harness never needs one before 2.2l.
 */
// Decision: E-081
final class AgentRunnerTest {
    static final int TIMEOUT_SECONDS = 120;

    public static void main(String[] args) {
        run().exit();
    }

    /** One run of the Python tests: how it ended and what it printed. */
    record Outcome(int exit, String output, boolean timedOut) {
    }

    /** The agent-runner check: the key-scrubbing cases, then the Python tests run for real and with stubbed outcomes; pending and green when python3 is absent. */
    static Check run() {
        Check check = new Check("agent-runner")
                .accepts("an environment variable that names no key", () -> namesKey("PATH") ? List.of("PATH") : List.of())
                .rejects("an environment variable that names a key", () -> namesKey("ANTHROPIC_API_KEY") ? List.of("scrubbed") : List.of())
                .rejects("a token variable", () -> namesKey("EVALUATOR_GITHUB_TOKEN") ? List.of("scrubbed") : List.of());
        if (!python3Present()) {
            System.out.println("agent-runner PENDING: python3 is absent, so its tests did not run (the pinned image has it)");
            return check
                    .rejects("a failed run", () -> check(new Outcome(1, "FAILED", false)))
                    .accepts("a passed run", () -> check(new Outcome(0, "OK", false)));
        }
        Path root = Check.ROOT.resolve("agent-runner");
        return check
                .repository(check(discover(root)))
                .rejects("a failed Python test", () -> check(new Outcome(1, "FAILED (failures=1)", false)))
                .rejects("a run that found no test", () -> check(new Outcome(5, "NO TESTS RAN", false)))
                .rejects("a run that timed out", () -> check(new Outcome(-1, "", true)))
                .accepts("a passed run", () -> check(new Outcome(0, "OK", false)))
                .rejects("a real failing Python test", () -> check(inTemp("import unittest\nclass T(unittest.TestCase):\n    def test_x(self):\n        self.fail('no')\n")))
                .accepts("a real passing Python test", () -> check(inTemp("import unittest\nclass T(unittest.TestCase):\n    def test_x(self):\n        pass\n")));
    }

    /** What the Python run gets wrong: it timed out, or did not exit 0 (a failure, an error, or no test found). */
    static List<String> check(Outcome outcome) {
        List<String> failures = new ArrayList<>();
        if (outcome.timedOut()) failures.add("the Python tests took longer than " + TIMEOUT_SECONDS + " s");
        else if (outcome.exit() != 0) failures.add("the Python tests exited " + outcome.exit() + ":\n" + tail(outcome.output()));
        return failures;
    }

    private static String tail(String output) {
        return output.length() <= 4000 ? output : "..." + output.substring(output.length() - 4000);
    }

    static boolean python3Present() {
        try {
            Process p = new ProcessBuilder("python3", "--version").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static Outcome inTemp(String testSource) {
        try {
            Path dir = Files.createTempDirectory("agent-runner-gate");
            Files.writeString(dir.resolve("test_gate.py"), testSource);
            return discover(dir);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** Runs `python3 -I -B -m unittest discover` in `dir`: isolated (no PYTHON variables, no cwd on the path), no bytecode written. */
    static Outcome discover(Path dir) {
        try {
            ProcessBuilder builder = new ProcessBuilder("python3", "-I", "-B", "-m", "unittest", "discover")
                    .directory(dir.toFile()).redirectErrorStream(true);
            builder.environment().keySet().removeIf(AgentRunnerTest::namesKey);
            Process process = builder.start();
            // Read on a thread so a full pipe cannot hold the child past the timeout.
            var reader = new java.io.ByteArrayOutputStream();
            Thread copy = new Thread(() -> {
                try {
                    process.getInputStream().transferTo(reader);
                } catch (IOException ignored) {
                    // The process ended; what was read is enough.
                }
            });
            copy.start();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Outcome(-1, "", true);
            }
            copy.join(5000);
            return new Outcome(process.exitValue(), reader.toString(StandardCharsets.UTF_8), false);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static boolean namesKey(String variable) {
        String upper = variable.toUpperCase(java.util.Locale.ROOT);
        return upper.contains("KEY") || upper.contains("TOKEN") || upper.contains("SECRET") || upper.contains("PASSWORD");
    }
}
