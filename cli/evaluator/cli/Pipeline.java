package evaluator.cli;

import evaluator.analyze.Complexity;
import evaluator.analyze.Coupling;
import evaluator.analyze.Dependencies;
import evaluator.analyze.Duplication;
import evaluator.analyze.Navigability;
import evaluator.analyze.Sandbox;
import evaluator.analyze.TestFiles;
import evaluator.analyze.TestRunner;
import evaluator.github.Authorship;
import evaluator.github.HttpGitHub;
import evaluator.history.Commits;
import evaluator.history.Shape;
import evaluator.ingest.Ingest;
import evaluator.ingest.Inventory;
import evaluator.ingest.Size;
import evaluator.rubric.Fact;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Every module's facts about one repository, in one order (E-054): what it holds (`ingest/`), its code
 * (`analyze/`), its own tests run in the sandbox unless told not to, its history when it has one
 * (`history/`), and what GitHub verifies when a repository and an applicant are named (`github/`).
 */
// Decision: E-054
final class Pipeline {
    /** What to read besides the code: the sandbox to run its tests in, and the GitHub repository and applicant; each null for none. */
    record Options(Sandbox sandbox, String github, String applicant) {}

    private Pipeline() {}

    /** The repository's facts; what kept a part from being read is said on `notes`. */
    static List<Fact> facts(Path root, Options options, PrintStream notes) throws IOException {
        Inventory inventory = Ingest.read(root);
        List<Fact> facts = new ArrayList<>(Size.facts(inventory));
        facts.addAll(code(root, inventory));
        if (options.sandbox() != null) {
            TestRunner.Result run = TestRunner.run(root, inventory, options.sandbox());
            facts.addAll(run.facts());
            run.notMeasured().forEach(why -> notes.println("tests not measured: " + why));
        }
        if (Files.isDirectory(root.resolve(".git"))) facts.addAll(Shape.facts(Commits.read(root), Commits.paths(root), TestFiles::isTest));
        else notes.println("history not read: no .git");
        if (options.github() != null) facts.addAll(github(options));
        return facts;
    }

    /** The code's facts: complexity, the dependency graph, coupling, duplication, the tests' share, the checklist. */
    static List<Fact> code(Path root, Inventory inventory) throws IOException {
        Complexity.Report report = Complexity.measure(root, inventory);
        Dependencies.Graph graph = Dependencies.graph(report, inventory);
        List<Fact> facts = new ArrayList<>(Complexity.facts(report, inventory));
        facts.addAll(Dependencies.facts(graph, report, Dependencies.TOOL));
        facts.addAll(Coupling.facts(graph));
        Duplication.fact(report).ifPresent(facts::add);
        TestFiles.ratio(inventory).ifPresent(facts::add);
        facts.addAll(Navigability.facts(root, inventory, report, graph));
        return facts;
    }

    /** Layer 2: commit verification and push attribution for `owner/repo` and its applicant. */
    private static List<Fact> github(Options options) throws IOException {
        String[] name = options.github().split("/", 2);
        return Authorship.facts(HttpGitHub.fromEnvironment(), name[0], name[1], options.applicant());
    }
}
