# Before calling work done

- The fast loop passes (`docs/how/commands.md`); `scripts/prepush.sh` passes before the push.
- A refactor changes no golden (E-018); a golden that moves on purpose was reviewed value by value.
- A function that holds a decision cites it on the line above: `// Decision: E-011`.
- `gradle kernelMap` has run (as soon as a function was added, moved or removed; it closes gone ones, E-062).
- A new or changed unit test has been run by `gradle mutate` over its module; each survivor in what it changed
  has a test or a row in `docs/MUTATION.md` (E-019). A test sits in the module of the code it tests: a per-module
  `gradle mutate` runs only that package's tests, so a test in another module's folder is not seen.
- A doubt left in the code is on a line of its own above it, `// Unsure: <why>`; nothing fails on one.
- A finished step of `TODO.md` is moved to `docs/DONE.md`, its journal entry written (`docs/JOURNAL.md`),
  `docs/HANDOFF.md` updated, and pushed; the next step starts, in order (E-029).
- Your debrief is written, if a task was given to you (`docs/how/debrief-after-a-task.md`).
- Committed and pushed as `docs/how/commit-and-push.md` says.
