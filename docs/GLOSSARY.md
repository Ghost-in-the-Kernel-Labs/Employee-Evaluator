# Glossary: the project's words

One word for one thing (E-007). "Not" lists the words that mean the same and are not used; an alias
in backticks there is enforced: `test/GlossaryTest.java` fails on it in any document outside this
file and `docs/research/`. An alias without backticks is a review guide.

| Term | Means | Not |
| --- | --- | --- |
| **submission** | one repository an applicant hands in to be scored | resume, CV, application |
| **applicant** | the person who submitted; on the employer side, after an unlock, the candidate | submitter |
| **employer** | an account that browses the board and pays to unlock | recruiter, client, customer |
| **score card** | the card of seven scores, ratios, tier and review for one submission (docs/SCORE-CARD.md) | scorecard (the Java type is ScoreCard), report, result |
| **inventory** | a repository read as files: its source by path, language and lines, and what was set aside and why (`ingest/`) | file list, manifest |
| **set aside** | a source file not counted as the applicant's: vendored or generated, with the rule that said so (E-025) | excluded, ignored, filtered |
| **fact** | one thing measured or judged, with evidence, confidence and state | finding, signal, feature |
| **metric** | one of the seven: Quality, Complexity, Size, Ambition, AI navigability, Maintainability, Provenance | score (that is the metric's number), dimension, category |
| **ratio** | Quality per thousand lines (ranked within its size band), or Maintainability at Complexity (E-047) | normalized score |
| **provenance** | the confidence that the applicant built the submission; the metric and its three layers | authorship (that is layer 2 alone), `authenticity` |
| **history shape** | layer 3 of provenance: the pattern of a git history over time | `commit-shape`, commit pattern |
| **tooling bucket** | which agent tooling built a submission, read from its fingerprints; the peer group a Quality rank is taken in | cohort, `peer-group` |
| **peer corpus** | every score card the platform holds, by tooling bucket; the seed is our own repositories | database, dataset |
| **calibration set** | the hand-labeled repositories a judging prompt is scored against before it scores anything live | training set, ground truth |
| **unlock** | an employer paying to see one applicant's identity, repository and review; it starts the response clock | purchase, reveal |
| **response guarantee** | the rule that an unlocked applicant gets a decision inside the window or the employer loses the board | no-ghosting rule (in public copy only) |
| **kernel** | the Java: the modules of docs/modules.json | core, backend, engine (that is phase 2's job queue and judges) |
| **module** | one folder at the root, one entry in docs/modules.json, package `evaluator.<module>` | package (that is the Java word), component, layer |
| **agent runner** | the Python worker (phase 2) that runs one agent per job and returns facts | `judge service`, LLM service |
| **tithe** | a commit or pass that changes no behaviour and improves navigability (E-015) | cleanup, refactor sprint |
| **golden** | a file a test holds the code to byte for byte (E-018) | snapshot, fixture (that is the input) |
| **fixture** | a small repository under `test/` a metric is proven on | `sample repo`, example |
| **Ghost** | the maintainer, in every file and commit (E-020) | the user, the author |
