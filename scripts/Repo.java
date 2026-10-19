import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/** What the scripts read of the repository: files by path from the root, our Java, the modules. */
final class Repo {
    static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final Set<String> NOT_OURS = Set.of("build", "third_party", "web", "agent-runner");

    static String read(String relative) {
        try {
            return Files.readString(ROOT.resolve(relative));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every file with one of these endings, by path from the root, outside build output and vendored code. */
    static Map<String, String> files(String... endings) {
        Map<String, String> files = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(ROOT)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                String rel = ROOT.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith(".") || NOT_OURS.contains(rel.split("/")[0]) || rel.contains("/prototypes/")) continue;
                for (String ending : endings) if (rel.endsWith(ending)) files.put(rel, Files.readString(p));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    /** The modules docs/modules.json names. */
    @SuppressWarnings("unchecked")
    static Set<String> modules() {
        Map<String, Object> json = (Map<String, Object>) Json.parse(read("docs/modules.json"));
        return ((Map<String, Object>) json.get("modules")).keySet();
    }

    /** The Java in the kernel's modules: what the kernel map records. */
    static Map<String, String> moduleSources() {
        Set<String> modules = modules();
        Map<String, String> sources = new TreeMap<>(files(".java"));
        sources.keySet().removeIf(path -> !modules.contains(path.split("/")[0]));
        return sources;
    }

    static int lines(String text) {
        return text.isEmpty() ? 0 : text.replaceAll("\n$", "").split("\n", -1).length;
    }

    static List<Scan.Function> functions(Map<String, String> sources) {
        return sources.entrySet().stream().flatMap(e -> Scan.functions(e.getKey(), e.getValue()).stream()).toList();
    }
}
