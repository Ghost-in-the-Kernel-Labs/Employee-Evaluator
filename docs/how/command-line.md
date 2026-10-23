# The command line: `evaluator score`, every module's facts in order

**Start here:**
- `cli/evaluator/cli/Main.java` (`Main.run()`, `Main.parse()`)
- `cli/evaluator/cli/Pipeline.java` (the modules in order, E-054)

**Contract:** E-010, E-054; MOD-04: nothing imports `cli/`, and it decides nothing.

**Tests:** `test/evaluator/cli/MainTest.java`, `CardGoldensTest` and `test/goldens/cards/`.

## Steps

- `cli/` only names the work: a rule belongs in the module that does the work, not here.
- Every typed value is checked before use; a wrong one is a usage error on stderr with a non-zero exit, never a stack trace.
- The cards' goldens are written from the fixtures by `gradle test -PwriteGoldens`; review the diff.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
