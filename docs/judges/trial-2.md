# Judge trial 2: three repositories, three runs of each model

Ghost, 2026-10-22: "attach them and run the trial with opus x3". Trial 1 (this folder) judged one repository
with one Opus run; here every model judges three repositories three times, so Opus's own noise is measured
and a model's bias can be told from one repository's. The brief is trial 1's, read only; on this repository
the judges were blind to the kernel's own scores, as before. Flight Recorder, named in TODO.md 1.9, is not
started (Ghost, 2026-10-22), so the three are Employee Evaluator (17062f1, Java, 4 days), Suite Builder
(b8f2e8c1, TypeScript, 720 commits over 26 days) and Katana (af9b88d, Java, 320 commits over 19 days).
23 runs: 9 Haiku 5.5, 9 Sonnet 5.5, 9 Opus 5.5 (trial 1's six cheaper runs and one Opus run on this
repository count here).

## The scores, three runs each

| Repository | Metric | Opus | Haiku | Sonnet |
| --- | --- | --- | --- | --- |
| Employee Evaluator | Quality | 78, 74, 82 | 72, 78, 74 | 74, 72, 74 |
| | Complexity | 62, 63, 66 | 66, 72, 68 | 72, 72, 70 |
| | Size | 42, 45, 45 | 35, 40, 57 | 40, 38, 38 |
| | Ambition | 72, 76, 74 | 68, 80, 74 | 82, 85, 80 |
| | AI navigability | 90, 87, 88 | 88, 88, 85 | 92, 93, 92 |
| | Maintainability | 78, 77, 82 | 74, 80, 78 | 82, 78, 80 |
| | Provenance | 60, 60, 62 | 62, 55, 55 | 72, 68, 68 |
| Suite Builder | Quality | 84, 82, 82 | 84, 84, 86 | 82, 82, 80 |
| | Complexity | 82, 82, 85 | 88, 84, 84 | 85, 85, 85 |
| | Size | 80, 78, 80 | 90, 76, 80 | 82, 85, 85 |
| | Ambition | 85, 80, 82 | 85, 82, 84 | 82, 80, 85 |
| | AI navigability | 85, 88, 88 | 80, 80, 85 | 93, 92, 94 |
| | Maintainability | 80, 80, 80 | 78, 78, 80 | 84, 85, 82 |
| | Provenance | 82, 85, 80 | 70, 80, 72 | 74, 78, 78 |
| Katana | Quality | 80, 82, 80 | 82, 78, 80 | 78, 74, 80 |
| | Complexity | 88, 90, 88 | 88, 85, 88 | 88, 90, 94 |
| | Size | 70, 72, 74 | 70, 60, 60 | 68, 72, 75 |
| | Ambition | 90, 92, 92 | 85, 88, 92 | 90, 92, 90 |
| | AI navigability | 86, 85, 88 | 88, 80, 84 | 92, 90, 93 |
| | Maintainability | 76, 76, 72 | 75, 72, 80 | 78, 76, 78 |
| | Provenance | 76, 74, 74 | 55, 65, 72 | 55, 80, 74 |

## Readings

Distance is the mean absolute difference from the mean of the three Opus runs, over the seven metrics.

| Reading | Opus | Haiku | Sonnet |
| --- | --- | --- | --- |
| One run's distance (EE, SB, Katana) | 2.3, 1.7, 1.7 (each against the other two: Opus's own noise) | 4.1, 3.7, 4.6 | 5.8, 3.6, 3.7 |
| Mean of three runs' distance | | 2.1, 3.3, 4.0 | 5.7, 3.3, 2.8 |
| Median of three runs' distance | | 2.8, 3.4, 4.1 | 5.5, 3.3, 2.1 |
| Run-to-run spread, average | 4.1, 2.9, 2.7 | 8.9, 5.7, 8.1 | 2.9, 2.7, 7.3 |
| Bias, mean of three minus Opus, over the three repositories | | Provenance −7.4, navigability −3.0, size −2.0, complexity +1.9 | navigability +5.1, complexity +3.9, quality −3.1, ambition +2.6 |
| Ranking the three repositories per metric, means: pairs in Opus's order, of 21 | | 20 | 19 |
| One run's ranking: repository pairs in the Opus means' order | 54 of 58 (93%) | 50 of 55 (91%) | 54 of 60 (90%) |
| Votes: tier, Quality 1–5 | product, product, and research twice on Katana (one product); 4 | the same, research three times on Katana; 4 | the same as Haiku; 4 |
| Real tests told from fixture data | 9 of 9 | 9 of 9 | 8 of 9 (one called `sandbox/` fixture data) |
| Answer a JSON parser accepts | 2 of 9 | 7 of 9 | 9 of 9 |
| Tokens; seconds; tool calls (per run) | 105k; 74; 16.6 | 100k; 48; 10.2 | 71k; 24; 5.6 |
| Price per million tokens in / out (cached table, 2026-10-06) | $4 / $20 | $0.10 / $0.50 | $2 / $10 |
| Cost per run, roughly (tokens at the input price; output and caching not split) | about $0.42 | about $0.01 | about $0.14 |

Flagged (the six cheaper runs more than 15 apart on a metric): this repository's Size, Ambition and
Provenance, and Katana's Provenance. Provenance is the hardest metric for every model but Opus.

## What each model found that the scores do not show

- Opus read the most and found real defects: a crash in this repository's `--date` (fixed, f0ea3be);
  Suite Builder's own hostile review recording a critical bypass of the crawler's address guard; a real
  person's name in this repository's `docs/DESIGN.md` against its own rule; stale numbers in `docs/AGENT.md`.
- Haiku found Katana's GPL-licensed reference output in the tree, and saved third-party crawls in Suite
  Builder against its own rule. It also miscounted history twice ("about 40 commits" here; "about 60
  commits from 10-18" on Katana, which has 320 from 10-04), which is where its Provenance bias comes from.
- Sonnet read least (one run 2 tool calls, one scoring navigability without opening CLAUDE.md), agreed with
  itself most, and reads navigability and complexity high.

## What it shows

- For ranking repositories against each other, which is what the board does, every model is close to Opus:
  one run puts repository pairs in Opus's order 90–93% of the time, Opus itself 93%.
- For the absolute score, Opus agrees with itself within about 2 points. No cheaper model's single run gets
  there (3.6–5.8). Haiku's mean of three comes nearest (2.1–4.0) at about a fourteenth of one Opus run's
  cost; Sonnet's mean of three is good on two repositories and 5.7 off on the third.
- Haiku's largest error, Provenance, came from counting the history wrong. The kernel already counts it
  (`PROVENANCE.history.*`): a judge given the kernel's facts would judge and not count, as E-017 means.
- Free-text JSON fails most on the strongest model (7 of 9 Opus answers malformed): the harness must enforce
  the schema (`output_config.format`), whatever the model.
- Three repositories, all Ghost's, all with unusually good agent docs, all built with Claude: the calibration
  set (TODO.md 2.1) is still what decides it.
