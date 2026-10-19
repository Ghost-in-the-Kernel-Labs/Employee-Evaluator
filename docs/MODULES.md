# Modules: the kernel by folder

An agent starts at the module level, not the function level (Suite Builder's finding, E-004). Each
folder of the kernel: what it is for, what goes in and comes out, what it may and must not import,
and where to start reading. For a task, `CLAUDE.md` says which modules to open and what proves a
change.

The source of truth is `docs/modules.json`; `test/ModulesTest.java` holds every import in the source
to it. This file is written by hand until a generator exists (TODO.md 1.10); when the two disagree,
the JSON is right.

The pipeline: `ingest` reads → `analyze`, `history` and `github` measure → `score` combines facts into
the card → `cli` names the work. `rubric` is the vocabulary all of them speak.

### `rubric/`
- **Purpose:** the vocabulary: the seven metrics as data, a Fact (what was measured, its evidence, its confidence, its state), the score card's types and invariants (docs/SCORE-CARD.md).
- **May import:** nothing of ours (MOD-02).
- **Start at:** `rubric/evaluator/rubric/Fact.java` (`Fact.measured()`, `Fact.judged()`), `rubric/evaluator/rubric/ScoreCard.java`.

### `ingest/`
- **Purpose:** a repository on disk as an inventory: every source file by path and language, vendored and generated code set aside, nothing interpreted.
- **Input:** a path to a checked-out repository. **Output:** an Inventory; the Size facts.
- **May import:** `rubric`.
- **Start at:** `ingest/evaluator/ingest/Ingest.java` (`Ingest.read()`), `ingest/evaluator/ingest/Size.java`; what is set aside, `ingest/evaluator/ingest/SetAside.java` (E-025).

### `analyze/`
- **Purpose:** static analysis over the inventory, one adapter per language: complexity, the dependency graph, duplication, coupling, the test-to-code ratio, and the repo's own tests run in a sandbox (E-013).
- **Input:** an Inventory. **Output:** the Complexity, Maintainability and tool-half-of-Quality facts.
- **May import:** `rubric`, `ingest`.

### `history/`
- **Purpose:** the git history's shape: spread, diff variance, reverts and fixups, tests beside code, agent fingerprints. Provenance layer 3.
- **Input:** a repository with its `.git`. **Output:** the history-shape facts.
- **May import:** `rubric`.

### `github/`
- **Purpose:** what GitHub verifies: commit signatures and push attribution for the submitting account. Provenance layer 2. The only module that reaches the network, and only GitHub.
- **Input:** a repository name and the submitting account. **Output:** the authorship facts.
- **May import:** `rubric`.

### `score/`
- **Purpose:** facts into a score card: the seven scores, the ratios, the tier; the card as JSON and Markdown. It reads no code and runs nothing (MOD-03).
- **Input:** a list of Facts. **Output:** a ScoreCard and its renderings.
- **May import:** `rubric` only.

### `cli/`
- **Purpose:** `evaluator score <path>`, and nothing the other modules do not already do (MOD-04).
- **May import:** every module. **Imported by:** nothing.
