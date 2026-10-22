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

### J-017 TODO 1.4 closed; its two ratios moved to 1.9
- **Who:** Ghost, with Claude. **Class:** decision.
- **Intent:** Close 1.4, whose check also named "the two ratios" (J-016).
- **Found:** Both ratios divide one score by another; Quality is not measured before 1.5, and scores
  are `score/`'s (MOD-03), at 1.9.
- **Options:** close 1.4 and move the ratios to 1.9; start `score/` early with Quality's ratio not measured.
- **Resolved:** Ghost: moved to 1.9 (its check and docs/DESIGN.md step 9 now name them); 1.4 to
  docs/DONE.md. Still open for Ghost: docs/DESIGN.md says "three efficiency ratios" once and two
  everywhere else.
- **Touched:** TODO.md, docs/DONE.md, docs/DESIGN.md, docs/HANDOFF.md.

### J-018 TODO 1.5's three questions answered; one continuous session
- **Who:** Ghost, with Claude. **Class:** decision.
- **Intent:** Start 1.5, which runs hostile code and adds dependencies, so it waited on Ghost (E-033).
- **Found:** Three choices: the sandbox, the runners and coverage tools, and how a submission's packages
  arrive with the network off. Ghost also said how the work runs: one continuous session, never
  handed to another; the handoff is the record, not a relay.
- **Options:** Docker per run, Docker with gVisor, or bubblewrap; all three languages' tools or fewer
  first; two phases or never installing.
- **Resolved:** Docker per run, all three languages, two phases (E-037). 1.5 in slices: the readers,
  the runner, the image, the fixtures. This workspace has no Docker daemon, so the real runs close
  on Ghost's machine.
- **Touched:** E-037; docs/HANDOFF.md.

### J-019 TODO 1.5, S1 and S2: the readers and the runner, against a fake sandbox
- **Who:** Claude. **Class:** decision.
- **Intent:** The parts of 1.5 that can be proven without Docker: reading a run's reports, and running
  each language's plan behind a `Sandbox` interface.
