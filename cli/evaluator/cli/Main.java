package evaluator.cli;

import evaluator.analyze.DockerSandbox;
import evaluator.rubric.Fact;
import evaluator.rubric.Produced;
import evaluator.rubric.ScoreCard;
import evaluator.score.CardJson;
import evaluator.score.CardMarkdown;
import evaluator.score.Cards;
import evaluator.score.FactsText;
import evaluator.score.Formulas;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * The command line: turns what was typed into a command and prints what it did. The work is in the
 * other modules; this one only names it. `evaluator score <path>` is the scorer's command (phase 1); `evaluator brief <path>`
 * (phase 2) prints the kernel's facts for a judge.
 */
// Decision: E-010
public final class Main {
    /** The kernel's version, as a card says what made it. */
    static final String VERSION = "0.1";
    static final String USAGE = """
            usage: evaluator score <path-to-repository> [--markdown] [--no-run] [--facts] [--github <owner/repo> --applicant <login>] [--date <yyyy-mm-dd>]
              Scores a repository on the seven metrics of docs/SCORE-CARD.md and prints its score card, as JSON
              or with --markdown as Markdown. --no-run leaves its tests unrun (no sandbox); --github with
              --applicant reads what GitHub verifies (needs EVALUATOR_GITHUB_TOKEN); --date fixes the card's date.
              --facts prints instead the values behind the card, one `name value` line each, and goes with neither
              --markdown nor --date.
            usage: evaluator brief <path-to-repository>
              Prints, as one JSON document, what a judge is told before it reads any file: the inventory, the facts
              `score --facts` prints, the newest 200 commits (short hash, date, subject, files changed) and the paths
              of any README and agent instruction files. No file's contents, no author; tests are not run.
            """;

    /** GitHub's own rule for owner/repo: an owner of letters, digits and hyphens; a repository not starting with a dot (so never `..`). */
    static final String GITHUB_NAME = "[A-Za-z0-9-]+/[A-Za-z0-9_-][A-Za-z0-9_.-]*";

    /** What was typed, read. */
    record Command(Path path, boolean markdown, boolean facts, Pipeline.Options options, String date) {}

    public static void main(String[] args) {
        System.exit(run(List.of(args), System.out, System.err));
    }

    /** The exit status of a command: 0 done, 1 refused. */
    static int run(List<String> args, PrintStream out, PrintStream err) {
        if (!args.isEmpty() && args.get(0).equals("brief")) return brief(args, out, err);
        Command command;
        try {
            command = parse(args);
        } catch (IllegalArgumentException e) {
            err.println("evaluator: " + e.getMessage());
            err.print(USAGE);
            return 1;
        }
        return score(command, out, err);
    }

    /** `brief <path>`: the repository's brief printed (TODO.md 2.2b); a wrong use is refused with the usage, as score's are. */
    static int brief(List<String> args, PrintStream out, PrintStream err) {
        if (args.size() != 2 || args.get(1).startsWith("--")) {
            err.println("evaluator: brief takes one path and no option");
            err.print(USAGE);
            return 1;
        }
        Path path = Path.of(args.get(1));
        if (!Files.isDirectory(path)) {
            err.println("evaluator brief: not a folder: " + path);
            return 1;
        }
        try {
            out.print(Brief.render(path, err));
            return 0;
        } catch (IOException | UncheckedIOException e) {
            err.println("evaluator brief: " + e.getMessage());
            return 1;
        }
    }

    /** The card of the repository the command names, printed as it asks. */
    static int score(Command command, PrintStream out, PrintStream err) {
        if (!Files.isDirectory(command.path())) {
            err.println("evaluator score: not a folder: " + command.path());
            return 1;
        }
        try {
            List<Fact> facts = Pipeline.facts(command.path(), command.options(), err);
            if (command.facts()) {
                out.print(FactsText.render(facts));
                return 0;
            }
            ScoreCard card = Cards.card(facts, List.of(), Formulas.phaseOne(), new Produced(VERSION, command.date(), Formulas.rubricHash()));
            out.print(command.markdown() ? CardMarkdown.render(card) : CardJson.render(card));
            return 0;
        } catch (IOException | UncheckedIOException e) {
            err.println("evaluator score: " + e.getMessage());
            return 1;
        }
    }

    /** The command typed; anything else is refused with what was wrong. */
    static Command parse(List<String> args) {
        if (args.size() < 2 || !args.get(0).equals("score")) throw new IllegalArgumentException("the one command is score <path>");
        Path path = null;
        boolean markdown = false, run = true, facts = false, dateTyped = false;
        String github = null, applicant = null, date = LocalDate.now(ZoneOffset.UTC).toString();
        for (int i = 1; i < args.size(); i++) {
            String a = args.get(i);
            switch (a) {
                case "--markdown" -> markdown = true;
                case "--no-run" -> run = false;
                case "--facts" -> facts = true;
                case "--github" -> github = value(args, ++i, a);
                case "--applicant" -> applicant = value(args, ++i, a);
                case "--date" -> {
                    date = date(value(args, ++i, a));
                    dateTyped = true;
                }
                default -> {
                    if (a.startsWith("--") || path != null) throw new IllegalArgumentException("not understood: " + a);
                    path = Path.of(a);
                }
            }
        }
        if (path == null) throw new IllegalArgumentException("score needs a path");
        if ((github == null) != (applicant == null)) throw new IllegalArgumentException("--github and --applicant go together");
        if (github != null && !github.matches(GITHUB_NAME)) throw new IllegalArgumentException("--github takes owner/repo, not " + github);
        if (facts && (markdown || dateTyped)) throw new IllegalArgumentException("--facts prints no card: it goes with neither --markdown nor --date");
        return new Command(path, markdown, facts, new Pipeline.Options(run ? new DockerSandbox() : null, github, applicant), date);
    }

    /** A date as yyyy-mm-dd; anything else refused as what was typed, never a parser's exception. */
    private static String date(String typed) {
        try {
            return LocalDate.parse(typed).toString();
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("--date takes yyyy-mm-dd, not " + typed);
        }
    }

    private static String value(List<String> args, int i, String option) {
        if (i >= args.size()) throw new IllegalArgumentException(option + " needs a value");
        return args.get(i);
    }
}
