# Journal: every event in the work, as a record a researcher can count

One entry per event: a contradiction found, a decision taken, a failure caught or missed, a change of
direction, a measurement. Not a restatement of the commit log (`git log` has what changed) and not
a status list (`DONE.md`, `TODO.md`) and not the design (`theGhost.md`): the journal records *what
happened while the work was done*. Fixed fields, so entries can be counted and compared across
projects (Katana keeps the same journal under its R-046, Suite Builder under G-162). Newest last.
Written by whoever was there, in the same commit as the event when possible; an agent writes its own
entries (E-008). `test/JournalTest.java` holds it: ids rise, every entry has its Who and its Class.
To write one, read only the end (`tail -30 docs/JOURNAL.md`): the next id is one past the last `### J-`.

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

### J-036 TODO 1.9, second slice: Maintainability's formula
- **Who:** Claude. **Class:** decision.
- **Intent:** Maintainability's formula (Ghost: "go with those weights").
- **Found:** This repository reads 84, Suite Builder 88, as estimated; Maintainability at Complexity reads
  31.08 and 71.28. The functions part reads Complexity's facts, so a formula now sees the whole card.
- **Options:** the six parts as proposed; tests weighed heavier.
- **Resolved:** E-051. Five of the seven score in phase 1's way; Quality's measured half is next (its facts
  come from the sandbox's run), then the tooling bucket, then the CLI and the corpus.
- **Touched:** `score/` (`Maintainability`, `Cards`, `Formula`, `Formulas`); `test/evaluator/score/MaintainabilityTest.java`;
  E-051; docs/MODULES.md, docs/modules.json, CLAUDE.md, docs/HANDOFF.md.

### J-037 TODO 1.9, second slice: Quality's formula, and no test at all reads 0
- **Who:** Claude. **Class:** decision.
- **Intent:** Quality's tools' half (Ghost: "go with the formula and the no-tests rule").
- **Found:** As the facts stood, a repository with no tests was "not measured" on Quality while failing
  tests read low: having none paid. The test-to-code ratio of 0 is the fact that closes it.
- **Options:** pass rate alone; pass rate and coverage; no tests not measured, or 0.
- **Resolved:** E-052. Every phase-1 metric has its formula (Ambition waits on its judge, phase 2). Next in
  1.9: the tooling bucket's rule, then `evaluator score <path>`, the fixtures' goldens and the corpus.
- **Touched:** `score/evaluator/score/Formulas.java`; `test/evaluator/score/CardsTest.java`; E-052; docs/HANDOFF.md.

### J-038 TODO 1.9, second slice done: the tooling bucket
- **Who:** Claude. **Class:** decision.
- **Intent:** The bucket's rule (Ghost: "go with the bucket rule"), the last of the second slice.
- **Found:** The history's facts said an agent had a hand, not which: `history/` now names each tool, and
  `Shape`'s own list of agent names gave way to the new one so the two cannot drift. Both repositories read Claude.
- **Options:** the most evidence wins; more than half wins, else mixed; small tools merged into "other".
- **Resolved:** E-053. Next in 1.9, the third slice: `evaluator score <path>`, the fixtures' goldens, the corpus.
- **Touched:** `history/` (`AgentTools`, `Shape`); `score/` (`Buckets`, `Cards`); their tests; E-053;
  docs/SCORE-CARD.md, docs/modules.json, docs/MODULES.md, docs/HANDOFF.md.

### J-039 `evaluator score` runs end to end; every fixture's card is a golden; two gaps in the test runs
- **Who:** Claude. **Class:** finding.
- **Intent:** TODO.md 1.9's third slice, first half (Ghost: "yes, start on the command and goldens").
- **Found:** The command scores this repository in 28 s with its tests run in the sandbox. Ten fixtures'
  cards, JSON and Markdown, are goldens; the recorded runs give Quality 100, 73 and 92 as worked by hand,
  and the network run "not measured". A test caught `--github ../x` passing as owner/repo; the form is now
  GitHub's own. Two gaps: this repository's Quality read 0, because pytest ran the Python fixture
  repositories under `test/fixtures/` as its tests (the failing and network ones among them, and colliding
  module names, every case an error); and its real tests, a Gradle build, are not run by any plan, with no
  TODO item for it.
- **Options:** for Ghost: a fixture folder's tests are data, not tests; a run where no test body ran is "not
  measured", not 0; a Gradle plan.
- **Resolved:** E-054; the two gaps put to Ghost. The corpus (the slice's second half) waits on them.
- **Touched:** `cli/` (`Main`, `Pipeline`); `score/evaluator/score/Formulas.java`; `test/evaluator/cli/`,
  `test/evaluator/analyze/RecordedSandbox.java`, `TestRunnerFixturesTest`; `test/goldens/cards/`; E-054;
  CLAUDE.md, docs/MODULES.md, docs/modules.json, docs/HANDOFF.md.

### J-040 Judge trial 1: Haiku ×3 averaged tracks Opus closest on this repository; Sonnet steady but offset
- **Who:** Claude. **Class:** finding.
- **Intent:** Ghost: rules for which test files are data fit the data curve to Suite Builder; rely on agents for
  judgment, and find the cheapest model that does Opus's job of code evaluation reliably. Three Haiku, three
  Sonnet and one Opus judged this repository blind to the kernel's scores.
- **Found:** docs/judges/trial-1.md. Every model told the fixture folders from the real tests. Haiku's mean of
  three came within 2.9 points of Opus (Sonnet's 5.9); Sonnet's runs agreed within 3 points but read Ambition,
  Provenance and Complexity about 10 higher than Opus. One Haiku answer was not valid JSON. One Opus run cannot
  separate Opus's own noise, and one repository cannot separate a model's bias from the repository's.
- **Options:** Haiku ×3 averaged, flagging spreads over 15 to Opus; Sonnet once, its offset calibrated; Opus.
- **Resolved:** none yet: for Ghost. The fixture question (J-039) goes to the judges, not to a rule.
- **Touched:** docs/judges/trial-1.md.

### J-041 Judge trial 2: every model ranks like Opus; Haiku three times comes nearest Opus's scores
- **Who:** Claude. **Class:** finding.
- **Intent:** Ghost: "attach them and run the trial with opus x3", after trial 1 (J-040). Katana attached read
  only; Suite Builder judged from a copy of `origin/main`. Flight Recorder is not started (Ghost), so it is
  struck from TODO.md 1.9 and DESIGN.md; theGhost.md's mention stays, the log being append-only.
- **Found:** docs/judges/trial-2.md. Opus agrees with itself within about 2 points; one cheaper run is 3.6–5.8
  off; Haiku's mean of three 2.1–4.0, at about a fourteenth of one Opus run. Ranking the three repositories,
  one run of any model puts pairs in Opus's order 90–93% of the time. Haiku's Provenance bias came from
  miscounting history the kernel already counts. 7 of 9 Opus answers were malformed JSON. An Opus judge found
  a crash in `--date` (fixed, f0ea3be) and Suite Builder's recorded address-guard bypass (reported).
- **Options:** Opus once; Sonnet once with its offset corrected; Haiku three times averaged, Opus on a split.
- **Resolved:** none yet: the judge's design is for Ghost (docs/HANDOFF.md). The tithe (30591ab) came a commit
  late; the cadence is back.
- **Touched:** docs/judges/ (trial-2.md, both trials' score tables), TODO.md, docs/DESIGN.md, docs/HANDOFF.md.

### J-042 The seed corpus: three cards, and what Katana's says about what is one's own code
- **Who:** Claude. **Class:** finding.
- **Intent:** TODO.md 1.9's last part (Ghost: "go with the haiku x3 recommendation, then the corpus"); E-055
  recorded first.
- **Found:** `corpus/`: this repository, Suite Builder and Katana, scored with `--no-run` (no test run is
  possible in this container: npm needs the unreachable registries, the two Java ones are Gradle builds).
  Suite Builder's card sits near its judges'. Katana's does not: about 19k lines of another tool's generated
  reference Java and 8.7k lines of COBOL test programs count as its source (Size 92, duplication 24%,
  Maintainability 56), and its real `sir/build/` package is set aside as a `build/` folder.
- **Options:** new set-aside rules for prototypes and fixture languages; leave it to the judges (E-055).
- **Resolved:** the cards as the kernel reads them, the differences written beside them for Ghost's sanity
  read (`corpus/README.md`); no rule fitted to Katana. 1.9 waits on that read.
- **Touched:** `corpus/` (three cards, JSON and Markdown, and its README); docs/HANDOFF.md.

### J-043 Judge trial 3: Fable's reference scores for six repositories, and where the scale breaks
- **Who:** Claude. **Class:** finding.
- **Intent:** Ghost: Katana is in theory harder than Suite Builder; the kernel's Katana card shows the Haiku
  judges need other instructions; first an authoritative scoring by Fable of six repositories, three new
  (Neon Rain, the Jensen migration, a suite Suite Builder generated).
- **Found:** docs/judges/trial-3-fable.md. Fable ranks Katana first (complexity 92, ambition 92), Suite Builder
  second, as Ghost read it. Its folder classification sets aside what the kernel counts: Katana's 20k lines of
  GPL reference output, Suite Builder's 195k lines of goldens and saved crawls, the atos suite's 84k generated
  lines (its author's own work: 3 lines). The earlier judges sat within 1–4 points of Fable on the three easy
  repositories; the generated suite and the plan-only Neon Rain are where a quick reader and the kernel fail.
- **Options:** for Ghost: the seventh Fable's five doubts about the scale (generated and plan-only repositories
  printed with no scores; ambition posed vs attempted; navigability above a size floor; one provenance rule).
- **Resolved:** the six reports and their pitfalls kept as the Haiku experiment's reference
  (`docs/judges/fable-pitfalls.md`); the scale's doubts put to Ghost.
- **Touched:** docs/judges/ (trial-3-fable.md, fable-pitfalls.md, trial-3-fable-scores.tsv).

### J-044 E-056: only AI-assisted work is ranked, with three outcomes
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: purely human code is rejected, since the candidate is ranked on AI code structuring
  proficiency and traditional review suits a non-AI developer better.
- **Found:** Proving the absence of AI is unreliable (trailers are stripped, inline completion leaves no
  trace), so a two-way gate would decline real AI-assisted developers; a tool's wholesale output (the atos
  suite, trial 3) is AI-made but not the applicant's structuring.
- **Options:** two outcomes (evidence admits, none declines); three (none asks, then a judge decides).
- **Resolved:** E-056, three outcomes (Ghost: "record it with the three outcomes"); SC-11 held from TODO.md 3.3;
  DESIGN.md's pitch says it. E-012 stands: who built it remains a score.
- **Touched:** theGhost.md, docs/SCORE-CARD.md, docs/DESIGN.md, TODO.md.

### J-045 A pre-push failure pushed anyway, and a fast loop that skipped the doc tests
- **Who:** Claude. **Class:** finding.
- **Intent:** E-056's commit (9d22e80).
- **Found:** SC-11 broke ScoreCardDocTest's own self-check (its "accepts" sample hard-coded the open steps the
  document then cited, 1.9, 2.6 and 3.5; 3.3 was new). Two process faults let it out: `gradle test` reported a
  pass without running, since a docs-only change touches none of the task's inputs; and the push was chained
  after `scripts/prepush.sh` with `;`, so it went through though the check failed. Main carried a red commit for
  minutes.
- **Options:** declare docs, fixtures and goldens as the test task's inputs; never count the task up to date.
- **Resolved:** the sample now adds its step to the real open steps; the test task always runs (the loop is
  seconds); the agent pushes only on `scripts/prepush.sh && git push`.
- **Touched:** test/ScoreCardDocTest.java, build.gradle.

### J-046 The maintainer is Ghost everywhere, the byline included
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: the repositories are all authored under the pen name Ghost in the Kernel (Ghost in
  sessions); a real name goes in no file.
- **Found:** `docs/DESIGN.md`'s byline and two calibration lines carried a real name, and the glossary listed
  it as the alias to avoid, which put it in a fourth place.
- **Options:** keep the name as a forbidden alias so a test catches it; take it out everywhere.
- **Resolved:** out everywhere, the glossary's alias list included; the rule stays CLAUDE.md's "Ghost" in every
  file. Earlier commits still hold it in history; rewriting `main` is Ghost's call, not done here.
- **Touched:** docs/DESIGN.md, docs/GLOSSARY.md, docs/HANDOFF.md; J-045's typo.

### J-047 Haiku round 1: the kind of repository decides more than the instructions
- **Who:** Claude. **Class:** finding.
- **Intent:** Ghost: find the context that makes Haiku sane across codebases, weighing the seventh Fable.
- **Found:** docs/judges/trial-4-haiku-round1.md. 150 runs, 25 sets. Haiku tells generated and plan-only
  repositories every time but scores the generated code anyway (+55 navigability on the atos suite); under
  E-056 such a repository is declined, so the score is never asked. The seventh Fable's insights, as
  instructions, over-discount a plan; as card rules (withhold what a plan cannot have) they would not. On
  built repositories the sets sit 4.2–8.3 from Fable, mostly within one run's noise. Size errs most, from
  counting reference code as the author's.
- **Options:** pick a set now; replicate a shortlist; move Size to the kernel counting the judge's own folders.
- **Resolved:** round 2 to replicate the shortlist with the folder classification in the output.
- **Touched:** docs/judges/ (trial-4-haiku-round1.md, trial-4-round1-cards.json).

### J-048 Haiku round 2: the judge classifies folders, the kernel counts them, within 17% of Fable
- **Who:** Claude. **Class:** finding.
- **Intent:** Ghost: "yes, run round 2": the shortlist three times, with the folder classification.
- **Found:** docs/judges/trial-4-haiku-round2.md. Averaged over three runs the sets sit 3.6 (archetype anchors)
  to 5.9 (code first) from Fable on the built repositories, baseline 4.7. Counting only the folders Haiku
  calls own code puts own code within 0.07 decades of Fable's, nearly identical run to run, where the kernel
  alone is 0.24–0.45 off on built repositories and counts 24.7k lines for a generated one. My first counter
  credited a parent folder's subfolders called reference (Katana 40k); counting each file under its most
  specific listed folder fixed it. The kernel has no JavaScript adapter.
- **Options:** for Ghost: the division of labour (judge classifies and judges, kernel counts and applies card
  rules); the instruction set (v22 + v08, v17 once the corpus holds cards).
- **Resolved:** proposed to Ghost; nothing built.
- **Touched:** docs/judges/ (trial-4-haiku-round2.md, trial-4-round2-cards.json).

### J-049 E-057 recorded; the JavaScript adapter is TODO.md 1.10
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "record E-057 and add the javascript adapter".
- **Found:** TODO.md must stay in numeric order and the phase-1 boundary tithe must stay last, so the adapter is
  1.10 and the tithe 1.11; its references in AGENT.md, GHOST-IN-THE-KERNEL.md, HANDOFF.md, MODULES.md,
  modules.json and two decisions' Held-by lines follow it. A JavaScript grammar would be a new dependency, so
  the item tries the vendored TypeScript and TSX grammars first.
- **Options:** the adapter in phase 2; 1.10 with the tithe renumbered.
- **Resolved:** E-057 in force, in TODO.md 2.2's check; 1.10 the JavaScript adapter, 1.11 the tithe pass.
- **Touched:** theGhost.md, TODO.md and the references to 1.10.

### J-050 TODO 1.9 done: the card, the formulas, the command and the seed corpus, closed as phase-1 readings
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "close 1.9 and start the javascript adapter".
- **Found:** The corpus cards did not fully survive Ghost's sanity read (Katana is harder than its card), and
  the cause is phase 1's limit, not a formula's: the kernel cannot tell the author's code from reference or
  fixture code. E-057 settles it once the judge classifies folders.
- **Options:** keep 1.9 open until phase 2 can make the cards; close it with the cards marked as phase-1 readings.
- **Resolved:** closed; the cards stay as phase-1 readings beside Fable's reference, to be made again under E-057.
  Next: 1.10.
- **Touched:** TODO.md, docs/DONE.md, docs/HANDOFF.md, corpus/README.md; SC-09, held from 1.9, now names what
  holds it (`score/`'s CardJson, CardGoldensTest), and ScoreCardDocTest reads citations in `score/` as in `rubric/`,
  since `score/` builds and renders the card.

### J-051 TODO 1.10 done: JavaScript is measured, the Jensen migration's 17 functions become 121
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.10 (Ghost: "close 1.9 and start the javascript adapter").
- **Found:** The vendored TSX grammar reads JavaScript, JSX included, so no grammar was added. Every value worked
  by hand held on the first run: TypeScript's examples without types give TypeScript's numbers; a Pages Functions
  handler 4 and 3; `require` bindings used and unused. One existing card changed as it should: node-pass's
  JavaScript now counts (Complexity 0 to 2).
- **Options:** a tree-sitter JavaScript grammar (a new dependency); the TypeScript grammar (no JSX); TSX.
- **Resolved:** E-058, TSX; 1.10 to docs/DONE.md; next 1.11.
- **Touched:** `analyze/` (`JavaScriptAdapter`, `JavaScriptImports`, `TypeScriptAdapter`, `TypeScriptImports`,
  `Complexity`); the fixture, its README and golden; node-pass's card goldens; E-058; TODO.md, DONE.md, HANDOFF.md.

### J-052 TODO 1.11, slice 1: docs/AGENT.md's metrics held by a test; pass 2 recorded
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.11 in slices (Ghost: "start 1.11 with slices 1-3"); this one a tithe.
- **Found:** Every target held at pass 2 (context 1,067 words, largest file 280 lines, routing row 5 files,
  no function over 20 paths, no duplicate helper, nothing stale); the fast loop 18 s with every test rerun.
  The cadence was 13 commits: the second slip (J-045's habit, J-051).
- **Options:** hold the fast loop and the cadence in the test too; hold them where they can be measured.
- **Resolved:** test/MetricsTest.java holds every target in the table and that each named metric is measured,
  proving itself on four broken copies; the fast loop is timed at tithe passes, the cadence at push (slice 3).
- **Touched:** test/MetricsTest.java, test/DocTests.java, docs/AGENT.md.

### J-053 TODO 1.11, slice 2: docs/MODULES.md written from docs/modules.json
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.11 in slices (Ghost: "start 1.11 with slices 1-3"); this one a tithe.
- **Found:** The hand-written file had drifted from the JSON in small ways: purposes shortened, `cli/` and
  `rubric/` without inputs and outputs, no "Imported by" for most modules, the rules not listed. The
  "Start at" prose had no home in the JSON.
- **Options:** keep it by hand with a test comparing fields; generate it, the prose moved into the JSON.
- **Resolved:** generated. modules.json gains `pipeline` and each module's `startAt`; scripts/ModulesDoc.java
  writes the file (`gradle modulesDoc`), "Imported by" read off the other modules' mayImport and the MOD rules
  listed; ModulesTest fails on a stale or hand-edited file, and `gradle metrics` counts it as stale.
- **Touched:** scripts/ModulesDoc.java, scripts/Metrics.java, build.gradle, docs/modules.json, docs/MODULES.md,
  test/ModulesTest.java, CLAUDE.md.

### J-054 TODO 1.11, slice 3: the tithe gate refuses a push past sixteen commits
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.11 in slices (Ghost: "start 1.11 with slices 1-3"); the last of the three.
- **Found:** The cadence was the one rule held by habit, and it slipped twice (J-045 at 9, J-051 at 13).
- **Options:** a warning only (what `gradle metrics` already gives); refuse at eight; refuse at sixteen.
- **Resolved:** sixteen, as TODO.md 1.11 says: eight stays the target and the warning, sixteen the hard stop.
  scripts/TitheGate.java counts subjects since the last "Tithe"; `gradle titheGate` runs it in the pre-push, on
  the fresh clone in the image; Metrics.cadence reads the same count; MetricsTest proves it on four histories.
  Slice 4 (the mutation tool) waits on Ghost's approval of PIT as a dependency.
- **Touched:** scripts/TitheGate.java, scripts/Metrics.java, scripts/prepush.sh, build.gradle,
  test/MetricsTest.java, CLAUDE.md, docs/HANDOFF.md.

### J-055 TODO 1.11 done: PIT vendored, the first mutation run, phase 1 closed but for 1.7
- **Who:** Claude. **Class:** decision.
- **Intent:** TODO.md 1.11, slice 4 (Ghost: "yes, vendor PIT and run slice 4").
- **Found:** PIT 1.30.1 needs eleven jars; its command-line jar bundles StringTemplate, ANTLR 2 and jopt-simple.
  Every jar matched Maven Central's SHA-1; all Apache-2.0, BSD or MIT. The first run mutated the tests too
  (they share the kernel's packages): 72 % of 2,316. With every class from `test/` excluded, 86 % of 1,714.
- **Options:** work all 243 survivors now; record the run as the baseline and work them as a pass of their own.
- **Resolved:** the baseline, in docs/MUTATION.md; E-059 records the dependency, E-019 is in force; 1.11 to
  docs/DONE.md. The survivors' pass is put to Ghost (docs/HANDOFF.md).
- **Touched:** third_party/pitest/, third_party/MANIFEST.md, build.gradle (`mutate`), docs/MUTATION.md, E-019,
  E-059, TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-056 TODO 1.12 started: rubric/Json.java's mutants, and three escapes read wrong
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "add the survivor pass to TODO and start with Json.java".
- **Found:** `rubric/` had no test of its own JSON reader; the other modules' tests reached it only through what
  they read. Working the values by hand found `\r`, `\b` and `\f` undone as letters: a report holding them
  would have been read wrong. Two of my own hand values were off (an offset, one nesting level), caught by the
  first run and corrected before the code was blamed.
- **Options:** for the two empty-container returns, a test that writes to a parsed value (no caller does);
  judge them harmless.
- **Resolved:** test/evaluator/rubric/JsonTest.java; the escapes fixed; two redundant `space()` calls removed;
  65 of 67 killed, two judged harmless in docs/MUTATION.md (run 2). TODO.md 1.12 added after 1.7.
- **Touched:** rubric/evaluator/rubric/Json.java, test/evaluator/rubric/JsonTest.java, docs/MUTATION.md,
  TODO.md, docs/HANDOFF.md.

### J-057 TODO 1.12: rubric/ closed, four edges given tests
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "go on with the rest of rubric".
- **Found:** Four survivors outside Json, none dead code: two boundaries of a calibration's precision never
  tried, a kind starting with a dot refused only by the later check, and SC-07's two messages never read.
- **Options:** none worth weighing: each wanted one assertion.
- **Resolved:** tests in test/evaluator/rubric/FactTest.java and ScoreCardTest.java; 130 of 132 killed, the two
  left Json's harmless pair (run 3). Next in 1.12: score/.
- **Touched:** test/evaluator/rubric/FactTest.java, ScoreCardTest.java, docs/MUTATION.md, docs/HANDOFF.md.

### J-058 TODO 1.12: score/ closed, three edges given tests
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "go on with score".
- **Found:** Four left of 155: values on Maintainability's two "at most" limits never tried; a rank and a review
  never rendered (phase 1 fills neither); the rubric's hash held only from `cli/`; and `logScale`'s guard at
  100, an equivalent mutant (both sides give 0).
- **Options:** for the hash, compute SHA-256 again in the test (the code twice) or name the value (a rubric
  change rewrites it, as it does every golden): the value.
- **Resolved:** tests in MaintainabilityTest and CardsTest; 154 of 155 killed, the guard judged harmless (run 4).
  Next in 1.12: history/.
- **Touched:** test/evaluator/score/MaintainabilityTest.java, CardsTest.java, docs/MUTATION.md, docs/HANDOFF.md.

### J-059 A Sonnet agent escalates once to Opus (E-060)
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: a Sonnet agent tries a hard fix once and then invokes Opus, so Sonnet can work here.
- **Found:** CLAUDE.md is the one file every agent reads, so the rule goes there; 88 words, CLAUDE.md at 1,218 of
  AGT-03's 1,500.
- **Options:** a rule in CLAUDE.md; a skill the agent loads; a rule in docs/AGENT.md (read only when routed).
- **Resolved:** CLAUDE.md, "A Sonnet agent escalates once"; E-060. "Once" is read as: a plain fix is made; a
  failure that does not say why gets one more try at most, then Opus; if Opus fails too, Ghost.
- **Touched:** CLAUDE.md, theGhost.md.

### J-060 Ghost's two calls on the Fable advisor's pass: one copy of the supply-chain rules, a map that closes itself
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "yes to gradle kernel map"; the supply-chain block moved to theGhost.md "provided agents
  read the ghost before doing tasks".
- **Found:** theGhost.md is about 11,000 words, so reading all of it before a task would cost more than the
  block saves. Its first section can be the block: about 200 words, read first.
- **Options:** read all of theGhost.md up front; read its first section, made the block.
- **Resolved:** E-061: the block is theGhost.md's first section and its one copy; CLAUDE.md's first section
  sends every agent there; the metric counts both (1,290 words, CLAUDE.md 1,090). E-062: `gradle kernelMap`
  closes a gone function itself, `superseded-by` when it moved. E-033's directive clause superseded in part.
- **Touched:** theGhost.md, CLAUDE.md, test/SupplyChainTest.java, scripts/Metrics.java, scripts/KernelMap.java,
  test/KernelMapTest.java, docs/KERNEL-MAP.md.

### J-061 Navigability pass, slice 1: the docs that misled
- **Who:** Claude. **Class:** contradiction.
- **Intent:** The Fable advisor's pass (Ghost: "the plan is approved for the four slices").
- **Found:** docs/SCORE-CARD.md gave `HISTORY.spread_days` as a fact kind, which `Metric.ofKind` refuses, and
  listed `QUALITY.lint` and `MAINTAINABILITY.refactors` under modules that make neither: an agent adding a
  fact would copy the example and fail. Eight stale lines besides (a closed step cited as open, a doc test
  named as future). docs/HANDOFF.md, the first file of the first routing row, was 791 words, most of it
  Ghost's items and one past session's Docker state.
- **Options:** fix the words only; fix them and hold the kind table to the code.
- **Resolved:** the table fixed and held: ScoreCardDocTest's `kinds` checks every example kind against
  `Metric.ofKind` and each module's row against the kinds its source names, proven on three broken copies.
  Stale lines fixed. HANDOFF.md cut to 226 words, Ghost's items moved to docs/FOR-GHOST.md (no row routes
  there); the first routing row reads a decision by number. JOURNAL.md says how to find the next id.
- **Caught by:** the Fable advisor (read only, 2026-10-23).
- **Touched:** docs/SCORE-CARD.md, test/ScoreCardDocTest.java, docs/HANDOFF.md, docs/FOR-GHOST.md, docs/JOURNAL.md,
  docs/GHOST-IN-THE-KERNEL.md, CLAUDE.md, analyze/evaluator/analyze/TestPlans.java and its test,
  history/evaluator/history/Commits.java.

### J-062 Navigability pass, slice 2: rows as pointers, instructions in docs/how/, a decision index
- **Who:** Claude. **Class:** decision.
- **Intent:** The Fable advisor's pass; Ghost: "lets start thinking about the instructions as pointers".
- **Found:** Adding a language touched ten files and a row may name five, so `Languages.java` (without it
  nothing is measured) was named nowhere. Finding a decision meant theGhost.md, about 11,000 words. The new
  path check caught eight bare file names and placeholder paths in the first instructions file.
- **Options:** one recipes page; one instructions file per action, its row a pointer (Ghost's).
- **Resolved:** E-063. docs/how/ holds add-a-language-adapter.md, add-a-fact.md and change-a-formula.md,
  each row in CLAUDE.md naming its file; DocsTest holds every path in them. docs/DECISIONS.md, one line per
  decision (1,405 words), written by `gradle decisionsDoc` and held by GhostTest. CLAUDE.md 1,114 words
  (from 1,218 this morning): Commands rewritten with the one-module `gradle mutate` first, the Docker command
  moved to AGT-01, a duplicated line cut; the history row names AgentTools.java in place of Git.java.
- **Touched:** CLAUDE.md, docs/how/, docs/DECISIONS.md, scripts/DecisionsDoc.java, scripts/Metrics.java,
  build.gradle, test/DocsTest.java, test/GhostTest.java, docs/AGENT.md, .gitattributes, theGhost.md (E-063).

### J-063 Ghost's directive: an escalation replays its route, is logged, and three of a type go to Ghost
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: Opus asks the Sonnet agent to replay the instructions it followed and validates they
  reached it at the start; the issue is logged; three of a type are raised for review and route updates.
- **Found:** "Three of the same type" needs types an agent names alike every time, and a raise nobody forgets.
- **Options:** free-text issues counted by eye; five fixed types with a test that refuses an unraised third.
- **Resolved:** E-064. docs/how/escalate-to-opus.md (both sides' steps, the five types), docs/ESCALATIONS.md
  (the log), test/EscalationsTest.java (a type outside the five, an unraised third, a raise not named in
  docs/FOR-GHOST.md all fail; Ghost's `reviewed:` resets the count). CLAUDE.md's rule points to the file.
- **Touched:** CLAUDE.md, docs/how/escalate-to-opus.md, docs/ESCALATIONS.md, test/EscalationsTest.java,
  test/DocTests.java, theGhost.md, docs/DECISIONS.md.

### J-064 Every workpath a pointer: CLAUDE.md's table down to task and instructions
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "the pointer file should include instructions for each general workpath."
- **Found:** Following the pointers, two workpaths opened six files (the instructions file and five more),
  past AGT-04's five; the metric counted only the row until it followed the pointer.
- **Options:** count only the row (the pointer hides the cost); follow it and make room.
- **Resolved:** E-065, superseding E-063: fourteen new files in docs/how/ (seventeen with the three before),
  each with its start files, contract, tests and steps; the table is two columns; reading-only documents
  share one row. The routing metric follows a pointer; the sandbox's and the card's start lists lost one file
  each to their steps. CLAUDE.md 836 words; mandatory context 1,036.
- **Touched:** CLAUDE.md, docs/how/, scripts/Metrics.java, theGhost.md (E-063, E-065), docs/DECISIONS.md.

### J-065 Long runs to a file, their summaries read
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: long outputs (named for Suite Builder's `npm run check` and Stryker) go to a file, and
  sub-agents read only the summary or failures.
- **Found:** Here the long runs are `gradle mutate` (minutes to ten), `scripts/prepush.sh` and a failing
  `gradle test`; PIT's console runs to thousands of lines.
- **Resolved:** CLAUDE.md's Commands and docs/how/agent-environment.md: a long run always goes to a file,
  grepped for failures before anything is read, and nothing more read when there are none (Ghost's
  refinement). Suite Builder not touched (Ghost: "that is ok"). The first wording named build-output paths
  in backticks; DocsTest refused them on the pre-push's clean clone, where build/ does not exist.
- **Touched:** CLAUDE.md, docs/how/agent-environment.md.

### J-066 Four navigability refactors: Shape's constants, Adapters, Tarjan, Dependencies.TOOL
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** A navigability review: no behaviour change, no golden change (E-018).
- **Found:** Shape's thresholds were bare literals explained only in E-042 and ShapeTest; the adapter registry
  sat in Complexity, which Dependencies reached into; Tarjan was nested in Dependencies and used by Coupling
  and Navigability too; Pipeline alone named a tool for another module's facts.
- **Resolved:** Named constants in Shape; `Adapters` (ALL, forLanguage) out of Complexity; `Tarjan` its own file;
  `Dependencies.TOOL` replaces `Pipeline.DEPENDENCIES_TOOL`, same string. add-a-language-adapter.md step 4
  points at `Adapters.ALL`. `gradle kernelMap` rewritten. MainTest.thisRepositorysCard fails before and after
  in this worktree (Provenance reads "not measured"); not touched.
- **Touched:** history/Shape.java, analyze/ (Adapters, Tarjan, Complexity, Dependencies, Coupling, Navigability),
  cli/Pipeline.java, docs/how/add-a-language-adapter.md, docs/KERNEL-MAP.md.

### J-067 A Sonnet agent's first refactor: slice 3 done unaided; what it found missing
- **Who:** Claude. **Class:** trial.
- **Intent:** Ghost: "spin up a sonnet model, and see if it can handle the refactor for item 3".
- **Found:** Sonnet, entering from CLAUDE.md with a plain brief in its own worktree, made the four refactors
  in one commit (5aaaff1, J-066) in under two minutes and 13 tool calls, no escalation, no golden changed,
  and found two callers of the nested Tarjan the brief did not name. Review found it right. It reported four
  gaps: the fast loop red in a worktree (`.git` is a file there, deliberately not followed); `gradle
  kernelMap` needed before the tests go green, not only before the commit; bare grep flooded; modules.json's
  start points not naming the new classes. Review found one more, mine: E-062 closed the moved Tarjan's
  methods as removed, since their names changed from `Dependencies.Tarjan.f` to `Tarjan.f`.
- **Options:** none weighed: each gap a line where the agent would look.
- **Resolved:** the four gaps written into docs/how/agent-environment.md, Pipeline's comment, docs/modules.json
  (and MODULES.md); KernelMap.sameFunction takes a nested class moved to its own file as a move, with a test.
- **Touched:** scripts/KernelMap.java, test/KernelMapTest.java, cli/evaluator/cli/Pipeline.java,
  docs/how/agent-environment.md, docs/modules.json, docs/MODULES.md.

### J-068 Ghost's directive on agents in conflict
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost, from a reported trial where three agents with conflicting objectives overwrote each
  other, ran kill scripts on each other, and never told the human: "an object lesson for us".
- **Found:** Nothing here said what an agent does when another's work collides with its own; agents here
  start daemons and background runs, and nothing said not to stop another's.
- **Options:** a `conflict` type counted to three like the others; raised at once (Ghost's choice).
- **Resolved:** E-066. CLAUDE.md "When agents conflict" (the rule, every agent reads it);
  docs/how/when-agents-conflict.md (what counts, the steps, the nevers); a sixth escalation type, `conflict`,
  raised on its first row, held by EscalationsTest. Never force-push, with no exception (one branch, E-020).
- **Touched:** CLAUDE.md, docs/how/when-agents-conflict.md, docs/how/escalate-to-opus.md, docs/ESCALATIONS.md,
  test/EscalationsTest.java, theGhost.md, docs/DECISIONS.md.

### J-069 A force-push needs every active agent's agreement
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: no force-push unless all agents actively working on the project agree; check in and wait
  for the slow ones.
- **Found:** E-066 said never; Ghost wants it possible, by agreement.
- **Options:** none weighed: how to check in was the open part.
- **Resolved:** E-067, superseding E-066 in part: find the active agents (ListAgents, Ghost's sessions), check
  in with each, wait for all, slow ones included; any no or no answer means no force-push and Ghost is told;
  alone, allowed and journaled.
- **Touched:** CLAUDE.md, docs/how/when-agents-conflict.md, theGhost.md, docs/DECISIONS.md.

### J-070 Ghost's directive: fresh Sonnet agents, debriefs, a navigation review about every ten commits
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: new tasks use fresh Sonnet agents; each debriefs its navigation complaints and
  suggestions to a file; about every ten commits the file is reviewed for what agents converge on, then wiped;
  open questions go to a Fable instance.
- **Found:** The first debrief already exists in effect: the Sonnet agent's report on slice 3 (J-067), acted
  on in full.
- **Options:** the cadence held by habit; by the gate the tithe uses (warn past ten, refuse past twenty).
- **Resolved:** E-068. docs/NAVIGATION-NOTES.md (empty set), docs/how/debrief-after-a-task.md,
  docs/how/navigation-review.md (convergence means two or more agents; a plain fact gone wrong is fixed alone),
  CLAUDE.md's section and two rows; TitheGate and Metrics count commits since the last "Navigation review".
  This commit is the first review: its one set (J-067) was already acted on.
- **Touched:** CLAUDE.md, docs/NAVIGATION-NOTES.md, docs/how/, scripts/TitheGate.java, scripts/Metrics.java,
  test/MetricsTest.java, theGhost.md, docs/DECISIONS.md.

### J-071 CLAUDE.md as a memory map: situations point to files
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: "If you are a claude sonnet model, and you encounter X situation, look up Y file."
- **Found:** Of CLAUDE.md's 1,017 words, about 370 applied only sometimes: the commands, the finishing list, the
  commit rules, the three directives. Six decisions' Held-by lines name those sections; decisions are
  append-only, so E-069 says where each went rather than editing them.
- **Options:** pointers for all of it; pointers, with the conflict line keeping its nevers inline (taken: an
  agent may stop a process before it sees a conflict).
- **Resolved:** E-069. "When it happens, read this": eight lines. docs/how/commands.md, finish-work.md,
  commit-and-push.md hold the moved text (with the pre-push's gated form from J-045, which CLAUDE.md never
  said). The routing table lost the four rows the map now covers. CLAUDE.md 647 words.
- **Touched:** CLAUDE.md, docs/how/commands.md, docs/how/finish-work.md, docs/how/commit-and-push.md,
  theGhost.md, docs/DECISIONS.md.

### J-072 history/'s mutation survivors closed; a path with a newline was read as two
- **Who:** Claude Sonnet 5.5. **Class:** failure-caught.
- **Intent:** TODO.md 1.12, the `history/` module: a test or a reason for each of run 1's 25 survivors.
- **Found:** A surviving "removed empty-path filter" in `Commits.paths` led to `.lines()` before the NUL split:
  a file name holding a newline was cut in two. The other survivors were untested edges of Shape's parts and
  of git's output cap and environment; three mutants are equal to the code (docs/MUTATION.md).
- **Options:** a harmless row for the filter (wrong: the filter is dead only because of the bug); split on NUL
  alone, taken.
- **Resolved:** `Commits.splitPaths`, `aPathWithANewlineIsOnePath` (fails before); the 26 mutants not killed are now 3, by new
  tests; run 6 recorded; HANDOFF names `cli/` next.
- **Touched:** history/, test/evaluator/history/, docs/MUTATION.md, docs/HANDOFF.md, docs/KERNEL-MAP.md.
- **Caught by:** mutation (E-019).

### J-073 cli/'s mutation survivors closed; this commit is a tithe
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** TODO.md 1.12, the `cli/` module: a test or a reason for each survivor; and the tithe nine commits
  called for (E-015).
- **Found:** Of `cli/`'s 7 mutants not killed, 5 were two things never run: a `.git` that is no repository
  (so `Main.score`'s IOException line) and the two notes `Pipeline.facts` prints; the GitHub line could not be
  reached without a network until `Pipeline.github` took the API. One, `System.exit`, cannot be tested. Also:
  PIT needs a green suite, which a worktree never has (`thisRepositorysCard`), so the run was in a plain clone.
- **Options:** a harmless row for the GitHub line (rejected: it could change a result); an injected API,
  taken.
- **Resolved:** run 7 (45 mutants, 44 killed); PipelineTest and one MainTest test; the tithe fills the gaps two
  earlier debriefs named (agent-environment.md, finish-work.md, add-or-change-a-module.md), puts a comment on the
  two helpers Metrics flagged, and records Pass 3 in AGENT.md.
- **Touched:** cli/, test/evaluator/cli/, docs/MUTATION.md, docs/HANDOFF.md, docs/AGENT.md, docs/how/.
- **Caught by:** mutation (E-019).

### J-074 A Sonnet agent's "broken history" was a shallow clone
- **Who:** Claude. **Class:** failure-caught.
- **Intent:** Reviewing the cli/ survivors (525a109, J-073, by a Sonnet agent).
- **Found:** The agent reported commit 2c28030 missing and `git log` failing in a clone. The session's checkout
  was shallow (cut at 82fd968); a clone made from it lost the cut-off, so git saw a missing parent. GitHub has
  the commit; `git fetch --unshallow` gave 90 commits and a clean `git fsck`. Its Pass 3 column also left the fast
  loop untimed for want of Docker, which the timing does not need: 18 s.
- **Options:** none weighed.
- **Resolved:** the main checkout unshallowed; docs/how/agent-environment.md says how to tell a shallow checkout
  and fix it; the Pass 3 fast loop timed. The agent's other work held on review: six mutants killed by tests,
  one harmless (`System.exit` in `main`), `Pipeline.github` taking its API as an argument, no golden moved.
- **Caught by:** review of the agent's report.
- **Touched:** docs/how/agent-environment.md, docs/AGENT.md.

### J-075 Navigation review: three debriefs, all converged points already fixed
- **Who:** Claude (Sonnet 5.5). **Class:** measurement.
- **Intent:** The navigation review that `gradle metrics` called due (docs/how/navigation-review.md).
- **Found:** Three debriefs (Json.quote/TestRunner/CardsTest refactors; history/ survivors; cli/ survivors). Two points
  were met by two agents separately. (1) Running `gradle mutate` on a fresh checkout or worktree (no `build/`,
  PIT refusing a red suite): valid when written, already fixed by 525a109 and 6590a3f in agent-environment.md
  (plain clone, `mkdir -p build`, a survivors one-liner, the shallow-clone fix). (2) Mutation bookkeeping gaps
  (a moved function's test must sit in its module; MUTATION.md runs numbered in order): already fixed in
  finish-work.md and agent-environment.md. One agent alone: "Start here" lists hold five files (already in
  add-or-change-a-module.md); the missing parent object (a shallow clone, J-074); Json.java being an existing
  class (read-a-repository.md says nothing) and a tithe's meaning beside E-015 in commit-and-push.md (the
  subject rule is there, the meaning is not): noted, not acted on, being one agent's each and no fact gone wrong.
- **Options:** fix the two single-agent gaps now; leave them until a second agent meets them (step 2). The second.
- **Resolved:** no change to instructions; no open question, so no Fable agent asked. Notes wiped.
- **Caught by:** the navigation review.
- **Touched:** docs/NAVIGATION-NOTES.md, docs/JOURNAL.md.

### J-076 github/'s mutation survivors closed; the "real GitHub only" lines were reachable
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** TODO.md 1.12, the `github/` module: a test or a reason for each survivor.
- **Found:** Of 17 mutants not killed, 11 were in `HttpGitHub`, which run 1 expected to need the real GitHub. It
  does not: `get` is plain HTTP to a host, and a loopback server answers it. The host was a constant, so it took a
  field. The other 6 were edges in `Authorship` (page caps, a push that lists commits, a commit with no date).
- **Options:** harmless rows for the adapter (rejected: a status, a size cap and a redirect rule can change a
  result); a loopback server with the host injected, taken.
- **Resolved:** run 8 (66 mutants, 64 killed); HttpGitHubTest, four tests in AuthorshipTest; two rows of harmless.
  No bug.
- **Touched:** github/, test/evaluator/github/, docs/MUTATION.md, docs/HANDOFF.md, docs/KERNEL-MAP.md.
- **Caught by:** mutation (E-019).

### J-077 analyze/ part 1 of 6 (Java) reviewed; a debrief written into its report, not the file
- **Who:** Claude. **Class:** failure-caught.
- **Intent:** TODO.md 1.12's analyze/, split by concern across six Sonnet agents, one at a time (Ghost).
- **Found:** Part 1 (0f2624e, run 9) killed 24 of the Java classes' 31 survivors with JavaComplexityTest; the 7
  harmless (six visitor methods returning Void, one boundary giving the same directory) hold on review; no bug,
  no golden moved. It wrote no journal entry, reading 1.12 as unfinished, and gave its debrief only in its
  report: docs/NAVIGATION-NOTES.md had no entry.
- **Options:** leave the gap; write the debrief in from its report and watch whether another agent misses it.
- **Resolved:** the debrief appended, marked as written by the reviewer; a second miss makes it a check.
  The scratch clones finished agents left (jclone, plain, mclone) removed.
- **Caught by:** review (counting the notes' entries).
- **Touched:** docs/NAVIGATION-NOTES.md.

### J-078 analyze/ part 2 of 6 (Python and the tree-sitter base): 29 survivors down to 5, all harmless
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** TODO.md 1.12, `PythonAdapter`, `TreeSitterAdapter` and `PythonImports`: a test or a reason for each survivor.
- **Found:** The Python fixture never reached a nesting-sensitive rule below the top, a bracketed run of `and`, an error
  past line 1, or a relative import above the package; and `TreeSitterAdapter.normalize` had no test of its own
  (relative imports were never resolved in a test). No bug.
- **Options:** extend the fixture (rejected: its golden would move for no behaviour change); source held in a new
  test, taken.
- **Resolved:** run 10 (1,104 mutants, 977 killed); PythonAdapterTest, five rows of harmless.
- **Touched:** test/evaluator/analyze/, docs/MUTATION.md, docs/HANDOFF.md, docs/KERNEL-MAP.md.
- **Caught by:** mutation (E-019).

### J-079 analyze/ part 3 of 6 (TypeScript/JavaScript and the navigability checklist): 23 survivors down to 0
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** TODO.md 1.12, `TypeScriptAdapter`, `JavaScriptImports`, `TypeScriptImports`, `JavaScriptAdapter`, `Naming`, `Navigability` and `DocPaths`: a test or a reason for each survivor.
- **Found:** The TypeScript fixture never put a condition or a block inside an `else if` or an `else`; no test had a constructor, a name of no style, a `var` or an aliased require, an import and a require out of order, `../` against a bare package name, a path made only of container folders, or a doc of exactly the size cap. One of my own hand values was wrong (`src/lib/main` is the module `src/lib`, as the code says); the test was corrected, not the code. No bug.
- **Options:** extend the fixture (rejected: goldens would move, E-018); source held in a new test, taken.
- **Resolved:** run 11 (1,104 mutants, 1,000 killed); TypeScriptAdapterTest (new), NavigabilityTest extended; no new harmless row.
- **Touched:** test/evaluator/analyze/, docs/MUTATION.md, docs/HANDOFF.md, docs/NAVIGATION-NOTES.md.
- **Caught by:** mutation (E-019).

### J-080 analyze/ part 4 of 6 (the dependency graph and duplication): 35 survivors down to 11, all harmless; this commit is a tithe
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** TODO.md 1.12, `Duplication`, `Tokens`, `Dependencies`, `Measure`, `Tarjan` and `TestFiles`: a test or a reason for each survivor; and the tithe eight commits called for (E-015).
- **Found:** `Measure.within` and `Tarjan.order`'s deeper walk had no test of their own (`order` is always given parts already in dependency order, so its `push` never ran); no tokenizer test closed a string at the end of the text, escaped a quote or counted a block comment's lines; the duplication fixture's stretches all start at token 0, so `i - start` and `i + start` agreed; `Duplication.apart` was private and its single-place guard unreachable without a 64-bit hash collision; a cycle's first file came out right only because every member is also some edge's target. The fingerprint's six mutants cannot change a result (`sameTokens` checks every group). One Negate in `Dependencies.chain` could not be killed or proved harmless: 700,000 random graphs gave no difference. No bug.
- **Options:** a harmless row for `apart`'s guard (rejected: a collision would make it wrong); `apart` made package-private, taken. A proof for the `chain` row: tried, not found; the row says so.
- **Resolved:** run 12 (1,104 mutants, 1,024 killed); MeasureTest, TokensTest and TarjanTest (new), DependenciesTest, DuplicationTest and TestFilesTest extended; five harmless rows. The tithe puts the five points the debriefs agreed on into agent-environment.md (reading a survivor, `-Ptarget`, source held in a test, a network class on loopback, a scratch clone's name) and records Pass 4 in AGENT.md.
- **Touched:** analyze/ (`Duplication.apart`), test/evaluator/analyze/, docs/MUTATION.md, docs/HANDOFF.md, docs/AGENT.md, docs/how/agent-environment.md, docs/NAVIGATION-NOTES.md.
- **Caught by:** mutation (E-019).

### J-081 analyze/ part 4 reviewed: a build path in backticks, caught before the push
- **Who:** Claude. **Class:** failure-caught.
- **Intent:** Reviewing part 4 (4313875, run 12, a tithe, by a Sonnet agent).
- **Found:** Its tithe wrote the points three debriefs converged on (reading a survivor) into
  docs/how/agent-environment.md, ahead of the navigation review; right in content, kept. One line named
  mutations.xml under build/ in backticks: fine in a worktree, where build/ exists, refused by DocsTest on the
  pre-push's clean clone, as J-065's first wording was. Its one unproved harmless row (Dependencies.chain, a
  negated equals) says so plainly, with the search that failed to break it.
- **Options:** none weighed.
- **Resolved:** the line reworded; DocsTest's message now says build output is not in a clean clone, so the
  next agent learns why from the failure itself. The review will record the converged points as already fixed.
- **Caught by:** review, and a test run on a clean clone before the push.
- **Touched:** docs/how/agent-environment.md, test/DocsTest.java.

### J-082 analyze/ part 5 of 6 (a submission's test runs): 32 survivors down to 9, all harmless
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** TODO.md 1.12, `Reports`, `ReportFiles`, `TestPlans`, `TestRunner` and `TestRuns`: a test or a reason for each survivor.
- **Found:** No test ran `ReportFiles.outcome` with a surefire folder or a `jest.json`, or `files` with a link, a folder or a `.txt` in it; the size caps were tried far over, never at the cap; `TestPlans` had no test for a missing or unparsable `package.json`, a `build.gradle.kts`, no build file, or a requirements file that is a link or a folder; no test checked that the run's output folder is removed or that an install can time out; a run of skipped tests only, and a coverage of no lines, had no test. Eight `Reports.parse` survivors are the XML doors: only one (secure processing's attribute limit) has an effect I could see, and the JDK turns it on by default, so removing the call changes nothing. No bug.
- **Options:** a test for each door of `parse` (tried: only the attribute limit, and only against the default, so no); harmless rows saying why, taken for seven doors and `runPlans`' `valid > 0`.
- **Resolved:** run 13 (1,104 mutants, 1,047 killed); ReportFilesTest (new), ReportsTest, TestPlansTest and TestRunnerTest extended; seven harmless rows.
- **Touched:** test/evaluator/analyze/, docs/MUTATION.md, docs/HANDOFF.md, docs/NAVIGATION-NOTES.md.
- **Caught by:** mutation (E-019).

### J-083 analyze/ part 5 reviewed: the XML hardening proved by a test, not only argued
- **Who:** Claude. **Class:** decision.
- **Intent:** Reviewing part 5 (48e9c7b, run 13, by a Sonnet agent): eight of its nine harmless rows are
  Reports.parse's hardening, and a submission's reports are hostile (E-013).
- **Found:** An external DTD reference (`<!DOCTYPE x SYSTEM "file:...">`) has no `<!ENTITY`, so the text check
  passes it and the parser's features are the only defence. The rows' reasoning held (each call a second lock
  behind `load-external-dtd`, which an existing test kills), but no test showed a hostile DTD is never read. By
  hand: without `load-external-dtd` the new test fails; without `ACCESS_EXTERNAL_DTD` alone it passes (a
  redundant layer); without both, the file is read.
- **Options:** call the layers harmless; prove the defence and say the layers back each other up.
- **Resolved:** `ReportsTest.anExternalDtdIsNeverRead`; the access-list row says defence in depth, kept on
  purpose, and names the test. No production code changed.
- **Caught by:** review.
- **Touched:** test/evaluator/analyze/ReportsTest.java, docs/MUTATION.md.

### J-084 Baseline scoring decided: Suite Builder at 32762a6 is 100, empty is 0, linear
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost: score counted aspects against a declared baseline repository.
- **Found:** The card bounds every score to 0–100 (`Score`, SC-07); Ghost's baseline lets a repository read
  above 100. Anthropic's agent-built C compiler is public and would be a second reference, but is Rust, which
  the kernel cannot read yet.
- **Options:** Katana or Suite Builder as the baseline (Ghost: Suite Builder, the mature working product); a
  log scale or linear (Ghost: linear).
- **Resolved:** E-070 (proposed until built); TODO.md 1.13 after 1.12, Size first; the score's bound and the
  compiler put to Ghost in docs/FOR-GHOST.md.
- **Touched:** theGhost.md, TODO.md, docs/FOR-GHOST.md, docs/DECISIONS.md.

### J-085 A baselined score may pass 100
- **Who:** Claude. **Class:** decision.
- **Intent:** Ghost answered E-070's open question: "Yes a score can go past 100."
- **Found:** The bound lives in SC-01 and `Score`. Provenance's tiers (50, 80), Quality's shares and the
  navigability checklist are not counts, so a ceiling-free scale does not fit them.
- **Resolved:** E-071 (proposed until 1.13 builds it): counted, baselined metrics have no ceiling; the others
  keep 0–100. TODO.md 1.13's check says so; the item left docs/FOR-GHOST.md.
- **Touched:** theGhost.md, TODO.md, docs/FOR-GHOST.md, docs/DECISIONS.md.

### J-086 Navigation review: seven debriefs, five points converged, one fix, one gap closed
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The navigation review that `gradle metrics` called due (11 commits since the last).
- **Found:** Points met by several agents, each checked against the repository now. (1) Reading a survivor needs
  one file's `cat -n` at a time, and mutations.xml for what a mutant changed: five agents (parts 1 to 5),
  already fixed by 4313875 in agent-environment.md. (2) Scratch folder naming, `-Ptarget` taking a package
  glob only, source held in a new test rather than a grown fixture, a network class tried on a loopback
  `HttpServer`: one agent each, all already fixed by 4313875 (MUTATION.md run 8 says the same of `HttpGitHub`).
  (3) A worktree or fresh clone has no `build/`, so a redirect to build/run.log fails: part 5, and the two
  agents of J-075; true, `mkdir -p build` was only under mutation. Fixed in commands.md. (4) The review's own
  step 4 had no word for a point already fixed: the J-075 reviewer and this one; fixed in navigation-review.md
  with the `git log -S` way to find the commit. One agent alone, noted and not acted on: no docs/how file for
  github/; where the navigability mutation tests live; per-class survivor counts (HANDOFF now names the classes;
  run 1 lists the files); a scratch program to try a JDK-default mutant (the harmless rows already say it).
- **Options:** act on the single-agent points (step 2 says no); leave the build/ gap (a second recurrence). Acted only on converged ones.
- **Resolved:** two one-line edits; no open question, so no Fable agent asked. Notes wiped, this review's debrief starts the next set.
- **Caught by:** the navigation review.
- **Touched:** docs/how/commands.md, docs/how/navigation-review.md, docs/NAVIGATION-NOTES.md, docs/JOURNAL.md.

### J-087 analyze/ part 6 of 6 (the sandbox's network and Docker): 25 survivors down to 1, 1.12 closed
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** TODO.md 1.12, `RegistriesProxy` (15 alive) and `DockerSandbox` (10): the walls a hostile submission is on the other side of (E-013, E-037).
- **Found:** What was missing was the limits and the run, not the refusals. No test held the proxy's idle limit (client or registry), its tunnel cap (the 503 and the place given back), the header's byte cap at its edge, half-close through a tunnel, an `accept` that fails once, or `main`; none ran `DockerSandbox.run` at all (the owner, the hand-over, the exit, the kill by name, the interrupt). The proxy's idle limit and cap were constants, and `docker` a literal, so a test could not reach them without waiting a minute or starting Docker. No bug, no hole. One survivor is harmless: `out.flush()` on a socket stream that does not buffer.
- **Options:** test the idle limit by waiting 60 s (no); mark the cap and `run` harmless as "real thing only" (no: a wall's limit is what a hostile run presses on); constructor arguments and a program name, package-private, for tests (taken, as `HttpGitHub.at`).
- **Resolved:** run 14 (1,105 mutants, 1,072 killed); RegistriesProxyTest and DockerSandboxTest extended; one harmless row. 1.12 moved to DONE.md; 1.13 is next. No golden moved.
- **Touched:** analyze/evaluator/analyze/ (`RegistriesProxy`, `DockerSandbox`), test/evaluator/analyze/, docs/MUTATION.md, docs/KERNEL-MAP.md, TODO.md, docs/DONE.md, docs/HANDOFF.md, docs/NAVIGATION-NOTES.md.
- **Caught by:** mutation (E-019).
\n
### J-088 The fast loop back under its target: classes in parallel, two racy interrupt tests made certain
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** AGT-02 (20 s) read 23 s on Ghost's machine; make room without dropping, skipping or weakening a test.
- **Found:** The tests were 12 s of the run, one class at a time; here the loop read 15 s, 16, 15 (`gradle test --offline --rerun-tasks`) and 15, 16, 15 by `gradle metrics --args=--time`. With classes in parallel it read 8 to 9 s; 5 runs of 34 failed on `GitTest.anInterruptedRunFailsAndKeepsTheInterrupt` and `DockerSandboxTest.anInterruptedRunKeepsTheInterrupt` interrupted the thread, then ran a program (`git --version`, a fake docker that exits) that could already be finished when the interrupt was looked at, so no interrupt was seen. They were racy before (load only made it likely).
- **Options:** parallel classes in one JVM (taken: the code shares nothing, checked); `maxParallelForks` (a JVM start per fork, no gain over this); a 1 s timeout in `aRunPastItsTimeoutIsKilledByName` (tried: no change in the total, and more risk under load, backed out); a junit-platform.properties (test/ holds only Java sources, so the same settings sit in build.gradle beside the other test properties).
- **Resolved:** E-072. The two tests above run a program that waits for its standard input or sleeps, so the interrupt always lands on a running one; their assertions are unchanged. Serial under -PwriteGoldens and -Pdocker. 25 clean runs after the fixes, and the final 10 below.
- **Touched:** theGhost.md (E-072), docs/DECISIONS.md, build.gradle, test/evaluator/history/GitTest.java, test/evaluator/analyze/DockerSandboxTest.java, docs/AGENT.md.
- **Caught by:** repeated runs of the whole suite.
- **Cost:** about 50 full runs.

### J-089 Tithe: Pass 5 recorded, three long tests given their comments
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The tithe `gradle metrics` called due (8 commits since 4313875): change no behaviour, improve navigability, record the pass (E-015).
- **Found:** Every held metric within target. The fast loop reads 10 to 13 s in a plain clone (all green); in a git worktree `gradle metrics --args=--time` reads 21 and "failed" only because `MainTest.thisRepositorysCard` fails on Provenance there, so a pass is measured in a plain clone. Three tests were over 15 lines with no comment saying what they are for. Larger, left: the two debriefs in NAVIGATION-NOTES.md each suggest an edit to agent-environment.md (`grep -v TIMED_OUT` on the survivors line; how to time the loop); one agent each, so not acted on before a review.
- **Options:** only record the column (the minimum); also comment the three tests (taken, no code moves); act on the single-agent suggestions (no).
- **Resolved:** Pass 5 column in docs/AGENT.md; a one-line comment on each of the three tests. No golden, no function added or moved, so no kernel map change.
- **Touched:** docs/AGENT.md, test/evaluator/analyze/TestRunnerTest.java, test/evaluator/github/AuthorshipTest.java, test/evaluator/github/HttpGitHubTest.java.
- **Caught by:** `gradle metrics` (the cadence line and the comment listing).

### J-090 The supply-chain rules back in CLAUDE.md, after an agent skipped the pointer
- **Who:** Claude. **Class:** failure-caught.
- **Intent:** Reviewing the tithe (2c3810a, J-089) and its context use, as Ghost asked.
- **Found:** The tithe agent read about 5,800 words through 13 calls (82k tokens all told; 2,464 of them in one
  read, gathering what a tithe is from five files). Its commands never read theGhost.md's first section, the
  supply-chain rules CLAUDE.md pointed it to before any task. Nothing it did touched a dependency.
- **Options:** the rules back in CLAUDE.md, read on entry; keep the pointer and have each debrief confirm it.
- **Resolved:** Ghost: back in CLAUDE.md. E-073 supersedes E-061: one copy in CLAUDE.md, theGhost.md points to
  it, SupplyChainTest holds both; the mandatory context is CLAUDE.md alone.
- **Caught by:** counting the agent's commands, without reading its transcript into context.
- **Touched:** CLAUDE.md, theGhost.md, test/SupplyChainTest.java, scripts/Metrics.java,
  docs/how/add-a-language-adapter.md, docs/how/sandbox-test-runs.md, docs/DECISIONS.md.

### J-091 Size against the baseline: the first counted metric, 84,971 lines is 100
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The first slice of TODO.md 1.13: Size as 100 × lines ÷ the baseline's (E-070), and the ceiling lifted for it (E-071).
- **Found:** Suite Builder at 32762a6, read by this kernel without the sandbox on a clean clone (`evaluator score --no-run`; the card
  carries only the score, so the line count came from `Size.facts` on the same inventory): 84,971 source lines, 916 files, 119 folders,
  122 set aside. The old log scale read it 98. No card golden's Size moved: every fixture is under 425 lines (the largest 103), which
  reads 0 on both scales, so the goldens do not exercise the new formula and the unit test carries it. The seed corpus cards in
  `corpus/` (dated 2026-10-22, rubric b8043f1115a0) were not regenerated: no test holds them to the current rubric.
- **Options:** the ceiling in `Score` per metric (it knows none), a flag on `Metric` checked by `ScoreCard`; the second taken.
- **Resolved:** `Formulas.BASELINE_SOURCE_LINES` and `BASELINE_COMMIT`, the rubric string naming them (version 2, hash 15affbf5ee9e);
  `Metric.counted` (Size only), `ScoreCard` refusing a score over 100 for the rest, `Score` keeping only a floor; SC-01 reworded and
  held by `ScoreCardDocTest`; E-048 superseded for its formula, E-070 and E-071 in force for Size.
- **Touched:** E-048, E-070, E-071, TODO.md 1.13, docs/SCORE-CARD.md, docs/HANDOFF.md, docs/MUTATION.md (run 15), the ten card goldens (hash only).

### J-092 Complexity against the baseline: 13,361 cognitive and depth 28 are 100
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The second slice of TODO.md 1.13: Complexity as 0.6 × (100 × cognitive ÷ the baseline's) + 0.4 × (100 × depth ÷ the baseline's), E-050's weights and inputs on E-070's scale.
- **Found:** Suite Builder at 32762a6, read on a clean clone by a scratch program calling the modules `evaluator score --no-run` calls (`Ingest`, `Complexity`,
  `Dependencies`; none of its code run): total cognitive complexity 13,361 over 4,410 functions, dependency depth 28, 84,971 lines again (Size's figure), one file unread
  by its adapter. E-050 defines the inputs as the lead described them, so none changed; its depth cap of 25 and log scale go. The old formula read the baseline 81
  and capped its depth (28) at 25. The goldens were read before they were written: a scratch program rendered the ten fixtures' new cards, each
  Complexity was worked by hand from the fixtures' cognitive total and depth (small-app, py-pass, py-fail 1 and 1 → 1.43, 1; node-pass 0 and 1 → 1.43, 1; imports 0 and 2 → 2.86, 3, unmoved;
  the rest cognitive 21 to 32, depth 0 → under 0.2, 0, unmoved), then `-PwriteGoldens` wrote files byte for byte equal to them. Maintainability at Complexity, a product with Complexity, moved with it on the same four cards.
- **Options:** keeping `logScale` for nothing (removed, with its harmless row), a cap on depth (the lead's formula has none).
- **Resolved:** `Formulas.BASELINE_COGNITIVE`, `BASELINE_DEPTH`, `complexity`, the rubric (version 3, hash 4d7287ec37e6), `Metric.counted` for Complexity, SC-01 naming both, E-074 superseding E-050's formula.
- **Touched:** E-050, E-070, E-071, E-074, TODO.md 1.13, docs/SCORE-CARD.md, docs/HANDOFF.md, docs/MUTATION.md (run 16), docs/KERNEL-MAP.md, the ten card goldens.

### J-093 Navigation review: facts not shown to the formula agents, the tithe's steps
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The navigation review `gradle metrics` called due, over seven debriefs.
- **Found:** (1) No way to run `evaluator score` from a build, and a card holds fact ids, not values: two agents (Size, Complexity), still true; the java command was tried and works. Fixed in command-line.md; a `--facts` option is larger, so a next step in HANDOFF. (2) Step 6 of change-a-formula.md cannot be followed in its order (read goldens before writing): the same two agents, true; a `-PshowGoldens` is a next step in HANDOFF, the step now says to work scores by hand and that a 0-on-both-scales fixture proves nothing. (3) A status's parenthesis may not hold a `;`, learned only from a failing test: the same two, true (GhostTest splits on it first); fixed in theGhost.md's entry form. (4) What a tithe pass does is spread over three files, and the fast loop is timed in a plain clone: the fast-loop and tithe agents, true; fixed in agent-environment.md (a Tithe step, timing, the race remark). Already fixed: none. Single agent: the survivors grep overcounts TIMED_OUT rows, which the doc itself calls killed, a fact gone wrong, fixed in agent-environment.md; a "commit only" closing step has no home in do-a-todo-step.md, noted and not acted on; the first debrief had nothing.
- **Options:** a new HANDOFF item for the java command (it is an instructions change, so no); writing `--facts` now (a command-line option, so a next step).
- **Resolved:** five instruction edits and one HANDOFF section; no open question, so no Fable agent asked. Notes wiped, this review's debrief starts the next set.
- **Caught by:** the navigation review.
- **Touched:** docs/how/command-line.md, docs/how/change-a-formula.md, docs/how/agent-environment.md, theGhost.md, docs/HANDOFF.md, docs/NAVIGATION-NOTES.md, docs/JOURNAL.md.

### J-094 Maintainability against the baseline: 0.895670359282887 weighted is 100
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The third and last slice of TODO.md 1.13: Maintainability as 100 × E-051's weighted score ÷ Suite Builder's, E-051's six parts and weights kept.
- **Found:** Suite Builder at 32762a6, read on a clean clone by a scratch program on the code facts `evaluator score --no-run` reads (none of its code run): duplication 0.027 (0.73),
  tests 1.412 (1), cycles 87 of 914 (0.9048), functions 4,233 of 4,410 at most 15 (0.95986), coupling 851 of 914 at most 10 (0.93107), unused 88 over 913 (0.90361); weighted 0.895670359282887.
  E-051 had said 88 for it (an earlier reading); this one is 89.57. The test builds those exact facts and reads 100. The goldens were read before they were kept: the files were
  written, then every new value worked by hand from a scratch program's parts for each fixture (duplication 0.579 → 64.6 → 65; imports 0.7083 → 79.1 → 79; the three complexity fixtures
  0.75 → 83.7 → 84; small-app 0.7875 → 87.9 → 88; the four test-runs fixtures all parts 1, two of them with parts absent (node-pass 0.6 of the weight, py-network 0.5) → 111.65 → 112).
  Maintainability at Complexity moved on the three cards that have both (imports 2.13 → 2.37, small-app 0.79 → 0.88, node-pass, py-pass and py-fail 1.0 → 1.12). Nothing else moved.
- **Options:** scaling each part (refused: a part where the baseline reads 0 divides by zero); comparing a repository with a part absent to the baseline's same parts (the lead said the baseline's weighted score; taken as said, and recorded in E-075).
- **Resolved:** `Formulas.BASELINE_MAINTAINABILITY`, `Maintainability.weighted` and `score`, the rubric (version 4, hash cb62abb8be3c), `Metric.counted` for Maintainability, SC-01 naming all three, E-075 superseding E-051's scale, E-070 and E-071 in force, TODO.md 1.13 to DONE.md.
- **Touched:** E-051, E-070, E-071, E-075, TODO.md, docs/DONE.md, docs/SCORE-CARD.md, docs/HANDOFF.md, docs/MUTATION.md (run 17), docs/KERNEL-MAP.md, the ten card goldens.


### J-095 `evaluator score --facts`: the values behind a card, without a scratch program
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** The first next step of the navigation review (J-093): three baseline agents (J-091, J-092, J-094) each wrote a scratch program to see the repository-wide facts and the inputs of a counted metric.
- **Found:** Complexity's total was an inline loop in `Formulas.complexity` and Maintainability's parts are package-private in `score/`, so the printing could not live in `cli/` without arithmetic of its own (E-010). The card's own output needed no change: `--facts` replaces the card, so no golden moved. Run on Suite Builder at 32762a6 (a scratch clone, nothing of it run, `--no-run`), it printed SIZE.source_lines 84971, COMPLEXITY.cognitive_total 13361 over 4410 functions, COMPLEXITY.dependency_depth 28 and MAINTAINABILITY.weighted 0.895670359282887 with the six parts (0.73, 1, 0.9048, 0.9599, 0.9311, 0.9036): the three baseline measures `Formulas` records, to the digit. The first mutation pass left one survivor in the printing of a number (a condition `toPlainString` made unneeded); it was removed rather than excused, and the rerun of `score/` killed 170 of 170.
- **Options:** the facts after the card on the same stdout (refused: a JSON card would no longer parse); the facts as JSON (refused: a `name value` line is what grep and a hand check want); the print in `cli/` over `Formulas` (refused: it would redo their arithmetic there).
- **Resolved:** `FactsText` in `score/` (sorted `name value` lines; the cognitive total and function count, the file count, the six parts and the weighted score computed by `Formulas.cognitiveTotal` and `Maintainability`), `Main`'s `--facts` flag refused with `--markdown` or `--date`, E-076, `docs/how/command-line.md` and step 6 of `docs/how/change-a-formula.md` no longer send an agent to a scratch program, HANDOFF's next step is `-PshowGoldens` alone.
- **Touched:** E-010, E-076, docs/how/command-line.md, docs/how/change-a-formula.md, docs/HANDOFF.md, docs/MUTATION.md (run 18), docs/KERNEL-MAP.md, docs/DECISIONS.md.

### J-096 Tithe: Pass 6 in AGENT.md, the fast loop timed five times
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The tithe due (E-015): seven commits since the last, eight with J-095's.
- **Found:** In a plain clone, `gradle metrics --args=--time` read the fast loop 17 s cold, then 9, 9, 9 and 8; no run failed, so no race in a test (E-072). Every held metric is within target; the largest source file is 283 lines (test/SupplyChainTest.java), mandatory context 780 words, docs per source line 0.41. The metrics list no function over 15 lines or 6 paths without a comment, so no comment was added.
- **Options:** none.
- **Resolved:** Pass 6 column in docs/AGENT.md.
- **Touched:** docs/AGENT.md.

### J-097 `gradle test -PshowGoldens`: read a changed golden before writing it
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** The last next step of the navigation review (J-093): step 6 of `docs/how/change-a-formula.md` says read each changed golden, then write, but only writing showed one.
- **Found:** Six tests compared goldens, each with its own copy of the `-PwriteGoldens` property and its own compare (`CardGoldensTest`, `IngestTest`, `JavaAdapterTest`, `TreeSitterAdaptersTest`, `DependenciesTest`, `DuplicationTest`); the build prints a failed test's message and nothing else (`testLogging`), so the diff goes in the message and no setting changed. Proof on a real change: `BASELINE_SOURCE_LINES` 84,971 to 1,000, run with `-PshowGoldens`: the output named all ten cards in both renderings, e.g. `small-app.json` line 4, `"size": {"value": 0` to `{"value": 2`; no golden file changed; the edit was undone. No golden moved in the commit.
- **Options:** the diff on stdout (refused: not shown by the build); `-PwriteGoldens` showing the diff as it writes (refused: the reading must come before the writing); the properties together meaning "write" (refused: nobody would guess it).
- **Resolved:** `test/evaluator/Goldens.java` (`check`, `Batch`, one line diff by common lines) used by the six tests, the properties together refused in `build.gradle` and in `Goldens.mode`, E-077, `test/evaluator/GoldensTest.java`, docs/how/commands.md, step 6 of docs/how/change-a-formula.md, docs/HANDOFF.md. No kernel code changed, so no mutation run; the helper is test code, which `gradle mutate` leaves unmutated.
- **Touched:** E-018, E-077, build.gradle, docs/DECISIONS.md.

### J-098 The seed corpus made again under the baseline rubric
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The three cards in `corpus/` were made under rubric b8043f1115a0 and the rubric is now version 4 (E-070 to E-075); make them again at the same commits and date so the rubric is the only change.
- **Found:** Each new value matches its facts and formula by hand (`--facts`): Size 100 x lines / 84,971 (this repository 8,815 lines, 10; Suite Builder 81,906, 96; Katana 57,154, 67); Complexity 0.6 x 100 x cognitive / 13,361 + 0.4 x 100 x depth / 28 (22, 96, 55); Maintainability 100 x weighted / 0.895670359282887 (94, 100, 63). Two things moved that were not the rubric. The JavaScript adapter (E-058, after these cards) reads `.js` and `.mjs` now: Suite Builder gained 193 Complexity and 252 Maintainability facts, this repository 6 Maintainability facts, Katana none. And this repository's Provenance reads 72, not 73: the kernel at a7fea50f, built in a scratch clone, reads 72 on the same history, while it reproduces Suite Builder's and Katana's old cards byte for byte, so the old card of this repository was not made from that exact history (it reads 73 two commits earlier).
- **Options:** keep the old Provenance by hand (refused: a card is the tool's output); leave the corpus alone and say why (refused: HANDOFF asked for regenerate or say why not, and the numbers in the README's judges table had moved).
- **Resolved:** the six card files, `corpus/README.md` (rubric, regeneration, the judges table, scores over 100), E-078 and `test/CorpusTest.java` (each card's JSON and Markdown say the rubric the README names; not the current one, since that would fail every rubric change until outside repositories were scored again), `docs/HANDOFF.md`.
- **Touched:** E-078, corpus/, docs/HANDOFF.md, docs/DECISIONS.md.

### J-099 Navigation review: four debriefs and the second review's, one rule moved to where agents meet it
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** The review due (E-068): ten commits since J-093's, past which `gradle metrics` warns.
- **Found:** Five entries (the previous review's own and four tasks'). Converging points: (1) a scratch program to see fact values, met by the Maintainability and the `--facts` agents (and three of the set before): already fixed by 53586fb (`--facts`, J-095); the Maintainability agent's suggested line (a scratch class goes in the package it reads) is moot. (2) A rule met only through a failure, by two agents here (the order of `Metric`, found by a failing `ScoreCardDocTest`; `Main.Command` built by hand in `CardGoldensTest`, found by the compiler) and by three earlier ones (the `;` in a status's parenthesis, J-093): valid. The entry form's note (6ff6e5f) is where an agent writes a decision, not where it meets the rule; the failing test's own message said only "unknown status: in force (x" with no reason. Fixed there: `GhostTest` names the allowed statuses and, when a parenthesis is in the way, the `;` rule; `ScoreCardDocTest` says when the document and the code hold the same words in another order. The compiler's message for `Main.Command` is already plain, so no change. Noted, one agent each, not acted on: the seed corpus has no routing row and no regeneration recipe outside `corpus/README.md` (HANDOFF names it); a helper shared by several modules' tests lives in `test/evaluator/` and `gradle mutate` leaves it unmutated; other goldens docs name only `-PwriteGoldens` (commands.md and step 6 of change-a-formula.md carry `-PshowGoldens`); three more tests fail in a fresh worktree until `kernelMap` and `decisionsDoc` have run; "`gradle metrics` lists nothing" is not said to be an answer.
- **Options:** a CLAUDE.md or instructions line for each rule (refused: an agent reads it before the task, not at the failure); a test message (taken).
- **Resolved:** two test messages; no open question, so no Fable agent asked; notes wiped, this review's debrief starts the next set. No new HANDOFF step: nothing found is larger than an instructions change. Test code only, so no mutation run (`gradle mutate` leaves `test/` unmutated).
- **Caught by:** the navigation review.
- **Touched:** test/GhostTest.java, test/ScoreCardDocTest.java, docs/NAVIGATION-NOTES.md, docs/JOURNAL.md.

### J-100 Handoff for the session with Ghost's GitHub token
- **Who:** Claude. **Class:** direction.
- **Intent:** Ghost made a GitHub token and asked for a handoff to the next session.
- **Found:** The token Ghost made reads public repositories only, with no permissions; the live test's
  repository, Ghost-in-the-Kernel-Labs/suite-builder, is public (GitHub answers it unauthenticated), so it
  suffices. The old handoff asked for a repository-scoped token with Contents read.
- **Resolved:** docs/HANDOFF.md rewritten: 1.7's live check first (check the variable is set without printing
  it, run the test alone to a file, close 1.7 or report GitHub's refusal), what is done, what waits on Ghost
  (2.1's open questions, the token-optional proposal), how work is handed out, the session's environment.
- **Touched:** docs/HANDOFF.md.

### J-101 The live test's repository is private; a check through the session's proxy proved nothing
- **Who:** Claude. **Class:** failure-caught.
- **Intent:** Writing J-100's handoff.
- **Found:** J-100 said Ghost-in-the-Kernel-Labs/suite-builder is public because an unauthenticated `curl`
  got 200. The cloud session's egress proxy carries the session's own GitHub access, so the request was
  not unauthenticated. Ghost: the repository is private; the session reads it through the GitHub
  integration. Ghost's public-repositories token cannot read it.
- **Options:** a token scoped to the repository; the public token with the live test pointed at one of
  Ghost's public repositories (changes 1.7's check).
- **Resolved:** docs/HANDOFF.md corrected: the next session asks Ghost which way before running the test.
- **Caught by:** Ghost.
- **Touched:** docs/HANDOFF.md.

### J-102 The live test's first real run: a path guard refused GitHub's own compare path; and a pass that proves nothing about the token
- **Who:** Claude (Sonnet 5.5), with Ghost. **Class:** failure-caught.
- **Intent:** Run 1.7's live check (docs/HANDOFF.md) now that `EVALUATOR_GITHUB_TOKEN` is set.
- **Found:** (1) `HttpGitHub.get` refused any path containing `..`, and a compare path is `<sha>...<sha>`: the
  live test failed with "not an API path" on its first compare, a bug no loopback test could show because no
  test sent a real compare range. (2) After the fix the live test passes, but it is no proof of the token: from
  this cloud session `api.github.com` answers the private repository 200 with an invalid token, with the
  environment's token and with none (J-101's proxy caveat, checked directly).
- **Options:** close 1.7 on the pass; keep 1.7 open until a run that does not go through the session's proxy.
- **Resolved:** the guard now refuses a `..` path segment only, with a test for both; 1.7 stays open. The live
  check needs a run outside a session whose proxy carries GitHub access, or Ghost's decision that this pass
  is enough.
- **Caught by:** the live test; the proxy by a request with a bad token.
- **Touched:** github/HttpGitHub.java, test/evaluator/github/HttpGitHubTest.java; TODO.md 1.7 (open); docs/HANDOFF.md.

### J-103 1.7 closed on a live pass the proxy made possible
- **Who:** Claude (Sonnet 5.5), with Ghost. **Class:** decision.
- **Intent:** Settle 1.7 after J-102 found the pass proves nothing about the token.
- **Found:** The live run read, of Suite Builder's history: 8 signed, 302 pushed, 190 claimed, 268 foreign,
  0 unmatched; applicant share 0.651; push window 20 days.
- **Options:** run again outside a proxied session; accept the pass.
- **Resolved:** Ghost accepted the pass as enough (2026-10-24). 1.7 moved to DONE.md. Ghost also cleared uhneer's
  and CryptoDevving's repositories (lab members) for the calibration set, 2.1.
- **Touched:** TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-104 2.1's format half: the calibration label, its loader and its test; the labels wait on Ghost
- **Who:** Claude (Sonnet 5.5), for Ghost. **Class:** feature.
- **Intent:** Make the calibration set ready to receive Ghost's 30 hand labels (TODO.md 2.1): a fixture format and a test that holds every label to its fields.
- **Found:** DESIGN.md names the scales (Ambition toy / utility / product / research, Quality 1–5) and nothing else, so the other fields are a new decision, E-079. The check "30 labelled, at least five domains" cannot pass yet, so it reports PENDING below 30 and enforces from 30.
- **Options:** a CSV or one file for the set; one JSON file per label named by an opaque id (chosen: a leak shows in one file, the loader refuses a file not named by its id).
- **Resolved:** `Label` and `CalibrationSet` in rubric/ (no new import, no dependency), `calibration/` (empty but for its README), synthetic labels under `test/fixtures/calibration-synthetic/`. 2.1 stays open in TODO.md: it waits on Ghost's labels. No golden moved. Mutation over rubric: the new classes 0 survivors; the 2 left are `Json`'s, already in docs/MUTATION.md.
- **Caught by:** the kernel map and metrics doc tests, which asked for `gradle kernelMap` once the functions existed.
- **Touched:** rubric/evaluator/rubric/Label.java, CalibrationSet.java; test/evaluator/rubric/CalibrationSetTest.java; calibration/README.md; test/fixtures/calibration-synthetic/; theGhost.md (E-079), docs/DECISIONS.md, docs/KERNEL-MAP.md.

### J-105 The calibration label trimmed to id, kind, ambition and quality (E-080)
- **Who:** Claude (Sonnet 5.5), for Ghost. **Class:** decision.
- **Intent:** Carry out Ghost's instruction to trim the calibration label to ambition, quality and kind (TODO.md 2.1).
- **Found:** A label exists only to give ground truth to the judges and the admission gate; domain was only a spread check and nothing consumes a rationale. With the rationale gone no free text remains, so the URL and name screen has nothing to screen.
- **Options:** keep domain as optional; keep the screen as dead code; remove both (chosen, E-080 supersedes E-079's field list). The five-domains check cannot be tested from labels, so TODO.md 2.1 now says the spread is recorded in Ghost's private id-to-repository map (E-014).
- **Resolved:** `Label` and `CalibrationSet` take four required fields and refuse any other; Status has PENDING and MET (30 labels). Fixtures, README, tests, TODO.md 2.1 and DECISIONS.md follow. The 30 draft labels were trimmed outside the repository and validated by the real loader as 30 labelled; they are not committed and await Ghost's review.
- **Caught by:** DocTests.ghost, which refused a `;` inside the superseded status.
- **Touched:** rubric/evaluator/rubric/Label.java, CalibrationSet.java; test/evaluator/rubric/CalibrationSetTest.java; calibration/README.md; test/fixtures/calibration-synthetic/; TODO.md; theGhost.md (E-079 status, E-080), docs/DECISIONS.md, docs/KERNEL-MAP.md.

### J-106 The 30 calibration labels land; 2.1 closed
- **Who:** Ghost, Claude (Sonnet 5.5). **Class:** measurement.
- **Intent:** Land the calibration set (TODO.md 2.1) now that Ghost has reviewed the 30 labels.
- **Found:** Each file holds exactly id, kind, ambition and quality, and none holds a name, an owner or a URL (checked against the private map, which stays outside the repository). Kind: own-work 23, clone 7, generated 0. Ambition: utility 14, product 10, toy 3, research 3. Quality: 1 x 2, 2 x 6, 3 x 17, 4 x 4, 5 x 1. The real-folder test reads the set as MET at 30.
- **Options:** none; the format was settled in E-080.
- **Resolved:** the labels were drafted by Claude Sonnet agents applying the rubric and reviewed by Ghost, who confirmed them. Caveat: agreement measured later against these labels (2.2 to 2.4) is partly model against model; Ghost's review is what keeps it from being circular. The id-to-repository map and the domain spread stay outside the repository (E-014).
- **Caught by:** the real-folder test of `CalibrationSetTest`.
- **Touched:** calibration/c001.json to c030.json; TODO.md, docs/DONE.md, docs/HANDOFF.md, docs/NAVIGATION-NOTES.md.

### J-107 Tithe: Pass 7 in AGENT.md, three functions commented
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The tithe due (E-015): eleven commits since the last, three past the eight.
- **Found:** In a plain clone, `gradle metrics --args=--time` read the fast loop 8 s on five runs, none failed, so no race in a test (E-072). With `EVALUATOR_GITHUB_TOKEN` set in the environment, `AuthorshipLiveTest` calls GitHub and the same loop reads 143 to 158 s (138 s in that one test): the number is the token's, not the code's, so the timing ran with the variable unset. Every other held metric is within target; the largest source file is 283 lines (test/SupplyChainTest.java), mandatory context 780 words, docs per source line 0.40. The metrics listed three functions over 15 lines or 6 paths with no comment (`Label`'s constructor, `GhostTest.fieldsAndStatus`, one `GoldensTest` test); each has one now. Eight commits have passed since the last navigation review.
- **Options:** none.
- **Resolved:** Pass 7 column in docs/AGENT.md, three comments, HANDOFF's cadence line corrected to the counts above. No behaviour changed, no golden moved.
- **Touched:** docs/AGENT.md, docs/HANDOFF.md, rubric/evaluator/rubric/Label.java, test/GhostTest.java, test/evaluator/GoldensTest.java.

### J-108 Navigation review: five debriefs read; do-a-todo-step, commands and the tithe step gain the lines the agents hunted for
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The navigation review due (E-068): nine commits since the last (J-099), the five debriefs of J-104 to J-107 and the previous review's own.
- **Found:** Converging (three agents): do-a-todo-step.md is silent on a superseded entry's status (one hit the `;` rule only after a 3-minute run, though J-099 already put the rule in the failing message, fdf5d9a: valid, the message cannot save the run), on the journal Class for a landing, and on a new phase's heading in DONE.md; the tithe agent also found no word on where its Class and debrief go. Single, but plainly facts: gradle runs exceed the 120 s tool timeout (commands.md silent); the token makes the tithe loop read 150 s. Not acted on: the Ambition and Quality scales have no one home (one agent; the label's enums now hold them). Also found: J-104 has Class `feature`, outside the list, unchecked by JournalTest; history is not rewritten.
- **Options:** (a) tithe step says to unset the token, or Metrics.java unsets it for its child run (a code change, not this review's): chose the line, yes. Open questions for Fable: none.
- **Resolved:** do-a-todo-step.md (superseding, Class, new phase heading), agent-environment.md (`env -u EVALUATOR_GITHUB_TOKEN`, tithe Class and debrief), commands.md (background for long gradle runs); NAVIGATION-NOTES.md wiped.
- **Touched:** docs/how/do-a-todo-step.md, agent-environment.md, commands.md, NAVIGATION-NOTES.md.

### J-109 TODO.md 2.2 sliced into fourteen parts; TodoTest and DocsTest learn a part's letter
- **Who:** Claude Sonnet 5.5, with Ghost's go. **Class:** decision.
- **Intent:** Slice 2.2, the judge harness, too large for one go (`docs/how/do-a-todo-step.md`), into parts one fresh Sonnet agent can do, the ones needing no model API first.
- **Found:** `TodoTest` and `DocsTest` read only `### N.M ` headings, so a part such as 2.2a did not parse. The agent runner is Python (E-001) and outside the Java module map, so no module entry is needed; the pinned image has no `python3`, so its tests would be PENDING in the gate. The labels' kind (own-work, clone, generated) and the judge's (built, plan only, generated, mixed) share only "generated", and the set has none. A constant answer clears 77 % on kind and 90 % on Quality within one.
- **Options:** parts as a list inside one item (as 1.13 was, but then no check per part); `2.2.1` numbering (also unparsed, and the same change); letters (chosen: smallest change, DONE.md keeps the number).
- **Resolved:** 2.2a to 2.2n in TODO.md, each with a check; DESIGN.md "Step 2.2 sliced" with the table, the findings and nine questions; the questions in FOR-GHOST.md; HANDOFF names 2.2a. Two tests accept an optional letter after the step and refuse parts out of order or without a check. No decision added: the open ones are questions to Ghost.
- **Touched:** TODO.md, docs/DESIGN.md, docs/FOR-GHOST.md, docs/HANDOFF.md, test/TodoTest.java, test/DocsTest.java.

## 2026-10-25

### J-110 TODO.md 2.2a: the agent-runner scaffold, its Java gate and its import guard; python3 joins the pinned image (E-081)
- **Who:** Claude Sonnet 5.5, with Ghost's approval of python3. **Class:** decision.
- **Intent:** Build 2.2a: `agent-runner/` as a standard-library Python package, a Java test that runs its tests under `gradle test`, and a guard against a network or key door before 2.2l.
- **Found:** The pinned base already carried `python3` 3.12.3-0ubuntu2.1 as a dependency of another package, so installing it by name changed no bytes but keeps it on purpose. `python3 -I` ignores `PYTHONDONTWRITEBYTECODE`, so the gate adds `-B`. The plan says `docs/modules.json` gains nothing (Python is outside the Java map), against the task's wording; the plan and `ModulesTest` were followed.
- **Options:** a text grep for the banned imports (fooled by strings and comments; refused) or parsing each file with `ast` (chosen: catches `__import__`, `importlib`, `from os import getenv`, a whole-environment read); a gate that fails without `python3` (refused by the check: PENDING as `CalibrationSet` is).
- **Resolved:** `agent-runner/agent_runner/{__init__,guard}.py`, `agent-runner/tests/test_guard.py`, `test/AgentRunnerTest.java` (in `DocTests`; the child sees no variable naming a key), the Dockerfile line, E-081, `docs/how/judge-harness.md` with its CLAUDE.md row, 2.2a to DONE.md, HANDOFF names 2.2b, FOR-GHOST question 5 answered.
- **Touched:** E-081; Dockerfile; CLAUDE.md; TODO.md; docs/DONE.md; docs/HANDOFF.md; docs/FOR-GHOST.md; test/DocTests.java.

### J-111 TODO.md 2.2b: `evaluator brief`, the kernel's facts, inventory and commit log as JSON for a judge
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2b: one deterministic JSON document a judge is given before it reads any file, from what the modules already count.
- **Found:** `Inventory.toJson`, `FactsText` and `Commits` already hold everything but the README and agent-file paths; `Commits` reads no author, so identity stays out by construction (E-014). The cards' fixtures hold no `.git`, so the golden has an empty log; the log is tested on a history built in a temporary folder and on synthetic commits.
- **Options:** the facts as `--facts` lines in an array, or an object (chosen: an object, numbers kept as numbers); README paths from the whole tree, or the root only (chosen: root, an `// Unsure:`); a new module (refused: `cli/` names the work, the path rules went to `Navigability`).
- **Resolved:** `Brief` in `cli/`, `Main.brief`, two public path lists in `Navigability`, `BriefTest`, a golden of small-app. No new decision.
- **Touched:** cli/, analyze/Navigability.java, docs/how/command-line.md, TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-112 TODO.md 2.2c: the repo summary builder, the judge's whole reading, in a data fence
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2c: turn the brief and a checkout into one deterministic text a judge reads with no tool, treating the repository's text as data.
- **Found:** Python goldens have no Gradle switch: `-PwriteGoldens` reaches only the JVM, and the gate's child sees a scrubbed environment, so the Python golden test rewrites on `EVALUATOR_WRITE_GOLDENS=1` run by hand. A path, a commit subject and a fact value are repository text too, so they go in fences, not only files.
- **Options:** a fixed-length fence (refused: a file can hold it) or one longer than any tilde run inside (chosen); dropping files over the total cap (refused: named in a `not-shown` fence); reading the brief's agent instruction paths into the summary (left out: the brief names them, 2.2e may decide).
- **Resolved:** `summary.py`, `test_summary.py`, two synthetic fixtures with briefs and two goldens, `judge-harness.md` updated, 2.2c to DONE.md, HANDOFF names 2.2d.
- **Touched:** agent-runner/, test/fixtures/summary-*, test/goldens/summaries/, docs/how/judge-harness.md, TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-113 TODO.md 2.2d: the judgment schema and its parser; the judge's kind becomes six values (E-082)
- **Who:** Claude Sonnet 5.5, with Ghost's choice. **Class:** decision.
- **Intent:** Build 2.2d: one schema file for the judge's answer and a strict validator, with Ghost's change of 2026-10-25 that the judge's kind is E-057's four plus own-work and clone.
- **Found:** The labels (own-work, clone, generated) and the judge's four kinds shared only "generated" (DESIGN, the label vocabulary finding). Six values in one enum mix two axes (contents, whose work), so ambiguity is cut by an ordered rule in the schema's own description, which 2.2e will quote. There is no `jsonschema` library, and the structured-output subset is narrow, so the validator checks the schema file against the subset as well as an answer against the schema.
- **Options:** the labels' three only (refused by Ghost's choice); a separate clone flag beside the four kinds (DESIGN question 8's idea; not chosen); six values in one enum, with a precedence rule (chosen, E-082). The verdict's length is a validator rule outside the schema, since string lengths are not in the subset.
- **Resolved:** `schema/judgment.schema.json`, `judgment.py`, `test_judgment.py`; E-082 recorded and E-057's status set to superseded in part; TODO 2.2d, 2.2i, DESIGN, FOR-GHOST and judge-harness.md brought in line; 2.2d to DONE.md; HANDOFF names 2.2e. Ambition is an object (tier, confidence, evidence) so its fact can carry evidence (SC-02), a small step past the check's wording.
- **Touched:** E-057, E-082, agent-runner/, TODO.md, docs/DONE.md, docs/DESIGN.md, docs/FOR-GHOST.md, docs/HANDOFF.md, docs/how/judge-harness.md.

### J-114 TODO.md 2.2e: the judging prompt, its rendering and its hash
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2e: the judge's instructions as files, rendered with a summary into the exact text sent, with a hash that names the prompt without naming the repository.
- **Found:** The trial notes record the winning sets only by name and one line (round 2's v22 archetypes "a 100-line script, a CRUD app, a compiler" and v08 navigability by mechanism, paths checked), not their wording, so the wording is new and general; the archetype scores are invented bands, not calibration material, and the calibration labels were not read. The E-082 rule and E-057's classes live in the schema's descriptions, so the prompt quotes them by substitution and a copy cannot drift. The CLI cannot run under `-I` (the package is not on the path without the current directory), unlike the tests, which discover it.
- **Options:** hash the template's body too (refused: the check names the version, and the golden holds the body); embed the schema compacted (refused: its bytes are what the hash and the API share); a placeholder pass that rescans (refused: a hostile summary could carry a placeholder).
- **Resolved:** `prompts/judge.*`, `agent-runner/agent_runner/prompt.py`, `agent-runner/tests/test_prompt.py`, a golden, judge-harness.md, 2.2e to DONE.md, HANDOFF names 2.2f. No new decision.
- **Touched:** agent-runner/, test/goldens/prompts/, docs/how/judge-harness.md, TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-115 TODO.md 2.2f: the judge interface, a fake judge and the recorded-response replay
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2f: the interface every judge implements, the two judges tests use, and the recordings file the replay reads.
- **Found:** The check says a judge takes "a rendered prompt", but a replay keyed by prompt hash and summary hash cannot recover either from plain text (the summary sits inside the render and the hash is of the files), so the judge is given a `RenderedPrompt`: the text plus both hashes, made by one function. No kernel code changed, so no mutation run.
- **Options:** one JSON object for the file (refused: a new recording rewrites the whole diff); JSON lines with a version header (taken: one line per recording, sorted, byte-stable); a loader rule refusing recordings outside `build/` (refused: the check does not call for it, and a test holds only synthetic files).
- **Resolved:** `agent_runner/judge.py`, `tests/test_judge.py`, a synthetic fixture, judge-harness.md, 2.2f to DONE.md, HANDOFF names 2.2g. No new decision.
- **Touched:** agent-runner/, docs/how/judge-harness.md, TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-116 Tithe: Pass 8 in AGENT.md, one function commented
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The tithe due (E-015): eight commits since the last, at the limit.
- **Found:** In a plain clone with `EVALUATOR_GITHUB_TOKEN` unset, `gradle metrics --args=--time` read the fast loop 29 s on the cold first run (daemon start), then 16, 14, 13, 13, 13 and 13 s, none failed, so no race in a test (E-072). It was 8 s at Pass 7: the loop now also runs the Python suite of `agent-runner/` through its Java gate (`AgentRunnerTest`), but `python3 -I -B -m unittest discover` alone takes about 0.3 s, so the rise is the Java tests added since, and the loop stays under its 20 s. Every other held metric is within target; largest source file 283 lines (test/SupplyChainTest.java), mandatory context 794 words, docs per source line 0.41. The metrics listed one function over 15 lines or 6 paths with no comment (`AgentRunnerTest.run`); it has one now. Seven commits had passed since the last navigation review (eight with this tithe); no review was done in this pass.
- **Options:** none.
- **Resolved:** Pass 8 column in docs/AGENT.md, one comment, HANDOFF's cadence line corrected. No behaviour changed, no golden moved.
- **Touched:** docs/AGENT.md, docs/HANDOFF.md, docs/JOURNAL.md, docs/NAVIGATION-NOTES.md, test/AgentRunnerTest.java.

### J-117 TODO.md 2.2g: the cost cap
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2g: a token budget a judge runs under, refusing a call before it is made and recording what each judgment cost.
- **Found:** There is no tokenizer, so the prompt's tokens cannot be counted before a call; the reserve uses ceil(bytes / 3), above the fake judge's four characters to a token and near the worst real case, and the real counts replace it after the call. The check's "per trial" is taken as a whole run over many evaluations and "per evaluation" as one repository, so the two counters reset separately; a reserve still held counts against a cap, so an overlapping call cannot slip past. A usage above its reserve is possible only if the API breaks its output limit; it is charged and flagged, not refused after the fact. The prices file's format is built and tested with synthetic numbers; no real price, cap or model id is written anywhere.
- **Options:** dollar caps (refused: Ghost's question 2 asks for token caps first); a cap checked only after a charge (refused: the check says reserve first); raising on an overrun (refused: it would lose the run that holds the evidence); a default cap in code (refused: caps are Ghost's, None means no cap).
- **Resolved:** `agent_runner/budget.py`, `tests/test_budget.py`, a synthetic prices fixture, judge-harness.md, 2.2g to DONE.md, HANDOFF names 2.2h and what waits on Ghost. No new decision, no Java changed.
- **Touched:** agent-runner/, docs/how/judge-harness.md, TODO.md, docs/DONE.md, docs/HANDOFF.md.

### J-118 Navigation review: nine debriefs read; numbering of slices, the journal id, Python outside the module map, Python goldens, evaluation and trial, and a finished worktree's removal now written
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The navigation review due (E-068): nine commits since the last (J-108), the debriefs of 2.2a to 2.2g, the tithe pass 8 and the previous review's own.
- **Found:** Converging: (1) the next J id and the Class list took a read of the journal's header (2.2g, tithe 8, the previous review); the header has the rule, do-a-todo-step.md did not point to it: fixed. (2) Python and `web/` are outside the module map (slicing agent, 2.2a): judge-harness.md said it, add-or-change-a-module.md did not: fixed. (3) Python goldens and the units of the Budget (2.2c, 2.2g; and the 2.2e/2.2f entries): judge-harness.md holds the Python golden command since 4680199 (`git log -S'EVALUATOR_WRITE_GOLDENS'`), commands.md under Goldens did not: one line added; "evaluation" (one repository) and "trial" (a whole run) added to judge-harness.md and DESIGN question 2. Single, plainly facts: a sliced item's numbering (slicing agent; J-109 taught TodoTest the letter): one clause in do-a-todo-step.md. Already fixed: the `-B` of `-I -B` (judge-harness.md, 2.2a's own commit); mutating `cli` from a plain clone in a worktree (agent-environment.md, 525a109); the hash a replay is keyed by and `RenderedPrompt` (judge-harness.md, 54583b3). Noted, not acted on (one agent each): the decision's required fields (do-a-todo-step.md says "every field", the header shows the form); a table of vocabularies; how a Check-style test is registered in DocTests; the cold first run of the tithe's metrics (29 s against 13 s warm); the Bash classifier failing in a row (not ours). The previous review's own question, whether a point fixed in a test message and hit again still counts: it counts as a new hit (the message did not save the run), recorded here, no rule written. Recorded suggestions: (a) worktrees and clones pile up under /home/user (13 worktrees, one clone, about 16 MB each): yes, one line in commit-and-push.md, the session that merged removes its worktree and branch after the push; none removed by this review. (b) the template body is not hashed and the golden holds it, so a change needs a version bump: yes, already written in judge-harness.md (2.2e bullet) and J-114. (c) the prompt CLI runs without `-I`, tests with `-I -B`: already written there. (d) the invented conventions: now in commands.md and judge-harness.md, as above. (e) J-104's Class `feature` is outside the list and JournalTest checks only that a Class exists: do-a-todo-step.md already says use the nine (J-108); a check of the values would need an exception for J-104 (history is not rewritten) and a test change, which a review does not make: not done, left to the next agent that touches JournalTest, recorded here. Open questions for Fable: none.
- **Options:** a table of vocabularies in DESIGN (refused: one agent, and the kind's home is the schema); a JournalTest Class check (refused for a review, see (e)).
- **Resolved:** do-a-todo-step.md, add-or-change-a-module.md, commands.md, judge-harness.md, commit-and-push.md, DESIGN question 2; NAVIGATION-NOTES.md wiped to its header. No code changed.
- **Touched:** docs/how/ (five files), docs/DESIGN.md, docs/NAVIGATION-NOTES.md, docs/JOURNAL.md.

### J-119 TODO.md 2.2h: three runs, the mean, the vote and Opus on a split
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2h: the harness runs the prompt three times on Haiku, takes the mean and the vote, and calls Opus once on a split, keeping every run behind each field.
- **Found:** The Check names two rules the text does not finish. (1) Quality's 1 to 5 integer is not a "score of a metric" on the 0 to 100 scale: 15 of 100 is 0.6 of a step there, so the 15 rule would call Opus on any difference; it is an ordinal label the calibration compares exactly and within one, so it takes the median and calls Opus when more than one step apart (a call for Ghost, flagged). Confidence and evidence follow their score or tier. (2) A retry needs a run number of its own or a replay could not tell it from the first attempt, so Haiku slot k is run k and its retry run 3 + k, Opus run 1 and its retry run 2. The parser already refuses a run lacking a metric, so a "missing metric" is an unparsable run and counts as missing. A folder path stands only when more than half of the usable runs list it; the voters are the runs that list it, and two voters that differ have no majority. Ghost decided that a category without a majority goes to Opus, whose answer stands (E-083). The real model ids are still open (question 3); the defaults `claude-haiku-5-5` and `claude-opus-5-5` are marked as such. No Java changed, so no mutation run.
- **Options:** the 15 rule scaled to 1 to 5 (refused: any difference triggers); the mean rounded for Quality (refused: it is an integer label); Opus failing twice raising an error (refused: the fields are returned unresolved, partial, so the runs are not lost).
- **Resolved:** `agent_runner/ensemble.py`, `tests/test_ensemble.py` (34 tests: 15 and 16, the tie, two of three, retry, two unparsable, a metric missing, Quality, budget refusals counted by the fake's calls, a replay), E-083, question 9 answered in FOR-GHOST.md, judge-harness.md, 2.2h to DONE.md, HANDOFF names 2.2i and what waits on Ghost.
- **Touched:** agent-runner/, theGhost.md (E-083), docs/DECISIONS.md, docs/how/judge-harness.md, TODO.md, docs/DONE.md, docs/DESIGN.md, docs/FOR-GHOST.md, docs/HANDOFF.md.

### J-120 TODO.md 2.2i: agreement with the calibration set
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2i: the figures of a judgment against the 30 labels, beside their constant-answer baselines, the record under the prompt's hash and the live test.
- **Found:** Ghost defined agreement as both tests together (raw 80 % and a margin over the baseline) but did not size the margin. A fixed +10 points is impossible at a 90 % baseline (Quality within one), so the margin is a share of the gap to 100 %, one half by default, a parameter flagged for Ghost: it gives 29 of 30 for Quality within one, 27 for kind, and for Ambition and Quality exactly the 80 % floor (24) binds. The exact arithmetic is `Fraction`, so 24 of 30 is exactly 80 %. Quality within one has a best constant (3, within one of 27), not a most common label, so its baseline is the best constant. `is_live` recomputes from the record's counts instead of trusting its flag, so a changed margin applies to old records. A partial or refused result is a miss, counted apart.
- **Options:** a fixed margin in points (refused: impossible at 90 %); trusting the stored live flag (refused: a hand edit or a new margin would not show); reading values out of a partial result (refused: Ghost's rule is a miss).
- **Resolved:** `agent_runner/agreement.py`, `tests/test_agreement.py`, E-084, question 8 answered in FOR-GHOST.md (margin size to confirm), judge-harness.md, 2.2i to DONE.md, HANDOFF names 2.2j. No record committed, no Java changed so no mutation run.
- **Touched:** agent-runner/, theGhost.md (E-084), docs/DECISIONS.md, docs/how/judge-harness.md, TODO.md, docs/DONE.md, docs/DESIGN.md, docs/FOR-GHOST.md, docs/HANDOFF.md.

### J-121 TODO.md 2.2j: the kernel counts the folders the judge calls own code
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2j: `evaluator score --judged <judgment.json>` measures Size, Complexity's total and depth and Maintainability's parts over the files the judge's most specific folder calls own code, and the card names the prompt hash and the models.
- **Found:** Three things the check left open. (1) The 2.2d answer has no prompt hash or models, and the ensemble `Result` is not that shape, so the file is a wrapper of three members round the answer (E-085); a bare answer and an ensemble result are refused by name. (2) Maintainability's tests part is test lines over the other counted lines, so setting own tests aside with the other classes would read 0 for every judged repository; own tests are kept for the ratio alone, and the judge's class decides over a file's name (a file in no folder is a test by the kernel's name rule). (3) `Complexity.Report` and `TestFiles.ratio` needed two small additions in `analyze/` that the task did not list (`Report.restrictedTo`, `ratio` with a predicate); the navigability checklist keeps the whole inventory, since the judge scores it itself. Own code `docs/tools` inside a `docs` folder, a generated `src/gen` inside an own-code `src`, and a folder in none are all in the fixture.
- **Options:** classing own tests as set aside (refused: the part reads 0); the file's name over the judge's class (refused: it would ignore the judge's reading of the folder); narrowing the checklist too (refused: not asked, and it would change a number the judge also scores); accepting a bare answer with no hash (refused: the card must name the prompt, SC-05); a `kind` that withholds anything (2.2k's).
- **Resolved:** `ingest/` `Judgment.java` and `OwnCode.java`, `Pipeline.code` through a split, `Main --judged`, `score/Judged.java` with `CardJson`/`CardMarkdown` overloads; tests (`OwnCodeTest`, `JudgmentTest`, `ComplexityReportTest`, `TestFilesTest`, `CardRenderingTest`, `MainTest`, `CardGoldensTest`) with `test/fixtures/judged/` worked by hand (22 lines, depth 2, tests 0.35, weighted 0.8375 against 38, 3, 0.267, 0.81675 without `--judged`); new goldens for the judged card, none of an existing card moved; E-085; command-line.md and four other how-files; 2.2j to DONE.md, HANDOFF names 2.2k.
- **Touched:** ingest/, analyze/, score/, cli/, test/, theGhost.md (E-085), docs/ (DECISIONS, KERNEL-MAP, MODULES, modules.json, SCORE-CARD, DONE, JOURNAL, HANDOFF, MUTATION, how/), TODO.md.


### J-122 TODO.md 2.2k: the card's rules for the judged kind
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2k: a judge's kind, Ambition and Quality change a card only where the prompt's recorded agreement says that field is live; generated is declined, a plan carries no Size, Complexity or Maintainability.
- **Found:** Four things the check left open. (1) Where the code lives: `score/` reads facts only (MOD-03) and may not make one, so the facts and the rules are in `ingest/` beside `Judgment`, which already reads the judge's file; the record's file is read by `cli/`. (2) The existing judged golden's prompt has no record, and "the card says unverified for that field" would have moved it: a missing record is therefore silent on stdout (the metrics already read not measured, SC-07) and said on stderr. (3) E-082 says a clone call alone is not a decline and E-056 names no clone outcome: clone is scored with a note, flagged for Ghost. (4) The liveness rule is `agreement.is_live`'s, and Java and Python were compared on 12,840 combinations of counts, margins and floors: no difference. The 30 labels hold no generated repository, so a live kind figure does not show that "generated" is recognised, and a decline rests on it all the same (FOR-GHOST.md). No formula reads the new facts (2.3, 2.4), so the card's Ambition and Quality stay not measured.
- **Options:** declining on a kind that is not live (refused: the gravest effect of a judgment needs a verified field); gating Quality by Quality exactly (refused: within one is the calibration's headline); exit status 0 for a decline (refused: a caller reading stdout for a card would take the message for one); a note for a missing record on the card (refused: it moves a golden); a new module (refused: `ingest/` already reads the judge's file).
- **Resolved:** `AgreementRecord`, `JudgedRules`, `Declined`, `Judged` notes, `--records`, exit status 2; eight hand-written cases with synthetic records, each a golden in three forms; E-086 (proposed), command-line.md, SCORE-CARD.md, MUTATION.md run 21 (ingest 270 of 271, score 178 of 178, cli 112 of 114, the survivors old rows), FOR-GHOST.md, 2.2k to DONE.md, HANDOFF names 2.2l.
- **Touched:** ingest/, score/, cli/, test/, theGhost.md (E-086), docs/ (DECISIONS, KERNEL-MAP, MODULES, modules.json, SCORE-CARD, DONE, JOURNAL, HANDOFF, MUTATION, FOR-GHOST, how/command-line.md, how/judge-harness.md), TODO.md.

### J-123 Handoff for the session with the API key
- **Who:** Claude Sonnet 5.5, with Ghost. **Class:** direction.
- **Intent:** Hand 2.2l (the live judge) to a new session in which Ghost's API key is in the environment.
- **Found:** The Network-secrets form would not save a Bearer secret with header `x-api-key` for `api.anthropic.com` although its preview
  resolved; Ghost chose an environment variable, `ANTHROPIC_KEY`. A key sent through a session's proxy cannot be shown to work for that host
  from a cloud session (the same trap as J-101), so the new session checks the variable first.
- **Resolved:** docs/HANDOFF.md carries the key's route, the questions still open (caps, transport, model ids and prices), the cheaper
  session-agent route offered, what lived outside the repository, and what the provenance study showed.
- **Touched:** docs/HANDOFF.md.


### J-124 TODO.md 2.2l: the live judge, the one door to the API
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build 2.2l under Ghost's answers of 2026-10-26: a standard-library live `Judge` in `transport.py`, every call through a `Budget` that now counts dollars, his caps and prices as data.
- **Found:** Four things the brief and the check left open or in tension. (1) The loopback test needs `http.server`, which the guard bans everywhere but the door: the narrowest change is a second allowed file, `tests/test_transport.py`, allowed `http` only; `urllib` for a URL parse in that test was refused by the guard at once, so the test builds its URL by hand. The fake key is set with `mock.patch.dict("os.environ", ...)` on the name as a string, which the guard reads as no environment access. (2) The "unreadable key" rules reach further than printing: `http.client` echoes a bad header value in its `ValueError`, so a key with a space or newline would have leaked through an exception; the door checks the key's shape first and refuses it unshown. (3) Ghost was told about 4,000 output tokens a call; both models think adaptively, the thinking is billed as output and counts inside `max_tokens`, so the limit is 8,000 (Opus at most $0.16 a call, two fit an evaluation's $0.50). (4) Above Haiku's 100,000-token prompt tier the price doubles and five-fold on output: the prices file holds one price, so a longer prompt is refused before the call, and a real input above the tier (the estimate is only a reserve) is charged at the file's price, which is a floor, and flagged `above_price_tier`. Not found: whether the API accepts every keyword in the answer schema (`minimum`, `maximum`); only the smoke call, which the orchestrator runs, can say.
- **Options:** the SDK (refused: a dependency, E-033); `urllib.request` for the call (refused: a proxy tunnel with `http.client` is explicit and the loopback rule is plain); a token-only cap (refused: the models differ four-fold in price); a constructor that takes the budget optionally (refused: a judge with no budget must be impossible); retrying a certificate failure or a refused tunnel (refused: it cannot change).
- **Resolved:** `transport.py`, `caps.py`, `budget.py` (dollar caps, `PromptTooLong`, `above_price_tier`), `guard.py` (`LOOPBACK_TEST`), `caps.json`, `prices/2026-10-26.json`; 26 loopback tests, 5 caps tests, 13 more budget and guard tests, the smoke test written and skipped without its switch and a key; E-087; judge-harness.md, HANDOFF.md, FOR-GHOST.md (the ids confirmed, the monthly spend limit to set); 2.2l to DONE.md. The live smoke run is for the orchestrating session. The orchestrator's review: the smoke run's first call was refused with HTTP 400 (the API takes no `minimum`/`maximum` on an integer); `transport.api_schema` now drops the numeric ranges from the copy sent, `judgment.parse` still enforces them, and the second smoke run passed, one Haiku answer that parses.
- **Touched:** agent-runner/, theGhost.md (E-087), docs/ (DECISIONS, DONE, JOURNAL, HANDOFF, FOR-GHOST, NAVIGATION-NOTES, KERNEL-MAP if changed, how/judge-harness.md), TODO.md.

### J-125 Tithe: Pass 9 in AGENT.md, six docstrings brought up to date
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The tithe due (E-015): ten commits since the last, past the limit of eight (2.2l's two among them).
- **Found:** In a plain clone with `EVALUATOR_GITHUB_TOKEN` unset, `gradle metrics --args=--time` read the fast loop 28 s on the cold first run (daemon start), then 16, 15, 16 and 16 s, none failed, so no race in a test (E-072). It was 14 s at Pass 8: the Java and Python tests of 2.2l (26 loopback tests, 5 for the caps) account for the rise; the loop stays under its 20 s. Every other held metric is within target; largest source file 283 lines (test/SupplyChainTest.java), mandatory context 794 words, docs per source line 0.38 (0.41 at Pass 8: the Python of 2.2l is source). The metrics listed no function needing a comment, so no Java comment was added. In `agent-runner/` six docstrings and comments written before 2.2l still said the door was future (`__init__.py` "does not exist yet", `judge.py` "will be", `judgment.py` "is later given", `summary.py` "a later part", `budget.py` "in 2.2l"); they now say it is built. Eight commits had passed since the last navigation review (nine with this tithe); no review was done in this pass.
- **Options:** none.
- **Resolved:** Pass 9 column in docs/AGENT.md, five docstrings and one comment in agent-runner/agent_runner/ (wording only), HANDOFF's cadence line corrected. No behaviour changed, no golden moved.
- **Touched:** docs/AGENT.md, docs/HANDOFF.md, docs/JOURNAL.md, docs/NAVIGATION-NOTES.md, agent-runner/agent_runner/ (`__init__.py`, `budget.py`, `judge.py`, `judgment.py`, `summary.py`).

### J-126 Navigation review: six debriefs read; the next decision's E-number is found by its last heading, the rest already fixed or single
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The navigation review due (E-068): the six entries since J-118: the previous review's own debrief, 2.2h, 2.2i, 2.2k, 2.2l and the tithe pass 9 (J-125).
- **Found:** Converging (2.2i, 2.2l): writing a decision entry in `theGhost.md` left details to find: how the last E-number is found (2.2i) and where a mixed status goes (2.2l). Fixed: do-a-todo-step.md now says the next E-number is one past the last `### E-` heading and gives the `git grep`. The mixed status is not fixed here: the form is in theGhost.md ("How an entry is written", Status takes one value), so a rule for it would change a decision's form; to Ghost (the 2.2l suggestion: a mixed status is "in force" with the unconfirmed parts named in Source). Already fixed: `mkdir -p build` before a redirect (2.2h) is in commands.md since be1e96d (`git log -S'`mkdir -p build` first'`; J-086), and agent-environment.md has it for a mutation run; `patch.dict("os.environ", ...)` under the guard (2.2l) and the `-p 'test_x.py'` form of running one Python test file (2.2i) are in judge-harness.md since e965180 (2.2l's own commit; the `-p` form is shown there for the transport tests, not stated as a general rule). Noted, not acted on (one agent each): where a rule over a judge's answer lives, ingest/ beside `Judgment` (2.2k; E-086 and modules.json settle it); `DocTests.scoreCard` and `metrics` failing until SCORE-CARD.md's module row and `gradle kernelMap` are updated (2.2k; add-a-fact.md step 3 and 7 and finish-work.md hold both); a plain-clone recipe for an uncommitted change by `git ls-files -m -o --exclude-standard | tar` (2.2k; agent-environment.md says to copy the changed files across); `git grep` of a slice's number in agent-runner/ for stale future-tense docstrings, and naming the empty "Over 15 lines or 6 paths" heading as the nothing-to-comment answer (tithe 9); the FOR-GHOST question text wrapping across lines (2.2i); the entries not naming the commit they were written in (previous review). One point each, none a stale line or wrong path. The previous review's own item, the duplicate holding of the prices format in judge-harness.md and the budget.py docstring (2.2l), is one agent's, noted. Open questions for Fable: none asked in this commit (the agent that wrote it cannot start agents); the mixed status above waits for Ghost.
- **Options:** a general single-test-file clause in judge-harness.md line 15 (refused: one agent, and the transport bullet shows the form); a docstring-grep line in the 2.x closing step (refused: one agent).
- **Resolved:** docs/how/do-a-todo-step.md (the E-number clause); HANDOFF's cadence line; NAVIGATION-NOTES.md wiped to its header. No code changed.
- **Touched:** docs/how/do-a-todo-step.md, docs/HANDOFF.md, docs/NAVIGATION-NOTES.md, docs/JOURNAL.md.

### J-127 Ghost's call on a decision partly his and partly an agent's
- **Who:** Claude Opus 5.5. **Class:** decision.
- **Intent:** Settle the point J-126 left for Ghost: theGhost.md's Status takes one value, and E-087 holds both Ghost's answers and the agent's own choices.
- **Found:** E-087 is `in force` with the agent's choices (the retry numbers, the base-URL and proxy rules, the key-shape check, the error scrubbing) named in its Source.
- **Options:** a mixed status, which changes the entry form (refused by Ghost); keep one value.
- **Resolved:** Ghost, 2026-10-26: keep `in force` with Source naming the agent's parts for him to confirm. Written in docs/how/do-a-todo-step.md's decision step. No code changed.
- **Touched:** docs/how/do-a-todo-step.md, docs/JOURNAL.md.

### J-128 The context canary: every reply starts with "Context Canary"
- **Who:** Claude Opus 5.5. **Class:** decision.
- **Intent:** Ghost's request of 2026-10-29: a fixed opening word on every reply, so an agent that has stopped following its instructions shows it a turn or two before things go wrong; mostly for the Sonnet agents' hand-backs.
- **Found:** docs/AGENT.md is the metrics contract, read at a tithe, not on entry; CLAUDE.md is the one file every agent loads. The line adds about forty words of mandatory context, under the 1500 limit.
- **Options:** docs/AGENT.md (refused: an agent rarely reads it); CLAUDE.md (taken, with Ghost's agreement).
- **Resolved:** E-088; CLAUDE.md's "Three rules" became "Four rules" with the canary line. The orchestrating session checks hand-backs for it. No code changed.
- **Touched:** CLAUDE.md, theGhost.md, docs/DECISIONS.md, docs/JOURNAL.md.

### J-129 Ghost's answers: the margin, 2.2j's two rules, E-087's details, the retry steps, the token
- **Who:** Claude Opus 5.5. **Class:** decision.
- **Intent:** Record Ghost's answers of 2026-10-29 to the open calls in FOR-GHOST.md.
- **Found:** Confirmed as proposed: E-084's margin (half the gap), E-085's own-tests rule and wrapper, E-087's refusal of an unencrypted key and its scrubbing of errors. Changed: the retry sleeps, 3, 9 and 27 seconds and no `retry-after` (Ghost: a jam is a spike or a flaky link). New: a key received unencrypted is taken and its sender warned, for endpoints not yet built. Decided: the GitHub token optional, private repositories later through a GitHub App.
- **Options:** keep honouring `retry-after` above the steps (not asked for; the steps are the rule).
- **Resolved:** E-089 (supersedes E-087's retry sleeps), E-090; `transport.py` and its tests; the Source lines of E-084, E-085, E-087; FOR-GHOST.md, HANDOFF.md, judge-harness.md. E-090's code is still to build.
- **Touched:** agent-runner/agent_runner/transport.py, agent-runner/tests/test_transport.py, theGhost.md, docs/DECISIONS.md, docs/FOR-GHOST.md, docs/HANDOFF.md, docs/JOURNAL.md, docs/how/judge-harness.md.
### J-130 TODO.md 2.2m: the calibration driver, and the per-evaluation cap that would have held across all 30
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build the tool 2.2m's first run uses (brief, summary, prompt, ensemble, agreement record for a folder of checkouts); the run itself is not done and TODO.md 2.2m stays open.
- **Found:** `agreement.judge_all` never called `begin_evaluation()`, so the $0.50 evaluation cap would have accumulated over the 30 repositories and refused after a few; a test with the cap set between one repository's spend and two shows it (the control in `test_calibrate.py` refuses the second repository without the reset). Also: `ensemble` catches only `BudgetRefused`, so a `PromptTooLong` or a `MissingRecording` would end the whole run and lose the answers already paid for.
- **Options:** the loop in the driver (refused: `judge_all` stays the one place that loops); an optional `budget` on `judge_all` (taken), with `errors` and `catch` so one id's budget or recording error is a missing id, and a stop after a trial-cap refusal. A record written into `records/` by any run (refused: a partial run would make the hash look not live by a short count; only a live run over all labels with none missing is written there).
- **Resolved:** `calibrate.py` (the CLI is run as a child by `cli_brief`, the key variable removed, tests inject the brief), `agreement.judge_all`, 19 tests, judge-harness.md, HANDOFF.md. No decision entry: nothing here changes an existing decision.
- **Touched:** agent-runner/ (`calibrate.py`, `agreement.py`, `tests/test_calibrate.py`), docs/how/judge-harness.md, docs/HANDOFF.md, docs/NAVIGATION-NOTES.md, docs/JOURNAL.md.

### J-131 The kernel crashed on a source file holding an emoji: the binding's offsets index modified UTF-8
- **Who:** Claude Sonnet 5.5. **Class:** failure-missed.
- **Intent:** Fix the crash the first calibration run (2026-10-29) found: `evaluator brief` exited 1 on 5 of 30 real repositories with `StringIndexOutOfBoundsException` at `TreeSitterAdapter$Source.text`, in JavaScript, TypeScript and Python files alike.
- **Found:** Confirmed the hypothesis from synthetic text: `TSParser.parseStringEncoding` passes the Java string to native code as modified UTF-8, where a character beyond the Basic Multilingual Plane is 6 bytes, not 4, so a Python file of 37 UTF-8 bytes holding two emoji read as ending at byte 41 (two extra bytes per emoji), while `Source` held the real bytes. The binding's `parse(byte[], oldTree, reader, encoding)` indexes the real bytes, but it hands the reader the very array it was given as the buffer (a first try that copied into that buffer overwrote the file's first byte and moved the duplication golden's lines by one: caught by `DuplicationTest`), and a null reader segfaults the JVM. No test had a character outside ASCII and the Basic Multilingual Plane, so none caught it.
- **Options:** parse the real bytes through the byte entry with a copy as the parse's array (taken: root fix, no dependency change); build `Source` from the modified UTF-8 (refused: every slice would hold a different text from the file's); map offsets (refused: more code than the cause needs).
- **Resolved:** `TreeSitterAdapter.parse` passes `source.bytes().clone()` and a reader over `source.bytes()`; `EmojiOffsetsTest` (Python, TypeScript, JavaScript: an emoji in a string and a comment, before and inside a function; imports; a 5000-line file; the file's own text kept) fails on the old code and passes now. No golden moved (ASCII input parses to the same tree). Run 22 in `docs/MUTATION.md`; F-001 in `docs/FINDINGS.md`.
- **Touched:** analyze/evaluator/analyze/TreeSitterAdapter.java, test/evaluator/analyze/EmojiOffsetsTest.java, docs/KERNEL-MAP.md, docs/MUTATION.md, docs/FINDINGS.md, docs/JOURNAL.md.
- **Caught by:** the first calibration run on real repositories.

### J-132 The reserve estimate undercounted real tokens: bytes / 3 became bytes / 2
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** Make the budget's reserve bound a call again after the first live run.
- **Found:** Of 77 live calls, 13 used more than their reserve (prompt estimate plus the 8000 output limit). Real input tokens ran about 1.4 times the estimate (23588 real against 17118 estimated, 27806 against 17535); the prompts are code-heavy summaries at about 2.1 bytes to a token. Over-reserve usage is charged and flagged, never refused, so no cap was broken. Even bytes / 2 stays 6% under the second example (about 52605 bytes, 26303 estimated against 27806 real), which is why the output limit's margin, not the estimate alone, bounds a call.
- **Options:** ceil(bytes / 2) (taken, Ghost's choice); a lower divisor or a measured factor (not asked for).
- **Resolved:** `estimate_tokens` is ceil(bytes / 2), at least 1, the measured numbers in its docstring and in `docs/how/judge-harness.md`; the tests that pinned the rule and the default test prompt (20 bytes, still an estimate of 10) updated. Nothing else in `budget.py` changed.
- **Touched:** agent-runner/agent_runner/budget.py, agent-runner/tests/test_budget.py, agent-runner/tests/test_transport.py, docs/how/judge-harness.md, docs/JOURNAL.md.

### J-133 The kernel crashed on a TypeScript import with a comment inside its braces
- **Who:** Claude Sonnet 5.5. **Class:** failure-missed.
- **Intent:** Fix the crash the second calibration run (2026-10-29) found on one real TypeScript repository: `evaluator brief` exited 1 with `NullPointerException: Cannot invoke "org.treesitter.TSNode.getStartByte()" because "node" is null` at `TreeSitterAdapter$Source.text`, from `TypeScriptImports.specifiers`.
- **Found:** Confirmed from synthetic source: `import { a, // note\n b } from "./m";` and the block form `/* x */` crash the old code. tree-sitter gives a comment as a named child of `named_imports`, `specifiers` took every named child for an `import_specifier`, and a comment has no `name` field, so `source.text(null)` threw. An `ERROR` node is not a second cause: a file with one is set aside as unread before the imports are read. `JavaScriptImports` shares the TypeScript reader (the same crash) and its `require` patterns skip a comment already; `PythonImports` reads only `name`-field children and a comment is none, so its parenthesised `from m import (a,  # note` was safe (a test says so). `namespace_import` took its named child 0, which a comment could in principle be; it now looks for the identifier.
- **Options:** skip a specifier with no name (refused alone: it keeps a wrong reading of what a specifier is); read only `import_specifier` children (taken), with a null guard on the name for a tree that holds one without.
- **Resolved:** `TypeScriptImports.specifiers` reads only `import_specifier` children; new `namespace` reads the identifier child. `ImportCommentsTest` (line and block comments before, between and after specifiers, only a comment, default and namespace clauses, type imports, re-exports, JavaScript with a require pattern, Python) fails on the old code in six of seven tests. No golden moved. Run 23 in `docs/MUTATION.md`.
- **Touched:** analyze/evaluator/analyze/TypeScriptImports.java, test/evaluator/analyze/ImportCommentsTest.java, docs/KERNEL-MAP.md, docs/MUTATION.md, docs/JOURNAL.md, docs/NAVIGATION-NOTES.md.
- **Caught by:** the second calibration run on real repositories.

### J-134 TODO.md 2.2m: the first run on the calibration set
- **Who:** Claude Opus 5.5. **Class:** measurement.
- **Intent:** Judge the 30 labelled repositories with the live harness and record the agreement under the prompt's hash (2.2m).
- **Found:** Run 1 judged 25 of 30: `evaluator brief` crashed on an emoji in a source file (fixed, J-131) and the reserve estimate ran about 1.4 times low (fixed, J-132). Run 2 judged 29: a comment inside a TypeScript import's braces crashed the import reader (fixed, J-133). Run 3 judged all 30 (29 ok, 1 partial, after an answer cut at the 8000-token output limit), $0.95: 93 Haiku calls $0.24, 4 Opus calls $0.70; no refusal, no overrun. Agreement: Ambition 21 of 30 (needs 24; baseline 14), Quality exactly 23 (needs 24; 17), Quality within one 28 (needs 29; 27), kind 22 (needs 27; 23). No field is live; kind is under its constant answer, and Quality within one swung 29 to 28 between runs 2 and 3, so one run's figure moves by a repository or two.
- **Options:** none: 2.2n iterates the prompt against this record.
- **Resolved:** `agent-runner/records/5b05dcacec1e82b42caf6b165744b39ce5a4a57320f4598301a489282dc090bb.json` committed (ids, labels, judged values only, E-014); 2.2m moved to docs/DONE.md; HANDOFF.md. The repositories stay outside the repository and are deleted once Ghost says 2.2n no longer needs them.
- **Touched:** agent-runner/records/, TODO.md, docs/DONE.md, docs/JOURNAL.md, docs/HANDOFF.md.

### J-135 Tithe: Pass 10 in AGENT.md, three test functions commented, stale 2.2m wording brought up to date
- **Who:** Claude Sonnet 5.5. **Class:** measurement.
- **Intent:** The tithe due (E-015): nine commits since the last (J-125), the calibration driver, the emoji and import-comment fixes, the reserve estimate and 2.2m among them.
- **Found:** In a plain clone with `EVALUATOR_GITHUB_TOKEN` unset, `gradle metrics --args=--time` read the fast loop 24 s on the cold first run, then 16, 15, 15 and 14 s, none failed, so no race in a test (E-072). Every held metric is within target: fast loop under 20 s, largest source file 283 lines (test/SupplyChainTest.java), mandatory context 829 words (794 at Pass 9; the "Context Canary" rule, E-088), docs per source line 0.39, no function over 20 paths, no duplicate helper, no stale generated artifact. The metrics listed three functions in `EmojiOffsetsTest` (J-131) with no comment; each has one now. Stale wording found by reading the files the five changes touched: HANDOFF.md still headed a section "2.2m ... which waits on the 30 repositories" and said 2.2m's run was next and "to be redone", and its two cadence lines disagreed with the top section on what is next (2.2n); `agreement.py`'s docstring and judge-harness.md called a real record "the question of 2.2m"; judge-harness.md and command-line.md said 2.2m's run would write the `--judged` wrapper (it records agreement and `results.json`, not a wrapper; the wrapper is still written by hand); FOR-GHOST.md said 2.2m waits on Ghost. J-131 and J-133 each suggested a line in add-a-language-adapter.md and none had been written; it now says the parse is fed the file's real bytes on a copy and that a named child may be a comment. Left alone, as history: DESIGN.md's open question 1 ("until it is settled ... 2.2m and 2.2n wait"), whose answer is in E-087. The docstrings of `calibrate.py`, `budget.py` and `TreeSitterAdapter.java` match their code.
- **Options:** none.
- **Resolved:** Pass 10 column in docs/AGENT.md, three Javadoc comments in test/evaluator/analyze/EmojiOffsetsTest.java, wording in HANDOFF.md, judge-harness.md, command-line.md, FOR-GHOST.md and agreement.py's docstring, the two-line note in add-a-language-adapter.md, HANDOFF's cadence line (the tithe done; the navigation review one commit away). No behaviour changed, no golden moved.
- **Touched:** docs/AGENT.md, docs/HANDOFF.md, docs/JOURNAL.md, docs/NAVIGATION-NOTES.md, docs/FOR-GHOST.md, docs/how/judge-harness.md, docs/how/command-line.md, docs/how/add-a-language-adapter.md, agent-runner/agent_runner/agreement.py, test/evaluator/analyze/EmojiOffsetsTest.java.

## 2026-11-01

### J-136 The dependency-update posture adopted: a change is read before it comes in, and Ghost is asked only the rare question
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Adopt for this repository the posture Ghost first took in another project and passed on for each project to apply to itself (his three answers, 2026-11-01): the decision rule, two read-only agent files each pinned by hash, the rule and the scan built now.
- **Found:** The repository pins 90 artifacts from public registries: 24 jars (`third_party/MANIFEST.md`), 55 npm packages (c8 12.0.0 and 54 dependencies), 6 PyPI wheels, 2 tarballs (Node, Maven), 2 base images by digest and 1 GitHub Action. The only line asking for Ghost's approval of every dependency change was CLAUDE.md's, the baseline's copy (E-033, E-073). Three layers of the posture do not apply as written: a jar has no install step (layer 2), PyPI does not publish who uploaded a file (layer 4), and no exact advisory database covers a tarball or an image (layer 3).
- **Options:** supersede E-033 in part (taken: it holds the baseline and the line came from it); a new column in MANIFEST.md for the record (refused: the manifest holds only jars, and the record also needs npm, PyPI, tarballs, images and the action); a record row per pin, keyed by the version or the first twelve hex digits of the hash (taken, so a changed pin must change its row); treat today's pins as read (refused): they are entered as pinned before E-091 and not yet reviewed, the first review of each reads from the pin.
- **Resolved:** E-091 (supersedes E-033 in part); CLAUDE.md's line and a routing row; `docs/how/update-a-dependency.md`, the posture adapted to npm, PyPI, Maven Central, tarballs and images; `third_party/REVIEWED.md` with 90 rows; `test/DependencyReviewTest.java` (a row for every pin, none stale) and a check in `SupplyChainTest.directive` that CLAUDE.md's dependency rule names E-091 and the how-file and no longer asks for approval of every change. No pin, hash, lockfile or allow-list changed.
- **Touched:** theGhost.md (E-091, E-033's status), CLAUDE.md, docs/how/update-a-dependency.md, third_party/REVIEWED.md, test/DependencyReviewTest.java, test/DocTests.java, test/SupplyChainTest.java, docs/DECISIONS.md, docs/JOURNAL.md.

### J-137 The rule-based scan: a standard-library Python script with synthetic-package tests, run by the gate
- **Who:** Claude Sonnet 5.5. **Class:** decision.
- **Intent:** Build E-091's Step 2: a script that pairs renamed bundle chunks, diffs two unpacked folders and runs the posture's fixed rules over what the new side adds, printing each hit with its file and line.
- **Found:** `scripts/` holds Java for gradle and a Java module needs an entry in `docs/modules.json`, which is an architecture decision for Ghost (`docs/how/add-or-change-a-module.md`). Python outside the module map has a precedent in `agent-runner/`, held by `AgentRunnerTest` running `unittest discover`. The scan has no business in the judge harness, whose guard (`guard.py`) is about the network and the key. A rule written with the posture's npm wording needed three additions for the other registries: the JVM's process classes, a Python `setup.py` or `.pth` file, and reading a class file as its printable strings. `+` is a base64 character, so `1+1+1+...` in a test sample fired the encoded-string rule: the tests use text with spaces. An early version scanned before it refused an `--out` inside the repository.
- **Options:** Java in `scripts/` (refused: its tests would need a module or a place in the map, and a decision); Python in `agent-runner/` (refused: not the harness); a folder of its own, `dependency-review/`, with its tests beside it and the gate in `DependencyReviewTest` (taken). The scan refuses, rather than follows, a symlink: it is reported as a hit and never opened (taken).
- **Resolved:** `dependency-review/dependency_scan.py` and `test_dependency_scan.py` (32 tests: chunk pairing and its ambiguity, each of ten rules firing on an added line and not on an unchanged or removed one, an added and a changed install script, new large and minified files and their boundaries, a base64 string at 200 and 199, a binary file's strings, a symlink to a file and to a folder, nothing executed from a package that tries to run itself, the script's own imports, refusals). `gradle mutate` is for Java, so the module was mutated by hand, about thirty-five changes to the code: every one that survived got a test, or was an equivalent mutant and the code was simplified (a dead `dirs.remove`, a condition no input reaches). `AgentRunnerTest.discover` is package-private so the one gate runs both Python suites.
- **Touched:** dependency-review/, test/DependencyReviewTest.java, test/AgentRunnerTest.java, docs/how/update-a-dependency.md, theGhost.md (E-091's Held by), docs/JOURNAL.md.
