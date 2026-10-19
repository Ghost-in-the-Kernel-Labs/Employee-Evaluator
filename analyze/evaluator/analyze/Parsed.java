package evaluator.analyze;

import java.util.List;

/** What an adapter made of one file: its functions' measures and its imports, or why it could not read it (null when it could). */
public record Parsed(List<Measure> functions, List<Import> imports, String unread) {
    public Parsed {
        functions = List.copyOf(functions);
        imports = List.copyOf(imports);
    }

    /** A file the adapter could not read, and why. */
    public static Parsed unread(String why) {
        return new Parsed(List.of(), List.of(), why);
    }
}
