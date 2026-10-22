# Judge trial 4, round 2: the shortlist three times, and the judge classifying for the kernel to count

Ghost, 2026-10-22: "yes, run round 2". Round 1's shortlist (v01 baseline, v03 classify from evidence, v08
navigability by mechanism, v16 code first, v17 calibration anchors, v21 verdict first, v22 archetype anchors),
each run three times on the six repositories: 126 Claude Haiku 5.5 runs, schema-enforced, about 10.7M
tokens. Every run also classified the folders (own code, own tests, own tooling, docs, generated, vendored,
reference, fixture data). Measured against the seventh Fable's calibrated scale (trial 3); cards in
`trial-4-round2-cards.json`.

## The instruction sets, on the four built repositories

| Set | One run's distance | Mean of three | Spread of three | Pairs in Fable's order |
| --- | --- | --- | --- | --- |
| v22 archetype anchors | 4.7 | 3.6 | 7.2 | 81% |
| v08 navigability by mechanism | 4.8 | 3.8 | 8.5 | 86% |
| v17 calibration anchors | 5.0 | 4.2 | 6.5 | 83% |
| v03 classify from evidence | 5.3 | 4.5 | 7.9 | 81% |
| v01 baseline | 5.3 | 4.7 | 5.9 | 81% |
| v21 verdict first | 5.1 | 4.7 | 6.2 | 79% |
| v16 code first | 6.1 | 5.9 | 6.2 | 81% |

The sets differ by about a point once three runs are averaged; anchoring the scale (archetypes, or other
repositories' scores) helps most, reading code before docs hurts. Every run of every set called the atos
suite generated and Neon Rain a plan, but for two runs.

## The judge classifies, the kernel counts

For each run, the kernel counted the lines of the files whose most specific listed folder Haiku called own
code. Against Fable's own code (in what the kernel counts as source: Suite Builder without its 16k lines of
templates; the Jensen migration's JavaScript):

| Repository | Haiku classifies, kernel counts (median, range) | Kernel counts everything | Fable |
| --- | --- | --- | --- |
| Katana | 16.1k (15.0k–32.9k) | 57.2k | 20.4k |
| Suite Builder | 31.5k (all runs alike) | 81.9k | 33.8k |
| Employee Evaluator | 4.5k (all alike) | 8.8k | 5.0k |
| Jensen migration | 1.2k (all alike) | 1.8k | 1.5k |
| Neon Rain | 33 | 33 | 36 |
| atos suite | 0 | 24.7k | 3 |

The mean error is 0.07 of a decade (about 17%), most of it the kernel's counting convention (it counts
fewer lines per file than `wc`); the kernel alone is off by 0.24 to 0.45 decades on the built repositories
and four decades on the generated one. Haiku's classification is the stable part: nearly every run gives the
same count.

## What it says

- Size, and every measure that depends on what the author's code is (Complexity's total, Maintainability's
  parts), should be the kernel's count over the folders the judge calls own code: a judgment where it needs
  one, a measurement after.
- The judge's instructions: archetype anchors (v22) and navigability by mechanism (v08); calibration anchors
  (v17) once the corpus holds scored cards. Their gain over the baseline is about a point; most of the
  improvement comes from what the judge is asked (the kind of repository, the folders), not how.
- Generated repositories are declined (E-056) and plans get card rules (withhold Size, Complexity,
  Maintainability; Ambition unverified), so the judge is never asked to score what it cannot.
- Found on the way: the kernel has no JavaScript adapter, so the Jensen migration's functions go unmeasured.
