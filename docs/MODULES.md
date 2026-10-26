# Modules: the kernel by folder

<!-- Written by `gradle modulesDoc` from docs/modules.json; edit the JSON, never this file. -->

An agent starts at the module level, not the function level (Suite Builder's finding, E-004). Each
folder of the kernel: what it is for, what goes in and comes out, what it may import, what imports it,
and where to start reading. For a task, `CLAUDE.md` says which modules to open and what proves a
change. `test/ModulesTest.java` holds every import in the source to the JSON, and this file to it.

The pipeline: `ingest` reads → `analyze`, `history` and `github` measure → `score` combines facts into the card → `cli` names the work. `rubric` is the vocabulary all of them speak.

### `rubric/`
- **Purpose:** The vocabulary: the seven metrics and their definitions as data, a Fact (what was measured, its evidence, its confidence, its state), the score card's types and invariants (docs/SCORE-CARD.md).
- **Input:** nothing. **Output:** types every other module uses.
- **May import:** nothing of ours.
- **Imported by:** `ingest`, `analyze`, `history`, `github`, `score`, `cli`.
- **Start at:** `rubric/evaluator/rubric/Fact.java` (`Fact.measured()`, `Fact.judged()`), `rubric/evaluator/rubric/ScoreCard.java`.

### `ingest/`
- **Purpose:** A repository on disk as an inventory: every source file by path and language, vendored and generated code set aside, nothing interpreted.
- **Input:** a path to a checked-out repository. **Output:** an Inventory; the Size facts.
- **May import:** `rubric`.
- **Imported by:** `analyze`, `cli`.
- **Start at:** `ingest/evaluator/ingest/Ingest.java` (`Ingest.read()`), `ingest/evaluator/ingest/Size.java`; what is set aside, `ingest/evaluator/ingest/SetAside.java` (E-025); the folders a judge calls own code, `OwnCode.java` over `Judgment.java` (E-085).

### `analyze/`
- **Purpose:** Static analysis over the inventory, one adapter per language: complexity per function, the dependency graph, duplication, coupling, the test-to-code ratio, and the repo's own tests run in a sandbox.
- **Input:** an Inventory. **Output:** the Complexity, Maintainability and tool-half-of-Quality facts.
- **May import:** `rubric`, `ingest`.
- **Imported by:** `cli`.
- **Start at:** `analyze/evaluator/analyze/Complexity.java` (`Complexity.measure()`, `Complexity.facts()`), the language adapters' registry `Adapters.java`; the dependency graph and unused imports, `Dependencies.java` (E-032) with its cycle finder `Tarjan.java`; Maintainability's test files, coupling and duplication, `TestFiles.java`, `Coupling.java`, `Duplication.java` (E-034 to E-036); a submission's own tests, `TestRunner.java` over `TestPlans.java`, `Sandbox.java` (Docker: `DockerSandbox.java`) and `Reports.java`, the install phase's one way out `RegistriesProxy.java`, the image itself `sandbox/` (E-037, E-038); the navigability checklist, `Navigability.java` with `DocPaths.java` and `Naming.java` (E-044); a language's reader implements `analyze/evaluator/analyze/Adapter.java` (Java: `JavaAdapter.java`, its counting in `JavaComplexity.java`, E-030; TypeScript and Python: `TypeScriptAdapter.java` and `PythonAdapter.java` on `TreeSitterAdapter.java`, E-031).

### `history/`
- **Purpose:** The git history's shape: spread over time, diff variance, reverts and fixups, tests landing beside code, agent fingerprints. Provenance layer 3.
- **Input:** a path to a checked-out repository with its .git. **Output:** the history-shape facts.
- **May import:** `rubric`.
- **Imported by:** `cli`.
- **Start at:** `history/evaluator/history/Shape.java` (`Shape.facts()`, E-042) over `Commits.java` (`Commits.read()`); git run safely, `Git.java` (E-033). A test-file rule comes in as a predicate: `history/` may not import `analyze/`.

### `github/`
- **Purpose:** What GitHub verifies: commit signature status and push attribution for the account that submitted. Provenance layer 2. The only module that reaches the network, and only GitHub.
- **Input:** a repository name and the submitting account. **Output:** the authorship facts.
- **May import:** `rubric`.
- **Imported by:** `cli`.
- **Start at:** `github/evaluator/github/Authorship.java` (`Authorship.facts()`, E-043) over `GitHubApi.java`; live, `HttpGitHub.java` with `EVALUATOR_GITHUB_TOKEN`. The only module that reaches the network (MOD-05).

### `score/`
- **Purpose:** Facts into a score card: the seven scores, the efficiency ratios, the provenance tier; the card as JSON and as Markdown. It reads no code and runs nothing.
- **Input:** a list of Facts. **Output:** a ScoreCard and its renderings.
- **May import:** `rubric`.
- **Imported by:** `cli`.
- **Start at:** `score/evaluator/score/Cards.java` (`Cards.card()`), the formulas `Formulas.java` (E-045) and `Maintainability.java` (E-051), the bucket `Buckets.java` (E-053), the renderings `CardJson.java` and `CardMarkdown.java`.

### `cli/`
- **Purpose:** The command line: `evaluator score <path>` and nothing the other modules do not already do.
- **Input:** what was typed. **Output:** the score card on stdout, what went wrong on stderr, an exit status.
- **May import:** `rubric`, `ingest`, `analyze`, `history`, `github`, `score`.
- **Imported by:** nothing.
- **Start at:** `cli/evaluator/cli/Main.java` (`Main.run()`, `Main.parse()`), the order of the modules `Pipeline.java` (E-054).

## Rules

- **MOD-01** No module of the kernel imports a model client. Judgment lives in agent-runner/ (phase 2), behind a queue, and reaches the kernel only as facts. (E-003)
- **MOD-02** rubric/ imports no module of ours: it is the vocabulary every other module speaks. (E-004)
- **MOD-03** score/ reads rubric/ only and decides nothing about code: it combines facts the analyzers made. The referee does not read the players' code. (E-004, E-011)
- **MOD-04** Nothing imports cli/: the command line names the work and does none of it. (E-010)
- **MOD-05** Only github/ reaches the network (java.net.http, sockets, URL connections); the one other door is the sandbox's registries proxy, analyze/evaluator/analyze/RegistriesProxy.java, named here. java.net.URI is a name, not a door. (E-043, E-037)
