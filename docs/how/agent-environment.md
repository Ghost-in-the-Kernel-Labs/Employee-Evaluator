# The agent's environment: the metrics, the kernel map, mutation runs

**Start here:**
- `docs/AGENT.md` (the contract AGT-01 onward and the metrics table)
- `docs/KERNEL-MAP.md`
- `scripts/Metrics.java`
- `scripts/KernelMap.java`

**Contract:** E-015, E-021, E-062; E-019 (`docs/MUTATION.md`).

**Tests:** `test/MetricsTest.java`, `test/KernelMapTest.java`.

## Steps

- `gradle metrics` prints every metric; `test/MetricsTest.java` fails on one past its target. A limit is never raised to pass: make room.
- `gradle kernelMap` appends new functions and closes gone ones (E-062); never edit the map by hand. Run it
  as soon as a function is added, moved or removed: the doc tests fail until it has.
- The fast loop needs a plain clone. In a git worktree `.git` is a file, which the scorer deliberately does
  not follow (`cli/evaluator/cli/Pipeline.java`), so `MainTest.thisRepositorysCard` fails there on Provenance.
- Search with `git grep`, not a bare grep: the judges' JSON and the kernel map flood a bare one.
- **A tithe pass** (about every eighth commit, E-015): in a plain `git clone` of your worktree's folder (enough; the
  fast loop cannot pass in a worktree), run `env -u EVALUATOR_GITHUB_TOKEN gradle metrics --args=--time` (with the token set, `AuthorshipLiveTest` calls GitHub and the loop reads about 150 s instead of 8 s: that is the token's time, not the code's; it times the loop with every test rerun;
  `date` around `gradle test --offline --rerun-tasks` does the same, and the time program is not installed). Run it
  several times: a parallel run that fails once in ten is a race in a test, not noise (E-072). Add a column to
  `docs/AGENT.md`'s table, put a comment on whatever `gradle metrics` lists, write the journal entry (Class `measurement`), append your debrief to `docs/NAVIGATION-NOTES.md`, and commit
  with the subject "Tithe" (E-015).
- A mutation run over a module is recorded in `docs/MUTATION.md`, numbered in order (a refactor's run counts too);
  each survivor gets a test or a harmless row. `mkdir -p build` first on a fresh checkout. PIT refuses a red
  suite, so in a worktree (where `thisRepositorysCard` fails) run `gradle mutate` in a plain `git clone` of it
  and copy your changed files across. To read the survivors with their source lines:
  `grep -v KILLED build/mutation/mutations.csv | grep -v TIMED_OUT | cut -d, -f1,3,4,5,6` (the line numbers move as you add lines, so re-read after a rerun).
  A TIMED_OUT mutant counts as killed, which is why the line above drops those rows too.
- Reading a survivor (J-074 to J-079): `cat -n` one source file at a time (the CSV's line numbers are that file's own);
  the CSV names a mutator only, and mutations.xml beside it (build output, so not in backticks: a path check holds every backticked path) says what each mutant changed (its `lineNumber`,
  `mutatedMethod` and `description`). `-Ptarget` takes a package glob (`evaluator.analyze.*`), not one class, so a rerun of
  `analyze/` is four minutes: batch the tests first. A survivor that needs a shape the fixture lacks is tried with source
  held in a new test, never by growing the fixture (its golden would move, E-018). A network class is tried against a JDK
  `HttpServer` on loopback (`HttpGitHubTest`), not marked harmless. A scratch clone is a new folder named after your branch.
- Long output never enters the context whole: it always goes to a file, which is grepped for failures
  before anything is read; with none, nothing more is read. `gradle mutate` to build/mutate.log, then grep
  it for "Statistics", and the survivors from build/mutation/mutations.csv (the lines without KILLED).
  `gradle test`: grep its log for "FAILED"; only then the failing test's report under
  build/test-results/test/. A sub-agent given a long run returns only those.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
- A cloud session's checkout may be a shallow clone (`git rev-parse --is-shallow-repository` says `true`): its
  history stops at a cut-off commit, and a clone made from it can lose the cut-off and make `git log` fail
  (`MainTest.thisRepositorysCard` then fails on Provenance). Nothing is missing on GitHub: run
  `git fetch --unshallow origin main` first.
