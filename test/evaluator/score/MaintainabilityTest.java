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
     * 0.05 = 0.5625 weighted; against the baseline's 0.89567 that is 100 × 0.5625 ÷ 0.89567 = 62.80: 63 (E-075).
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
        assertEquals(0.5625, Maintainability.weighted(facts, new ArrayList<>()), 1e-12);
        assertEquals(63, Maintainability.score(facts).value());
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

    /**
     * A part with no facts is left out and the others' weights fill it (E-051): duplication 0 (0.25) and tests 1 (0.25) weigh 0.5 of
     * 0.5, so 0.5; and that is what is scaled against the baseline (E-075): 100 × 0.5 ÷ 0.89567 = 55.82, 56. The baseline stays whole
     * (all six parts), so a repository missing a part is read against the baseline's full score, not against the baseline's same parts.
     */
    @Test
    void absentPartsAreLeftOutNotZeroed() {
        List<Fact> two = List.of(repository("MAINTAINABILITY.duplication", 0.2), repository("MAINTAINABILITY.test_to_code_ratio", 1));
        assertEquals(0.5, Maintainability.weighted(two, new ArrayList<>()), 1e-12);
        assertEquals(56, Maintainability.score(two).value());
        assertEquals(112, Maintainability.score(List.of(repository("MAINTAINABILITY.test_to_code_ratio", 1))).value(), "one part alone fills the weight: 1 ÷ 0.89567");
        assertEquals(0, Maintainability.score(List.of(repository("MAINTAINABILITY.duplication", 0.2))).value(), "a part that reads 0 is present, and reads 0");
    }

    /** No Maintainability fact at all is "not measured", never 0 (SC-07): the facts of other metrics do not make a part. */
    @Test
    void noFactsIsNotMeasured() {
        assertNull(Maintainability.score(List.of(repository("SIZE.source_lines", 100))), "no part, no score");
        assertNull(Maintainability.score(List.of()));
        assertNull(Maintainability.weighted(List.of(), new ArrayList<>()));
    }

    /**
     * E-075 worked by hand. Suite Builder at 32762a6 has duplication 0.027 (0.73), tests 1.412 (1), 87 of 914 files in a cycle (0.9048),
     * 4,233 of 4,410 functions at most cognitive 15 (0.95986), 851 of 914 files importing at most 10 (0.93107) and 88 unused imports over
     * 913 files (0.90361): 0.1825 + 0.25 + 0.13572 + 0.14398 + 0.09311 + 0.09036 = 0.89567. Those facts are built here exactly, so the
     * baseline reads 100 against itself; a repository with every part perfect reads 100 ÷ 0.89567 = 111.65, 112 (no ceiling, E-071);
     * the tests part alone at 0.4433 is 49.49 (49) and at 0.4434 is 49.50 (50).
     */
    @Test
    void theBaselineReadsHundredAgainstItself() {
        List<Fact> facts = new ArrayList<>(List.of(repository("MAINTAINABILITY.duplication", 0.027), repository("MAINTAINABILITY.test_to_code_ratio", 1.412),
                repository("MAINTAINABILITY.import_cycle_files", 87)));
        for (int i = 0; i < 914; i++) facts.add(of("MAINTAINABILITY.fan_out", Subject.Kind.FILE, "f" + i, i < 851 ? 3 : 11));
        for (int i = 0; i < 4410; i++) facts.add(of("COMPLEXITY.cognitive", Subject.Kind.FUNCTION, "f" + i, i < 4233 ? 2 : 16));
        for (int i = 0; i < 913; i++) facts.add(of("MAINTAINABILITY.unused_imports", Subject.Kind.FILE, "u" + i, i < 88 ? 1 : 0));
        Map<String, Double> parts = Maintainability.parts(facts, new ArrayList<>());
        assertEquals(0.73, parts.get("duplication"), 1e-12);
        assertEquals(1.0, parts.get("tests"));
        assertEquals(1 - 87.0 / 914, parts.get("cycles"), 1e-12);
        assertEquals(4233.0 / 4410, parts.get("functions"), 1e-12);
        assertEquals(851.0 / 914, parts.get("coupling"), 1e-12);
        assertEquals(1 - 88.0 / 913, parts.get("unused"), 1e-12);
        assertEquals(Formulas.BASELINE_MAINTAINABILITY, Maintainability.weighted(facts, new ArrayList<>()), 1e-12, "the recorded weighted score is what those facts give");
        assertEquals(0.89567, Formulas.BASELINE_MAINTAINABILITY, 5e-6);
        assertEquals(100, Maintainability.score(facts).value(), "Suite Builder at the baseline commit reads 100 against itself");

        assertEquals(112, Maintainability.score(List.of(repository("MAINTAINABILITY.duplication", 0), repository("MAINTAINABILITY.test_to_code_ratio", 1))).value(),
                "every part perfect: better than the baseline, 111.65, past 100");
        assertEquals(49, Maintainability.score(List.of(repository("MAINTAINABILITY.test_to_code_ratio", 0.4433))).value());
        assertEquals(50, Maintainability.score(List.of(repository("MAINTAINABILITY.test_to_code_ratio", 0.4434))).value());
    }
}
