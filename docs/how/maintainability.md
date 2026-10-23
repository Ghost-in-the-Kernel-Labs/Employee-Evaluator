# Maintainability's facts: test files, coupling, duplication

**Start here:**
- `analyze/evaluator/analyze/TestFiles.java` (E-034)
- `analyze/evaluator/analyze/Coupling.java` (E-035)
- `analyze/evaluator/analyze/Duplication.java` and `analyze/evaluator/analyze/Tokens.java` (E-036)

**Contract:** E-034, E-035, E-036; the score from them is `score/evaluator/score/Maintainability.java` (E-051).

**Tests:** `test/evaluator/analyze/DuplicationTest.java` and its golden, `CouplingTest`, `TestFilesTest`.

## Steps

- These make facts; the weights that turn them into a score are a formula: `docs/how/change-a-formula.md`.
- Each fact's value worked by hand on a small fixture before the golden is written.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
