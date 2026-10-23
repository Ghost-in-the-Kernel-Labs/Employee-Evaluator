import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Holds the seed corpus (E-078): the README names one rubric, and every card in corpus/ says it was made under that
 * rubric, in its JSON and in its Markdown alike. It reads only this repository, so it runs on a clean clone; it does
 * not hold the cards to the current rubric (the README says why).
 */
// Decision: E-078
final class CorpusTest {
    private static final Pattern README_RUBRIC = Pattern.compile("rubric `([0-9a-f]{12})`");
    private static final Pattern JSON_RUBRIC = Pattern.compile("\"rubricHash\":\\s*\"([0-9a-f]{12})\"");
    private static final Pattern MARKDOWN_RUBRIC = Pattern.compile("rubric `([0-9a-f]{12})`");
    private static final Pattern README_CARD = Pattern.compile("(?m)^\\| `([a-z-]+)` \\|");

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        String readme = Check.read("corpus/README.md");
        Map<String, String> json = cards(".json"), markdown = cards(".md");
        return new Check("corpus")
                .repository(check(readme, json, markdown))
                .rejects("a README that names no rubric", () -> check("no rubric here", Map.of("a", card("1a2b3c4d5e6f")), Map.of("a", "rubric `1a2b3c4d5e6f`")))
                .rejects("a README that names two rubrics", () -> check(table("a") + "rubric `1a2b3c4d5e6f` rubric `aaaaaaaaaaaa`", Map.of("a", card("1a2b3c4d5e6f")), Map.of("a", "rubric `1a2b3c4d5e6f`")))
                .rejects("a JSON card under another rubric", () -> check(table("a") + "rubric `1a2b3c4d5e6f`", Map.of("a", card("aaaaaaaaaaaa")), Map.of("a", "rubric `1a2b3c4d5e6f`")))
                .rejects("a Markdown card under another rubric", () -> check(table("a") + "rubric `1a2b3c4d5e6f`", Map.of("a", card("1a2b3c4d5e6f")), Map.of("a", "rubric `aaaaaaaaaaaa`")))
                .rejects("a card with no Markdown", () -> check(table("a") + "rubric `1a2b3c4d5e6f`", Map.of("a", card("1a2b3c4d5e6f")), Map.of()))
                .rejects("a card the README does not list", () -> check(table("b") + "rubric `1a2b3c4d5e6f`", Map.of("a", card("1a2b3c4d5e6f")), Map.of("a", "rubric `1a2b3c4d5e6f`")))
                .rejects("the repository's README moved to another rubric", () -> check(readme.replaceAll("rubric `[0-9a-f]{12}`", "rubric `ffffffffffff`"), json, markdown))
                .accepts("cards under the README's rubric", () -> check(table("a") + "rubric `1a2b3c4d5e6f`", Map.of("a", card("1a2b3c4d5e6f")), Map.of("a", "rubric `1a2b3c4d5e6f`")));
    }

    /** What the corpus gets wrong: a README naming no rubric or two, a card without both renderings or not in the README's table, or a rendering under another rubric. */
    static List<String> check(String readme, Map<String, String> json, Map<String, String> markdown) {
        List<String> failures = new ArrayList<>();
        TreeSet<String> named = new TreeSet<>();
        Matcher m = README_RUBRIC.matcher(readme);
        while (m.find()) named.add(m.group(1));
        if (named.size() != 1) failures.add("the README names " + named.size() + " rubrics " + named + ", not one");
        TreeSet<String> listed = new TreeSet<>();
        Matcher row = README_CARD.matcher(readme);
        while (row.find()) listed.add(row.group(1));
        TreeSet<String> cards = new TreeSet<>(json.keySet());
        cards.addAll(markdown.keySet());
        if (!listed.equals(cards)) failures.add("the README lists cards " + listed + " but corpus/ holds " + cards);
        for (String card : cards) {
            String fromJson = only(JSON_RUBRIC, json.get(card)), fromMarkdown = only(MARKDOWN_RUBRIC, markdown.get(card));
            if (fromJson == null) failures.add(card + ".json has no single rubricHash");
            if (fromMarkdown == null) failures.add(card + ".md has no single rubric");
            if (fromJson != null && fromMarkdown != null && !fromJson.equals(fromMarkdown)) {
                failures.add(card + ": the JSON says rubric " + fromJson + " and the Markdown " + fromMarkdown);
            }
            if (named.size() == 1 && fromJson != null && !named.contains(fromJson)) {
                failures.add(card + " is under rubric " + fromJson + ", the README names " + named);
            }
        }
        return failures;
    }

    /** The one hash the pattern finds in the text, or null when the text is missing or the pattern finds none or two different. */
    private static String only(Pattern pattern, String text) {
        if (text == null) return null;
        TreeSet<String> found = new TreeSet<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) found.add(m.group(1));
        return found.size() == 1 ? found.first() : null;
    }

    private static String card(String hash) {
        return "{\"produced\": {\"rubricHash\": \"" + hash + "\"}}";
    }

    private static String table(String name) {
        return "| Card |\n| --- |\n| `" + name + "` |\n";
    }

    /** The corpus's files with this ending, by card name (the file's name without it). */
    private static Map<String, String> cards(String ending) {
        Map<String, String> files = new TreeMap<>();
        try (Stream<Path> list = Files.list(Check.ROOT.resolve("corpus"))) {
            for (Path p : list.filter(p -> p.getFileName().toString().endsWith(ending) && !p.getFileName().toString().equals("README.md")).toList()) {
                String name = p.getFileName().toString();
                files.put(name.substring(0, name.length() - ending.length()), Files.readString(p));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }
}
