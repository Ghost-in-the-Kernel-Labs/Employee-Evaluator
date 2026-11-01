import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Holds the supply-chain baseline (E-033) to the repository: every workflow pins its actions to a
 * full commit SHA, starts at `permissions: contents: read` and uses no trigger or runner the baseline
 * forbids; no agent or editor config runs anything (a hook, a folder-open task, an MCP server,
 * auto-approve) and no git hook is installed; no file of a known worm is present; every vendored jar
 * is in third_party/MANIFEST.md with the SHA-256 it has; the build declares no repository; and the
 * binding directive is in CLAUDE.md, its one copy, read by every agent on entry; theGhost.md keeps none (E-073).
 */
// Decision: E-033
final class SupplyChainTest {
    private static final Pattern USES = Pattern.compile("(?m)^\\s*-?\\s*uses:\\s*(\\S+)(.*)$");
    private static final Pattern PINNED = Pattern.compile("[^@\\s]+@[0-9a-f]{40}");
    private static final List<String> FORBIDDEN_IN_WORKFLOWS = List.of("pull_request_target", "self-hosted", "toJSON(secrets)", "schedule:");
    private static final Set<String> WORM_FILES = Set.of("setup_bun.js", "bun_environment.js", "Math_Symbol.js");
    private static final Set<String> AGENT_FOLDERS = Set.of(".claude", ".vscode", ".cursor", ".gemini");
    private static final Set<String> SKIPPED = Set.of("build", ".gradle", ".git");
    static final String DIRECTIVE_HEADING = "## Supply-chain rules (binding)";

    public static void main(String[] args) {
        run().exit();
    }

