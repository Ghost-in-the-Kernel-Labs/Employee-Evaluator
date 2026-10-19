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

## For Opus: before the next session

The scaffold was compiled with `javac` and its suite run through the JUnit launcher on a fresh clone
(9 tests, no lint warnings), but Gradle itself has not yet executed `build.gradle`, and the image has
not yet been built: the workspace that wrote them had neither. So the first thing a session on
Ghost's machine does is `docker build -t evaluator .` and the fast loop from `CLAUDE.md` (or `gradle
test --offline` on JDK 21 with Gradle 8.14.3 exactly; `build.gradle` refuses another version). If
Gradle objects to anything, fix `build.gradle`, run `gradle kernelMap`, write it up as J-004 with
class `failure-caught`, and only then start 1.2.

## For Opus: the next session (1.2)

Read `CLAUDE.md`, then TODO.md 1.2. Build `rubric/` (the Fact and ScoreCard types of
docs/SCORE-CARD.md, with a doc test holding the document to the types) and `ingest/` (the inventory
and the Size facts), with the first fixture repository under `test/fixtures/` and its golden. Run
`gradle kernelMap` before the commit. Write the journal entry.
