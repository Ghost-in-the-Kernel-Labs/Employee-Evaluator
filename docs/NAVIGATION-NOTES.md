# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-24 · navigation review of five debriefs (fourth) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, navigation-review.md, NAVIGATION-NOTES.md, commit-and-push.md, debrief-after-a-task.md, commands.md, then do-a-todo-step.md, agent-environment.md, JournalTest and `git log` for fdf5d9a.
- **Complaints:** the review does not say whether a point whose fix landed in the previous review still counts (the `;` rule: fixed in a test message, hit again). J-104 wrote Class `feature`, which the journal's list lacks and `JournalTest` does not check.
- **Suggestions:** none.

### 2026-10-24 · slicing TODO.md 2.2, the judge harness · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md (its "slice it" line), then TODO.md, DESIGN.md step 2.2, theGhost.md E-055/E-057, add-or-change-a-module.md.
- **Complaints:** nothing says how a sliced item is numbered; `TodoTest` and `DocsTest` parse only `N.M`, found by reading them. 1.13's slices exist only as prose in DONE.md. `docs/modules.json` says the agent runner is a phase-2 module but ModulesTest leaves `agent-runner/` out and E-001 makes it Python: no file says whether it needs an entry.
- **Suggestions:** one line in do-a-todo-step.md: "a sliced item's parts are 2.2a, 2.2b ... (TodoTest)". A line in add-or-change-a-module.md that `agent-runner/` (Python) and `web/` are outside the Java module map.
