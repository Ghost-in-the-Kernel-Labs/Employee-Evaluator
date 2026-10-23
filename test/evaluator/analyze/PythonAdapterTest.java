package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Python adapter and the tree-sitter base under it, on source held in the test (so the fixture and its
 * golden are unchanged, E-018): each value worked out by hand, in the comments.
 */
class PythonAdapterTest {
    private static final PythonAdapter PYTHON = new PythonAdapter();

    /** The cyclomatic and cognitive values of the one function in the source. */
    private static List<Integer> measure(String source) {
        Parsed parsed = PYTHON.parse("m.py", source);
        assertEquals(null, parsed.unread());
        assertEquals(1, parsed.functions().size());
        Measure m = parsed.functions().get(0);
        return List.of(m.cyclomatic(), m.cognitive());
    }

    private static Import imp(String module, List<String> names) {
        return new Import(module, names, 1, List.of());
    }

    @Test
    void itReadsPythonAndNamesItself() {
        assertTrue(PYTHON.reads("Python"));
        assertFalse(PYTHON.reads("Java"));
        assertFalse(PYTHON.reads("python"));
        assertEquals("evaluator analyze python 1 (tree-sitter-python 0.25.0)", PYTHON.tool());
    }

    /**
     * An if with a boolean condition, an elif with one, and nested ifs in the if's block, the elif's and the else's.
     * Cognitive: if +1, `and` +1 (2); elif +1, `or` +1 (4); the if in the elif's block at nesting 1: +2 (6);
     * else +1 (7); the if in its block: +2 (9). Cyclomatic: 1 + if, and, elif, or, and the two inner ifs = 7.
     */
    @Test
    void anIfsConditionsAndBlocksAreWalkedAtTheirNesting() {
        assertEquals(List.of(7, 9), measure("""
                def f(a, b, c, d):
                    if a and b:
                        pass
                    elif c or d:
                        if a:
                            pass
                    else:
                        if b:
                            pass
                """));
    }

    /** The if's own block, one level deeper: an if inside it is +2. If +1, the inner if +2 = 3; cyclomatic 3. */
    @Test
    void anIfInAnIfsBlockCountsItsNesting() {
        assertEquals(List.of(3, 3), measure("def f(a, b):\n    if a:\n        if b:\n            pass\n"));
    }

    /** An except inside an if is at nesting 1: if +1, except +2 = 3. Its block is one deeper: except +1, the if in it at nesting 1 +2 = 3. */
    @Test
    void anExceptCountsItsNestingAndItsBlockIsDeeper() {
        assertEquals(List.of(3, 3), measure("def f(a):\n    if a:\n        try:\n            pass\n        except E:\n            pass\n"));
        assertEquals(List.of(3, 3), measure("def f(a):\n    try:\n        pass\n    except E:\n        if a:\n            pass\n"));
    }

    /** A ternary in an if is at nesting 1: if +1, ternary +2 = 3. A ternary at the top, with a ternary in its test, is +1 then +2 deeper: 3. */
    @Test
    void aTernaryCountsItsNesting() {
        assertEquals(List.of(3, 3), measure("def f(a, b):\n    if a:\n        return 1 if b else 2\n"));
        assertEquals(List.of(3, 3), measure("def f(a, b):\n    return 1 if (2 if b else 3) else a\n"));
    }

    /** A comprehension's if is +1 flat and its condition is read: the `and` +1. Cyclomatic: 1 + for, if, and = 4; cognitive 2. */
    @Test
    void aComprehensionsIfIsReadInside() {
        assertEquals(List.of(4, 2), measure("def f(xs, a, b):\n    return [x for x in xs if a and b]\n"));
    }

    /** A call's arguments are read: `g(a or b)` is one `or`. Cyclomatic 2, cognitive 1. */
    @Test
    void aCallsArgumentsAreRead() {
        assertEquals(List.of(2, 1), measure("def f(a, b):\n    return g(a or b)\n"));
    }

    /**
     * A loop's header is at the loop's own level and its else one deeper. for +1; the ternary in the header +1 (2);
     * the if in the else's block at nesting 1: +2 (4). Cyclomatic: 1 + for, ternary, if = 4.
     */
    @Test
    void aLoopsHeaderIsAtItsLevelAndItsElseIsDeeper() {
        assertEquals(List.of(4, 4), measure("""
                def f(xs, a):
                    for x in (1 if a else 2):
                        pass
                    else:
                        if a:
                            pass
                """));
    }

