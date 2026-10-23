package evaluator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

/** E-077: a golden that matches shows nothing, one that differs shows exactly its changed lines, and showing writes nothing. */
class GoldensTest {
    @TempDir Path dir;

    private Path golden(String text) throws IOException {
        Path g = dir.resolve("g.json");
        Files.writeString(g, text);
        return g;
    }

    @Test
    void aGoldenThatMatchesShowsNothing() throws IOException {
        assertEquals("", Goldens.show(golden("a\nb\n"), "a\nb\n"));
        assertDoesNotThrow(() -> Goldens.check(Goldens.Mode.SHOW, golden("a\nb\n"), "a\nb\n", "hint"));
    }

    @Test
    void oneChangedLineIsShownAloneWithItsNumberAndColumn() {
        assertEquals("@@ golden line 2, new line 2 @@\n- \"size\": 80\n+ \"size\": 85\n  (the two part at column 10)",
                Goldens.diff("{\n\"size\": 80\n}\n", "{\n\"size\": 85\n}\n"));
    }

    @Test
    void lineSeparatedChangesAreSeparateHunksAndTheLinesBetweenAreNotShown() {
        String old = "a\nb\nc\nd\ne\nf\n";
        assertEquals("@@ golden line 2, new line 2 @@\n- b\n+ B\n  (the two part at column 1)\n@@ golden line 5, new line 5 @@\n- e\n+ E\n  (the two part at column 1)",
                Goldens.diff(old, "a\nB\nc\nd\nE\nf\n"));
    }

    @Test
    void anInsertedAndADeletedLineAreEachOnTheirOwnWithTheNeighboursKept() {
        assertEquals("@@ golden line 3, new line 3 @@\n+ new", Goldens.diff("a\nb\nc\n", "a\nb\nnew\nc\n"));
        assertEquals("@@ golden line 2, new line 2 @@\n- b", Goldens.diff("a\nb\nc\n", "a\nc\n"));
    }

    @Test
    void aMovedLineIsOneGoneAndOneAdded() {
        assertEquals("@@ golden line 1, new line 1 @@\n- a\n@@ golden line 4, new line 3 @@\n+ a", Goldens.diff("a\nb\nc\n", "b\nc\na\n"));
    }

    @Test
    void aMissingFinalNewlineIsShownAsTheEmptyLineItIs() {
        assertEquals("@@ golden line 2, new line 2 @@\n- ", Goldens.diff("a\n", "a"));
    }

    @Test
    void aVeryBigMiddleIsShownWholeChangedRatherThanMatched() {
        StringBuilder x = new StringBuilder();
        StringBuilder y = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            x.append("x").append(i).append('\n');
            y.append("y").append(i).append('\n');
        }
        String shown = Goldens.diff(x.toString(), y.toString());
        assertTrue(shown.startsWith("@@ golden line 1, new line 1 @@\n- x0\n- x1\n"));
        assertTrue(shown.endsWith("\n+ y2499"));
    }

    @Test
    void showingNamesTheChangeAndWritesNothing() throws IOException {
        Path g = golden("a\nb\n");
        AssertionFailedError e = assertThrows(AssertionFailedError.class, () -> Goldens.check(Goldens.Mode.SHOW, g, "a\nB\n", "the card changed"));
        assertTrue(e.getMessage().startsWith("the card changed\n"), e.getMessage());
        assertTrue(e.getMessage().contains("would change:\n@@ golden line 2, new line 2 @@\n- b\n+ B"), e.getMessage());
        assertEquals("a\nb\n", Files.readString(g));
    }

    @Test
    void aMissingGoldenIsSaidMissingAndNotCreatedWhenShowing() {
        Path g = dir.resolve("none.json");
        assertTrue(Goldens.show(g, "a\nb").endsWith("none.json is missing; it would be created (2 lines)"));
        assertThrows(AssertionFailedError.class, () -> Goldens.check(Goldens.Mode.SHOW, g, "a\nb", "hint"));
        assertFalse(Files.exists(g));
    }

    @Test
    void comparingFailsAsItAlwaysDidAndShowsNoDiff() throws IOException {
        Path g = golden("a\n");
        AssertionFailedError e = assertThrows(AssertionFailedError.class, () -> Goldens.check(Goldens.Mode.COMPARE, g, "b\n", "hint"));
        assertFalse(e.getMessage().contains("@@"), e.getMessage());
        assertDoesNotThrow(() -> Goldens.check(Goldens.Mode.COMPARE, g, "a\n", "hint"));
        assertThrows(NoSuchFileException.class, () -> {
            try {
                Goldens.check(Goldens.Mode.COMPARE, dir.resolve("none"), "a", "hint");
            } catch (java.io.UncheckedIOException u) {
                throw u.getCause();
            }
        });
    }

    @Test
    void writingWritesAndCreatesTheFolder() throws IOException {
        Path g = dir.resolve("sub/deeper/g.json");
        Goldens.check(Goldens.Mode.WRITE, g, "new\n", "hint");
        assertEquals("new\n", Files.readString(g));
    }

    @Test
    void theTwoPropertiesTogetherAreRefusedAndEachAloneIsItsMode() {
        assertThrows(IllegalStateException.class, () -> Goldens.mode(true, true));
        assertEquals(Goldens.Mode.WRITE, Goldens.mode(true, false));
        assertEquals(Goldens.Mode.SHOW, Goldens.mode(false, true));
        assertEquals(Goldens.Mode.COMPARE, Goldens.mode(false, false));
    }

    @Test
    void aBatchNamesEveryChangedGoldenAtOnceAndShowsEachWhenShowing() throws IOException {
        Path same = golden("a\n");
        Path other = dir.resolve("o.json");
        Files.writeString(other, "x\ny\n");
        Goldens.Batch compare = new Goldens.Batch(Goldens.Mode.COMPARE);
        compare.check(same, "a\n");
        compare.check(other, "x\nY\n");
        AssertionFailedError c = assertThrows(AssertionFailedError.class, () -> compare.assertUnchanged("hint"));
        assertTrue(c.getMessage().contains("o.json") && !c.getMessage().contains("g.json") && !c.getMessage().contains("@@"), c.getMessage());
        Goldens.Batch show = new Goldens.Batch(Goldens.Mode.SHOW);
        show.check(same, "a\n");
        show.check(other, "x\nY\n");
        AssertionFailedError s = assertThrows(AssertionFailedError.class, () -> show.assertUnchanged("hint"));
        assertTrue(s.getMessage().contains("- y\n+ Y"), s.getMessage());
        assertEquals("x\ny\n", Files.readString(other));
        Goldens.Batch clean = new Goldens.Batch(Goldens.Mode.SHOW);
        clean.check(same, "a\n");
        assertDoesNotThrow(() -> clean.assertUnchanged("hint"));
    }

    @Test
    void aBatchWritesWhenWriting() throws IOException {
        Goldens.Batch write = new Goldens.Batch(Goldens.Mode.WRITE);
        write.check(dir.resolve("n/new.md"), "t\n");
        assertDoesNotThrow(() -> write.assertUnchanged("hint"));
        assertEquals("t\n", Files.readString(dir.resolve("n/new.md")));
    }
}
