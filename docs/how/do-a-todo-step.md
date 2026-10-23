# Do the next step of TODO.md, or settle a decision

**Start here:**
- `docs/HANDOFF.md` (what is next and what waits on whom)
- `TODO.md`, its first open item and its check
- the step's plan in `docs/DESIGN.md`, by the same number
- a decision: `docs/DECISIONS.md`, then its entry in `theGhost.md` by number

**Contract:** `theGhost.md` (E-016: in order; E-029: a finished step lands at once).

**Tests:** `test/TodoTest.java`, `test/GhostTest.java`.

## Steps

- Work the item in order; an item that cannot be done as written stops and goes to Ghost (TODO.md's header). Too large for one go: slice it and say so.
- A new decision: the next E-number at the end of `theGhost.md`, every field (its header shows the form), then `gradle decisionsDoc`. A change supersedes the old entry, never rewrites it.
- Finishing a step: move it to `docs/DONE.md` with a line saying what was done, write the journal entry (`docs/JOURNAL.md`), update `docs/HANDOFF.md`, push (`scripts/prepush.sh` first).
- Every eighth commit is a tithe (its subject starts "Tithe", E-015); the pre-push refuses past sixteen.
- Something only Ghost can settle goes in `docs/FOR-GHOST.md`, and you say so.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
