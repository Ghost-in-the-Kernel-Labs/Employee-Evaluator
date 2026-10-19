package evaluator.ingest;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.util.List;
import java.util.TreeSet;

/** The Size facts of an inventory: what the applicant's own source amounts to, vendored and generated code apart. */
// Decision: E-026
public final class Size {
    /** The tool every Size fact names as its evidence. */
    public static final String TOOL = "evaluator ingest 1";

    private Size() {}

    /** Source lines, source files and source folders, counted; and the files set aside. Each cites the paths it counted. */
    public static List<Fact> facts(Inventory inventory) {
        List<String> counted = inventory.source().stream().map(Inventory.Source::path).toList();
        List<String> aside = inventory.setAside().stream().map(Inventory.Aside::path).toList();
        TreeSet<String> folders = new TreeSet<>();
        for (String path : counted) folders.add(path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : ".");
        int lines = inventory.source().stream().mapToInt(Inventory.Source::lines).sum();
        return List.of(
                fact("SIZE.source_lines", lines, counted),
                fact("SIZE.source_files", counted.size(), counted),
                fact("SIZE.source_folders", folders.size(), List.copyOf(folders)),
                fact("SIZE.set_aside_files", aside.size(), aside));
    }

    /** A repository-wide count; with nothing counted, the evidence is that nothing was found. */
    private static Fact fact(String kind, int value, List<String> sources) {
        List<String> read = sources.isEmpty() ? List.of("(none found)") : sources;
        return Fact.measured(kind, Subject.repository(), value, new Evidence(TOOL, read));
    }
}
