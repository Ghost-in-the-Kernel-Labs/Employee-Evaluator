import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The tithe gate (E-015, TODO.md 1.11): a push is refused once more than sixteen commits have passed
 * since the last whose subject starts "Tithe". `gradle metrics` warns past eight; the gate is the hard
 * stop at twice that, after the cadence slipped twice when only habit held it (J-045, J-051).
 */
// Decision: E-015
final class TitheGate {
    static final int WARN = 8;
    static final int LIMIT = 16;

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> failures = check(subjects());
        failures.forEach(f -> System.out.println("tithe gate: " + f));
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    /** The last 200 commit subjects, newest first. */
    static List<String> subjects() throws IOException, InterruptedException {
        Process git = new ProcessBuilder("git", "log", "--format=%s", "-n", "200").directory(Repo.ROOT.toFile()).redirectErrorStream(true).start();
        List<String> subjects = new String(git.getInputStream().readAllBytes()).lines().toList();
        if (git.waitFor() != 0) throw new IOException("no git history here");
        return subjects;
    }

    /** Commits since the last tithe, newest first; all of them when there is none. */
    static int since(List<String> subjects) {
        int since = 0;
        while (since < subjects.size() && !subjects.get(since).startsWith("Tithe")) since++;
        return since;
    }

    static List<String> check(List<String> subjects) {
        int since = since(subjects);
        List<String> failures = new ArrayList<>();
        if (since > LIMIT) failures.add(since + " commits since the last tithe, over " + LIMIT + ": make the next commit a tithe (docs/AGENT.md) before pushing");
        return failures;
    }
}
