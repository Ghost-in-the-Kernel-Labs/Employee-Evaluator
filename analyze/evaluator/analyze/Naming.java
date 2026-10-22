package evaluator.analyze;

import evaluator.ingest.Inventory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Naming consistency (E-044): per language, the share of function names and of file names in the style
 * most of them use. A one-word lowercase name (`total`, `index`) fits snake_case, camelCase and kebab-case
 * alike, so it is never against the style; a constructor carries its class's name and is left out.
 */
final class Naming {
    private Naming() {}

    /** The style of a name: snake, camel, pascal, kebab, lower (one lowercase word), or other. */
    static String style(String name) {
        if (name.matches("[a-z][a-z0-9]*")) return "lower";
        if (name.matches("[a-z][a-z0-9]*(_[a-z0-9]+)+")) return "snake";
        if (name.matches("[a-z][a-z0-9]*(-[a-z0-9]+)+")) return "kebab";
        if (name.matches("[a-z][a-z0-9]*([A-Z][a-z0-9]*)+")) return "camel";
        if (name.matches("[A-Z][a-zA-Z0-9]*")) return "pascal";
        return "other";
    }

    /** The share of names in their language's most used style, lowercase words counting with it; 1 with no names. */
    static double consistency(Inventory inventory, Complexity.Report report) {
        int[] counts = counts(inventory, report);
        return counts[1] == 0 ? 1.0 : (double) counts[0] / counts[1];
    }

    /** What the share was made of, for the fact's evidence. */
    static String summary(Inventory inventory, Complexity.Report report) {
        int[] counts = counts(inventory, report);
        return counts[0] + " of " + counts[1] + " names in their language's style (functions and files)";
    }

    /** Names in their language's style, and names in all: over each language's functions, then its files. */
    private static int[] counts(Inventory inventory, Complexity.Report report) {
        Map<String, String> languages = new HashMap<>();
        Map<String, List<String>> functions = new TreeMap<>(), files = new TreeMap<>();
        for (Inventory.Source f : inventory.source()) {
            languages.put(f.path(), f.language());
            String name = f.path().substring(f.path().lastIndexOf('/') + 1);
            files.computeIfAbsent(f.language(), k -> new ArrayList<>()).add(name.contains(".") ? name.substring(0, name.indexOf('.')) : name);
        }
        report.functions().forEach((path, measures) -> {
            for (Measure m : measures) {
                String[] parts = m.function().replaceAll("#\\d+$", "").split("\\.");
                String name = parts[parts.length - 1];
                boolean constructor = parts.length > 1 && name.equals(parts[parts.length - 2]);
                if (!constructor && !name.startsWith("<")) functions.computeIfAbsent(languages.get(path), k -> new ArrayList<>()).add(name);
            }
        });
        int fitting = 0, all = 0;
        for (List<String> names : functions.values()) { int[] c = fit(names); fitting += c[0]; all += c[1]; }
        for (List<String> names : files.values()) { int[] c = fit(names); fitting += c[0]; all += c[1]; }
        return new int[] {fitting, all};
    }

    /** Of one language's names, those in its most used style (lowercase words counting with any style), and all. */
    static int[] fit(List<String> names) {
        Map<String, Integer> styles = new HashMap<>();
        int lower = 0;
        for (String name : names) {
            String s = style(name);
            if (s.equals("lower")) lower++;
            else styles.merge(s, 1, Integer::sum);
        }
        int dominant = styles.entrySet().stream().filter(e -> !e.getKey().equals("other")).mapToInt(Map.Entry::getValue).max().orElse(0);
        return new int[] {dominant + lower, names.size()};
    }
}
