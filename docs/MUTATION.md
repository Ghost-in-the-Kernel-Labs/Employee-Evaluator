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

## Survivors judged harmless

| Where | Mutant | Why it is harmless |
| --- | --- | --- |
| `rubric/Json.java`, `object()`, the empty-object return | returns `Collections.emptyMap()` for the new empty map | Equal to what it replaces: an empty map is an empty map. The only difference is that the mutant's map refuses writes, and no caller writes to a parsed value. |
| `rubric/Json.java`, `array()`, the empty-array return | returns `Collections.emptyList()` for the new empty list | The same, for a list. |
