package evaluator.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintainabilityTest {
    private static final Evidence TOOL = new Evidence("a tool 1", List.of("src/a.py"));

    private static Fact repository(String kind, Number value) {
        return Fact.measured(kind, Subject.repository(), value, TOOL);
    }

    private static Fact of(String kind, Subject.Kind subject, String id, Number value) {
        return Fact.measured(kind, new Subject(subject, id), value, TOOL);
    }

    /**
     * Worked by hand. Four files: duplication 0.05 (0.5), tests 0.5 (0.5), one file of four in a cycle
     * (0.75), functions of cognitive 3, 20 (one of two over 15: 0.5), fan-out 2, 12, 0, 1 (three of four
     * at most 10: 0.75), unused imports 2 over four files (0.5). 0.125 + 0.125 + 0.1125 + 0.075 + 0.075 +
     * 0.05 = 0.5625: 56.
     */
    @Test
    void sixPartsAsWorkedByHand() {
        List<Fact> facts = new ArrayList<>(List.of(repository("MAINTAINABILITY.duplication", 0.05), repository("MAINTAINABILITY.test_to_code_ratio", 0.5),
                repository("MAINTAINABILITY.import_cycle_files", 1),
                of("COMPLEXITY.cognitive", Subject.Kind.FUNCTION, "a.py:f", 3), of("COMPLEXITY.cognitive", Subject.Kind.FUNCTION, "a.py:g", 20)));
        int[] fanOut = {2, 12, 0, 1}, unused = {2, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            facts.add(of("MAINTAINABILITY.fan_out", Subject.Kind.FILE, "f" + i, fanOut[i]));
            facts.add(of("MAINTAINABILITY.unused_imports", Subject.Kind.FILE, "f" + i, unused[i]));
        }
        Map<String, Double> parts = Maintainability.parts(facts, new ArrayList<>());
        assertEquals(Map.of("duplication", 0.5, "tests", 0.5, "cycles", 0.75, "functions", 0.5, "coupling", 0.75, "unused", 0.5), parts);
        assertEquals(56, Maintainability.score(facts).value());
        assertEquals(facts.size(), Maintainability.score(facts).facts().size(), "every fact read is behind the score");
    }

    /** The limits are "at most": a function of cognitive 15 and a file of fan-out 10 are within them, 16 and 11 are not. */
    @Test
    void aValueOnTheLimitIsWithinIt() {
        List<Fact> facts = List.of(of("COMPLEXITY.cognitive", Subject.Kind.FUNCTION, "a.py:f", 15), of("COMPLEXITY.cognitive", Subject.Kind.FUNCTION, "a.py:g", 16),
                of("MAINTAINABILITY.fan_out", Subject.Kind.FILE, "a.py", 10), of("MAINTAINABILITY.fan_out", Subject.Kind.FILE, "b.py", 11));
        Map<String, Double> parts = Maintainability.parts(facts, new ArrayList<>());
        assertEquals(0.5, parts.get("functions"));
        assertEquals(0.5, parts.get("coupling"));
    }

    /** Past the limits each part reads 0, never below; tests past 1 read 1, never above. */
    @Test
    void eachPartIsHeldToZeroAndOne() {
        Map<String, Double> parts = Maintainability.parts(List.of(repository("MAINTAINABILITY.duplication", 0.4), repository("MAINTAINABILITY.test_to_code_ratio", 2.5)), new ArrayList<>());
        assertEquals(Map.of("duplication", 0.0, "tests", 1.0), parts);
    }

    /** A part with no facts is left out and the rest fill its weight: duplication 0 (0.25) and tests 1 (0.25): 50. */
    @Test
    void absentPartsAreLeftOutNotZeroed() {
        assertEquals(50, Maintainability.score(List.of(repository("MAINTAINABILITY.duplication", 0.2), repository("MAINTAINABILITY.test_to_code_ratio", 1))).value());
        assertNull(Maintainability.score(List.of(repository("SIZE.source_lines", 100))), "no part, no score");
    }
}
