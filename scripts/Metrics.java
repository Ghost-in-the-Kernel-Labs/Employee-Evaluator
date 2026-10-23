import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The navigation tithe's numbers (docs/AGENT.md, E-008), measured the same way every time and
 * printed as the Markdown table docs/AGENT.md records, then the warnings: long or
 * branching functions with no comment saying what they are for, and the `// Unsure:` lines.
 *   gradle metrics                 the table and the warnings
 *   gradle metrics --args=--time   also times the fast loop (`gradle test --offline --rerun-tasks`)
 * Ported from Katana's scripts/Metrics.java, itself from Suite Builder's scripts/kernel-metrics.mjs.
 */
final class Metrics {
    private static final Pattern CODE = Pattern.compile("`([^`]+)`");

    public static void main(String[] args) throws Exception {
        boolean time = List.of(args).contains("--time");
        System.out.println("| Metric (at " + commit() + ") | Measured |\n| --- | --- |");
        measure(time).forEach((name, value) -> System.out.println("| " + name + " | " + value + " |"));
        warnings().forEach(System.out::println);
    }

    /** Each metric of docs/AGENT.md by its name there, with its value and what it was measured on. */
    static Map<String, String> measure(boolean time) throws Exception {
        Map<String, String> java = Repo.files(".java");
        List<Scan.Function> functions = Repo.functions(java);
        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Mandatory context (words)", words(Repo.read("CLAUDE.md")) + " (CLAUDE.md, the supply-chain rules in it)");
        metrics.put("Fast loop (s)", time ? fastLoop() : "not timed (--time)");
        metrics.put("Largest source file (lines)", largest(java));
        metrics.put("Files a routing row opens", routing(Repo.read("CLAUDE.md")));
        metrics.put("Functions over 20 paths", String.valueOf(functions.stream().filter(f -> f.paths() > 20).count()));
        metrics.put("Duplicate helpers (same body, two names)", String.valueOf(duplicates(functions)));
        metrics.put("Decisions without `See also`", withoutSeeAlso(Repo.read("theGhost.md")));
        metrics.put("Docs lines per source line", docsRatio(java));
        metrics.put("Stale generated artifacts", stale());
        metrics.put("Commits since the last tithe", cadence());
        return metrics;
    }

    static int words(String text) {
        return text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }

    private static String largest(Map<String, String> java) {
        var biggest = java.entrySet().stream().max(Comparator.comparingInt(e -> Repo.lines(e.getValue())));
        return biggest.map(e -> Repo.lines(e.getValue()) + " (" + e.getKey() + ")").orElse("0");
    }

    /** The most paths a row of CLAUDE.md's routing table names under "Start here". */
    static String routing(String claude) {
        int most = 0;
        String which = "";
        for (String line : claude.split("\n")) {
            String[] cells = line.split("\\|");
            if (cells.length < 3 || line.startsWith("| Task") || line.startsWith("| ---")) continue;
            int count = 0;
            Matcher code = CODE.matcher(cells[2]);
            while (code.find()) {
                String path = code.group(1);
                if (!path.contains("/") && !path.matches(".*\\.(md|json|java|sh)")) continue;
                count++;
                // A pointer to a workpath's instructions (E-063): its "Start here" files are opened too.
                if (path.startsWith("docs/how/")) count += startHere(Repo.read(path));
            }
            if (count > most) {
                most = count;
                which = cells[1].trim();
            }
        }
        return most + (which.isEmpty() ? "" : " (" + which + ")");
    }

    /** The files an instructions file's "Start here" list opens: one per bullet. */
    static int startHere(String how) {
        int start = how.indexOf("**Start here:**");
        if (start < 0) return 0;
        int end = how.indexOf("\n\n", start);
        String list = how.substring(start, end < 0 ? how.length() : end);
        return (int) list.lines().filter(l -> l.startsWith("- ")).count();
    }

    /** Bodies of three statements or more that appear under more than one name. */
    static int duplicates(List<Scan.Function> functions) {
        Map<String, Set<String>> names = new HashMap<>();
        for (Scan.Function f : functions) {
            if (f.body().chars().filter(c -> c == ';').count() >= 3) names.computeIfAbsent(f.body(), b -> new HashSet<>()).add(f.name());
        }
        return (int) names.values().stream().filter(n -> n.size() > 1).count();
    }

