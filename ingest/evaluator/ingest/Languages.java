package evaluator.ingest;

import java.util.Map;

/** A file's language from its name's ending; a file whose ending names none is not source (docs, data, images). */
final class Languages {
    private static final Map<String, String> BY_ENDING = Map.ofEntries(
            Map.entry("java", "Java"), Map.entry("kt", "Kotlin"), Map.entry("kts", "Kotlin"), Map.entry("scala", "Scala"),
            Map.entry("groovy", "Groovy"), Map.entry("ts", "TypeScript"), Map.entry("tsx", "TypeScript"),
            Map.entry("mts", "TypeScript"), Map.entry("cts", "TypeScript"), Map.entry("js", "JavaScript"),
            Map.entry("jsx", "JavaScript"), Map.entry("mjs", "JavaScript"), Map.entry("cjs", "JavaScript"),
            Map.entry("py", "Python"), Map.entry("go", "Go"), Map.entry("rs", "Rust"), Map.entry("c", "C"),
            Map.entry("h", "C"), Map.entry("cc", "C++"), Map.entry("cpp", "C++"), Map.entry("cxx", "C++"),
            Map.entry("hpp", "C++"), Map.entry("cs", "C#"), Map.entry("fs", "F#"), Map.entry("rb", "Ruby"),
            Map.entry("php", "PHP"), Map.entry("swift", "Swift"), Map.entry("m", "Objective-C"), Map.entry("dart", "Dart"),
            Map.entry("lua", "Lua"), Map.entry("ex", "Elixir"), Map.entry("exs", "Elixir"), Map.entry("erl", "Erlang"),
            Map.entry("hs", "Haskell"), Map.entry("ml", "OCaml"), Map.entry("clj", "Clojure"), Map.entry("r", "R"),
            Map.entry("jl", "Julia"), Map.entry("zig", "Zig"), Map.entry("sh", "Shell"), Map.entry("bash", "Shell"),
            Map.entry("ps1", "PowerShell"), Map.entry("sql", "SQL"), Map.entry("vue", "Vue"), Map.entry("svelte", "Svelte"),
            Map.entry("cob", "COBOL"), Map.entry("cbl", "COBOL"));

    private Languages() {}

    /** The language of a file by its name, or null when it is not source. */
    static String of(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? null : BY_ENDING.get(name.substring(dot + 1).toLowerCase());
    }
}
