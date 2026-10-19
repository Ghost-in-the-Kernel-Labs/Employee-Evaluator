package evaluator.analyze;

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
}
