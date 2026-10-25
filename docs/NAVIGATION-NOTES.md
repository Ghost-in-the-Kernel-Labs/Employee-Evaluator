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

### 2026-10-25 · TODO.md 2.2a, the agent-runner scaffold and its gate · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md, TODO.md 2.2a and DESIGN.md "Step 2.2 sliced", add-or-change-a-module.md, commands.md.
- **Complaints:** the task wording said to add the module to docs/modules.json while DESIGN.md says it gains nothing (Python is outside the Java map); the contradiction cost a check of ModulesTest. TODO 2.2a's check says `python3 -I -m unittest discover`, but `-I` ignores PYTHONDONTWRITEBYTECODE, so `__pycache__` appears unless `-B` is added; `.gitignore` held only the top-level `agent-runner/__pycache__/`. No doc says how a Check-style test is registered (DocTests).
- **Suggestions:** one line in add-or-change-a-module.md: "Python (`agent-runner/`) and `web/` are not modules: no entry". One line in agent-environment.md on registering a new test class in `test/DocTests.java`.

### 2026-10-25 · TODO.md 2.2b, `evaluator brief` · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md, TODO.md 2.2b and DESIGN.md "Step 2.2 sliced", command-line.md, judge-harness.md, add-or-change-a-module.md, commands.md.
- **Complaints:** `gradle mutate -Ptarget=evaluator.cli.*` cannot run in a worktree (`MainTest.thisRepositorysCard` fails without mutation, so PIT refuses); I ran it in a plain clone with my files copied in, and no doc says so. A test of `analyze/` code must sit in `test/evaluator/analyze/` or PIT reports no coverage (finish-work.md says it, but late). The Bash classifier failed several times in a row and cost the end of the run.
- **Suggestions:** one line in commands.md or agent-environment.md: "in a worktree, mutate `cli` from a plain clone".

### 2026-10-25 · TODO.md 2.2c, the repo summary builder · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md, TODO.md 2.2c and DESIGN.md "Step 2.2 sliced", judge-harness.md, commands.md.
- **Complaints:** no doc says how a Python golden is held or rewritten (commands.md describes only `-PwriteGoldens` for Java, which the Python child never sees); I invented `EVALUATOR_WRITE_GOLDENS`. The task's sampling rule ("largest by lines in each folder") leaves the count per folder open.
- **Suggestions:** one line in commands.md under Goldens: "Python goldens (`agent-runner/`): `EVALUATOR_WRITE_GOLDENS=1 python3 -I -B -m unittest discover`".

### 2026-10-25 · TODO.md 2.2d, the judgment schema and parser, with E-082 · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md, TODO.md 2.2d to 2.2k and DESIGN.md "Step 2.2 sliced", judge-harness.md, E-055 to E-057 and E-080 in theGhost.md.
- **Complaints:** a decision's fields are required by GhostTest (Decision, Why, Held by, Source, Status, subject) but no doc lists them; I read the test. There is no "Trade-offs" field, so trade-offs went into Why. The kind vocabulary was in five places (TODO 2.2d and 2.2i, DESIGN finding and question 8, FOR-GHOST) and found only by grep.
- **Suggestions:** list the decision fields in do-a-todo-step.md; a DESIGN table of vocabularies (kind, folder classes, tiers) with the one file that owns each.

### 2026-10-25 · TODO.md 2.2f, the judge interface, fake judge and replay · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md, TODO.md 2.2f, DESIGN.md "Step 2.2 sliced", judge-harness.md, prompt.py's docstring for the hashes.
- **Complaints:** the check says a judge takes "a rendered prompt" yet keys a recording by hashes the text does not carry; the docs do not say where the summary hash comes from, so I made `RenderedPrompt`. The task named `build/` for real recordings but no doc says which hash a replay file is keyed under for a changed template version (it follows the prompt hash).
- **Suggestions:** one line in the 2.2f check: "the judge is given the text with its two hashes".

### 2026-10-25 · the tithe pass (Pass 8) · Claude Sonnet 5.5
- **Route:** CLAUDE.md "when you run a command" row, the tithe step in docs/how/agent-environment.md, the last tithe commit for its shape, commit-and-push.md and this debrief page.
- **Complaints:** the tithe step names the metrics run but not that the first run after a fresh clone is cold (29 s against 13 s warm), so the number to record is easy to get wrong. The next free J number is only found by reading the journal's tail.
- **Suggestions:** one line in the tithe step: "discard the cold first run, record the warm ones and name the cold one apart".
