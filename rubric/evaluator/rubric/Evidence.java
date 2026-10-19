package evaluator.rubric;

import java.util.List;

/**
 * Where a fact was read: the tool and its version (or the judging prompt's hash), and the sources it
 * read (paths and lines, commit hashes, the sampled files).
 */
public record Evidence(String tool, List<String> sources) {
    public Evidence {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    /** Whether there is anything here a person could check. */
    public boolean isEmpty() {
        return tool == null || tool.isBlank() || sources.isEmpty();
    }
}
