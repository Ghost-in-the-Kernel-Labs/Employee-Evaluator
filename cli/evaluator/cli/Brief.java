package evaluator.cli;

import evaluator.analyze.Navigability;
import evaluator.history.Commits;
import evaluator.ingest.Ingest;
import evaluator.ingest.Inventory;
import evaluator.rubric.Fact;
import evaluator.rubric.Json;
import evaluator.score.FactsText;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * What a judge is told about a repository before it reads any of it (`evaluator brief`, TODO.md 2.2b): the inventory,
 * the kernel's facts as `--facts` prints them, the newest commits and the paths of the README and agent instruction
 * files, as one JSON document, the same repository always the same bytes. It only reports what the other modules
 * counted. No file's contents are in it (a path is the most), no author name or address (E-014), and no model is
 * called (E-003): the harness in agent-runner/ reads this as data (E-013, E-017).
 */
// Decision: E-017
final class Brief {
    /** The commits listed, newest first; the brief says how many there were. */
    static final int MAX_COMMITS = 200;
    /** The longest commit subject kept, in characters; a hostile history could hold a very long one. */
    static final int MAX_SUBJECT = 200;
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private Brief() {}

    /** The brief of the repository at root; what kept a part from being read is said on `notes`. */
    static String render(Path root, PrintStream notes) throws IOException {
        Inventory inventory = Ingest.read(root);
        // Tests are not run: a brief needs no sandbox, and a judge is shown facts, not a run's log.
        List<Fact> facts = Pipeline.facts(root, new Pipeline.Options(null, null, null), notes);
        List<Commits.Commit> commits = Files.isDirectory(root.resolve(".git")) ? Commits.read(root) : List.of();
        StringBuilder json = new StringBuilder("{\n  \"inventory\": ").append(inventory.toJson().stripTrailing().replace("\n", "\n  ")).append(",\n");
        json.append("  \"facts\": ").append(facts(FactsText.render(facts))).append(",\n");
        json.append("  \"commitsTotal\": ").append(commits.size()).append(",\n");
        json.append("  \"commits\": ").append(commits(commits)).append(",\n");
        json.append("  \"readme\": ").append(strings(Navigability.readmes(root))).append(",\n");
        json.append("  \"agentInstructions\": ").append(strings(Navigability.agentDocs(root))).append("\n}\n");
        return json.toString();
    }

    /** `--facts`' lines as an object, in their order: a number stays a number, anything else is a string. */
    static String facts(String lines) {
        List<String> members = new ArrayList<>();
        for (String line : lines.lines().toList()) {
            int space = line.indexOf(' ');
            String name = line.substring(0, space), value = line.substring(space + 1);
            members.add("\n    " + Json.quote(name) + ": " + (NUMBER.matcher(value).matches() ? value : Json.quote(value)));
        }
        return members.isEmpty() ? "{}" : "{" + String.join(",", members) + "\n  }";
    }

    /** The newest MAX_COMMITS of these commits (oldest first), newest first: short hash, UTC date, subject, files changed. */
    static String commits(List<Commits.Commit> oldestFirst) {
        List<String> rows = new ArrayList<>();
        for (int i = oldestFirst.size() - 1; i >= 0 && rows.size() < MAX_COMMITS; i--) {
            Commits.Commit c = oldestFirst.get(i);
            String message = c.message();
            int end = message.indexOf('\n');
            String subject = end < 0 ? message : message.substring(0, end).stripTrailing();
            if (subject.codePointCount(0, subject.length()) > MAX_SUBJECT) subject = subject.substring(0, subject.offsetByCodePoints(0, MAX_SUBJECT));
            rows.add("\n    {\"hash\": " + Json.quote(c.hash().substring(0, Math.min(7, c.hash().length()))) + ", \"date\": "
                    + Json.quote(c.at().atZone(ZoneOffset.UTC).toLocalDate().toString()) + ", \"subject\": " + Json.quote(subject)
                    + ", \"filesChanged\": " + c.changes().size() + "}");
        }
        return rows.isEmpty() ? "[]" : "[" + String.join(",", rows) + "\n  ]";
    }

    private static String strings(List<String> paths) {
        return "[" + paths.stream().map(Json::quote).collect(Collectors.joining(", ")) + "]";
    }
}
