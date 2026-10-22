# Handoff: what to work on next, and what is waiting on whom

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. Phase 1 steps 1.1 (the
scaffold) and 1.2 (the rubric's types and ingestion) are done; 1.3 (static analysis adapters) is next.

## For Ghost

- **The image.** `docker build -t evaluator .` has still never run: no session has a Docker daemon.
  The `sandbox` job builds it on a GitHub runner (E-039), and its fast loop proves tree-sitter's native
  library loads there. Linux only: no Windows or macOS run is wanted.
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
  candidate; its jar and license go under `third_party/` with a MANIFEST.md row). Decide at 1.11.
- **Naming.** "Employee Evaluator" may read as surveillance to applicants (docs/DESIGN.md, open
  questions). No decision needed before phase 3.

## Now: 1.11, the tithe pass at the phase-1 boundary

1.10 is done (J-051, E-058): JavaScript is measured as TypeScript is. 1.11 closes phase 1: `gradle metrics`
armed as a test, a generator for docs/MODULES.md, the tithe gate, the mutation tool's first run. The corpus
cards (1.9) are phase-1 readings, made again once the judge classifies folders (E-057). The judge trials
(`docs/judges/`, J-040 to J-048) settled phase 2's design ahead of it: E-055, E-056, E-057. A Gradle test plan
is still unplanned.

**For Ghost, from judge trial 2 (J-041):**
- The judge: Haiku three times, averaged, with the kernel's facts and an enforced schema; Opus where they
  split (E-055, Ghost's choice, in TODO.md 2.2's check).
- Found in other repositories, reported, not touched (read only): Suite Builder's
  `docs/HOSTILE-REVIEW-2026-10-20.md` records a critical bypass of the crawler's address guard
  (IPv6-mapped IPv4), whether fixed unchecked; Suite Builder keeps saved third-party crawls in `test/data`
  and `test/bench` beside a rule against pages saved from others' sites (perhaps an allowed exception);
  Katana keeps about 19k lines of GPL-licensed opensourcecobol4j output under `lower/prototypes/` as
  reference.

**1.7 is open, out of order by Ghost's leave (2026-10-21):** `github/` is built (J-028, E-043) and tested
on recorded answers; its check waits on a fine-grained, read-only token for
Ghost-in-the-Kernel-Labs/suite-builder (Contents and Metadata: read, with an expiry), set in the
environment's settings as `EVALUATOR_GITHUB_TOKEN`; a new session picks it up, and `AuthorshipLiveTest`
runs. The session's own GitHub credentials are not the product's and are not used.

This session runs a Docker daemon (Ghost allowed it, 2026-10-21): `dockerd` started by hand, in the
background (it stops at the background limit; start it again), base images pulled by digest from
`mirror.gcr.io` and tagged when Docker Hub refuses (E-041), then `sandbox/prefetch.sh`, the two builds,
and `gradle test -Pdocker`. A new session must start the daemon again; nothing persists.
`scripts/prepush.sh` runs as written here.

**Waiting on a run, on paper only:** the install phase through the registries network and its proxy in
Docker; the proxy is proven on loopback, but this session's egress cannot be routed through it. The
first hosted machine runs `sandbox/setup.sh` and a fixture with a locked dependency.

- **For Ghost:** the kernel's `Dockerfile` installs git with `apt-get` unpinned, the one install in the
  repository not held by a hash; the supply-chain test does not check apt. Pin it (a version, or a
  snapshot date of the archive) or record why apt's signatures are enough.
- **For Ghost:** this session's container has no seccomp or AppArmor (Docker reports no security
  options), so isolation here is our flags alone; the hosted machine must have both (TODO.md 2.7).
- **Not yet read:** JavaScript (`.js`, `.jsx`, `.mjs`) has no adapter; tree-sitter's JavaScript grammar
  is `io.github.bonede:tree-sitter-javascript` (MIT), vendored the same way when its turn comes.
- **Still open:** docs/DESIGN.md says "three efficiency ratios" once and two everywhere else.
- Add a fixture beside the others when a case needs one, its golden under `test/goldens/`; work each
  value by hand before the golden is written. Run `gradle kernelMap` before the commit, and write
  the journal entry.
