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

### 2026-10-25 · TODO 2.2i, agreement with the calibration set · Claude Sonnet 5.5
- **Route:** TODO.md 2.2i, then judge-harness.md, do-a-todo-step.md; ensemble.py and judge.py for the interfaces.
- **Complaints:** decisions are in theGhost.md and do-a-todo-step.md says "the end of theGhost.md" but not that the last E-number is found by its last `### E-` heading; the question-8 text in FOR-GHOST.md wraps across lines, so a one-line search for it found nothing. `python3 -I -m unittest tests.test_x` cannot import the package (-I drops the current directory); only `discover` works.
- **Suggestions:** in judge-harness.md, say that a single Python test file runs as `python3 -I -B -m unittest discover -p 'test_x.py'`.

### 2026-10-26 · TODO.md 2.2k, the card's rules for the judged kind · Claude Sonnet 5.5
- **Route:** CLAUDE.md, then the handed-over task text, `do-a-todo-step.md`, `change-the-score-card.md`, `command-line.md`, `judge-harness.md`; E-056/E-057/E-082/E-084/E-085 by number in `theGhost.md`.
- **Complaints:** (1) The task said "reads `rubric/`'s Fact, Calibration and Label only" but no row or file says which module such code belongs in; `score/` may not make facts (MOD-03) and `cli/` decides nothing, so the home (`ingest/`, beside `Judgment`) took a read of `modules.json` and `add-a-fact.md` to settle. (2) `DocTests.scoreCard` and `metrics` fail until `SCORE-CARD.md`'s module row and `gradle kernelMap` are updated; `add-a-fact.md` says the first, the second is only in `finish-work.md`. (3) The mutation clone for `cli/` needs the uncommitted files copied in; `rsync` is not installed (tar over `git ls-files -m -o --exclude-standard` worked).
- **Suggestions:** a line in `command-line.md` or `judge-harness.md` saying where a rule over a judge's answer lives; `commands.md` could name the plain-clone recipe for an uncommitted change (clone, then `git ls-files -m -o --exclude-standard | tar` into it).
