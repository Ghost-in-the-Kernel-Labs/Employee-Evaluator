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

## Now: 1.5, in slices (Ghost's answers in E-037, E-038)

The work runs in one continuous session (Ghost, 2026-10-19); this file is its record, kept current at
each step. S1 (the readers) and S2 (the runner) are built and proven against a fake sandbox (J-019).
S3 (the image) is built (J-021): `sandbox/Dockerfile`, its pins, `sandbox/fetch.py`, the registries proxy
and `sandbox/setup.sh`. Proven here: fetch.py unpacks Maven and refuses a wrong hash; the Python pins
install with every hash checked; c8's lockfile installs with scripts off and reports coverage; the proxy
tunnels a registry and refuses all else on loopback. Not provable here (no Docker daemon), for Ghost's
machine: `sandbox/setup.sh` (both images build, the network and the proxy come up), then a real run.
S4 is built (J-022): `test/fixtures/runs/` (py-pass, py-fail, py-network, node-pass), their reports
from a real run recorded in `test/goldens/runs/`, and `TestRunnerFixturesTest` holding the runner to the
facts worked by hand. **1.5 waits on one run, Ghost's:** on a machine with Docker, `sandbox/setup.sh`,
then `gradle test -Pdocker`; `theRealSandboxGivesTheSameFacts` must pass (it is skipped without
`-Pdocker`). Then 1.5 moves to docs/DONE.md, and 1.6 (git history) starts.

- **For Ghost:** the kernel's `Dockerfile` installs git with `apt-get` unpinned, the one install in the
  repository not held by a hash; the supply-chain test does not check apt. Pin it (a version, or a
  snapshot date of the archive) or record why apt's signatures are enough.
- **Not yet read:** JavaScript (`.js`, `.jsx`, `.mjs`) has no adapter; tree-sitter's JavaScript grammar
  is `io.github.bonede:tree-sitter-javascript` (MIT), vendored the same way when its turn comes.
- **Still open:** docs/DESIGN.md says "three efficiency ratios" once and two everywhere else.
- Add a fixture beside the others when a case needs one, its golden under `test/goldens/`; work each
  value by hand before the golden is written. Run `gradle kernelMap` before the commit, and write
  the journal entry.
