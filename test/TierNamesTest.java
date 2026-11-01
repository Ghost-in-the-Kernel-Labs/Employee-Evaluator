import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Holds E-092: a worker's tier is named everywhere in CLAUDE.md and docs/how/, and its model only in the map file
 * `docs/how/hand-out-a-task.md`. A model family name (Haiku, Sonnet, Opus, Fable) elsewhere fails, except in the files
 * that state a product's or the posture's own settings: the judge harness's model ids (`judge-harness.md`,
 * `judge-trials.md`) and the dependency posture's reviewer pins (`update-a-dependency.md`). The agents folder under .claude is pinned by hash
 * (`DependencyReviewTest`) and not read here; the journal, decisions and history keep their names.
 */
// Decision: E-092
final class TierNamesTest {
    static final String MAP = "docs/how/hand-out-a-task.md";
    static final Set<String> EXEMPT = Set.of(MAP, "docs/how/judge-harness.md", "docs/how/judge-trials.md", "docs/how/update-a-dependency.md");
    /** The one file kept under its old name so what links it does not break (E-092); the name is not a use of the model. */
    static final String KEPT_NAME = "escalate-to-opus.md";
    private static final Pattern FAMILY = Pattern.compile("(?i)\\b(haiku|sonnet|opus|fable)\\b");

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        return new Check("tier names")
                .repository(check(files()))
                .rejects("a family name in CLAUDE.md", () -> check(Map.of("CLAUDE.md", "- a fresh Claude Sonnet agent\n")))
                .rejects("a lower-case family name in a how-file", () -> check(Map.of("docs/how/x.md", "model `opus`\n")))
                .accepts("a tier name", () -> check(Map.of("CLAUDE.md", "- a mid-tier agent\n")))
                .accepts("the map file", () -> check(Map.of(MAP, "| small | Haiku | mid | Sonnet | strong | Opus |\n")))
                .accepts("an exempt file", () -> check(Map.of("docs/how/judge-harness.md", "claude-haiku-5-5\n")))
                .accepts("the kept file name", () -> check(Map.of("CLAUDE.md", "see docs/how/escalate-to-opus.md\n")))
                .rejects("the map file losing the table", () -> check(Map.of(MAP, "no models here\n")));
    }

    /** Each file with a family name that is neither the map nor exempt, and the map without all three models. */
    static List<String> check(Map<String, String> files) {
        List<String> failures = new ArrayList<>();
        files.forEach((path, text) -> {
            if (!EXEMPT.contains(path) && FAMILY.matcher(text.replace(KEPT_NAME, "")).find()) {
                failures.add(path + " names a model family: name the tier, and keep the model in " + MAP + " (E-092)");
            }
        });
        String map = files.get(MAP);
        if (map != null && !(map.contains("Haiku") && map.contains("Sonnet") && map.contains("Opus"))) {
            failures.add(MAP + " must hold the tier-to-model table (Haiku, Sonnet, Opus)");
        }
        return failures;
    }

    private static Map<String, String> files() {
        Map<String, String> files = new TreeMap<>();
        files.put("CLAUDE.md", Check.read("CLAUDE.md"));
        try (Stream<Path> how = Files.list(Check.ROOT.resolve("docs/how"))) {
            how.filter(p -> p.toString().endsWith(".md")).forEach(p -> files.put("docs/how/" + p.getFileName(), read(p)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
