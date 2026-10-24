package evaluator.rubric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The calibration set's format (TODO.md 2.1, E-080): each refusal once, the real folder held to its fields, the 30-label check pending until 30. */
class CalibrationSetTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final String OK = "{\"id\":\"c901\",\"kind\":\"own-work\",\"ambition\":\"toy\",\"quality\":3}";

    private static String message(String json) {
        return assertThrows(IllegalArgumentException.class, () -> CalibrationSet.parse(json)).getMessage();
    }

    private static Label label(String id) {
        return new Label(id, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3);
    }

    @Test
    void aSoundLabelParses() {
        assertEquals(new Label("c901", Label.Kind.OWN_WORK, Label.Ambition.TOY, 3), CalibrationSet.parse(OK));
        assertEquals(Label.Kind.CLONE, CalibrationSet.parse(OK.replace("own-work", "clone")).kind());
        assertEquals(Label.Ambition.RESEARCH, CalibrationSet.parse(OK.replace("toy", "research")).ambition());
    }

    @Test
    void aMissingFieldIsRefused() {
        for (String field : List.of("id", "kind", "ambition", "quality")) {
            String without = OK.replaceAll("\"" + field + "\":(\"[^\"]*\"|\\d),?", "").replace(",}", "}");
            assertEquals("missing field " + field, message(without));
        }
    }

    @Test
    void anUnknownFieldIsRefused() {
        assertTrue(message(OK.replace("}", ",\"repo\":\"x\"}")).startsWith("unknown field repo"));
        assertTrue(message(OK.replace("}", ",\"domain\":\"game\"}")).startsWith("unknown field domain"));
        assertTrue(message(OK.replace("}", ",\"rationale\":\"x\"}")).startsWith("unknown field rationale"));
        assertEquals("a label is a JSON object", message("[]"));
    }

    @Test
    void aValueOutOfItsRangeIsRefused() {
        assertTrue(message(OK.replace("own-work", "stolen")).contains("kind stolen is not one of"));
        assertTrue(message(OK.replace("toy", "huge")).contains("ambition huge is not one of [toy, utility, product, research]"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":0")).contains("1–5, not 0"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":6")).contains("1–5, not 6"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":2.5")).contains("whole number"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":\"3\"")).contains("whole number"));
        assertEquals(1, CalibrationSet.parse(OK.replace("\"quality\":3", "\"quality\":1")).quality());
        assertEquals(5, CalibrationSet.parse(OK.replace("\"quality\":3", "\"quality\":5")).quality());
        assertEquals("field kind is text", message(OK.replace("\"own-work\"", "7")));
    }

    @Test
    void anIdThatCouldNameARepositoryIsRefused() {
        for (String id : List.of("my-project", "c1", "c1234", "C123", "")) {
            assertTrue(message(OK.replace("c901", id)).contains("opaque"), id);
        }
    }

    @Test
    void aLabelWithoutAKindOrAmbitionIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", null, Label.Ambition.TOY, 3));
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", Label.Kind.CLONE, null, 3));
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", Label.Kind.CLONE, Label.Ambition.TOY, 0));
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", Label.Kind.CLONE, Label.Ambition.TOY, 6));
        assertEquals(1, new Label("c901", Label.Kind.CLONE, Label.Ambition.TOY, 1).quality());
        assertEquals(5, new Label("c901", Label.Kind.CLONE, Label.Ambition.TOY, 5).quality());
    }

    @Test
    void aDuplicateIdIsRefused() {
        assertEquals("duplicate id c001", assertThrows(IllegalArgumentException.class,
                () -> CalibrationSet.of(List.of(label("c001"), label("c001")))).getMessage());
        assertEquals(2, CalibrationSet.of(List.of(label("c001"), label("c002"))).size());
    }

    @Test
    void theFolderIsReadByIdAndNamedByIt(@TempDir Path dir) throws IOException {
        assertEquals(List.of(), CalibrationSet.load(dir.resolve("absent")));
        Files.writeString(dir.resolve("README.md"), "not a label");
        Files.writeString(dir.resolve("c901.json"), OK);
        assertEquals(1, CalibrationSet.load(dir).size());
        Files.writeString(dir.resolve("c902.json"), OK);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CalibrationSet.load(dir)).getMessage().startsWith("c902.json: a label's file is named by its id"));
        Files.writeString(dir.resolve("c902.json"), OK.replace("toy", "huge"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CalibrationSet.load(dir)).getMessage().startsWith("c902.json: ambition huge"));
    }

    @Test
    void theSyntheticFixturesLoad() {
        List<Label> labels = CalibrationSet.load(ROOT.resolve("test/fixtures/calibration-synthetic"));
        assertEquals(3, labels.size());
        assertEquals(CalibrationSet.Status.State.PENDING, CalibrationSet.status(labels).state());
    }

    @Test
    void theSetIsPendingUntilThirtyThenMet() {
        assertEquals(new CalibrationSet.Status(CalibrationSet.Status.State.PENDING, "0 of 30 repositories labelled (waits on Ghost's labels)"), CalibrationSet.status(List.of()));
        assertEquals(new CalibrationSet.Status(CalibrationSet.Status.State.PENDING, "29 of 30 repositories labelled (waits on Ghost's labels)"), CalibrationSet.status(set(29)));
        assertEquals(new CalibrationSet.Status(CalibrationSet.Status.State.MET, "30 labelled"), CalibrationSet.status(set(30)));
        assertEquals(CalibrationSet.Status.State.MET, CalibrationSet.status(set(31)).state());
    }

    private static List<Label> set(int count) {
        List<Label> labels = new ArrayList<>();
        for (int i = 0; i < count; i++) labels.add(label("c%03d".formatted(i + 1)));
        return labels;
    }

    /** Every committed label in calibration/ is held to its fields; the count is reported, PENDING until Ghost has labelled 30, then enforced. */
    @Test
    void theRealSetHoldsItsLabels() {
        List<Label> labels = CalibrationSet.load(ROOT.resolve("calibration"));
        CalibrationSet.Status status = CalibrationSet.status(labels);
        System.out.println("calibration set " + status.state() + ": " + status.message());
        assertEquals(labels.size() >= CalibrationSet.TARGET ? CalibrationSet.Status.State.MET : CalibrationSet.Status.State.PENDING, status.state(), status.message());
    }
}
