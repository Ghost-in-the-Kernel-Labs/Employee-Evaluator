import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.ProvenanceTier;
import evaluator.rubric.ScoreCard;
import evaluator.rubric.State;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds docs/SCORE-CARD.md to rubric/ (E-017, TODO.md 1.2): the seven metrics, a fact's fields and
 * states, the card's fields and the provenance tiers are the same words in both, in the same order;
 * every invariant SC-nn is cited in rubric/ or score/ (`// Invariant: SC-02`) or names the open TODO step that
 * will hold it, and no citation names an invariant the document lacks.
 */
// Decision: E-006
final class ScoreCardDocTest {
    private static final Pattern BACKTICKED = Pattern.compile("`([^`]+)`");
    private static final Pattern INVARIANT = Pattern.compile("(?m)^- \\*\\*(SC-\\d{2}) ");
    private static final Pattern CITED = Pattern.compile("// Invariant: (SC-\\d{2})");
    private static final Pattern STEP = Pattern.compile("TODO\\.md (\\d+\\.\\d+)");
    private static final Pattern TIER = Pattern.compile("(\\w+) \\(");

    /** The words of rubric/ the document must use: what the code says. */
    record Code(List<String> metrics, List<String> factFields, List<String> states, List<String> cardFields, List<String> tiers, Set<String> cited) {}

    public static void main(String[] args) {
        run().exit();
    }

    /** The document against the code, and against copies of it broken one way each, which must be caught. */
    static Check run() {
        String doc = Check.read("docs/SCORE-CARD.md");
        Set<String> open = DocsTest.openSteps(Check.read("TODO.md"));
        Map<String, String> sources = Repo.moduleSources();
        Code code = code(sources);
        return new Check("score card")
                .repository(check(doc, code, open))
                .repository(kinds(doc, sources))
                .rejects("an example kind naming no metric", () -> kinds(doc.replace("`PROVENANCE.history.spread_days`", "`HISTORY.spread_days`"), sources))
                .rejects("a kind a module's row lists and its source never names", () -> kinds(doc.replace("`QUALITY.tests.*`,", "`QUALITY.tests.*`, `QUALITY.lint`,"), sources))
                .rejects("a kind a module names that its row lacks", () -> kinds(doc, with(sources, "history/X.java", "Fact.measured(\"MAINTAINABILITY.refactors\", s, 1, e);")))
                .accepts("a module the table names that has no source yet", () -> kinds(doc, sources))
                .rejects("a fact field the code lacks", () -> check(doc.replace("| `confidence` |", "| `certainty` |"), code, open))
                .rejects("a metric renamed", () -> check(doc.replace("| Ambition |", "| Vision |"), code, open))
                .rejects("a card field missing", () -> check(doc.replace("| `toolingBucket` |", "| bucket |"), code, open))
                .rejects("a state the code lacks", () -> check(doc.replace("`refuted`", "`disputed`"), code, open))
                .rejects("a tier renamed", () -> check(doc.replace("Partial (50–79)", "Some (50–79)"), code, open))
                .rejects("an invariant nobody holds", () -> check(doc + "\n- **SC-99 A new rule.** Nothing cites it.\n", code, open))
                .rejects("an invariant whose step is done", () -> check(doc, code, Set.of("3.5")))
                .rejects("a citation of an invariant not in the document", () -> check(doc, withCited(code, "SC-98"), open))
                .accepts("an invariant held from an open step", () -> check(doc + "\n- **SC-99 A rule.** Held from TODO.md 2.6.\n", code, with(open, "2.6")));
    }

    /** The open steps and one more, so a sample invariant held from it is accepted whatever the document cites. */
    private static Set<String> with(Set<String> open, String step) {
        Set<String> steps = new java.util.HashSet<>(open);
        steps.add(step);
        return steps;
    }

    private static final Pattern KIND_LITERAL = Pattern.compile("\"((?:SIZE|COMPLEXITY|MAINTAINABILITY|QUALITY|NAVIGABILITY|PROVENANCE|AMBITION)\\.[a-z_.]*)");

    /**
     * The fact kinds against the code: each example a kind `Metric.ofKind` takes, and the table "Where the facts
     * come from" the same as each module's source: every kind a module names is in its row (exactly, or under a
     * `.*`), and every kind in the row of a module with source is named in it. A kind ending in "." in the source
     * is built on at run time, so a row lists it as `.*`.
     */
    static List<String> kinds(String doc, Map<String, String> sources) {
        List<String> failures = new ArrayList<>();
        for (String example : backticked(row(section(doc, "A fact"), "`kind`"))) {
            try {
                Metric.ofKind(example);
            } catch (IllegalArgumentException e) {
                failures.add("docs/SCORE-CARD.md gives `" + example + "` as a kind, which the code refuses: " + e.getMessage());
            }
        }
        for (String line : section(doc, "Where the facts come from").lines().filter(l -> l.startsWith("| `")).toList()) {
            String[] cells = line.split("\\|");
            String module = cells[1].trim().replace("`", "");
            Set<String> named = new TreeSet<>();
            sources.forEach((path, text) -> {
                if (!path.startsWith(module)) return;
                Matcher m = KIND_LITERAL.matcher(text);
                while (m.find()) named.add(m.group(1));
            });
            List<String> listed = new ArrayList<>();
            Matcher m = BACKTICKED.matcher(cells[2]);
            while (m.find()) listed.add(m.group(1));
            // A module with no source yet, or one that makes no fact (score/ reads kinds, MOD-03), is not held.
            if (named.isEmpty() || listed.isEmpty()) continue;
            for (String kind : named) {
                boolean covered = listed.stream().anyMatch(k -> k.endsWith(".*") ? kind.startsWith(k.substring(0, k.length() - 1)) : k.equals(kind));
                if (!covered) failures.add(module + " names the kind `" + kind + (kind.endsWith(".") ? "*" : "") + "`, which its row in docs/SCORE-CARD.md lacks");
            }
            for (String kind : listed) {
                boolean found = kind.endsWith(".*") ? named.stream().anyMatch(n -> n.startsWith(kind.substring(0, kind.length() - 1))) : named.contains(kind);
                if (!found) failures.add("docs/SCORE-CARD.md lists `" + kind + "` under " + module + ", whose source never names it: move it to \"not built\"");
            }
        }
        return failures;
    }

