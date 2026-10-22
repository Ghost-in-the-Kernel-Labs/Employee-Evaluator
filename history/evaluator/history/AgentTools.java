package evaluator.history;

import evaluator.history.Commits.Commit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which agent tools left their hand on a history, and the evidence for each (E-053): every commit whose
 * Co-Authored-By trailer names one, and every instruction file only that tool reads. AGENTS.md is read by
 * several and names none. A trailer naming Anthropic counts as Claude, OpenAI or ChatGPT as Codex.
 */
// Decision: E-053
final class AgentTools {
    private static final Pattern TRAILER = Pattern.compile("(?im)^co-authored-by:(.*)$");
    private static final Pattern NAMED = Pattern.compile("(?i)\\b(claude|anthropic|copilot|cursor|codex|openai|chatgpt|gemini|devin|aider|windsurf)\\b");
    /** A name in a trailer as the tool it means. */
    static final Map<String, String> SAME_TOOL = Map.of("anthropic", "claude", "openai", "codex", "chatgpt", "codex");
    /** An instruction file, by its name in lower case, as the one tool that reads it. */
    static final Map<String, String> FILES = Map.of("claude.md", "claude", "gemini.md", "gemini", ".cursorrules", "cursor", "copilot-instructions.md", "copilot");

    private AgentTools() {}

    /** Each tool found, by its name, with its evidence: `commit <hash>` once per commit, and each file's path. */
    static Map<String, List<String>> found(List<Commit> commits, List<String> pathsAtHead) {
        Map<String, List<String>> tools = new TreeMap<>();
        for (Commit c : commits) for (String tool : named(c.message())) tools.computeIfAbsent(tool, k -> new ArrayList<>()).add("commit " + c.hash());
        for (String path : pathsAtHead) {
            String tool = fileTool(path);
            if (tool != null) tools.computeIfAbsent(tool, k -> new ArrayList<>()).add(path);
        }
        return tools;
    }

    /** The tools a message's Co-Authored-By trailers name, each once. */
    static Set<String> named(String message) {
        Set<String> tools = new TreeSet<>();
        Matcher trailer = TRAILER.matcher(message);
        while (trailer.find()) {
            Matcher name = NAMED.matcher(trailer.group(1));
            while (name.find()) {
                String found = name.group(1).toLowerCase(Locale.ROOT);
                tools.add(SAME_TOOL.getOrDefault(found, found));
            }
        }
        return tools;
    }

    /** The one tool that reads an instruction file at this path, or null (AGENTS.md, and any other file). */
    static String fileTool(String path) {
        if (path.startsWith(".cursor/rules/")) return "cursor";
        return FILES.get(path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT));
    }
}
