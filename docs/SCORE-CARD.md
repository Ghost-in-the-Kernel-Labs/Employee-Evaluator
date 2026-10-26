# The score card: the project's intent map

The score card is what the platform produces for one submission and what an employer reads. It is
built entirely from **facts**, each with its evidence, its confidence and its state, the way Katana's
Intent IR holds what a program means (E-017). A score nobody can trace to a fact is refused. This
document is the contract `rubric/` implements; `test/ScoreCardDocTest.java` holds this document to
its types: the metrics, a fact's fields, its states, the card's fields and the tiers are the code's,
and every invariant is cited in `rubric/` or `score/` (`// Invariant: SC-02`) or names the open step that will hold it.

Invariants are numbered SC-01 onward, in this document only, and a number is never reused.

## The seven metrics (E-011)

| Metric | Measures | Facts it is built from | Judge |
| --- | --- | --- | --- |
| Quality | correctness, tests, error handling, idiom | test pass rate and coverage, lint findings, agent review of sampled modules | tools + agent |
| Complexity | difficulty of the problem actually solved | cyclomatic and cognitive complexity per function, dependency-graph depth, agent assessment | tools + agent |
| Size | amount of working software | source lines, files, modules, with vendored and generated code set aside | tools |
| Ambition | how hard the stated goal was | agent judgment against a calibrated rubric: toy, utility, product, research | agent, calibrated |
| AI navigability | how easily an agent with little context works in the repo | phase 1: a checklist (`CLAUDE.md`/`AGENTS.md` present and fresh, module boundaries, naming); phase 2: a cold-start task measured in tool calls and tokens | checklist, then agent task |
| Maintainability | how safely the code can be changed | coupling, duplication, test-to-code ratio, refactor history | tools + agent |
| Provenance | confidence the applicant built it | identity (sign-in), authorship (signed or pushed commits), history shape | tools |

- **SC-01 Seven scores, each ranked on its own and each 0–100 except the counted metrics (Complexity, Size, Maintainability); there is no composite.** A
  counted metric is scored against the baseline repository (E-070) and has no ceiling (E-071): twice the baseline's lines
  reads 200. Provenance, Quality and Navigability keep 0–100, and the card refuses a score above 100 for any metric not
  counted. The card never carries a single overall number, and no renderer computes one.
- **SC-04 Size and Complexity rank through ratios.** The card shows them raw and ranks on Quality per
  thousand lines and Maintainability at Complexity (their product over 100, high only when both are; E-047).
- **SC-10 Quality per thousand lines ranks only within its size band.** Any score per line ranks the
  smallest first, so a card is ranked only among cards of its own band (under 2k lines, 2k to 10k, 10k to
  50k, 50k and over; E-047); the card carries its band. Held from TODO.md 2.6, the corpus store.

## A fact

A `Fact` is one thing measured or judged about the submission:

| Field | Holds |
| --- | --- |
| `kind` | which metric it feeds and what it says (`SIZE.source_lines`, `PROVENANCE.history.spread_days`, `QUALITY.agent_review`): the metric's name, a dot, then what it says, or it is refused |
| `subject` | what it is about: the whole repository, a file, a function, a commit, a module |
| `value` | the measurement or the judgment, typed by the kind |
| `evidence` | where it was read: paths and lines, commit hashes, the tool and its version, the prompt hash and the sampled files for an agent judgment |
| `confidence` | 0–1; a tool measurement is 1.0; an agent judgment carries the precision its prompt scored on the calibration set, never more |
| `state` | `inferred`, `confirmed` or `refuted` |

- **SC-02 Every fact carries its evidence.** A fact with no evidence is refused when built.
- **SC-03 A refuted fact never scores.** A person refutes a fact (Ghost during calibration, an employer
  in review); the card is recomputed without it and says which facts a person refuted.
- **SC-05 An agent judgment's confidence is bounded by calibration.** The judging prompt's precision
  on the labeled set (30 repositories to start) is recorded with the prompt's hash; a fact from that
  prompt carries at most that number. A prompt with no recorded precision produces no fact.
- **SC-06 A fact is known by its kind and its subject.** A rule's next version that infers it again is
  the same fact, so a person's word on it stays with it. Its key is its kind and its subject (`SIZE.source_lines repository:.`).

## The card

One card per submission:

| Field | Holds |
| --- | --- |
| `scores` | the seven, each 0–100 (a counted metric may pass 100, SC-01), with the facts behind it listed by key |
| `ratios` | Quality per thousand lines and the size band it ranks in; Maintainability at Complexity |
| `provenanceTier` | Verified (≥ 80), Partial (50–79), Unverified (< 50) |
| `toolingBucket` | which agent tooling built the repository, read from its fingerprints: the tool with more than half the evidence, else mixed (E-053) |
| `corpusRank` | per metric, the percentile within the tooling bucket, once the bucket holds enough cards (200 to start); absent before that |
| `notMeasured` | metrics no fact fed (the judgment metrics, when no agent ran, E-003) |
| `refuted` | the facts a person refuted and by whom |
| `review` | the agent's prose, one paragraph per judgment metric, each citing the facts it rests on |
| `produced` | the kernel's version, the date, the rubric's hash (a judged card's rendering also names the prompt's hash and the models, E-085) |

- **SC-07 A metric with no fact is "not measured", never 0.** A zero is a measurement; an absence is
  an absence, and the employer sees which.
- **SC-11 A card is made only for AI-assisted work** (E-056). Evidence of AI assistance admits a
  submission; no evidence asks the applicant to declare and show their tooling, and a judge decides;
  work clearly written by hand, or generated wholesale by a tool, is declined with no card. What is
  ranked is the applicant's proficiency at structuring AI-built code. Held from TODO.md 3.3, the
  submission flow.
- **SC-08 The anonymized card is the card minus identity.** The public board shows the scores, the
  ratios, the tier, the bucket, the language and a coarse category; never the review, the README, a
  path or any fact's evidence, since evidence identifies. Held from TODO.md 3.5, the board.
- **SC-09 A card for a fixture repository is a golden** (E-018). Its JSON rendering is compared byte
  for byte; a refactor changes none. Held by `score/` (`CardJson`) and `test/evaluator/cli/CardGoldensTest.java`.

## Where the facts come from

| Module | Facts |
| --- | --- |
| `ingest/` | `SIZE.source_lines`, `SIZE.source_files`, `SIZE.source_folders`, `SIZE.set_aside_files` (E-026) |
| `analyze/` | `COMPLEXITY.*`, `MAINTAINABILITY.*`, `QUALITY.tests.*`, `NAVIGABILITY.checklist`, `NAVIGABILITY.checklist.*` |
| `history/` | `PROVENANCE.history.*` |
| `github/` | `PROVENANCE.authorship.*` |
| `agent-runner/` (phase 2) | `AMBITION.*`, `QUALITY.agent_review`, `COMPLEXITY.agent_assessment`, `NAVIGABILITY.task` |
| `score/` | none: it combines facts into the card and makes no fact of its own |
| not built | `QUALITY.lint` (E-052), `MAINTAINABILITY.refactors` |

`test/ScoreCardDocTest.java` holds this table to the code: every kind a module's source names is in its row
(exactly, or under a `.*`), and every kind in the row of a module that exists is named in its source.
