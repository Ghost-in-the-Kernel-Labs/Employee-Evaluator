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

    /**
     * TODO.md 2.2j, worked by hand: the predicate says what is a test, not the path rule. a/x.py (10) and b/y.py (6) are tests by it,
     * tests/z.py (4, a test by the path rule) and c.py (20) are not: 16 over 24 = 0.667, citing the two.
     */
    @Test
    void theRatioTakesWhatIsATestFromThePredicate() {
        Inventory inventory = new Inventory(List.of(new Inventory.Source("a/x.py", "Python", 10), new Inventory.Source("b/y.py", "Python", 6),
                new Inventory.Source("tests/z.py", "Python", 4), new Inventory.Source("c.py", "Python", 20)), List.of());
        Fact fact = TestFiles.ratio(inventory, path -> path.equals("a/x.py") || path.equals("b/y.py")).orElseThrow();
        assertEquals(0.667, fact.value());
        assertEquals(List.of("a/x.py", "b/y.py"), fact.evidence().sources());
        assertEquals(0.111, TestFiles.ratio(inventory).orElseThrow().value(), "the path rule: tests/z.py alone, 4 over 36");
        assertTrue(TestFiles.ratio(inventory, path -> true).isEmpty(), "everything a test: no code, no ratio");
    }
}
