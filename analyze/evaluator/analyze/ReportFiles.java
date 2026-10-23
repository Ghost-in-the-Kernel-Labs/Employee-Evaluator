package evaluator.analyze;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The report files a submission's tests wrote, found and read as hostile files (E-037): never through
 * a link, each one under the size cap before it is read. Split from TestRunner, which keeps the
 * orchestration.
 */
final class ReportFiles {
    private ReportFiles() {}

    /** The JUnit reports (junit.xml, surefire/*.xml) and Jest's (jest.json) in a language's folder, summed. */
    static Reports.Outcome outcome(Path dir, List<String> reports) throws Reports.Refused, IOException {
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
    static Optional<Reports.Lines> coverage(Path dir, List<String> reports) throws Reports.Refused, IOException {
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
    static List<Path> files(Path dir, String file, String folder) throws IOException {
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
    static String text(Path file) throws IOException, Reports.Refused {
        if (Files.size(file) > Reports.MAX_BYTES) throw new Reports.Refused("over " + Reports.MAX_BYTES + " bytes");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    static Reports.Outcome add(Reports.Outcome a, Reports.Outcome b) {
        List<String> network = new ArrayList<>(a.networkAttempts());
        network.addAll(b.networkAttempts());
        return new Reports.Outcome(a.total() + b.total(), a.passed() + b.passed(), a.failed() + b.failed(), a.skipped() + b.skipped(), network);
    }
}
