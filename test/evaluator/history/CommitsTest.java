package evaluator.history;

import static org.junit.jupiter.api.Assertions.assertEquals;

import evaluator.history.Commits.Change;
import evaluator.history.Commits.Commit;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommitsTest {
    /** A record's numstat may follow its closing \u0002 at once; the first stat line is not lost or mixed with the message's end. */
    @Test
    void parseReadsTheNumstatRightAfterTheClose() {
        String log = "\u0001h2\u001f200\u001fsecond 7\u00023\t4\tb.py\n-\t-\timg.png\n"
                + "\u0001h1\u001f100\u001ffirst\u0002\n\n1\t2\ta.py\n";
        assertEquals(List.of(
                new Commit("h2", Instant.ofEpochSecond(200), "second 7", List.of(new Change("b.py", 3, 4), new Change("img.png", 0, 0))),
                new Commit("h1", Instant.ofEpochSecond(100), "first", List.of(new Change("a.py", 1, 2)))), Commits.parse(log));
    }

    /** A record with no close, or a header short of its parts, is left out. */
    @Test
    void parseSkipsWhatIsNotARecord() {
        assertEquals(List.of(), Commits.parse("\u0001h\u001f1\u001fno close"));
        assertEquals(List.of(), Commits.parse("\u0001h\u001f1\u0002"));
        assertEquals(List.of(), Commits.parse("\u0002\u0001"));
    }

    /** `ls-tree -z` closes each path with NUL; a newline is part of a name, and an empty tree lists no path. */
    @Test
    void splitPathsSplitsOnNulOnly() {
        assertEquals(List.of("a\nb.py", "c.py"), Commits.splitPaths("a\nb.py\u0000c.py\u0000"));
        assertEquals(List.of(), Commits.splitPaths(""));
    }

    /** A file whose name holds a newline is one path at HEAD (it was read as two before). */
    @Test
    void aPathWithANewlineIsOnePath(@TempDir Path dir) throws Exception {
        Path repo = new GitFixture().commit(Instant.parse("2026-09-01T10:00:00Z"), "one",
                Map.of("\"src/a\\nb.py\"", "x = 1\n", "c.py", "y = 2\n")).write(dir);
        assertEquals(List.of("c.py", "src/a\nb.py"), Commits.paths(repo).stream().sorted().toList());
    }
}
