package evaluator.analyze;

import evaluator.ingest.Inventory;
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
 * The repository's dependency graph: each file's imports resolved to the repository's own files by its
 * language's adapter (anything else is outside it), the longest chain of imports through the graph,
 * and each file's unused imports. Files that import each other in a cycle are one link of a chain.
 */
// Decision: E-032
public final class Dependencies {
    /** The tool the dependency graph's facts name as their evidence. */
    public static final String TOOL = "evaluator analyze dependencies 1";

    /** The graph's edges, file to the files it imports, both in path order; and one longest chain. */
    public record Graph(Map<String, Set<String>> edges, List<String> longest) {}

    private Dependencies() {}

    /** The graph of a measured repository. */
    public static Graph graph(Complexity.Report report, Inventory inventory) {
        Set<String> files = new TreeSet<>();
        Map<String, String> languages = new HashMap<>();
        for (Inventory.Source file : inventory.source()) {
            files.add(file.path());
            languages.put(file.path(), file.language());
        }
        Map<String, Set<String>> edges = new TreeMap<>();
        for (var file : report.imports().entrySet()) {
            Adapter adapter = Adapters.forLanguage(languages.get(file.getKey()));
            Set<String> to = new TreeSet<>();
            for (Import imported : file.getValue()) to.addAll(adapter.resolve(imported, file.getKey(), files));
            to.remove(file.getKey());
            edges.put(file.getKey(), to);
        }
        return new Graph(edges, longest(edges));
    }

    /** The repository's dependency depth (links in its longest chain), and each read file's unused imports. */
    public static List<Fact> facts(Graph graph, Complexity.Report report, String tool) {
        List<Fact> facts = new ArrayList<>();
        List<String> chain = graph.longest().isEmpty() ? List.of("(no file imports another)") : graph.longest();
        facts.add(Fact.measured("COMPLEXITY.dependency_depth", Subject.repository(), Math.max(0, graph.longest().size() - 1), new Evidence(tool, chain)));
        for (var file : report.imports().entrySet()) {
            List<String> where = new ArrayList<>();
            for (Import imported : file.getValue()) for (String name : imported.unused()) where.add(file.getKey() + ":" + imported.line() + " " + name);
            Evidence evidence = new Evidence(tool, where.isEmpty() ? List.of(file.getKey()) : where);
            facts.add(Fact.measured("MAINTAINABILITY.unused_imports", Subject.file(file.getKey()), where.size(), evidence));
        }
        return facts;
    }

    /**
     * One longest chain of imports, cycles collapsed: the strongly connected parts found (Tarjan, without
     * recursion, so a deep graph cannot exhaust the stack), then the longest path through them, a part
     * standing for its first file. Ties go to the chain starting at the first file in path order.
     */
    static List<String> longest(Map<String, Set<String>> edges) {
        Map<String, Integer> part = Tarjan.parts(edges);
        Map<Integer, Set<Integer>> next = new TreeMap<>();
        Map<Integer, String> first = new TreeMap<>();
        for (var e : edges.entrySet()) {
            int from = part.get(e.getKey());
            first.merge(from, e.getKey(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
            next.computeIfAbsent(from, k -> new TreeSet<>());
            for (String to : e.getValue()) {
                int target = part.get(to);
                first.merge(target, to, (a, b) -> a.compareTo(b) <= 0 ? a : b);
                next.computeIfAbsent(target, k -> new TreeSet<>());
                if (target != from) next.get(from).add(target);
            }
        }
        return chain(next, first);
    }

    /** The longest path through the parts (they form no cycle), as the first file of each; empty with no edge. */
    private static List<String> chain(Map<Integer, Set<Integer>> next, Map<Integer, String> first) {
        Map<Integer, Integer> length = new HashMap<>();
        Map<Integer, Integer> step = new HashMap<>();
        for (int p : Tarjan.order(next)) {
            int best = 0;
            for (int q : next.get(p)) {
                if (length.get(q) + 1 > best) {
                    best = length.get(q) + 1;
                    step.put(p, q);
                }
            }
            length.put(p, best);
        }
        Integer start = null;
        for (int p : next.keySet()) {
            if (start == null || length.get(p) > length.get(start) || (length.get(p).equals(length.get(start)) && first.get(p).compareTo(first.get(start)) < 0)) start = p;
        }
        if (start == null || length.get(start) == 0) return List.of();
        List<String> path = new ArrayList<>();
        for (Integer p = start; p != null; p = step.get(p)) path.add(first.get(p));
        return path;
    }

    /** The graph as JSON, the same bytes for the same repository: each file's edges, then the longest chain. */
    public static String toJson(Graph graph, Complexity.Report report) {
        StringBuilder json = new StringBuilder("{\n  \"imports\": [");
        boolean first = true;
        for (var file : report.imports().entrySet()) {
            List<String> unused = new ArrayList<>();
            for (Import imported : file.getValue()) unused.addAll(imported.unused());
            json.append(first ? "\n" : ",\n").append("    {\"path\": ").append(Inventory.quote(file.getKey()))
                    .append(", \"to\": ").append(list(graph.edges().get(file.getKey()))).append(", \"unused\": ").append(list(unused)).append('}');
            first = false;
        }
        return json.append(first ? "],\n" : "\n  ],\n").append("  \"longest\": ").append(list(graph.longest())).append("\n}\n").toString();
    }

    private static String list(java.util.Collection<String> items) {
        List<String> quoted = new ArrayList<>();
        for (String item : items) quoted.add(Inventory.quote(item));
        return "[" + String.join(", ", quoted) + "]";
    }
}
