package evaluator.history;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A repository's commits as git records them: each one's hash, the time it claims to have been written,
 * its message, and the lines it added and removed per path. No author's name or address is read: who
 * wrote a commit is the GitHub adapter's to establish (TODO.md 1.7), and identity stays out of this
 * module (E-014).
 */
public final class Commits {
    /** The most commits read, newest first; a longer history is read to here. */
    static final int MAX_COMMITS = 20_000;

    /** One path's lines added and removed in a commit (0 for a binary file). */
    public record Change(String path, int added, int deleted) {}

    /** One commit: its hash, the time its author claims, its message, its changes (none for a merge). */
    public record Commit(String hash, Instant at, String message, List<Change> changes) {
        public Commit {
            changes = List.copyOf(changes);
        }

        /** Lines added and removed, together. */
        public int size() {
            return changes.stream().mapToInt(c -> c.added() + c.deleted()).sum();
        }
    }

    private Commits() {}

    // Unsure: a commit message holding the separator bytes (\u0001, \u0002) could split one commit in two; only the submitter's own score is touched.
    /** The repository's commits, oldest first. */
    public static List<Commit> read(Path repo) throws IOException {
        String log = Git.run(repo, List.of("log", "--max-count=" + MAX_COMMITS, "--no-renames", "--no-ext-diff", "--no-textconv",
                "--no-color", "--numstat", "--format=%x01%H%x1f%at%x1f%B%x02", "HEAD"));
        List<Commit> commits = parse(log);
        java.util.Collections.reverse(commits);
        return commits;
    }

    /** The paths in the repository at HEAD. */
    public static List<String> paths(Path repo) throws IOException {
        return Git.run(repo, List.of("ls-tree", "-r", "--name-only", "-z", "HEAD")).lines()
                .flatMap(line -> java.util.Arrays.stream(line.split("\u0000"))).filter(p -> !p.isEmpty()).toList();
    }

    /** `git log`'s output, newest first: a header (hash, time, message) closed by \u0002, then its numstat lines. */
    static List<Commit> parse(String log) {
        List<Commit> commits = new ArrayList<>();
        for (String record : log.split("\u0001")) {
            int close = record.indexOf('\u0002');
            if (close < 0) continue;
            String[] head = record.substring(0, close).split("\u001f", 3);
            if (head.length < 3) continue;
            List<Change> changes = new ArrayList<>();
            for (String line : record.substring(close + 1).split("\n")) {
                String[] cols = line.split("\t", 3);
                if (cols.length == 3) changes.add(new Change(cols[2], count(cols[0]), count(cols[1])));
            }
            commits.add(new Commit(head[0], Instant.ofEpochSecond(Long.parseLong(head[1].strip())), head[2].strip(), changes));
        }
        return commits;
    }

    private static int count(String numstat) {
        return numstat.equals("-") ? 0 : Integer.parseInt(numstat);
    }
}
