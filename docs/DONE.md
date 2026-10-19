# Done: the items of TODO.md that are finished, with their numbers kept

`test/TodoTest.java` holds that no number is both here and in `TODO.md`.

## Phase 1: the rubric and the scorer CLI

### 1.1 Scaffold

Check: a clean clone builds and tests with the network off; `evaluator score <path>` runs and says
what it does not do yet; every doc test holds; the kernel map holds every function.
Done 2026-10-18 (J-003): the parents' guardrails ported, CLI stub, doc tests, kernel map, pinned
image, CI.

### 1.2 Rubric types and repo ingestion

Check: `rubric/` holds the Fact and ScoreCard types of `docs/SCORE-CARD.md`, and a doc test holds
the document to them (every type and invariant in both). `ingest/` reads a fixture repository under
`test/fixtures/` into an inventory, vendored and generated code set aside, and its Size facts; the
inventory's JSON is a golden, byte for byte.
Done 2026-10-19 (J-006): `rubric/` (Fact, ScoreCard and their parts; `test/ScoreCardDocTest.java`),
`ingest/` (Inventory, the set-aside rules E-025, the Size facts E-026), the small-app fixture and
its golden.

### 1.3 Static analysis adapters

Check: one adapter interface; adapters for TypeScript and Python (tree-sitter, vendored with its
licenses and native libraries, E-028; its Java binding and the image proven first) and for Java (the JDK's own parser, as `scripts/Scan.java` does); each gives cyclomatic and
cognitive complexity per function and the dependency graph on the fixtures; the Complexity facts
are goldens. Two adapters disagreeing on one function is a finding (`docs/FINDINGS.md`).
Done 2026-10-19 in four slices (J-010, J-012, J-013, J-014): tree-sitter vendored (E-028); one adapter
interface with Java (the JDK's parser), TypeScript and TSX, and Python (tree-sitter) adapters, counted
by one set of rules (E-030, E-031); the dependency graph, its longest chain and unused imports
(E-032); every fixture's values worked by hand, then golden. No two adapters read one language, so no
disagreement could arise; the one pair that measured the same thing (the kernel and `scripts/Scan.java`)
now shares its code.
