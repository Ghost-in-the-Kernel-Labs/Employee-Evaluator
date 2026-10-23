# Mutation runs: what the tests catch (E-019)

A mutation tool changes one line of the kernel at a time (a `<` to `<=`, a condition negated, a call
removed, a return emptied) and runs the tests. A change no test notices is a **survivor**: a line the
tests run but do not check. Coverage cannot tell the two apart; this can (Suite Builder's G-239).

`gradle mutate` runs PIT (E-059) over every module's tests, `-Ptarget=evaluator.score.*` over one; the
report is in `build/mutation/` (`index.html`, `mutations.csv`). The doc tests are not mutated (they hold
docs), and no class compiled from `test/` is mutated. The rule (E-019): a new or changed unit test is run
by mutation over the module it tests; each survivor in what it changed gets a test or a row here saying
why it is harmless. A test is never weakened to make code pass.

## Runs

### Run 1: 2026-10-22, the phase-1 boundary (TODO.md 1.11, J-055)

PIT 1.30.1, its default mutators, 4 threads, 7 min 53 s, at the commit that vendored it. 1,714 mutants;
1,471 killed (86 %); 172 survived; 71 on lines no test runs. Test strength (killed of those run) 90 %.

| Module | Mutants | Killed | Survived | No coverage | Killed % |
| --- | --- | --- | --- | --- | --- |
| `analyze/` | 1104 | 939 | 117 | 48 | 85% |
| `cli/` | 45 | 38 | 2 | 5 | 84% |
| `github/` | 64 | 47 | 3 | 14 | 73% |
| `history/` | 107 | 81 | 25 | 1 | 76% |
| `ingest/` | 105 | 105 | 0 | 0 | 100% |
| `rubric/` | 134 | 109 | 23 | 2 | 81% |
| `score/` | 155 | 152 | 2 | 1 | 98% |

Where the 243 live, most first: `rubric/Json.java` 21, `analyze/JavaComplexity.java` 20,
`history/Shape.java` 19, `analyze/RegistriesProxy.java` 15, `analyze/PythonAdapter.java` 13,
`analyze/TestRunner.java` 12, `analyze/TreeSitterAdapter.java` 11, `analyze/Reports.java` 11,
`github/HttpGitHub.java` 11, `analyze/DockerSandbox.java` 10. By mutator: negated conditions 52,
removed calls 50, arithmetic 48, boundaries 47, returns emptied or nulled 43.

What to expect of them: the network and sandbox classes (`HttpGitHub`, `RegistriesProxy`,
`DockerSandbox`) run only against the real thing, which the fast loop does not reach (E-040), so most of
their 36 are lines no test runs; `Json` is the hand-written parser behind every golden, so its survivors
are the likeliest to matter; `JavaComplexity` and `Shape` hold the numbers on the card.

**Not yet worked.** This run is the baseline. Its survivors are not yet each given a test or a reason:
243 at once is a pass of its own, TODO.md 1.12, done module by module, `rubric/Json.java` first. Until then
E-019 holds for new and changed tests, and each run below says which survivors it closed.

### Run 2: 2026-10-22, `rubric/Json.java` (TODO.md 1.12, J-056)

`gradle mutate -Ptarget=evaluator.rubric.*`. Run 1 counted Json's mutants against the other modules' tests,
which only reach it through GitHub's answers, Jest's reports and docs/modules.json; `rubric/` itself had no
test of it, so alone it had none covered. `test/evaluator/rubric/JsonTest.java`, every value worked by hand,
now kills 65 of its 67 (the code lost two lines, so the count differs from run 1's). The other two are below.

What the pass found besides: `\r`, `\b` and `\f` were read as the letters `r`, `b` and `f` (fixed); two
`space()` calls before `take(',')` did nothing, since `take` skips space itself (removed, which made
`take`'s own call the one a test now checks).

### Run 3: 2026-10-22, the rest of `rubric/`, which closes it (TODO.md 1.12, J-057)

`gradle mutate -Ptarget=evaluator.rubric.*`: 132 mutants, 130 killed; the two left are Json's, below. The
four the rubric's own tests missed were each a check with no test of its edge: a calibration's precision of
exactly 0 or 1 (allowed, never tried); `Metric.ofKind(".lines")`, refused, but by the wrong check and with
the wrong message until a test named the message; and the card's "not both" and "and is neither", whose
words nothing read. Tests added in FactTest and ScoreCardTest; no code changed. `rubric/` is done.

### Run 4: 2026-10-22, `score/`, which closes it (TODO.md 1.12, J-058)

`gradle mutate -Ptarget=evaluator.score.*`: 155 mutants, 154 killed; the one left is below. Three were tests
missing at an edge: a cognitive complexity of exactly 15 and a fan-out of exactly 10, on Maintainability's
"at most" limits (MaintainabilityTest); a card with a corpus rank and a review, which phase 1 never fills, so
their JSON had never been rendered; and the rubric's hash, held only by `cli/`'s goldens until CardsTest
named it. No code changed. `score/` is done.

### Run 5: 2026-10-23, the three navigability refactors (Json.quote, TestRunner split, CardsTest split)

`gradle mutate` over `score/`, `ingest/`, `rubric/` and `analyze/`. `Json.quote` had no test in `rubric/` (its
only test sat in `ingest/`, which a per-module run does not see): `JsonTest.quoteEscapesWhatCouldBreakOut` now
kills all six of its mutants. In `analyze/` the survivors in `Workspace`, `ReportFiles` and `TestRunner` are the
lines `TestRunner` held before, moved with no change of text (the same ones run 2 counted); none is new.

### Run 6: 2026-10-23, `history/`, which closes it (TODO.md 1.12, J-072)

`gradle mutate -Ptarget=evaluator.history.*`: 107 mutants before, 81 killed, 25 survived, 1 on a line no test
runs (Shape 19, Commits 3, Git 3). After: 108 mutants (the path split became its own function), 105 killed;
the three left are below. Tests added in ShapeTest (cadence, messy share, variation with a size-0 commit and
with exactly two sizes, co-location with two beside and one alone, fingerprints at their edges and read from the
commits that touch an agent file, the evidence of the facts: hashes or "(none)"), and two new files,
CommitsTest (`parse` with the numstat right after its close, records that are no record, the path split) and
GitTest (the environment scrub, the output cap at exactly `MAX_OUTPUT`, an interrupted run).

One real bug, found by the survivor in `Commits.paths`: `ls-tree -z` separates paths by NUL, but the output
was cut into lines first, so a file whose name holds a newline became two paths (one of them wrong, for
the agent-file and test-file rules). Fixed (`Commits.splitPaths`), with `aPathWithANewlineIsOnePath`, which
fails before the fix. Two small changes of shape only, to make the code testable: `Git.scrub` (the
environment, taken out of `builder`) and `Git.readCapped` no longer private. `history/` is done.

## Survivors judged harmless

| Where | Mutant | Why it is harmless |
| --- | --- | --- |
| `rubric/Json.java`, `object()`, the empty-object return | returns `Collections.emptyMap()` for the new empty map | Equal to what it replaces: an empty map is an empty map. The only difference is that the mutant's map refuses writes, and no caller writes to a parsed value. |
| `rubric/Json.java`, `array()`, the empty-array return | returns `Collections.emptyList()` for the new empty list | The same, for a list. |
| `score/Formulas.java`, `logScale`, `n <= 100` | `n < 100` | The two differ only at 100, where the formula gives 100 · (log10 100 − 2) / 3 = 0, which the guard returns anyway. |
| `history/Shape.java`, `variation`, the two `-` in `(s - mean) * (s - mean)` | `(s + mean) * (s - mean)`, `(s - mean) * (s + mean)` | Equal to what they replace: (s + m)(s - m) = s² - m², and the sum of that over n values is Σs² - n·m², which is Σ(s - m)² when m is their mean. Only floating-point rounding, far below the three decimals the fact is rounded to, differs. |
| `history/Commits.java`, `parse`, `close < 0` | `close <= 0` | Equal in result: a record whose close is at index 0 has an empty header, which splits into one part, and `head.length < 3` skips it on the next line. |