- **Found:** Every hand count of the report fixtures held on the first run; the XXE and billion-laughs
  reports were refused. Running as `nobody` (E-037's first wording) would leave files only `nobody` can
  delete in our temporary copy, so the container runs as the copy's owner: unprivileged, never root.
  The JSON reader the doc tests used (`scripts/Json.java`) moved into the kernel for Jest's report,
  with a nesting cap, since a report is an attacker's file. A test of mine matched `--pid` inside
  `--pids-limit`; it now matches whole flags. One name from the repository reaches a shell, a
  requirements file's: only plain names pass. Maven has no lockfile, so E-037 as written measures no
  Maven build; that rule is one switch (`TestPlans.MAVEN_WITHOUT_LOCKFILE`), off, put to Ghost.
  Gradle is not run yet.
- **Options:** for Maven: not measured until it has a lockfile; versions pinned in the pom count as
  locked; or Maven runs with its checksums checked against Maven Central.
- **Resolved:** S1 and S2 built and pushed; the Maven question to Ghost; S3 (the image) next.
- **Caught by:** `DockerSandboxTest` (the loose match, mine).
- **Touched:** `analyze/` (`Reports`, `TestRuns`, `Sandbox`, `DockerSandbox`, `TestPlans`,
  `TestRunner`, `Json` moved from `scripts/`); CLAUDE.md, docs/MODULES.md, docs/modules.json.

### J-020 Maven's tests run with exact versions and strict checksums
- **Who:** Ghost, with Claude. **Class:** decision.
- **Intent:** Answer J-019's question: Maven has no lockfile.
- **Found:** Strict E-037 would leave most Java repositories' tests unmeasured.
- **Options:** strict checksums with exact versions; exact versions alone; not measured.
- **Resolved:** Ghost: checksums enforced (E-038). The off-switch is gone; a pom with a range,
  SNAPSHOT, LATEST or RELEASE is not measured, and the install runs `mvn -C`.
- **Touched:** E-038; `analyze/evaluator/analyze/TestPlans.java` and its test.

### J-021 TODO 1.5, S3: the sandbox image, its pins, and a proxy of our own
- **Who:** Claude. **Class:** decision.
- **Intent:** The image a submission's tests run in, every piece pinned past the 7-day cooldown, and the
  install phase's way out limited to the registries.
- **Found:** Docker Hub rate-limited the digest lookups after the first, and this environment's network
  refuses Adoptium. So the image has one outside base (`python:3.12-slim-bookworm`, built 2026-10-06), takes
  Temurin from the kernel's own pinned image, and fetches Node and Maven as release tarballs checked by
  hash (`sandbox/fetch.py`). Maven Central rate-limited one download, and the 96-byte error page came back
  as the JaCoCo CLI jar: the SHA-1 check refused it, but I then recorded the error page's SHA-256 in the
  manifest; `SupplyChainTest` caught that on its next run. The test also first read a Dockerfile comment
  as a command; comments are skipped now. For the proxy, a small Java one of our own (CONNECT only, the
  registries by exact name, port 443) instead of a vendored squid or tinyproxy: no new dependency, and
  it is tested here.
- **Options:** for the proxy, a vendored one or our own; for the tools, three base images or one plus
  hashed tarballs.
- **Resolved:** Built and pushed; what can be proven here is (the handoff lists it), the rest is
  Ghost's run of `sandbox/setup.sh`. `SupplyChainTest` now holds every Dockerfile to digests, checked
  fetches, hashed pip and `npm ci --ignore-scripts`, and the sandbox's own locks. The kernel image's
  unpinned `apt-get install git` put to Ghost.
- **Caught by:** `SupplyChainTest` (the wrong hash, mine; the comment, my test's).
- **Touched:** `sandbox/`; `third_party/jacoco/`, `third_party/MANIFEST.md`; `analyze/` (`RegistriesProxy`,
  `DockerSandbox`); `test/SupplyChainTest.java`; docs/MODULES.md, docs/HANDOFF.md.

### J-022 TODO 1.5, S4: fixtures that run, their real reports recorded
- **Who:** Claude. **Class:** failure-caught.
- **Intent:** Fixtures whose own tests pass, fail and reach for the network, held to facts worked by hand.
- **Found:** Each fixture's real plan was taken from `TestPlans` and run here, outside a container (the
  fixtures are ours), with the tools S3 proved; every count matched the hand count. The counts showed a
  real inconsistency: Python's coverage counted the test files themselves (7 lines, always run), while
  c8 leaves tests out. Coverage of tests is no measure, and it inflated the number (13 of 13 where the
  code was 6 of 6). The Node plan was also labelled "TypeScript" for a JavaScript repository.
- **Options:** leave Python counting tests; leave them out by E-034's names, as c8 does.
- **Resolved:** Python's coverage omits test files (E-034's names); the Node plan is labelled "Node".
  The recorded reports are goldens; `gradle test -Pdocker` runs the same fixtures in the real sandbox
  and must give the same facts. 1.5 waits on that run, on Ghost's machine.
- **Caught by:** reading the recorded coverage report's files, before writing the expected values.
- **Touched:** `analyze/evaluator/analyze/TestPlans.java`, `TestRunner.java`; `test/fixtures/runs/`,
  `test/goldens/runs/`; build.gradle (`-Pdocker`); docs/HANDOFF.md.

### J-023 A commit that fails in a clean clone reached main: the fixtures were ignored, and my check passed it
- **Who:** Claude. **Class:** failure-missed.
- **Intent:** Push 1.5's S4 (J-022).
- **Found:** `.gitignore`'s `runs/` (E-014: no run output in the repository) is unanchored, so it also
  ignored `test/fixtures/runs/` and `test/goldens/runs/`: the fixtures were never committed, and every
  clean clone failed two tests. The check I ran before each push, a stand-in for `scripts/prepush.sh`
  without Docker, piped Gradle's output through `grep`, so Gradle's exit status was lost and the check
  printed "ok" over a failed build. The pushed commit (d0e8d34) failed in a clean clone; every earlier
  push's output shows no failed test, so it is the only one.
