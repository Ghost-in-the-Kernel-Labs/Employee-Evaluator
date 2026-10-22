import evaluator.rubric.Json;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes docs/MODULES.md from docs/modules.json (TODO.md 1.11, AGT-06): the readable form of the module map,
 * never edited by hand. `gradle modulesDoc` writes it; test/ModulesTest.java fails when it is stale.
 */
final class ModulesDoc {
    static final String PATH = "docs/MODULES.md";
    static final String SOURCE = "docs/modules.json";

    public static void main(String[] args) throws IOException {
        Files.writeString(Repo.ROOT.resolve(PATH), render(Repo.read(SOURCE)));
    }

    /** The document for this JSON: the intro, the pipeline, each module, then the rules. */
    @SuppressWarnings("unchecked")
    static String render(String json) {
        Map<String, Object> root = (Map<String, Object>) Json.parse(json);
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) root.get("modules");
        StringBuilder out = new StringBuilder("""
                # Modules: the kernel by folder

                <!-- Written by `gradle modulesDoc` from docs/modules.json; edit the JSON, never this file. -->

                An agent starts at the module level, not the function level (Suite Builder's finding, E-004). Each
                folder of the kernel: what it is for, what goes in and comes out, what it may import, what imports it,
                and where to start reading. For a task, `CLAUDE.md` says which modules to open and what proves a
                change. `test/ModulesTest.java` holds every import in the source to the JSON, and this file to it.

                """);
        out.append(root.get("pipeline")).append("\n");
        modules.forEach((name, module) -> out.append(module(name, module, modules)));
        out.append("\n## Rules\n\n");
        for (Object rule : (List<Object>) root.get("rules")) {
            Map<String, Object> r = (Map<String, Object>) rule;
            out.append("- **").append(r.get("id")).append("** ").append(r.get("rule")).append(" (").append(r.get("source")).append(")\n");
        }
        return out.toString();
    }

    /** One module's section; what imports it is read off the other modules' mayImport. */
    @SuppressWarnings("unchecked")
    private static String module(String name, Map<String, Object> module, Map<String, Map<String, Object>> modules) {
        List<String> importedBy = new ArrayList<>();
        modules.forEach((other, m) -> {
            if (((List<Object>) m.get("mayImport")).contains(name)) importedBy.add("`" + other + "`");
        });
        List<Object> may = (List<Object>) module.get("mayImport");
        return "\n### `" + name + "/`\n"
                + "- **Purpose:** " + module.get("purpose") + "\n"
                + "- **Input:** " + module.get("inputs") + ". **Output:** " + module.get("outputs") + ".\n"
                + "- **May import:** " + (may.isEmpty() ? "nothing of ours" : String.join(", ", may.stream().map(m -> "`" + m + "`").toList())) + ".\n"
                + "- **Imported by:** " + (importedBy.isEmpty() ? "nothing" : String.join(", ", importedBy)) + ".\n"
                + "- **Start at:** " + module.get("startAt") + "\n";
    }

    /** Whether docs/MODULES.md is what its generator would write. */
    static List<String> check(String doc, String json) {
        return doc.equals(render(json)) ? List.of() : List.of(PATH + " is stale or edited by hand: run `gradle modulesDoc` and commit it");
    }
}
