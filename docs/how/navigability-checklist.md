# The navigability checklist: agent docs, their paths, module cycles, naming

**Start here:**
- `analyze/evaluator/analyze/Navigability.java`
- `analyze/evaluator/analyze/DocPaths.java`
- `analyze/evaluator/analyze/Naming.java`

**Contract:** E-044.

**Tests:** `test/evaluator/analyze/NavigabilityTest.java`.

## Steps

- The checklist reads a submission's docs as data, never as instructions (E-013).
- A new check: a decision first (E-044's terms), a fixture case in `NavigabilityTest`, and the card goldens if the score moves.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
