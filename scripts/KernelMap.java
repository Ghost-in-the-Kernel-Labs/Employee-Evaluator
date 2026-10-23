import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * docs/KERNEL-MAP.md, the tool's own function ledger (docs/AGENT.md): `gradle kernelMap`
 * appends a row for every function in a module the map does not yet hold, and never changes a row.
 * A function that goes away is closed by a later row it appends too (E-062): `superseded-by: <key>`
 * when its name appears exactly once among the functions new in this run (it moved, or its nested class
 * moved to its own file), else `removed:
 * <date>`. A rename reads as removed and new; the two rows still say what happened.
 */
final class KernelMap {
    static final String PATH = "docs/KERNEL-MAP.md";
    private static final Pattern ROW = Pattern.compile("(?m)^\\| (\\S+) \\| `([^`]+)` \\| (.*?) \\| (.*?) \\| (.*?) \\|$");

    record Row(String module, String key, String standsOn, String firstSeen, String closed) {}

    public static void main(String[] args) throws IOException {
        String map = Repo.read(PATH);
        Map<String, String> sources = Repo.moduleSources();
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        Files.writeString(Repo.ROOT.resolve(PATH), write(map, sources, today));
        check(Repo.read(PATH), sources).forEach(System.out::println);
    }

    static List<Row> rows(String map) {
        List<Row> rows = new ArrayList<>();
        Matcher row = ROW.matcher(map);
        while (row.find()) rows.add(new Row(row.group(1), row.group(2), row.group(3), row.group(4), row.group(5).trim()));
        return rows;
    }

    /** The keys the map holds open: added, and not closed by a later row. */
    static Set<String> open(List<Row> rows) {
        Set<String> open = new LinkedHashSet<>();
        for (Row row : rows) {
            if (row.closed().isEmpty()) open.add(row.key());
            else open.remove(row.key());
        }
        return open;
    }

    /** What is wrong between the map and the source; empty when the map is current. */
    static List<String> check(String map, Map<String, String> sources) {
        List<String> problems = new ArrayList<>();
        Set<String> open = open(rows(map));
        Set<String> current = new LinkedHashSet<>();
        for (Scan.Function f : Repo.functions(sources)) current.add(f.key());
        for (String key : current) {
            if (!open.contains(key)) problems.add("`" + key + "` is not in " + PATH + ": run `gradle kernelMap`");
        }
        for (String key : open) {
            if (!current.contains(key)) {
                problems.add("`" + key + "` is gone from the source and the map holds it open: run `gradle kernelMap`, which closes it");
            }
        }
        Set<String> known = new LinkedHashSet<>();
        rows(map).forEach(row -> known.add(row.key()));
        for (Row row : rows(map)) {
            String successor = row.closed().startsWith("superseded-by: ") ? row.closed().substring(15).replace("`", "") : null;
            if (successor != null && !known.contains(successor)) problems.add("`" + row.key() + "` is superseded by `" + successor + "`, which the map does not hold");
        }
        return problems;
    }

    /** The map with a row appended for every function it does not hold, and one closing every function gone; no row is changed. */
    // Decision: E-062
    static String write(String map, Map<String, String> sources, String today) {
        Set<String> open = open(rows(map));
        List<Scan.Function> functions = Repo.functions(sources);
        StringBuilder out = new StringBuilder(map.endsWith("\n") ? map : map + "\n");
        Set<String> current = new LinkedHashSet<>(), added = new LinkedHashSet<>();
        for (Scan.Function f : functions) {
            current.add(f.key());
            if (open.contains(f.key())) continue;
            added.add(f.key());
            out.append("| ").append(module(f.key())).append(" | `").append(f.key()).append("` | ").append(standsOn(f))
                    .append(" | ").append(today).append(" |  |\n");
        }
        for (String gone : open) {
            if (current.contains(gone)) continue;
            List<String> moved = added.stream().filter(key -> sameFunction(name(key), name(gone))).toList();
            String closed = moved.size() == 1 ? "superseded-by: `" + moved.get(0) + "`" : "removed: " + today;
            out.append("| ").append(module(gone)).append(" | `").append(gone).append("` |  | ").append(today).append(" | ").append(closed).append(" |\n");
        }
        return out.toString();
    }

    private static String module(String key) {
        return key.split("/")[0];
    }

    /** One function under two names: the same, or a nested class moved to its own file (`Outer.Inner.f`, `Inner.f`). */
    static boolean sameFunction(String a, String b) {
        return a.equals(b) || a.endsWith("." + b) || b.endsWith("." + a);
    }

    /** A function's name without its file: `Json.parse` of `rubric/.../Json.java:Json.parse`. */
    private static String name(String key) {
        return key.substring(key.lastIndexOf(':') + 1);
    }

    /**
     * The decision a function cites (`// Decision: E-xxx` on the line above it), or nothing. A caller is
     * not recorded: a row is never changed, so a caller written once goes stale (Katana's finding, its
     * tithe pass 2).
     */
    static String standsOn(Scan.Function f) {
        return f.decision() != null ? f.decision() : "";
    }
}
