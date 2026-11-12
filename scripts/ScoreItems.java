import evaluator.rubric.Json;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The token posture's step 3 (E-092, E-094): each open item of TODO.md scored from facts, with no model, and placed in a
 * tier (`docs/how/hand-out-a-task.md`). The facts are read from the item's own text: the files it names (the largest one's
 * tokens, at Metrics.CHARS_PER_TOKEN), the layers it names (a module of docs/modules.json, agent-runner/, sandbox/), a
 * contract, a pass line a machine checks, and whether it waits on Ghost. The points are in the constants below and the
 * tier edges in SMALL_MAX and MID_MAX, the one place to tune them (with Ghost's approval). An item that names no file and
 * does not wait is unplaced: a mid-tier agent scores it and writes the facts it used.
 *
 *   gradle scoreItems                   every open item: id, points, tier, title, then the facts
 *   gradle scoreItems --args=--tune     what the workers cost against their placements (WorkerLog, E-095)
 * Ported from Suite Builder's scripts/score-items.mjs (its G-357).
 */
final class ScoreItems {
    /** Per this many tokens of the largest file an item names, one point. */
    static final int TOKENS_PER_POINT = 10_000;
    /** Each layer beyond the first the item names. */
    static final int EXTRA_LAYER = 2;
    /** A contract: the score card's shape, an invariant, a decision's rule. */
    static final int CONTRACT = 3;
    /** A pass line a machine checks. */
    static final int MECHANICAL = -2;
    /** The most points of the small tier, and of the mid tier; above, or a wait on Ghost, the strong tier. */
    static final int SMALL_MAX = 3, MID_MAX = 7;

    /** The layers outside docs/modules.json an item can name by folder. */
    private static final List<String> OTHER_LAYERS = List.of("agent-runner", "sandbox");
    private static final Pattern ITEM = Pattern.compile("(?m)^### (\\S+) (.+)$");
    private static final Pattern CODE = Pattern.compile("`([^`\\s]+)`");
    private static final Pattern CONTRACT_WORDS = Pattern.compile("\\bcontracts?\\b|\\bscore card'?s? shape\\b|\\binvariants?\\b|SCORE-CARD\\.md|\\bdecision'?s? rule\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MECHANICAL_WORDS = Pattern.compile("\\b(unit-tested|test fails|a test|golden|goldens|the gate)\\b|Test\\.java", Pattern.CASE_INSENSITIVE);
    private static final Pattern WAITS = Pattern.compile("\\bwaits? on\\b|\\bGhost'?s? (word|answer|approval|call)\\b|\\bopen question\\b|\\bGhost (decides|rules)\\b", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        if (List.of(args).contains("--tune")) {
            WorkerLog.report(WorkerLog.parse(Repo.read(WorkerLog.PATH))).forEach(System.out::println);
            return;
        }
        for (Score s : score(Repo.read("TODO.md"), layers(Repo.read("docs/modules.json")))) System.out.println(s.line());
    }

    /** One item's facts. `tokens` is null when it names no file of the repository. */
    record Facts(List<String> files, Integer tokens, List<String> layers, boolean contract, boolean mechanical, boolean waits) {}

    /** One item scored: its id and title, points (null: unplaced), tier, and the parts that made the points. */
    record Score(String id, String title, Integer points, String tier, List<String> parts, Facts facts) {
        /** Two lines for the report: id, points, tier and title; then what made the points and the files and layers read. */
        String line() {
            String why = points == null ? "names no file or layer: unplaced" : parts.isEmpty() ? "under a point" : String.join("; ", parts);
            String read = (facts.files().isEmpty() ? "" : "; files: " + String.join(", ", facts.files())) + (facts.layers().isEmpty() ? "" : "; layers: " + String.join(", ", facts.layers()));
            return id + "\t" + (points == null ? "-" : points) + "\t" + tier + "\t" + title + "\n\t" + why + read + (facts.waits() ? "; waits on Ghost" : "");
        }
    }

    /** The layers an item can name: each module of docs/modules.json, and the folders outside it. */
    @SuppressWarnings("unchecked")
    static List<String> layers(String modulesJson) {
        Map<String, Object> modules = (Map<String, Object>) ((Map<String, Object>) Json.parse(modulesJson)).get("modules");
        List<String> layers = new ArrayList<>(modules.keySet());
        layers.addAll(OTHER_LAYERS);
        return layers;
    }

    /** Every open item of TODO.md, in order, scored. */
    static List<Score> score(String todo, List<String> layers) {
        List<Score> scores = new ArrayList<>();
        Matcher item = ITEM.matcher(todo);
        List<int[]> spans = new ArrayList<>();
        while (item.find()) spans.add(new int[] {item.start(), item.end()});
        for (int i = 0; i < spans.size(); i++) {
            int end = i + 1 < spans.size() ? spans.get(i + 1)[0] : todo.length();
            String heading = todo.substring(spans.get(i)[0], spans.get(i)[1]);
            Matcher h = ITEM.matcher(heading);
            if (!h.find()) continue;
            String body = todo.substring(spans.get(i)[0], end);
            int phase = body.indexOf("\n## ");
            scores.add(scored(h.group(1), h.group(2).trim(), factsOf(phase < 0 ? body : body.substring(0, phase), layers)));
        }
        return scores;
    }

    /** The facts of one item's text. A layer is named by its folder (`score/`) or package (`evaluator.score`), never the bare word. */
    static Facts factsOf(String text, List<String> layers) {
        Set<String> files = new LinkedHashSet<>();
        Matcher code = CODE.matcher(text);
        while (code.find()) {
            String path = code.group(1);
            if (Files.isRegularFile(Repo.ROOT.resolve(path))) files.add(path);
        }
        Integer tokens = files.stream().map(f -> (Repo.read(f).length() + Metrics.CHARS_PER_TOKEN - 1) / Metrics.CHARS_PER_TOKEN).max(Integer::compare).orElse(null);
        List<String> named = layers.stream().filter(l -> Pattern.compile("(?<![\\w-])" + Pattern.quote(l) + "/|evaluator\\." + Pattern.quote(l) + "\\b").matcher(text).find()).toList();
        return new Facts(List.copyOf(files), tokens, named, CONTRACT_WORDS.matcher(text).find(), MECHANICAL_WORDS.matcher(text).find(), WAITS.matcher(text).find());
    }

    /** The tier of a score; a wait on Ghost is the strong tier whatever the points. */
    static String tierOf(int points, boolean waits) {
        if (waits) return "strong";
        return points <= SMALL_MAX ? "small" : points <= MID_MAX ? "mid" : "strong";
    }

    /** An item's points, tier and their parts; one naming no file and not waiting is unplaced. */
    static Score scored(String id, String title, Facts facts) {
        if (facts.tokens() == null && facts.layers().isEmpty() && !facts.waits()) return new Score(id, title, null, "unplaced", List.of(), facts);
        List<String> parts = new ArrayList<>();
        int points = 0;
        int size = facts.tokens() == null ? 0 : facts.tokens() / TOKENS_PER_POINT;
        if (size > 0) parts.add("+" + size + " largest file " + facts.tokens() + " tokens");
        points += size;
        int extra = Math.max(0, facts.layers().size() - 1) * EXTRA_LAYER;
        if (extra > 0) parts.add("+" + extra + " layers: " + String.join(", ", facts.layers()));
        points += extra;
        if (facts.contract()) {
            parts.add("+" + CONTRACT + " a contract");
            points += CONTRACT;
        }
        if (facts.mechanical()) {
            parts.add(MECHANICAL + " a mechanical pass line");
            points += MECHANICAL;
        }
        points = Math.max(0, points);
        return new Score(id, title, points, tierOf(points, facts.waits()), parts, facts);
    }
}
