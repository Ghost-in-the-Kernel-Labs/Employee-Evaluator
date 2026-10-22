# Judge trial 1: which model can do Opus's code evaluation of this repository

Ghost, 2026-10-22: "We are looking for the cheapest model that can do the opus job for code eval of this
repo, with reliable results." Seven agents judged this repository at commit 17062f1 from one brief, read
only, blind to the kernel's own scores (theGhost.md, the journal, the handoff, `docs/DONE.md`, the card
goldens and commit messages withheld): three Haiku 5.5, three Sonnet 5.5, one Opus 5.5. Each gave the seven
metrics 0–100 with confidence and evidence, an Ambition tier, Quality 1–5, and which folders hold this
repository's real tests and which hold fixture data. This is one repository and one Opus run: a first look
for phase 2's judge harness (TODO.md 2.2), not a calibration (TODO.md 2.1).

## The scores

| Metric | Opus | Haiku ×3 | mean | Sonnet ×3 | mean |
| --- | --- | --- | --- | --- | --- |
| Quality | 78 | 72, 78, 74 | 74.7 | 74, 72, 74 | 73.3 |
| Complexity | 62 | 66, 72, 68 | 68.7 | 72, 72, 70 | 71.3 |
| Size | 42 | 35, 40, 57 | 44.0 | 40, 38, 38 | 38.7 |
| Ambition | 72 | 68, 80, 74 | 74.0 | 82, 85, 80 | 82.3 |
| AI navigability | 90 | 88, 88, 85 | 87.0 | 92, 93, 92 | 92.3 |
| Maintainability | 78 | 74, 80, 78 | 77.3 | 82, 78, 80 | 80.0 |
| Provenance | 60 | 62, 55, 55 | 57.3 | 72, 68, 68 | 69.3 |

All seven: tier "product", Quality 4 of 5. Test classification: all seven named `test/` and
`test/evaluator/` as the real tests, run by `gradle test`, and `test/fixtures/` as data; one Sonnet run
(two tool calls in all) also called `sandbox/`, which holds no tests, fixture data.

## Ways of reading the six

| Reading | Haiku | Sonnet |
| --- | --- | --- |
| One run, mean distance from Opus over the seven | 4.1, 4.1, 5.3 (avg 4.5) | 6.3, 6.3, 5.1 (avg 5.9) |
| Mean of three, distance from Opus | 2.9 | 5.9 |
| Median of three, distance from Opus | 3.0 | 5.7 |
| Run-to-run spread, average over metrics | 8.9 | 2.9 |
| Order of the seven within the card, Spearman vs Opus | 0.96, 0.86, 0.93 | 0.86, 0.86, 0.86 |
| Votes: tier, Quality 1–5, real tests | unanimous | unanimous |
| Output a parser accepts | 2 of 3 (one JSON missing a comma) | 3 of 3 |
| Tool calls; tokens; seconds (each run) | 10–13; ~103k; ~43 | 2–7; ~70k; ~21 |

Flagging: where the six cheaper runs spread more than 15 points, the metric would go to Opus. Three were
flagged: Size (35–57: one Haiku run applied this repository's own log-scale formula to itself), Ambition
(68–85) and Provenance (55–72). Opus fell inside the six's range on every metric but Complexity, where all
six read higher (66–72 to its 62).

## What it shows, and what it cannot

- Sonnet agrees with itself (spread 3) but sits off Opus in one direction: Ambition +10, Provenance +9,
  Complexity +9. A steady offset can be calibrated away; noise cannot. Haiku is noisier (spread 9) but
  centred on Opus: three runs averaged come within 3 points of it, closer than Sonnet's three.
- With one Opus run, part of every difference is Opus's own noise; Sonnet's tight cluster may be nearer the
  truth than Opus on Provenance. One repository cannot tell a model's bias from this repository's.
- Facts checked against the repository (48 commits on 28/9/11 days, a 3,498-line first commit, 5,038 main
  lines in 63 files, 149 tests): Opus exact to the line; Haiku mostly right (one run said about 40 commits
  and 28 test files); Sonnet right on the history, one run 2.4k test lines for 3.7k.
- The judgment the kernel's rules could not make (which test files are data) every model made correctly.
- Next, for the harness: Opus three times as the baseline; the same brief over the calibration set (2.1);
  structured output enforced by the harness, since one run in six was not parseable.
