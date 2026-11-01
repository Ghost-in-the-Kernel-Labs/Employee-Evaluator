import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the dependency-update posture (E-091) to the repository. The record: every artifact the repository pins, whatever its
 * registry, has one row in `third_party/REVIEWED.md` naming the version or hash pinned and the last version reviewed, and no row
 * outlives its pin. A pin that changes therefore changes its row in the same commit, which is where a review leaves its record.
 * The two agents (`.claude/agents/`, E-091): each file's SHA-256 is pinned here, its `tools:` line is checked as well, and no third file may be there.
 * The scan: `dependency-review/` is Python, standard library only, outside the Java module map as `agent-runner/` is; its tests
 * (synthetic packages made in a temporary folder) run here as `agent-runner/`'s do, and a failing one, or none found, fails the build.
 */
// Decision: E-091
final class DependencyReviewTest {
    /**
     * The two agent files, each pinned by the SHA-256 of its bytes, and the one `tools:` line each may have (its privileges). A file
     * that changes fails the build until Ghost approves the change here; nothing else may be under `.claude/agents/` (E-091).
     * patch-reviewer reads files and nothing else; reputation-checker has the web and nothing else, and never sees a file of the repository.
     */
    static final Map<String, String> AGENT_PINS = Map.of(
            ".claude/agents/patch-reviewer.md", "5cb61c16cca054598a142e964d2ff0c5ebc7e4ad253875e074bd80264e2ca70a",
            ".claude/agents/reputation-checker.md", "5949cef496dab20aadfdac534310891b499ce3996581566458ff43d10f45f747");
    static final Map<String, String> AGENT_TOOLS = Map.of(
            ".claude/agents/patch-reviewer.md", "tools: Read, Grep, Glob",
            ".claude/agents/reputation-checker.md", "tools: WebSearch, WebFetch");
    static final String RECORD = "third_party/REVIEWED.md";
    /** How many hex digits of a hash or digest a row carries: enough to tell two pins apart, short enough to read. */
    static final int PREFIX = 12;
    private static final Pattern ROW = Pattern.compile("(?m)^\\| (maven|npm|pypi|tarball|image|action) \\| (\\S+) \\| (\\S+) \\| (.+?) \\|\\s*$");
    private static final Pattern JAR = Pattern.compile("(?m)^\\| `([^`]+\\.jar)` \\|.*\\| `([0-9a-f]+)` \\|\\s*$");
    private static final Pattern REQUIREMENT = Pattern.compile("(?m)^([A-Za-z0-9_.-]+)==(\\S+)");
    private static final Pattern FETCH = Pattern.compile("fetch\\.py get (\\S+) sha\\d+ ([0-9a-f]+) ");
    private static final Pattern FROM = Pattern.compile("(?m)^FROM (\\S+)@sha256:([0-9a-f]{64})");
    private static final Pattern ACTION = Pattern.compile("(?m)^\\s*-?\\s*uses:\\s*([^@\\s./][^@\\s]*)@([0-9a-f]{40})");

    public static void main(String[] args) {
        run().exit();
    }

