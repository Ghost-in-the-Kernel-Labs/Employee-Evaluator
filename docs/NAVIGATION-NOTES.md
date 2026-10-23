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


### 2026-10-23 · evaluator score --facts, and the tithe · Claude Sonnet 5.5
- **Route:** CLAUDE.md, the HANDOFF next step, `docs/how/command-line.md`, then Main.java, Pipeline.java, Formulas.java, Maintainability.java, change-a-formula.md and the tithe step of agent-environment.md.
- **Complaints:** `Main.Command` is a record that `CardGoldensTest` builds by hand, which only the compiler told me (command-line.md names the test but not that). A new decision needs the next E-number, its form and `gradle decisionsDoc`, but the command line row does not say an option is a decision; I judged it was. The tithe step says "put a comment on whatever `gradle metrics` lists": it listed nothing, which is not said to be an answer. Run in a worktree, the full suite failed three more tests than `thisRepositorysCard` until `kernelMap` and `decisionsDoc` had run, so the plain clone is needed only for the card test, mutation and timing.
- **Suggestions:** in command-line.md's Steps: "a new option changes `Main.Command`; `CardGoldensTest` builds one, and a new flag is a decision in `theGhost.md`". In agent-environment.md's tithe step: "if `gradle metrics` lists nothing, say so in the journal".

### 2026-10-23 · gradle test -PshowGoldens (J-093's last next step) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, do-a-todo-step.md, change-a-formula.md, commands.md, then `git grep writeGoldens` to find the six tests and build.gradle.
- **Complaints:** no row or instructions file says where a test helper shared by every module's tests may live, or that `gradle mutate` leaves everything under test/evaluator/ unmutated (found in build.gradle); the build shows only a failed test's message (`testLogging`), found by reading build.gradle, which decided where the diff goes. The paragraph under "Always" of agent-environment.md and the shallow-clone note sit after it, hard to see.
- **Suggestions:** a line in docs/how/commands.md or the module guide: "a helper for several modules' tests goes in test/evaluator/ (package evaluator), unmutated"; the other goldens docs (add-a-fact, card-and-renderings, read-a-repository) still say only "-PwriteGoldens, review the diff" and could name -PshowGoldens.

### 2026-10-23 · the seed corpus made again under the baseline · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/do-a-todo-step.md` (nothing in it about a corpus task), `docs/how/command-line.md`, `corpus/README.md`, then `test/Check.java`, `test/GlossaryTest.java` and `test/DocTests.java` as the pattern for a doc test.
- **Complaints:** no routing row for the corpus: HANDOFF's one line was the only pointer, and no file says how a card is regenerated (scratch clone, build, java command, `--markdown` for the `.md`, same `--date`). Reproducing the old card to tell rubric from drift meant a second scratch build of the old commit, which no doc suggests. Scoring Katana takes minutes and a foreground command over two minutes goes to the background.
- **Suggestions:** a CLAUDE.md row "The seed corpus" pointing to `corpus/README.md` (its regeneration paragraph now exists) and `test/CorpusTest.java`; in `docs/how/command-line.md` one line that the kernel at the card's own commit, built in a scratch clone, tells a rubric change from a tool change.
