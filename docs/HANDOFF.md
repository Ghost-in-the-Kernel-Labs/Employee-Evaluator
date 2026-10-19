# Handoff: what to work on next, and what is waiting on whom

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. Phase 1 steps 1.1 (the
scaffold) and 1.2 (the rubric's types and ingestion) are done; 1.3 (static analysis adapters) is next.

## For Ghost

- **The image.** `docker build -t evaluator .` has still never run: no workspace so far had a
  Docker daemon. Gradle itself has run the build (J-005), so `scripts/prepush.sh` is the one untried
  piece. The fast loop in the image now also proves tree-sitter's native library loads there with the
  network off (`test/evaluator/analyze/TreeSitterLoadsTest.java`); the same test on your Windows
  machine proves the Windows library.
- **Supply chain (E-033), what only you can do:** branch protection on `main` (pull requests
  required, force-pushes blocked; note this changes E-020's "a finished step lands on main at once",
  so decide how an agent's work lands), secret scanning and Dependabot alerts, passkey MFA on GitHub,
  fine-grained tokens with an expiry, and `audit-agent-configs.ps1` on this repository as the
  rollout's acceptance test. `osv-scanner` has no lockfile to read here: every dependency is a
  vendored jar, held by its SHA-256 (`test/SupplyChainTest.java`). Suite Builder is the baseline's
  first repository and has not had its session yet.
- **Calibration set.** Phase 2 cannot start until 30 repositories are hand-labeled on Ambition and
  Quality (TODO.md 2.1). Only Ghost can label; the fixture format comes from Opus first.
- **Mutation tool.** E-019 is proposed until a Java mutation tool is vendored (PIT, Apache-2.0, is the
  candidate; its jar and license go under `third_party/` with a MANIFEST.md row). Decide at 1.10.
- **Naming.** "Employee Evaluator" may read as surveillance to applicants (docs/DESIGN.md, open
  questions). No decision needed before phase 3.

## For the next session (1.5, waiting on Ghost)

1.4 is done (J-016, J-017; its two ratios moved to 1.9). 1.5 runs a submission's own tests, the first
step that runs hostile code (E-013), and it needs Ghost's approval before it starts, because each
answer below adds a dependency (E-033: none without a decision and approval):

- **The sandbox.** A container per run (`docker run --network none --memory ... --pids-limit ...
  --read-only`, no credentials mounted), or a stronger runtime under it (gVisor's `runsc`), or
  bubblewrap on the host. This workspace has no Docker daemon, so whichever is chosen is proven on
  Ghost's machine first.
- **The runtimes and coverage tools,** vendored or pinned in the image: Python with pytest and
  coverage.py; Node with the submission's own runner and c8 for coverage; Java with JUnit's console
  launcher and JaCoCo. Each is a new dependency.
- **A submission's own dependencies.** Its tests need its packages, and "no network" forbids fetching
  them. Proposal: two phases. First, install from the submission's lockfile only (`npm ci
  --ignore-scripts`, `pip install --require-hashes`), with the network limited to the registries;
  no lockfile, no install, and the tests are "not measured" (SC-07). Then the tests run with the
  network off entirely.

- **Not yet read:** JavaScript (`.js`, `.jsx`, `.mjs`) has no adapter; tree-sitter's JavaScript grammar
  is `io.github.bonede:tree-sitter-javascript` (MIT), vendored the same way when its turn comes.
- **Still open:** docs/DESIGN.md says "three efficiency ratios" once and two everywhere else.
- Add a fixture beside the others when a case needs one, its golden under `test/goldens/`; work each
  value by hand before the golden is written. Run `gradle kernelMap` before the commit, and write
  the journal entry.
