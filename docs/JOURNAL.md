# Journal: every event in the work, as a record a researcher can count

One entry per event: a contradiction found, a decision taken, a failure caught or missed, a change of
direction, a measurement. Not a restatement of the commit log (`git log` has what changed) and not
a status list (`DONE.md`, `TODO.md`) and not the design (`theGhost.md`): the journal records *what
happened while the work was done*. Fixed fields, so entries can be counted and compared across
projects (Katana keeps the same journal under its R-046, Suite Builder under G-162). Newest last.
Written by whoever was there, in the same commit as the event when possible; an agent writes its own
entries (E-008). `test/JournalTest.java` holds it: ids rise, every entry has its Who and its Class.

Field meanings. **Who**: Ghost, or the agent by its model name, or both. **Intent**: what the work
was trying to do when the event happened. **Found**: the fact the event turned on. **Options**: what
was considered, including the one taken. **Resolved**: what was done, by whom. **Touched**: decisions,
metrics, documents or modules the event changed or created. **Caught by**: for a failure, what
noticed it. **Cost**: wall time or commits, where known. **Class**: one of `contradiction`,
`ambiguity`, `failure-caught`, `failure-missed`, `decision`, `measurement`, `direction`, `trial`,
`deferred`.

---

## 2026-10-18

### J-001 The project started, as the talent-matching idea with a name and a rubric
- **Who:** Ghost, with Fable. **Class:** decision.
- **Intent:** Start Employee Evaluator from the pitch.
- **Found:** The pitch was aimed at HR staff; the buyer is HR staff. The six metrics rewarded bloat as
  written; "actual quality against the same agent" needs a corpus that does not exist yet.
- **Options:** keep the pitch as written; aim it at the process. One composite score; seven ranked
  separately with ratios. A gate on provenance; a score.
- **Resolved:** Pitch aimed at the process (E-023); seven metrics, no composite, ratios (E-011);
  provenance a score with four layers, the fourth deferred (E-012). Website and GitHub app merged
  (E-002). Design doc written; repository created under Ghost in the Kernel Labs.
- **Touched:** docs/DESIGN.md; E-002, E-011, E-012, E-023.

### J-002 Java for the kernel, decided on what scales
- **Who:** Ghost, with Fable. **Class:** decision.
- **Intent:** Choose the scorer's language before any scaffold.
- **Found:** At 20k submitters the cost is model spend and the sandbox fleet, not orchestration; the
  Agent SDK has no Java.
- **Options:** TypeScript everywhere; Java kernel with a TypeScript frontend and a Python agent
  runner; Java with a TypeScript agent runner.
- **Resolved:** Java 21 kernel, TypeScript web, Python agent runner (E-001, E-003).
- **Touched:** E-001, E-003; docs/DESIGN.md.

### J-003 The parents' guardrails ported before the first line of product code
- **Who:** Ghost, with Fable. **Class:** direction.
- **Intent:** Ghost asked that every lesson from Katana and Suite Builder be in from the start:
  theGhost, intent maps, tests, readability rules, the kernel as the goal, modular design, AI
  navigability rules, the environment.
- **Found:** Katana's tooling (`scripts/`, the doc tests under `test/`) is plain Java with no
  dependency and ports with a rename; Suite Builder's module map and kernel map are the same idea
  Katana already carried. The one thing without a direct parent is the "intent map": Katana's Intent
  IR is facts with evidence, confidence and state, which is exactly what a score card should be.
- **Options:** write the guardrails fresh for a smaller project; port Katana's and prune later.
- **Resolved:** Ported: theGhost.md (E-001 to E-023), docs/modules.json and its test, the kernel map
  and its generator, the doc tests (ghost, docs, todo, glossary, journal, kernel map), AGENT.md with
  the metrics, the pinned image and offline build, CI with the network off. The score card became the
  intent map (docs/SCORE-CARD.md, E-017). Mutation testing and the tithe gate are proposed, not yet
  vendored (E-019, TODO.md 1.10).
- **Touched:** everything under docs/, scripts/, test/; theGhost.md; build.gradle; Dockerfile.

## 2026-10-19

### J-004 CI made manual: the minutes are a scarce resource
- **Who:** Ghost, with Fable. **Class:** decision.
- **Intent:** The spotless pass over the scaffold before Opus starts 1.2.
- **Found:** The organization's GitHub Actions minutes are severely constrained, and GitHub is the
  only place apart from Ghost's PC where Windows builds work; a workflow on every push to `main`
  would spend them on what `scripts/prepush.sh` already proves locally.
- **Options:** keep the push trigger; run on a schedule; run by hand only.
- **Resolved:** By hand only (E-024). Also in this pass: `gradle metrics` aligned with the rows
  `docs/AGENT.md` tracks; the README got a Setup section; HANDOFF tells Opus that Gradle itself has
  not yet executed `build.gradle` (the workspace that wrote it had no Gradle and no Docker daemon)
  and to run the fast loop first.
