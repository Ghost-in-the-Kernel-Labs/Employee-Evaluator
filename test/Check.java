import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * What every doc test shares: reading the repository, and holding a checker to the repository and to
 * inputs that must fail (or pass) so a check that has stopped catching anything is caught itself.
 * Plain Java 21 with no dependency (E-009); `DocTests` runs each under JUnit in `gradle test`, and
 * each still runs alone through its `main`.
 */
// Decision: E-009
final class Check {
    static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private final String name;
    private final List<String> problems = new ArrayList<>();

    Check(String name) {
        this.name = name;
    }

    static String read(String relative) {
        try {
            return Files.readString(ROOT.resolve(relative));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static boolean exists(String relative) {
        return Files.exists(ROOT.resolve(relative));
    }

    /** The repository itself must give no failures. */
    Check repository(List<String> failures) {
        failures.forEach(f -> problems.add("repository: " + f));
        return this;
    }

    /** A deliberately broken input must give at least one failure. */
    Check rejects(String what, Supplier<List<String>> checker) {
        if (checker.get().isEmpty()) problems.add("does not reject " + what);
        return this;
    }

    /** A deliberately sound input must give none. */
    Check accepts(String what, Supplier<List<String>> checker) {
        checker.get().forEach(f -> problems.add("wrongly rejects " + what + ": " + f));
        return this;
    }

    List<String> problems() {
        return List.copyOf(problems);
    }

    void exit() {
        problems.forEach(p -> System.out.println(name + ": " + p));
        System.out.println(name + (problems.isEmpty() ? ": ok" : ": " + problems.size() + " failure(s)"));
        if (!problems.isEmpty()) System.exit(1);
    }
}
