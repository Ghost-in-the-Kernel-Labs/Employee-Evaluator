package evaluator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class MainTest {
    @Test
    void noArgumentsPrintsUsageAndRefuses() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(1, Main.run(List.of(), new PrintStream(new ByteArrayOutputStream()), new PrintStream(err)));
        assertTrue(err.toString().startsWith("usage: evaluator score"));
    }

    @Test
    void scoreIsNotBuiltYet() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(2, Main.run(List.of("score", "/tmp/repo"), new PrintStream(new ByteArrayOutputStream()), new PrintStream(err)));
        assertTrue(err.toString().contains("not built yet"));
    }
}
