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

## For the next session (1.3, in slices)

Read `CLAUDE.md`, then TODO.md 1.3. Ghost asked for 1.3 in slices, each tested and pushed on its own
(2026-10-19): (1) tree-sitter vendored and proven, **done** (J-010); (2) the adapter interface and the
Java adapter; (3) the TypeScript and Python adapters; (4) the dependency graph and unused imports.
1.3 moves to `docs/DONE.md` only when all four are in. Next is slice 2.

- **tree-sitter** is `third_party/tree-sitter/` (the bonede JNI binding, Java 21, natives in the jars;
  E-028, J-010); `org.treesitter.TSParser` with `TreeSitterPython` or `TreeSitterTypescript`, as
  `TreeSitterLoadsTest` shows. The kernel stays on Java 21.
- **The Java adapter** needs nothing new: the JDK's parser, as `scripts/Scan.java` uses.
- **`analyze/`** reads `ingest/`'s Inventory (`Ingest.read()`); its Complexity facts are built with
  `Fact.measured()`.
- **Padding** the applicant writes themselves is not set aside (E-025 sets aside only what they did
  not write): Ghost, 2026-10-19, "someone just spamming imports is not a good candidate". In slice 4,
  make imports nothing uses a fact per file (`MAINTAINABILITY.unused_imports`, say), so padding counts
  against the applicant; 1.4's duplication catches the copy-paste kind.
- Add a fixture beside `test/fixtures/small-app/` when a case needs one, its golden under
  `test/goldens/`. Run `gradle kernelMap` before the commit, and write the journal entry.
