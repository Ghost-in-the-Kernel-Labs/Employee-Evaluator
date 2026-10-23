# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-23 · three navigability refactors (Json.quote, TestRunner split, CardsTest split) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, then `docs/how/add-or-change-a-module.md`, `agent-environment.md`, `do-a-todo-step.md`, `sandbox-test-runs.md`, `card-and-renderings.md`, `finish-work.md`.
- **Complaints:** `rubric/evaluator/rubric/Json.java` is the JSON reader, so "put Json.quote in rubric/Json.java" meant adding to an existing class, not creating one; nothing says so in `docs/how/read-a-repository.md`. A "Start here" list is capped at five files by `DocTests.metrics`, found only by a failing test after adding two paths (`docs/how/sandbox-test-runs.md`). A per-module `gradle mutate` only runs same-package tests, so a quote test in `ingest/` left `Json.quote` with no coverage until a `rubric/` test existed; `docs/MUTATION.md` says it only in passing.
- **Suggestions:** one line in `docs/how/finish-work.md` saying a function moved to another module needs its test in that module for mutation to see it; one line in `docs/how/add-or-change-a-module.md` (or the sandbox page) that "Start here" lists hold five files at most.

### 2026-10-23 · mutation survivors of history/ (TODO 1.12) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/agent-environment.md`, `docs/how/history-shape.md`, `docs/MUTATION.md`, `docs/how/finish-work.md`, then the module's three sources and ShapeTest.
- **Complaints:** `gradle mutate` needs `build/` to exist before redirecting its log (`> build/mutate.log` failed on a fresh worktree; `agent-environment.md` says to redirect there without saying to mkdir). The CSV has only line numbers, so each survivor needed a `sed -n Np` to read; the lines shift by the lines you add, so a re-run's numbers differ from the first. `docs/how/finish-work.md` and `agent-environment.md` do not say MUTATION.md's run entries are numbered in order (run 5 is a refactor run, not a 1.12 one).
- **Suggestions:** in `agent-environment.md`, "mkdir -p build first" and a one-liner that prints each survivor with its source line (cut -d, -f3-7 of mutations.csv plus sed); a `gradle mutate` summary that lists survivors itself.