    static String withoutSeeAlso(String ghost) {
        List<String> missing = new ArrayList<>();
        String[] blocks = ghost.split("(?m)^(?=### E-)");
        for (String block : blocks) {
            if (!block.startsWith("### E-")) continue;
            if (!block.contains("- **See also:**")) missing.add(block.substring(4, 9));
        }
        return missing.size() + (missing.isEmpty() ? "" : " (" + String.join(", ", missing) + ")");
    }

    /** Lines of the docs we write (the study's notes and the generated kernel map aside) per line of our Java. */
    private static String docsRatio(Map<String, String> java) {
        int docs = 0;
        for (var doc : Repo.files(".md").entrySet()) {
            String path = doc.getKey();
            if (path.startsWith("docs/research/") || path.equals(KernelMap.PATH)) continue;
            if (path.contains("/") && !path.startsWith("docs/")) continue;
            docs += Repo.lines(doc.getValue());
        }
        int source = java.values().stream().mapToInt(Repo::lines).sum();
        return source == 0 ? "no source" : "%.2f (%d / %d)".formatted((double) docs / source, docs, source);
    }

    /** Generated artifacts whose generator would change them: today, the kernel map. */
    private static String stale() {
        List<String> stale = new ArrayList<>();
        if (!KernelMap.check(Repo.read(KernelMap.PATH), Repo.moduleSources()).isEmpty()) stale.add(KernelMap.PATH);
        if (!ModulesDoc.check(Repo.read(ModulesDoc.PATH), Repo.read(ModulesDoc.SOURCE)).isEmpty()) stale.add(ModulesDoc.PATH);
        if (!DecisionsDoc.check(Repo.read(DecisionsDoc.PATH), Repo.read(DecisionsDoc.SOURCE)).isEmpty()) stale.add(DecisionsDoc.PATH);
        return stale.isEmpty() ? "0" : stale.size() + " (" + String.join(", ", stale) + ")";
    }

    /** One `gradle test` from scratch, in seconds; the first of a session pays for starting the daemon. */
    private static String fastLoop() throws Exception {
        long started = System.nanoTime();
        Process run = new ProcessBuilder("gradle", "test", "--offline", "--rerun-tasks", "-q")
                .directory(Repo.ROOT.toFile()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        int status = run.waitFor();
        return Math.round((System.nanoTime() - started) / 1e9) + (status == 0 ? "" : " (failed)");
    }

    /** Commits since the last whose subject starts "Tithe", a warning past eight; the push is refused past sixteen (TitheGate). */
    static String cadence() {
        try {
            List<String> subjects = TitheGate.subjects();
            int since = TitheGate.since(subjects), review = TitheGate.since(subjects, TitheGate.REVIEW);
            return since + " commits since the last tithe" + (since > TitheGate.WARN ? ": one is due" : "")
                    + "; " + review + " since the last navigation review" + (review > TitheGate.REVIEW_WARN ? ": one is due" : "");
        } catch (Exception e) {
            return "not counted (" + e.getMessage() + ")";
        }
    }

    /** The warnings: they never fail; they say where to look. */
    static List<String> warnings() {
        List<String> lines = new ArrayList<>();
        List<Scan.Function> functions = Repo.functions(Repo.files(".java"));
        lines.add("\nOver 20 paths (split it, each step a function named for what it does):");
        functions.stream().filter(f -> f.paths() > 20).forEach(f -> lines.add("  " + f.key() + " (line " + f.line() + "): " + f.paths() + " paths"));
        lines.add("\nOver 15 lines or 6 paths, with no comment saying what it is for:");
        functions.stream().filter(f -> !f.documented() && (f.lines() > 15 || f.paths() > 6))
                .forEach(f -> lines.add("  " + f.key() + " (line " + f.line() + "): " + f.lines() + " lines, " + f.paths() + " paths"));
        lines.add("\n`// Unsure:` lines (doubts written down; never failed):");
        Repo.files(".java").forEach((path, text) -> {
            String[] source = text.split("\n");
            for (int i = 0; i < source.length; i++) if (source[i].trim().startsWith("// Unsure:")) lines.add("  " + path + ":" + (i + 1) + " " + source[i].trim());
        });
        return lines;
    }

    private static String commit() throws Exception {
        Process git = new ProcessBuilder("git", "rev-parse", "--short", "HEAD").directory(Repo.ROOT.toFile()).start();
        String out = new String(git.getInputStream().readAllBytes()).trim();
        return git.waitFor() == 0 && !out.isEmpty() ? out : "unknown";
    }
}
