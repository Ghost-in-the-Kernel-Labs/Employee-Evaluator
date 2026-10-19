package evaluator.rubric;

/** What a fact is about: the whole repository, a file, a function, a commit or a module, by its id. */
public record Subject(Kind kind, String id) {
    /** The things a fact can be about. */
    public enum Kind { REPOSITORY, FILE, FUNCTION, COMMIT, MODULE }

    public Subject {
        if (kind == null || id == null || id.isBlank()) throw new IllegalArgumentException("a subject has a kind and an id");
    }

    /** The whole repository, whose id is its root. */
    public static Subject repository() {
        return new Subject(Kind.REPOSITORY, ".");
    }

    /** One file, by its path from the repository's root. */
    public static Subject file(String path) {
        return new Subject(Kind.FILE, path);
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase() + ":" + id;
    }
}
