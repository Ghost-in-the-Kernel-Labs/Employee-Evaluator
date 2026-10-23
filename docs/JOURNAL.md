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
