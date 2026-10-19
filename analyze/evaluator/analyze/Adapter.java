package evaluator.analyze;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One language's reader: a source file's functions, each with its complexity. An adapter only parses;
 * it never compiles, loads or runs what it reads (E-013).
 */
public interface Adapter {
    /** Whether this adapter reads files of the inventory's language name ("Java", "Python"). */
    boolean reads(String language);

    /** The functions of one source file, in source order; a file that does not parse is said, not guessed at. */
    Parsed parse(String path, String text);

    /** The tool every fact from this adapter names as its evidence. */
    String tool();

    /**
     * The repository's files an import of `from` leads to, among `files` (the inventory's source paths);
     * none when it leads outside the repository. Resolved by path, never by running anything.
     */
    List<String> resolve(Import imported, String from, Set<String> files);

    /** The files whose path ends with this relative path (a whole segment at a time), in path order. */
    static List<String> endingWith(String suffix, Set<String> files) {
        List<String> found = new ArrayList<>();
        for (String file : files) if (file.equals(suffix) || file.endsWith("/" + suffix)) found.add(file);
        return found;
    }
}
