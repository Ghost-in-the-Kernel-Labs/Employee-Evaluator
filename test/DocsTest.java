import evaluator.rubric.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds CLAUDE.md (E-006; Suite Builder's SRC-15): every path it names exists. A path
 * not built yet is allowed only while the phase or step that builds it is open in TODO.md: the phase
 * docs/modules.json gives its module, or one its paragraph or table row names ("(phase 2)", "phase
 * 0.3 and 0.6", "(phases 5–6)"). Once that is done, the path must exist.
 */
final class DocsTest {
    private static final Pattern CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern PHASES = Pattern.compile("phases? (\\d+(?:\\.\\d+)?)((?:\\s*(?:and|,|–|-)\\s*\\d+(?:\\.\\d+)?)*)");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Pattern TODO_ITEM = Pattern.compile("(?m)^### (\\d+\\.\\d+) ");

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        Set<String> open = openSteps(Check.read("TODO.md"));
        Map<String, Integer> phases = modulePhases(Check.read("docs/modules.json"));
        return new Check("docs")
                .repository(check(Check.read("CLAUDE.md"), open, phases, Check::exists))
                .rejects("a path that does not exist", () -> check("Read `docs/NOWHERE.md`.", open, phases, Check::exists))
                .rejects("a path whose phase is done", () -> check("`x/types` (phase 2)", Set.of("3.1"), Map.of(), p -> false))
                .accepts("a path whose phase is open", () -> check("`x/types`\nand (phase 2)", Set.of("2.4"), Map.of(), p -> false))
                .rejects("a module whose phase is done", () -> check("`ingest/`", Set.of("3.1"), Map.of("ingest", 2), p -> false))
                .accepts("a module whose phase is open", () -> check("`ingest/`", Set.of("2.4"), Map.of("ingest", 2), p -> false));
    }

    /** What a document gets wrong: a code path it names that does not exist, unless a planned step or module says it will. */
    static List<String> check(String doc, Set<String> openSteps, Map<String, Integer> modulePhases, Predicate<String> exists) {
        List<String> failures = new ArrayList<>();
        for (String unit : units(doc)) {
            Matcher code = CODE.matcher(unit);
            while (code.find()) {
                String path = path(code.group(1));
                if (path == null || exists.test(path)) continue;
                Integer phase = modulePhases.get(path.split("/")[0]);
                String named = phase == null ? unit : unit + " phase " + phase;
                if (!planned(named, openSteps)) failures.add("names `" + path + "`, which does not exist");
            }
        }
        return failures;
    }

    /** A paragraph, or one row of a table: the span a phase named beside a path applies to. */
    private static List<String> units(String doc) {
        List<String> units = new ArrayList<>();
        for (String paragraph : doc.split("\\n\\s*\\n")) {
            if (paragraph.startsWith("|")) units.addAll(List.of(paragraph.split("\n")));
            else units.add(paragraph.replace('\n', ' '));
        }
        return units;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Integer> modulePhases(String json) {
        Map<String, Integer> phases = new TreeMap<>();
        var modules = (Map<String, Map<String, Object>>) ((Map<String, Object>) Json.parse(json)).get("modules");
        modules.forEach((name, m) -> phases.put(name, ((Double) m.get("phase")).intValue()));
        return phases;
    }

    /** The path in a code span, or null when the span is not one (a comment, a glob, a sentence). */
    private static String path(String span) {
        String first = span.split(" ")[0];
        if (span.contains(" ") && !first.startsWith("./")) return null;
        if (first.contains("*") || first.startsWith("//")) return null;
        boolean looksLikePath = first.contains("/") || first.matches(".*\\.(md|json|sh|java|gradle)$");
        return looksLikePath ? first : null;
    }

    /** Whether a line names a phase or step still open, so a path in it may not exist yet. */
    private static boolean planned(String line, Set<String> openSteps) {
        Matcher phases = PHASES.matcher(line);
        while (phases.find()) {
            List<String> named = new ArrayList<>();
            Matcher number = NUMBER.matcher(phases.group());
            while (number.find()) named.add(number.group());
            if (phases.group(2).contains("–") || phases.group(2).contains("-")) named = range(named);
            for (String step : named) {
                for (String open : openSteps) {
                    if (open.equals(step) || open.startsWith(step + ".")) return true;
                }
            }
        }
        return false;
    }

    /** "phases 5–6" names every phase from the first to the last. */
    private static List<String> range(List<String> ends) {
        List<String> all = new ArrayList<>();
        int from = (int) Double.parseDouble(ends.get(0));
        int to = (int) Double.parseDouble(ends.get(ends.size() - 1));
        for (int phase = from; phase <= to; phase++) all.add(String.valueOf(phase));
        return all;
    }

    static Set<String> openSteps(String todo) {
        Set<String> steps = new TreeSet<>();
        Matcher item = TODO_ITEM.matcher(todo);
        while (item.find()) steps.add(item.group(1));
        return steps;
    }
}