    private static Map<String, String> with(Map<String, String> sources, String path, String text) {
        Map<String, String> more = new java.util.HashMap<>(sources);
        more.put(path, text);
        return more;
    }

    /** What the document gets wrong against the code. */
    static List<String> check(String doc, Code code, Set<String> openSteps) {
        List<String> failures = new ArrayList<>();
        same("the metrics", firstCells(section(doc, "The seven metrics"), false), code.metrics(), failures);
        same("a fact's fields", firstCells(section(doc, "A fact"), true), code.factFields(), failures);
        same("a fact's states", backticked(row(section(doc, "A fact"), "`state`")), code.states(), failures);
        same("the card's fields", firstCells(section(doc, "The card"), true), code.cardFields(), failures);
        same("the provenance tiers", tiers(row(section(doc, "The card"), "`provenanceTier`")), code.tiers(), failures);
        invariants(doc, code.cited(), openSteps, failures);
        return failures;
    }

    /** Each invariant cited in the code or held from an open step; each citation an invariant of the document. */
    private static void invariants(String doc, Set<String> cited, Set<String> openSteps, List<String> failures) {
        Set<String> documented = new TreeSet<>();
        Matcher m = INVARIANT.matcher(doc);
        while (m.find()) {
            documented.add(m.group(1));
            if (cited.contains(m.group(1))) continue;
            int end = doc.indexOf("\n- ", m.end());
            Matcher step = STEP.matcher(doc.substring(m.start(), end < 0 ? doc.length() : end));
            if (!step.find() || !openSteps.contains(step.group(1))) {
                failures.add(m.group(1) + " is cited nowhere in rubric/ or score/ (`// Invariant: " + m.group(1) + "`) and names no open TODO step that will hold it");
            }
        }
        for (String id : cited) if (!documented.contains(id)) failures.add("the code cites " + id + ", which docs/SCORE-CARD.md does not have");
    }

    private static void same(String what, List<String> documented, List<String> coded, List<String> failures) {
        if (!documented.equals(coded)) failures.add(what + ": the document says " + documented + ", rubric/ says " + coded);
    }

    /** The text under a `## heading` (a decision after it allowed, "(E-011)"), up to the next one. */
    private static String section(String doc, String heading) {
        Matcher start = Pattern.compile("\\n## " + Pattern.quote(heading) + "( \\(E-\\d{3}\\))?\\n").matcher(doc);
        if (!start.find()) return "";
        int at = start.start();
        int end = doc.indexOf("\n## ", at + 1);
        return doc.substring(at, end < 0 ? doc.length() : end);
    }

    /** The first cell of each body row of the section's table, its backticks taken off when asked. */
    private static List<String> firstCells(String section, boolean backticked) {
        List<String> cells = new ArrayList<>();
        List<String> rows = section.lines().filter(l -> l.startsWith("|")).toList();
        for (String row : rows.subList(Math.min(2, rows.size()), rows.size())) {
            String cell = row.split("\\|")[1].trim();
            cells.add(backticked ? cell.replace("`", "") : cell);
        }
        return cells;
    }

    private static String row(String section, String first) {
        return section.lines().filter(l -> l.startsWith("| " + first + " |")).findFirst().orElse("");
    }

    /** The backticked words of a row's second cell. */
    private static List<String> backticked(String row) {
        String[] cells = row.split("\\|");
        List<String> words = new ArrayList<>();
        if (cells.length < 3) return words;
        Matcher m = BACKTICKED.matcher(cells[2]);
        while (m.find()) words.add(m.group(1));
        return words;
    }

    private static List<String> tiers(String row) {
        String[] cells = row.split("\\|");
        List<String> names = new ArrayList<>();
        if (cells.length < 3) return names;
        Matcher m = TIER.matcher(cells[2]);
        while (m.find()) names.add(m.group(1));
        return names;
    }

    /** The code's words, read from rubric/'s types and the citations in rubric/ and score/, which builds and renders the card. */
    static Code code(Map<String, String> sources) {
        Set<String> cited = new TreeSet<>();
        sources.forEach((path, text) -> {
            if (!path.startsWith("rubric/") && !path.startsWith("score/")) return;
            Matcher m = CITED.matcher(text);
            while (m.find()) cited.add(m.group(1));
        });
        return new Code(
                Arrays.stream(Metric.values()).map(metric -> metric.title).toList(),
                components(Fact.class),
                Arrays.stream(State.values()).map(state -> state.name().toLowerCase()).toList(),
                components(ScoreCard.class),
                Arrays.stream(ProvenanceTier.values()).map(tier -> tier.title).toList(),
                cited);
    }

    private static List<String> components(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    private static Code withCited(Code code, String extra) {
        Set<String> cited = new TreeSet<>(code.cited());
        cited.add(extra);
        return new Code(code.metrics(), code.factFields(), code.states(), code.cardFields(), code.tiers(), cited);
    }
}
