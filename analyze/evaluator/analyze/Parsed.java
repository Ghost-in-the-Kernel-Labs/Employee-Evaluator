package evaluator.analyze;

import java.util.List;

/** What an adapter made of one file: its functions' measures, or why it could not read it (null when it could). */
public record Parsed(List<Measure> functions, String unread) {
    public Parsed {
        functions = List.copyOf(functions);
    }

    /** A file the adapter could not read, and why. */
    public static Parsed unread(String why) {
        return new Parsed(List.of(), why);
    }
}
