# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-23 · navigation review of three debriefs · Claude Sonnet 5.5
- **Route:** CLAUDE.md, `docs/how/navigation-review.md`, `docs/NAVIGATION-NOTES.md`, then agent-environment.md, finish-work.md, commit-and-push.md, add-or-change-a-module.md, MUTATION.md, J-074 to check each point.
- **Complaints:** none worth acting on. The review file's step 3 says what to record when nothing is actionable but not when points converged yet were already fixed (step 4 assumes unfixed ones); I recorded one measurement entry.
- **Suggestions:** one clause in `navigation-review.md` step 4: a valid point already fixed is recorded as "already fixed" with the commit.

### 2026-10-23 · github/ mutation survivors (TODO 1.12) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, agent-environment.md, finish-work.md, commit-and-push.md, MUTATION.md, HANDOFF.md, then github/ and its two test files.
- **Complaints:** MUTATION.md's run 1 says the network classes "run only against the real thing"; that sent me to look for a hook before I saw `get` only needs a host. `docs/how/` has no file for the github/ module's work (the table's nearest row is github-authorship.md, about commit verification). `Pipeline.github` (J-073) was the one precedent for injecting the API; nothing says a loopback server is allowed in tests.
- **Suggestions:** one line in agent-environment.md's mutation step: "a network class is tested against a JDK `HttpServer` on loopback (HttpGitHubTest), not marked harmless". Also the shallow/worktree clone steps worked as written; nothing to add there.

### 2026-10-23 · analyze/ part 1, the Java classes' mutants · Claude Sonnet 5.5 (written from its report by the reviewing agent: it did not append it)
- **Route:** CLAUDE.md, `docs/how/agent-environment.md`, `docs/how/do-a-todo-step.md`, `docs/MUTATION.md` (runs 6 to 8).
- **Complaints:** a scratch clone folder another agent left (`plain`) made its first clone fail. The mutation CSV's line numbers point at the line the mutated code sits on, not always the construct it names (the "nesting + 1" mutants are on the scan lines).
- **Suggestions:** name a scratch clone after the branch; read the numbered source before a survivor.

### 2026-10-23 · analyze/ part 2, Python and the tree-sitter base mutants (TODO 1.12) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, theGhost.md's supply-chain block, agent-environment.md, do-a-todo-step.md, finish-work.md, debrief-after-a-task.md, MUTATION.md (run 9, the harmless rows), HANDOFF.md, then the three classes and TreeSitterAdaptersTest.
- **Complaints:** the survivors' CSV needs the numbered source beside it (`cat -n`) to read; nothing in agent-environment.md says `-Ptarget` takes a package glob only, so every rerun was the whole `analyze/` (four minutes). The fixture's README says its values are worked in TreeSitterAdaptersTest, which does not say a new test may hold source inline instead.
- **Suggestions:** one line in MUTATION.md: "a survivor that needs a shape the fixture lacks is tried with source held in a new test, not by growing the fixture (its golden would move, E-018)".

### 2026-10-23 · analyze/ part 3, TypeScript/JavaScript and navigability mutants (TODO 1.12) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, theGhost.md's supply-chain block, agent-environment.md, do-a-todo-step.md, finish-work.md, debrief-after-a-task.md, navigability-checklist.md, MUTATION.md (run 10), HANDOFF.md, then the seven classes and TreeSitterAdaptersTest.
- **Complaints:** mutations.csv gives a mutator name only; what each mutant changed ("negated conditional" on which operand) is in mutations.xml's `description`, which agent-environment.md does not mention. Its line numbers are the source file's own, while my `cat -n` of several files at once runs on, so I misread them first. The navigability-checklist.md "Always" is the generic line and says nothing of where its mutation tests live (NavigabilityTest holds Naming and DocPaths too).
- **Suggestions:** in agent-environment.md, one line: "the XML's `description` says what each survivor changed: `python3` over `mutations.xml` for `lineNumber`, `mutatedMethod`, `description`"; and `cat -n` one file at a time.

### 2026-10-23 · analyze/ part 4, the dependency graph and duplication mutants (TODO 1.12) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, theGhost.md's supply-chain block, do-a-todo-step.md, agent-environment.md, commit-and-push.md, finish-work.md, debrief-after-a-task.md, MUTATION.md (runs 6 to 11 and the harmless rows), HANDOFF.md, then the six classes and their tests.
- **Complaints:** `cat -n` of several files at once ran its numbers on, so the CSV's lines did not match (the same as part 3's note). The CSV prints a mutator and no operand, so which of three negations survived on one line (Dependencies 103) could be learned only by a search, not read. HANDOFF names "test runs" as next without saying which classes; MUTATION.md run 1's table has no per-class counts to find them by.
- **Suggestions:** the XML-description line, now in agent-environment.md (tithe); a per-class count of run 1's survivors in MUTATION.md, or HANDOFF naming the next pass's classes as the task text did.

### 2026-10-23 · analyze/ part 5, a submission's test runs mutants (TODO 1.12) · Claude Sonnet 5.5
- **Route:** CLAUDE.md, theGhost.md's supply-chain block, do-a-todo-step.md, agent-environment.md, commands.md, finish-work.md, commit-and-push.md, debrief-after-a-task.md, MUTATION.md (runs 9 to 12 and the harmless rows), HANDOFF.md, then the five classes and their tests.
- **Complaints:** The CSV's line numbers are each file's own, so reading five classes at one `cat -n` again ran them on (the same as parts 3 and 4); I subtracted offsets by hand. A worktree has no `build/`, so the first `gradle test` failed on its log redirect (agent-environment.md says `mkdir -p build` only under mutation). "Survivors judged harmless" says nothing about how to prove a removed-call mutant is a default (I tried a scratch Java file in the scratchpad).
- **Suggestions:** in agent-environment.md, "`cat -n` one file at a time" is there already; add that `mkdir -p build` comes before any redirect to build/run.log, in a worktree too. For a removed `setFeature`/`setXIncludeAware` mutant, a one-line note that a JDK default can make the call redundant, and a scratch program is the way to try it.
