# The git history's shape: provenance layer 3

**Start here:**
- `history/evaluator/history/Shape.java` (`Shape.facts()`, E-042)
- `history/evaluator/history/Commits.java` (`Commits.read()`)
- `history/evaluator/history/AgentTools.java` (the agent tools found, E-053)

**Contract:** E-042, E-053; docs/DESIGN.md (Provenance); git runs only through `history/evaluator/history/Git.java` (E-033).

**Tests:** `test/evaluator/history/ShapeTest.java` and `AgentToolsTest` (histories built by `test/evaluator/history/GitFixture.java`).

## Steps

- A threshold in `history/evaluator/history/Shape.java` is E-042's: change the constant, E-042's words (a superseding entry) and ShapeTest's worked numbers together, or they disagree.
- A test history is written to order with `GitFixture`: each commit's time, message and files, so every number is known before the run.
- `history/` may not import `analyze/`: a test-file rule comes in as a predicate from `cli/` (MOD rules).
- Facts are `PROVENANCE.history.*`; a new one: `docs/how/add-a-fact.md`.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
