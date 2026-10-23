package evaluator.ingest;

import evaluator.rubric.Json;
import java.util.List;

/**
 * A repository as files: its source, each by path, language and lines, and what was set aside and
 * why. Paths are from the repository's root with `/`, in order; nothing in a file is interpreted.
 */
public record Inventory(List<Source> source, List<Aside> setAside) {
    /** A source file that counts: its path, its language, and its lines that are not blank. */
    public record Source(String path, String language, int lines) {}

    /** A source file set aside, and why (E-025). */
    public record Aside(String path, String language, String reason) {}

    public Inventory {
        source = List.copyOf(source);
        setAside = List.copyOf(setAside);
    }

    /** The inventory as JSON, the same bytes for the same repository: two spaces, fields in a fixed order, a final newline. */
    public String toJson() {
        StringBuilder json = new StringBuilder("{\n  \"source\": [");
        for (int i = 0; i < source.size(); i++) {
            Source s = source.get(i);
            json.append(i == 0 ? "\n" : ",\n").append("    {\"path\": ").append(Json.quote(s.path())).append(", \"language\": ")
                    .append(Json.quote(s.language())).append(", \"lines\": ").append(s.lines()).append('}');
        }
        json.append(source.isEmpty() ? "],\n" : "\n  ],\n").append("  \"setAside\": [");
        for (int i = 0; i < setAside.size(); i++) {
            Aside a = setAside.get(i);
            json.append(i == 0 ? "\n" : ",\n").append("    {\"path\": ").append(Json.quote(a.path())).append(", \"language\": ")
                    .append(Json.quote(a.language())).append(", \"reason\": ").append(Json.quote(a.reason())).append('}');
        }
        return json.append(setAside.isEmpty() ? "]\n}\n" : "\n  ]\n}\n").toString();
    }
}
