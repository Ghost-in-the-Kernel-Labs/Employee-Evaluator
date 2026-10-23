# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-23 · navigation review of seven debriefs (second) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/navigation-review.md`, `docs/NAVIGATION-NOTES.md`, then command-line.md, change-a-formula.md, agent-environment.md, commands.md, HANDOFF.md, the GhostTest status parser and `git log` for the last tithe.
- **Complaints:** the review's steps do not say which journal number to take (I read the last J- heading); fine. Checking "is the java command real" meant compiling first, as the agents did: no `gradle` task prints the classpath.
- **Suggestions:** none beyond the next steps now in `docs/HANDOFF.md`.

### 2026-10-23 · Maintainability against the baseline (TODO.md 1.13, third slice) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md, change-a-formula.md, then Maintainability.java, its test, E-051, E-070, E-071, E-074 and the Complexity commit as the pattern.
- **Complaints:** the baseline's parts needed a scratch class in package `evaluator.score` (`Maintainability` is package-private, `Pipeline.code` is in `cli`), a third agent to write one; the Complexity journal entry (J-092) says what the scratch program did but not its code. The enum order of `Metric` decides the order SC-01 and `ScoreCardTest` list counted metrics in (found by a failing test).
- **Suggestions:** the `--facts` step in HANDOFF would have saved the scratch class; until then a line in change-a-formula.md step 6 that a scratch class goes in the package of what it reads, compiled against build/classes/java/main and the tree-sitter jars. SC-01's counted list is in `Metric` declaration order.