- **Options:** narrow `.gitignore`; rename the folders.
- **Resolved:** The folders renamed `test-runs/` (E-014's rule left as it is). The stand-in rewritten with
  `set -euo pipefail` and every exit status checked; run first against the broken commit, it failed, as it
  must; then against the fix. Missed by me twice: the ignore rule when adding the folders (`git status`
  showed nothing to add, which I did not read), and the check's masked status.
- **Caught by:** the fresh clone's test run, whose failure lines were printed above the false "ok".
- **Cost:** one broken commit on main, fixed by the next.
- **Touched:** `test/fixtures/test-runs/`, `test/goldens/test-runs/`; `TestRunnerFixturesTest`; docs/HANDOFF.md.

### J-024 Linux only and cloud first; Docker's proofs move to a GitHub runner
- **Who:** Ghost, with Claude. **Class:** direction.
- **Intent:** Finish 1.5 without Ghost's machine, which overheats.
- **Found:** This session has `dockerd` but starting it was refused as a containment escape, rightly.
  Gradle's download is blocked from here, and a checksum from memory is not a pin; but the kernel's own
  pinned image holds Gradle 8.14.3 and JDK 21, and `docker cp` takes them out of it on a runner.
- **Options:** grant this session a Docker daemon; a GitHub runner, started by hand.
- **Resolved:** E-039: Linux only, every run in the cloud. A `sandbox` job in `ci.yml`, by hand (E-024):
  setup.sh, then the fast loop with `-Pdocker`. Windows asks removed from the handoff. The website the
  work aims at is phase 3's web app (E-002).
- **Touched:** E-039; `.github/workflows/ci.yml`; docs/HANDOFF.md.

### J-025 The agent starts runs; the month's minutes are spent
- **Who:** Ghost, with Claude. **Class:** decision.
- **Intent:** Start the `sandbox` job to close 1.5.
- **Found:** The organization's GitHub Actions minutes are spent until November 2026; Ghost also hands the
  agent the starting of runs.
- **Options:** start the run now (it would fail or bill); wait for the minutes; give the session a Docker
  daemon instead.
- **Resolved:** E-040 (supersedes E-024): the agent starts runs, by hand, within the minutes. No run now;
  the handoff says 1.5 waits on the renewal, or on Ghost allowing a Docker daemon in the session.
- **Touched:** E-024, E-040; `.github/workflows/ci.yml`; docs/HANDOFF.md.

### J-026 TODO 1.5 done: the first real sandbox runs, and one bug only they could find
- **Who:** Ghost, with Claude. **Class:** failure-caught.
- **Intent:** Close 1.5 with a real run, in this session, now that Ghost allowed a Docker daemon here.
- **Found:** The daemon started (Docker 29.8.2; cgroup v1, no seccomp or AppArmor in this container).
  Docker Hub refused the base images (429); the same digests came from `mirror.gcr.io`. The kernel's image
  built for the first time anywhere, and its fast loop passed offline, tree-sitter's native library
  loading in it. The sandbox build could not reach the internet from a build container through the
  session's proxy, so the image now builds offline from pieces prefetched and checked (E-041). The real
  run then failed: as root, the scorer gave the sandbox nobody's user, and nobody could not read the
  root-owned `700` working copy or write the output. No fake or recorded run could have shown it.
- **Options:** run the sandbox as root (never); open the trees to everyone; hand them to nobody.
- **Resolved:** `DockerSandbox.handOver` gives the trees to nobody when the scorer runs as root, re-owning
  a planted link itself and never its target (tested). `gradle test -Pdocker`: 102 tests, the fixtures
  giving the recorded facts. 1.5 to docs/DONE.md; the install phase through the proxy stays on paper.
- **Caught by:** `TestRunnerFixturesTest.theRealSandboxGivesTheSameFacts`, on its first real run.
- **Touched:** E-041; `sandbox/`; `analyze/evaluator/analyze/DockerSandbox.java`; `.gitignore`;
  TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-027 TODO 1.6 done: the history's shape, read by a git that runs nothing
- **Who:** Claude. **Class:** decision.
- **Intent:** Provenance layer 3: the shape of a history, and a replay told from weeks of work.
- **Found:** Every value of a five-commit history worked by hand held on the first run (shape 70); a
  replay of 300 commits in an hour faking every signal but time scored 23; six weeks of varied work
  over 70. A repository's own `.git/config` can make git run commands: plain `git status` ran a planted
  fsmonitor (checked by hand); the scorer only calls `log` and `ls-tree`, which never touch it, so the
  hostile-config test now also runs `status` through the safe runner, and with the fsmonitor override
  taken out, the test failed, as it must. A textconv driver did not fire under `log -p` on git 2.43;
  its override stays.
- **Options:** for the shape's weights, equal parts, or time first; for identity, read authors here, or
  leave them to layer 2.
- **Resolved:** E-042, time first (0.45), weights for Ghost's review; no author is read (E-014), layer 2's
  push times check the claimed dates. 1.6 to docs/DONE.md; 1.7 next.
- **Caught by:** the mutation by hand of `Git.SAFE` (the test's teeth).
- **Touched:** `history/`; E-042; CLAUDE.md, docs/MODULES.md, docs/modules.json, TODO.md, docs/DONE.md,
  docs/HANDOFF.md.

### J-028 TODO 1.7 built; its live check waits on a token of the product's own
- **Who:** Claude. **Class:** deferred.
- **Intent:** Layer 2: signed, pushed and foreign commits told apart through GitHub's API, on Suite Builder
  (Ghost: "check against suite builder. Read only").
- **Found:** Suite Builder's 711 commits hold every kind: 8 pull-request merges GitHub signed, 435 of
  Ghost's unsigned, 268 by GitHub's `claude` account (`noreply@anthropic.com`). This session reaches
  api.github.com, but authenticated with the session's own credentials, which are not the product's
  and not read only, and the session is to use its GitHub tools rather than call the API; those tools
  return no verification verdict and no push events. `github/` may not import `analyze/`'s JSON reader:
  it moved to `rubric/` first, in a commit of its own. Every value of the recorded answers worked by hand
  held on the first run, the push by another account not counting for Ghost among them.
- **Options:** use the session's credentials for the live check; wait for a token of the product's own.
- **Resolved:** E-043; the live test waits on `EVALUATOR_GITHUB_TOKEN`, and 1.7 stays open. The recorded
  answers say what is real (the commits, their accounts) and what is not (verdicts read from git; events
  made for the test). MOD-05 (only `github/` reaches the network) is now a test.
- **Touched:** `github/`; `rubric/evaluator/rubric/Json.java`; E-043, MOD-05; `test/ModulesTest.java`;
  CLAUDE.md, docs/MODULES.md, docs/modules.json, docs/HANDOFF.md.

### J-029 TODO 1.8 done: the navigability checklist; this repository scores 99
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.8, while 1.7 waits on its token (Ghost: "yes, go on to 1.8").
- **Found:** Every value of a repository worked by hand held on the first run (70: a stale path, one out of
  the repository, two modules in a cycle, one camelCase name among snake_case ones). This repository:
  CLAUDE.md and AGENTS.md, 62 of 64 named paths present (the two missing are the planned `score/` and
  `agent-runner/`), no cycle among its 9 modules, 766 of 770 names in style: 99.
- **Options:** weigh the four parts equally; weigh freshness most.
- **Resolved:** E-044, freshness 0.35, for Ghost's review. 1.8 to docs/DONE.md; 1.9 next, 1.7 still open.
- **Touched:** `analyze/` (`Navigability`, `DocPaths`, `Naming`); E-044; CLAUDE.md, docs/MODULES.md,
  docs/modules.json, TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-030 TODO 1.9, first slice: facts into a card, as JSON and as Markdown
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.9, sliced in three (Ghost: "yes"): the card's machinery; the formulas, one decision
  each; the CLI, the fixtures' goldens and the corpus.
- **Found:** Every card test held on the first run. Writing the ratios as DESIGN.md words them shows they
  favour small, simple repositories; flagged in E-045 rather than changed.
- **Options:** score every metric with a placeholder formula now; score only what is decided.
- **Resolved:** E-045: only AI navigability scores; the rest is "not measured" until its formula is decided.
- **Touched:** `score/` (`Cards`, `Formula`, `Formulas`, `CardJson`, `CardMarkdown`); E-045; docs/modules.json,
  docs/MODULES.md, docs/HANDOFF.md.

### J-031 TODO 1.9, second slice: Provenance's formula
- **Who:** Claude. **Class:** decision.
- **Intent:** The first formula after navigability (Ghost: "lets go with the provenance algo, then settle the ratios").
- **Found:** On history alone (no token in this session), this repository reads 67 and Suite Builder 73: both
  Partial, as the cap means. A worked card (shape 70, 8 of 10 commits backed) reads 74.
- **Options:** the shape alone; GitHub's share alone; the two blended, the shape capped without GitHub.
- **Resolved:** E-046. Next: the ratios' question (E-045), then the other metrics' formulas.
- **Touched:** `score/evaluator/score/Formulas.java`; `test/evaluator/score/CardsTest.java`; E-046; docs/HANDOFF.md.

### J-032 The ratios settled: a size band for Quality per lines; Maintainability at Complexity
- **Who:** Claude. **Class:** decision.
- **Intent:** The ratios' question E-045 raised (Ghost: "go with both recommendations").
- **Found:** A floor on lines does not stop a per-line ratio ranking toys first (a 200-line toy at Quality 90
  still reads 45 to a clean 6,000-line project's 13 with a 2,000-line floor); only ranking within a band does.
- **Options:** keep both as worded; a floor on lines; a size band; the product in place of the quotient.
- **Resolved:** E-047, superseding E-011's and E-045's ratios in part; SC-10, held from TODO.md 2.6; DESIGN.md's
  "three efficiency ratios" made two.
- **Touched:** `rubric/` (`Ratios`); `score/` (`Cards`, `CardJson`, `CardMarkdown`); E-011, E-045, E-047;
  docs/SCORE-CARD.md, docs/DESIGN.md, docs/GLOSSARY.md, TODO.md, docs/HANDOFF.md.

### J-033 TODO 1.9, second slice: Size's formula
- **Who:** Claude. **Class:** decision.
- **Intent:** The next formula (Ghost: "go with the size log scale").
- **Found:** This repository reads 64 (8,160 lines, the 2k to 10k band), Suite Builder 96 (74,802 lines, 50k and over).
- **Options:** lines on a linear scale with a cap; on a log scale; files or folders in place of lines.
- **Resolved:** E-048. AI navigability, Provenance and Size now score; Complexity, Maintainability and
  Quality's measured half are next, then the tooling bucket's rule.
- **Touched:** `score/evaluator/score/Formulas.java`; `test/evaluator/score/CardsTest.java`; E-048; docs/HANDOFF.md.

### J-034 Java's same-package classes are dependencies; this repository's depth reads 8, not 2
- **Who:** Claude. **Class:** finding.
- **Intent:** Measuring Complexity's parts for its formula showed Java's dependency depth reading low
  (this repository 2, Suite Builder's TypeScript 27); Ghost: "go with the proposal, fix java depth first".
- **Found:** Java needs no import for its own package, so no edge was drawn; a class of `analyze/` naming
  another reached it by no line. With the package as one more import, this repository reads 8.
- **Options:** resolve every capitalised name against the whole repository; only against the file's own folder.
- **Resolved:** E-049, the file's own folder: a same-package class is always beside it. Next: Complexity's formula.
- **Touched:** `analyze/` (`JavaImports`, `JavaAdapter`, `Import`); the imports fixture, its README, its golden;
  `DependenciesTest`, `CouplingTest`; E-049.

### J-035 TODO 1.9, second slice: Complexity's formula
- **Who:** Claude. **Class:** decision.
- **Intent:** Complexity's formula, after E-049 (Ghost: "go with the proposal, fix java depth first").
- **Found:** This repository reads 37 (depth 8, cognitive about 1,700), Suite Builder 81 (depth 27, about
  11,000). A score behind thousands of function facts buried the Markdown table, so it names three and counts
  the rest; the JSON keeps all. "Not measured (ranked among …)" read wrong: the band shows only beside a ratio.
- **Options:** per-function mean; total; total and depth.
- **Resolved:** E-050. AI navigability, Provenance, Size and Complexity score; Maintainability and Quality's
  measured half are next, then the tooling bucket.
- **Touched:** `score/` (`Formulas`, `CardMarkdown`); `test/evaluator/score/CardsTest.java`; E-050; docs/HANDOFF.md.
