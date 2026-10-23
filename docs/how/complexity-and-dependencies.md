# Fix or change complexity, or the dependency graph

**Start here:**
- `analyze/evaluator/analyze/Complexity.java` (`Complexity.measure()`, `Complexity.facts()`)
- `analyze/evaluator/analyze/Dependencies.java` (the graph and unused imports, E-032)
- the language's adapter: `analyze/evaluator/analyze/JavaComplexity.java` (Java, E-030), or `analyze/evaluator/analyze/TreeSitterAdapter.java` and its subclass (E-031)

**Contract:** E-030, E-031, E-032.

**Tests:** `test/evaluator/analyze/`, the goldens under `test/goldens/` (`java-complexity`, `python-complexity`, `typescript-complexity`, `javascript-complexity`, `imports`).

## Steps

- A wrong number: find the function in the fixture's README (`test/fixtures/java-complexity/`, and the others), work the right value by hand, and make the test say it before the code changes.
- A counting rule is a decision: E-030 says what counts; a change to it supersedes the entry.
- A new language: `docs/how/add-a-language-adapter.md`.
- Goldens move only when a number is meant to: `gradle test -PwriteGoldens`, review every changed value.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
