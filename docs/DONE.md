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

### 1.7 GitHub adapter

Check: signed, unsigned-but-pushed, and foreign-author commits are told apart on a real repository
of Ghost's through the API, with the token read from the environment and never logged; the module
is the only one that reaches the network (`docs/modules.json`).
Done 2026-10-24 (J-102, J-103): `HttpGitHub` and `Authorship` (E-043) told Suite Builder's commits apart through GitHub's API with the token from the environment, never logged. The live run, 8 signed, 302 pushed, 190 claimed, 268 foreign, 0 unmatched, applicant share 0.651 over a 20-day push window, went through a session proxy that answers without a valid token, so it proves the adapter and not the token; Ghost took it as enough.

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

### 1.13 Baseline scoring

Check: E-070 built: each counted aspect scored as 100 × its measure ÷ Suite Builder's at 32762a6 (inverted
where more is worse), Size first, then Complexity and Maintainability's parts, one commit each, each worked by
hand on a fixture; the baseline's measures recorded with the commit they came from; the rubric's hash carrying
the baseline; every moved card golden reviewed. With the first metric, a baselined score may pass 100 (E-071): `Score`'s
upper bound and SC-01's "each 0–100" go for counted metrics, while Provenance, Quality and Navigability keep
0–100. E-070 and E-071 move to in force.
Done 2026-10-23 in three slices, one commit each (J-091, J-092, J-094): Size (E-070: 100 × lines ÷ 84,971), Complexity (E-074: 0.6 × cognitive ÷ 13,361 + 0.4 × depth ÷ 28, each ×100)
and Maintainability (E-075: E-051's weighted score ÷ Suite Builder's 0.895670359282887, ×100), each against Suite Builder at 32762a6 with its baseline in `Formulas`, a test that the baseline reads 100
against itself, no ceiling (E-071: `Metric.counted` holds the three, `ScoreCard` refuses over 100 for the rest), the rubric (version 4) naming the baseline, and every moved card golden read and checked by hand.
E-070 and E-071 are in force.

## Phase 2: the evaluation engine with agents

### 2.1 Calibration set

Check: 30 repositories labeled by Ghost on Ambition and Quality, in a fixture format with a test
that holds every label to its fields; the spread of domains is recorded in the private id-to-repository map outside the repository (E-014).
Done 2026-10-24 (J-106): 30 labels in `calibration/` (c001 to c030), each exactly id, kind, ambition and quality. Kind spread: own-work 23, clone 7,
generated 0. Ambition spread: utility 14, product 10, toy 3, research 3. Quality spread: 1 x 2, 2 x 6, 3 x 17, 4 x 4, 5 x 1. The labels were drafted by Claude Sonnet agents
applying the rubric (`docs/SCORE-CARD.md`, `docs/DESIGN.md`) and reviewed by Ghost, who confirmed them. The id-to-repository map and the domain spread live outside the repository (E-014).
The real-folder test reads the set as MET at 30.

### 2.2a The agent-runner scaffold and its gate

Check: `agent-runner/` exists as a Python 3 package (standard library only) with `docs/how/judge-harness.md` (the
workpath, one row in `CLAUDE.md`) and a Java test, `test/AgentRunnerTest.java`, that runs `python3 -I -m unittest discover`
in it and fails the build when a Python test fails (PENDING and green when `python3` is absent, as `CalibrationSet` is
before 30 labels); a Python test fails any `agent-runner/` file other than `transport.py` (2.2l) that imports `socket`, `http`,
`urllib`, `ssl` or `anthropic`, and one that reads an environment variable naming a key. No API, no key; Ghost is not needed
(whether the pinned image gains `python3` is a question for him: until it does the gate reports PENDING and agents run the
tests by hand). Cost: none.
Done 2026-10-25 (J-110): `agent-runner/` as a Python 3 package on the standard library (`agent_runner/`, `tests/`), `agent_runner/guard.py` and `tests/test_guard.py` (no file but the future `transport.py` imports a network or
model-client module or reads an environment variable naming a key; the guard parses, never runs, a file), `test/AgentRunnerTest.java` (in `DocTests`: runs `python3 -I -B -m unittest discover`, PENDING when `python3` is absent),
`docs/how/judge-harness.md` with its row in `CLAUDE.md`. Ghost approved `python3` in the pinned image (E-081). Not a Java module, so `docs/modules.json` gains nothing (docs/DESIGN.md, "Step 2.2 sliced").

### 2.2b The kernel's brief

