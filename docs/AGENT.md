# The agent's environment: its contract and its metrics

What an agent working in Employee Evaluator can count on, each line a measurable, and the metrics
the navigation tithe ratchets (E-015). A metric may improve; it never regresses without a decision in
`theGhost.md`. A metric is **armed** the first time it is measured within its target; until then it
is "not armed" and not compared. A test holding the table arrives at the phase-1 boundary (TODO.md
1.10); until then `gradle metrics` prints it and the agent compares by eye.

## The contract

- **AGT-01 A clean clone builds offline.** `gradle build --offline` in the image with `--network none`,
  from `third_party/` (E-021).
- **AGT-02 The fast loop runs under 20 s, warm.** Unit and doc tests only. Anything that clones a
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

| Metric | Phase 1 target | Pass 0 (2026-10-18, scaffold) | Why it matters to the agent |
| --- | --- | --- | --- |
| Mandatory context (words) | < 1,500 | 747 | every task pays it |
| Fast loop (s) | ≤ 20 | not timed yet | the agent runs it after every edit |
| Largest source file (lines) | ≤ 400 | 214 (test/GhostTest.java) | a file over it does not fit a routing row |
| Files a routing row opens | ≤ 5 | 4 | the context one task costs |
| Functions over 20 paths | 0 | 0 | a function nobody can hold in mind |
| Duplicate helpers (same body, two names) | 0 | 0 | two names for one thing |
| Decisions without `See also` | reported, not held | 15 | a decision that floats |
| Docs lines per source line | reported, not held | 0.73 | the ratio of intent to code |
| Stale generated artifacts | 0 | 0 | a map that lies |
| Commits since the last tithe | ≤ 8 | 0 | E-015 |

Pass 0's numbers are the scaffold's: almost no product code, so most are trivially met. The first
pass that means anything is 1.10.
