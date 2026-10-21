import evaluator.rubric.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Holds docs/modules.json (E-004): every entry complete and naming only modules; the may-import graph
 * without a cycle; the entries agreeing with the rules MOD-01 to MOD-04; and every import in the
 * source within its module's may-import list, with no model client in the kernel (MOD-01).
 */
// Decision: E-004
final class ModulesTest {
    private static final List<String> FIELDS = List.of("purpose", "inputs", "outputs", "mayImport", "mustNotImport", "phase", "entryPoints");
    private static final Pattern IMPORT = Pattern.compile("(?m)^import (?:static )?([\\w.]+)");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package ([\\w.]+);");
    private static final List<String> MODEL_CLIENTS =
            List.of("com.anthropic", "com.openai", "dev.langchain4j", "com.google.cloud.vertexai", "software.amazon.awssdk.services.bedrock");
    private static final Set<String> NOT_SOURCE = Set.of("test", "scripts", "build", "third_party", "docs", "web", "agent-runner");
    /** What reaches the network (MOD-05): HTTP, sockets, URL connections. */
    private static final List<String> NETWORK = List.of("java.net.http", "java.net.Socket", "java.net.ServerSocket", "java.net.URL",
            "java.net.HttpURLConnection", "java.net.URLConnection", "java.net.DatagramSocket", "java.nio.channels.SocketChannel");
    /** The one door outside github/ (MOD-05): the sandbox's registries proxy. */
    private static final String PROXY = "analyze/evaluator/analyze/RegistriesProxy.java";

    public static void main(String[] args) {
        run().exit();
    }

    /** Every rule on docs/modules.json: the imports each module may make, MOD-05's network rule, the entry points named. */
    static Check run() {
        String json = Check.read("docs/modules.json");
        return new Check("modules")
                .repository(check(json, source()))
                .rejects("score/ importing analyze/", () -> check(json, Map.of("score/Card.java", "package evaluator.score;\nimport evaluator.analyze.Complexity;\n")))
                .rejects("a model client in the kernel", () -> check(json, Map.of("analyze/Judge.java", "package evaluator.analyze;\nimport com.anthropic.Client;\n")))
                .rejects("source outside any module", () -> check(json, Map.of("misc/X.java", "package evaluator.misc;\n")))
                .rejects("rubric/ allowed an import", () -> check(json.replaceFirst("(\"rubric\": \\{[^}]*\"mayImport\": \\[)", "$1\"ingest\""), Map.of()))
                .rejects("a module importing cli/", () -> check(json, Map.of("ingest/Walk.java", "package evaluator.ingest;\nimport evaluator.cli.Main;\n")))
                .rejects("a file in the wrong package", () -> check(json, Map.of("ingest/Walk.java", "package evaluator.score;\n")))
                .rejects("the network outside github/", () -> check(json, Map.of("history/Fetch.java", "package evaluator.history;\nimport java.net.http.HttpClient;\n")))
                .rejects("a socket outside github/", () -> check(json, Map.of("analyze/Beacon.java", "package evaluator.analyze;\nimport java.net.Socket;\n")))
                .accepts("the network in github/, the proxy's sockets, and a URI anywhere", () -> check(json, Map.of(
                        "github/Api.java", "package evaluator.github;\nimport java.net.http.HttpClient;\n",
                        PROXY, "package evaluator.analyze;\nimport java.net.ServerSocket;\n",
                        "history/Name.java", "package evaluator.history;\nimport java.net.URI;\n")))
                .accepts("an allowed import", () -> check(json, Map.of("analyze/Complexity.java", "package evaluator.analyze;\nimport evaluator.ingest.Inventory;\nimport java.util.List;\n")));
    }

