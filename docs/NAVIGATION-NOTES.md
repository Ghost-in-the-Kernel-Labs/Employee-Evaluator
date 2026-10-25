# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-25 · navigation review of nine debriefs (fifth) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, navigation-review.md, commit-and-push.md, debrief-after-a-task.md, commands.md, NAVIGATION-NOTES.md, then judge-harness.md, do-a-todo-step.md, add-or-change-a-module.md, agent-environment.md and the journal's tail.
- **Complaints:** the review does not say how a complaint already fixed is found beyond `git log -S`; several complaints were already fixed in judge-harness.md by the agent who raised them, so checking each took a grep. The entries do not name the commit they were written in.
- **Suggestions:** none.

### 2026-10-25 · TODO 2.2h, three runs and the vote · Claude Sonnet 5.5
- **Route:** TODO.md 2.2h, then judge-harness.md and do-a-todo-step.md; the module docstrings of judge.py and budget.py gave the interfaces.
- **Complaints:** the Check says "scores of a metric" and "15 apart" without saying how the rule meets Quality's 1 to 5 integer or a confidence, nor how a retry is numbered under a replay key; both were chosen and written in the `ensemble.py` docstring. `gradle decisionsDoc` fails to write its output if `build/` does not exist in a fresh worktree (the redirect fails first); `mkdir -p build` is in the task text but not in commands.md.
- **Suggestions:** a line in commands.md that a new worktree needs `mkdir -p build` before any redirect to `build/run.log`.
