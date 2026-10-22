package evaluator.analyze;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * A sandbox that runs nothing: it writes the reports a real run wrote (test/goldens/test-runs/) where that
 * run wrote them, so a run's facts and a card's Quality are tested without Docker.
 */
public final class RecordedSandbox implements Sandbox {
    private final Path reports;

    public RecordedSandbox(Path reports) {
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
