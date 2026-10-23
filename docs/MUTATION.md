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

### Run 7: 2026-10-23, `cli/`, which closes it (TODO.md 1.12, J-073)

`gradle mutate -Ptarget=evaluator.cli.*`, in a plain clone (PIT needs a green suite; see agent-environment.md).
Before: 45 mutants, 38 killed (three of them TIMED_OUT, the `++i` of `parse`'s option loop turned to `--i`, which
never ends), 7 not killed: 2 survived and 5 on lines no test runs. After: 45 mutants, 44 killed, 1 left, below.
Run 1 counted 2 survivors and 5 with no coverage in 45; the same.

Tests added: `PipelineTest` (new): the note "history not read: no .git directory" for a folder with no `.git`;
the note "tests not measured: Node: no package.json at the root" from a sandbox that is never reached; and
`Pipeline.github`, which asks GitHub for the owner and repository named (`/repos/ghost/app/commits...`) and
returns `Authorship`'s facts. `MainTest.whatCouldNotBeReadIsRefusedWithItsReason`: a `.git` that is no
repository makes `git log` fail, which `Main.score` says as one line, exit 1, nothing on stdout (the lines
`score` ran for IOException, which no test reached). One change of shape only, to make the GitHub line testable:
`Pipeline.github` takes the `GitHubApi` (package-private) and `facts` passes `HttpGitHub.fromEnvironment()`.
No bug found. `cli/` is done.

### Run 8: 2026-10-23, `github/`, which closes it (TODO.md 1.12, J-076)

`gradle mutate -Ptarget=evaluator.github.*`, in a plain clone. Before: 64 mutants, 47 killed, 3 survived, 14 on
lines no test runs (Authorship 6, HttpGitHub 11; run 1's counts). After: 66 mutants (the adapter gained two small
functions), 64 killed; the two left are below.

