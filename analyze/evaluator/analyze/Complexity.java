package evaluator.analyze;

import evaluator.ingest.Inventory;
import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Json;
import evaluator.rubric.Subject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The Complexity facts of a repository: each counted source file read by the adapter for its language,
 * each function's cyclomatic and cognitive complexity a fact about that function. A file no adapter
 * reads is not measured; a file its adapter cannot parse is listed with why, never guessed at.
 */
public final class Complexity {
    /** Every function measured and every import read, by file in path order, and the files an adapter could not read. */
    public record Report(Map<String, List<Measure>> functions, Map<String, List<Import>> imports, Map<String, String> unread) {
        public Report {
            functions = new TreeMap<>(functions);
            imports = new TreeMap<>(imports);
            unread = new TreeMap<>(unread);
        }

        /** The same report over only these files (a judge's own code, TODO.md 2.2j): every other file's functions, imports and unread entry dropped. */
        public Report restrictedTo(Set<String> paths) {
            return new Report(kept(functions, paths), kept(imports, paths), kept(unread, paths));
        }

        private static <V> Map<String, V> kept(Map<String, V> all, Set<String> paths) {
            Map<String, V> kept = new TreeMap<>(all);
            kept.keySet().retainAll(paths);
            return kept;
        }
    }

    private Complexity() {}

    /** The inventory's source files measured by their adapters; the files are read from root. */
    public static Report measure(Path root, Inventory inventory) {
        Map<String, List<Measure>> functions = new TreeMap<>();
        Map<String, List<Import>> imports = new TreeMap<>();
        Map<String, String> unread = new TreeMap<>();
        for (Inventory.Source file : inventory.source()) {
            Adapter adapter = Adapters.forLanguage(file.language());
            if (adapter == null) continue;
            Parsed parsed = adapter.parse(file.path(), read(root.resolve(file.path())));
            if (parsed.unread() != null) unread.put(file.path(), parsed.unread());
            else {
                functions.put(file.path(), parsed.functions());
                imports.put(file.path(), parsed.imports());
            }
        }
        return new Report(functions, imports, unread);
    }

    /** Two facts per function, its cyclomatic and its cognitive complexity, each citing the file and line. */
    public static List<Fact> facts(Report report, Inventory inventory) {
        List<Fact> facts = new ArrayList<>();
        for (Inventory.Source file : inventory.source()) {
            List<Measure> measures = report.functions().get(file.path());
            if (measures == null) continue;
            for (Measure m : measures) {
                Evidence where = new Evidence(Adapters.forLanguage(file.language()).tool(), List.of(file.path() + ":" + m.line()));
                Subject function = new Subject(Subject.Kind.FUNCTION, file.path() + ":" + m.function());
                facts.add(Fact.measured("COMPLEXITY.cyclomatic", function, m.cyclomatic(), where));
                facts.add(Fact.measured("COMPLEXITY.cognitive", function, m.cognitive(), where));
            }
        }
        return facts;
    }

    /** The report as JSON, the same bytes for the same repository: files in path order, functions in source order. */
    public static String toJson(Report report) {
        StringBuilder json = new StringBuilder("{\n  \"functions\": [");
        boolean first = true;
        for (var file : report.functions().entrySet()) {
            for (Measure m : file.getValue()) {
                json.append(first ? "\n" : ",\n").append("    {\"path\": ").append(Json.quote(file.getKey()))
                        .append(", \"function\": ").append(Json.quote(m.function())).append(", \"line\": ").append(m.line())
                        .append(", \"cyclomatic\": ").append(m.cyclomatic()).append(", \"cognitive\": ").append(m.cognitive()).append('}');
                first = false;
            }
        }
        json.append(first ? "],\n" : "\n  ],\n").append("  \"unread\": [");
        first = true;
        for (var file : report.unread().entrySet()) {
            json.append(first ? "\n" : ",\n").append("    {\"path\": ").append(Json.quote(file.getKey()))
                    .append(", \"why\": ").append(Json.quote(file.getValue())).append('}');
            first = false;
        }
        return json.append(first ? "]\n}\n" : "\n  ]\n}\n").toString();
    }

    private static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
