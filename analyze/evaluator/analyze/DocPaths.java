package evaluator.analyze;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A repository's instructions for agents, and whether the paths they name still exist (E-044). The docs
 * are a submission's text, so data: read, never followed; a path that would leave the repository counts
 * as missing, and a doc over the size cap is not read.
 */
final class DocPaths {
    /** Where agents look first: Claude Code, the AGENTS.md convention, Gemini, Cursor, Copilot. */
    static final List<String> AGENT_DOCS = List.of("CLAUDE.md", "AGENTS.md", "GEMINI.md", ".cursorrules", ".github/copilot-instructions.md");
    static final int MAX_DOC = 1024 * 1024;
    private static final Pattern CODE = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s#]+)(#[^)]*)?\\)");
    private static final Pattern FILE = Pattern.compile(".*\\.(md|json|java|py|ts|tsx|js|go|rs|kt|yml|yaml|toml|gradle|sh|txt|xml|html|css)$");

    /** The agent docs a repository holds, by path from its root. */
    static List<String> found(Path root) {
        return AGENT_DOCS.stream().filter(d -> Files.isRegularFile(root.resolve(d)) && !Files.isSymbolicLink(root.resolve(d))).toList();
    }

    /** The paths the docs name, each once, in the order they are first named. */
    static List<String> named(Path root, List<String> docs) throws IOException {
        Set<String> paths = new LinkedHashSet<>();
        for (String doc : docs) {
            Path file = root.resolve(doc);
            if (Files.size(file) > MAX_DOC) continue;
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher code = CODE.matcher(text);
            while (code.find()) if (path(code.group(1)) != null) paths.add(path(code.group(1)));
            Matcher link = LINK.matcher(text);
            while (link.find()) if (path(link.group(1)) != null) paths.add(path(link.group(1)));
        }
        return new ArrayList<>(paths);
    }

    /** The named paths that are not in the repository: missing, or pointing out of it. */
    static List<String> missing(Path root, List<String> named) {
        Path base = root.toAbsolutePath().normalize();
        List<String> missing = new ArrayList<>();
        for (String path : named) {
            Path target = base.resolve(path).normalize();
            if (!target.startsWith(base) || !Files.exists(target)) missing.add(path);
        }
        return missing;
    }

    /**
     * The path a code span or link names, or null when it names none: a URL, a glob, a command, an option,
     * a sentence. A path has a slash or a file's ending, and no space.
     */
    static String path(String span) {
        String s = span.strip();
        if (s.isEmpty() || s.contains(" ") || s.contains("://") || s.startsWith("-") || s.contains("*") || s.contains("$") || s.contains("<")) return null;
        if (s.startsWith("./")) s = s.substring(2);
        if (s.startsWith("/") || s.startsWith("~")) return null;
        boolean looks = (s.contains("/") && s.matches("[A-Za-z0-9._/@-]+")) || FILE.matcher(s).matches();
        return looks ? s : null;
    }

    private DocPaths() {}
}
