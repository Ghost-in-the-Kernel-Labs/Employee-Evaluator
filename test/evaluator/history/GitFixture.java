package evaluator.history;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A git repository with a history written to order, in one `git fast-import`: each commit's time,
 * message and files as given, so a test knows every number its history should give. Our own fixtures,
 * built in a temporary folder, never committed (a repository cannot hold another's .git).
 */
final class GitFixture {
    private final StringBuilder stream = new StringBuilder();
    private final Map<String, String> files = new LinkedHashMap<>();
    private int mark;

    /** One commit at a time, with a message, writing each named file's whole new text. */
    GitFixture commit(Instant at, String message, Map<String, String> writes) {
        mark++;
        String who = "Fixture <fixture@example.invalid> " + at.getEpochSecond() + " +0000";
        stream.append("commit refs/heads/main\nmark :").append(mark).append('\n')
                .append("author ").append(who).append('\n').append("committer ").append(who).append('\n');
        data(message);
        if (mark > 1) stream.append("from :").append(mark - 1).append('\n');
        writes.forEach((path, text) -> {
            files.put(path, text);
            stream.append("M 100644 inline ").append(path).append('\n');
            data(text);
        });
        return this;
    }

    /** The text of a file as the history left it. */
    String file(String path) {
        return files.getOrDefault(path, "");
    }

    /** The repository, written at dir with main checked out. */
    Path write(Path dir) throws IOException, InterruptedException {
        run(dir, List.of("git", "init", "-q", "-b", "main", dir.toString()), null);
        run(dir, List.of("git", "-C", dir.toString(), "fast-import", "--quiet"), stream.toString());
        run(dir, List.of("git", "-C", dir.toString(), "checkout", "-q", "main"), null);
        return dir;
    }

    /** n lines of text, each different, so git counts each one. */
    static String lines(String prefix, int n) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < n; i++) text.append(prefix).append(' ').append(i).append('\n');
        return text.toString();
    }

    private void data(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        stream.append("data ").append(bytes.length).append('\n').append(text).append('\n');
    }

    private static void run(Path dir, List<String> command, String input) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
        builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
        Process git = builder.start();
        if (input != null) {
            try (OutputStream in = git.getOutputStream()) {
                in.write(input.getBytes(StandardCharsets.UTF_8));
            }
        }
        String out = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (git.waitFor() != 0) throw new IOException(String.join(" ", command) + ": " + out);
    }
}