    /** The record against the pins, and a broken sample of each rule, which must be caught. */
    static Check run() {
        Map<String, String> files = new TreeMap<>();
        for (String path : List.of("third_party/MANIFEST.md", "sandbox/requirements.txt", "sandbox/node/package-lock.json", "sandbox/prefetch.sh", "sandbox/Dockerfile", "Dockerfile", ".github/workflows/ci.yml")) {
            files.put(path, Check.read(path));
        }
        String record = Check.read(RECORD);
        TreeSet<String> pins = pins(files);
        String row = "| pypi | pytest | 9.1.1 | not yet |\n";
        Check check = new Check("dependency review")
                .repository(reviewed(record, pins))
                .repository(agents(agentFiles(), AGENT_PINS, AGENT_TOOLS))
                .rejects("an agent file that changed", () -> agents(Map.of("a.md", bytes("---\ntools: Read\n---\nx\n")), Map.of("a.md", sha(bytes("---\ntools: Read\n---\ny\n"))), Map.of("a.md", "tools: Read")))
                .rejects("an agent file with a shell among its tools", () -> agents(Map.of("a.md", bytes("---\ntools: Read, Bash\n---\n")), Map.of("a.md", sha(bytes("---\ntools: Read, Bash\n---\n"))), Map.of("a.md", "tools: Read")))
                .rejects("an agent file with no tools line", () -> agents(Map.of("a.md", bytes("---\nname: a\n---\n")), Map.of("a.md", sha(bytes("---\nname: a\n---\n"))), Map.of("a.md", "tools: Read")))
                .rejects("an agent file that is gone", () -> agents(Map.of(), Map.of("a.md", "0"), Map.of("a.md", "tools: Read")))
                .rejects("a third agent file", () -> agents(Map.of("a.md", bytes("---\ntools: Read\n---\n"), "b.md", bytes("x")), Map.of("a.md", sha(bytes("---\ntools: Read\n---\n"))), Map.of("a.md", "tools: Read")))
                .accepts("the pinned file as it is", () -> agents(Map.of("a.md", bytes("---\ntools: Read\n---\n")), Map.of("a.md", sha(bytes("---\ntools: Read\n---\n"))), Map.of("a.md", "tools: Read")))
                .rejects("a pin with no row", () -> reviewed("", new TreeSet<>(List.of("pypi | pytest | 9.1.1"))))
                .rejects("a row whose pin is gone", () -> reviewed(row, new TreeSet<>()))
                .rejects("a row for another version than the pin", () -> reviewed(row, new TreeSet<>(List.of("pypi | pytest | 9.2.0"))))
                .rejects("a row that says nothing of its review", () -> reviewed("| pypi | pytest | 9.1.1 |  |\n", new TreeSet<>(List.of("pypi | pytest | 9.1.1"))))
                .rejects("two rows for one pin", () -> reviewed(row + row, new TreeSet<>(List.of("pypi | pytest | 9.1.1"))))
                .accepts("a row for each pin", () -> reviewed(row, new TreeSet<>(List.of("pypi | pytest | 9.1.1"))))
                .accepts("a jar read as one pin", () -> sampleCount(JAR_MANIFEST, 1))
                .accepts("a jar, a requirement, a tarball, an image and an action each read as a pin", () -> sampleCount(SAMPLE_FILES, 5));
        if (!AgentRunnerTest.python3Present()) {
            System.out.println("dependency review PENDING: python3 is absent, so the scan's tests did not run (the pinned image has it)");
            return check;
        }
        return check.repository(AgentRunnerTest.check(AgentRunnerTest.discover(Check.ROOT.resolve("dependency-review"))));
    }

    /** Each agent file with the pinned hash and exactly the one tools line, and no other file beside it. */
    static List<String> agents(Map<String, byte[]> files, Map<String, String> pins, Map<String, String> tools) {
        List<String> failures = new ArrayList<>();
        pins.forEach((path, hash) -> {
            byte[] bytes = files.get(path);
            if (bytes == null) {
                failures.add(path + " is missing: it is one of the two read-only agents (E-091)");
                return;
            }
            if (!sha(bytes).equals(hash)) failures.add(path + " differs from its pinned SHA-256: its tools line is its privileges, so a change is Ghost's to approve (E-091)");
            List<String> lines = new String(bytes, StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("tools:")).toList();
            if (!lines.equals(List.of(tools.get(path)))) failures.add(path + " has the tools line(s) " + lines + ", not exactly [" + tools.get(path) + "]");
        });
        files.keySet().stream().filter(path -> !pins.containsKey(path)).forEach(path -> failures.add(path + " is under .claude/agents/ and is not pinned: Ghost approved two agent files only (E-091)"));
        return failures;
    }

