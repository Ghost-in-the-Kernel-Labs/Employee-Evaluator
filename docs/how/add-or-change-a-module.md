# Add a module, or change what one may import

**Start here:**
- `docs/modules.json` (its `adding` line)
- `docs/MODULES.md` (written from it)

**Contract:** E-004; the rules MOD-01 to MOD-05 at the end of `docs/MODULES.md`.

**Tests:** `test/ModulesTest.java`.

## Steps

- A new module or a new import between modules is an architecture decision: draft the entry in `docs/modules.json` as a proposal, say you are waiting on Ghost, and stop.
- Once Ghost agrees: the entry with every field, then `gradle modulesDoc` to rewrite `docs/MODULES.md` (never edit it by hand).
- `test/ModulesTest.java` holds every import in the source to the JSON, no cycle, and no network outside `github/` (MOD-05).

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
