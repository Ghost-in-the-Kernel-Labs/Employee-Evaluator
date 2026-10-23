package evaluator.score;

import evaluator.rubric.Fact;
import evaluator.rubric.Score;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintainability, the tools' half: E-051's six parts, each 0–1 with 1 good, weighed (a part whose facts are absent is left
 * out and the others' weights scaled to fill; none at all, no score), and that weighted score scaled to the baseline's (E-075).
 */
// Decision: E-075
final class Maintainability {
    /** The parts and their weights, summing to 1. */
    static final Map<String, Double> WEIGHTS = Map.of("duplication", 0.25, "tests", 0.25, "cycles", 0.15, "functions", 0.15, "coupling", 0.10, "unused", 0.10);
    /** The share of duplicated code that reads 0. */
    static final double FULL_DUPLICATION = 0.10;
    /** A function over this cognitive complexity counts against (SonarSource's usual limit). */
    static final int COGNITIVE_LIMIT = 15;
    /** A file importing more than this many of the repository's files counts against. */
    static final int FAN_OUT_LIMIT = 10;

    private Maintainability() {}

    /**
     * The score: 100 × the weighted score of the parts whose facts are there ÷ Suite Builder's at the baseline commit, rounded, no
     * ceiling (E-071), with every fact it read behind it. The baseline is the six-part score whatever parts this repository has.
     */
    static Score score(List<Fact> facts) {
        List<String> keys = new ArrayList<>();
        Double weighted = weighted(facts, keys);
        if (weighted == null) return null;
        return new Score((int) Math.round(100 * weighted / Formulas.BASELINE_MAINTAINABILITY), keys);
    }

    /** E-051's weighted score, 0–1: the parts present weighed and the weights scaled to fill; null with no part. */
    static Double weighted(List<Fact> facts, List<String> keys) {
        Map<String, Double> parts = parts(facts, keys);
        if (parts.isEmpty()) return null;
        double weight = 0, sum = 0;
        for (var p : parts.entrySet()) {
            weight += WEIGHTS.get(p.getKey());
            sum += WEIGHTS.get(p.getKey()) * p.getValue();
        }
        return sum / weight;
    }

    /** Each part present, 0–1; the keys of the facts read are added to `keys`. */
    static Map<String, Double> parts(List<Fact> facts, List<String> keys) {
        Map<String, Double> parts = new LinkedHashMap<>();
        Fact duplication = read(facts, "MAINTAINABILITY.duplication", keys), tests = read(facts, "MAINTAINABILITY.test_to_code_ratio", keys);
        if (duplication != null) parts.put("duplication", 1 - Math.min(number(duplication) / FULL_DUPLICATION, 1));
        if (tests != null) parts.put("tests", Math.min(number(tests), 1));
        List<Fact> fanOut = all(facts, "MAINTAINABILITY.fan_out", keys);
        Fact cycles = read(facts, "MAINTAINABILITY.import_cycle_files", keys);
        if (cycles != null && !fanOut.isEmpty()) parts.put("cycles", 1 - Math.min(number(cycles) / fanOut.size(), 1));
        if (!fanOut.isEmpty()) parts.put("coupling", shareAtMost(fanOut, FAN_OUT_LIMIT));
        List<Fact> functions = all(facts, "COMPLEXITY.cognitive", keys);
        if (!functions.isEmpty()) parts.put("functions", shareAtMost(functions, COGNITIVE_LIMIT));
        List<Fact> unused = all(facts, "MAINTAINABILITY.unused_imports", keys);
        if (!unused.isEmpty()) parts.put("unused", 1 - Math.min(unused.stream().mapToDouble(Maintainability::number).sum() / unused.size(), 1));
        return parts;
    }

    /** The share of these facts whose value is at most the limit. */
    private static double shareAtMost(List<Fact> facts, int limit) {
        return (double) facts.stream().filter(f -> number(f) <= limit).count() / facts.size();
    }

    private static Fact read(List<Fact> facts, String kind, List<String> keys) {
        Fact f = Formulas.find(facts, kind);
        if (f != null) keys.add(f.key());
        return f;
    }

    private static List<Fact> all(List<Fact> facts, String kind, List<String> keys) {
        List<Fact> found = facts.stream().filter(f -> f.kind().equals(kind)).toList();
        found.forEach(f -> keys.add(f.key()));
        return found;
    }

    private static double number(Fact f) {
        return ((Number) f.value()).doubleValue();
    }
}