    /** A while's header in a loop's body: while +1, the ternary in its test +1 at nesting 0. Cyclomatic 3, cognitive 2. */
    @Test
    void aWhilesTestIsAtTheWhilesLevel() {
        assertEquals(List.of(3, 2), measure("def f(a):\n    while 1 if a else 2:\n        pass\n"));
    }

    /**
     * Brackets are looked through: `a and (b and c)` and `a and ((b and c))` are one run of `and`, +1 (cyclomatic 3); a
     * bracketed run of the other operator is a change: `a and (b or c)` is +2 (cyclomatic 3).
     */
    @Test
    void bracketsAreLookedThroughWhenTheyHoldOneExpression() {
        assertEquals(List.of(3, 1), measure("def f(a, b, c):\n    return a and (b and c)\n"));
        assertEquals(List.of(3, 1), measure("def f(a, b, c):\n    return a and ((b and c))\n"));
        assertEquals(List.of(3, 2), measure("def f(a, b, c):\n    return a and (b or c)\n"));
    }

    @Test
    void aFileThatDoesNotParseNamesTheLineOfItsFirstError() {
        assertEquals("does not parse: line 3", PYTHON.parse("m.py", "x = 1\ny = 2\nz = = 3\n").unread());
        assertEquals("does not parse: line 4",
                PYTHON.parse("m.py", "class A:\n    def f(self):\n        a = 1\n        b = = 2\n        c = 3\n").unread());
    }

    @Test
    void aFileWithATokenMissingNamesTheLineItShouldHaveBeen() {
        assertEquals("does not parse: line 3", PYTHON.parse("m.py", "a = 1\nb = 2\nc = foo(1, 2\n").unread());
    }

    /** Where imports lie: the statement's line (1-based), and what each binds that nothing uses. */
    @Test
    void importsKeepTheirLines() {
        Parsed parsed = PYTHON.parse("m.py", "\n\nimport os\nfrom a.b import c, d as e\n\nprint(c)\n");
        assertEquals(List.of(new Import("os", List.of(), 3, List.of("os")), new Import("a.b", List.of("c", "d"), 4, List.of("e"))),
                parsed.imports());
    }

    @Test
    void aRelativeImportOfAPackageItselfLeadsToItsInit() {
        Set<String> files = Set.of("pkg/__init__.py", "pkg/a.py");
        // `from . import missing`: no module pkg/missing, so the package.
        assertEquals(List.of("pkg/__init__.py"), PYTHON.resolve(imp(".", List.of("missing")), "pkg/a.py", files));
        assertEquals(List.of("pkg/__init__.py"), PYTHON.resolve(imp(".", List.of()), "pkg/a.py", files));
        assertEquals(List.of("pkg/a.py"), PYTHON.resolve(imp(".", List.of("a")), "pkg/b.py", files));
    }

    @Test
    void aRelativeImportGoesUpOneFolderPerDotAndStopsAtTheRoot() {
        Set<String> files = Set.of("c.py", "a/d.py", "a/b/e.py");
        assertEquals(List.of("c.py"), PYTHON.resolve(imp("..c", List.of("x")), "a/b.py", files));
        assertEquals(List.of("a/d.py"), PYTHON.resolve(imp("..d", List.of("x")), "a/b/f.py", files));
        assertEquals(List.of("c.py"), PYTHON.resolve(imp("...c", List.of("x")), "a/b/f.py", files));
        assertEquals(List.of(), PYTHON.resolve(imp("...c", List.of("x")), "a/b.py", files));
        assertEquals(List.of(), PYTHON.resolve(imp("....c", List.of("x")), "a/b/f.py", files));
    }

    @Test
    void thePathHelpers() {
        assertEquals("a/b", TreeSitterAdapter.normalize("a/b"));
        assertEquals("a/c", TreeSitterAdapter.normalize("a/./b/../c"));
        assertEquals("", TreeSitterAdapter.normalize("a/.."));
        assertEquals(null, TreeSitterAdapter.normalize("a/../.."));
        assertEquals("a/b", TreeSitterAdapter.folder("a/b/c.py"));
        assertEquals("", TreeSitterAdapter.folder("c.py"));
        assertEquals("a", TreeSitterAdapter.folder("a/c.py"));
    }
}
