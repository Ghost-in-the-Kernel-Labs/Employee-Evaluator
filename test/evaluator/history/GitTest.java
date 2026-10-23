package evaluator.history;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GitTest {
    /** Our own GIT_ variables are dropped (a GIT_DIR would point git elsewhere); the rest, PATH included, stays; git's settings are put. */
    @Test
    void scrubDropsOurGitVariablesAndSetsGits() {
        Map<String, String> env = new HashMap<>(Map.of("GIT_DIR", "/evil", "GIT_SSH_COMMAND", "evil", "PATH", "/bin", "HOME", "/home/x", "LANG", "de"));
        Git.scrub(env);
        assertEquals(Map.of("PATH", "/bin", "HOME", "/nonexistent", "LANG", "de", "LC_ALL", "C", "GIT_CONFIG_NOSYSTEM", "1",
                "GIT_CONFIG_GLOBAL", "/dev/null", "GIT_TERMINAL_PROMPT", "0", "GIT_OPTIONAL_LOCKS", "0"), env);
    }

    /** Exactly MAX_OUTPUT bytes are read whole; one more and the output is refused (null). */
    @Test
    void theCapAllowsExactlyMaxOutput() {
        byte[] exact = new byte[Git.MAX_OUTPUT];
        assertArrayEquals(exact, Git.readCapped(new ByteArrayInputStream(exact)));
        assertNull(Git.readCapped(new ByteArrayInputStream(new byte[Git.MAX_OUTPUT + 1])));
        assertEquals(0, Git.readCapped(new ByteArrayInputStream(new byte[0])).length);
    }

    /** A thread interrupted while git runs gets an IOException and stays interrupted, for whoever called it. */
    @Test
    void anInterruptedRunFailsAndKeepsTheInterrupt() {
        Thread.currentThread().interrupt();
        try {
            IOException e = assertThrows(IOException.class, () -> Git.run(Path.of("."), List.of("--version")));
            assertEquals("interrupted", e.getMessage());
            assertTrue(Thread.currentThread().isInterrupted(), "the interrupt is passed on");
        } finally {
            assertTrue(Thread.interrupted());
        }
        assertFalse(Thread.currentThread().isInterrupted());
    }
}
