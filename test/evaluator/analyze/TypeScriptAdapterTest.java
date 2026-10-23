package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The TypeScript and JavaScript adapters and the import readers under them, on source held in the test (so the
 * fixtures and their goldens are unchanged, E-018): each value worked out by hand, in the comments.
 */
class TypeScriptAdapterTest {
    private static final TypeScriptAdapter TS = new TypeScriptAdapter();
    private static final JavaScriptAdapter JS = new JavaScriptAdapter();

    private static Import imp(String module) {
        return new Import(module, List.of(), 1, List.of());
    }

    @Test
    void theyReadTheirLanguagesAndNameThemselves() {
        assertTrue(TS.reads("TypeScript"));
        assertFalse(TS.reads("JavaScript"));
        assertTrue(JS.reads("JavaScript"));
        assertFalse(JS.reads("TypeScript"));
        assertEquals("evaluator analyze typescript 1 (tree-sitter-typescript 0.23.2)", TS.tool());
        assertEquals("evaluator analyze javascript 1 (tree-sitter-typescript 0.23.2, TSX grammar)", JS.tool());
    }

    /**
     * An if with a boolean condition, an else if with one, and an if in the if's block, the else if's block and the
     * else's. Cognitive: if +1, `&&` +1 (2); the if in its block at nesting 1: +2 (4); else if +1, `||` +1 (6); the
     * if in its block at nesting 1: +2 (8); else +1 (9); the if in its block: +2 (11). Cyclomatic: 1 + the if, `&&`,
     * the inner if, the else if, `||`, its inner if and the else's inner if = 8.
     */
    @Test
    void anIfsConditionsAndBlocksAreWalkedAtTheirNesting() {
        Parsed parsed = TS.parse("m.ts", """
                function f(a, b, c, d) {
                  if (a && b) {
                    if (c) { a; }
                  } else if (c || d) {
                    if (a) { b; }
                  } else {
                    if (b) { c; }
                  }
                }
                """);
        assertEquals(1, parsed.functions().size());
        assertEquals(List.of(8, 11), List.of(parsed.functions().get(0).cyclomatic(), parsed.functions().get(0).cognitive()));
    }

    /** `import` statements and requires in one file: the order is the file's, whichever kind comes first. */
    @Test
    void requiresAndImportsComeInFileOrderWithTheirUnusedNames() {
        Parsed parsed = JS.parse("m.js", """
                const a = require("./a");
                import b from "./b";
                const { c, d: e, f } = require("./m");
                var g = require("./g");
                foo("./x");
                const h = require("./h", 1);
                const i = other("./i");
                b(); c(); f();
                """);
        assertEquals(null, parsed.unread());
        // a, e and g are bound and never used (c, f and b are called); the calls foo(...) and other(...), and a
        // require with two arguments, are no imports.
        assertEquals(List.of(
                new Import("./a", List.of(), 1, List.of("a")),
                new Import("./b", List.of(), 2, List.of()),
                new Import("./m", List.of(), 3, List.of("e")),
                new Import("./g", List.of(), 4, List.of("g"))), parsed.imports());
    }

    /** A bare require binds nothing; a name bound as `require` is used only where it is used outside its own declaration. */
    @Test
    void aBareRequireBindsNothingAndADeclarationsOwnPartsAreNotUses() {
        assertEquals(List.of(new Import("./side", List.of(), 2, List.of())), JS.parse("m.js", "const x = 1;\nrequire(\"./side\");\n").imports());
        assertEquals(List.of(new Import("./r", List.of(), 1, List.of("require"))), JS.parse("m.js", "const { require } = require(\"./r\");\n").imports());
    }

    /** `./` and `../` lead into the repository, a package name does not, even when a file of that name sits beside. */
    @Test
    void onlyARelativeModuleLeadsIntoTheRepository() {
        Set<String> files = Set.of("a/x.ts", "a/b/x.ts", "a/b/y.tsx", "a/z.js", "a/b/w.js");
        assertEquals(List.of("a/x.ts"), TS.resolve(imp("../x"), "a/b/f.ts", files));
        assertEquals(List.of("a/b/y.tsx"), TS.resolve(imp("./y"), "a/b/f.ts", files));
        assertEquals(List.of(), TS.resolve(imp("x"), "a/b/f.ts", files));
        assertEquals(List.of("a/z.js"), JS.resolve(imp("../z"), "a/b/f.js", files));
        assertEquals(List.of("a/b/w.js"), JS.resolve(imp("./w"), "a/b/f.js", files));
        assertEquals(List.of(), JS.resolve(imp("w"), "a/b/f.js", files));
    }
}
