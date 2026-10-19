package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TreeSitterPython;
import org.treesitter.TreeSitterTypescript;

/**
 * The vendored tree-sitter loads its native library and parses, wherever the fast loop runs: here,
 * in the image with the network off, on Ghost's Windows machine (E-028). If this fails, the platform
 * has no native library in the jars, and the adapters cannot run there.
 */
class TreeSitterLoadsTest {
    private static TSNode parse(TSParser parser, String source) {
        return parser.parseString(null, source).getRootNode();
    }

    @Test
    void pythonParses() {
        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterPython());
        TSNode root = parse(parser, "def f(x):\n    if x:\n        return 1\n    return 0\n");
        assertEquals("module", root.getType());
        assertFalse(root.hasError());
        assertEquals("function_definition", root.getChild(0).getType());
    }

    @Test
    void typescriptParsesNewSyntax() {
        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterTypescript());
        TSNode root = parse(parser, "export const n = 1 satisfies number;\n");
        assertEquals("program", root.getType());
        assertFalse(root.hasError());
    }

    @Test
    void brokenCodeStillGivesATree() {
        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterPython());
        TSNode root = parse(parser, "def h(:\n    if x\n        return\n");
        assertEquals("module", root.getType());
        assertTrue(root.hasError());
    }
}
