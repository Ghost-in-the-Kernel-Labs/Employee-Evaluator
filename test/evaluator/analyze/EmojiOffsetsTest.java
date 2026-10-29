package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A character beyond the Basic Multilingual Plane (an emoji) in a source file (J-131): tree-sitter's byte offsets
 * must index the same bytes `Source` slices, so a function's name, line and text stay right with an emoji before
 * and inside it. Synthetic source only, for each tree-sitter language the kernel has.
 */
class EmojiOffsetsTest {
    private static final String SMILE = "😀";

    private static void check(TreeSitterAdapter adapter, String path, String template, String body) {
        Parsed parsed = adapter.parse(path, template.replace("@", SMILE));
        assertNull(parsed.unread());
        assertEquals(2, parsed.functions().size());
        Measure first = parsed.functions().get(0);
        Measure second = parsed.functions().get(1);
        assertEquals("first", first.function());
        assertEquals("second", second.function());
        assertEquals(body.replace("@", SMILE), second.text().strip());
        // The same source with each emoji a two-letter word has the same lines and values: only the text differs.
        Parsed plain = adapter.parse(path, template.replace("@", "ab"));
        for (int i = 0; i < 2; i++) {
            Measure e = parsed.functions().get(i);
            Measure p = plain.functions().get(i);
            assertEquals(p.function(), e.function());
            assertEquals(p.line(), e.line());
            assertEquals(p.endLine(), e.endLine());
            assertEquals(p.cyclomatic(), e.cyclomatic());
            assertEquals(p.cognitive(), e.cognitive());
        }
        assertEquals(plain.functions().get(1).text().replace("ab", SMILE), second.text());
    }

    @Test
    void pythonOffsetsSurviveAnEmojiBeforeAndInsideAFunction() {
        check(new PythonAdapter(), "m.py", """
                # @ @ a comment
                label = "@@"
                def first(a):
                    return "@" if a else "@@"
                def second(b):
                    # @ inside
                    if b:
                        return "@@@"
                    return 0
                """, """
                def second(b):
                    # @ inside
                    if b:
                        return "@@@"
                    return 0""");
    }

    @Test
    void typeScriptAndJavaScriptOffsetsSurviveAnEmojiBeforeAndInsideAFunction() {
        String template = """
                // @ @ a comment
                import { used, unused } from "./m";
                const label = "@@";
                function first(a: number) { return a ? "@" : "@@" + used; }
                function second(b: number) {
                  // @ inside
                  if (b) { return "@@@"; }
                  return 0;
                }
                """;
        String body = """
                function second(b: number) {
                  // @ inside
                  if (b) { return "@@@"; }
                  return 0;
                }""";
        check(new TypeScriptAdapter(), "m.ts", template, body);
        String js = template.replace(": number", "");
        check(new JavaScriptAdapter(), "m.js", js, body.replace(": number", ""));
    }

    @Test
    void importsAndTheirUnusedNamesSurviveAnEmoji() {
        Parsed parsed = new TypeScriptAdapter().parse("m.ts", "const s = \"" + SMILE + SMILE + "\";\nimport { a, b } from \"./m\";\nconsole.log(a);\n");
        assertNull(parsed.unread());
        assertEquals(List.of("b"), parsed.imports().get(0).unused());
        assertEquals(2, parsed.imports().get(0).line());
    }

    @Test
    void aLargeFileWithManyEmojiIsReadWhole() {
        String filler = ("x = \"" + SMILE + "\"\n").repeat(5000);
        Parsed parsed = new PythonAdapter().parse("m.py", filler + "def last():\n    return \"" + SMILE + "\"\n");
        assertNull(parsed.unread());
        assertEquals(1, parsed.functions().size());
        assertEquals("last", parsed.functions().get(0).function());
        assertEquals(5001, parsed.functions().get(0).line());
        assertEquals("def last():\n    return \"" + SMILE + "\"", parsed.functions().get(0).text());
    }

    /** The parse reads through a reader whose buffer is the array it was given: the file's own bytes stay as they were. */
    @Test
    void theTextOfAFunctionIsTheFilesOwnTextAndAnEmptyFileHasNone() {
        Parsed parsed = new PythonAdapter().parse("m.py", "def f():\n    return 1\n");
        assertEquals("def f():\n    return 1", parsed.functions().get(0).text());
        assertEquals(0, new PythonAdapter().parse("m.py", "").functions().size());
    }
}
