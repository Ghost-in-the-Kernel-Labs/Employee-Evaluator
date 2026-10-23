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
