# Handoff: what to work on next, and what is waiting on whom

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. Phase 1 steps 1.1 (the
scaffold) and 1.2 (the rubric's types and ingestion) are done; 1.3 (static analysis adapters) is next.

## For Ghost

- **The image.** `docker build -t evaluator .` has still never run: no workspace so far had a
  Docker daemon. Gradle itself has run the build (J-005), so `scripts/prepush.sh` is the one untried
  piece. The fast loop in the image now also proves tree-sitter's native library loads there with the
  network off (`test/evaluator/analyze/TreeSitterLoadsTest.java`); the same test on your Windows
  machine proves the Windows library.
- **Calibration set.** Phase 2 cannot start until 30 repositories are hand-labeled on Ambition and
  Quality (TODO.md 2.1). Only Ghost can label; the fixture format comes from Opus first.
- **Mutation tool.** E-019 is proposed until a Java mutation tool is vendored (PIT, Apache-2.0, is the
  candidate; its jar and license go under `third_party/` with a MANIFEST.md row). Decide at 1.10.
- **Naming.** "Employee Evaluator" may read as surveillance to applicants (docs/DESIGN.md, open
  questions). No decision needed before phase 3.

## For the next session (1.4)

Read `CLAUDE.md`, then TODO.md 1.4: coupling, duplication by token shingling, the test-to-code ratio,
and the Maintainability facts and two ratios as goldens; a fixture with a copied function is caught.

- **Coupling** has its graph already: `Dependencies.graph()` gives each file's edges into the
  repository (E-032). Fan-in and fan-out per file, or per folder, come from it without a new parse.
- **Duplication** needs tokens, not lines: tree-sitter's leaves for TypeScript and Python, javac's
  scanner for Java. A renamed copy is the padding an agent writes most (E-025's weak spot: a library
  renamed out of `vendor/` is caught here or nowhere).
- **Test-to-code ratio** needs to know a test file: by path and name (`test/`, `tests/`, `*_test.py`,
  `test_*.py`, `*.test.ts`, `*Test.java`); record the rule as a decision.
- **Not yet read:** JavaScript (`.js`, `.jsx`, `.mjs`) has no adapter; tree-sitter's JavaScript grammar
  is `io.github.bonede:tree-sitter-javascript` (MIT), vendored the same way when its turn comes.
- Add a fixture beside the others when a case needs one, its golden under `test/goldens/`; work each
  value by hand before the golden is written. Run `gradle kernelMap` before the commit, and write
  the journal entry.
