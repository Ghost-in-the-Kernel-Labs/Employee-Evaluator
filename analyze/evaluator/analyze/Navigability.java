package evaluator.analyze;

import evaluator.ingest.Inventory;
import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The navigability checklist, the phase-1 stand-in for AI navigability (docs/SCORE-CARD.md, E-044): how
 * easily an agent with little context finds its way. Instructions for agents present; the paths they name
 * still there; modules that do not import each other round in a cycle; names in one style per language.
 * Each a fact, and together `NAVIGABILITY.checklist`, 0–100. Retired by the navigability task (TODO.md 2.5).
 */
// Decision: E-044
public final class Navigability {
    /** The tool every checklist fact names as its evidence. */
    public static final String TOOL = "evaluator analyze navigability 1 (checklist)";
    /** The checklist's parts and their weights, summing to 1. */
    static final Map<String, Double> WEIGHTS = Map.of("agent_docs", 0.25, "freshness", 0.35, "boundaries", 0.20, "naming", 0.20);
    /** Folders that hold modules rather than being one: looked through to the module inside. */
    static final Set<String> CONTAINERS = Set.of("src", "lib", "app", "apps", "packages", "source", "main", "java", "python", "kotlin", "scala", "internal", "pkg");

    private Navigability() {}

    /** The checklist's facts for the repository at root, its inventory, measures and graph given. */
    public static List<Fact> facts(Path root, Inventory inventory, Complexity.Report report, Dependencies.Graph graph) throws IOException {
        List<String> docs = DocPaths.found(root);
        List<String> named = DocPaths.named(root, docs);
        List<String> missing = DocPaths.missing(root, named);
        List<String> cyclic = cyclicModules(graph);
        int modules = modules(inventory).size();
        double naming = Naming.consistency(inventory, report);
        Map<String, Double> parts = new TreeMap<>();
        parts.put("agent_docs", docs.isEmpty() ? 0.0 : 1.0);
        parts.put("freshness", named.isEmpty() ? 0.0 : (double) (named.size() - missing.size()) / named.size());
        parts.put("boundaries", modules == 0 ? 0.0 : 1.0 - (double) cyclic.size() / modules);
        parts.put("naming", naming);
        double score = 0;
        for (var p : parts.entrySet()) score += WEIGHTS.get(p.getKey()) * p.getValue();
        List<Fact> facts = new ArrayList<>();
        facts.add(fact("agent_docs", docs.size(), docs.isEmpty() ? List.of("(none of " + String.join(", ", DocPaths.AGENT_DOCS) + ")") : docs));
        facts.add(fact("doc_paths_named", named.size(), named.isEmpty() ? List.of("(none)") : named));
        facts.add(fact("doc_paths_missing", missing.size(), missing.isEmpty() ? List.of("(none)") : missing));
        facts.add(fact("module_cycles", cyclic.size(), cyclic.isEmpty() ? List.of("(none of " + modules + " modules)") : cyclic));
        facts.add(fact("naming_consistency", round(naming), List.of(Naming.summary(inventory, report))));
        List<String> partList = new ArrayList<>();
        parts.forEach((name, value) -> partList.add(name + " " + round(value)));
        facts.add(Fact.measured("NAVIGABILITY.checklist", Subject.repository(), (int) Math.round(100 * score), new Evidence(TOOL, partList)));
        return facts;
    }

    /** A file's module: its first folder, containers looked through; "." for a file at the root. */
    static String module(String path) {
        String[] parts = path.split("/");
        int i = 0;
        while (i < parts.length - 1 && CONTAINERS.contains(parts[i])) i++;
        if (i >= parts.length - 1) return i == 0 ? "." : String.join("/", java.util.Arrays.copyOf(parts, i));
        return String.join("/", java.util.Arrays.copyOf(parts, i + 1));
    }

    /** The modules of the repository's source. */
    static Set<String> modules(Inventory inventory) {
        Set<String> modules = new TreeSet<>();
        for (Inventory.Source f : inventory.source()) modules.add(module(f.path()));
        return modules;
    }

    /** The modules caught in a cycle of imports between modules (a module importing itself is no cycle). */
    static List<String> cyclicModules(Dependencies.Graph graph) {
        Map<String, Set<String>> between = new TreeMap<>();
        graph.edges().forEach((from, to) -> {
            String a = module(from);
            between.computeIfAbsent(a, k -> new TreeSet<>());
            for (String t : to) {
                String b = module(t);
                between.computeIfAbsent(b, k -> new TreeSet<>());
                if (!a.equals(b)) between.get(a).add(b);
            }
        });
        Map<String, Integer> part = Dependencies.Tarjan.parts(between);
        Map<Integer, Integer> sizes = new HashMap<>();
        part.values().forEach(p -> sizes.merge(p, 1, Integer::sum));
        List<String> cyclic = new ArrayList<>();
        for (var e : new TreeMap<>(part).entrySet()) if (sizes.get(e.getValue()) > 1) cyclic.add(e.getKey());
        return cyclic;
    }

    private static Fact fact(String kind, Number value, List<String> sources) {
        return Fact.measured("NAVIGABILITY.checklist." + kind, Subject.repository(), value, new Evidence(TOOL, sources));
    }

    static double round(double value) {
        return Math.round(1000 * value) / 1000.0;
    }
}
