# theGhost: the decisions Employee Evaluator stands on

## Supply-chain rules: in CLAUDE.md

Ghost's Supply-Chain Security Baseline (2026-10-19) has one copy, in `CLAUDE.md`, where every agent reads it on
entry (E-073). `test/SupplyChainTest.java` holds it there, holds that this file keeps no second copy, and holds
the repository to it (E-033).

## What this file is

This is the baseline of intention. [docs/GHOST-IN-THE-KERNEL.md](docs/GHOST-IN-THE-KERNEL.md) says why
the codebase must remember why it exists; this file is that memory, for the decisions that shape it.
Comments say what one piece of code is for; the decisions here are the layer beneath them, the
reasons the pieces are shaped as they are. Everything else should be extensible from it: a plan
builds on these decisions, a rule enforces one, a test proves one holds.

**Every major decision is recorded, including those the design already implies.** The code shows
*what* was chosen, never *why*, nor what was weighed against it. The decisions against are recorded
too, so they are not proposed again without new evidence.

## How an entry is written

```text
### X-000: the decision, in one line (X is E)
- **Decision:** what was decided, plainly.
- **Why:** the reason, and the evidence it came from.
- **Held by:** the code, rule, test or document that keeps it true.
- **Source:** who decided and when.
- **Status:** in force | proposed | refused | withdrawn | superseded by E-000 (a parenthesis after a status may not hold a `;`: the test splits statuses on it first)
- **subject:** one or two words, so entries can be grouped
- **See also:** E-000 (optional)
- **Supersedes:** E-000 (only on a decision that replaces one; the old one's status names this one)
```

Append, never rewrite history. A decision that changes gets a new entry; the old one says "superseded
by" the new one, so the reasoning that was replaced stays readable. Ids are consecutive; a gap is
refused unless a paragraph opening "Reserved" names it. `test/GhostTest.java` holds all of this.

## Lineage

Katana (R-001 onward) and Suite Builder (G-001 onward) are this project's parents. A decision taken
from one of them says so in its Why; the reasoning there is not copied here, only pointed at.

## Purpose and shape

### E-001: Java 21 for the kernel, TypeScript for the web, Python for the agent runner
- **Decision:** The scorer, the engine and the API are Java 21 on Gradle with JUnit 5. The web
  frontend (phase 3) is TypeScript. The agent runner (phase 2) is a small Python worker around the
  Claude Agent SDK. Each lives in its own folder; the kernel is the Java.
- **Why:** At the scale planned (tens of thousands of submitters) the cost is model API spend and the
  sandbox fleet, not orchestration; the JVM's concurrency and static-analysis ecosystem fit the
  engine, and Ghost is faster in Java. The Agent SDK ships in TypeScript and Python, not Java.
- **Held by:** `build.gradle`; `docs/modules.json` (MOD-01: the kernel never imports a model client).
- **Source:** Ghost, with Fable, 2026-10-18.
- **Status:** in force
- **subject:** languages
- **See also:** E-003, E-021

### E-002: The website and the GitHub app are one web app
- **Decision:** There is one web application: GitHub sign-in, the submission flow, the employer board
  and billing. There is no separate "GitHub app" product; the GitHub App registration is how the web
  app reads repositories with scoped, revocable permissions.
