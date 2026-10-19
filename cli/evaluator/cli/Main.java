package evaluator.cli;

import java.io.PrintStream;
import java.util.List;

/**
 * The command line: turns what was typed into a command and prints what it did. The work is in the
 * other modules; this one only names it. `evaluator score <path>` is the one command (phase 1); until
 * ingest/ exists (1.2) it says so and exits 2.
 */
// Decision: E-010
public final class Main {
    static final String USAGE = """
            usage: evaluator score <path-to-repository>
              Scores a repository on the seven metrics of docs/SCORE-CARD.md and prints its score card.
            """;

    public static void main(String[] args) {
        System.exit(run(List.of(args), System.out, System.err));
    }

    /** The exit status of a command: 0 done, 1 refused, 2 not built yet. */
    static int run(List<String> args, PrintStream out, PrintStream err) {
        if (args.size() != 2 || !args.get(0).equals("score")) {
            err.print(USAGE);
            return 1;
        }
        err.println("evaluator score: not built yet (TODO.md 1.2, repo ingestion); " + args.get(1) + " was not read");
        return 2;
    }
}