    /** The repository against the baseline, and a broken sample of each rule, which must be caught. */
    static Check run() {
        Map<String, String> files = files();
        return new Check("supply chain")
                .repository(workflows(files))
                .repository(agentConfigs(files, hooks()))
                .repository(vendored(Check.read("third_party/MANIFEST.md"), jars()))
                .repository(build(Check.read("build.gradle")))
                .repository(directive(Check.read("CLAUDE.md"), Check.read("theGhost.md")))
                .repository(images(files))
                .repository(sandboxLocks(Check.read("sandbox/requirements.txt"), Check.read("sandbox/node/package-lock.json")))
                .rejects("a base image by tag", () -> images(Map.of("sandbox/Dockerfile", "FROM python:3.12-slim\n")))
                .rejects("a download with no hash", () -> images(Map.of("sandbox/Dockerfile", "FROM a@sha256:" + "0".repeat(64) + "\nRUN python3 /tmp/fetch.py https://x/y.tgz /opt/y\n")))
                .rejects("a hash of the wrong length", () -> images(Map.of("sandbox/prefetch.sh", "python3 fetch.py get https://x/y.tgz sha256 abc cache/y.tgz\n")))
                .rejects("a prefetch that installs with npm install", () -> images(Map.of("sandbox/prefetch.sh", "npm install --cache c\n")))
                .rejects("a pip install without hashes", () -> images(Map.of("Dockerfile", "FROM a@sha256:" + "0".repeat(64) + "\nRUN pip install pytest\n")))
                .rejects("an npm install", () -> images(Map.of("Dockerfile", "FROM a@sha256:" + "0".repeat(64) + "\nRUN npm install c8\n")))
                .rejects("a script piped to a shell", () -> images(Map.of("Dockerfile", "FROM a@sha256:" + "0".repeat(64) + "\nRUN curl -fsSL https://x/install.sh | sh\n")))
                .accepts("a comment naming fetch.py", () -> images(Map.of("Dockerfile", "# checked by sandbox/fetch.py before use\nFROM a@sha256:" + "0".repeat(64) + "\n")))
                .accepts("the sandbox's own way", () -> images(Map.of("Dockerfile", "FROM a@sha256:" + "0".repeat(64) + " AS b\nRUN python3 /tmp/fetch.py unpack /c/y.tgz sha256 " + "a".repeat(64) + " /opt/y\nRUN pip install --require-hashes -r r.txt\nRUN npm ci --ignore-scripts\n")))
                .rejects("an unhashed requirement", () -> sandboxLocks("pytest==9.1.1\n", "{\"packages\": {}}"))
                .rejects("a package with an install script", () -> sandboxLocks("a==1 --hash=sha256:" + "0".repeat(64) + "\n", "{\"packages\": {\"node_modules/x\": {\"integrity\": \"sha512-x\", \"hasInstallScript\": true}}}"))
                .rejects("a package with no sha512", () -> sandboxLocks("a==1 --hash=sha256:" + "0".repeat(64) + "\n", "{\"packages\": {\"node_modules/x\": {\"integrity\": \"sha1-x\"}}}"))
                .rejects("an action pinned to a tag", () -> workflows(Map.of(".github/workflows/a.yml", "permissions:\n  contents: read\njobs:\n  - uses: actions/checkout@v4\n")))
                .rejects("a workflow with no permissions", () -> workflows(Map.of(".github/workflows/a.yml", "jobs:\n  - uses: actions/checkout@" + "a".repeat(40) + " # v4\n")))
                .rejects("pull_request_target", () -> workflows(Map.of(".github/workflows/a.yml", "on: pull_request_target\npermissions:\n  contents: read\n")))
                .rejects("a scheduled run", () -> workflows(Map.of(".github/workflows/a.yml", "on:\n  schedule:\n    - cron: '0 0 * * *'\npermissions:\n  contents: read\n")))
                .accepts("a pinned, read-only workflow", () -> workflows(Map.of(".github/workflows/a.yml", "permissions:\n  contents: read\njobs:\n  - uses: actions/checkout@" + "a".repeat(40) + " # v4\n")))
                .rejects("a SessionStart hook", () -> agentConfigs(Map.of(".claude/settings.json", "{\"hooks\": {\"SessionStart\": []}}"), List.of()))
                .rejects("a folder-open task", () -> agentConfigs(Map.of(".vscode/tasks.json", "{\"runOptions\": {\"runOn\": \"folderOpen\"}}"), List.of()))
                .rejects("an MCP server", () -> agentConfigs(Map.of(".mcp.json", "{\"mcpServers\": {\"x\": {}}}"), List.of()))
                .rejects("auto-approve", () -> agentConfigs(Map.of(".claude/settings.local.json", "{\"defaultMode\": \"bypassPermissions\"}"), List.of()))
                .rejects("an installed git hook", () -> agentConfigs(Map.of(), List.of("pre-commit")))
                .rejects("a worm's file", () -> agentConfigs(Map.of("tools/bun_environment.js", ""), List.of()))
                .rejects("setup.mjs in an agent folder", () -> agentConfigs(Map.of(".vscode/setup.mjs", ""), List.of()))
                .accepts("a permissions list with no hook", () -> agentConfigs(Map.of(".claude/settings.local.json", "{\"permissions\": {\"allow\": [\"Bash(gradle test:*)\"]}}"), List.of("pre-commit.sample")))
                .rejects("a jar not in the manifest", () -> vendored(MANIFEST_SAMPLE, Map.of("third_party/x/a.jar", "00", "third_party/x/b.jar", "11")))
                .rejects("a jar whose hash changed", () -> vendored(MANIFEST_SAMPLE, Map.of("third_party/x/a.jar", "ff")))
                .rejects("a manifest row with no jar", () -> vendored(MANIFEST_SAMPLE, Map.of()))
                .accepts("a jar as recorded", () -> vendored(MANIFEST_SAMPLE, Map.of("third_party/x/a.jar", "00")))
                .rejects("a repository in the build", () -> build("repositories {\n    mavenCentral()\n}\n"))
                .accepts("a comment naming repositories", () -> build("// No repositories are declared.\nplugins { id 'java' }\n"))
                .rejects("the directive missing from CLAUDE.md", () -> directive("# t\n\nnothing\n", "# g\n"))
                .rejects("the directive with no rules", () -> directive("# t\n\n" + DIRECTIVE_HEADING + "\n\nnothing\n", "# g\n"))
                .rejects("a second copy in theGhost.md", () -> directive("# t\n\n" + DIRECTIVE_HEADING + "\n\n" + RULE + "\n", "# g\n\n" + DIRECTIVE_HEADING + "\n\n- a\n"))
                .rejects("a dependency rule that names neither the how-file nor E-091", () -> directive("# t\n\n" + DIRECTIVE_HEADING + "\n\n- a\n", "# g\n"))
                .rejects("the old rule of human approval", () -> directive("# t\n\n" + DIRECTIVE_HEADING + "\n\n" + RULE + "\n- Never change a dependency without human approval.\n", "# g\n"))
                .accepts("one copy, in CLAUDE.md", () -> directive("# t\n\n" + DIRECTIVE_HEADING + "\n\n" + RULE + "\n", "# g\n\n## Supply-chain rules: in CLAUDE.md\n"));
    }

