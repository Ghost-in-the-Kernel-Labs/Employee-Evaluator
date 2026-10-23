package evaluator.analyze;

import java.util.List;

/** The language adapters, one per language they read, and the lookup of the one for a language (E-031). */
// Decision: E-031
final class Adapters {
    /** The adapters, in the order facts are made. */
    static final List<Adapter> ALL = List.of(new JavaAdapter(), new TypeScriptAdapter(), new JavaScriptAdapter(), new PythonAdapter());

    private Adapters() {}

    /** The adapter that reads a language, or null when none does. */
    static Adapter forLanguage(String language) {
        for (Adapter adapter : ALL) if (adapter.reads(language)) return adapter;
        return null;
    }
}
