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

/** The calibration set's format (TODO.md 2.1, E-079): each refusal once, the real folder held to its fields, the 30-and-five-domains check pending until 30. */
class CalibrationSetTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final String OK = "{\"id\":\"c901\",\"domain\":\"web-app\",\"kind\":\"own-work\",\"ambition\":\"toy\",\"quality\":3,\"rationale\":\"synthetic\"}";

    private static String message(String json) {
        return assertThrows(IllegalArgumentException.class, () -> CalibrationSet.parse(json)).getMessage();
    }

    private static Label label(String id, Label.Domain domain) {
        return new Label(id, domain, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, "");
    }

    @Test
    void aSoundLabelParses() {
        assertEquals(new Label("c901", Label.Domain.WEB_APP, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, "synthetic"), CalibrationSet.parse(OK));
        assertEquals("", CalibrationSet.parse(OK.replace(",\"rationale\":\"synthetic\"", "")).rationale());
        assertEquals(Label.Domain.MACHINE_LEARNING, CalibrationSet.parse(OK.replace("web-app", "machine-learning")).domain());
        assertEquals(Label.Domain.HARDWARE, CalibrationSet.parse(OK.replace("web-app", "hardware")).domain());
        assertEquals(Label.Domain.CRYPTO_DEFI, CalibrationSet.parse(OK.replace("web-app", "crypto-defi")).domain());
        assertEquals(Label.Domain.DEVELOPER_TOOLING, CalibrationSet.parse(OK.replace("web-app", "developer-tooling")).domain());
        assertEquals(Label.Domain.AGENT, CalibrationSet.parse(OK.replace("web-app", "agent")).domain());
        assertEquals(Label.Kind.CLONE, CalibrationSet.parse(OK.replace("own-work", "clone")).kind());
        assertEquals(Label.Ambition.RESEARCH, CalibrationSet.parse(OK.replace("toy", "research")).ambition());
    }

    @Test
    void aMissingFieldIsRefused() {
        for (String field : List.of("id", "domain", "kind", "ambition", "quality")) {
            String without = OK.replaceAll("\"" + field + "\":(\"[^\"]*\"|\\d),?", "").replace(",}", "}");
            assertEquals("missing field " + field, message(without));
        }
    }

    @Test
    void anUnknownFieldIsRefused() {
        assertTrue(message(OK.replace("}", ",\"repo\":\"x\"}")).startsWith("unknown field repo"));
        assertEquals("a label is a JSON object", message("[]"));
    }

    @Test
    void aValueOutOfItsRangeIsRefused() {
        assertTrue(message(OK.replace("web-app", "weather")).contains("domain weather is not one of [web-app, cli-tool,"));
        assertTrue(message(OK.replace("own-work", "stolen")).contains("kind stolen is not one of"));
        assertTrue(message(OK.replace("toy", "huge")).contains("ambition huge is not one of [toy, utility, product, research]"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":0")).contains("1–5, not 0"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":6")).contains("1–5, not 6"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":2.5")).contains("whole number"));
        assertTrue(message(OK.replace("\"quality\":3", "\"quality\":\"3\"")).contains("whole number"));
        assertEquals(1, CalibrationSet.parse(OK.replace("\"quality\":3", "\"quality\":1")).quality());
        assertEquals(5, CalibrationSet.parse(OK.replace("\"quality\":3", "\"quality\":5")).quality());
        assertEquals("field domain is text", message(OK.replace("\"web-app\"", "7")));
        assertEquals("field rationale is text", message(OK.replace("\"synthetic\"", "7")));
    }

    @Test
    void anIdThatCouldNameARepositoryIsRefused() {
        for (String id : List.of("my-project", "c1", "c1234", "C123", "")) {
            assertTrue(message(OK.replace("c901", id)).contains("opaque"), id);
        }
    }

    @Test
    void aUrlLikeOrNameLikeRationaleIsRefused() {
        for (String bad : List.of("see https://example.test/x", "at www.example.test", "mail a@b", "on github", "on GitLab", "example.com", "acme/widget", "by Jane Smith", "a Foo Bar thing")) {
            assertThrows(IllegalArgumentException.class, () -> new Label("c901", Label.Domain.GAME, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, bad), bad);
        }
        new Label("c901", Label.Domain.GAME, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, "a small game with a clean loop, tests at 70% and a 2.5 ratio");
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", Label.Domain.GAME, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, "x".repeat(281)));
        new Label("c901", Label.Domain.GAME, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, "x".repeat(280));
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", Label.Domain.GAME, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, "two\nlines"));
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", Label.Domain.GAME, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, null));
        assertThrows(IllegalArgumentException.class, () -> new Label("c901", null, Label.Kind.OWN_WORK, Label.Ambition.TOY, 3, ""));
    }

    @Test
    void aDuplicateIdIsRefused() {
        assertEquals("duplicate id c001", assertThrows(IllegalArgumentException.class,
                () -> CalibrationSet.of(List.of(label("c001", Label.Domain.GAME), label("c001", Label.Domain.LIBRARY)))).getMessage());
        assertEquals(2, CalibrationSet.of(List.of(label("c001", Label.Domain.GAME), label("c002", Label.Domain.GAME))).size());
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
    void theSetIsPendingUntilThirtyThenEnforced() {
        Label.Domain[] domains = Label.Domain.values();
        assertEquals(new CalibrationSet.Status(CalibrationSet.Status.State.PENDING, "0 of 30 repositories labelled (waits on Ghost's labels)"), CalibrationSet.status(List.of()));
        assertEquals(CalibrationSet.Status.State.PENDING, CalibrationSet.status(set(29, 1)).state());
        assertEquals(CalibrationSet.Status.State.FAILED, CalibrationSet.status(set(30, 4)).state());
        assertEquals("30 labelled but only 4 domains, 5 needed", CalibrationSet.status(set(30, 4)).message());
        assertEquals(new CalibrationSet.Status(CalibrationSet.Status.State.MET, "30 labelled across 5 domains"), CalibrationSet.status(set(30, 5)));
        assertEquals(CalibrationSet.Status.State.MET, CalibrationSet.status(set(31, 8)).state());
        assertEquals(CalibrationSet.Status.State.FAILED, CalibrationSet.status(set(35, 2)).state());
        assertTrue(domains.length >= CalibrationSet.MIN_DOMAINS);
    }

    private static List<Label> set(int count, int domainCount) {
        List<Label> labels = new ArrayList<>();
        for (int i = 0; i < count; i++) labels.add(label("c%03d".formatted(i + 1), Label.Domain.values()[i % domainCount]));
        return labels;
    }

    /** Every committed label in calibration/ is held to its fields; the count is reported, PENDING until Ghost has labelled 30, then enforced. */
    @Test
    void theRealSetHoldsItsLabels() {
        List<Label> labels = CalibrationSet.load(ROOT.resolve("calibration"));
        CalibrationSet.Status status = CalibrationSet.status(labels);
        System.out.println("calibration set " + status.state() + ": " + status.message());
        assertTrue(status.state() != CalibrationSet.Status.State.FAILED, status.message());
    }
}
