import java.util.ArrayList;
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
                .accepts("a metric reported, not held", () -> check(agent, with(m, "Docs lines per source line", "9.99 (1 / 1)")));
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
