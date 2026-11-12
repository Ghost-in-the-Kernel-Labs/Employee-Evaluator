# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-11-12 · the token posture, steps 2 to 4, a navigation review and Pass 11 · strong tier (Claude Opus 5.5)
- **Route:** CLAUDE.md -> do-a-todo-step.md -> docs/HANDOFF.md for the order; `scripts/Metrics.java` and `test/MetricsTest.java` for the shape of a measure and its check; Suite Builder's `scripts/score-items.mjs` and `score-tune.mjs` read for the posture's points.
- **Complaints:** the posture's steps 2 to 4 are named only in HANDOFF.md and hand-out-a-task.md, never as a TODO item, so `gradle scoreItems` cannot place them. A shallow clone makes `gradle metrics` count the commits since the tithe from the clone's own start (6, where it was 9). The journal holds two entries headed J-140.
- **Suggestions:** in agent-environment.md's tithe bullet: "a shallow clone counts the cadence short: `git fetch --depth=200 origin main` first". Work the handoff names as next goes into TODO.md as an item, so the scorer sees it.
