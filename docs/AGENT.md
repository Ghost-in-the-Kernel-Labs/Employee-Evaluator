# The agent's environment: its contract and its metrics

What an agent working in Employee Evaluator can count on, each line a measurable, and the metrics
the navigation tithe ratchets (E-015). A metric may improve; it never regresses without a decision in
`theGhost.md`. A metric is **armed** the first time it is measured within its target; until then it
is "not armed" and not compared. `test/MetricsTest.java` holds every metric with a target (TODO.md 1.11):
the fast loop is timed at each tithe pass (`gradle metrics --args=--time`), and the tithe cadence is held at
push by `scripts/prepush.sh`.

## The contract

- **AGT-01 A clean clone builds offline.** `gradle build --offline` in the image with `--network none`,
  from `third_party/` (E-021): `docker run --rm --network none -u "$(id -u)" -v "$PWD":/evaluator
  evaluator gradle test --offline` is the fast loop there.
- **AGT-02 The fast loop runs under 20 s, warm.** Unit and doc tests only. Test classes run side by side (E-072); read 8 to 9 s on 2026-10-23 (`gradle metrics --args=--time`, from 15 to 16 s; Ghost's machine read 23 s before). Anything that clones a
  repository, runs its tests or reaches GitHub is an integration test with its own target and budget.
- **AGT-03 Mandatory context stays under 1,500 words.** `CLAUDE.md` plus whatever an agent must read
  before a task.
- **AGT-04 A routing row opens at most five files, none over 400 lines.**
- **AGT-05 Generated files are marked and kept out of diff review** (`.gitattributes`
  `linguist-generated -diff`).
- **AGT-06 Every generated artifact has a command that regenerates it and a test that fails when it
  is stale.** Today: the kernel map (`gradle kernelMap`, `test/KernelMapTest.java`).
- **AGT-07 The container image is pinned** (JDK 21, Gradle 8.14.3, git, by digest). An agent never
  installs tooling.
- **AGT-08 Logs are structured and short.** A failing check says what to do in its first line.
- **AGT-09 A submitted repository never runs outside the sandbox** (E-013), the tests' fixtures included.

## The metrics

Measured by `gradle metrics` in the image at every tithe pass.

| Metric | Phase 1 target | Pass 0 (2026-10-18, scaffold) | Pass 1 (2026-10-19, TODO 1.5 begun) | Pass 2 (2026-10-22, TODO 1.11) | Pass 3 (2026-10-23, TODO 1.12) | Pass 4 (2026-10-23, TODO 1.12, analyze/ part 4) | Pass 5 (2026-10-23, TODO 1.13) | Pass 6 (2026-10-23, `evaluator score --facts`) | Pass 7 (2026-10-24, TODO 2.1) | Why it matters to the agent |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Mandatory context (words) | < 1,500 | 747 | 976 | 1,067 | 847 | 847 | 847 | 780 | 780 | every task pays it |
| Fast loop (s) | ≤ 20 | not timed yet | 7 (warm) | 18 (all tests rerun) | 18 (all tests rerun; `gradle metrics --args=--time`, no Docker needed) | 20 (all tests rerun, `gradle metrics --args=--time`) | 10 (plain clone, warm, `gradle metrics --args=--time`; 13 and 10 on two runs) | 9 (plain clone, warm, `gradle metrics --args=--time`; 17 cold, then 9, 9, 9 and 8 on five runs, none failed) | 8 (plain clone, warm, `gradle metrics --args=--time`; five runs, none failed; with `EVALUATOR_GITHUB_TOKEN` set `AuthorshipLiveTest` calls GitHub and the loop reads 143 to 158, so unset it to time) | the agent runs it after every edit |
| Largest source file (lines) | ≤ 400 | 214 (test/GhostTest.java) | 233 (JavaComplexity.java) | 280 (test/SupplyChainTest.java) | 289 (test/SupplyChainTest.java) | 289 (test/SupplyChainTest.java) | 289 (test/SupplyChainTest.java) | 283 (test/SupplyChainTest.java) | 283 (test/SupplyChainTest.java) | a file over it does not fit a routing row |
| Files a routing row opens | ≤ 5 | 4 | 5 | 5 | 5 | 5 | 5 | 5 | 5 | the context one task costs |
| Functions over 20 paths | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | a function nobody can hold in mind |
| Duplicate helpers (same body, two names) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | two names for one thing |
| Decisions without `See also` | reported, not held | 15 | 15 | 15 | 15 | 15 | 15 | 15 | 15 | a decision that floats |
| Docs lines per source line | reported, not held | 0.76 | 0.31 | 0.32 | 0.40 | 0.39 | 0.40 | 0.41 | 0.40 | the ratio of intent to code |
| Stale generated artifacts | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | a map that lies |
| Commits since the last tithe | ≤ 8 | 2 | 0 (this tithe; it was 9) | 0 (this tithe; it was 13) | 0 (this tithe; it was 9) | 0 (this tithe; it was 8) | 0 (this tithe; it was 8) | 0 (this tithe; it was 8) | 0 (this tithe; it was 11) | E-015 |

Pass 0's numbers are the scaffold's: almost no product code, so most are trivially met. Pass 2 is the
first that means anything, at the phase-1 boundary: about 9.4k lines of Java, every target held, and the
tithe cadence slipped twice before the gate existed (J-045, J-051).