- **Touched:** E-024; `.github/workflows/ci.yml`; `scripts/Metrics.java`; README.md; docs/HANDOFF.md.

### J-005 Gradle's first run: the scaffold builds, one deprecation fixed
- **Who:** Claude. **Class:** failure-caught.
- **Intent:** The handoff's first step: run `build.gradle` under Gradle itself before 1.2, since the
  workspace that wrote it had none.
- **Found:** On JDK 21.0.12 and Gradle 8.14.3 the build and the fast loop pass (9 tests: 7 doc tests,
  2 of the command line); `gradle metrics` runs. Gradle warned once: `exceptionFormat 'full'` sets a
  property with the space syntax, removed in Gradle 10. No Docker daemon in this workspace, so the
  image is still unbuilt.
- **Options:** leave the warning until a Gradle upgrade; fix it now.
- **Resolved:** `exceptionFormat = 'full'`; the build runs with no deprecation. The image's first
  build stays with Ghost (docs/HANDOFF.md).
- **Caught by:** `gradle test --warning-mode all`.
- **Touched:** build.gradle.

### J-006 TODO 1.2 built: the rubric's types and the first golden
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.2: the Fact and ScoreCard types held to docs/SCORE-CARD.md, and a repository
  read into an inventory with its Size facts, the inventory a golden.
- **Found:** The score card's table named the card's fields in prose ("seven scores", "provenance
  tier"), which no test can match against a type; two invariants (SC-08 the anonymized card, SC-09
  the card goldens) belong to steps not yet built; "modules" in Size has no meaning before something
  reads imports; the fixtures' own `.java` would have been compiled as our tests and counted in our
  metrics.
- **Options:** for the table, a mapping from prose to field names inside the test, or the table
  naming the fields; for SC-08 and SC-09, stub code citing them, or the document naming the step
  that holds each; for modules, a guess now, or folders now and modules at 1.3.
- **Resolved:** The card table names the record's components in backticks; an invariant is cited in
  `rubric/` or names an open TODO step, and the test fails once that step is done and nothing cites
  it. Size counts folders (E-026), set-aside rules recorded (E-025), both for Ghost's review. Fixtures
  excluded from the test source set and from `scripts/Repo.java`. One count in a new test was wrong
  (20 for 23); the code was right.
- **Caught by:** the doc test on its first run (the `(E-011)` after a heading); the unit test.
- **Touched:** `rubric/`, `ingest/`; E-025, E-026; docs/SCORE-CARD.md, docs/GLOSSARY.md,
  docs/modules.json, docs/MODULES.md, CLAUDE.md; build.gradle; scripts/Repo.java.
- **Cost:** one session, four commits.