- **Why:** Ghost listed a website and a GitHub-integrated app as two components and asked whether
  they could merge; they are one front door with one identity (E-012's layer 1).
- **Held by:** `docs/DESIGN.md` (Architecture).
- **Source:** Ghost, 2026-10-18.
- **Status:** in force
- **subject:** architecture

### E-003: No model runs in the kernel; judgment arrives as facts
- **Decision:** No module of the Java kernel imports a model client. Agent judgments (Ambition, the
  agent half of Quality, the navigability task) run in `agent-runner/`, behind a queue, and reach the
  kernel as Facts with evidence and confidence (E-017). The kernel can score a repository with no
  model at all; it then reports the judgment metrics as not measured.
- **Why:** Katana's R-004 (no model at conversion time) made the compiler testable and its output
  reproducible; here the same split keeps the deterministic metrics deterministic, lets the kernel's
  tests run offline, and puts every model call behind one cost-capped door.
- **Held by:** `docs/modules.json` MOD-01; `test/ModulesTest.java`.
- **Source:** Ghost, with Fable, 2026-10-18.
- **Status:** in force
- **subject:** architecture
- **See also:** E-001, E-017

### E-004: Modules with declared imports, held by a test
- **Decision:** The kernel is modules, one folder each at the root, package `evaluator.<module>`.
  Each is an entry in `docs/modules.json` with what it may and must not import. Every import in the
  source is checked against it. A new module or a new import is an architecture decision for Ghost.
- **Why:** Suite Builder's finding: the module map (and the kernel map, E-005) was the biggest
  context reducer in the codebase, and the guardrail that tripped when an agent nearly overwrote
  another area's function. Katana carried it over as `modules.json` with a test; so does this.
- **Held by:** `docs/modules.json`; `test/ModulesTest.java` (cycles, rules MOD-01 to MOD-04, every import).
- **Source:** Ghost, 2026-10-18, from Suite Builder and Katana.
- **Status:** in force
- **subject:** architecture
- **See also:** E-005, E-016

### E-005: This file is the decision record; every decision-holding function cites one
- **Decision:** Every decision is an entry here, numbered, append-only. A function that holds a
  decision cites it on the line above, `// Decision: E-nnn`. The kernel map (`docs/KERNEL-MAP.md`)
  records every function and the decision it cites, append-only, generated by `gradle kernelMap`.
- **Why:** Suite Builder's ghost document stopped an agent from overwriting another area's code
  before any logging existed; its append-only kernel map was the guardrail that tripped. Katana kept
  both. The decisions are the layer beneath the comments; the map is how a function is found.
- **Held by:** `test/GhostTest.java`; `test/KernelMapTest.java`; `scripts/KernelMap.java`.
- **Source:** Ghost, 2026-10-18, from Suite Builder (G-011, G-105) and Katana (R-008, R-042).
- **Status:** in force
- **subject:** intent
- **See also:** E-004, E-006

### E-006: Every document that can drift is held to the code by a test
- **Decision:** A document a test cannot hold is a hope. `CLAUDE.md`'s paths exist or belong to an
  open step; `TODO.md` is in order with a check per item and the first open item's check written;
  `docs/DONE.md` and `TODO.md` share no number; the kernel map matches the source.
- **Why:** Suite Builder's G-080: a principle with no failing test is a wish; Katana's doc tests made
  the plan and the tree agree every push.
- **Held by:** `test/DocsTest.java`, `test/TodoTest.java`, `test/KernelMapTest.java`, `test/DocTests.java`.
- **Source:** Ghost, 2026-10-18, from Suite Builder (G-080).
- **Status:** in force
- **subject:** intent

### E-007: One word for one thing: the glossary and its enforced aliases
- **Decision:** `docs/GLOSSARY.md` names the project's words. An alias written in backticks in its
  "Not" column is refused wherever it appears in a document outside the glossary and `docs/research/`.
- **Why:** Katana's R-039: two words for one thing cost every reader a lookup, and an agent a wrong
  search. Enforcing the few that matter keeps the rest a review guide.
- **Held by:** `test/GlossaryTest.java`.
- **Source:** Ghost, 2026-10-18, from Katana (R-039).
- **Status:** in force
- **subject:** intent

### E-008: The journal: every event, in fixed fields, by whoever was there
- **Decision:** `docs/JOURNAL.md` records what happened while the work was done (a contradiction
  found, a decision, a failure caught or missed, a measurement, a change of direction), one entry per
  event with fixed fields, newest last, written by the agent or Ghost in the same commit when possible.
- **Why:** Katana's R-046 and Suite Builder's G-162: `git log` says what changed, never what was on
  the table; a journal with fixed fields can be counted across projects, which the NSF study needs.
- **Held by:** `test/JournalTest.java`.
- **Source:** Ghost, 2026-10-18, from Katana (R-046).
- **Status:** in force
- **subject:** intent

### E-009: Doc tests are plain Java with no dependency, run under JUnit
- **Decision:** The tests that hold the documents and the kernel are plain Java 21 with no library,
  each runnable alone through its `main` and all run under JUnit by `test/DocTests.java`. Each holds
  the repository and also rejects a deliberately broken input, so a check that stops catching anything
  is caught itself.
- **Why:** Katana's R-038: a check that only ever sees sound input can rot silently; one that must
  also fail on a broken fixture proves it still reads.
- **Held by:** `test/Check.java` (`repository`, `rejects`, `accepts`).
- **Source:** Ghost, 2026-10-18, from Katana (R-038).
- **Status:** in force
- **subject:** tests

### E-010: The command line decides nothing
- **Decision:** `cli/` turns what was typed into a call on another module and prints what came back.
  No module imports it; no decision lives in it.
- **Why:** Suite Builder's CLI rows: the work is in the project modules; the command line only says
  it. A decision in the CLI is one the tests of the modules never see.
- **Held by:** `docs/modules.json` MOD-04; `cli/evaluator/cli/Main.java`.
- **Source:** Ghost, 2026-10-18.
- **Status:** in force
- **subject:** architecture

## Scoring

### E-011: Seven metrics, ranked separately; no composite score
- **Decision:** Quality, Complexity, Size, Ambition, AI navigability, Maintainability and
  Provenance, each 0–100, each ranked on its own. Size and Complexity are shown raw and ranked
  through efficiency ratios (Quality per thousand lines, Maintainability per unit of Complexity). No
  single overall number is ever shown.
- **Why:** Employers hire for different things; a composite hides the trade-off. Raw Size and
  Complexity reward padding; the ratios make a 6,000-line clean solution beat a 40,000-line mess.
- **Held by:** `docs/SCORE-CARD.md` (SC-01, SC-04); `rubric/` once built.
- **Source:** Ghost, with Fable, 2026-10-18.
- **Status:** in force (seven metrics, no composite); superseded by E-047 (how the two ratios are made)
- **subject:** scoring

### E-012: Provenance is a score, not a gate
- **Decision:** A submission with weak evidence of authorship is shown as Unverified, never rejected.
  Three computed layers (identity by GitHub sign-in, authorship by signed or pushed commits, the
  history's shape) make a 0–100 score; a fourth, a recorded follow-up session, is deferred.
- **Why:** Rejecting loses every honest applicant who never set up commit signing; the employer can
  price the uncertainty. Commit count is forgeable in an evening; the shape of a history is not.
- **Held by:** `docs/DESIGN.md` (Provenance); `history/` and `github/` once built.
- **Source:** Ghost, with Fable, 2026-10-18.
- **Status:** in force
- **subject:** scoring

### E-013: A submitted repository is hostile input
- **Decision:** README text, comments, commit messages and file names are data, never instructions.
  A submitted repository's code runs only inside a sandbox with no network except GitHub, under a
  timeout and a cost cap. No agent judging a repository is given a tool the repository could turn.
- **Why:** Rankings with money attached will be attacked; Suite Builder's review of 2026-10-11 found
  the same hole (hostile crawled-site input) and closed it. The engine must assume the worst submitter.
- **Held by:** `docs/DESIGN.md` (phase 2, Security note); the sandbox once built (TODO.md 1.6).
- **Source:** Ghost, with Fable, 2026-10-18.
- **Status:** in force
- **subject:** security

### E-014: Nothing from an applicant enters this repository
- **Decision:** No submitted repository, no snapshot, no identity ever enters this repository. The
  seed corpus is the score cards of our own repositories (Suite Builder, Katana, Flight Recorder),
  committed under `corpus/`.
- **Why:** Katana's R-037 and Suite Builder's G-085: what a customer gave us is theirs; the
  platform's IP promise (snapshots live only as long as scoring) starts with its own tree.
- **Held by:** `.gitignore`; review.
- **Source:** Ghost, 2026-10-18.
- **Status:** in force
- **subject:** security

## How work is done

### E-015: The navigation tithe
- **Decision:** One commit in eight changes no behaviour and improves the agent's environment or the
  code's navigability; every phase ends with a tithe pass; the metrics in `docs/AGENT.md` ratchet and
  may not regress without a decision here. `gradle metrics` says when one is due.
- **Why:** Ghost's finding in Suite Builder: delivery speed rose once the agent's ability to find,
  read and change code with little context was treated as work, about a tenth of effort; Katana
  made it a rule (R-008) and its tithe gate. This project has three languages in the tree, so the
  share rises.
- **Held by:** `docs/AGENT.md`; `scripts/Metrics.java` (the cadence warning); the tithe gate once built (TODO.md 1.11).
- **Source:** Ghost, 2026-10-18, from Suite Builder and Katana (R-008).
- **Status:** in force
- **subject:** process
- **See also:** E-005

### E-016: The kernel first, then width
- **Decision:** Build in this order: the rubric and the deterministic scorer (phase 1), the judging
  engine (phase 2), the web app (phase 3). No phase starts until the one before it is proven on real
  repositories. Within a phase, the foundation before the next layer.
- **Why:** Ghost's design philosophy from Suite Builder: do the kernel architecture before expanding
  wide, to save context, lower token burn, and avoid structural problems that make refactoring painful.
- **Held by:** `TODO.md` (one chain, in order); `test/TodoTest.java`.
- **Source:** Ghost, 2026-10-18, from Suite Builder.
- **Status:** in force
- **subject:** process
- **See also:** E-004

### E-017: The score card is the intent map: facts with evidence, confidence and state
- **Decision:** Every score is built from Facts. A Fact names what it is about (a file, a function, a
  commit, the whole repository), the evidence it was read from, a confidence, and a state: inferred,
  confirmed or refuted. A person (Ghost in calibration, an employer in review) confirms or refutes; a
  refuted fact never scores. No judgment metric scores a live submission until its judging prompt has
  been run against the labeled calibration set and the agreement recorded.
- **Why:** Katana's Intent IR (R-029): a fact a person can check beats a number nobody can question;
  Ghost wants every inferred fact laid out in a checkable file with a correctness probability. Here
  the file is the employer-facing review, and the probability is the confidence.
- **Held by:** `docs/SCORE-CARD.md` (SC-02, SC-03, SC-05); `rubric/` once built.
- **Source:** Ghost, with Fable, 2026-10-18, from Katana (R-029, R-064).
- **Status:** in force
- **subject:** scoring
- **See also:** E-003, E-011

### E-018: Goldens, and one change per commit
- **Decision:** A score card for a fixture repository is a golden, byte for byte. A refactor changes
  no golden and is committed on its own, first. One change per commit; a changed golden is reviewed
  as a diff and explained in the commit.
- **Why:** Suite Builder's G-083 and G-084, Katana's P-02: a golden is the cheapest proof that a
  refactor changed nothing, and the only honest record of what a scoring change did.
- **Held by:** the goldens under `test/` once the first fixture exists (TODO.md 1.2); review.
- **Source:** Ghost, 2026-10-18, from Suite Builder (G-083, G-084).
- **Status:** in force
- **subject:** tests

### E-019: Every new or changed unit test is tried by mutation
- **Decision:** A mutation tool runs over every new or changed unit test; each surviving mutant gets a
  test or is written up as harmless in `docs/MUTATION.md`. A test is never weakened to make code pass.
- **Why:** Suite Builder's G-239: the first mutation run found tests that caught 53% of mutants in a
  core file; after it, 97%. Tests that test nothing are worse than none, because they are trusted.
- **Held by:** proposed: `docs/MUTATION.md` and the vendored tool once TODO.md 1.11 lands; until then review.
  Now: PIT, vendored by E-059, run by `gradle mutate`; `docs/MUTATION.md` records each run and each survivor.
- **Source:** Ghost, 2026-10-18, from Suite Builder (G-239).
- **Status:** in force (E-059 vendored the tool, TODO.md 1.11)
- **subject:** tests

### E-020: The maintainer is Ghost; one branch, main
- **Decision:** In every file, comment and commit the maintainer is "Ghost" (GitHub:
  ghost-in-the-kernel). Commits are authored as Ghost (`git config user.name Ghost`, `git config
  user.email 118085043+ghost-in-the-kernel@users.noreply.github.com`); an agent keeps its
  Co-Authored-By line. There is one working branch, `main`; a finished step lands on it at once.
- **Why:** Suite Builder's G-085 and G-162, Katana's R-040: commits should count on Ghost's profile,
  and work on a branch nobody merges is work nobody has.
- **Held by:** review; `scripts/prepush.sh` (fetches `main` before a push).
- **Source:** Ghost, 2026-10-18, from Suite Builder and Katana.
- **Status:** in force
- **subject:** process

### E-021: The environment: one pinned image, offline, vendored
- **Decision:** Everything runs in one container image pinned by digest (`Dockerfile`: JDK 21, Gradle
  8.14.3, git). A clean clone builds and tests with the network off; every dependency is a file under
  `third_party/` with its license and hash in `third_party/MANIFEST.md`. An agent never installs tooling.
- **Why:** Katana's R-001 and AGT-01 to AGT-07: a build that needs the network is a build that fails
  in CI, in a sandbox and in a year; a pinned image is the one environment every session shares.
- **Held by:** `Dockerfile`; `build.gradle` (no repository declared); `.github/workflows/ci.yml` (`--network none`).
- **Source:** Ghost, 2026-10-18, from Katana (R-001).
- **Status:** in force
- **subject:** environment
- **See also:** E-001

### E-022: A research findings list
- **Decision:** `docs/FINDINGS.md` records every flaw the project's tests or runs prove in an
  existing tool, metric or practice (a static analyzer that miscounts, a hiring heuristic the data
  refutes), each with the evidence, so an evaluator can see what the project has discovered.
- **Why:** Ghost's directive for Katana: a research department keeps a findings list; the NSF study
  (Ghost in the Kernel versus conventional LLM-assisted development) needs one per project.
- **Held by:** `docs/FINDINGS.md`; review.
- **Source:** Ghost, 2026-10-18, from Katana.
- **Status:** in force
- **subject:** research

### E-023: The public pitch attacks the process, never the people
- **Decision:** Public copy (the site, the README, the board) argues against resumes, live coding and
  volume. It never calls the buyer's staff incompetent. The internal pitch may.
- **Why:** The buyer is the HR director or the CTO beside them; copy that insults them closes the tab
  before the contract opens. The same argument lands aimed at the process.
- **Held by:** `docs/DESIGN.md` (Pitch, "Tone note"); review.
- **Source:** Ghost, with Fable, 2026-10-18.
- **Status:** in force
- **subject:** pitch

### E-024: GitHub's minutes are spent by hand, never by a push
- **Decision:** The workflow in `.github/workflows/ci.yml` runs only on `workflow_dispatch`. No push,
  branch or pull request starts a run. The check before a push is `scripts/prepush.sh`, the same
  loop on a fresh clone in the pinned image, locally. A run on GitHub is for what a local machine
  cannot do (a Windows build, a run Ghost wants on record), and Ghost starts it.
- **Why:** Ghost's finding across the projects (2026-10-19): the organization's Actions minutes are
  severely constrained and, apart from his own PC, GitHub is the only place Windows builds work.
  Katana ran under the same constraint with its pre-push script as the gate.
- **Held by:** `.github/workflows/ci.yml` (`on: workflow_dispatch` alone); `scripts/prepush.sh`.
- **Source:** Ghost, 2026-10-19.
- **Status:** superseded by E-040
- **subject:** environment
- **See also:** E-021

## Reading a repository

### E-025: Vendored and generated code is set aside by what the repository shows, and says why
- **Decision:** A source file is set aside, not counted, when a folder on its path is a vendoring or
  build folder (`node_modules`, `vendor`, `third_party`, `dist`, `build`, `target`, ...), when the
  repository's own `.gitattributes` marks it `linguist-vendored` or `linguist-generated`, when its name
  is a generator's (`*.min.js`, `*_pb2.py`, `*.pb.go`, `*.generated.ts`, ...), or when one of its first
  five lines carries a generator's mark (`@generated`, `Code generated`, `DO NOT EDIT`). The first
  rule that matches is written beside the file in the inventory. Nothing is run to decide.
- **Why:** Size and every ratio built on it reward padding if copied or generated code counts; an
  applicant who vendors a library did not write it. The rules are the ones GitHub's linguist and the
  generators themselves use, so an honest repository is read as its own tooling already reads it,
  and a reason beside each file lets a person check (E-017). Reading, never running, keeps the
  hostile-input rule (E-013).
- **Held by:** `ingest/evaluator/ingest/SetAside.java`; `test/evaluator/ingest/SetAsideTest.java`;
  the small-app fixture's golden (`test/goldens/small-app/inventory.json`), one file per rule.
- **Source:** Claude, building TODO.md 1.2, 2026-10-19; approved by Ghost, 2026-10-19 (J-009).
- **Status:** in force
- **subject:** ingestion
- **See also:** E-013, E-026

### E-026: What Size counts: non-blank lines, files and folders of the applicant's own source
- **Decision:** The Size facts are `SIZE.source_lines` (lines that are not blank, comments included,
  in counted source files), `SIZE.source_files`, `SIZE.source_folders` (folders that directly hold
  counted source) and `SIZE.set_aside_files`. A file is source when its ending names a programming
  language; documents, data and binaries are not. Each fact cites the paths it counted. "Modules" in
  the score card's Size row waits for the dependency graph (TODO.md 1.3), which knows what a module is.
- **Why:** Non-blank lines is the count every tool agrees on without parsing; stripping comments
  needs a parser per language, which is 1.3's work, and comments are part of working software. A
  folder count stands in for modules until something reads imports. The set-aside count is kept so an
  employer sees how much of the tree the applicant did not write.
- **Held by:** `ingest/evaluator/ingest/Size.java`; `test/evaluator/ingest/IngestTest.java`.
- **Source:** Claude, building TODO.md 1.2, 2026-10-19; approved by Ghost, 2026-10-19 (J-009).
- **Status:** in force
- **subject:** ingestion
- **See also:** E-011, E-025

### E-027: ANTLR, not tree-sitter, parses TypeScript and Python
- **Decision:** The TypeScript and Python adapters of `analyze/` (TODO.md 1.3) parse with ANTLR and
  the community grammars-v4 Python and TypeScript grammars, vendored under `third_party/` with their
  licenses (BSD). tree-sitter, which the plan first named, is not used.
- **Why:** tree-sitter's Java bindings carry a native library per platform, the first platform-specific
  files the offline, vendored build (E-021) would hold, and the official bindings are believed to need
  a newer Java than 21. ANTLR is pure Java: plain jars, like JUnit. Both are free; the project runs
  lean and spends nothing on a service it does not need. The cost is a little more work turning
  ANTLR's parse trees into complexity counts.
- **Held by:** TODO.md 1.3 (its check); `third_party/MANIFEST.md` once vendored.
- **Source:** Ghost, 2026-10-19, on Claude's recommendation.
- **Status:** superseded by E-028
- **subject:** environment
- **See also:** E-021

### E-028: tree-sitter parses TypeScript and Python, after all
- **Decision:** The TypeScript and Python adapters of `analyze/` (TODO.md 1.3) parse with tree-sitter
  and its languages' own grammars, vendored under `third_party/` with their licenses (MIT) and the
  native library for each platform the kernel builds on (Linux for the image, Windows for Ghost's
  machine). Before anything is vendored, 1.3 confirms which Java binding works on the kernel's Java
  (the official bindings are believed to need Java 22 or later: a JNI binding on Java 21, or a move to
  Java 25, is Ghost's call) and that its native library loads in the image with the network off. If
  either fails, 1.3 stops and asks Ghost; ANTLR (E-027) is the fallback.
- **Why:** The scorer reads strangers' repositories at scale: code that does not compile, syntax newer
  than any volunteer grammar, more languages later. tree-sitter was built for broken code, its grammars
  are kept by each language's community, every language gives the same tree shape and query style, and
  it is fast. E-027 weighed a simpler build above all of that; the native library is one per platform,
  and production is one Linux image.
- **Held by:** TODO.md 1.3 (its check); `third_party/MANIFEST.md` once vendored.
- **Source:** Ghost, 2026-10-19, on Claude's second look.
- **Status:** in force
- **subject:** environment
- **See also:** E-021
- **Supersedes:** E-027

## How a session runs

### E-029: A session works through the steps in order; it does not stop at one
- **Decision:** A session is not limited to one step of `TODO.md`. Each finished step is closed the
  same way (moved to `docs/DONE.md`, its journal entry, `docs/HANDOFF.md` updated, pushed to `main`),
  and then the next step starts, in order. A step too large for one push is built in slices, each
  tested and pushed on its own. The order of the chain (E-016) is unchanged.
- **Why:** The one-step rule came from work where a long context cannot be cleared. In a coding
  session, compaction clears it well, and the record each step leaves (the handoff, the journal, the
  commits) is what a compacted or fresh session resumes from; stopping after every step only cost
  Ghost a restart.
- **Held by:** `CLAUDE.md` ("Before calling work done"); review.
- **Source:** Ghost, 2026-10-19.
- **Status:** in force
- **subject:** process
- **See also:** E-016, E-018

### E-030: Complexity is counted two ways: McCabe's paths, and SonarSource's cognitive complexity
- **Decision:** Every function gets two Complexity facts. `COMPLEXITY.cyclomatic` is decision points
  plus one (if, loops, a case that is not default, catch, `?:`, `&&`, `||`), counted as
  `scripts/Scan.java` counts paths. `COMPLEXITY.cognitive` is SonarSource's cognitive complexity
  (G. Ann Campbell, 2017): +1 for each break in linear flow, plus the nesting level for those that
  nest, one per run of like logical operators, and one per call of the function to itself. A class
  declared inside a function is not counted in it: its methods are functions of their own. A file
  that does not parse is listed as unread, with why, and gets no fact.
- **Why:** Cyclomatic complexity is the measure every tool reports, so an employer and another tool
  can check it; it counts a flat switch the same as nested ifs, which is how agent-written code games
  it. Cognitive complexity weights nesting, which is what makes code hard to read and change, and its
  paper's worked examples (7 and 1) are the fixture's anchors. A guessed number from a broken parse
  would be a fact nobody could check (E-017).
- **Held by:** `analyze/evaluator/analyze/JavaComplexity.java`; `test/evaluator/analyze/JavaAdapterTest.java`
  (every method of the java-complexity fixture worked by hand) and its golden.
- **Source:** Claude, building TODO.md 1.3, 2026-10-19; for Ghost's review.
- **Status:** in force
- **subject:** analysis
- **See also:** E-013, E-017

### E-031: TypeScript and Python are counted by Java's rules, where a function is drawn as each language draws it
- **Decision:** The TypeScript and Python adapters count as E-030 counts Java. What differs is where a
  function begins. In TypeScript, a function declaration, a method, and a function or arrow function
  held by a variable or a class field (`const f = () => ...`) are functions of their own; an unnamed
  one, a callback, counts in the function around it one level deeper, as a Java lambda does. In
  Python, every `def` is a function of its own, a closure included, as a class inside a Java method
  is; a lambda counts one level deeper. A comprehension's `for` and `if` are decisions for
  cyclomatic complexity; for cognitive, its `if` is +1 flat and its `for` nothing (a map is linear).
  `case _` is a match's default and `??` is not a decision. `.tsx` files read with the TSX grammar.
- **Why:** One set of rules across languages lets an employer compare a Java card with a TypeScript
  one, and the same example written in each gives the same numbers (the fixtures prove it, but for
  Python's missing labelled continue). Arrow functions are everywhere in TypeScript: counting every
  callback as a function would split one function's logic into many small, easy-looking ones, which
  is how agent-written code would hide its complexity.
- **Held by:** `analyze/evaluator/analyze/TypeScriptAdapter.java`, `PythonAdapter.java`,
  `TreeSitterAdapter.java`; `test/evaluator/analyze/TreeSitterAdaptersTest.java` and the fixtures' goldens.
- **Source:** Claude, building TODO.md 1.3, 2026-10-19; for Ghost's review.
- **Status:** in force
- **subject:** analysis
- **See also:** E-028, E-030

### E-032: The dependency graph is the repository's own files, resolved by path; unused imports count against a file
- **Decision:** Each file's imports are resolved to the repository's own source files by its language's
  rule: Java's `a.b.C` to a file ending `a/b/C.java` (a static member or nested class dropping a
  segment), Python's module to `m.py` or `m/__init__.py` (relative from the file's folder, never above
  the root), TypeScript's relative path with its endings and `index`; anything else is outside the
  repository and no edge. `COMPLEXITY.dependency_depth` is the links in the longest chain of imports,
  files that import each other in a cycle counting as one link, citing the chain.
  `MAINTAINABILITY.unused_imports` counts, per file, the names an import binds that the file never
  uses, citing each line; a Python package's `__init__.py` re-exports and is not counted.
- **Why:** Depth is how far a change can travel, the dependency graph's part of Complexity (the score
  card's Size and Complexity rows). Resolving by path needs no build and runs nothing (E-013). Unused
  imports are padding the applicant wrote themselves, which set-aside (E-025) cannot see: Ghost,
  2026-10-19, "someone just spamming imports is not a good candidate".
- **Held by:** `analyze/evaluator/analyze/Dependencies.java`, `JavaImports.java`, `PythonImports.java`,
  `TypeScriptImports.java`; `test/evaluator/analyze/DependenciesTest.java` and the imports fixture's golden.
- **Source:** Claude, building TODO.md 1.3, 2026-10-19, on Ghost's direction for unused imports; for Ghost's review.
- **Status:** in force
- **subject:** analysis
- **See also:** E-025, E-030

## Supply chain

### E-033: Ghost's Supply-Chain Security Baseline, held by a test
- **Decision:** The baseline of 2026-10-19 binds this repository, and is enforced by a test, not by
  habit: `test/SupplyChainTest.java` fails on a workflow action not pinned to a full commit SHA, a
  workflow without `permissions: contents: read`, `pull_request_target`, a self-hosted runner,
  `toJSON(secrets)` or a schedule; on an agent or editor config that runs anything (a hook, a
  folder-open task, an MCP server, auto-approve) and on an installed git hook; on a known worm's file;
  on a vendored jar not in `third_party/MANIFEST.md` or whose SHA-256 differs from its row (Gradle's
  checksum verification, for a build with no repository); on a repository declared in the build; and
  on the binding directive differing between `CLAUDE.md` and this file (E-061 makes this file its one
  copy, CLAUDE.md pointing to it). A submission is stripped of
  `.claude/`, `.vscode/`, `.cursor/`, `.gemini/`, `.mcp.json` and its git hooks before any tool or
  agent touches it (`Quarantine.strip`), and git is never run on it with its own hooks.
- **Why:** Worms now travel through agent configs: the keyv wave (August 2026) committed a SessionStart
  hook and a folder-open task, so opening a checkout ran the payload. This platform opens strangers'
  repositories by design (E-013). A rule held by review is a hope (E-006); each rule here fails the
  fast loop when broken, and each check is proven to catch a broken sample (E-009).
- **Held by:** `test/SupplyChainTest.java`; `ingest/evaluator/ingest/Quarantine.java` and its test;
  `.github/workflows/ci.yml`.
- **Source:** Ghost, 2026-10-19: the Supply-Chain Security Baseline, from Ghost's research with another
  Claude session ("This gets institutionalized. it goes into tests, and is validated against.").
- **Status:** in force (part); superseded by E-061 (part: where the directive lives)
- **subject:** security
- **See also:** E-013, E-014, E-021

### E-034: A test file is known by its path, as test runners find them
- **Decision:** A counted source file is a test when a folder on its path is `test`, `tests`,
  `__tests__`, `spec`, `specs` or `testing`, or when its name is a test runner's (`test_*.py`,
  `*_test.py`, `conftest.py`, `*_test.go`, `*.test.ts`, `*.spec.ts` and their JavaScript kin,
  `*Test.java`, `*Tests.java`, `*IT.java`, `*Spec.java` and their JVM kin). `MAINTAINABILITY.test_to_code_ratio`
  is test lines over the lines of every other counted file, to three places, citing the test files;
  with no code that is not a test there is no fact (SC-07).
- **Why:** These are the defaults of pytest, Jest, Vitest, Go, Maven's Surefire and Failsafe, and
  RSpec, so a repository is read as its own runner reads it, without running anything (E-013). Lines
  come from the inventory, so vendored and generated tests are already set aside (E-025).
- **Held by:** `analyze/evaluator/analyze/TestFiles.java`; `test/evaluator/analyze/TestFilesTest.java`.
- **Source:** Claude, building TODO.md 1.4, 2026-10-19; for Ghost's review.
- **Status:** in force
- **subject:** analysis
- **See also:** E-025, E-026

### E-035: Coupling is fan-in, fan-out and import cycles, read from the dependency graph
- **Decision:** From the graph of E-032: `MAINTAINABILITY.fan_in` and `MAINTAINABILITY.fan_out` per file
  (the repository's files that import it, and that it imports, each citing them), and
  `MAINTAINABILITY.import_cycle_files` for the repository (the files in a strongly connected part of
  more than one file, citing them). Tests are counted like any file: a file that tests import is a
  file that is tested.
- **Why:** Fan-in is how far a change to a file spreads; fan-out is how much a file must know; a cycle
  is files that cannot change one without the others, the coupling that most raises the cost of a
  change. All three come from the graph already built, so no file is parsed twice.
- **Held by:** `analyze/evaluator/analyze/Coupling.java`; `test/evaluator/analyze/CouplingTest.java`.
- **Source:** Claude, building TODO.md 1.4, 2026-10-19; for Ghost's review.
- **Status:** in force
- **subject:** analysis
- **See also:** E-032

### E-036: Duplication is shared 60-token runs inside functions, names and literals normalized
- **Decision:** `MAINTAINABILITY.duplication` is the share of tokens, inside the outermost functions of
  files that are not tests, that lie in a run of 60 tokens found at another place too (another
  function, or the same one at least a run's length away), to three places, citing each duplicated
  stretch by file and lines. Tokens are normalized: comments dropped, every name that is not a
  keyword one token, every string one token, every number one token; keywords and punctuation stay.
  Runs are grouped by fingerprint and checked token for token, and a group is marked whole.
- **Why:** The padding an agent writes most is a function copied and renamed; normalized tokens catch
  it where comparing lines cannot, and kept keywords keep code of a different shape apart. Reading only
  inside functions keeps import blocks and constant tables, which look alike everywhere, out of it.
  Tests copy setup by design and are left out, as Sonar and PMD's CPD leave them out by default. 60
  normalized tokens is eight to ten lines, between Sonar's ten lines and CPD's 100 raw tokens.
  Grouping makes a hostile file of one line repeated cost its length, not its square (E-013).
- **Held by:** `analyze/evaluator/analyze/Duplication.java`, `Tokens.java`;
  `test/evaluator/analyze/DuplicationTest.java` and the duplication fixture's golden.
- **Source:** Claude, building TODO.md 1.4, 2026-10-19; for Ghost's review.
- **Status:** in force
- **subject:** analysis
- **See also:** E-025, E-034

### E-037: A submission's tests run in a Docker container per run, in two phases, with pinned runners
- **Decision:** A submission's own tests run in a throwaway Docker container, on a copy stripped by
  `Quarantine.strip`, with no credentials, a read-only root, dropped capabilities, no new privileges,
  an unprivileged user, and limits on memory, processes, CPU and time. Two phases: first its packages
  are installed from its lockfile only, install scripts off (`npm ci --ignore-scripts`, `pip install
  --require-hashes`), with the network reaching the package registries and nothing else; then its
  tests run with the network off. No lockfile, no install, and the tests are not measured (SC-07).
  The runners and coverage tools, pinned with their hashes and licenses past the 7-day cooldown:
  pytest and coverage.py for Python, the repository's own runner and c8 for TypeScript, JUnit's
  console launcher and JaCoCo for Java. Results are read from the reports they write (JUnit XML,
  Cobertura, JaCoCo XML), as hostile files: no DTD loaded, no external entity, a size cap.
- **Why:** Ghost's three answers, 2026-10-19. Docker is already the project's one environment (E-021);
  a container per run is the isolation every CI service gives untrusted code. Installing needs the
  network and running must not have it, so the two never share a phase; a lockfile is the only
  install that is exactly what the applicant committed, and scripts off is the baseline's rule
  (E-033). The reports are written by the submission's own process, so they are read as an attacker's.
- **Held by:** `analyze/` (the runner and the readers, TODO.md 1.5); the sandbox image once built;
  `test/SupplyChainTest.java` for its pins.
- **Source:** Ghost, 2026-10-19 (the sandbox, the tools, two phases), on Claude's proposals.
- **Status:** in force
- **subject:** security
- **See also:** E-013, E-021, E-033

### E-038: A Maven build is installed with exact versions and strict checksums, Maven having no lockfile
- **Decision:** A Maven repository's tests run when every version its pom names is exact (no range, no
  SNAPSHOT, no LATEST or RELEASE); its install phase runs with strict checksums (`mvn -C`), so a file
  whose checksum does not match Maven Central's fails the install, and its test phase runs offline.
  Otherwise its tests are not measured. This is E-037's lockfile rule as Maven can keep it.
- **Why:** Maven has no lockfile, and most Java repositories are Maven builds; strict E-037 would leave
  their tests unmeasured. Exact versions are what a lockfile pins; Central's checksums are what its
  hashes check. Ghost's answer of 2026-10-19, of three: checksums enforced, pinned versions alone, or
  not measured.
- **Held by:** `analyze/evaluator/analyze/TestPlans.java` (`java`, `exactVersions`);
  `test/evaluator/analyze/TestPlansTest.java`.
- **Source:** Ghost, 2026-10-19, on Claude's proposal (J-019).
- **Status:** in force
- **subject:** security
- **See also:** E-033, E-037

## Where it runs

### E-039: Linux only, and every run in the cloud
- **Decision:** Employee Evaluator is built for, and runs on, Linux only: the kernel, the sandbox and,
  later, the web app behind the website. Development and every check run in the cloud: the agent's
  cloud session for the fast loop, and GitHub's Linux runners, started by hand (E-024), for what needs a
  Docker daemon (the `sandbox` job). No check depends on Ghost's machine, and no Windows or macOS build
  is kept working.
- **Why:** Ghost, 2026-10-21: his machine overheats; "keep this linux only as much as possible, and run
  it as much as possible in the cloud. The goal is to eventually put a wrapper on it and push it to a
  website." The website's servers are Linux, so a second platform buys nothing. The agent's session
  cannot start a Docker daemon (refused as a containment escape, rightly), so Docker's proofs run on a
  GitHub runner, which is Linux and disposable.
- **Held by:** `.github/workflows/ci.yml` (the `sandbox` job); docs/HANDOFF.md.
- **Source:** Ghost, 2026-10-21.
- **Status:** in force
- **subject:** environment
- **See also:** E-021, E-024, E-037

### E-040: The agent starts GitHub runs itself, within the month's minutes
- **Decision:** The agent may start the workflow's runs itself (the `sandbox` job and the fast loop), no
  longer only Ghost; runs stay by hand, never on a push or a schedule. When the organization's Actions
  minutes are spent, no run is started until they renew, and what waits on a run says so in the handoff.
- **Why:** Ghost, 2026-10-21: "You trigger it yourself. We are out of minutes on the github cl runner until
  next month as well." Linux everywhere means the work can run in the agent's own session; GitHub's runner
  is kept for what the session cannot do (a Docker daemon, E-039).
- **Held by:** `.github/workflows/ci.yml` (`workflow_dispatch` only); docs/HANDOFF.md.
- **Source:** Ghost, 2026-10-21.
- **Status:** in force
- **subject:** environment
- **Supersedes:** E-024
- **See also:** E-039

### E-041: The sandbox image builds with the network off, from pieces fetched and checked first
- **Decision:** `sandbox/prefetch.sh` fetches every pinned piece of the sandbox image (Node, Maven, the
  Python wheels, c8's packages) into `sandbox/cache/`, which git ignores, each checked by its hash; the
  image then builds with `--network none`, checking each piece's hash again before it is used. Base
  images are pinned by digest, so they may be pulled from any mirror of Docker Hub (`mirror.gcr.io` when
  Docker Hub refuses with 429): a digest names the same bytes wherever it is fetched.
- **Why:** The first real build (2026-10-21, J-026) could not reach the internet from inside a build
  container through the session's proxy, and Docker Hub rate-limited the base images. Building offline
  works wherever the fetch works, and a compromised build step has no network to call. The hashes are
  checked twice, so the cache is no more trusted than the internet was.
- **Held by:** `sandbox/prefetch.sh`, `sandbox/fetch.py`, `sandbox/Dockerfile`; `test/SupplyChainTest.java`
  (every fetch hashed, in the Dockerfile and the sandbox's scripts).
- **Source:** Claude, building TODO.md 1.5, 2026-10-21; for Ghost's review.
- **Status:** in force
- **subject:** environment
- **See also:** E-021, E-033, E-037

## Provenance

### E-042: The history's shape is six parts of how it was written, weighted, 0–100
- **Decision:** `PROVENANCE.history.shape` is 100 × the weighted sum of six parts, each 0–1: spread (active
  days over 14, weight 0.25), cadence (15 commits a day or fewer, 0.20), variation (commit sizes'
  standard deviation over their mean, 0.20), the messy middle (reverts, fixes, fixups, typos, as a
  share of a twentieth of the commits, 0.15), co-location (of commits touching tests, the share touching
  code too, 0.10), and an agent's fingerprints (Co-Authored-By trailers naming an agent on a tenth of
  commits, or an agent file such as CLAUDE.md kept in two commits or more, 0.10). Each measure is a fact
  beside it. Commit times are the author's claim: anyone can set them, so the shape is evidence, and
  layer 2 (TODO.md 1.7) checks push times against them. No author's name or address is read here.
- **Why:** docs/DESIGN.md's layer 3: a stolen repository replayed as 300 commits in an evening has
  history but not shape. The weights put time first (spread and cadence, 0.45), since time is what a
  replay cannot spend; the fixture that fakes everything else (a test beside each commit, a trailer on
  each) still scores 23. Fingerprints count because the platform ranks agent-assisted work (DESIGN:
  "double duty"), lightly, since a person without an agent built it too.
- **Held by:** `history/evaluator/history/Shape.java`; `test/evaluator/history/ShapeTest.java` (a history
  worked by hand to 70, a replay under 30, weeks of work over 70).
- **Source:** Claude, building TODO.md 1.6, 2026-10-21; weights for Ghost's review (DESIGN: "a tuning
  question").
- **Status:** in force
- **subject:** provenance
- **See also:** E-012, E-033

### E-043: Authorship is read from GitHub's API with a token of its own, read only, never logged
- **Decision:** `github/` reads, for the submitting account, each commit's class: signed (GitHub verified
  its signature and the applicant is its author), pushed (unsigned, but the applicant's push in GitHub's log
  brought it, by the push's own commit list or by GitHub's compare of its before and head), claimed (the
  applicant's by its author field alone), foreign (another account's) or unmatched (no account's). The
  facts count each class, citing commits; give the applicant's share; and say how far back the push log
  reaches (GitHub keeps 90 days, at most 300 events). The token is `EVALUATOR_GITHUB_TOKEN`, fine-grained
  and read only, set in the environment, never in a message, an error, a fact or the repository. Only GET,
  only api.github.com, no redirect followed, answers capped; names that could steer a URL are refused.
  `github/` is the kernel's one door to the network (MOD-05, now held by `ModulesTest`), the sandbox's
  registries proxy the one named exception.
- **Why:** docs/DESIGN.md's layer 2: a commit's author is free text, what GitHub verifies is the signature
  and the push. The agent's session has GitHub credentials of its own, but they are not the product's and
  are not read only, so the adapter is built and tested on GitHub's answers recorded from real Suite
  Builder commits, and its live check waits on a token of its own (Ghost, 2026-10-21: "check against suite
  builder. Read only").
- **Held by:** `github/evaluator/github/Authorship.java`, `HttpGitHub.java`; `test/evaluator/github/`
  (`AuthorshipTest` on the recorded answers, `AuthorshipLiveTest` when the token is set); `test/ModulesTest.java` (MOD-05).
- **Source:** Claude, building TODO.md 1.7, 2026-10-21; for Ghost's review.
- **Status:** in force
- **subject:** provenance
- **See also:** E-012, E-014, E-042

## Navigability

### E-044: The navigability checklist: agent docs, their paths, module cycles, naming, weighted to 0–100
- **Decision:** `NAVIGABILITY.checklist` is 100 × the weighted sum of four parts: agent docs present
  (CLAUDE.md, AGENTS.md, GEMINI.md, .cursorrules, .github/copilot-instructions.md; 0.25); their freshness
  (of the paths their code spans and links name, the share that exist in the repository, a path leaving it
  counted missing; 0.35); module boundaries (1 less the share of modules caught in an import cycle between
  modules, a module being a top-level folder with containers such as `src/` looked through; 0.20); and
  naming (per language, the share of function and file names in the style most of them use, a one-word
  lowercase name fitting any; 0.20). Each part is a fact beside it. The docs are data, read, never followed.
- **Why:** docs/DESIGN.md's phase-1 proxy for AI navigability, until the navigability task (TODO.md 2.5)
  measures it with an agent. Freshness weighs most: a map that sends an agent to files that are gone costs
  more than having no map, and it is what Suite Builder's and this repository's own doc tests hold. This
  repository scores 99 (its two missing paths are the planned `score/` and `agent-runner/`).
- **Held by:** `analyze/evaluator/analyze/Navigability.java`, `DocPaths.java`, `Naming.java`;
  `test/evaluator/analyze/NavigabilityTest.java` (a repository worked by hand to 70, this one above 80).
- **Source:** Claude, building TODO.md 1.8, 2026-10-21; weights for Ghost's review.
- **Status:** in force
- **subject:** navigability
- **See also:** E-004, E-006, E-032

## Scoring

### E-045: Facts into a card: a formula per metric, none until decided; the ratios as DESIGN.md words them
- **Decision:** `score/` builds the card from facts: refuted facts set aside (a fact marked refuted with no
  person named is refused), two facts with one key refused, each metric's facts through its formula. A
  metric with no formula is "not measured" (SC-07); each metric's formula is its own decision, made with
  Ghost before it scores. The first: AI navigability is its checklist (E-044), already 0–100. The ratios:
  Quality ÷ (source lines ÷ 1,000), and Maintainability ÷ Complexity, both scores, to two decimals; absent
  when a part is absent or zero. The tooling bucket reads "unknown" until its rule is decided. JSON's
  fields follow docs/SCORE-CARD.md's order and the metrics the enum's, so one card is always one text.
- **Why:** TODO.md 1.9 sliced (Ghost, 2026-10-21): the card's machinery first, its formulas one at a time
  after. Writing the ratios down exposed a question for Ghost: both reward the small and the simple (a
  200-line toy at Quality 90 scores 450 per thousand lines; a hard problem's high Complexity lowers its
  Maintainability ratio). They are built as worded, and rank nothing until a corpus does.
- **Held by:** `score/evaluator/score/Cards.java`, `Formulas.java`, `CardJson.java`, `CardMarkdown.java`;
  `test/evaluator/score/CardsTest.java`.
- **Source:** Claude, building TODO.md 1.9's first slice, 2026-10-21; the ratios' question for Ghost.
- **Status:** in force (the card from facts, a formula per metric); superseded by E-047 (the ratios)
- **subject:** scoring
- **See also:** E-011, E-017, E-044

### E-046: Provenance is 0.6 × the history's shape + 0.4 × what GitHub backs; the shape alone is capped at 79
- **Decision:** Provenance = round(0.6 × `PROVENANCE.history.shape` + 0.4 × 100 × the share of commits
  GitHub backs as the applicant's: signed, or pushed by them (E-043), over all five kinds counted). With
  no authorship read (no token, or no commit read), the shape alone, capped at 79: Partial at best, never
  Verified. No shape, no score: Provenance is not measured.
- **Why:** The history's shape can be written by anyone with `git commit --date`; GitHub's record of who
  signed and who pushed cannot. So the shape alone may reach Partial, and Verified (80) needs GitHub's word.
  Ghost chose it over the alternatives, 2026-10-22. On history alone this repository reads 67 (two days
  old), Suite Builder 73: both Partial.
- **Held by:** `score/evaluator/score/Formulas.java` (`provenance`); `test/evaluator/score/CardsTest.java`.
- **Source:** Claude's proposal, Ghost's choice ("lets go with the provenance algo"), 2026-10-22.
- **Status:** in force
- **subject:** scoring
- **See also:** E-012, E-042, E-043, E-045

### E-047: The ratios: Quality per thousand lines within a size band; Maintainability at Complexity
- **Decision:** Quality per thousand lines stays Quality ÷ (source lines ÷ 1,000), and a card is ranked on
  it only among cards in its size band: under 2k lines, 2k to 10k, 10k to 50k, 50k and over (SC-10); the
  card carries its band. Maintainability per unit of Complexity becomes Maintainability at Complexity:
  Maintainability × Complexity ÷ 100, to two decimals.
- **Why:** A score per line ranks the smallest first, since Quality stops at 100 and lines do not: a
  200-line toy at Quality 90 reads 450 to a clean 6,000-line project's 13, and a floor on lines only
  narrows it. DESIGN.md's own example compares solutions "to the same problem"; a size band is the nearest
  the kernel can say that, until the Ambition judge (TODO.md 2.3) can say "the same kind of project". A
  quotient over Complexity rewards easy problems; the product is high only when the code is hard and
  maintainable both, and padding Complexity tends to lower Maintainability, so it does not pay.
- **Held by:** `score/evaluator/score/Cards.java` (`ratios`, `band`); `rubric/evaluator/rubric/Ratios.java`;
  `docs/SCORE-CARD.md` (SC-04, SC-10); `test/evaluator/score/CardsTest.java`.
- **Source:** Claude's proposal, Ghost's choice ("go with both recommendations"), 2026-10-22.
- **Status:** in force
- **subject:** scoring
- **Supersedes:** E-011 (the ratios), E-045 (the ratios)
- **See also:** E-011, E-045

### E-048: Size is source lines on a log scale: 100 lines read 0, 100,000 read 100
- **Decision:** Size = round(100 × (log10(source lines) − 2) ÷ 3), held to 0–100: 100 lines and under
  read 0, 1,000 read 33, 10,000 read 67, 100,000 and over read 100. The lines are `SIZE.source_lines`,
  vendored and generated code already set aside (E-026). No line count, no score.
- **Why:** Each tenfold step in working software is a like step in effort, so the scale is even per
  tenfold, not per line. A padded repository gains a few points at most, and the ratios (E-047) are what
  it ranks on. This repository reads 64 (8,160 lines), Suite Builder 96 (74,802).
- **Held by:** `score/evaluator/score/Formulas.java` (`size`); `test/evaluator/score/CardsTest.java`.
- **Source:** Claude's proposal, Ghost's choice ("go with the size log scale"), 2026-10-22.
- **Status:** in force (Size's facts: source lines, vendored and generated code set aside); superseded by E-070 (Size's formula)
- **subject:** scoring
- **See also:** E-026, E-045, E-047

### E-049: A Java class used from its own package is a dependency, though no import line names it
- **Decision:** Each Java file carries one more import, written by no line: its own package, `a.b.*`, with
  the type-like names its code uses (a name starting with a capital), at its package line. It resolves to
  the named classes beside the file (`dir/Name.java`), so the default package works alike. An
  `import a.b.*` as written carries no names and resolves as before (E-032).
- **Why:** Java needs no import for a class of its own package, so E-032's graph missed every such edge
  and Java's dependency depth read low beside TypeScript's and Python's, where every edge is a line: this
  repository read 2 and now reads 8 (`Coupling` → `Dependencies` → `Complexity` → … → `Import`). Ghost: "fix
  java depth first", before Complexity's formula leans on depth.
- **Held by:** `analyze/evaluator/analyze/JavaImports.java` (`ownPackage`, `siblings`), `JavaAdapter.java`
  (`resolve`); `test/evaluator/analyze/DependenciesTest.java`, the imports fixture's `shop/Checkout.java` and golden.
- **Source:** Claude, on Ghost's direction, 2026-10-22.
- **Status:** in force
- **subject:** analysis
- **See also:** E-032, E-034

### E-050: Complexity is the total cognitive complexity on a log scale and the dependency depth
- **Decision:** Complexity (the tools' half) = round(0.6 × the total cognitive complexity of every function
  on Size's log scale, 100 and under reading 0 and 100,000 and over 100, + 0.4 × 100 × the dependency
  depth over 25 links, held at 25). Every function's cognitive fact is listed behind the score; the card's
  Markdown names three and counts the rest. No depth read, no score. Per-function averages are left out.
- **Why:** Measured on two repositories, the mean per function barely moved (2.7 and 3.1 cyclomatic): it
  says more of style than of the problem, and a high one belongs to Maintainability. The total is the
  decision logic the repository holds; depth is how far a change can travel (E-032), once Java's own
  package counts (E-049). This repository reads 37, Suite Builder 81. The agent's assessment
  (`COMPLEXITY.agent_assessment`, phase 2) is the other half, weighed when it exists.
- **Held by:** `score/evaluator/score/Formulas.java` (`complexity`, `logScale`); `CardMarkdown.java` (`facts`);
  `test/evaluator/score/CardsTest.java`.
- **Source:** Claude's proposal, Ghost's choice ("go with the proposal, fix java depth first"), 2026-10-22.
- **Status:** in force (Complexity's facts: total cognitive complexity and dependency depth); superseded by E-074 (Complexity's formula)
- **subject:** scoring
- **See also:** E-030, E-031, E-032, E-048, E-049

### E-051: Maintainability is six parts weighed: duplication, tests, cycles, complex functions, coupling, unused imports
- **Decision:** Maintainability (the tools' half) = 100 × the weighted sum of six parts, each 0–1 with 1
  good: duplication (0.25; 1 less the duplicated share over 10%, held at 0), tests (0.25; the test-to-code
  ratio, held at 1), import cycles (0.15; 1 less the share of files in one), functions (0.15; the share at
  most cognitive 15, SonarSource's usual limit), coupling (0.10; the share of files importing at most 10 of
  the repository's), unused imports (0.10; 1 less their count per file, held at 0). A part with no facts is
  left out and the others' weights scaled to fill it; no part, no score. A formula is now given every fact
  on the card, not only its own metric's, since the functions part reads Complexity's facts.
- **Why:** Each part is something an agent changing the code trips on; duplication and missing tests the
  most, so they weigh most. Refactor history (DESIGN.md) is not measured yet and is left out, not guessed.
  This repository reads 84, Suite Builder 88 (its 81 files in cycles cost it, its tests make it up).
  Two repositories cannot calibrate it: the seed corpus is its first test.
- **Held by:** `score/evaluator/score/Maintainability.java`; `Cards.java` (every fact to every formula);
  `test/evaluator/score/MaintainabilityTest.java`.
- **Source:** Claude's proposal, Ghost's choice ("go with those weights"), 2026-10-22.
- **Status:** in force (its six parts and weights); superseded by E-075 (the scale)
- **subject:** scoring
- **See also:** E-034, E-035, E-036, E-045, E-050

### E-052: Quality is the pass rate and coverage of the submission's own tests; no test at all reads 0
- **Decision:** Quality (the tools' half) = round(100 × (0.6 × `QUALITY.tests.pass_rate` + 0.4 ×
  `QUALITY.tests.coverage`)), from the sandbox's run (E-037); one alone when only it was read. A repository
  with code and no test file (`MAINTAINABILITY.test_to_code_ratio` 0) reads 0, resting on that fact. Tests
  there but not run (no lockfile; a run that reached for the network, which gets no pass rate) are not
  measured (SC-07).
- **Why:** Without the rule a repository with no tests was "not measured" while one with failing tests read
  low, so having none paid. No test file is a measurement, looked for and found; a run that could not
  happen is an absence. Passing weighs more than covering: a covered line proves it ran, not that it was
  checked. Lint (`QUALITY.lint`) is not built and the agent's review (`QUALITY.agent_review`) is phase 2;
  each is weighed when it exists.
- **Held by:** `score/evaluator/score/Formulas.java` (`quality`); `test/evaluator/score/CardsTest.java`.
- **Source:** Claude's proposal, Ghost's choice ("go with the formula and the no-tests rule"), 2026-10-22.
- **Status:** in force
- **subject:** scoring
- **See also:** E-034, E-037, E-045, E-047

### E-053: The tooling bucket is the agent tool with more than half the evidence; else mixed
- **Decision:** `history/` names each agent tool a history shows, one fact each
  (`PROVENANCE.history.agent_tool.claude` = its evidence counted): each commit whose Co-Authored-By trailer
  names it, once per commit, and each instruction file only it reads (CLAUDE.md Claude, GEMINI.md Gemini,
  `.cursorrules` and `.cursor/rules/` Cursor, `copilot-instructions.md` Copilot; AGENTS.md, read by several,
  names none). Anthropic counts as Claude; OpenAI and ChatGPT as Codex. The bucket is the tool with more
  than half the evidence; "mixed" when none has; "none detected" when the history was read and named none;
  "unknown" when no history was read.
- **Why:** The bucket is what a card is ranked within (TODO.md 2.6, percentiles at 200 cards a bucket), so it
  must be read from the repository itself and not be split finer than the evidence can carry. `score/` reads
  no repository (MOD-03), so the names come as facts from `history/`. This repository and Suite Builder
  both read Claude.
- **Held by:** `history/evaluator/history/AgentTools.java`; `score/evaluator/score/Buckets.java`;
  `test/evaluator/history/AgentToolsTest.java`, `ShapeTest`, `test/evaluator/score/CardsTest.java`.
- **Source:** Claude's proposal, Ghost's choice ("go with the bucket rule"), 2026-10-22.
- **Status:** in force
- **subject:** scoring
- **See also:** E-042, E-045

### E-054: `evaluator score <path>`: every module's facts, one card, as JSON or Markdown
- **Decision:** `evaluator score <path> [--markdown] [--no-run] [--github <owner/repo> --applicant <login>]
  [--date <yyyy-mm-dd>]` reads, in this order, what the repository holds (`ingest/`), its code (`analyze/`),
  its own tests in the Docker sandbox unless `--no-run`, its history when it has a `.git` (`history/`), and
  what GitHub verifies when a repository and an applicant are named (`github/`, the token from
  `EVALUATOR_GITHUB_TOKEN`); then prints the card, JSON by default. What kept a part unread goes to stderr.
  `owner/repo` must be GitHub's own form (an owner of letters, digits and hyphens; a repository not starting
  with a dot), since it is built into an API path. The card's rubric hash is the SHA-256 of the formulas'
  named version (`Formulas.RUBRIC`), raised when a formula changes. Every fixture repository's card, JSON and
  Markdown, is a golden (SC-09), the test-run fixtures run in the recorded sandbox.
- **Why:** TODO.md 1.9's third slice. The command names the work and does none (E-010); `--date` and the
  recorded sandbox make a card the same bytes on every machine, which SC-09 needs; `--no-run` scores without
  Docker. Scoring this repository end to end showed two gaps, for Ghost (J-039).
- **Held by:** `cli/evaluator/cli/Main.java`, `Pipeline.java`; `score/evaluator/score/Formulas.java` (`rubricHash`);
  `test/evaluator/cli/MainTest.java`, `CardGoldensTest.java` and `test/goldens/cards/`.
- **Source:** Claude, building TODO.md 1.9's third slice on Ghost's go ("yes, start on the command and goldens"), 2026-10-22.
- **Status:** in force
- **subject:** architecture
- **See also:** E-010, E-018, E-037, E-043, E-045

## Judges

### E-055: The judge is Haiku three times, averaged, given the kernel's facts; Opus when the three disagree
- **Decision:** A judgment (phase 2: Ambition, Quality's review, Complexity's assessment, which test files are
  fixture data) is made by Claude Haiku 5.5 three times on the same brief, and the score is the mean of the
  three; a category (a tier, a folder's kind) is their vote. Each run is given the kernel's own facts (sizes,
  counts, the history's measures) to judge, never to count, and answers in a schema the API enforces
  (structured output), never free JSON. Where the three runs are more than 15 points apart on a metric, Claude
  Opus 5.5 judges that metric and its answer stands. Each fact carries the runs and the model behind it.
- **Why:** Judge trials 1 and 2 (`docs/judges/`, J-040, J-041): on three repositories, Haiku's mean of three
  came within 2.1–4.0 points of the mean of three Opus runs (Opus agrees with itself within about 2) for
  about a fourteenth of one Opus run; every model ranked the repositories as Opus did and told fixture data
  from tests. Haiku's worst error, Provenance, came from miscounting history the kernel counts exactly. Free
  JSON failed on 7 of 9 Opus answers. Opus's deeper read finds what averaging cannot, so it takes the cases
  the cheap runs disagree on. To be confirmed on the calibration set (TODO.md 2.1) before it scores live.
- **Held by:** TODO.md 2.2 (the harness, its check); `agent-runner/` once built.
- **Source:** Claude's recommendation, Ghost's choice ("go with the haiku x3 recommendation"), 2026-10-22.
- **Status:** in force
- **subject:** judges
- **See also:** E-003, E-017

## Admission

### E-056: Only AI-assisted work is ranked: evidence admits, its absence asks, hand-written or generated wholesale is declined
- **Decision:** The platform ranks AI-augmented developers on how well they structure AI-built code; a
  repository built without AI assistance is outside what it measures. Each submission takes one of three
  outcomes. (1) Evidence of AI assistance (agent co-author trailers, agent instruction files kept up over the
  history, the judges' reading, E-055): admitted and scored. (2) No evidence: not declined; the applicant is
  asked to declare their tooling and show something (an agent instruction file, session links, an account of
  the workflow), and a judge decides. (3) Clearly hand-written (no evidence, and the history and the judges
  agree), or generated wholesale by a tool with little of the applicant's own work (Fable's "generated",
  trial 3): declined, with a respectful message that this platform assesses AI-augmented development and a
  traditional code review would serve the applicant better. No card is made for a declined submission.
- **Why:** Ghost, 2026-10-22: purely human code "should be rejected on the grounds that the candidate is not
  an ai augmented developer, and thus traditional methods of inspecting the candidates coding ability are
  better suited", and the candidate "is being ranked on ai code structuring proficiency". Three outcomes, not
  two, because the absence of evidence is not evidence of absence: trailers are stripped in a minute and
  inline completion (Cursor, Copilot) leaves no trace, so a two-way gate would decline the very developers the
  platform is for. A tool's wholesale output shows the tool's structuring, not the applicant's. This is a gate
  on how the work was built; E-012 stays in force for who built it (provenance is still a score).
- **Held by:** docs/SCORE-CARD.md (SC-11), held from TODO.md 3.3 (the submission flow); the evidence today is
  `PROVENANCE.history.agent_tool.*` (E-053) and the bucket's "none detected"; the judgment is the judges' (E-055).
- **Source:** Ghost, 2026-10-22, on Claude's proposal of the three outcomes ("record it with the three outcomes").
- **Status:** in force
- **subject:** admission
- **See also:** E-012, E-023, E-053, E-055

### E-057: The judge classifies and judges; the kernel counts what the judge calls the author's own, and applies the card's rules
- **Decision:** Work is divided by what each does reliably. The judge (Haiku three times, E-055) says what kind
  of repository it is (built, plan only, generated or mixed), classifies its folders (own code, own tests, own
  tooling, docs, generated, vendored, reference, fixture data), and scores only what needs judgment: Ambition,
  Quality's review, Complexity's assessment of the problem. The kernel then measures only the files whose most
  specific classified folder is own code: Size, Complexity's total and depth, Maintainability's parts. The
  card's rules are the kernel's: a generated repository is declined (E-056); a plan carries no Size,
  Complexity or Maintainability and its Ambition reads unverified. The judge's instructions anchor the scale
  with archetypes and judge navigability by its mechanisms (round 2's v22 and v08), adding scored corpus cards
  as anchors once the corpus holds them (v17). Fable's scoring stays the reference judge prompts are measured
  against, beside the labelled calibration set (TODO.md 2.1).
- **Why:** Judge trial 4 (J-047, J-048, docs/judges/): Haiku named the kind of every repository in all but two
  of 276 runs, and its folders, counted by the kernel, put own code within 17% of Fable's on every repository
  and run after run alike, where the kernel alone counted 1.7–2.8 times the author's code on built repositories
  and 24.7k lines for a generated one with 3 of its owner's. Haiku told the generated suite apart yet still
  scored its code (navigability +55), and given the seventh Fable's insights as instructions over-discounted
  a plan: so the judge is not asked what a rule settles. The wording of the instructions moved the mean of
  three by about a point; what the judge is asked moved it far more.
- **Held by:** TODO.md 2.2 (the harness) and the kernel's reading of the judge's folders once built;
  docs/judges/trial-4-haiku-round2.md for the measurements.
- **Source:** Claude's proposal from trials 3 and 4, Ghost's choice ("record E-057 and add the javascript
  adapter"), 2026-10-22.
- **Status:** superseded by E-082 (its list of kinds only, the rest stands)
- **subject:** judges
- **See also:** E-017, E-025, E-055, E-056

### E-058: JavaScript is read as TypeScript is, with the vendored TSX grammar, and `require` counts as an import
- **Decision:** `.js`, `.mjs`, `.cjs` and `.jsx` files are measured by `JavaScriptAdapter`, which is the
  TypeScript adapter (E-030, E-031) reading every JavaScript file with the vendored TSX grammar: functions,
  cyclomatic and cognitive complexity as TypeScript's. Its imports are TypeScript's `import` statements and
  each top-level `require("m")`, binding the declared name or the object pattern's names (a bare require binds
  nothing); a require's names are used when they appear outside its own declaration. A relative import
  resolves with JavaScript's endings (`.js`, `.mjs`, `.cjs`, `.jsx`) and folder index files.
- **Why:** Judge trial 4 found the Jensen migration's 1,352 lines of JavaScript registering 17 functions and no
  dependency (J-048); now 121 functions and depth 3. TSX rather than the TypeScript grammar, because plain
  `.js` often holds JSX, and TSX's one difference (no `<T>x` casts) is TypeScript-only syntax JavaScript never
  has. No new grammar, so no new dependency (E-033); a JavaScript grammar would come with its own approval.
- **Held by:** `analyze/evaluator/analyze/JavaScriptAdapter.java`, `JavaScriptImports.java`;
  `test/evaluator/analyze/TreeSitterAdaptersTest.java` and `test/fixtures/javascript-complexity/` with its golden.
- **Source:** Claude, building TODO.md 1.10 on Ghost's go ("close 1.9 and start the javascript adapter"), 2026-10-22.
- **Status:** in force
- **subject:** analysis
- **See also:** E-030, E-031, E-032, E-057

### E-059: PIT is the mutation tool, vendored with what its poms declare
- **Decision:** PIT 1.30.1 with its JUnit 5 plugin 1.2.3 is vendored under `third_party/pitest/`, with the runtime
  dependencies its poms declare (ASM 9.11, commons-text 1.14.0, commons-lang3 3.18.0): eleven jars, each in
  `third_party/MANIFEST.md` with its SHA-256 and license. It is on no classpath of the build or the tests; only
  `gradle mutate` reads it. A run's numbers and its survivors are written in `docs/MUTATION.md`.
- **Why:** Dependency reason (E-033): PIT measures whether the tests catch a changed line, which coverage cannot,
  and it is the standard Java mutation tool. Apache-2.0; ASM BSD-3-Clause; the libraries the command-line jar
  bundles (StringTemplate, ANTLR 2, jopt-simple) BSD and MIT. ASM's extra modules and the commons jars were
  tried without: the commons jars are needed; the ASM modules are declared, so they stay rather than fail on
  a path the trial did not reach.
- **Held by:** `third_party/MANIFEST.md` and `test/SupplyChainTest.java`; `build.gradle` (`mutate`); `docs/MUTATION.md`.
- **Source:** Ghost, 2026-10-22 ("yes, vendor PIT and run slice 4"), on Claude's proposal at TODO.md 1.11.
- **Status:** in force
- **subject:** tests
- **See also:** E-019, E-021, E-033

### E-060: A Sonnet agent tries a hard fix once, then asks Opus
- **Decision:** A Claude Sonnet agent working here fixes what it spots plainly without asking anyone. When
  a fix fails and the failure does not plainly say why, it tries once more at most, then spawns a Claude Opus
  agent with the task, the failing output, its diff and the files it read, and applies the answer. If that
  fails too, it stops and tells Ghost. The step's journal entry notes the escalation.
- **Why:** So a Sonnet agent can work on the codebase: cheaper for the plain work, which is most of it,
  without guessing in circles on the hard part. Opus is spent only where a failure resists. The journal's
  escalations show where the repository is hard to work in, which the navigability passes then fix.
- **Held by:** `CLAUDE.md` ("A Sonnet agent escalates once"); review of the journal.
- **Source:** Ghost, 2026-10-23.
- **Status:** in force
- **subject:** agents
- **See also:** E-015, E-055

### E-061: The supply-chain directive has one copy, theGhost.md's first section, read before any task
- **Decision:** Ghost's Supply-Chain Security Baseline lives once, as the first section of theGhost.md.
  CLAUDE.md no longer holds a copy; it sends every agent there before any task ("Before any task: the
  supply-chain rules"). The rest of theGhost.md is read by a decision's number, not up front.
- **Why:** Two copies held equal by a test are two places to edit and a test that only says they match.
  One copy, read first, keeps the rules binding and in front of every agent. Mandatory context counts
  both CLAUDE.md and that section (`gradle metrics`), so AGT-03 still sees the whole cost.
- **Held by:** `test/SupplyChainTest.java` (`directive`: the section first in theGhost.md, no copy in
  CLAUDE.md, CLAUDE.md's pointer present); `scripts/Metrics.java` (`mandatory`).
- **Source:** Ghost, 2026-10-23 ("move it to the ghost provided agents read the ghost before doing tasks"),
  from the Fable advisor's navigability pass.
- **Status:** superseded by E-073
- **subject:** security
- **Supersedes:** E-033
- **See also:** E-015

### E-062: `gradle kernelMap` closes a function that is gone
- **Decision:** `gradle kernelMap` appends a closing row for every function the map holds open that the
  source no longer has: `superseded-by: <key>` when its name appears exactly once among the functions new
  in the same run (it moved), else `removed: <date>`. Rows are still never changed. A rename reads as
  removed and new.
- **Why:** The hand-written closing row was the likeliest place for an agent to stall on a refactor:
  `KernelMapTest` failed after `gradle kernelMap` had run (the Fable advisor's pass, 2026-10-23). The
  tool knows what is gone and what is new; only the link between a rename's two names needs a person, and
  the two rows record it either way.
- **Held by:** `scripts/KernelMap.java` (`write`); `test/KernelMapTest.java`.
- **Source:** Ghost, 2026-10-23 ("yes to gradle kernel map").
- **Status:** in force
- **subject:** intent
- **See also:** E-005

### E-063: CLAUDE.md's routing rows are pointers; an action's steps live in its instructions file
- **Decision:** A row of CLAUDE.md's routing table points; it does not instruct. Where an action has steps
  (add a language adapter, add a fact, change a formula), its row names one instructions file in
  `docs/how/`, and the steps, the files they touch and what proves them live there. A new step, a file an
  action now touches, or more detail goes into that action's file, never into the table; a new action gets
  one short row and its own file. Every path an instructions file names is held to the repository.
- **Why:** The table is read by every agent; an action's steps are read only by the agent doing it. Adding
  a language touched ten files, and AGT-04 allows a row five, so the rest was nowhere and an agent guessed
  (the Fable advisor's pass, 2026-10-23). Steps in their own file grow without growing what every agent
  reads.
- **Held by:** `test/DocsTest.java` (every path in `docs/how/` exists, as CLAUDE.md's do); AGT-03 and
  AGT-04 (`test/MetricsTest.java`).
- **Source:** Ghost, 2026-10-23 ("lets start thinking about the instructions as pointers").
- **Status:** superseded by E-065
- **subject:** agents
- **See also:** E-015, E-060

### E-064: Every escalation replays the route, is logged, and three of a kind go to Ghost
- **Decision:** A Ghost-level directive. When a Sonnet agent escalates to Opus (E-060), Opus first asks it
  to replay the instructions it followed from the task's start, and validates that the instructions it needed
  reached it then: in the routing row and instructions file it started from. The Sonnet agent replays if it
  can recall, briefly; if it cannot, that is fine. Either way the escalation is logged in
  `docs/ESCALATIONS.md` with one of five types (`missing-step`, `not-reached`, `wrong-step`, `hard-problem`,
  `no-replay`). Three rows of one type not yet reviewed are raised to Ghost, for review and a possible pass
  updating the routes.
- **Why:** An escalation is the clearest sign a route failed an agent. Replaying the route says whether the
  instructions were missing, not reached, wrong, or fine, so a route is fixed where it fails rather than where
  someone guesses; three of a kind is a pattern, not chance.
- **Held by:** `docs/how/escalate-to-opus.md`; `test/EscalationsTest.java` (the types; a third row of a type
  not raised, or raised and not in `docs/FOR-GHOST.md`, fails); `CLAUDE.md` ("A Sonnet agent escalates once").
- **Source:** Ghost, 2026-10-23 ("This is a ghost level directive").
- **Status:** in force
- **subject:** agents
- **See also:** E-060, E-063

### E-065: Every workpath in CLAUDE.md is a pointer to its instructions
- **Decision:** CLAUDE.md's routing table has two columns, the task and its instructions file. Every general
  workpath has one file in `docs/how/`: the files to open first (its "Start here", at most four, so the
  pointer and they stay within AGT-04's five), the contract, the tests, and the steps. A row that only names
  a document to read points to the document itself. New steps and files go into the workpath's file.
- **Why:** Ghost: "the pointer file should include instructions for each general workpath." E-063 made
  pointers of the rows with steps only; the rest still carried files, contracts and tests inline, read by
  every agent for every task. As pointers, CLAUDE.md fell from 1,114 words to 836.
- **Held by:** `test/DocsTest.java` (every path in `docs/how/` exists); `scripts/Metrics.java` (`routing`
  follows a pointer and counts its "Start here" files) and `test/MetricsTest.java` (AGT-03, AGT-04).
- **Source:** Ghost, 2026-10-23.
- **Status:** in force
- **subject:** agents
- **Supersedes:** E-063
- **See also:** E-064

### E-066: When agents' work conflicts, they stop and tell Ghost; none undoes another's
- **Decision:** A Ghost-level directive. An agent whose work conflicts with another agent's (a commit or
  process that breaks its task, contradicting tasks, its changes changed back) stops the conflicting work,
  assumes conflicting objectives rather than sabotage, logs a `conflict` row in `docs/ESCALATIONS.md`,
  raised at once and named in `docs/FOR-GHOST.md`, and tells Ghost, saying both objectives plainly and asking
  which way to go. It never kills, stops or restarts a process it did not start; never reverts, rewrites,
  deletes or pushes over another agent's commit; never force-pushes; never edits the log, the record or a test
  to get around a conflict.
- **Why:** A reported trial (Ghost, from a LinkedIn post, 2026-10-23): three agents with conflicting
  objectives overwrote each other's work and ran kill scripts against each other, each taking the others for
  saboteurs, and none told the human who could have revised the objectives. The conflict was in the tasks;
  only the person who set them can settle it, and only if told.
- **Held by:** `CLAUDE.md` ("When agents conflict"); `docs/how/when-agents-conflict.md`;
  `test/EscalationsTest.java` (a `conflict` row unraised, or not in `docs/FOR-GHOST.md`, fails);
  `scripts/prepush.sh` (refuses a push not on top of `origin/main`).
- **Source:** Ghost, 2026-10-23 ("Agreed to the escalations handling ruleset").
- **Status:** in force (part); superseded by E-067 (part: force-pushing)
- **subject:** agents
- **See also:** E-020, E-060, E-064

### E-067: A force-push only when every active agent agrees, the slow ones waited for
- **Decision:** A Ghost-level directive, replacing E-066's "never force-push". An agent force-pushes only
  when every agent actively working on the project has agreed: it finds them (Claude Code's `ListAgents`, and
  any session Ghost runs on the repository), checks in with each (what it would force-push, which commits that
  drops or rewrites, and why), and waits for every one; a slow agent is checked in on again and waited for,
  never skipped or taken as agreeing. One that disagrees or cannot be reached means no force-push, and Ghost is
  told. An agent working alone may, and its journal entry says so.
- **Why:** Ghost: "not to force push unless all agents actively working on the project agree. Basically
  check in and wait for others if they are slow." A force-push rewrites what others built on; agreement first
  keeps it from being one agent settling a conflict alone (E-066's lesson).
- **Held by:** `CLAUDE.md` ("When agents conflict"); `docs/how/when-agents-conflict.md` ("A force-push").
- **Source:** Ghost, 2026-10-23.
- **Status:** in force
- **subject:** agents
- **Supersedes:** E-066
- **See also:** E-020

### E-068: Fresh Sonnet agents per task; each debriefs; a navigation review about every ten commits
- **Decision:** A Ghost-level directive. Each new task goes to a fresh Claude Sonnet agent, spawned by the
  orchestrating session. When its task ends, the agent appends a debrief to `docs/NAVIGATION-NOTES.md`: its
  complaints and suggestions about finding its way. About every ten commits an agent reviews the file for
  points agents converge on. None actionable: it says so in the journal and wipes the file. Actionable: it
  still wipes the file, records each item, checks each is valid against the repository, and fixes the valid
  ones; an open question goes to a Claude Fable agent, read only. A review's commit starts "Navigation review";
  `gradle metrics` warns past ten commits without one, and the pre-push refuses past twenty.
- **Why:** A fresh agent meets the repository as a newcomer does, so its trouble is the repository's, not
  its memory's; Sonnet is the agent the routes are built for (E-060). One agent's complaint may be its own;
  several agents' agreement is a route to fix. The tithe slipped twice while habit held it (J-045, J-051), so
  the cadence is held by the same gate.
- **Held by:** `CLAUDE.md` ("Fresh agents, debriefs, navigation reviews"); `docs/how/debrief-after-a-task.md`;
  `docs/how/navigation-review.md`; `scripts/TitheGate.java` and `test/MetricsTest.java` (the cadence).
- **Source:** Ghost, 2026-10-23.
- **Status:** in force
- **subject:** agents
- **See also:** E-015, E-060, E-064, E-065

### E-069: CLAUDE.md is a memory map: what applies only sometimes is a "When X, read Y" pointer
- **Decision:** CLAUDE.md holds inline only what every task needs (the supply-chain pointer, the routing
  table, the three rules). Everything that applies only in a situation is one line in "When it happens, read
  this": the situation, then the file that holds what to do ("If you are a Claude Sonnet model and a fix fails
  ..., the escalation policy, `docs/how/escalate-to-opus.md`"). The agent looks it up when it happens. The
  conflict line keeps its three nevers inline, since an agent may not see the conflict before it acts.
  CLAUDE.md's former sections now live here: "Commands" in `docs/how/commands.md`; "Before calling work done"
  (E-029's holder) in `docs/how/finish-work.md`; the commit rules in `docs/how/commit-and-push.md`; "A Sonnet
  agent escalates once" (E-060, E-064), "When agents conflict" (E-066, E-067) and "Fresh agents, debriefs,
  navigation reviews" (E-068) as lines of the map, each pointing to its file.
- **Why:** Ghost: "By splitting out the memory mapping, we can replace large chunks of data that would have
  been read into context with very cheap one line entries that point to the data the agent needs at the time
  it needs it." CLAUDE.md went from 1,017 words to 647.
- **Held by:** `test/DocsTest.java` (every file the map names exists); `test/MetricsTest.java` (AGT-03, the
  mandatory context); `test/SupplyChainTest.java` (the supply-chain pointer).
- **Source:** Ghost, 2026-10-23.
- **Status:** in force
- **subject:** agents
- **See also:** E-061, E-065

### E-070: Counted aspects score against a baseline repository: Suite Builder at 32762a6 is 100, empty is 0
- **Decision:** A counted aspect (lines of code, logic statements, function size, how well factored, and the
  like) is scored against a baseline: Suite Builder at commit `32762a6110ebd73246ca2413b1d54db947aee179`
  (main, 2026-10-23, 746 commits) reads 100, an empty project reads 0, and the scale between is linear:
  100 × the aspect's measure ÷ Suite Builder's. A repository may read above or below 100. For an aspect where
  more is worse (the largest function), the ratio is inverted. The baseline and its commit are part of the
  rubric, so every card names what it was counted against. The formulas move to it one metric at a time
  (TODO.md 1.13), Size first.
  Built so far, Size: the baseline's measure, read by this kernel without the sandbox (`evaluator score --no-run`, none of
  its own code run) on a clean clone at that commit, is 84,971 source lines (916 files, 119 folders, 122 files set aside,
  E-026; the old log scale read it 98). Size = round(100 × lines ÷ 84,971): 21,243 lines read 25, 100,000 read 118,
  169,942 read 200, an empty project 0.
  Built next, Complexity (E-074): the baseline's total cognitive complexity is 13,361 and its dependency depth 28.
- **Why:** Ghost: "If we declare a given repository to be baseline in a specific aspect, then we create a
  measurable method to count that aspect." The anchors so far (Size's 100 and 100,000 lines, E-048) were
  constants picked by hand; a baseline is one that can be pointed at. Suite Builder over Katana: Katana is the
  purer AI-built project (Fable as manager, Suite Builder's lessons taken in), but Suite Builder is the working,
  mature product, its harness noted for rigour by other models, and a rigorously tested codebase following
  compiler principles is the next best thing to a compiler. Every baseline carries its developer's tendencies;
  that is accepted, not avoided (Ghost).
- **Held by:** Size, built (TODO.md 1.13, first slice): `score/evaluator/score/Formulas.java` (`size`, `BASELINE_COMMIT`,
  `BASELINE_SOURCE_LINES`, `RUBRIC`, whose hash every card carries), `test/evaluator/score/FormulasTest.java` (the worked
  example), the cards' goldens. Complexity, built (second slice, E-074): `Formulas.complexity`, `BASELINE_COGNITIVE`, `BASELINE_DEPTH`, the same worked-example
  test. Maintainability, built (third slice, E-075): `Formulas.BASELINE_MAINTAINABILITY`, `Maintainability.score`, the same kind of test.
- **Source:** Ghost, 2026-10-23 ("ok lets set a commit level for suite builder at its current level ... the
  baseline ... to be 100. A project that is empty scores at 0"; "linear is fine").
- **Status:** in force (Size, Complexity and Maintainability, TODO.md 1.13 built)
- **subject:** scoring
- **See also:** E-045, E-050, E-071
- **Supersedes:** E-048 (Size's formula; its facts stay)

### E-071: A baseline-scored metric may pass 100
- **Decision:** A metric scored against the baseline (E-070) has no ceiling: a repository counted at twice
  Suite Builder's measure reads 200. The floor stays 0. Metrics that are shares or checklists rather than counts
  (Provenance, whose tiers read 50 and 80; Quality, from pass rate and coverage; Navigability's checklist) keep
  0–100. SC-01's "each 0–100" and `Score`'s bound change with TODO.md 1.13, when the first metric moves.
- **Why:** Ghost: "Yes a score can go past 100." A cap at the baseline would make every repository larger
  than Suite Builder look the same, which is what the baseline is there to tell apart.
- **Held by:** `rubric/evaluator/rubric/Metric.java` (`counted`: Size, Complexity and Maintainability), `rubric/evaluator/rubric/ScoreCard.java`
  (a score above 100 refused for any other metric), `rubric/evaluator/rubric/Score.java` (a floor of 0 only), docs/SCORE-CARD.md
  SC-01, `test/evaluator/rubric/ScoreCardTest.java`, `test/ScoreCardDocTest.java`.
- **Source:** Ghost, 2026-10-23.
- **Status:** in force (Size, Complexity and Maintainability are counted, the rest keep 0–100)
- **subject:** scoring
- **See also:** E-070

### E-072: The fast loop runs test classes side by side, the tests of a class one after another
- **Decision:** `gradle test` runs JUnit 5's classes concurrently (`build.gradle`: `junit.jupiter.execution.parallel.*`,
  classes concurrent, tests in a class on one thread). A run with `-PwriteGoldens` (goldens written in place) or `-Pdocker`
  (the real sandbox) stays serial. A new test class keeps to what the others do: its own `@TempDir`, port 0, no system
  property set, no static field changed, no working directory assumed.
- **Why:** AGT-02's 20 s was passed (23 s read on Ghost's machine; 15 to 16 s here, `gradle metrics --args=--time`). Compiling and
  Gradle are fixed costs; the tests were 12 s, one class at a time. Side by side they read 8 to 9 s. The tests were
  checked for shared state (none sets a property or a static, every file written is in a temporary folder, every
  server binds port 0, goldens are one class each) and run many times. Two tests that interrupt their own thread passed a
  git or a fake docker that could finish before the interrupt was looked at, and failed on a busy machine; they now run a
  program that waits (`git hash-object --stdin`, `sleep`), which holds what they claim more strictly (J-088).
  A shorter wait in `aRunPastItsTimeoutIsKilledByName` (2 s to 1 s) gained nothing measurable, so it stays 2 s.
- **Held by:** `build.gradle`; `test/MetricsTest.java` (AGT-02's 20 s); `docs/AGENT.md`.
- **Source:** Ghost's directive to make room in the fast loop, 2026-10-23; Claude Sonnet 5.5.
- **Status:** in force
- **subject:** tests
- **See also:** E-018, E-019

### E-073: The supply-chain rules are back in CLAUDE.md, their one copy, read on entry
- **Decision:** Ghost's Supply-Chain Security Baseline lives once, in `CLAUDE.md`, under "Supply-chain rules
  (binding)", so every agent has read it the moment it starts. `theGhost.md`'s first section is a pointer to it
  and keeps no copy.
- **Why:** Under E-061 the rules sat in theGhost.md behind a "read before any task" pointer, and the first quick
  task done that way (the tithe, J-089) never read them: its commands opened theGhost.md only for E-015's lines.
  A pointer depends on being followed; text read on entry does not. The cost is about 190 words of mandatory
  context, which AGT-03 has room for. Ghost: "put the supply-chain rules back in claude.md".
- **Held by:** `test/SupplyChainTest.java` (`directive`: the rules in CLAUDE.md, no copy in theGhost.md);
  `scripts/Metrics.java` (the mandatory context is CLAUDE.md's words).
- **Source:** Ghost, 2026-10-23.
- **Status:** in force
- **subject:** security
- **Supersedes:** E-061
- **See also:** E-033, E-069

### E-074: Complexity is scored against the baseline: 0.6 on cognitive complexity, 0.4 on dependency depth, Suite Builder at 32762a6 is 100
- **Decision:** Complexity (the tools' half) = round(0.6 × (100 × the total cognitive complexity of every function ÷ 13,361) + 0.4 ×
  (100 × the dependency depth ÷ 28)), no ceiling (E-071); an empty project (depth 0, no function) reads 0, and no depth read is still no
  score. The baseline's measures, read by this kernel without the sandbox (the modules `evaluator score --no-run` calls for the code,
  `Ingest`, `Complexity` and `Dependencies`, on the facts `COMPLEXITY.cognitive` and `COMPLEXITY.dependency_depth`; none of Suite
  Builder's code run) on a clean clone of Suite Builder at `32762a6110ebd73246ca2413b1d54db947aee179`: total cognitive complexity 13,361 over 4,410 functions,
  dependency depth 28 (84,971 source lines again, so the reading is the one Size used; one file was unreadable to its adapter and is
  listed, not guessed at). E-050's weights (0.6, 0.4) and inputs (the total of every function's cognitive complexity, and the
  dependency depth, E-032, E-049) are kept as they were; what changes is the scale: E-050's log scale and its depth cap of 25 go,
  each input is the ratio to the baseline's. Worked: the baseline reads 0.6 × 100 + 0.4 × 100 = 100; twice it (26,722 and 56) 200;
  cognitive 6,681 with depth 7 is 30.002 + 10 = 40; cognitive 13,361 with depth 0 is 60; depth 14 with no function is 20.
- **Why:** Ghost's lead: keep E-050's weights and inputs and put each on the baseline's scale (E-070). The log scale and the cap at 25
  were constants picked by hand; Suite Builder's own depth (28) was past the cap, so the old scale could not tell it from a
  shallower repository, and the old formula read it 81 (this repository 37). Both parts are now counts, linear, so the one rule
  (E-070) holds for Complexity: the score counts how much there is, as Size does.
- **Held by:** `score/evaluator/score/Formulas.java` (`complexity`, `BASELINE_COGNITIVE`, `BASELINE_DEPTH`, `RUBRIC`, whose hash every
  card carries); `rubric/evaluator/rubric/Metric.java` (`counted`); `test/evaluator/score/FormulasTest.java` (the worked example and
  its edges); the cards' goldens.
- **Source:** Ghost's lead, 2026-10-23 (second slice of TODO.md 1.13); Claude Sonnet 5.5 measured and built it.
- **Status:** in force
- **subject:** scoring
- **See also:** E-032, E-049, E-050, E-070, E-071
- **Supersedes:** E-050 (Complexity's formula; its facts stay)

### E-075: Maintainability is scored against the baseline: E-051's weighted score over Suite Builder's at 32762a6 is 100
- **Decision:** Maintainability (the tools' half) = round(100 × E-051's weighted score for the repository ÷ E-051's weighted score for Suite
  Builder at `32762a6110ebd73246ca2413b1d54db947aee179`, which is 0.895670359282887), no ceiling (E-071). E-051's six parts, their weights
  (duplication 0.25, tests 0.25, cycles 0.15, functions 0.15, coupling 0.10, unused imports 0.10) and its rule for a part with no facts (left
  out, the others' weights scaled to fill it, no part no score) are kept as they were. The whole weighted score is scaled, not each part: the
  parts are shares from 0 to 1, and a part where Suite Builder reads 0 would divide by zero. A repository with a part absent has its weighted
  score taken over the parts it has (E-051) and then divided by the baseline's six-part score, so it is compared with the whole baseline and not
  with the baseline's same parts; the parts are not zeroed for the baseline either. The baseline's measures, read by this kernel without the
  sandbox (the code facts `evaluator score --no-run` reads, by a scratch program on a clean clone of Suite Builder at that commit, none of its
  code run): duplication 0.027 (part 0.73), test-to-code ratio 1.412 (1), 87 of 914 files in an import cycle (0.9048), 4,233 of 4,410 functions
  at most cognitive 15 (0.95986), 851 of 914 files importing at most 10 (0.93107), 88 unused imports over 913 files (0.90361): weighted
  0.25 × 0.73 + 0.25 × 1 + 0.15 × 0.9048 + 0.15 × 0.95986 + 0.10 × 0.93107 + 0.10 × 0.90361 = 0.89567. The baseline reads 100 against itself.
  Worked: every part perfect 100 ÷ 0.89567 = 111.65, 112; duplication 0 and tests 1 only (the rest absent) weighted 0.5, 55.82, 56; E-051's
  four-file example, weighted 0.5625, 62.80, 63; the tests part alone at 0.4433 is 49.49, 49, at 0.4434 is 49.50, 50. Maintainability at
  Complexity (E-047, a product with Complexity over 100) moves with it.
- **Why:** Ghost's lead: keep E-051's parts and weights and scale the result against the baseline (E-070). E-051's scale was the weighted score
  itself, 0 to 100, which could not tell a repository better than Suite Builder from one as good (this kernel read Suite Builder 90, and every
  part is held at 1 at most); now a repository better than the baseline reads over 100, as Size and Complexity do. A per-part baseline was
  refused for the divide by zero. Absent parts are left as E-051 has them: a missing part is no reading, not a bad one.
- **Held by:** `score/evaluator/score/Maintainability.java` (`score`, `weighted`); `score/evaluator/score/Formulas.java` (`BASELINE_MAINTAINABILITY`,
  `RUBRIC`, whose hash every card carries); `rubric/evaluator/rubric/Metric.java` (`counted`); `test/evaluator/score/MaintainabilityTest.java`
  (the worked example, the baseline at 100 against itself, the edges); `test/evaluator/score/FormulasTest.java` (the rubric and its hash);
  the cards' goldens.
- **Source:** Ghost's lead, 2026-10-23 (third slice of TODO.md 1.13); Claude Sonnet 5.5 measured and built it.
- **Status:** in force
- **subject:** scoring
- **See also:** E-047, E-051, E-070, E-071, E-074
- **Supersedes:** E-051 (Maintainability's scale; its parts and weights stay)

### E-076: `evaluator score --facts` prints the values behind a card, one `name value` line each
- **Decision:** With `--facts` the command prints, instead of the card, the repository-wide facts and the inputs the counted metrics are scored from:
  one line per value, `name value` with one space, sorted by name, numbers plain (a whole one without a decimal point, a fraction in full and never
  in exponent form), nothing else on stdout. The names are fact kinds where the value is a fact (`SIZE.source_lines`) and, where it is computed from
  facts, a name under the metric's own: `COMPLEXITY.cognitive_total` and `COMPLEXITY.functions`, `MAINTAINABILITY.files`, `MAINTAINABILITY.part.<part>` for
  each of E-051's parts present and `MAINTAINABILITY.weighted`. The computed values come from `score/` (`FactsText`, over `Formulas` and `Maintainability`),
  the code that scores with them, never from arithmetic in `cli/` (E-010). `--facts` goes with neither `--markdown` nor `--date`, which shape a card. The
  card printed without `--facts` is unchanged, so no golden moves.
- **Why:** Three agents recording the baseline (J-091, J-092, J-094) each wrote a scratch program to see these values, and the navigation review (J-093) made
  the option a next step. One line per value is what a baseline's record, a hand check of a golden and `grep '^SIZE'` all want; sorted by name, so a
  change in the order the modules run in moves no line.
- **Held by:** `score/evaluator/score/FactsText.java`; `cli/evaluator/cli/Main.java` (`parse`, `score`); `test/evaluator/score/FactsTextTest.java`;
  `test/evaluator/cli/MainTest.java`; `docs/how/command-line.md`.
- **Source:** the navigation review J-093's first next step (Ghost's handoff); Claude Sonnet 5.5 built it.
- **Status:** in force
- **subject:** command line
- **See also:** E-010, E-051, E-054, E-074, E-075

### E-077: `gradle test -PshowGoldens` shows what a golden would change and writes nothing; one helper holds every golden
- **Decision:** A test run with `-PshowGoldens` compares the goldens as a plain run does and fails as it does, but each failure message also carries, for every golden
  that differs, the changed lines: `@@ golden line N, new line M @@`, each golden line gone as `- text`, each new line as `+ text` (no context lines), and, for one
  line changed alone, the column where the two part. A golden that matches shows nothing; a missing one is said missing and not created. Nothing is written. Every
  test that holds a golden does it through `test/evaluator/Goldens.java` (`check` for one golden, `Batch` for several in one test), the one place that reads
  `-PwriteGoldens` and `-PshowGoldens` and the one diff; none keeps a copy. The two properties together are refused (`build.gradle` fails the run before any test
  starts; `Goldens.mode` refuses them too): a run either writes or shows, so a reviewer shows first, reads, and then writes. Without either property a golden test
  behaves as before (E-018); its failure message now names `-PshowGoldens` first.
- **Why:** `docs/how/change-a-formula.md` step 6 asked an agent to read each changed golden before writing the goldens, but the only way to see one was to write it
  and read `git diff`, or render it with a scratch program (J-091, J-092, J-094), and the navigation review J-093 made it a next step. The diff is in the failure
  message, not on stdout, because the build shows a failed test's message in full and nothing else (`testLogging`). Both properties together could only mean
  "write, and also write nothing", so it is refused rather than given a precedence nobody would guess.
- **Held by:** `test/evaluator/Goldens.java`; `test/evaluator/GoldensTest.java` (a match shows nothing, a difference shows exactly its lines, nothing is written, the
  properties together are refused); the six golden tests: `CardGoldensTest`, `IngestTest`, `JavaAdapterTest`, `TreeSitterAdaptersTest`, `DependenciesTest`,
  `DuplicationTest`; `build.gradle`.
- **Source:** the navigation review J-093's last next step (Ghost's handoff); Claude Sonnet 5.5 built it.
- **Status:** in force
- **subject:** tests
- **See also:** E-018, E-076

### E-078: The seed corpus is held to the rubric its README names, not to the current rubric
- **Decision:** `corpus/README.md` names one rubric, in the form "rubric" and the hash in backticks; every card in `corpus/` is a JSON and a Markdown file, listed in
  the README's table, and both say they were made under that rubric (`produced.rubricHash` and the "Produced" line). The test reads this repository alone. It does not
  require the cards to carry the current rubric's hash: a card made under an older rubric stays a true record of its day, dated and hashed, until someone makes it
  again with the steps in the README.
- **Why:** The cards were made under rubric b8043f1115a0 and the rubric moved to version 4 (E-070 to E-075) with nothing noticing; a hand-edited hash or a Markdown
  left behind would have read as true. A test of the current hash would fail the pre-push at every rubric change until Suite Builder and Katana (outside this
  repository, which a clean clone cannot reach) and this repository at a7fea50f were scored again, so the check that can run anywhere is that each card says what the
  README says. The cards made again at the rubric's change are re-made on purpose, at the same commits and date (J-098).
- **Held by:** `test/CorpusTest.java` (which also rejects its own bad inputs); `corpus/README.md`.
- **Source:** the corpus task given to a Claude Sonnet 5.5 agent (HANDOFF's seed corpus); decided by that agent, for Ghost to confirm.
- **Status:** in force
- **subject:** corpus
- **See also:** E-054, E-057, E-070

### E-079: A calibration label is one JSON file, an opaque id and five closed fields, held by a loader in rubric/
- **Decision:** The calibration set (TODO.md 2.1) lives in `calibration/`, one file `<id>.json` per hand-labelled repository, the id `c` and three digits and the file named by it. Fields: `id`; `domain`, one of
  `web-app`, `cli-tool`, `library`, `data-pipeline`, `game`, `mobile-app`, `infrastructure`, `machine-learning`, `hardware`, `crypto-defi`, `developer-tooling`, `agent`; `kind`, one of `own-work`, `clone`, `generated` (the admission gate's cases, E-056);
  `ambition`, one of `toy`, `utility`, `product`, `research` (docs/SCORE-CARD.md); `quality`, a whole number 1 to 5 (docs/DESIGN.md step 2.1); and `rationale`, optional, one line of 280 characters at most. Ambition and
  Quality are labelled for every kind. A field outside this list, a missing required field, a duplicate id, a file not named by its id, a rationale holding anything URL-like (a scheme, `www.`, `@`, a host, `github`, an owner/repo
  pair) or name-like (two capitalised words in a row) is refused by `CalibrationSet` and `Label` in `rubric/`. The set must span at least five domains and reach 30 labels: the test reports PENDING below 30 and enforces both from 30.
  The id-to-repository map is Ghost's and stays outside this repository. The loader's own tests use synthetic labels under `test/fixtures/calibration-synthetic/`, ids `c9xx`, never in `calibration/`.
- **Why:** docs/DESIGN.md names the scales (Ambition toy / utility / product / research, Quality 1–5) but no other field; E-014 forbids any name, URL or identity from entering, so the id is opaque by construction
  and the one free field is screened. The domain list is closed so "spans five domains" and the per-domain drift the bias audit wants (DESIGN open question) are countable. The name-like screen over-refuses on purpose
  (it also stops "Spring Boot"): Ghost rewrites the line, a leak does not get in. Pending below 30 so the build is not red while the labelling waits on Ghost, enforced after so the item cannot close short.
- **Held by:** `rubric/evaluator/rubric/CalibrationSet.java`, `Label.java`; `test/evaluator/rubric/CalibrationSetTest.java`; `calibration/README.md`.
- **Source:** the TODO.md 2.1 format task given to a Claude Sonnet 5.5 agent; the field list is that agent's, for Ghost to confirm or change before labelling.
  Ghost added hardware, crypto-defi, developer-tooling and agent to the domain list (2026-10-24).
- **Status:** superseded by E-080 (its field list only, the rest stands)
- **subject:** calibration
- **See also:** E-014, E-056, E-057

### E-080: A calibration label is three fields, kind, ambition and quality, and an opaque id; no domain, no rationale
- **Decision:** A calibration label (TODO.md 2.1) is one file `<id>.json` in `calibration/`, the id `c` and three digits and the file named by it. Fields, all required: `id`; `kind`, one of `own-work`, `clone`,
  `generated` (the admission gate's cases, E-056); `ambition`, one of `toy`, `utility`, `product`, `research` (docs/SCORE-CARD.md); `quality`, a whole number 1 to 5 (docs/DESIGN.md step 2.1). Any other field,
  a missing field, an unknown value, a duplicate id or a file not named by its id is refused by `CalibrationSet` and `Label` in `rubric/`. `domain` and `rationale` are gone from the format, the loader, the tests, the
  fixtures and the README, and with the rationale its URL and name screen, since no free text remains to screen. The set still reaches 30 labels: the test reports PENDING below 30 and MET from 30. "Spans at least five
  domains" is no longer checked by the repository: the spread of domains is recorded in the id-to-repository map, which is Ghost's and stays outside this repository (E-014). This supersedes E-079's field list;
  everything else in E-079 (the opaque id, one file per label, the synthetic fixtures `c9xx`) stands.
- **Why:** A label exists only to give ground truth for the judges (Ambition, the agent-review half of Quality) and for the admission gate (kind). Domain was only a spread check and nothing consumes a rationale;
  a field nothing reads is a place for a leak (E-014) and a cost for Ghost to fill 30 times. Three closed fields cannot name a repository, an owner or a person.
- **Held by:** `rubric/evaluator/rubric/CalibrationSet.java`, `Label.java`; `test/evaluator/rubric/CalibrationSetTest.java`; `calibration/README.md`; `test/fixtures/calibration-synthetic/`; TODO.md 2.1's check.
- **Source:** Ghost's instruction to trim the calibration label to ambition, quality and kind (2026-10-24), carried out by a Claude Sonnet 5.5 agent.
- **Status:** in force
- **subject:** calibration
- **See also:** E-014, E-056, E-079
- **Supersedes:** E-079

### E-081: `python3`, standard library only, is in the pinned image for agent-runner/, and no network door exists yet
- **Decision:** The pinned image (`Dockerfile`) installs `python3` from Ubuntu 24.04's apt with `--no-install-recommends`, beside `git`, and prints its version. The base stays pinned by digest. Nothing else is added: no `pip`, no third-party Python package, no install script, no Java dependency. `agent-runner/` (TODO.md 2.2, E-001) is Python 3 on the standard library only. The image gets `python3` 3.12.3-0ubuntu2.1 (with `python3-minimal` 3.12.3-0ubuntu2.1, `python3.12` and `libpython3.12-stdlib` 3.12.3-1ubuntu0.10, the minimal set `python3` needs; `unittest` and `ast` come from the last). The pinned base already carried this version as a dependency of another package, so apt only marked it as installed on purpose: the line keeps it if that dependency goes. `test/AgentRunnerTest.java` runs `python3 -I -B -m unittest discover` in `agent-runner/` under `gradle test` and is PENDING and green when `python3` is absent. A Python test (`agent-runner/tests/test_guard.py`, `agent_runner/guard.py`) fails any file of `agent-runner/` other than `agent_runner/transport.py` (2.2l; it does not exist yet, so there is no door) that imports `socket`, `http`, `urllib`, `ssl`, `anthropic` (or another network or model-client module the guard lists) or reads an environment variable naming a key. No API, no key, no model call, no network access from `agent-runner/` is approved.
- **Why:** The Agent SDK and the judge client have no Java, and the kernel imports no model client (E-003), so the judge harness is Python (E-001) and its tests need an interpreter in the image that builds with the network off (E-021). One apt package is the least invasive way: no download outside the distribution's signed archive, no script piped to a shell, no package manager of its own. The import guard is the counterpart of MOD-05, so the harness cannot reach the network or a key by accident before Ghost approves 2.2l.
- **Held by:** `Dockerfile`; `test/AgentRunnerTest.java`; `agent-runner/tests/test_guard.py`; `agent-runner/agent_runner/guard.py`; `docs/how/judge-harness.md`; `test/SupplyChainTest.java` (images and their downloads).
- **Source:** Ghost approved python3 in the pinned image for agent-runner, 2026-10-25.
- **Status:** in force
- **subject:** agent-runner
- **See also:** E-001, E-003, E-013, E-014, E-017, E-021

### E-082: The judge's kind is six values, E-057's four plus own-work and clone
- **Decision:** The repository kind in the judge's answer (TODO.md 2.2d, `agent-runner/schema/judgment.schema.json`) is one of `generated`, `clone`, `plan only`, `mixed`, `own-work`, `built`: E-057's four
  (built, plan only, generated, mixed) plus the calibration labels' own-work and clone (E-080). The schema's description for `kind` gives the judge a precedence rule, and the 2.2e prompt quotes it: take the first that
  the summary shows, in this order. generated: a tool produced it wholesale and little is the author's own work. clone: a copy, or a lightly changed copy, of another project, said only on what the summary shows (the
  README naming an upstream, a history that begins with one bulk import, files that are another project's); with no such sign it is not a clone. plan only: a plan or a specification with little or no running code. mixed:
  original work with real code and a substantial part that is plan or generated. own-work: original working code. built: only where the repository is working code and the summary cannot tell whose it is or how it
  came about, never the default for doubt between the others. Everything else in E-057 stands: the judge classifies and judges, the kernel counts the own code and applies the card's rules, which now read on the
  six values (generated declines, E-056; plan only carries no Size, Complexity or Maintainability and reads Ambition unverified).
- **Why:** The judge's kind becomes comparable to the 30 calibration labels (own-work, clone, generated), so 2.2i can measure agreement on kind for all three of the labels' values and not only on "generated or not",
  which the set could not even test (it holds no generated repository). Trade-offs, chosen knowingly: (1) one enum now mixes two axes, what the repository contains (plan only, mixed, built) and whose work it is
  (own-work, clone, generated), so the six values overlap, and the precedence rule is what keeps them apart; the wording can reduce the ambiguity, not remove it. (2) A clone call is made from a summary with
  only the newest 30 commits and a few sampled files, which often cannot show a copy; the rule tells the judge to say clone only on what it sees, and the agreement figure shows how well that works. (3) Judge trial 4's
  finding that Haiku named the kind in all but 2 of 276 runs (E-057) was measured on the four-value vocabulary with tools; it must be re-measured on six values from a summary (2.2m, 2.2n). (4) E-056's gate may
  use the judge's clone call together with the kernel's fork and history facts; a clone call alone is not a decline.
- **Held by:** `agent-runner/schema/judgment.schema.json` (the enum and its description); `agent-runner/agent_runner/judgment.py`; `agent-runner/tests/test_judgment.py`; TODO.md 2.2d, 2.2i.
- **Source:** Ghost chose to add own-work and clone to the judge's kind, 2026-10-25.
- **Status:** in force
- **subject:** judges
- **See also:** E-055, E-056, E-057, E-080
- **Supersedes:** E-057 (in part: its list of kinds only)

### E-083: On a category tie among the three Haiku votes, Opus decides and its answer stands
- **Decision:** When the three Haiku votes on a category (the kind, a folder's class, the Ambition tier) have no majority, that is no value with more than half of the votes, Claude Opus 5.5 judges that category and its answer stands, as it does for a metric more than 15 points apart (E-055). The same holds when only two runs are usable and they differ. The Opus answer may be a value none of the Haiku runs gave. Opus is called once for everything that needs it. The rule is built in `agent-runner/agent_runner/ensemble.py` (TODO.md 2.2h).
- **Why:** E-055 says a category is the three runs' vote and that Opus takes the metrics the runs disagree on, but is silent on a vote with no winner. Opus's deeper read is what E-055 trusts where the cheap runs disagree, a tie is that disagreement in its plainest form, and the alternatives (a fixed pick, the run with the highest confidence) would be an arbitrary choice that hides the split. The cost is one Opus run, which E-055 already accepts for a split.
- **Held by:** `agent-runner/agent_runner/ensemble.py`; `agent-runner/tests/test_ensemble.py` (the three-way tie cases); `docs/how/judge-harness.md`; TODO.md 2.2h.
- **Source:** Ghost confirmed that Opus decides a category tie, 2026-10-25.
- **Status:** in force
- **subject:** judges
- **See also:** E-055

### E-084: A judged field is live only if its raw agreement is at least 80 % and it beats the constant answer by a margin
- **Decision:** A judged field (the Ambition tier, Quality exactly, Quality within one, the kind) is live for a prompt only if BOTH hold on the calibration set: (1) the raw agreement is at least 80 %, and (2) it beats the constant-answer baseline, always answering the most common label (for Quality within one, the integer that is within one of most labels), by a margin. The margin is a parameter, default one half: the figure must close at least half of the gap between the baseline and 100 %, that is figure >= baseline + 0.5 * (1 - baseline), so a baseline of 90 % needs 95 % and no fixed number of points is asked. The baselines are computed from the labels. On the 30 labels the counts are: Quality within one baseline 27 (90 %), needs 29 (28 of 30 is 93.3 %, under 95 %); kind baseline 23 (76.7 %), needs 27 (88.3 %); Ambition baseline 14 (46.7 %), the margin needs 22 and the 80 % floor binds at 24; Quality exactly baseline 17 (56.7 %), the margin needs 24 (78.3 % rounds up) and the floor 24. A partial, refused or missing result for an id counts as a miss for that id and is counted apart. A prompt with no record for its current hash is live for no field (SC-05). Built in `agent-runner/agent_runner/agreement.py` (TODO.md 2.2i).
- **Why:** A constant answer clears 77 % on kind and 90 % on Quality within one on this set, so the 80 % floor alone would call a judge that learned nothing live. The margin over the baseline answers that; a fixed +10 points would be impossible at a 90 % baseline, which is why the margin is a share of the gap. The size of the share, one half, is the agent's proposal and open for Ghost to confirm or change; it is one constant (`MARGIN`) and a parameter.
- **Held by:** `agent-runner/agent_runner/agreement.py`; `agent-runner/tests/test_agreement.py` (k of 30 either side of the floor and the margin for each field); `docs/how/judge-harness.md`; TODO.md 2.2i, 2.2k.
- **Source:** Ghost chose both the raw 80 % floor and the margin over the baseline, 2026-10-25; the margin's size (half the gap) is the agent's proposal for Ghost to confirm
- **Status:** in force
- **subject:** judges
- **See also:** E-055, E-056, E-082

### E-085: `score --judged` counts the judge's own code; its own tests make the test ratio, by class
- **Decision:** `evaluator score --judged <file>` reads a judgment file and measures Size, Complexity (the cognitive total and the dependency depth) and Maintainability's parts only over the files whose most specific classified folder is own code (E-057). The file is a wrapper of three members, `promptHash` (64 lower-case hex digits), `models` (one to eight model ids) and `judgment` (the answer in the shape of `agent-runner/schema/judgment.schema.json`, 2.2d), and any other file is refused with a message. A file belongs to the longest classified folder path holding it (`.` is the root and loses to any deeper folder). A file in a folder classed own code is counted; in no classified folder, the kernel's own rule counts it (it is counted, and is a test by its name); in own tests, it is not counted as code but is a test file of the test-to-code ratio, whatever its name; in any other class (own tooling, docs, generated, vendored, reference, fixture data) it is set aside with the reason `judged: <class> (folder <path>)`. In the ratio a file in an own-code folder is code whatever its name, so the class decides, and a file in no folder is a test by the kernel's name rule. The judge can only narrow: a file the kernel set aside as vendored or generated stays aside. The navigability checklist keeps the whole inventory. The judgment's kind is checked and kept, not acted on here (2.2k). The card names the judgment's prompt hash and models in `produced` (JSON) and on a `Judged` line (Markdown); without `--judged` the card and every fact are byte for byte as before. No formula changes.
- **Why:** E-057 divides the work: the judge classifies, the kernel counts. It did not say how own tests enter Maintainability, whose tests part is test lines over the other counted lines (E-034): if own tests were set aside with the other classes the part would read 0 for every judged repository, so they are kept for the ratio alone, as the numerator. Taking the class over the file's name respects the judge's reading of the folder, and the kernel's name rule stays the rule where the judge said nothing. The navigability checklist is a different measure (instruction files, naming, module cycles) that the judge also scores, and narrowing it would change a number the judge was not asked to change. The wrapper carries the prompt hash and models because the 2.2d answer has no place for them, and the card must say which prompt and which models its numbers rest on (SC-05).
- **Held by:** `ingest/evaluator/ingest/OwnCode.java`, `Judgment.java`; `cli/evaluator/cli/Pipeline.java` (`code`); `analyze/evaluator/analyze/TestFiles.java` (`ratio` with a predicate), `Complexity.java` (`Report.restrictedTo`); `score/evaluator/score/Judged.java`, `CardJson.java`, `CardMarkdown.java`; `test/evaluator/ingest/OwnCodeTest.java`, `JudgmentTest.java`, `test/evaluator/cli/MainTest.java`, `CardGoldensTest.java` (the judged goldens); TODO.md 2.2j; `docs/how/command-line.md`.
- **Source:** Claude Sonnet 5.5 building TODO.md 2.2j, 2026-10-26; the own-tests rule and the wrapper are for Ghost to confirm.
- **Status:** in force
- **subject:** judges
- **See also:** E-003, E-025, E-034, E-051, E-057, E-074, E-075
