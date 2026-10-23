package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.ingest.Ingest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Each language's plan, on repositories built here: what installs, from what, and what is not measured. */
class TestPlansTest {
    private static final String HASHED = "requests==2.32.3 \\\n    --hash=sha256:70761cfe03c773ceb22aa2f671b4757976145175cdfca038c02654d061d6dcc6\n";

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private static List<TestPlans.Plan> plans(Path repo) {
        return TestPlans.of(repo, Ingest.read(repo));
    }

    @Test
    void pythonInstallsAHashPinnedLockAsBinariesOnly(@TempDir Path repo) throws IOException {
        write(repo.resolve("tests/test_app.py"), "def test_a():\n    assert True\n");
        write(repo.resolve("requirements.txt"), "# pinned\n" + HASHED);
        TestPlans.Plan plan = plans(repo).get(0);
        assertEquals("pip install --no-input --disable-pip-version-check --no-deps --require-hashes --only-binary=:all: --target /work/.evaluator-deps -r requirements.txt", plan.install());
        assertTrue(plan.test().contains("--junitxml=/out/python/junit.xml"));
        assertNull(plan.notMeasured());
    }

    @Test
    void pythonWithNoPackagesInstallsNothing(@TempDir Path repo) throws IOException {
        write(repo.resolve("tests/test_app.py"), "def test_a():\n    assert True\n");
        write(repo.resolve("pyproject.toml"), "[tool.pytest.ini_options]\nminversion = \"8.0\"\n");
        assertNull(plans(repo).get(0).install());
        assertNull(plans(repo).get(0).notMeasured());
    }

    @Test
    void pythonUnpinnedIsNotMeasured(@TempDir Path repo) throws IOException {
        write(repo.resolve("tests/test_app.py"), "def test_a():\n    assert True\n");
        write(repo.resolve("requirements.txt"), "requests==2.32.3\n");
        assertEquals("requirements.txt is not pinned by hash (E-037)", plans(repo).get(0).notMeasured());
        Files.delete(repo.resolve("requirements.txt"));
        write(repo.resolve("pyproject.toml"), "[project]\nname = \"a\"\ndependencies = [\"requests>=2\"]\n");
        assertEquals("packages declared, but no requirements file pinned by hash (E-037)", plans(repo).get(0).notMeasured());
    }

    @Test
    void aFileNameNoShellMaySeeNeverReachesOne(@TempDir Path repo) throws IOException {
        write(repo.resolve("tests/test_app.py"), "def test_a():\n    assert True\n");
        write(repo.resolve("requirements$(touch pwned).txt"), HASHED);
        TestPlans.Plan plan = plans(repo).get(0);
        assertNull(plan.install());
        assertTrue(plan.notMeasured().startsWith("a requirements file with a name no shell may see"));
    }

    @Test
    void nodeRunsTheRepositorysOwnRunnerAfterNpmCi(@TempDir Path repo) throws IOException {
        write(repo.resolve("src/sum.test.ts"), "test('a', () => {});\n");
        write(repo.resolve("package.json"), "{\"devDependencies\": {\"jest\": \"29.7.0\"}, \"scripts\": {\"test\": \"jest\"}}");
        write(repo.resolve("package-lock.json"), "{}");
        TestPlans.Plan plan = plans(repo).get(0);
        assertEquals("npm ci --ignore-scripts --no-audit --no-fund", plan.install());
        assertTrue(plan.test().endsWith("npx --no-install jest --ci --json --outputFile=/out/node/jest.json"));
        assertTrue(plan.test().contains("c8 --reporter=cobertura --reports-dir=/out/node"));
    }

    @Test
    void nodeWithAnotherLockfileOrNoRunnerIsNotMeasured(@TempDir Path repo) throws IOException {
        write(repo.resolve("src/sum.test.ts"), "test('a', () => {});\n");
        write(repo.resolve("package.json"), "{\"devDependencies\": {\"vitest\": \"2.1.0\"}}");
        write(repo.resolve("yarn.lock"), "");
        assertEquals("packages declared, but no package-lock.json for npm ci (E-037)", plans(repo).get(0).notMeasured());
        write(repo.resolve("package.json"), "{\"scripts\": {\"test\": \"karma start\"}}");
        assertEquals("no test runner the sandbox reads (vitest, jest, mocha, node --test)", plans(repo).get(0).notMeasured());
        write(repo.resolve("package.json"), "{\"scripts\": {\"test\": \"node --test\"}}");
        assertNull(plans(repo).get(0).install());
        assertTrue(plans(repo).get(0).test().contains("node --test --test-reporter=junit"));
    }

    /** Maven with exact versions installs with strict checksums and runs offline under JaCoCo; a loose version is not measured. */
    @Test
    void javaWithMavenNeedsExactVersionsAndChecksumsAndGradleIsNotRunYet(@TempDir Path repo) throws IOException {
        write(repo.resolve("src/test/java/a/CartTest.java"), "class CartTest {}\n");
        write(repo.resolve("pom.xml"), "<project><dependencies><dependency><version>5.11.4</version></dependency></dependencies></project>");
        TestPlans.Plan plan = plans(repo).get(0);
        assertEquals("mvn -B -q -C -Dmaven.repo.local=/work/.evaluator-m2 dependency:go-offline", plan.install());
        assertTrue(plan.test().contains("mvn -B -o -q -C -Dmaven.repo.local=/work/.evaluator-m2 test"));
        assertTrue(plan.test().contains("-javaagent:/opt/evaluator/jacoco/jacocoagent.jar"));
        for (String loose : List.of("<version>[1.0,2.0)</version>", "<version>1.0-SNAPSHOT</version>", "<version>LATEST</version>", "<version> RELEASE </version>")) {
            write(repo.resolve("pom.xml"), "<project><dependencies><dependency>" + loose + "</dependency></dependencies></project>");
            assertEquals("pom.xml uses a version range, a SNAPSHOT, LATEST or RELEASE (E-038)", plans(repo).get(0).notMeasured(), loose);
        }
        Files.delete(repo.resolve("pom.xml"));
        write(repo.resolve("build.gradle"), "plugins { id 'java' }");
        assertEquals("Gradle builds are not run yet (unplanned: docs/HANDOFF.md)", plans(repo).get(0).notMeasured());
    }

    @Test
    void noTestsNoPlan(@TempDir Path repo) throws IOException {
        write(repo.resolve("src/app.py"), "x = 1\n");
        assertEquals(List.of(), plans(repo));
    }

    @Test
    void hashPinnedMeansEveryRequirement() {
        assertTrue(TestPlans.hashPinned("-r base.txt\n# c\n" + HASHED));
        assertEquals(false, TestPlans.hashPinned(HASHED + "flask==3.0.0\n"));
        assertEquals(false, TestPlans.hashPinned("# nothing\n"));
    }
}
