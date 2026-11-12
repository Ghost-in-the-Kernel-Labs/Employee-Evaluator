import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds docs/AGENT.md's metrics (E-015, TODO.md 1.11): every metric the table gives a target is measured by
 * `gradle metrics` and within it, and every one the table names is one the script measures. Two are held
 * elsewhere: the fast loop is timed at each tithe pass (`gradle metrics --args=--time`), since a test cannot
 * time the run it is part of; the tithe cadence is held at push (`scripts/prepush.sh`), not mid-work.
 */
final class MetricsTest {
    /** The table's rows a test does not hold, and what holds them instead. */
    static final Set<String> HELD_ELSEWHERE = Set.of("Fast loop (s)", "Commits since the last tithe");
    private static final Pattern TARGET = Pattern.compile("^(<|≤)?\\s*([\\d,]+)$");
    private static final Pattern LEADING = Pattern.compile("^(\\d+)");

    public static void main(String[] args) {
        run().exit();
    }

    /** The table against the repository, and against copies broken one way each, which must be caught. */
    static Check run() {
        String agent = Check.read("docs/AGENT.md");
        Map<String, String> measured;
        try {
            measured = Metrics.measure(false);
        } catch (Exception e) {
            throw new IllegalStateException("gradle metrics could not measure: " + e.getMessage(), e);
        }
        Map<String, String> m = measured;
        return new Check("metrics")
                .repository(check(agent, m))
                .rejects("a metric over its target", () -> check(agent, with(m, "Largest source file (lines)", "401 (Big.java)")))
                .rejects("a metric at a strict target", () -> check(agent, with(m, "Mandatory context (words)", "1500 (CLAUDE.md)")))
                .rejects("a metric the script does not measure", () -> check(agent.replace("| Functions over 20 paths |", "| Functions over 21 paths |"), m))
                .rejects("a target nobody can read", () -> check(agent.replace("| Stale generated artifacts | 0 |", "| Stale generated artifacts | few |"), m))
                .accepts("a metric reported, not held", () -> check(agent, with(m, "Docs lines per source line", "9.99 (1 / 1)")))
                .rejects("seventeen commits since the last tithe", () -> TitheGate.check(history(17)))
                .rejects("seventeen commits and never a tithe", () -> TitheGate.check(history(17).subList(0, 17)))
                .accepts("sixteen commits since the last tithe", () -> TitheGate.check(history(16)))
                .accepts("a young history with no tithe yet", () -> TitheGate.check(history(5).subList(0, 5)))
                .rejects("twenty-one commits since the last navigation review", () -> TitheGate.check(reviewed(21)))
                .accepts("twenty commits since the last navigation review", () -> TitheGate.check(reviewed(20)))
                .accepts("a routing row's working set: each file it names once, a command or a missing path nothing", MetricsTest::workingSet)
                .rejects("a working set that counts a file twice", () -> sameTokens(2));
    }

    /** A row naming CLAUDE.md twice, a command and a path that is no file costs CLAUDE.md's tokens once (token posture step 2, E-093). */
    private static List<String> workingSet() {
        return sameTokens(1);
    }

    /** The row's measured tokens against `times` CLAUDE.md's, at four characters a token. */
    private static List<String> sameTokens(int times) {
        String row = "| Task | Instructions |\n| --- | --- |\n| A task | `CLAUDE.md`, `gradle test`, `CLAUDE.md`, `no/such/file.md` |\n";
        int expected = times * ((Check.read("CLAUDE.md").length() + Metrics.CHARS_PER_TOKEN - 1) / Metrics.CHARS_PER_TOKEN);
        int measured = Metrics.workingSets(row).getOrDefault("A task", -1);
        return measured == expected ? List.of() : List.of("A task: " + measured + " tokens, not " + expected);
    }

    /** Subjects newest first: a tithe, n - 1 ordinary commits, then a navigation review, so only the review's count varies. */
    private static List<String> reviewed(int n) {
        List<String> subjects = new ArrayList<>(List.of("Tithe: the metrics"));
        subjects.addAll(Collections.nCopies(n - 1, "Add a thing"));
        subjects.add("Navigation review: the notes");
        return subjects;
    }

    /** Subjects newest first: n ordinary commits, then a tithe, then one more. */
    private static List<String> history(int n) {
        List<String> subjects = new ArrayList<>(Collections.nCopies(n, "Add a thing"));
        subjects.add("Tithe: the metrics");
        subjects.add("Start");
        return subjects;
    }

    /** What the measured values break of the table's targets. */
    static List<String> check(String agent, Map<String, String> measured) {
        List<String> failures = new ArrayList<>();
        for (String[] row : rows(agent)) {
            String name = row[0], target = row[1];
            if (!measured.containsKey(name)) {
                failures.add("docs/AGENT.md names \"" + name + "\", which gradle metrics does not measure");
                continue;
            }
            if (HELD_ELSEWHERE.contains(name) || target.startsWith("reported")) continue;
            Matcher t = TARGET.matcher(target), v = LEADING.matcher(measured.get(name));
            if (!t.matches()) failures.add(name + ": the target \"" + target + "\" is not a number with < or ≤");
            else if (!v.find()) failures.add(name + ": measured \"" + measured.get(name) + "\", not a number");
            else if (!within(Integer.parseInt(v.group(1)), t.group(1), Integer.parseInt(t.group(2).replace(",", "")))) {
                failures.add(name + " is " + measured.get(name) + ", past its target " + target + ": bring it back, or a decision in theGhost.md moves the target");
            }
        }
        return failures;
    }

    private static boolean within(int value, String comparison, int limit) {
        return "<".equals(comparison) ? value < limit : value <= limit;
    }

    /** The metrics table's rows: name and phase-1 target. */
    static List<String[]> rows(String agent) {
        List<String[]> rows = new ArrayList<>();
        String table = agent.substring(agent.indexOf("## The metrics"));
        for (String line : table.split("\n")) {
            if (!line.startsWith("| ") || line.startsWith("| Metric") || line.startsWith("| ---")) continue;
            String[] cells = line.split("\\|");
            rows.add(new String[] {cells[1].trim(), cells[2].trim()});
        }
        return rows;
    }

    private static Map<String, String> with(Map<String, String> measured, String name, String value) {
        Map<String, String> copy = new LinkedHashMap<>(measured);
        copy.put(name, value);
        return copy;
    }
}
