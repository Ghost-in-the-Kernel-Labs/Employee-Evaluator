package evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The one way a test holds a golden (E-018, E-077): compare it, write it (`-PwriteGoldens`), or show what would
 * change (`-PshowGoldens`) so a reviewer reads the change before writing it. Every test that has a golden calls
 * this; none keeps a copy of the property or of the diff. The two properties together are refused.
 */
public final class Goldens {
    /** What a run does with a golden that differs. */
    public enum Mode { COMPARE, SHOW, WRITE }

    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    /** Past this many line pairs the middle of a diff is not matched line by line: all of it is shown changed. */
    private static final long MATCH_LIMIT = 4_000_000L;

    private Goldens() {}

    /** The run's mode from `-PwriteGoldens` and `-PshowGoldens`; both at once is refused (E-077). */
    public static Mode mode() {
        return mode(Boolean.getBoolean("evaluator.writeGoldens"), Boolean.getBoolean("evaluator.showGoldens"));
    }

    // Decision: E-077
    static Mode mode(boolean write, boolean show) {
        if (write && show) throw new IllegalStateException("-PwriteGoldens and -PshowGoldens together: show first, read the diff, then write (E-077)");
        return write ? Mode.WRITE : show ? Mode.SHOW : Mode.COMPARE;
    }

    /** One golden: written in WRITE; in SHOW a difference fails with the changed lines, writing nothing; in COMPARE as ever. */
    public static void check(Path golden, String actual, String hint) {
        check(mode(), golden, actual, hint);
    }

    static void check(Mode mode, Path golden, String actual, String hint) {
        if (mode == Mode.WRITE) {
            write(golden, actual);
        } else if (mode == Mode.SHOW) {
            String report = show(golden, actual);
            if (!report.isEmpty()) fail(hint + "\n" + report);
        } else {
            assertEquals(read(golden), actual, hint);
        }
    }

    /** Several goldens in one test: every difference is named at once, and in SHOW each is also shown. */
    public static final class Batch {
        private final Mode mode;
        private final List<String> changed = new ArrayList<>();
        private final StringBuilder reports = new StringBuilder();

        public Batch() {
            this(mode());
        }

        Batch(Mode mode) {
            this.mode = mode;
        }

        public void check(Path golden, String actual) {
            if (mode == Mode.WRITE) {
                write(golden, actual);
                return;
            }
            String report = show(golden, actual);
            if (report.isEmpty()) return;
            changed.add(name(golden));
            if (mode == Mode.SHOW) reports.append(report).append('\n');
        }

        public void assertUnchanged(String hint) {
            assertEquals(List.of(), changed, hint + (reports.length() == 0 ? "" : "\n" + reports));
        }
    }

    /** The golden's path as the repository names it, for the report. */
    private static String name(Path golden) {
        Path abs = golden.toAbsolutePath().normalize();
        Path root = ROOT.toAbsolutePath().normalize();
        return abs.startsWith(root) ? root.relativize(abs).toString() : golden.toString();
    }

    private static String read(Path golden) {
        try {
            return Files.readString(golden);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path golden, String actual) {
        try {
            Files.createDirectories(golden.toAbsolutePath().getParent());
            Files.writeString(golden, actual);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** What writing would change in the golden, or the empty string when it would change nothing. Writes nothing. */
    static String show(Path golden, String actual) {
        if (!Files.exists(golden)) return name(golden) + " is missing; it would be created (" + lines(actual).size() + " lines)";
        String old = read(golden);
        if (old.equals(actual)) return "";
        return name(golden) + " would change:\n" + diff(old, actual);
    }

    private static List<String> lines(String text) {
        return List.of(text.split("\n", -1));
    }

    /**
     * The changed lines only, in hunks: "@@ golden line N, new line M @@", then each golden line gone as "- text" and
     * each new line as "+ text". A line changed alone (one for one) is followed by the column where the two part.
     */
    static String diff(String old, String actual) {
        List<String> a = lines(old);
        List<String> b = lines(actual);
        int head = 0;
        while (head < a.size() && head < b.size() && a.get(head).equals(b.get(head))) head++;
        int tail = 0;
        while (tail < a.size() - head && tail < b.size() - head && a.get(a.size() - 1 - tail).equals(b.get(b.size() - 1 - tail))) tail++;
        List<String> midA = a.subList(head, a.size() - tail);
        List<String> midB = b.subList(head, b.size() - tail);
        StringBuilder out = new StringBuilder();
        matchedHunks(midA, midB, head, out);
        return out.substring(0, out.length() - 1);
    }

    /** Walks the middle by its longest common subsequence, writing one hunk per run of lines not in common. */
    private static void matchedHunks(List<String> a, List<String> b, int offset, StringBuilder out) {
        int[][] same = commonLengths(a, b);
        int i = 0;
        int j = 0;
        while (i < a.size() || j < b.size()) {
            if (inCommon(a, b, same, i, j)) {
                i++;
                j++;
                continue;
            }
            int i0 = i;
            int j0 = j;
            while ((i < a.size() || j < b.size()) && !inCommon(a, b, same, i, j)) {
                if (j >= b.size() || (i < a.size() && same != null && same[i + 1][j] >= same[i][j + 1])) i++;
                else j++;
            }
            hunk(a.subList(i0, i), b.subList(j0, j), offset + i0 + 1, offset + j0 + 1, out);
        }
    }

    /** Whether a(i) and b(j) are the same line on a longest common subsequence (never, when the middle was too big to match). */
    private static boolean inCommon(List<String> a, List<String> b, int[][] same, int i, int j) {
        return same != null && i < a.size() && j < b.size() && a.get(i).equals(b.get(j)) && same[i][j] == same[i + 1][j + 1] + 1;
    }

    /** same[i][j] is the longest common run of lines in a from i and b from j; null when the middle is too big to match. */
    private static int[][] commonLengths(List<String> a, List<String> b) {
        if ((long) a.size() * b.size() > MATCH_LIMIT) return null;
        int[][] same = new int[a.size() + 1][b.size() + 1];
        for (int i = a.size() - 1; i >= 0; i--) {
            for (int j = b.size() - 1; j >= 0; j--) {
                same[i][j] = a.get(i).equals(b.get(j)) ? same[i + 1][j + 1] + 1 : Math.max(same[i + 1][j], same[i][j + 1]);
            }
        }
        return same;
    }

    private static void hunk(List<String> gone, List<String> added, int goldenLine, int newLine, StringBuilder out) {
        out.append("@@ golden line ").append(goldenLine).append(", new line ").append(newLine).append(" @@\n");
        for (String line : gone) out.append("- ").append(line).append('\n');
        for (String line : added) out.append("+ ").append(line).append('\n');
        if (gone.size() == 1 && added.size() == 1) out.append("  (the two part at column ").append(column(gone.get(0), added.get(0))).append(")\n");
    }

    private static int column(String x, String y) {
        int k = 0;
        while (k < x.length() && k < y.length() && x.charAt(k) == y.charAt(k)) k++;
        return k + 1;
    }
}
