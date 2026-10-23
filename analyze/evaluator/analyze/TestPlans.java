package evaluator.analyze;

import evaluator.rubric.Json;
import evaluator.ingest.Inventory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * How each language's tests run in the sandbox (E-037): what is installed, from which lockfile, and
 * the script that runs the tests under coverage, its reports written to `/out/<language>/`. A
 * language whose packages cannot be installed exactly as committed is not measured, and says why.
 * Every script is ours: nothing from the repository reaches a shell but file names that are plain.
 */
// Decision: E-037
public final class TestPlans {
    /** A version a pom may not use: a range, a snapshot, or the newest of anything (E-038). */
    private static final Pattern LOOSE_VERSION = Pattern.compile("<version>\\s*[\\[(]|SNAPSHOT|<version>\\s*(LATEST|RELEASE)\\s*</version>|version>\\s*\\$\\{[^}]*\\}\\s*[\\[(]");
    private static final Pattern PLAIN = Pattern.compile("[A-Za-z0-9._-]+");
    private static final int MAX_MANIFEST = 1024 * 1024;

    /** One language's run: its install script (null when nothing needs installing) and its test script; or why it is not measured. */
    public record Plan(String language, String install, String test, String notMeasured) {
        static Plan not(String language, String why) {
            return new Plan(language, null, null, why);
        }
    }

    private TestPlans() {}

    /** A plan for each language that has tests, read from the working copy at work. */
    public static List<Plan> of(Path work, Inventory inventory) {
        List<Plan> plans = new ArrayList<>();
        if (hasTests(inventory, "Python")) plans.add(python(work));
        if (hasTests(inventory, "TypeScript") || hasTests(inventory, "JavaScript")) plans.add(node(work));
        if (hasTests(inventory, "Java")) plans.add(java(work));
        return plans;
    }

    private static boolean hasTests(Inventory inventory, String language) {
        return inventory.source().stream().anyMatch(f -> f.language().equals(language) && TestFiles.isTest(f.path()));
    }

    /**
     * pytest under coverage.py, the tests themselves left out of coverage as c8 leaves them out (E-034's
     * names); packages from requirements files whose every line is pinned by hash, as binaries only.
     */
    static Plan python(Path work) {
        List<String> locked = new ArrayList<>();
        for (String name : rootFiles(work)) {
            if (!name.matches("requirements.*\\.txt")) continue;
            if (!PLAIN.matcher(name).matches()) return Plan.not("Python", "a requirements file with a name no shell may see: " + name);
            if (!hashPinned(read(work.resolve(name)))) return Plan.not("Python", name + " is not pinned by hash (E-037)");
            locked.add(name);
        }
        if (locked.isEmpty() && declaresPythonPackages(work)) return Plan.not("Python", "packages declared, but no requirements file pinned by hash (E-037)");
        String install = locked.isEmpty() ? null : String.join(" && ", locked.stream().map(name ->
                "pip install --no-input --disable-pip-version-check --no-deps --require-hashes --only-binary=:all: --target /work/.evaluator-deps -r " + name).toList());
        String test = "mkdir -p /out/python && PYTHONPATH=/work/.evaluator-deps:/work/src:/work python -m coverage run --data-file=/tmp/.coverage"
                + " --source=/work --omit='/work/.evaluator-deps/*,*/test/*,*/tests/*,*/test_*.py,*_test.py,*/conftest.py' -m pytest -p no:cacheprovider -q --junitxml=/out/python/junit.xml;"
                + " python -m coverage xml --data-file=/tmp/.coverage -o /out/python/coverage.xml";
        return new Plan("Python", install, test, null);
    }

    /** The repository's own runner under c8, after `npm ci` with scripts off; any lockfile but npm's is not measured. */
    @SuppressWarnings("unchecked")
    static Plan node(Path work) {
        Path manifest = work.resolve("package.json");
        if (!Files.isRegularFile(manifest)) return Plan.not("Node", "no package.json at the root");
        Map<String, Object> pkg;
        try {
            pkg = (Map<String, Object>) Json.parse(read(manifest));
        } catch (IllegalArgumentException | ClassCastException e) {
            return Plan.not("Node", "package.json does not parse");
        }
        boolean packages = !map(pkg.get("dependencies")).isEmpty() || !map(pkg.get("devDependencies")).isEmpty();
        boolean npmLock = Files.isRegularFile(work.resolve("package-lock.json")) || Files.isRegularFile(work.resolve("npm-shrinkwrap.json"));
        if (packages && !npmLock) return Plan.not("Node", "packages declared, but no package-lock.json for npm ci (E-037)");
        String runner = runner(pkg);
        if (runner == null) return Plan.not("Node", "no test runner the sandbox reads (vitest, jest, mocha, node --test)");
        String c8 = "mkdir -p /out/node && c8 --reporter=cobertura --reports-dir=/out/node --temp-directory=/tmp/c8 ";
        String test = c8 + switch (runner) {
            case "vitest" -> "npx --no-install vitest run --reporter=junit --outputFile=/out/node/junit.xml";
            case "jest" -> "npx --no-install jest --ci --json --outputFile=/out/node/jest.json";
            case "mocha" -> "npx --no-install mocha --reporter xunit --reporter-option output=/out/node/junit.xml";
            default -> "node --test --test-reporter=junit --test-reporter-destination=/out/node/junit.xml";
        };
        return new Plan("Node", packages ? "npm ci --ignore-scripts --no-audit --no-fund" : null, test, null);
    }

