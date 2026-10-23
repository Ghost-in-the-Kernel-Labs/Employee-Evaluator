package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.ingest.Ingest;
import evaluator.ingest.Inventory;
import evaluator.rubric.Fact;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestFilesTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));

    @Test
    void aTestIsKnownByItsFolderOrItsName() {
        for (String test : List.of("tests/test_util.py", "src/test/java/a/CartTest.java", "pkg/__tests__/x.ts", "app/util_test.py",
                "web/button.test.tsx", "web/button.spec.ts", "conftest.py", "a/CartIT.java", "server/handler_test.go", "spec/models/user.rb")) {
            assertTrue(TestFiles.isTest(test), test);
        }
        for (String code : List.of("src/app/main.py", "src/testing_tools.py", "src/Testimony.java", "lib/latest.ts", "contest.py", "test.py", "src/tests.ts")) {
            assertFalse(TestFiles.isTest(code), code);
        }
    }

    /** Only the folders above a file count: a file called `test` or `spec` is a file, not a test folder. */
    @Test
    void aFileNamedLikeATestFolderIsNotInIt() {
        assertFalse(TestFiles.isTest("src/test"));
        assertFalse(TestFiles.isTest("spec"));
        assertTrue(TestFiles.isTest("test/x"));
        assertTrue(TestFiles.isTest("a/spec/b/c.rb"));
    }

    /** small-app: tests/test_util.py's 3 lines over the 20 of Report.java (7), main.py (6), util.py (2), index.ts (5). */
    @Test
    void theRatioIsTestLinesOverCodeLines() {
        Fact fact = TestFiles.ratio(Ingest.read(ROOT.resolve("test/fixtures/small-app"))).orElseThrow();
        assertEquals("MAINTAINABILITY.test_to_code_ratio repository:.", fact.key());
        assertEquals(0.15, fact.value());
        assertEquals(List.of("tests/test_util.py"), fact.evidence().sources());
    }

    @Test
    void noTestsIsZeroAndNoCodeIsNotMeasured() {
        Inventory code = new Inventory(List.of(new Inventory.Source("a.py", "Python", 4)), List.of());
        assertEquals(0.0, TestFiles.ratio(code).orElseThrow().value());
        assertEquals(List.of("(no test file found)"), TestFiles.ratio(code).orElseThrow().evidence().sources());
        Inventory onlyTests = new Inventory(List.of(new Inventory.Source("tests/test_a.py", "Python", 4)), List.of());
        assertTrue(TestFiles.ratio(onlyTests).isEmpty());
    }
}
