package evaluator.cli;

import evaluator.analyze.DockerSandbox;
import evaluator.ingest.JudgedRules;
import evaluator.ingest.AgreementRecord;
import evaluator.ingest.Judgment;
import evaluator.rubric.Fact;
import evaluator.rubric.Produced;
import evaluator.rubric.ScoreCard;
import evaluator.score.CardJson;
import evaluator.score.CardMarkdown;
import evaluator.score.Cards;
import evaluator.score.FactsText;
import evaluator.score.Declined;
import evaluator.score.Judged;
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
            usage: evaluator score <path-to-repository> [--markdown] [--no-run] [--facts] [--judged <judgment.json>] [--records <folder>] [--github <owner/repo> --applicant <login>] [--date <yyyy-mm-dd>]
              Scores a repository on the seven metrics of docs/SCORE-CARD.md and prints its score card, as JSON
              or with --markdown as Markdown. --no-run leaves its tests unrun (no sandbox); --github with
              --applicant reads what GitHub verifies (needs EVALUATOR_GITHUB_TOKEN); --date fixes the card's date.
              --judged takes a judge's answer (docs/how/command-line.md): Size, Complexity and Maintainability's code parts count
              only the files in folders it classes as own code, its own tests make the test-to-code ratio, and the card names its
              prompt hash and models. No model is called. The card's rules for the judged kind (docs/how/command-line.md) read the prompt's
              agreement record, <folder>/<prompt hash>.json (--records, default agent-runner/records): a field not live there gives no fact,
              kind generated declines the submission (no card, a message instead, exit status 2), kind plan only withholds Size, Complexity
              and Maintainability. Exit status: 0 a card, 1 refused, 2 declined.
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

    /** The exit status of a command: 0 done, 1 refused, 2 a submission declined (E-056, E-086). */
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
            Judgment judgment = command.options().judgment();
            Judged judged = judgment == null ? null : new Judged(judgment.promptHash(), judgment.models());
            AgreementRecord record = judgment == null ? null : Pipeline.record(judgment, command.options().records());
            if (judgment != null && record == null) {
                err.println("judged: no agreement record for prompt " + judgment.promptHash() + ": the judge's kind, Ambition and Quality are not used (SC-05)");
            } else if (record != null) {
                JudgedRules.Outcome outcome = JudgedRules.apply(judgment, record, facts);
                judged = new Judged(judgment.promptHash(), judgment.models(), outcome.notes());
                if (outcome.declined()) {
                    out.print(command.markdown() ? Declined.markdown(judgment.kind(), judged) : Declined.json(judgment.kind(), judged));
                    return Declined.EXIT_STATUS;
                }
                facts = outcome.facts();
            }
            if (command.facts()) {
                out.print(FactsText.render(facts));
                return 0;
            }
            ScoreCard card = Cards.card(facts, List.of(), Formulas.phaseOne(), new Produced(VERSION, command.date(), Formulas.rubricHash()));
            out.print(command.markdown() ? CardMarkdown.render(card, judged) : CardJson.render(card, judged));
            return 0;
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            err.println("evaluator score: " + e.getMessage());
            return 1;
        }
    }

    /** The command typed; anything else is refused with what was wrong. */
    static Command parse(List<String> args) {
        if (args.size() < 2 || !args.get(0).equals("score")) throw new IllegalArgumentException("the one command is score <path>");
        Path path = null;
        boolean markdown = false, run = true, facts = false, dateTyped = false;
        String github = null, applicant = null, judged = null, records = null, date = LocalDate.now(ZoneOffset.UTC).toString();
        for (int i = 1; i < args.size(); i++) {
            String a = args.get(i);
            switch (a) {
                case "--markdown" -> markdown = true;
                case "--no-run" -> run = false;
                case "--facts" -> facts = true;
                case "--judged" -> judged = value(args, ++i, a);
                case "--records" -> records = value(args, ++i, a);
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
        checkGithub(github, applicant);
        if (facts && (markdown || dateTyped)) throw new IllegalArgumentException("--facts prints no card: it goes with neither --markdown nor --date");
        return new Command(path, markdown, facts, new Pipeline.Options(run ? new DockerSandbox() : null, github, applicant, judgment(judged), records(records, judged)), date);
    }

    /** The folder of agreement records named, or none for the default; it goes with --judged, and must be a folder. */
    private static Path records(String folder, String judged) {
        if (folder == null) return null;
        if (judged == null) throw new IllegalArgumentException("--records goes with --judged");
        try {
            Path path = Path.of(folder);
            if (!Files.isDirectory(path)) throw new IllegalArgumentException("--records: not a folder: " + folder);
            return path;
        } catch (java.nio.file.InvalidPathException e) {
            throw new IllegalArgumentException("--records: not a folder: " + folder);
        }
    }

    /** --github and --applicant go together, and --github is GitHub's owner/repo. */
    private static void checkGithub(String github, String applicant) {
        if ((github == null) != (applicant == null)) throw new IllegalArgumentException("--github and --applicant go together");
        if (github != null && !github.matches(GITHUB_NAME)) throw new IllegalArgumentException("--github takes owner/repo, not " + github);
    }

    /** The judgment file named, read (none named, none read); a file that cannot be read or is not the shape docs/how/command-line.md gives is refused as what was typed. */
    // Unsure: the file is read whole before Judgment.read measures it against its cap; a path to a huge file is the caller's own, not a submission's.
    private static Judgment judgment(String file) {
        if (file == null) return null;
        String text;
        try {
            text = Files.readString(Path.of(file), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException | UncheckedIOException | java.nio.file.InvalidPathException e) {
            throw new IllegalArgumentException("--judged: cannot read " + file);
        }
        try {
            return Judgment.read(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("--judged " + file + ": " + e.getMessage());
        }
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