    /**
     * Maven with JaCoCo's agent, its Surefire reports copied out: Maven has no lockfile, so its pom's
     * versions must be exact and every download's checksum must match (`-C`, E-038). A Gradle build is
     * not measured yet.
     */
    // Decision: E-038
    static Plan java(Path work) {
        if (Files.isRegularFile(work.resolve("pom.xml"))) {
            if (!exactVersions(read(work.resolve("pom.xml")))) return Plan.not("Java", "pom.xml uses a version range, a SNAPSHOT, LATEST or RELEASE (E-038)");
            String repo = "-C -Dmaven.repo.local=/work/.evaluator-m2";
            String test = "mkdir -p /out/java/surefire && mvn -B -o -q " + repo + " test -Dmaven.test.failure.ignore=true"
                    + " -DargLine=-javaagent:/opt/evaluator/jacoco/jacocoagent.jar=destfile=/out/java/jacoco.exec;"
                    + " cp target/surefire-reports/TEST-*.xml /out/java/surefire/ 2>/dev/null;"
                    + " java -jar /opt/evaluator/jacoco/jacococli.jar report /out/java/jacoco.exec --classfiles target/classes --xml /out/java/jacoco.xml";
            return new Plan("Java", "mvn -B -q " + repo + " dependency:go-offline", test, null);
        }
        if (Files.isRegularFile(work.resolve("build.gradle")) || Files.isRegularFile(work.resolve("build.gradle.kts"))) {
            return Plan.not("Java", "Gradle builds are not run yet (unplanned: docs/HANDOFF.md)");
        }
        return Plan.not("Java", "no build file the sandbox runs (pom.xml)");
    }

    /** Whether every version the pom names is exact, so what installs is what it says. */
    // Unsure: a parent pom or an imported BOM outside the repository is not read; their versions are Maven Central's to hold.
    static boolean exactVersions(String pom) {
        return !LOOSE_VERSION.matcher(pom).find();
    }

    /** The runner named in the package's dependencies, or `node --test` in its test script; null when none. */
    private static String runner(Map<String, Object> pkg) {
        for (String runner : List.of("vitest", "jest", "mocha")) {
            if (map(pkg.get("devDependencies")).containsKey(runner) || map(pkg.get("dependencies")).containsKey(runner)) return runner;
        }
        Object test = map(pkg.get("scripts")).get("test");
        return test instanceof String script && script.contains("node --test") ? "node" : null;
    }

    /** Whether every requirement in the file carries a hash (continuation lines joined; options and comments aside). */
    static boolean hashPinned(String requirements) {
        boolean any = false;
        for (String line : requirements.replace("\\\n", " ").lines().toList()) {
            String bare = line.strip();
            if (bare.isEmpty() || bare.startsWith("#") || bare.startsWith("-")) continue;
            if (!bare.contains("--hash=")) return false;
            any = true;
        }
        return any;
    }

    // Unsure: pyproject.toml's dependencies are found by a pattern, not a TOML parser; an unusual layout reads as none.
    private static boolean declaresPythonPackages(Path work) {
        Path pyproject = work.resolve("pyproject.toml");
        boolean pep621 = Files.isRegularFile(pyproject) && Pattern.compile("(?m)^dependencies\\s*=\\s*\\[\\s*[\"']").matcher(read(pyproject)).find();
        boolean poetry = Files.isRegularFile(pyproject) && Pattern.compile("(?ms)^\\[tool\\.poetry\\.dependencies\\]\\s*\\n(?!\\s*python\\s*=[^\\n]*\\n\\s*(\\[|\\z))").matcher(read(pyproject)).find();
        return pep621 || poetry || Files.isRegularFile(work.resolve("setup.py")) || Files.isRegularFile(work.resolve("Pipfile"));
    }

    private static List<String> rootFiles(Path work) {
        try (var list = Files.list(work)) {
            return list.filter(p -> Files.isRegularFile(p) && !Files.isSymbolicLink(p)).map(p -> p.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            if (Files.size(file) > MAX_MANIFEST) return "";
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> m ? m : Map.of();
    }
}
