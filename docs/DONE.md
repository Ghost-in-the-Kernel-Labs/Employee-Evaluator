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

### 2.2e The judging prompt and its hash

Check: a prompt is files under `agent-runner/prompts/` (instructions with archetype anchors and navigability by its mechanisms,
E-057, taken from the trial notes in `docs/judges/`, and the answer schema), rendered with a summary into the text sent;
rendering is deterministic (a golden on a synthetic summary); the prompt's hash is the SHA-256 of its instructions, schema
and template version, not of the summary, and moves when any one byte of them moves (tested for each) and when nothing else
does; `agent-runner/prompts/<name>.hash` holds it and a test refuses a prompt whose hash line is stale; the kernel's facts
are shown "to judge, never to count" (E-055). No API, no key, no Ghost. Cost: none.
Done 2026-10-25 (J-114): the prompt `judge` is `agent-runner/prompts/judge.instructions.md`, `judge.template.txt` (first line `template-version: 1`) and `judge.hash` (`sha256 <hex>`), with the answer schema from 2.2d; `agent-runner/agent_runner/prompt.py` (`render`, `prompt_hash`, `check`, `python3 -B -m agent_runner.prompt hash|check|write-hash|render`) and `agent-runner/tests/test_prompt.py`. `render(summary)` is the exact text sent: the instructions with the schema's own `kind`, folder-class and tier descriptions quoted into them (E-082's precedence rule is the schema's, not a copy), the schema's bytes, then the summary, in one substitution pass so the summary is never scanned. The hash is the SHA-256 of the instructions, the schema and the template version, each with its name and length; it moves with one byte of any of the three and not with the summary. The golden is `test/goldens/prompts/judge-summary-notes.txt` (the synthetic notes summary), hand-checked: the fixture's planted instruction shows only inside its fences. The instructions take archetype anchors (round 2 v22), navigability by mechanisms (v08), the classes and the facts "to judge, never to count" (E-055, E-057); `agent-runner/agent_runner/prompt.py` says which note each part comes from. No API, no key, no Ghost.

### 2.2f The judge interface, a fake judge and the recorded-response replay

