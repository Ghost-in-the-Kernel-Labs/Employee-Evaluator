package evaluator.rubric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FactTest {
    private static final Evidence READ = new Evidence("ingest 1", List.of("src/a.py"));

    @Test
    void aMeasurementIsCertainAndInferred() {
        Fact fact = Fact.measured("SIZE.source_lines", Subject.repository(), 12, READ);
        assertEquals(1.0, fact.confidence());
        assertEquals(State.INFERRED, fact.state());
        assertEquals(Metric.SIZE, fact.metric());
    }

    @Test
    void aFactWithNoEvidenceIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("SIZE.source_lines", Subject.repository(), 12, null));
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("SIZE.source_lines", Subject.repository(), 12, new Evidence("ingest 1", List.of())));
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("SIZE.source_lines", Subject.repository(), 12, new Evidence(" ", List.of("a"))));
    }

    @Test
    void aKindMustNameAMetric() {
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("VIBES.score", Subject.repository(), 1, READ));
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("SIZE", Subject.repository(), 1, READ));
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("SIZE.", Subject.repository(), 1, READ));
        assertEquals(Metric.NAVIGABILITY, Metric.ofKind("NAVIGABILITY.checklist"));
        assertEquals("a fact's kind is METRIC.what, not .lines",
                assertThrows(IllegalArgumentException.class, () -> Metric.ofKind(".lines")).getMessage());
        assertEquals("a fact's kind names no metric: VIBES.score",
                assertThrows(IllegalArgumentException.class, () -> Metric.ofKind("VIBES.score")).getMessage());
    }

    @Test
    void aValueIsANumberATextOrAYesNo() {
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("SIZE.source_lines", Subject.repository(), List.of(1), READ));
        assertThrows(IllegalArgumentException.class, () -> Fact.measured("SIZE.source_lines", Subject.repository(), null, READ));
        Fact.measured("QUALITY.lint", Subject.file("a.py"), true, READ);
        Fact.measured("QUALITY.agent_review", Subject.file("a.py"), "clear", READ);
    }

    @Test
    void aConfidenceIsBetweenZeroAndOne() {
        assertThrows(IllegalArgumentException.class, () -> new Fact("SIZE.x", Subject.repository(), 1, READ, 1.01, State.INFERRED));
        assertThrows(IllegalArgumentException.class, () -> new Fact("SIZE.x", Subject.repository(), 1, READ, -0.01, State.INFERRED));
        assertThrows(IllegalArgumentException.class, () -> new Fact("SIZE.x", Subject.repository(), 1, READ, Double.NaN, State.INFERRED));
        assertEquals(0.0, new Fact("SIZE.x", Subject.repository(), 1, READ, 0.0, State.INFERRED).confidence());
    }

    @Test
    void aJudgmentIsNeverMoreConfidentThanItsPromptsCalibration() {
        Calibration calibration = new Calibration("abc123", 0.82, 30);
        Evidence prompt = new Evidence("prompt abc123", List.of("README.md", "src/a.py"));
        assertEquals(0.82, Fact.judged("AMBITION.class", Subject.repository(), "product", prompt, 0.95, calibration).confidence());
        assertEquals(0.6, Fact.judged("AMBITION.class", Subject.repository(), "product", prompt, 0.6, calibration).confidence());
        assertThrows(IllegalArgumentException.class, () -> Fact.judged("AMBITION.class", Subject.repository(), "product", prompt, 0.6, null));
        Evidence other = new Evidence("prompt fff999", List.of("README.md"));
        assertThrows(IllegalArgumentException.class, () -> Fact.judged("AMBITION.class", Subject.repository(), "product", other, 0.6, calibration));
    }

    @Test
    void aFactIsKnownByItsKindAndSubjectNotItsValue() {
        Fact first = Fact.measured("SIZE.source_lines", Subject.repository(), 12, READ);
        Fact again = Fact.measured("SIZE.source_lines", Subject.repository(), 13, new Evidence("ingest 2", List.of("src/b.py")));
        assertEquals(first.key(), again.key());
        assertEquals("SIZE.source_lines repository:.", first.key());
        assertFalse(first.key().equals(Fact.measured("SIZE.source_lines", Subject.file("a.py"), 12, READ).key()));
    }

    @Test
    void aRefutedFactNeverScores() {
        Fact fact = Fact.measured("SIZE.source_lines", Subject.repository(), 12, READ);
        assertTrue(fact.scores());
        assertTrue(fact.withState(State.CONFIRMED).scores());
        assertFalse(fact.withState(State.REFUTED).scores());
        assertEquals(fact.key(), fact.withState(State.REFUTED).key());
    }

    @Test
    void aCalibrationIsMeasured() {
        assertThrows(IllegalArgumentException.class, () -> new Calibration("", 0.8, 30));
        assertThrows(IllegalArgumentException.class, () -> new Calibration("abc", 1.2, 30));
        assertThrows(IllegalArgumentException.class, () -> new Calibration("abc", 0.8, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration("abc", -0.01, 30));
        assertEquals(0.0, new Calibration("abc", 0.0, 1).precision());
        assertEquals(1.0, new Calibration("abc", 1.0, 1).precision());
    }

    @Test
    void aSubjectHasAKindAndAnId() {
        assertThrows(IllegalArgumentException.class, () -> new Subject(Subject.Kind.FILE, " "));
        assertThrows(IllegalArgumentException.class, () -> new Subject(null, "a"));
        assertEquals("file:src/a.py", Subject.file("src/a.py").toString());
    }
}