    /** Each workflow: actions pinned to a full SHA with the tag in a comment, read-only permissions, no forbidden trigger or runner. */
    static List<String> workflows(Map<String, String> files) {
        List<String> failures = new ArrayList<>();
        files.forEach((path, text) -> {
            if (!path.startsWith(".github/workflows/")) return;
            if (!Pattern.compile("(?m)^permissions:\\s*\\n\\s+contents:\\s*read\\s*$").matcher(text).find()) {
                failures.add(path + " does not start from `permissions: contents: read` at the top level");
            }
            Matcher uses = USES.matcher(text);
            while (uses.find()) {
                if (uses.group(1).startsWith("./")) continue;
                if (!PINNED.matcher(uses.group(1)).matches() || !uses.group(2).contains("#")) {
                    failures.add(path + " uses " + uses.group(1) + ": pin it to a full commit SHA, the tag in a trailing comment");
                }
            }
            for (String forbidden : FORBIDDEN_IN_WORKFLOWS) if (text.contains(forbidden)) failures.add(path + " uses " + forbidden + ", which needs Ghost's approval (E-033)");
        });
        return failures;
    }

    /** No agent or editor config runs anything, no git hook is installed, and no worm's file is present. */
    static List<String> agentConfigs(Map<String, String> files, List<String> gitHooks) {
        List<String> failures = new ArrayList<>();
        files.forEach((path, text) -> {
            String name = path.substring(path.lastIndexOf('/') + 1);
            String top = path.split("/")[0];
            if (WORM_FILES.contains(name) || name.contains("gh-token-monitor") || (AGENT_FOLDERS.contains(top) && name.equals("setup.mjs"))) {
                failures.add(path + " is a known worm's file: stop all work, change nothing, and report it; do not revoke tokens (E-033, Rule zero)");
            }
            if (top.equals(".claude") && name.startsWith("settings") && text.contains("\"hooks\"")) failures.add(path + " declares hooks (E-033)");
            if (AGENT_FOLDERS.contains(top) && (text.contains("bypassPermissions") || text.contains("enableAllProjectMcpServers") || text.contains("autoApprove"))) {
                failures.add(path + " turns on auto-approve (E-033)");
            }
            if (path.equals(".vscode/tasks.json") && text.contains("folderOpen")) failures.add(path + " runs a task on folder open (E-033)");
            boolean mcp = path.equals(".mcp.json") || path.equals(".cursor/mcp.json") || path.equals(".gemini/settings.json");
            if (mcp && text.contains("mcpServers")) failures.add(path + " declares an MCP server, which needs a decision naming it (E-033)");
        });
        for (String hook : gitHooks) if (!hook.endsWith(".sample")) failures.add(".git/hooks/" + hook + " is installed (E-033)");
        return failures;
    }

    /** Each jar under third_party/ in the manifest with the SHA-256 it has, and each manifest row's jar present. */
    static List<String> vendored(String manifest, Map<String, String> jars) {
        List<String> failures = new ArrayList<>();
        Map<String, String> recorded = new TreeMap<>();
        Matcher row = Pattern.compile("(?m)^\\| `([^`]+\\.jar)` \\|.*\\| `([0-9a-f]+)` \\|\\s*$").matcher(manifest);
        while (row.find()) recorded.put("third_party/" + row.group(1), row.group(2));
        jars.forEach((path, hash) -> {
            if (!recorded.containsKey(path)) failures.add(path + " is not in third_party/MANIFEST.md: a new dependency needs a decision and Ghost (E-091)");
            else if (!recorded.get(path).equals(hash)) failures.add(path + " has SHA-256 " + hash + ", not the " + recorded.get(path) + " recorded: it changed since it was vendored");
        });
        for (String path : recorded.keySet()) if (!jars.containsKey(path)) failures.add("third_party/MANIFEST.md lists " + path + ", which is not there");
        return failures;
    }

