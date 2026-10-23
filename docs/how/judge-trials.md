# Which model judges code: the judge trials (phase 2's groundwork)

**Start here:**
- `docs/judges/` (one file per trial: the brief, every run's scores, the comparisons)

**Contract:** E-017, E-055, E-056, E-057; TODO.md 2.1, 2.2.

**Tests:** none: trials are records.

## Steps

- A trial gets its own file: the brief exactly as sent, each run's scores, and the comparison.
- Nothing an applicant submitted ever enters the repository (E-014): trials run on Ghost's own repositories.
- No model runs in the kernel (E-003); judging is phase 2's `agent-runner/`.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
