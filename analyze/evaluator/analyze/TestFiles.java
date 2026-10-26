package evaluator.analyze;

import evaluator.ingest.Inventory;
import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Which counted source files are tests, by where they live and what they are called, and the test-to-
 * code ratio that makes of them. Nothing is run: a file is a test by its path alone, as every test
 * runner's default discovery decides it.
 */
// Decision: E-034
public final class TestFiles {
    /** The tool every fact here names as its evidence. */
    public static final String TOOL = "evaluator analyze tests 1";
    private static final Set<String> FOLDERS = Set.of("test", "tests", "__tests__", "spec", "specs", "testing");
    private static final Pattern NAMES = Pattern.compile(
            "test_.*\\.py|.*_test\\.py|conftest\\.py|.*_test\\.go|.*\\.(test|spec)\\.(ts|tsx|js|jsx|mjs|cjs)|.*(Test|Tests|IT|Spec)\\.(java|kt|scala|groovy)");

    private TestFiles() {}

    /** Whether a file is a test: a folder on its path is a test folder, or its name is a test runner's. */
    public static boolean isTest(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) if (FOLDERS.contains(parts[i])) return true;
        return NAMES.matcher(parts[parts.length - 1]).matches();
    }

    /**
     * Test lines per line of the code they test, to three places, citing the test files; none when the
     * repository has no code that is not a test, since a ratio over nothing measures nothing (SC-07).
     */
    public static Optional<Fact> ratio(Inventory inventory) {
        return ratio(inventory, TestFiles::isTest);
    }

    /** The same ratio where `test` says which files are tests: a judge's classes (TODO.md 2.2j) in place of the kernel's path rule. */
    public static Optional<Fact> ratio(Inventory inventory, Predicate<String> test) {
        int tested = 0, code = 0;
        List<String> tests = new ArrayList<>();
        for (Inventory.Source file : inventory.source()) {
            if (test.test(file.path())) {
                tested += file.lines();
                tests.add(file.path());
            } else {
                code += file.lines();
            }
        }
        if (code == 0) return Optional.empty();
        double ratio = Math.round(1000.0 * tested / code) / 1000.0;
        Evidence evidence = new Evidence(TOOL, tests.isEmpty() ? List.of("(no test file found)") : tests);
        return Optional.of(Fact.measured("MAINTAINABILITY.test_to_code_ratio", Subject.repository(), ratio, evidence));
    }
}