    /** The build reaches no repository: every dependency is a vendored file (E-021). */
    static List<String> build(String gradle) {
        List<String> failures = new ArrayList<>();
        String code = gradle.replaceAll("(?m)//.*$", "");
        Matcher declared = Pattern.compile("\\b(repositories\\s*\\{|mavenCentral\\s*\\(|google\\s*\\(|jcenter\\s*\\(|gradlePluginPortal\\s*\\(|maven\\s*\\{)").matcher(code);
        while (declared.find()) failures.add("build.gradle declares " + declared.group(1).replaceAll("\\s", "") + ": the build reaches no repository (E-021, E-033)");
        return failures;
    }

    /**
     * Every image file builds from digests and checked downloads: each `FROM` pinned by `@sha256`, each
     * fetch carrying its hash, pip only with `--require-hashes`, npm only as `npm ci --ignore-scripts`, and
     * no script piped into a shell.
     */
    static List<String> images(Map<String, String> files) {
        List<String> failures = new ArrayList<>();
        files.forEach((path, text) -> {
            String name = path.substring(path.lastIndexOf('/') + 1);
            boolean sandboxScript = path.startsWith("sandbox/") && name.endsWith(".sh");
            if (!name.equals("Dockerfile") && !name.endsWith(".Dockerfile") && !sandboxScript) return;
            String joined = String.join("\n", text.lines().filter(l -> !l.strip().startsWith("#")).toList()).replace("\\\n", " ");
            bases(path, joined, failures);
            fetches(path, joined, failures);
            installs(path, joined, failures);
        });
        return failures;
    }

    /** Every `FROM` pinned by `@sha256`. */
    private static void bases(String path, String joined, List<String> failures) {
        for (String line : joined.lines().toList()) {
            String bare = line.strip();
            if (bare.startsWith("FROM ") && !bare.matches("FROM \\S+@sha256:[0-9a-f]{64}( AS \\S+)?")) failures.add(path + ": `" + bare + "` is not pinned by digest");
        }
    }

    /** Every fetch.py call a `get` or an `unpack` with a SHA-256 or SHA-512 of the right length. */
    private static void fetches(String path, String joined, List<String> failures) {
        Matcher fetch = Pattern.compile("fetch\\.py (get|unpack) (\\S+) (\\S+) ([0-9a-f]*)").matcher(joined);
        boolean any = false;
        while (fetch.find()) {
            any = true;
            boolean hashed = fetch.group(3).equals("sha256") && fetch.group(4).length() == 64 || fetch.group(3).equals("sha512") && fetch.group(4).length() == 128;
            if (!hashed) failures.add(path + ": " + fetch.group(2) + " is fetched with no hash");
        }
        if (joined.contains("fetch.py ") && !any) failures.add(path + ": fetch.py is called without get or unpack and a hash");
    }

    /** pip only with hashes, npm only as `npm ci --ignore-scripts`, nothing piped into a shell. */
    private static void installs(String path, String joined, List<String> failures) {
        for (String command : joined.split("&&|;|\\n")) {
            String c = command.strip();
            if (c.contains("pip install") && !c.contains("--require-hashes")) failures.add(path + ": `" + c + "` installs without hashes");
            if (c.matches(".*\\bnpm (install|i|add)\\b.*") || (c.contains("npm ci") && !c.contains("--ignore-scripts"))) failures.add(path + ": `" + c + "` is not npm ci with scripts off");
            if (c.matches(".*\\|\\s*(sh|bash)\\b.*")) failures.add(path + ": `" + c + "` pipes a download into a shell");
        }
    }

