# Do the next step of TODO.md, or settle a decision

**Start here:**
- `docs/HANDOFF.md` (what is next and what waits on whom)
- `TODO.md`, its first open item and its check
- the step's plan in `docs/DESIGN.md`, by the same number
- a decision: `docs/DECISIONS.md`, then its entry in `theGhost.md` by number

**Contract:** `theGhost.md` (E-016: in order; E-029: a finished step lands at once).

**Tests:** `test/TodoTest.java`, `test/GhostTest.java`.

## Steps

- Work the item in order; an item that cannot be done as written stops and goes to Ghost (TODO.md's header). Too large for one go: slice it and say so; the parts are numbered 2.2a, 2.2b, ... (`TodoTest` parses `N.M` and that letter form only), and a slice's prose lives in `docs/DESIGN.md`, so say in `docs/DONE.md` what each part did.
- A new decision: the next E-number is one past the last `### E-` heading of `theGhost.md` (`git grep -n '^### E-' theGhost.md | tail -1`), the entry goes at the file's end, every field (its header shows the form), then `gradle decisionsDoc`. Status takes one value: an entry that is partly Ghost's call and partly the agent's own choice is `in force`, and its Source names the agent's parts for Ghost to confirm (Ghost, 2026-10-26, J-127). A change supersedes the old entry, never rewrites it: set the old Status to `superseded by E-000 (reason)`, add `Supersedes: E-000` to the new one, and keep `;` out of the parenthesis (statuses are split on it; `DocTests.ghost` fails only after a full test run).
- Finishing a step: move it to `docs/DONE.md` with a line saying what was done, write the journal entry (`docs/JOURNAL.md`), update `docs/HANDOFF.md`, push (`scripts/prepush.sh` first).
- The next journal id is one past the last `### J-` heading (`tail -30 docs/JOURNAL.md`).
- The journal Class for a step: `feature` is not in its list; use the header's nine (`docs/JOURNAL.md`): a landing of data or a recorded number is `measurement`, a choice `decision`. A step in a phase docs/DONE.md has no heading for gets a `## Phase N: name` heading, after the last phase's items, in TODO.md's own wording.
- Every eighth commit is a tithe (its subject starts "Tithe", E-015); the pre-push refuses past sixteen.
- Something only Ghost can settle goes in `docs/FOR-GHOST.md`, and you say so.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