Check: a `Judge` takes a rendered prompt, a model id and a run number and returns the raw answer with the tokens used;
`FakeJudge` returns scripted answers (and an unparsable one on request); `ReplayJudge` returns the recorded answer for
the key (prompt hash, summary hash, model id, run number) from a recordings file and raises on a missing key, never
falling through to a live judge; the recordings format is fixed in the how-file and a test holds hand-written synthetic
recordings to it; tests of everything above this slice use these two and never the network. Nothing from a real run is
committed here (E-014; the question of what may be, below, is Ghost's). No API, no key. Ghost: not to build. Cost: none.
Done 2026-10-25 (J-115): `agent-runner/agent_runner/judge.py` and `agent-runner/tests/test_judge.py`. `Judge.judge(prompt, model, run)` returns an `Answer` (raw `text`, `input_tokens`, `output_tokens`); `prompt` is a `RenderedPrompt` (the text sent, the prompt hash of 2.2e, `summary_hash`, the SHA-256 of the summary text's UTF-8 bytes as given to `prompt.render`), built by `judge.rendered(summary)`. `FakeJudge` returns scripted answers in order (`FakeJudge.unparsable()` scripts one `judgment.parse` refuses) and raises `ScriptSpent` when the script is out. `ReplayJudge` returns the recording for (prompt hash, summary hash, model, run) and raises `MissingRecording` otherwise; it holds only its recordings (a test walks its attributes for any `Judge`), so there is no live judge to fall to; the run number is in the key, so 2.2h's three runs are three recordings. The recordings file is JSON lines: a `{"version": 1}` line, then one sorted-keys line per recording (`answer`, `model`, `prompt_hash`, `run`, `summary_hash`, `tokens` {`input`, `output`}) in key order; `parse_recordings` refuses a missing or extra field, a wrong version, a duplicate key, non-integer tokens and a key off the format; `judge_from_recordings(path)` loads one. A hand-written synthetic file `agent-runner/tests/fixtures/synthetic-recordings.jsonl` is held to it. Real recordings would live under `build/` and are never committed (E-014); no loader rule was called for. No API, no key, no Ghost.

### 2.2g The cost cap

Check: a `Budget` counts tokens (dollars once Ghost supplies prices and model ids, a data file with its date) with a cap per
call, per evaluation and per trial; it reserves the worst case (the prompt's tokens plus the output limit) before a call
and refuses the call, the judge never reached, when the reserve would pass a cap; it charges the real usage after; the
cost of every judgment is recorded beside it (the number that sets the unlock price floor, DESIGN open question). Tested
with `FakeJudge`: the call that would pass the cap is not made, a refused call charges nothing, per-evaluation and per-trial
caps are independent. No API, no key. Ghost: the caps' numbers and the prices are his, but the type is built with any numbers.
Cost: none.
Done 2026-10-25 (J-117): `agent-runner/agent_runner/budget.py` and `agent-runner/tests/test_budget.py`. `Budget(per_call, per_evaluation, per_trial, prices)`: each cap None (no cap, the default) or a number of tokens, 0 or more; `reserve(prompt_tokens, output_limit, model, ...)` holds the worst case and raises `BudgetRefused` (`.cap` is "call", "evaluation" or "trial", the first passed in that order) when the reserve alone passes the call cap or spent plus held plus the reserve passes the evaluation or trial cap; `charge(reservation, input, output)` replaces it by the real usage and appends a record; `release` gives a reserve back when a call fails; `begin_evaluation` and `begin_trial` start fresh independent counters. A usage above its reserve is charged in full, flagged `over_reserve` and listed in `overruns`, never a crash. `records` (one dict per charged call: evaluation, trial, model, run, both hashes, tokens, reserved, `over_reserve`, dollars, prices date) is written by `write_records(path)` under a `build/` directory only; a refused call adds a line to `refusals`, not to `records`. `BudgetedJudge(judge, budget, output_limit)` has the `Judge` interface, so 2.2h runs any judge under a budget unchanged. `estimate_tokens(text)` is ceil(UTF-8 bytes / 3), at least 1: it only reserves, and the real counts come from the API in 2.2l. Prices: `load_prices` reads a dated JSON file (format in the module's docstring); the real file does not exist, so dollars are off, and the synthetic one in the tests is meaningless. No cap or price of Ghost's is chosen anywhere. No API, no key.

### 2.2h Three runs, the mean, the vote and Opus on a split

Check: with a judge and a budget, `agent-runner/` runs the prompt three times on Claude Haiku 5.5, takes the mean of each
score and the vote of each category (kind, folder classes, tier), and runs Claude Opus 5.5 once more when the three
scores of a metric are more than 15 apart (15 does not, 16 does) or a category has no majority; the Opus answer for that
metric or category stands (E-055); every fact keeps its runs and the model behind it; an unparsable run is retried once and
then counts as missing, and fewer than two usable runs is a refusal, not a score. Table-driven with `FakeJudge`, the
boundaries at 15 and 16 and a three-way category tie each a case; a budget refusal mid-way returns what was judged, flagged
partial. No API, no key. Ghost: the tie rule (answered 2026-10-25: Opus decides, E-083). Cost: none.
Done 2026-10-25 (J-119): `agent-runner/agent_runner/ensemble.py` and `agent-runner/tests/test_ensemble.py`. `judge_repository(judge, prompt, haiku, opus)` runs the prompt on Haiku three times, reads each answer with `judgment.parse`, and returns a `Result` (status ok, partial or refused; `fields`, `folders`, `dropped_folders`, `calls`, `notes`, `models`; `dumps()` is JSON). Each judged field is a record `{value, method (mean, median, majority, union, opus, unresolved), runs [{model, run, value}], models, why}`. Scores of 0 to 100: the mean, or Opus when max - min is over 15 (15 does not, 16 does); confidence and evidence follow their score. Quality's 1 to 5: the median, Opus when more than 1 apart (a call for Ghost, `ensemble.py` docstring). Categories (kind, each folder path, the tier): the majority, or Opus when there is none, the tie rule Ghost gave (E-083); a path stands only when more than half of the usable runs list it. Run numbers: Haiku slot k is run k and its retry run 3 + k; Opus is run 1 and its retry run 2; a slot unparsable twice is missing; fewer than two usable runs is a refused result. A `BudgetRefused` stops the calls and the result is partial (refused, with fewer than two runs). Model ids are parameters, defaulting to `claude-haiku-5-5` and `claude-opus-5-5`, to confirm (question 3). No API, no key.

### 2.2i Agreement with the calibration set

Check: given a judgment per calibration id (from a `FakeJudge` or a replay), the harness reads `calibration/` and
computes, per field, the fraction of labels the judgment matches: Ambition tier exactly; Quality exactly and within one;
kind exactly (the judge's six values, E-082, include the labels' own-work, clone and generated; the set holds no generated, which the
record says, and a clone is judged from a short summary); beside each figure the baseline of always answering the most common label
(Quality within one reads 90 % for a constant 3 on this set, a floor met by nothing); it writes
`agent-runner/records/<prompt hash>.json` (prompt name and hash, models, runs, date, the figures, the baselines, and for each
opaque id the judged value and the label); a prompt with a figure under 80 % is marked not live for that field, and no record
for the prompt's current hash means no fact (SC-05). A fake judge right on exactly k of 30 gives k/30, worked by hand. No API,
no key. Ghost: confirm the definition (question below). Cost: none.
Done 2026-10-25 (J-120): `agent-runner/agent_runner/agreement.py` and `agent-runner/tests/test_agreement.py`. `load_labels()` reads `calibration/`; `judged_value(result)` reads an `ensemble.Result` (or its dict) and gives the kind, tier and Quality only from an ok result, a partial, refused or missing one is a miss on every field and counted apart; `compute`/`build_record` give, for ambition, quality_exact, quality_within_one and kind, matched over all labels, the constant-answer baseline (computed from the labels), `needed` and `live` with a `why`; `write_record` writes `agent-runner/records/<prompt hash>.json` (sorted keys, byte-stable, the date passed in); `is_live(records_dir, prompt_hash, field)` answers from a record's counts under the rule as it stands, and no record for the hash is not live (SC-05). Ghost's definition (E-084): live only if the raw agreement is at least 80 % AND the figure closes at least half of the gap between the baseline and 100 % (the half is the agent's proposal, a parameter, for Ghost to confirm). On the 30 labels: kind baseline 23, needs 27; Ambition baseline 14, floor binds at 24; Quality exactly baseline 17, needs 24; Quality within one baseline 27, needs 29. No record is committed: records of real runs are 2.2m's. No API, no key.

### 2.2j The kernel counts the folders the judge calls own code

Check: `evaluator score --judged <judgment.json>` takes a judgment in the 2.2d shape and measures Size, Complexity's
total and depth and Maintainability's parts only over files whose most specific classified folder is own code (E-057);
a hand-written judgment on a fixture gives Size, Complexity and Maintainability worked by hand, a file in no classified
folder keeps the kernel's own rule, and the same fixture without `--judged` is unchanged (no golden moves); the card names
the judgment's prompt hash and models. Touches `ingest/` (a filter over the inventory), `score/`, `cli/`; no new module and
no model client (E-003). No API, no key, no Ghost. Cost: none.

Done 2026-10-26 (J-121): `ingest/evaluator/ingest/Judgment.java` reads the judgment file (the wrapper `{promptHash, models, judgment}` round the 2.2d answer; anything else is refused with a message, a bare answer and an ensemble result named) and `OwnCode.java` applies its folders to an inventory: the longest classified folder holding a file decides, own code and files in no folder are counted, own tests are kept for the test-to-code ratio only, every other class is set aside as `judged: <class> (folder <path>)`, and a file the kernel set aside stays aside (E-085). `cli/` `Pipeline.code` measures the whole inventory once, narrows the report and the graph to the counted files and takes the ratio by class (`TestFiles.ratio` with a predicate); the navigability checklist keeps the whole inventory. The card's `produced` names the prompt hash and models (`score/Judged.java`). Worked by hand on `test/fixtures/judged/`: 22 lines in 5 files and 3 folders (38 in 10 and 8 without `--judged`), cognitive total 3 and depth 2 (6 and 3), tests part 0.35 (0.267), Maintainability weighted 0.8375 (0.81675). No golden of an existing card moved; the judged card (JSON, Markdown, facts) and the fixture's plain card are new goldens. Touched `analyze/` by two small additions the filter needed (`Report.restrictedTo`, `TestFiles.ratio` with a predicate). No API, no key, no Ghost.

### 2.2k The card's rules for the judged kind

Check: a judged fact is built with `Fact.judged` and confidence at most the recorded agreement of its prompt's hash (SC-05),
read from `agent-runner/records/`; a prompt with no record for its hash gives no fact; kind plan only withholds Size,
Complexity and Maintainability and reads Ambition unverified, kind generated gives the declined outcome with no card
(E-056, E-057); each case a fixture with a hand-written judgment and a golden. Reads `rubric/`'s `Fact`, `Calibration` and `Label`
only; no model client. No API, no key. Ghost: none. Cost: none.

Done 2026-10-26 (J-122): `ingest/evaluator/ingest/AgreementRecord.java` reads an agreement record's counts (`agent-runner/records/<prompt hash>.json` from 2.2i) and works liveness out exactly as `agreement.is_live` does (80 % floor and half the gap over the baseline, exact integer arithmetic; the stored flag is never read; checked against the Python on 12,840 combinations), and `JudgedRules.java` applies E-056/E-057/E-082: kind generated declined, plan only withholds Size, Complexity and Maintainability and reads Ambition unverified, clone scored with a note, built, mixed and own-work scored as measured; Ambition and Quality within one, each when live, become `AMBITION.tier` and `QUALITY.agent_review` through `Fact.judged`, confidence at most the recorded agreement; a field not live or a prompt with no record gives no fact. `score/` `Declined.java` is the declined outcome (a respectful message, no card, exit status 2) and `Judged` carries the notes onto the card; `cli/` has `--records <folder>` (default `agent-runner/records`). Eight fixtures with hand-written judgments and synthetic records, each a golden in three forms (`JudgedRulesGoldensTest`); no existing golden moved. E-086 (proposed, for Ghost to confirm: clone, the declined shape and exit status, Quality's gate). No formula reads the new facts yet (2.3, 2.4).
