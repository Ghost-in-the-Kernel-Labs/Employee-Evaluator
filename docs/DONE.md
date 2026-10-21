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

### 1.4 Maintainability

Check: coupling, duplication by token shingling and the test-to-code ratio on the fixtures; the
Maintainability facts and the two ratios are goldens; a fixture with a copied function is caught.
Done 2026-10-19 (J-016, J-017): test files and the test-to-code ratio (E-034), coupling (E-035),
duplication by token shingling (E-036), each a fact proven on a fixture; the duplication fixture's
renamed copy caught, and its Maintainability facts a golden. The two ratios are ratios of scores, which
are `score/`'s (MOD-03): moved to 1.9 by Ghost's call.

### 1.5 Test runner adapter

Check: a fixture's own tests are found and run inside the sandbox (no network, no credentials, a
timeout, a memory cap), on a copy stripped by `Quarantine.strip` first and with no install script run
(E-033); pass rate and coverage become `QUALITY.tests` facts; a fixture whose tests try to reach
the network fails the run and says so.
Done 2026-10-21 in four slices (J-018 to J-022, J-026): the readers of a run's reports as hostile files;
the runner behind a `Sandbox` interface, each language's plan (E-037, E-038); the sandbox image, every
piece pinned and its build offline (E-041), and the registries proxy; four fixtures that run. Proven in
real containers in this session's Docker: `gradle test -Pdocker`, 102 tests, the fixtures giving the
facts their recorded runs gave, the network attempt named. On paper still: the install phase through
the registries network and its proxy in Docker (the proxy proven on loopback; this session's egress
cannot be routed through it).

### 1.6 Git history analysis

Check: git runs on a submission only with its hooks off (`-c core.hooksPath=/dev/null`, E-033); spread, diff variance, reverts and fixups, test co-location and agent fingerprints on
fixtures with real histories; a fixture replayed as 300 uniform commits in one hour scores under 30
on history shape; one with weeks of varied work scores over 70.
Done 2026-10-21 (J-027): `history/` reads a repository's commits through a git whose hooks are off and
whose repository-set commands (fsmonitor, pager, external diff, textconv) are overridden, proven by a
planted fsmonitor that plain git runs and ours does not; spread, cadence, diff variation, the messy
middle, test co-location and agent fingerprints, each a fact; the shape (E-042): a history worked by
hand to 70, a replay faking all but time at 23, six weeks of work over 70.
