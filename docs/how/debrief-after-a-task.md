# Debrief after a task (E-068)

Every task goes to a fresh agent of the tier its work fits (`docs/how/hand-out-a-task.md`), spawned by the session that orchestrates the work. When its
task is done (or stopped), the agent writes a short debrief before it hands back. It is about the way in,
not the work: how well the repository led it to what it needed.

## The entry

Append to the end of `docs/NAVIGATION-NOTES.md`, in the same commit as the work or one just after:

```text
### 2026-10-23 · the task in a few words · the tier
- **Route:** the routing row and instructions file you started from.
- **Complaints:** what was hard to find, missing, wrong, stale, or cost a wasted step (file and line).
- **Suggestions:** what would have saved it, as specific as you can (what text where, what check).
```

Three to eight lines. Nothing to say is an answer: write "none" in both. Say what happened, not what might
happen to someone else; the review looks for what several agents ran into.
