package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A comment inside an import's braces or clause (J-133): tree-sitter gives a comment as a named node, and the import
 * readers took every named child for a specifier and crashed on a null name. Synthetic source only.
 */
class ImportCommentsTest {
    private static Parsed ts(String source) {
        Parsed parsed = new TypeScriptAdapter().parse("m.ts", source);
        assertNull(parsed.unread());
        return parsed;
    }

    @Test
    void aLineCommentBetweenNamedImportsBindsNothing() {
        Parsed parsed = ts("import { a, // note\n  b as c } from \"./m\";\nconsole.log(a);\n");
        assertEquals(1, parsed.imports().size());
        assertEquals(List.of("c"), parsed.imports().get(0).unused());
    }

    @Test
    void aBlockCommentBeforeBetweenAndAfterSpecifiers() {
        Parsed parsed = ts("import { /* x */ a, /* y */ b as c /* z */ } from \"./m\";\nconsole.log(a);\n");
        assertEquals(List.of("c"), parsed.imports().get(0).unused());
        Parsed all = ts("import { /* x */ a, b } from \"./m\";\n");
        assertEquals(List.of("a", "b"), all.imports().get(0).unused());
    }

    @Test
    void aCommentAfterTheLastSpecifierAndOnlyAComment() {
        Parsed after = ts("import {\n  a, // last\n} from \"./m\";\nconsole.log(a);\n");
        assertEquals(List.of(), after.imports().get(0).unused());
        Parsed empty = ts("import { /* none */ } from \"./m\";\n");
        assertEquals(List.of(), empty.imports().get(0).unused());
        assertEquals("./m", empty.imports().get(0).module());
    }

    @Test
    void aCommentInADefaultAndNamespaceClause() {
        Parsed parsed = ts("import d, /* c */ { a } from \"./m\";\nimport * /* c */ as ns from \"./n\";\nimport /* c */ * as /* d */ ms from \"./o\";\n");
        assertEquals(List.of("d", "a"), parsed.imports().get(0).unused());
        assertEquals(List.of("ns"), parsed.imports().get(1).unused());
        assertEquals(List.of("ms"), parsed.imports().get(2).unused());
    }

    @Test
    void aReExportAndATypeImportWithComments() {
        Parsed parsed = ts("import { type A, /* c */ B } from \"./m\";\nexport { x, // c\n y } from \"./n\";\nconsole.log(B);\n");
        assertEquals(List.of("A"), parsed.imports().get(0).unused());
        assertEquals(List.of(), parsed.imports().get(1).unused());
    }

    @Test
    void javaScriptReadsTheSameCommentsAndRequirePatternsWithComments() {
        Parsed parsed = new JavaScriptAdapter().parse("m.js",
                "import { a, // note\n b } from \"./m\";\nconst { c, /* x */ d: e } = require(\"./n\"); // tail\nconsole.log(a, c);\n");
        assertNull(parsed.unread());
        assertEquals(List.of("b"), parsed.imports().get(0).unused());
        assertEquals(List.of("e"), parsed.imports().get(1).unused());
    }

    @Test
    void pythonFromImportsWithCommentsInParentheses() {
        Parsed parsed = new PythonAdapter().parse("m.py", "from m import (a,  # note\n    b as c,  # more\n)\nimport x  # tail\nprint(a)\n");
        assertNull(parsed.unread());
        assertEquals(List.of("a", "b"), parsed.imports().get(0).names());
        assertEquals(List.of("c"), parsed.imports().get(0).unused());
        assertEquals("x", parsed.imports().get(1).module());
    }
}
