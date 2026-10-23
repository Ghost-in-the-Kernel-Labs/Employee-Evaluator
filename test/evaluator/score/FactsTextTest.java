package evaluator.score;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FactsTextTest {
    private static final Evidence TOOL = new Evidence("a tool 1", List.of("src/a.py"));

    private static Fact fact(String kind, Subject subject, Object value) {
        return Fact.measured(kind, subject, value, TOOL);
    }

    /**
     * MaintainabilityTest's four files, worked by hand, with Size and Complexity's two facts added: 120 source lines, depth 2. Two
     * functions of cognitive 3 and 20 are 23 over 2 functions; four files; the six parts 0.5, 0.5, 0.75, 0.5, 0.75, 0.5 and their weighted
     * score 0.5625 (0.125 + 0.125 + 0.1125 + 0.075 + 0.075 + 0.05). A function's and a file's own facts are not repository-wide, so not listed.
     */
    @Test
    void theValuesBehindACardAsWorkedByHand() {
        List<Fact> facts = new ArrayList<>(List.of(fact("SIZE.source_lines", Subject.repository(), 120), fact("COMPLEXITY.dependency_depth", Subject.repository(), 2),
                fact("MAINTAINABILITY.duplication", Subject.repository(), 0.05), fact("MAINTAINABILITY.test_to_code_ratio", Subject.repository(), 0.5),
                fact("MAINTAINABILITY.import_cycle_files", Subject.repository(), 1),
                fact("COMPLEXITY.cognitive", new Subject(Subject.Kind.FUNCTION, "a.py:f"), 3), fact("COMPLEXITY.cognitive", new Subject(Subject.Kind.FUNCTION, "a.py:g"), 20)));
        int[] fanOut = {2, 12, 0, 1}, unused = {2, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            facts.add(fact("MAINTAINABILITY.fan_out", new Subject(Subject.Kind.FILE, "f" + i), fanOut[i]));
            facts.add(fact("MAINTAINABILITY.unused_imports", new Subject(Subject.Kind.FILE, "f" + i), unused[i]));
        }
        assertEquals("""
                COMPLEXITY.cognitive_total 23
                COMPLEXITY.dependency_depth 2
                COMPLEXITY.functions 2
                MAINTAINABILITY.duplication 0.05
                MAINTAINABILITY.files 4
                MAINTAINABILITY.import_cycle_files 1
                MAINTAINABILITY.part.coupling 0.75
                MAINTAINABILITY.part.cycles 0.75
                MAINTAINABILITY.part.duplication 0.5
                MAINTAINABILITY.part.functions 0.5
                MAINTAINABILITY.part.tests 0.5
                MAINTAINABILITY.part.unused 0.5
                MAINTAINABILITY.test_to_code_ratio 0.5
                MAINTAINABILITY.weighted 0.5625
                SIZE.source_lines 120
                """, FactsText.render(facts));
    }

    /** No facts, no lines; only the values that are there are listed, and no weighted score without a part. */
    @Test
    void whatIsAbsentIsNotListed() {
        assertEquals("", FactsText.render(List.of()));
        assertEquals("SIZE.source_lines 0\n", FactsText.render(List.of(fact("SIZE.source_lines", Subject.repository(), 0))));
        assertEquals("COMPLEXITY.cognitive_total 0\nCOMPLEXITY.functions 1\nMAINTAINABILITY.part.functions 1\nMAINTAINABILITY.weighted 1\n",
                FactsText.render(List.of(fact("COMPLEXITY.cognitive", new Subject(Subject.Kind.FUNCTION, "a.py:f"), 0))));
    }

    /** A number is plain: whole without a point, a fraction in full, a small one never in exponent form; a text or a yes/no as it is. */
    @Test
    void numbersArePlain() {
        assertEquals("100", FactsText.text(100.0));
        assertEquals("1", FactsText.text(1));
        assertEquals("0.0001", FactsText.text(1.0E-4));
        assertEquals("10000000000", FactsText.text(1.0E10));
        assertEquals("0", FactsText.text(0.0));
        assertEquals("0.895670359282887", FactsText.text(0.895670359282887));
        assertEquals("1.412", FactsText.text(1.412));
        assertEquals("true", FactsText.text(true));
        assertEquals("claude", FactsText.text("claude"));
    }

    /** A fact about a file or a function is not repository-wide: it is not listed under its own kind. */
    @Test
    void onlyTheRepositoryWideFactsAreListedAsFacts() {
        assertEquals("", FactsText.render(List.of(fact("SIZE.source_lines", new Subject(Subject.Kind.FILE, "a.py"), 5))));
    }
}
