# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-23 · navigation review of four debriefs (third) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/navigation-review.md`, `docs/NAVIGATION-NOTES.md`, finish-work.md, commit-and-push.md, then GhostTest, ScoreCardDocTest, `git log -S` and the HANDOFF and journal ends.
- **Complaints:** the review's step 2 counts entries but not whether an earlier set's point (the `;`) still counts toward convergence; I counted it, as the task said. Nothing says the previous review's own debrief is one of the entries to read.
- **Suggestions:** none.

### 2026-10-24 · 2.1 calibration set, the fixture format · Claude Sonnet 5.5
- **Route:** CLAUDE.md "what to work on" row, docs/how/do-a-todo-step.md, TODO.md 2.1, DESIGN.md step 2.1, then add-or-change-a-module.md and modules.json (no new module needed: rubric/ already holds Json).
- **Complaints:** DESIGN.md gives the Ambition and Quality scales only in prose in three places (lines 92, 152, SCORE-CARD.md 19) and no code holds them; I had to define the enums. `gradle decisionsDoc` took over two minutes in the foreground and was moved to the background.
- **Suggestions:** note in commands.md that gradle runs here exceed the 120 s tool timeout, so start them with run_in_background; a one-line "where the Ambition scale lives" in the glossary once Label exists.

### 2026-10-24 · 2.1 calibration label trimmed to three fields · Claude Sonnet 5.5
- **Route:** CLAUDE.md, docs/how/do-a-todo-step.md, then E-079, Label.java, CalibrationSet.java and their test.
- **Complaints:** the status grammar (no `;` in a parenthesis) is only in theGhost.md's header; I wrote one and DocTests.ghost caught it after a 3-minute test run. do-a-todo-step.md does not say how a superseded entry's status reads.
- **Suggestions:** one line in do-a-todo-step.md: "superseding: set the old Status to `superseded by E-000 (reason without ;)` and add `Supersedes:` to the new one".

### 2026-10-24 · 2.1 landing the 30 calibration labels · Claude Sonnet 5.5
- **Route:** CLAUDE.md "what to work on" row, then do-a-todo-step.md, finish-work.md, commit-and-push.md, debrief-after-a-task.md and commands.md.
- **Complaints:** DONE.md had no Phase 2 heading and its ordering rule for a new phase is not written down; I added the heading. do-a-todo-step.md does not say which journal Class to use for a landing.
- **Suggestions:** a line in do-a-todo-step.md on opening a new phase section in DONE.md.