### J-007 ANTLR chosen over tree-sitter for 1.3
- **Who:** Ghost, with Claude. **Class:** decision.
- **Intent:** Unblock TODO.md 1.3 before it starts.
- **Found:** tree-sitter is free (MIT) but brings native libraries per platform into an offline,
  pure-Java build; Ghost asked whether "vendoring" meant a paid service (it does not: a free
  library's files committed under `third_party/`).
- **Options:** tree-sitter with its native libraries; ANTLR with grammars-v4; a separate parsing process.
- **Resolved:** ANTLR (E-027); TODO.md 1.3's check and the handoff updated.
- **Touched:** E-027; TODO.md; docs/HANDOFF.md.

### J-008 tree-sitter after all: E-027 superseded the same evening
- **Who:** Ghost, with Claude. **Class:** direction.
- **Intent:** Ghost asked which parser is better for the project, not for the build.
- **Found:** Claude's recommendation (E-027) had weighed a pure-Java build above what the parser must
  do: read broken and newly written code in many languages, at scale. On those, tree-sitter is ahead;
  its native library is one per platform, and production is one Linux image.
- **Options:** keep ANTLR; tree-sitter with its Java binding proven first; a separate parsing process.
- **Resolved:** tree-sitter (E-028, superseding E-027), with two checks before anything is vendored
  and ANTLR as the fallback. TODO.md 1.3 and the handoff say so.
- **Caught by:** Ghost's question.
- **Touched:** E-027, E-028; TODO.md; docs/HANDOFF.md.

### J-009 E-025 and E-026 approved as built
- **Who:** Ghost, with Claude. **Class:** decision.
- **Intent:** Ghost's review of the two judgment calls made under TODO.md 1.2.
- **Found:** Their weak spots, as presented: a real source folder named `build` or `out` is set aside;
  a library renamed out of `vendor/` counts as the applicant's until copied code is detected (1.4);
  comments count toward Size; folders stand in for modules until 1.3's dependency graph.
- **Options:** approve; change the folder list; strip comments once 1.3 can parse.
- **Resolved:** Both approved as built.
- **Touched:** E-025, E-026 (their Source); docs/HANDOFF.md.

### J-010 tree-sitter's gate passed: a Java 21 binding, its natives in its jars
- **Who:** Claude. **Class:** measurement.
- **Intent:** TODO.md 1.3, slice 1 (Ghost: "keep the order, lets push through", with 1.3 in slices):
  the two checks E-028 asks before anything is vendored.
- **Found:** The official binding, `io.github.tree-sitter:jtreesitter` 0.26.1, is compiled for Java 23
  (class version 67) and ships no native library. The community JNI binding, `io.github.bonede:tree-sitter`
  0.26.6 (MIT), is compiled for Java 11, bundles tree-sitter's native library for Linux, macOS and
  Windows, and its Python (0.25.0) and TypeScript (0.23.2) grammars come the same way. On Java 21 it
  parsed both languages, `satisfies` included, and gave a tree with its error flagged for broken
  Python; run again with every proxy pointed nowhere, nothing changed. Its Maven `.pom` names no
  license; the project's repository says MIT.
- **Options:** the official binding, with the kernel moved to Java 23+ and natives built by us; the
  bonede binding on Java 21; ANTLR (E-027).
- **Resolved:** The bonede binding, vendored under `third_party/tree-sitter/` with four MIT licenses,
  checked against Maven Central's SHA-1 and recorded by SHA-256; the kernel stays on Java 21.
  `TreeSitterLoadsTest` proves the natives load wherever the fast loop runs. Still unproven: the image
  (no Docker daemon here) and Windows; both are Ghost's run. The risk kept: one maintainer behind the
  binding; it is MIT and vendored, so a fork or our own build of the natives stays open.
- **Touched:** `third_party/tree-sitter/`, `third_party/MANIFEST.md`; build.gradle; docs/HANDOFF.md.

### J-011 The one-step-per-session rule struck
- **Who:** Ghost, with Claude. **Class:** direction.
- **Intent:** Keep 1.3 moving after its first slice.
- **Found:** The rule in `CLAUDE.md` ("one step of TODO.md per session ... stop") fits work where
  context cannot be cleared; in coding sessions compaction clears it well, and stopping forced a
  restart after every step.
- **Options:** keep it; strike it; keep it but count slices instead of steps.
- **Resolved:** Struck (E-029): each step is closed and pushed as before, then the next one starts.
- **Touched:** E-029; CLAUDE.md.

### J-012 TODO 1.3, slice 2: the adapter interface and the Java adapter
- **Who:** Claude. **Class:** decision.
- **Intent:** Each Java function's cyclomatic and cognitive complexity as facts, behind one adapter
  interface the TypeScript and Python adapters will share.
- **Found:** Every value worked by hand on the java-complexity fixture matched on the first run,
  SonarSource's own examples included (`sumOfPrimes` 7, `getWords` 1). Two calls had to be made: how
  cyclomatic counts (as `scripts/Scan.java` already did, one per case), and where a class inside a
  method is counted (its own functions, not the enclosing one's). A file that does not parse would
  give a partial tree; it is listed as unread instead. `gradle metrics` then found the kernel's
  cyclomatic count and `Scan.paths` with one body under two names.
- **Options:** for a broken file, measure the partial tree, or say it was not read; for the copy, keep
  both, or the scripts calling the kernel's.
- **Resolved:** E-030 records the counting, for Ghost's review; a broken file is unread with its line.
  The copy goes in the next commit, a tithe (11 commits since the last).
- **Caught by:** `gradle metrics` (the duplicate helper).
- **Touched:** `analyze/`; E-030; docs/modules.json, docs/MODULES.md, CLAUDE.md; `Inventory.quote` made public.

### J-013 TODO 1.3, slice 3: the TypeScript and Python adapters on tree-sitter
- **Who:** Claude. **Class:** decision.
- **Intent:** The same two complexities for TypeScript and Python, by E-030's rules.
- **Found:** tree-sitter's node names were printed from real parses before the rules were written
  (Python's elif and else hang off the if; TypeScript's else if is an if inside an else clause). Every
  Python value worked by hand matched on the first run; one TypeScript value did not (`logic`, 3 for
  4): an arrow function whose body is an expression had its walk start below the body, losing the
  outer run of `||`. The hand count was right; the walk now starts at the body itself, in both
  adapters. A `.tsx` file needs tree-sitter's TSX grammar, vendored beside the others.
- **Options:** for TypeScript, every arrow function a function, or only named ones; for Python, a
  closure counted in its function, or on its own.
- **Resolved:** Named functions only; closures on their own (E-031, for Ghost's review). JavaScript has
  no adapter yet (not in 1.3's check); the handoff names its grammar.
- **Caught by:** `TreeSitterAdaptersTest` (the hand-worked `logic`).
- **Touched:** `analyze/`; `third_party/tree-sitter/` (TSX); E-031; CLAUDE.md, docs/MODULES.md.

### J-014 TODO 1.3 done: the dependency graph and unused imports, slice 4
- **Who:** Claude. **Class:** decision.
- **Intent:** The last slice of 1.3: the dependency graph on the fixtures, and unused imports as a fact
  (Ghost's ask, J-009's follow-up).
- **Found:** Every edge, the longest chain and each unused name on the imports fixture matched the hand
  count on the first run. The test that an import cannot climb out of the repository caught a real
  escape: Python's `from ...x import` in `a/b.py` stopped at the root and matched `x.py` there,
  where Python itself would refuse it. Fixed: a relative import above the root leads nowhere.
- **Options:** for depth with cycles, refuse a cyclic graph, count each file, or collapse each cycle to
  one link; for resolution, a build tool's view, or paths.
- **Resolved:** Cycles collapsed (Tarjan, without recursion: a 100,000-file chain is tested), paths
  only (E-032, for Ghost's review). 1.3 moved to docs/DONE.md; the handoff is for 1.4.
- **Caught by:** `DependenciesTest.aPathOutOfTheRepositoryLeadsNowhere`.
- **Touched:** `analyze/`; E-032; TODO.md, docs/DONE.md, docs/HANDOFF.md, CLAUDE.md, docs/MODULES.md,
  docs/modules.json.

### J-015 The supply-chain baseline, institutionalized as a test
- **Who:** Ghost, with Claude. **Class:** direction.
- **Intent:** Ghost's Supply-Chain Security Baseline (2026-10-19, from Ghost's research with another
  Claude session), rolled out to this repository, its item 2: "This gets institutionalized. it goes
  into tests, and is validated against."
- **Found:** Run first against the repository as it stood, the new test failed on three real gaps: the
  one workflow's action pinned to a tag (`actions/checkout@v4`), the workflow with no `permissions`
  block, and the directive missing. It also failed once on its own fault, reading the word
  "repositories" in a `build.gradle` comment as a declaration; it now reads code only. The vendored
  jars had SHA-256s in the manifest but nothing checked them.
- **Options:** the baseline as a document reviewers remember; as a test that fails the fast loop.
- **Resolved:** E-033 and `test/SupplyChainTest.java`, each rule proven on a broken sample;
  `actions/checkout` pinned to v4.4.0's SHA (`11d5960…`, July 2026, past the 7-day cooldown),
  `permissions: contents: read`; the directive in CLAUDE.md and theGhost.md, held equal;
  `Quarantine.strip` for submissions; TODO.md 1.5 and 1.6 carry the sandbox's and git's part. Left to
  Ghost (the handoff): branch protection, scanning, MFA, tokens, the audit script. Suite Builder,
  the baseline's first repository, waits for its own session.
- **Caught by:** `SupplyChainTest`, on its first run.
- **Touched:** E-033; CLAUDE.md, theGhost.md, TODO.md, docs/HANDOFF.md; `.github/workflows/ci.yml`;
  `ingest/evaluator/ingest/Quarantine.java`; `test/SupplyChainTest.java`.

### J-016 TODO 1.4's measurements: test files, coupling, duplication
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.4: coupling, duplication by token shingling and the test-to-code ratio, each
  a fact, with a fixture whose copied function is caught.
- **Found:** The renamed copy in the duplication fixture was caught whole and the near miss was not, on
  the first run. One hand count was wrong (`price_order` is 78 tokens, not 77: line 8 has 14); the
  tokens were printed and the test corrected, the code unchanged. The tokenizer's one function had 27
  paths, over `gradle metrics`' 20; split into a lexer of one method per kind of token, every golden
  unchanged. The check's "two ratios" cannot be built at 1.4 as written: they are ratios of scores,
  Quality is not measured before 1.5, and scores are `score/`'s, at 1.9. docs/DESIGN.md says "three
  efficiency ratios" in one place and two in another.
- **Options:** for the ratios, close 1.4 and move them to 1.9; or start `score/` now for
  Maintainability and Complexity, with Quality's ratio not measured.
- **Resolved:** The measurements built and pushed (E-034 to E-036, for Ghost's review); 1.4 stays open,
  the ratios put to Ghost (the chain stops on an item that cannot be done as written, TODO.md).
- **Caught by:** `DuplicationTest` (the hand count); `gradle metrics` (the paths).
- **Touched:** `analyze/` (`TestFiles`, `Coupling`, `Duplication`, `Tokens`; `Measure` carries its end
  line and text); E-034, E-035, E-036; docs/MODULES.md, docs/modules.json, docs/HANDOFF.md.
