package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Java adapter's rules on source held in the test, so the fixture and its golden stay as they are
 * (E-018): each construct is tried at nesting level 1 (inside an `if`, which costs 1), where a nesting
 * term of the wrong sign shows, and each value is worked by hand (mutation run 9).
 */
class JavaComplexityTest {
    private static Parsed parse(String text) {
        return new JavaAdapter().parse("src/A.java", text);
    }

    /** A construct inside an `if` is counted at its nesting: the if is 1, the construct 1 + 1. */
    @Test
    void eachConstructCountsItsNestingAndNestsItsBody() {
        String source = """
                class A {
                    void elseIfBody(boolean a, boolean b, boolean c) { if (a) {} else if (b) { if (c) {} } }
                    void elseBody(boolean a, boolean b) { if (a) {} else { if (b) {} } }
                    int conditionals(boolean a, boolean b, boolean c) { return a ? (b ? 1 : 2) : (c ? 3 : 4); }
                    void switchInIf(boolean a, int x) { if (a) { switch (x) { default: break; } } }
                    void switchCase(boolean a, int x) { switch (x) { case 1: if (a) {} break; default: } }
                    int switchExpressionCase(boolean a, int x) { return switch (x) { case 1 -> a ? 1 : 2; default -> 0; }; }
                    void forEachInIf(boolean a, int[] xs) { if (a) { for (int i : xs) {} } }
                    void whileInIf(boolean a) { if (a) { while (a) {} } }
                    void ifInWhile(boolean a) { while (a) { if (a) {} } }
                    void doInIf(boolean a) { if (a) { do {} while (a); } }
                    void ifInDo(boolean a) { do { if (a) {} } while (a); }
                    void catchInIf(boolean a) { if (a) { try { run(); } catch (RuntimeException e) {} } }
                    void ifInCatch(boolean a) { try { run(); } catch (RuntimeException e) { if (a) {} } }
                    void plainBreak(boolean a) { while (a) { break; } }
                    void labelledBreak(boolean a) { outer: while (a) { while (a) { break outer; } } }
                }
                """;
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("A.elseIfBody", 4);
        expected.put("A.elseBody", 4);
        expected.put("A.conditionals", 5);
        expected.put("A.switchInIf", 3);
        expected.put("A.switchCase", 3);
        expected.put("A.switchExpressionCase", 3);
        expected.put("A.forEachInIf", 3);
        expected.put("A.whileInIf", 3);
        expected.put("A.ifInWhile", 3);
        expected.put("A.doInIf", 3);
        expected.put("A.ifInDo", 3);
        expected.put("A.catchInIf", 3);
        expected.put("A.ifInCatch", 3);
        expected.put("A.plainBreak", 1);
        expected.put("A.labelledBreak", 4);
        Map<String, Integer> measured = new LinkedHashMap<>();
        for (Measure m : parse(source).functions()) measured.put(m.function(), m.cognitive());
        assertEquals(expected, measured);
    }

    /** `a.b.*` is the files directly in a folder ending `a/b`, and only .java files. */
    @Test
    void aPackageImportIsTheJavaFilesDirectlyInItsFolder() {
        Set<String> files = Set.of("a/b/X.java", "src/a/b/Y.java", "a/b/c/Z.java", "a/b/Notes.txt", "xa/b/W.java", "B.java", "/a.java");
        List<String> found = new JavaAdapter().resolve(new Import("a.b.*", List.of(), 1, List.of()), "src/Main.java", files);
        assertEquals(List.of("a/b/X.java", "src/a/b/Y.java"), found.stream().sorted().toList());
        assertEquals(List.of("B.java"), JavaImports.packageFiles("", files).stream().filter(f -> !f.contains("/")).toList());
    }

    @Test
    void siblingsOfAFileAtTheRootOfAnAbsolutePath() {
        assertEquals(List.of("/B.java"), JavaImports.siblings(List.of("B", "C"), "/A.java", Set.of("/B.java", "B.java")));
        assertEquals(List.of("B.java"), JavaImports.siblings(List.of("B"), "A.java", Set.of("B.java", "/B.java")));
    }

    /** The own-package import sits on the package line, or line 1 with no package line. */
    @Test
    void theOwnPackageImportIsAtThePackageLine() {
        Import withPackage = parse("// header\n\npackage p.q;\n\nclass A { B b; }\n").imports().get(0);
        assertEquals(new Import("p.q.*", List.of("B"), 3, List.of()), withPackage);
        Import none = parse("\n\nclass A { B b; }\n").imports().get(0);
        assertEquals(new Import("*", List.of("B"), 1, List.of()), none);
    }
}
