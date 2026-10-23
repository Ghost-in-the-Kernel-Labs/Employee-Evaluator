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

### 1.8 Navigability checklist

Check: `CLAUDE.md`/`AGENTS.md` presence and freshness (every path they name exists), module
boundary consistency and naming consistency on the fixtures; this repository scores above 80.
Done 2026-10-21 (J-029): the checklist (E-044) of agent docs, their paths' freshness, module cycles and
naming, each a fact; a repository worked by hand to 70; this one at 99, its two missing paths the planned
`score/` and `agent-runner/`.

### 1.9 Score card and the seed corpus

Check: `evaluator score <path>` prints the card as JSON and as Markdown for every fixture, each a
golden, its two efficiency ratios (SC-04: Quality per thousand lines, Maintainability per unit of
Complexity) computed by `score/` from the scores (moved here from 1.4, J-017); the cards for Suite Builder, Katana and this repository (Flight Recorder is not started; Ghost, 2026-10-22) are committed under `corpus/` and
survive Ghost's sanity read.
Done 2026-10-22 (J-050): `score/` (E-045), every phase-1 formula (E-044, E-046 to E-053), `evaluator score`
(E-054) and the fixtures' cards as goldens; the cards of Suite Builder, Katana and this repository in
`corpus/`. Ghost's read: Katana is harder than its card says, since the kernel counts its reference and
fixture code as its own; closed as phase-1 readings beside Fable's reference scores (trial 3), to be made
again once the judge classifies folders (E-057).

### 1.10 JavaScript adapter

Check: `.js`, `.mjs`, `.cjs` and `.jsx` files measured as TypeScript's are: every function (declarations,
methods, arrow and function expressions, `export async function`), cyclomatic and cognitive complexity,
imports (`import` and `require`) resolved to the repository's own files, unused imports; a small fixture
worked by hand and its golden. Parsed with the vendored TypeScript and TSX grammars first (JavaScript is
nearly a subset); a separate tree-sitter JavaScript grammar only if they fail, with its own reason in the
decision log and Ghost's approval (the supply-chain rules). Found in judge trial 4 (J-048): the Jensen
migration's 1,352 lines of JavaScript register no function.
Done 2026-10-22 (J-051): `JavaScriptAdapter` (E-058) reads JavaScript with the vendored TSX grammar and
`require` as an import; the fixture worked by hand and its golden; no new dependency. The Jensen migration
now registers 121 functions (17 before) and a dependency depth of 3 (0 before).

### 1.11 Tithe pass: the phase-1 boundary

Check: `gradle metrics` arms every metric of `docs/AGENT.md` and the test holds them; a generator
writes `docs/MODULES.md` from `docs/modules.json`; the tithe gate refuses a push past sixteen commits
without a tithe; the mutation tool is vendored and `docs/MUTATION.md` has its first run (E-019
moves to in force or is refused).
Done 2026-10-22 (J-052 to J-055): `test/MetricsTest.java` holds every metric with a target; `gradle modulesDoc`
writes docs/MODULES.md and ModulesTest refuses it stale; `scripts/prepush.sh` refuses a push past sixteen
commits without a tithe; PIT vendored (E-059), E-019 in force, and docs/MUTATION.md's run 1: 86 % of 1,714
mutants killed, its 243 others the baseline for a later pass.

### 1.12 Mutation survivors: run 1's 243, module by module

Check: each mutant run 1 left alive (`docs/MUTATION.md`) is killed by a new test or has a row under
"Survivors judged harmless" saying why; module by module, `rubric/Json.java` first, each module's run
recorded as it closes; no test weakened (E-019). Ghost's go, 2026-10-22.
Done 2026-10-23 (J-056 to J-058, J-072 to J-082, J-087): runs 2 to 14 of `docs/MUTATION.md`, `rubric/`, `score/`,
`history/`, `cli/`, `github/` and `analyze/` in six passes. Two real bugs found and fixed with tests that fail before the fix
(`Json` read `\r`, `\b` and `\f` as letters; `Commits.paths` split a path holding a newline in two). The network and sandbox
classes were reached without the real thing: loopback servers for `HttpGitHub` and `RegistriesProxy`, a script in place of `docker`
for `DockerSandbox.run`. No test weakened, no golden moved by this item.
