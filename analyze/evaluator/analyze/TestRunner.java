package evaluator.analyze;

import evaluator.ingest.Inventory;
import evaluator.ingest.Quarantine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
    private static final Map<String, String> OUT = Map.of("Python", "python", "TypeScript", "node", "Java", "java");

    /** The facts of the run, and each language that was not measured, with why. */
    public record Result(List<evaluator.rubric.Fact> facts, List<String> notMeasured) {}

    private TestRunner() {}

    /** Runs the tests of the repository at repo, its inventory given, in the sandbox. */
    public static Result run(Path repo, Inventory inventory, Sandbox sandbox) {
        Path work = null, out = null;
        try {
            work = Files.createTempDirectory("evaluator-work");
            out = Files.createTempDirectory("evaluator-out");
            copy(repo.toAbsolutePath().normalize(), work);
            Quarantine.strip(work);
            return runPlans(work, out, TestPlans.of(work, inventory), sandbox);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            delete(work);
            delete(out);
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
                Reports.Outcome outcome = outcome(dir, reports);
                if (outcome.total() == 0) {
                    notMeasured.add(plan.language() + ": the tests wrote no report");
                    continue;
                }
                sum = add(sum, outcome);
                Optional<Reports.Lines> lines = coverage(dir, reports);
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

    /** The JUnit reports (junit.xml, surefire/*.xml) and Jest's (jest.json) in a language's folder, summed. */
    private static Reports.Outcome outcome(Path dir, List<String> reports) throws Reports.Refused, IOException {
        List<String> junit = new ArrayList<>();
        for (Path file : files(dir, "junit.xml", "surefire")) {
            junit.add(text(file));
            reports.add(dir.getFileName() + "/" + dir.relativize(file).toString().replace('\\', '/'));
        }
        Reports.Outcome outcome = Reports.junit(junit);
        Path jest = dir.resolve("jest.json");
        if (Files.isRegularFile(jest) && !Files.isSymbolicLink(jest)) {
            reports.add(dir.getFileName() + "/jest.json");
            outcome = add(outcome, Reports.jest(text(jest)));
        }
        return outcome;
    }

    /** The first coverage report a language's folder holds: coverage.py's, c8's or JaCoCo's. */
    private static Optional<Reports.Lines> coverage(Path dir, List<String> reports) throws Reports.Refused, IOException {
        for (String name : List.of("coverage.xml", "cobertura-coverage.xml", "jacoco.xml")) {
            Path file = dir.resolve(name);
            if (Files.isRegularFile(file) && !Files.isSymbolicLink(file)) {
                reports.add(dir.getFileName() + "/" + name);
                return Reports.coverage(text(file));
            }
        }
        return Optional.empty();
    }

    /** The named file and every .xml file in the named folder, in order; never through a link. */
    private static List<Path> files(Path dir, String file, String folder) throws IOException {
        List<Path> found = new ArrayList<>();
        if (Files.isRegularFile(dir.resolve(file)) && !Files.isSymbolicLink(dir.resolve(file))) found.add(dir.resolve(file));
        Path sub = dir.resolve(folder);
        if (Files.isDirectory(sub) && !Files.isSymbolicLink(sub)) {
            try (Stream<Path> list = Files.list(sub)) {
                list.filter(p -> p.toString().endsWith(".xml") && Files.isRegularFile(p) && !Files.isSymbolicLink(p)).sorted().forEach(found::add);
            }
        }
        return found;
    }

    /** A report's text; one over the cap is refused here, before it is read. */
    private static String text(Path file) throws IOException, Reports.Refused {
        if (Files.size(file) > Reports.MAX_BYTES) throw new Reports.Refused("over " + Reports.MAX_BYTES + " bytes");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static Reports.Outcome add(Reports.Outcome a, Reports.Outcome b) {
        List<String> network = new ArrayList<>(a.networkAttempts());
        network.addAll(b.networkAttempts());
        return new Reports.Outcome(a.total() + b.total(), a.passed() + b.passed(), a.failed() + b.failed(), a.skipped() + b.skipped(), network);
    }

    // Unsure: a repository is copied whole; a size cap belongs with the fleet's limits (TODO.md 2.7).
    /** The repository's regular files and folders into work: no link followed or copied, no `.git`. */
    static void copy(Path from, Path to) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName() != null && dir.getFileName().toString().equals(".git") && !dir.equals(from)) return FileVisitResult.SKIP_SUBTREE;
                Files.createDirectories(to.resolve(from.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isRegularFile()) Files.copy(file, to.resolve(from.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** A temporary folder and everything in it; a link the run made is removed as a link, never followed. */
    static void delete(Path root) {
        if (root == null || !Files.exists(root)) return;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException failed) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