The 11 in `HttpGitHub` were lines that "run only against the real GitHub", but a recorded answer does reach them:
`get` only needs a server to answer it, and the JDK's own `HttpServer` on loopback is one, so no call to GitHub
is made. `HttpGitHubTest` (new) answers `get` for a 200 (parsed; the request's three headers, the token stripped), a
404 and a 302 (each refused with its status, the redirect not followed), a body that is not JSON, an answer of
exactly `MAX_BODY` bytes (read) and one byte more (refused), an interrupted thread (the flag is kept), and the
four kinds of path refused before anything is sent. Two changes of shape only, to make that possible: the host is
a field (`HttpGitHub.at(host, token)`, package-private, used by tests alone; production code still builds
the adapter against `https://api.github.com` only), and `HttpGitHub.from(token)` takes the environment's value, so
a missing, blank and padded token are tried without touching the process's environment.

In `Authorship`, tests added in AuthorshipTest: a push that lists its commits (no compare asked), a list that is
empty (falls back to the head), a head that is no hash, the oldest of several pushes; both page caps hit exactly
(50 commit pages, 3 event pages, and no more); a commit with no date, with and without a push (the window is 0,
never negative, and the evidence says "oldest push ..." or "(no push by the applicant in GitHub's log)"); and
the fixture test now checks the evidence of the last three facts. No bug found. `github/` is done.

### Run 9: 2026-10-23, `analyze/`, part 1 of 6: the Java classes only (TODO.md 1.12)

`gradle mutate -Ptarget=evaluator.analyze.*`, in a plain clone (about four minutes). `analyze/` is 1,104 mutants and is
split over six passes by concern; this one covers `JavaComplexity` (20 left alive), `JavaImports` (8) and `JavaAdapter`
(3) only. After: 1,104 mutants, 953 killed (the other classes' survivors untouched); of these three classes 7 are left,
all judged harmless below (6 in `JavaComplexity`/`JavaAdapter`, 1 in `JavaImports`).

Tests added, in `JavaComplexityTest` (new; source held in the test, so the fixture and its golden are unchanged, E-018):
every nesting-sensitive rule of the cognitive count tried at nesting 1 (inside an `if`), where `1 - nesting` or
`nesting - 1` shows: an `else if` body, an `else` body, both branches of `?:`, a `switch` in an `if`, a `case` body, a
switch expression, a for-each, a `while` and its body, a `do` and its body, a `catch` and its block; an unlabelled `break`
(0) and a labelled one (+1). The package import `a.b.*` resolved (only `.java` files directly in the folder, not a
deeper one, not `xa/b`, not a text file: `JavaImports.packageFiles` had no test at all), a sibling of a file at `/`, and
the own-package import's line (the package line; line 1 with no package). No bug found, no golden moved.

### Run 10: 2026-10-23, `analyze/`, part 2 of 6: Python and the tree-sitter base (TODO.md 1.12)

`gradle mutate -Ptarget=evaluator.analyze.*`, in a plain clone (about four minutes). Covers `PythonAdapter` (14 left
alive), `TreeSitterAdapter` (11) and `PythonImports` (4) only. After: 1,104 mutants, 977 killed (the other classes'
survivors untouched); of these three classes 5 are left, all judged harmless below.

Tests added, in `PythonAdapterTest` (new; source held in the test, so the fixture and its golden are unchanged, E-018),
each value worked by hand in its comment: an `if` with a boolean condition, an `elif` with one, and an `if` inside the
`if`'s, the `elif`'s and the `else`'s block (cognitive 9, cyclomatic 7); an `except` and a ternary inside an `if`
(nesting 1) and an `if` in an `except`'s block; a comprehension's `if` with an `and`; a call's arguments; a loop's
header at the loop's level and an `if` in its `else`; brackets looked through (`a and (b and c)`, `((...))`, and
`a and (b or c)`, which `flatten` had no Python test for); `reads` and `tool`; the line of a first error that is not
on line 1, nested in a class, and of a token tree-sitter had to invent; imports on a later line; relative imports
(`from . import missing` leads to the package's `__init__.py`; `..c` from `a/b.py` reaches the root, `...c` from there
does not); `normalize` and `folder` directly. No bug found, no golden moved.

## Survivors judged harmless

| Where | Mutant | Why it is harmless |
| --- | --- | --- |
| `rubric/Json.java`, `object()`, the empty-object return | returns `Collections.emptyMap()` for the new empty map | Equal to what it replaces: an empty map is an empty map. The only difference is that the mutant's map refuses writes, and no caller writes to a parsed value. |
| `rubric/Json.java`, `array()`, the empty-array return | returns `Collections.emptyList()` for the new empty list | The same, for a list. |
| `score/Formulas.java`, `logScale`, `n <= 100` | `n < 100` | The two differ only at 100, where the formula gives 100 · (log10 100 − 2) / 3 = 0, which the guard returns anyway. |
| `history/Shape.java`, `variation`, the two `-` in `(s - mean) * (s - mean)` | `(s + mean) * (s - mean)`, `(s - mean) * (s + mean)` | Equal to what they replace: (s + m)(s - m) = s² - m², and the sum of that over n values is Σs² - n·m², which is Σ(s - m)² when m is their mean. Only floating-point rounding, far below the three decimals the fact is rounded to, differs. |
| `history/Commits.java`, `parse`, `close < 0` | `close <= 0` | Equal in result: a record whose close is at index 0 has an empty header, which splits into one part, and `head.length < 3` skips it on the next line. |
| `cli/Main.java`, `main`, `System.exit(...)` | the call removed | `main` is the JVM's entry and exit; a test that called it would end the test JVM, so the work is in `run`, which returns the status and is tested for each status (0, 1). The only difference is the status the shell sees, which `run`'s return value is. |
| `github/Authorship.java`, `facts`, `if (total == 0) return facts;` | the returned list replaced by an empty immutable list | Equal to what it replaces: at that line `facts` is the empty list just made, so an empty list is the same list. The caller (`Pipeline.facts`) only reads it with `addAll`. |
| `github/HttpGitHub.java`, `fromEnvironment`, `return from(...)` | returns null | The line's return runs only when `EVALUATOR_GITHUB_TOKEN` is set, and a session without the token (this one) reaches it only by the throw. It is not harmless: with the token set, `AuthorshipTest.theAdapterNamesItsVariableAndNeverItsToken` calls `live.toString()` and `AuthorshipLiveTest` calls `Authorship.facts` on the result, both of which fail on null, so Ghost's token session kills it. A recorded answer cannot, since the environment is the process's and a test must not set it; `from` (which the line calls) is tried for every value. |
| `analyze/JavaComplexity.java`, `scan` (line 53), `visitMethodInvocation`, `visitBinary`, `visitSwitch`, `visitSwitchExpression` | the returned value replaced by null | Each returns `Void`, whose only value is null: `super.scan(...)`, `super.visitMethodInvocation(...)` and `super.visitBinary(...)` return null, and `aSwitch` returns null. The mutant returns what the code returned. |
| `analyze/JavaAdapter.java`, `Methods.visitMethod`, `return super.visitMethod(tree, null)` | the returned value replaced by null | The same: `Void`, always null. |
| `analyze/JavaImports.java`, `packageFiles`, `slash < 0` | `slash <= 0` | Equal in result: at slash 0 the directory is `file.substring(0, 0)`, which is "" as for no slash. |
| `analyze/TreeSitterAdapter.java`, `firstError`, `i < node.getChildCount()` | `i <= ...` | `firstError` is called only on a node with an error in it, which is not itself the error or a missing token, so one of its children has the error and the loop returns before `i` reaches the count. |
| `analyze/TreeSitterAdapter.java`, `firstError`, the last `return node` | returns null (no coverage) | The same: the line is a fallback no tree reaches, for a node that has an error but whose children have none. |
| `analyze/TreeSitterAdapter.java`, `folder`, `slash < 0` | `slash <= 0` | Equal in result: at slash 0 the folder is `path.substring(0, 0)`, "" as for no slash (as `JavaImports.packageFiles`, above). |
| `analyze/PythonImports.java`, `from`, `i < statement.getChildCount()` | `i <= ...` | At `i` equal to the count `getFieldNameForChild` gives no name, `"name".equals(null)` is false and the loop continues to its end. |
| `analyze/PythonAdapter.java`, `wildcard`, `getNamedChildCount() > 0` | `>= 0` | A `case_clause` always has its pattern as a named child (a clause without one does not parse), so the count is never 0 and the two conditions agree. |
