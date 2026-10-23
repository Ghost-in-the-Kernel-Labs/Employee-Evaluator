# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-23 · navigation review of seven debriefs · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/navigation-review.md`, `docs/NAVIGATION-NOTES.md`, then agent-environment.md, commands.md, MUTATION.md, HANDOFF.md, J-075 and `git log -S` to find which commit fixed each point.
- **Complaints:** none worth acting on. Finding a fix's commit took a search per phrase; `git log -S` worked.
- **Suggestions:** none beyond the clause now in `navigation-review.md` step 4.

### 2026-10-23 · analyze/ part 6, the sandbox's network and Docker · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/agent-environment.md`, `docs/how/do-a-todo-step.md`, `docs/MUTATION.md`, then the two classes and their tests; `docs/how/commands.md` and `finish-work.md` when closing.
- **Complaints:** the survivor list had to be rebuilt by hand: the CSV's TIMED_OUT rows count as killed but print among the non-KILLED ones, so `grep -v KILLED` overcounts (I filtered `TIMED_OUT` out too). `do-a-todo-step.md` says "push (`scripts/prepush.sh` first)" while the task forbade it: no conflict, but a closing step "commit only" has no home.
- **Suggestions:** in `agent-environment.md`'s survivors line, `grep -v KILLED build/mutation/mutations.csv | grep -v TIMED_OUT`. A recipe line for "a class that starts a program: take the program's name as a package-private constructor argument and test with a script".

### 2026-10-23 · the fast loop back under 20 s · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/agent-environment.md`, `docs/AGENT.md`, `build.gradle`, then the slow tests and `scripts/Metrics.java`; `docs/how/finish-work.md`, `commit-and-push.md` and this file's debrief page at the end.
- **Complaints:** `agent-environment.md` says to work in a plain clone but not that `git clone` of the worktree's folder is enough (it was); no word on `/usr/bin/time` missing, so I timed with `date`. Which doc holds the number: AGT-02 has no column for a non-tithe measurement.
- **Suggestions:** a line in `agent-environment.md`: "to time the loop, `gradle test --offline --rerun-tasks` in a plain clone, several runs; a parallel run that fails once in ten is a race in a test, not noise". A rule test for the shared-state list in E-072 if tests grow.

### 2026-10-23 · the tithe due after eight commits · Claude Sonnet 5.5
- **Route:** CLAUDE.md, then `git grep -i tithe` (docs/how/commit-and-push.md, E-015 in theGhost.md, docs/AGENT.md), `docs/how/agent-environment.md`, `docs/how/commands.md`, the last tithe commit as a model. About five minutes to learn what a tithe is.
- **Complaints:** no one file says what a tithe pass does; it is spread over agent-environment.md ("records a column"), commit-and-push.md (the subject) and E-015. The fast-loop number in a worktree reads "failed", so a pass needs a plain clone; only agent-environment.md hints at it.
- **Suggestions:** a "Tithe" section in `docs/how/agent-environment.md`: run `gradle metrics --args=--time` in a plain clone, add the column, comment what the metrics list, journal, commit with subject "Tithe".

### 2026-10-23 · Size against the baseline (TODO 1.13, first slice) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/do-a-todo-step.md`, `docs/how/change-a-formula.md`, `docs/how/command-line.md`, then E-070/E-071 and `Formulas.java`, `Metric.java`, `ScoreCard.java`, `test/ScoreCardDocTest.java`.
- **Complaints:** nothing says how to run `evaluator score` from a build: there is no application task, so I ran `java -cp build/classes/java/main:build/resources/main:third_party/tree-sitter/*` after `gradle compileJava` (the first try died on a missing tree-sitter class). The card prints only scores, not the fact values, so reading a baseline's line count needed a throwaway program on `Ingest.read` and `Size.facts`. `change-a-formula.md` step 6 says to read each golden before writing them, but `-PwriteGoldens` was the only way to see the new hash, and a golden whose Size is 0 on both scales proves nothing about the formula. A status's parenthesis may not hold a `;` (GhostTest splits on it first), which only the failing test said.
- **Suggestions:** in `command-line.md`: the java command above, and `--facts` (or a card section) printing the repository-wide fact values so a baseline can be recorded without a scratch program. In `change-a-formula.md`: a fixture with enough lines to put Size above 0 if the golden is to hold the formula. In theGhost.md's entry form: "no `;` inside a status's parentheses".

### 2026-10-23 · Complexity against the baseline (TODO 1.13, second slice) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/do-a-todo-step.md`, `docs/how/change-a-formula.md`, `docs/how/commands.md`, then E-050, E-070, E-071 in theGhost.md, the Size commits c18318c and c1ef2e3 as the model, `Formulas.java`.
- **Complaints:** the previous debrief's complaint stands: the goldens hold scores, not fact values, so checking a card by hand needs the cognitive total and depth, and only a scratch program on `Ingest`, `Complexity` and `Dependencies` gives them (and rendering new cards without writing goldens needed another, in package `evaluator.cli`). `change-a-formula.md` step 6 reads the cards before writing them but offers no way to see them first. A status's parenthesis may not hold `;` (the same trap as last time: it splits on it).
- **Suggestions:** a `-PshowGoldens` (or `gradle test` printing each changed golden's diff) so step 6 can be followed in its order; `--facts` on `evaluator score` for the repository-wide values; the status form's rule about `;` in theGhost.md's "How an entry is written".