    @SuppressWarnings("unchecked")
    static List<String> check(String json, Map<String, String> source) {
        List<String> failures = new ArrayList<>();
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) ((Map<String, Object>) Json.parse(json)).get("modules");
        for (var entry : modules.entrySet()) entry(entry.getKey(), entry.getValue(), modules.keySet(), failures);
        cycles(modules, failures);
        rules(modules, failures);
        for (var file : source.entrySet()) if (file.getKey().endsWith(".java")) imports(file.getKey(), file.getValue(), modules, failures);
        return failures;
    }

    /** One module's entry: every field, import lists naming other modules that exist, none both allowed and refused. */
    private static void entry(String name, Map<String, Object> m, Set<String> known, List<String> failures) {
        for (String field : FIELDS) if (!m.containsKey(field)) failures.add(name + " has no " + field);
        for (String list : List.of("mayImport", "mustNotImport")) {
            for (String other : names(m, list)) {
                if (!known.contains(other)) failures.add(name + "." + list + " names " + other + ", not a module");
                if (other.equals(name)) failures.add(name + "." + list + " names itself");
            }
        }
        for (String other : names(m, "mayImport")) {
            if (names(m, "mustNotImport").contains(other)) failures.add(name + " both may and must not import " + other);
        }
    }

    /** No module reaches itself through mayImport. */
    private static void cycles(Map<String, Map<String, Object>> modules, List<String> failures) {
        for (String start : modules.keySet()) {
            List<String> stack = new ArrayList<>(List.of(start));
            Set<String> seen = new HashSet<>();
            while (!stack.isEmpty()) {
                String at = stack.remove(stack.size() - 1);
                for (String next : names(modules.get(at), "mayImport")) {
                    if (next.equals(start)) failures.add(start + " can reach itself through mayImport");
                    else if (seen.add(next) && modules.containsKey(next)) stack.add(next);
                }
            }
        }
    }

    /** The entries must agree with the rules they carry (MOD-02 to MOD-04; MOD-01 is checked on source). */
    private static void rules(Map<String, Map<String, Object>> modules, List<String> failures) {
        expect(names(modules.get("rubric"), "mayImport").isEmpty(), "MOD-02: rubric/ imports no module of ours", failures);
        expect(names(modules.get("score"), "mayImport").equals(List.of("rubric")), "MOD-03: score/ reads rubric/ only", failures);
        for (var m : modules.entrySet()) {
            expect(!names(m.getValue(), "mayImport").contains("cli"), "MOD-04: nothing imports cli/ (" + m.getKey() + " does)", failures);
        }
    }

    /** A source's imports of other modules, each one its module may make. */
    private static void imports(String path, String text, Map<String, Map<String, Object>> modules, List<String> failures) {
        String module = path.split("/")[0];
        if (!modules.containsKey(module)) {
            failures.add(path + " is not in any module of docs/modules.json");
            return;
        }
        Matcher pkg = PACKAGE.matcher(text);
        if (!pkg.find() || !(pkg.group(1) + ".").startsWith("evaluator." + module + ".")) failures.add(path + " is not in package evaluator." + module);
        Matcher imp = IMPORT.matcher(text);
        while (imp.find()) {
            String name = imp.group(1);
            for (String client : MODEL_CLIENTS) if (name.startsWith(client)) failures.add(path + " imports a model client, " + name + " (MOD-01, E-003)");
            boolean network = NETWORK.stream().anyMatch(n -> name.equals(n) || name.startsWith(n + "."));
            if (network && !module.equals("github") && !path.equals(PROXY)) failures.add(path + " reaches the network with " + name + ": only github/ does (MOD-05)");
            if (!name.startsWith("evaluator.")) continue;
            String target = name.split("\\.")[1];
            if (target.equals("cli") && !module.equals("cli")) failures.add(path + " imports " + name + ": nothing imports cli/ (MOD-04)");
            if (!target.equals(module) && !names(modules.get(module), "mayImport").contains(target)) {
                failures.add(path + " imports " + name + ", but " + module + " may not import " + target);
            }
        }
    }

    /** Every .java outside the tests, the scripts, the vendored code and the other languages' packages, by path from the root. */
    private static Map<String, String> source() {
        Map<String, String> files = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(Check.ROOT)) {
            for (Path p : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = Check.ROOT.relativize(p).toString().replace('\\', '/');
                if (NOT_SOURCE.contains(rel.split("/")[0]) || rel.startsWith(".")) continue;
                files.put(rel, Files.readString(p));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    @SuppressWarnings("unchecked")
    private static List<String> names(Map<String, Object> module, String list) {
        Object value = module == null ? null : module.get(list);
        return value instanceof List<?> l ? (List<String>) l : List.of();
    }

    private static void expect(boolean holds, String rule, List<String> failures) {
        if (!holds) failures.add("modules.json breaks " + rule);
    }
}
