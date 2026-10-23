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
- A tithe pass records a column in `docs/AGENT.md`'s table.
- A mutation run over a module is recorded in `docs/MUTATION.md`, numbered in order (a refactor's run counts too);
  each survivor gets a test or a harmless row. `mkdir -p build` first on a fresh checkout. PIT refuses a red
  suite, so in a worktree (where `thisRepositorysCard` fails) run `gradle mutate` in a plain `git clone` of it
  and copy your changed files across. To read the survivors with their source lines:
  `grep -v KILLED build/mutation/mutations.csv | cut -d, -f1,3,4,5,6` (the line numbers move as you add lines, so re-read after a rerun).
  A TIMED_OUT mutant counts as killed.
- Long output never enters the context whole: it always goes to a file, which is grepped for failures
  before anything is read; with none, nothing more is read. `gradle mutate` to build/mutate.log, then grep
  it for "Statistics", and the survivors from build/mutation/mutations.csv (the lines without KILLED).
  `gradle test`: grep its log for "FAILED"; only then the failing test's report under
  build/test-results/test/. A sub-agent given a long run returns only those.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
