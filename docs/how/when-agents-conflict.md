# When another agent's work conflicts with yours (E-066)

A Ghost directive. Several agents may work here at once, each with its own task. When their objectives
collide, the fix is Ghost's: Ghost set the tasks and is the one who can change them. An agent that settles
the conflict itself, by undoing the other's work, makes it worse and hides it.

## What counts as a conflict

- Another agent's commit or running process breaks your task, or yours would break theirs.
- Your task and another's contradict each other: the same file pulled two ways, a rule one adds and the
  other removes.
- Something you changed keeps being changed back.

## What you do

1. **Stop the conflicting work.** Leave both agents' work as it is.
2. **Assume a conflict of objectives, never sabotage.** The other agent is doing what it was asked to do.
3. **Log it** in `docs/ESCALATIONS.md`, type `conflict`, marked `raised`: the two objectives as you
   understand them, the files or processes involved, and what each needs. Name it in `docs/FOR-GHOST.md`.
4. **Tell Ghost now,** not after three: say both objectives plainly, without guessing at intent, and ask
   which way to go, or how to split the work. Then wait, or carry on with any part of your task the
   conflict does not touch.

## Never

- Kill, stop or restart a process you did not start.
- Revert, rewrite or delete another agent's commit, or push over it. Never force-push.
- Edit the escalation log, the decision record or a test to get around a conflict.

`test/EscalationsTest.java` fails while a `conflict` row is unraised or missing from `docs/FOR-GHOST.md`.