    /** Every file under `.claude/agents/`, by path from the root, as bytes. */
    private static Map<String, byte[]> agentFiles() {
        Map<String, byte[]> files = new TreeMap<>();
        Path dir = Check.ROOT.resolve(".claude/agents");
        if (!Files.isDirectory(dir)) return files;
        try (var list = Files.walk(dir)) {
            for (Path p : list.filter(Files::isRegularFile).toList()) files.put(Check.ROOT.relativize(p).toString().replace('\\', '/'), Files.readAllBytes(p));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every pin of the repository as `registry | artifact | pinned`, from the files that hold them. */
    static TreeSet<String> pins(Map<String, String> files) {
        TreeSet<String> pins = new TreeSet<>();
        Matcher jar = JAR.matcher(files.getOrDefault("third_party/MANIFEST.md", ""));
        while (jar.find()) pins.add("maven | " + jar.group(1) + " | " + jar.group(2).substring(0, PREFIX));
        Matcher requirement = REQUIREMENT.matcher(files.getOrDefault("sandbox/requirements.txt", ""));
        while (requirement.find()) pins.add("pypi | " + requirement.group(1) + " | " + requirement.group(2));
        String lock = files.getOrDefault("sandbox/node/package-lock.json", "");
        if (!lock.isBlank()) npm(lock, pins);
        Matcher fetch = FETCH.matcher(files.getOrDefault("sandbox/prefetch.sh", ""));
        while (fetch.find()) pins.add("tarball | " + fetch.group(1).substring(fetch.group(1).lastIndexOf('/') + 1) + " | " + fetch.group(2).substring(0, PREFIX));
        for (String dockerfile : List.of("sandbox/Dockerfile", "Dockerfile")) {
            Matcher from = FROM.matcher(files.getOrDefault(dockerfile, ""));
            while (from.find()) pins.add("image | " + from.group(1) + " | " + from.group(2).substring(0, PREFIX));
        }
        for (Map.Entry<String, String> file : files.entrySet()) {
            if (!file.getKey().startsWith(".github/workflows/")) continue;
            Matcher action = ACTION.matcher(file.getValue());
            while (action.find()) pins.add("action | " + action.group(1) + " | " + action.group(2).substring(0, PREFIX));
        }
        return pins;
    }

    /** The npm tree: each package of the lockfile with the version it pins, the root (the empty name) left out. */
    @SuppressWarnings("unchecked")
    private static void npm(String lock, TreeSet<String> pins) {
        Map<String, Object> packages = (Map<String, Object>) ((Map<String, Object>) evaluator.rubric.Json.parse(lock)).get("packages");
        packages.forEach((name, value) -> {
            if (!name.isEmpty()) pins.add("npm | " + name.replaceFirst("^node_modules/", "") + " | " + ((Map<String, Object>) value).get("version"));
        });
    }

    /** Each pin has exactly one row naming it and a last-reviewed cell, and each row names a pin that is still held. */
    static List<String> reviewed(String record, TreeSet<String> pins) {
        List<String> failures = new ArrayList<>();
        Map<String, Integer> rows = new TreeMap<>();
        Matcher row = ROW.matcher(record);
        while (row.find()) {
            String key = row.group(1) + " | " + row.group(2) + " | " + row.group(3);
            rows.merge(key, 1, Integer::sum);
            if (row.group(4).isBlank()) failures.add(RECORD + ": the row " + key + " says nothing of its last review");
        }
        for (String pin : pins) if (!rows.containsKey(pin)) failures.add(RECORD + " has no row for " + pin + ": a pin that changes changes its row in the same commit (E-091)");
        rows.forEach((key, count) -> {
            if (!pins.contains(key)) failures.add(RECORD + " has a row for " + key + ", which the repository does not pin");
            if (count > 1) failures.add(RECORD + " has " + count + " rows for " + key);
        });
        return failures;
    }

    /** A failure when the pins read from the sample files are not `expected` in number. */
    private static List<String> sampleCount(Map<String, String> files, int expected) {
        int found = pins(files).size();
        return found == expected ? List.of() : List.of(found + " pins read, not " + expected);
    }

    private static final Map<String, String> JAR_MANIFEST = Map.of("third_party/MANIFEST.md", "| Jar |\n| `x/a.jar` | MIT | tests | no | `" + "a".repeat(64) + "` |\n");
    private static final Map<String, String> SAMPLE_FILES = Map.of(
            "third_party/MANIFEST.md", JAR_MANIFEST.get("third_party/MANIFEST.md"),
            "sandbox/requirements.txt", "pytest==9.1.1 \\\n    --hash=sha256:" + "b".repeat(64) + "\n",
            "sandbox/prefetch.sh", "python3 fetch.py get https://x/y/z-1.tar.xz sha256 " + "c".repeat(64) + " cache/z.tar.xz\n",
            "sandbox/Dockerfile", "FROM python:3.12-slim@sha256:" + "d".repeat(64) + " AS b\n",
            ".github/workflows/a.yml", "  - uses: actions/checkout@" + "e".repeat(40) + " # v4\n  - uses: ./local\n");
}