Check: `evaluator brief <path>` prints one JSON document, byte-identical across two runs on each fixture and a golden:
the inventory (path, language, lines, why set aside), the kernel's facts (the `--facts` values, E-076), the commit log
(short hash, date, subject, files changed, capped at 200 newest) and the paths of any README and agent instruction files; no
file's contents and no author name or email. It only reads and reports what the existing modules count (no new Java module). No API,
no key, no Ghost. Cost: none.
Done 2026-10-25 (J-111): `cli/evaluator/cli/Brief.java` and `Main.brief` print `inventory` (`Inventory.toJson`), `facts` (`FactsText`'s lines as an object), `commitsTotal`, `commits` (newest 200, short hash, UTC date, subject cut at 200 characters, files changed), `readme` (root README files) and `agentInstructions` (the E-044 list), from `Navigability.readmes` and `Navigability.agentDocs`. Tests are not run. Test: `test/evaluator/cli/BriefTest.java`, golden `test/goldens/briefs/small-app.json` (checked by hand); a stability run on all ten card fixtures. No new decision, no new module.

### 2.2c The repo summary builder

Check: `agent-runner/` turns the brief and the checked-out path into a summary: the README (trimmed), sampled files
(a fixed rule: the largest files by lines in each folder the inventory holds, then a sample of tests, to a byte cap), the commit
log and the facts, every part labelled with its path; the same input gives the same bytes (a golden on two of our own
synthetic fixtures); a file is cut at the cap with a marker, never dropped silently; every piece of the repository's text sits
inside a fence the prompt names data (E-013), control characters and ANSI escapes stripped, and a fixture whose README says
"ignore your instructions" shows it only inside the fence. The judge is given no tool (E-013): this summary is all it reads.
Summaries of real repositories are written only under `build/` (E-014). No API, no key, no Ghost. Cost: none.
Done 2026-10-25 (J-112): `agent-runner/agent_runner/summary.py` (`build(brief, root)`, `python3 -I -B -m agent_runner.summary BRIEF CHECKOUT OUT`, OUT refused unless under `build/`) and `tests/test_summary.py`. The sampling rule: the README (trimmed, 4000 bytes), the facts, the newest 30 commits, the folder table, then in each folder of the inventory its 2 largest non-test files by lines (ties by path), then 3 test files evenly spaced through the tests in path order; a file is cut at 3000 bytes and the files together at 24000, each cut with a marker (`[cut at N bytes; M bytes not shown]`) and each file the total cap leaves out named in a `not-shown` fence. Every piece of repository text (paths too) sits in a fence named `data`, longer than any tilde run inside it, its label a one-line JSON string; ANSI escapes, control and bidi characters stripped. `JUDGE_TOOLS` is empty (E-013). Goldens `test/goldens/summaries/summary-notes.txt` (the "ignore your instructions" README) and `summary-shop.txt` on our own fixtures `test/fixtures/summary-notes`, `summary-shop` with briefs in `test/fixtures/summary-briefs/`, checked by hand; rewritten by `EVALUATOR_WRITE_GOLDENS=1 python3 -I -B -m unittest discover` in `agent-runner/`. No new decision.

### 2.2d The judgment schema and its parser

Check: `agent-runner/` holds one JSON schema file for the judge's answer, and a validator that holds an answer to it: the
repository's kind (generated, clone, plan only, mixed, own-work, built, with the schema's rule for which to take first; E-057, E-082), each
folder's class (own code, own tests, own tooling, docs, generated, vendored, reference, fixture data), the 0–100 scores of the metrics
that need judgment (Quality's review, Complexity's assessment, AI navigability) with a confidence and evidence, the Ambition tier (toy, utility, product, research), Quality 1–5 and a short verdict. A valid answer round-trips; each
of these is refused with a message naming the field: a score of 101, an unknown kind, an unknown folder class, a missing
evidence list (SC-02), an extra field, free text where an enum belongs. A kind of own-work and of clone is accepted. The schema is kept to the subset
structured output enforces, checked by the validator itself. The schema file is the one the API is later given (2.2l). No API, no key, no Ghost. Cost: none.
Done 2026-10-25 (J-113): `agent-runner/schema/judgment.schema.json` and `agent_runner/judgment.py` (`parse`, `validate`, `check_schema`, `load_schema`, `dump`), tested by `tests/test_judgment.py`. The answer: `kind` (six values, E-082), `folders` (a list of path and class), `scores` (quality, complexity, navigability: score 0–100, confidence 0–1, a non-empty evidence list), `ambition` (tier, confidence, evidence), `quality` 1–5 and `verdict` (at most 400 characters). The metrics asked are the card's `agent-runner/` row (docs/SCORE-CARD.md) and E-057: Size, Maintainability's parts and Provenance are the kernel's. New decision E-082: Ghost added own-work and clone to the judge's kind, superseding E-057's list of kinds only.