    /** The sandbox's own locks: every Python requirement hashed; every npm package with a SHA-512 and no install script. */
    @SuppressWarnings("unchecked")
    static List<String> sandboxLocks(String requirements, String packageLock) {
        List<String> failures = new ArrayList<>();
        for (String line : requirements.replace("\\\n", " ").lines().toList()) {
            String bare = line.strip();
            if (!bare.isEmpty() && !bare.startsWith("#") && !bare.startsWith("-") && !bare.contains("--hash=sha256:")) failures.add("sandbox/requirements.txt: `" + bare + "` has no hash");
        }
        Map<String, Object> packages = (Map<String, Object>) ((Map<String, Object>) evaluator.rubric.Json.parse(packageLock)).get("packages");
        packages.forEach((name, value) -> {
            if (name.isEmpty()) return;
            Map<String, Object> pkg = (Map<String, Object>) value;
            if (!String.valueOf(pkg.get("integrity")).startsWith("sha512-")) failures.add("sandbox/node/package-lock.json: " + name + " has no SHA-512 integrity");
            if (Boolean.TRUE.equals(pkg.get("hasInstallScript"))) failures.add("sandbox/node/package-lock.json: " + name + " has an install script");
        });
        return failures;
    }

    /** The binding directive: its one copy in CLAUDE.md, read by every agent on entry; theGhost.md keeps none (E-073). */
    static List<String> directive(String claude, String ghost) {
        List<String> failures = new ArrayList<>();
        String rules = block(claude);
        if (rules == null || rules.lines().noneMatch(l -> l.startsWith("- "))) failures.add("CLAUDE.md has no supply-chain directive (`" + DIRECTIVE_HEADING + "`) with its rules: every agent reads them on entry (E-073)");
        else if (!rules.contains("docs/how/update-a-dependency.md") || !rules.contains("E-091")) failures.add("CLAUDE.md's dependency rule does not name docs/how/update-a-dependency.md and E-091: a dependency change is read before it comes in (E-091)");
        else if (rules.contains("human approval")) failures.add("CLAUDE.md asks for human approval of every dependency change, which E-091 replaced with the decision rule");
        if (ghost.contains(DIRECTIVE_HEADING)) failures.add("theGhost.md holds a copy of the supply-chain directive: its one copy is in CLAUDE.md (E-073)");
        return failures;
    }

    /** The directive: its heading and the bullet list under it, up to the first line that is neither. */
    private static String block(String doc) {
        int at = doc.indexOf(DIRECTIVE_HEADING);
        if (at < 0) return null;
        StringBuilder block = new StringBuilder(DIRECTIVE_HEADING);
        List<String> lines = doc.substring(at + DIRECTIVE_HEADING.length()).lines().toList();
        for (String line : lines) {
            if (!line.isBlank() && !line.startsWith("- ")) break;
            block.append('\n').append(line);
        }
        return block.toString().strip();
    }

    /** Every file of ours by path, its text (build output, Gradle's state and git's store left out). */
    private static Map<String, String> files() {
        Map<String, String> files = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(Check.ROOT)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                String rel = Check.ROOT.relativize(p).toString().replace('\\', '/');
                if (SKIPPED.contains(rel.split("/")[0]) || rel.endsWith(".jar")) continue;
                files.put(rel, Files.size(p) > 1_000_000 ? "" : new String(Files.readAllBytes(p), java.nio.charset.StandardCharsets.ISO_8859_1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    /** The git hooks installed in this clone, by name (none when there is no .git folder). */
    private static List<String> hooks() {
        Path dir = Check.ROOT.resolve(".git/hooks");
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> list = Files.list(dir)) {
            return list.map(p -> p.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every jar under third_party/ and its SHA-256. */
    private static Map<String, String> jars() {
        Map<String, String> jars = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(Check.ROOT.resolve("third_party"))) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".jar")).toList()) {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p));
                jars.put(Check.ROOT.relativize(p).toString().replace('\\', '/'), HexFormat.of().formatHex(hash));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        return jars;
    }

    private static final String RULE = "- Read it (`docs/how/update-a-dependency.md`, E-091).";
    private static final String MANIFEST_SAMPLE = "| Jar | License | Used by | Ships | SHA-256 |\n| --- | --- | --- | --- | --- |\n| `x/a.jar` | MIT | tests | no | `00` |\n";
}
