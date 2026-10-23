package evaluator.analyze;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Coupling from the dependency graph (E-032): each file's fan-in (the repository's files that import
 * it) and fan-out (the repository's files it imports), and the files caught in an import cycle, where
 * no one of them can change without the others.
 */
// Decision: E-035
public final class Coupling {
    /** The tool every coupling fact names as its evidence. */
    public static final String TOOL = "evaluator analyze coupling 1";

    private Coupling() {}

    /** Fan-in and fan-out per file, each citing the files at the edge's other end, and the files in cycles. */
    public static List<Fact> facts(Dependencies.Graph graph) {
        Map<String, Set<String>> importers = new TreeMap<>();
        graph.edges().forEach((from, to) -> {
            importers.computeIfAbsent(from, k -> new TreeSet<>());
            for (String target : to) importers.computeIfAbsent(target, k -> new TreeSet<>()).add(from);
        });
        List<Fact> facts = new ArrayList<>();
        for (String file : importers.keySet()) {
            Set<String> out = graph.edges().getOrDefault(file, Set.of());
            facts.add(edgeFact("MAINTAINABILITY.fan_in", file, importers.get(file)));
            facts.add(edgeFact("MAINTAINABILITY.fan_out", file, out));
        }
        List<String> cyclic = cyclic(graph.edges());
        facts.add(Fact.measured("MAINTAINABILITY.import_cycle_files", Subject.repository(), cyclic.size(),
                new Evidence(TOOL, cyclic.isEmpty() ? List.of("(no import cycle)") : cyclic)));
        return facts;
    }

    private static Fact edgeFact(String kind, String file, Set<String> others) {
        return Fact.measured(kind, Subject.file(file), others.size(), new Evidence(TOOL, others.isEmpty() ? List.of(file) : List.copyOf(others)));
    }

    /** The files in a strongly connected part of more than one file, or importing themselves, in path order. */
    static List<String> cyclic(Map<String, Set<String>> edges) {
        Map<String, Integer> part = Tarjan.parts(edges);
        Map<Integer, Integer> sizes = new HashMap<>();
        part.values().forEach(p -> sizes.merge(p, 1, Integer::sum));
        List<String> files = new ArrayList<>();
        for (var e : new TreeMap<>(part).entrySet()) if (sizes.get(e.getValue()) > 1) files.add(e.getKey());
        return files;
    }
}
