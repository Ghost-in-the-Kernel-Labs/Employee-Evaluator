package evaluator.analyze;

import evaluator.ingest.Inventory;
import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Duplication by token shingling (E-036): the code inside functions, tests aside, as normalized tokens
 * (`Tokens`); every run of WINDOW tokens fingerprinted; a run found in two places, or twice in one
 * function at least a window apart, marks its tokens duplicated. `MAINTAINABILITY.duplication` is the
 * share of tokens duplicated, citing each duplicated stretch by file and lines.
 */
// Decision: E-036
public final class Duplication {
    /** The tool every duplication fact names as its evidence. */
    public static final String TOOL = "evaluator analyze duplication 1 (60-token shingles)";
    /** Tokens in a shingle: about eight to ten lines of code, the size of copy worth a reader's notice. */
    static final int WINDOW = 60;

    /** One function's tokens, where it is, and which of its tokens are duplicated. */
    record Unit(String path, int line, List<String> tokens, List<Integer> lines, boolean[] duplicated) {}

    private Duplication() {}

    /** The functions of every file that is not a test, outermost only (a closure is inside its function already). */
    static List<Unit> units(Complexity.Report report) {
        List<Unit> units = new ArrayList<>();
        report.functions().forEach((path, measures) -> {
            if (TestFiles.isTest(path)) return;
            for (Measure m : measures) {
                if (measures.stream().anyMatch(m::within)) continue;
                Tokens.List2 t = Tokens.of(m.text(), path.endsWith(".py"));
                units.add(new Unit(path, m.line(), t.tokens(), t.lines(), new boolean[t.tokens().size()]));
            }
        });
        return units;
    }

    /**
     * Marks every token inside a window that occurs at more than one place. Windows are grouped by
     * fingerprint and checked token for token, so a fingerprint collision marks nothing; a group is
     * marked whole, so a run repeated a thousand times costs its length, not its square.
     */
    static void mark(List<Unit> units) {
        Map<String, Integer> ids = new HashMap<>();
        List<int[]> coded = new ArrayList<>();
        for (Unit u : units) coded.add(u.tokens().stream().mapToInt(t -> ids.computeIfAbsent(t, k -> ids.size())).toArray());
        Map<Long, List<int[]>> windows = new HashMap<>();
        for (int k = 0; k < units.size(); k++) {
            int[] code = coded.get(k);
            for (int at = 0; at + WINDOW <= code.length; at++) windows.computeIfAbsent(fingerprint(code, at), f -> new ArrayList<>()).add(new int[] {k, at});
        }
        for (List<int[]> places : windows.values()) {
            if (places.size() < 2) continue;
            for (List<int[]> same : sameTokens(places, coded)) if (apart(same)) for (int[] p : same) java.util.Arrays.fill(units.get(p[0]).duplicated(), p[1], p[1] + WINDOW, true);
        }
    }

    /** The share of tokens duplicated, to three places, citing each duplicated stretch; none with no function to read. */
    public static Optional<Fact> fact(Complexity.Report report) {
        List<Unit> units = units(report);
        mark(units);
        int total = 0, duplicated = 0;
        List<String> stretches = new ArrayList<>();
        for (Unit u : units) {
            total += u.tokens().size();
            for (int i = 0; i < u.duplicated().length; i++) {
                if (!u.duplicated()[i]) continue;
                int start = i;
                while (i < u.duplicated().length && u.duplicated()[i]) i++;
                duplicated += i - start;
                stretches.add(u.path() + ":" + (u.line() + u.lines().get(start) - 1) + "-" + (u.line() + u.lines().get(i - 1) - 1));
            }
        }
        if (total == 0) return Optional.empty();
        double share = Math.round(1000.0 * duplicated / total) / 1000.0;
        return Optional.of(Fact.measured("MAINTAINABILITY.duplication", Subject.repository(), share,
                new Evidence(TOOL, stretches.isEmpty() ? List.of("(no duplicated code)") : stretches)));
    }

    /** A polynomial fingerprint of WINDOW token ids from at. */
    private static long fingerprint(int[] code, int at) {
        long h = 0;
        for (int i = at; i < at + WINDOW; i++) h = h * 1_000_003L + code[i] + 1;
        return h;
    }

    /** The places split into groups whose tokens are truly equal (a fingerprint can collide). */
    private static List<List<int[]>> sameTokens(List<int[]> places, List<int[]> coded) {
        List<List<int[]>> groups = new ArrayList<>();
        for (int[] p : places) {
            List<int[]> home = null;
            for (List<int[]> g : groups) if (java.util.Arrays.equals(coded.get(p[0]), p[1], p[1] + WINDOW, coded.get(g.get(0)[0]), g.get(0)[1], g.get(0)[1] + WINDOW)) home = g;
            if (home == null) groups.add(home = new ArrayList<>());
            home.add(p);
        }
        return groups;
    }

    /** Whether a group is a copy: in two functions, or in one at least a window apart (not one run overlapping itself). */
    private static boolean apart(List<int[]> group) {
        if (group.size() < 2) return false;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] p : group) {
            if (p[0] != group.get(0)[0]) return true;
            min = Math.min(min, p[1]);
            max = Math.max(max, p[1]);
        }
        return max - min >= WINDOW;
    }
}
