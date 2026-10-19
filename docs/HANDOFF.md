# Handoff: what to work on next, and what is waiting on whom

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. Phase 1, step 1.1 (the
scaffold) is done; 1.2 (ingestion) is next.

## For Ghost

- **Calibration set.** Phase 2 cannot start until 30 repositories are hand-labeled on Ambition and
  Quality (TODO.md 2.1). Only Ghost can label; the fixture format comes from Opus first.
- **Mutation tool.** E-019 is proposed until a Java mutation tool is vendored (PIT, Apache-2.0, is the
  candidate; its jar and license go under `third_party/` with a MANIFEST.md row). Decide at 1.10.
- **Naming.** "Employee Evaluator" may read as surveillance to applicants (docs/DESIGN.md, open
  questions). No decision needed before phase 3.

## For Opus: the next session (1.2)

Read `CLAUDE.md`, then TODO.md 1.2. Build `rubric/` (the Fact and ScoreCard types of
docs/SCORE-CARD.md, with a doc test holding the document to the types) and `ingest/` (the inventory
and the Size facts), with the first fixture repository under `test/fixtures/` and its golden. Run
`gradle kernelMap` before the commit. Write J-004.
