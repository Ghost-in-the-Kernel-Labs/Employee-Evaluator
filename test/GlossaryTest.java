import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Holds docs/GLOSSARY.md (E-007): an alias in backticks in the "Not" column is
 * enforced and fails wherever it appears in a document outside the glossary and docs/research/; an
 * alias without backticks is a review guide no test judges.
 */
// Decision: E-007
final class GlossaryTest {
    private static final String GLOSSARY = "docs/GLOSSARY.md";
    private static final Pattern ROW = Pattern.compile("(?m)^\\| \\*\\*(.+?)\\*\\* \\|.*\\| (.*) \\|$");
    private static final Pattern ENFORCED = Pattern.compile("`([^`]+)`");

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        String glossary = Check.read(GLOSSARY);
        Map<String, String> docs = documents();
        String marked = glossary.replace("| resume, CV", "| `resume`, CV");
        return new Check("glossary")
                .repository(check(glossary, docs))
                .rejects("an enforced alias in the design doc", () -> check(marked, Map.of("docs/DESIGN.md", Check.read("docs/DESIGN.md"))))
                .accepts("an enforced alias in a research note", () -> check(marked, Map.of("docs/research/x.md", "a resume")))
                .accepts("an alias inside a longer word", () -> check(marked, Map.of("x.md", "resumes and aresume")));
    }

    static List<String> check(String glossary, Map<String, String> docs) {
        List<String> failures = new ArrayList<>();
        Map<String, String> enforced = enforced(glossary);
        for (var doc : docs.entrySet()) {
            if (doc.getKey().equals(GLOSSARY) || doc.getKey().startsWith("docs/research/")) continue;
            for (var alias : enforced.entrySet()) {
                Matcher found = Pattern.compile("(?i)(?<![\\w-])" + Pattern.quote(alias.getKey()) + "(?![\\w-])").matcher(doc.getValue());
                if (found.find()) {
                    int line = doc.getValue().substring(0, found.start()).split("\n", -1).length;
                    failures.add(doc.getKey() + ":" + line + " writes `" + alias.getKey() + "`; the word is " + alias.getValue());
                }
            }
        }
        return failures;
    }

    /** Each alias written in backticks in a row's "Not" column, with the term it stands in for. */
    private static Map<String, String> enforced(String glossary) {
        Map<String, String> aliases = new TreeMap<>();
        Matcher row = ROW.matcher(glossary);
        while (row.find()) {
            Matcher alias = ENFORCED.matcher(row.group(2));
            while (alias.find()) aliases.put(alias.group(1), row.group(1));
        }
        return aliases;
    }

    /** Every Markdown file in the repository, by path from the root; build output is not a document. */
    private static Map<String, String> documents() {
        Map<String, String> docs = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(Check.ROOT)) {
            for (Path p : walk.filter(p -> p.toString().endsWith(".md")).toList()) {
                String rel = Check.ROOT.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("build/") || rel.startsWith(".")) continue;
                docs.put(rel, Files.readString(p));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return docs;
    }
}
