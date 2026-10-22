# TODO: the open items, in order

The roadmap as a chain (E-016): items are done in this order, never out of it, and an item that
cannot be done as written stops the chain and goes to Ghost. Each item's plan is its step in
`docs/DESIGN.md`, by the same number; its check is written here. A done item moves to
`docs/DONE.md` with its number kept. `test/TodoTest.java` holds this file: headings in numeric
order, every item with a check, and the first open item's check a real one.

## Phase 1: the rubric and the scorer CLI

### 1.7 GitHub adapter

Check: signed, unsigned-but-pushed, and foreign-author commits are told apart on a real repository
of Ghost's through the API, with the token read from the environment and never logged; the module
is the only one that reaches the network (`docs/modules.json`).

### 1.9 Score card and the seed corpus

Check: `evaluator score <path>` prints the card as JSON and as Markdown for every fixture, each a
golden, its two efficiency ratios (SC-04: Quality per thousand lines, Maintainability per unit of
Complexity) computed by `score/` from the scores (moved here from 1.4, J-017); the cards for Suite Builder, Katana and Flight Recorder are committed under `corpus/` and
survive Ghost's sanity read.

### 1.10 Tithe pass: the phase-1 boundary

Check: `gradle metrics` arms every metric of `docs/AGENT.md` and the test holds them; a generator
writes `docs/MODULES.md` from `docs/modules.json`; the tithe gate refuses a push past sixteen commits
without a tithe; the mutation tool is vendored and `docs/MUTATION.md` has its first run (E-019
moves to in force or is refused).

## Phase 2: the evaluation engine with agents

### 2.1 Calibration set

Check: 30 repositories labeled by Ghost on Ambition and Quality, in a fixture format with a test
that holds every label to its fields; the set spans at least five domains.

### 2.2 Judge harness

Check: a repo summary (inventory, README, sampled files, commit log) to a judging prompt, structured
output back; agreement with the calibration set measured and recorded with the prompt's hash; no
prompt scores live under 80 % agreement.

### 2.3 Ambition judge

Check: the Ambition facts on the calibration set, within the recorded agreement.

### 2.4 Quality judge

Check: the agent review of sampled modules combined with 1.5's facts into the Quality metric; the
calibration set holds.

### 2.5 Navigability task

Check: a fresh agent in the sandbox given a fixture and a small change request; tool calls, tokens
and whether the repo's tests pass become the Navigability facts; the checklist of 1.8 retired.

### 2.6 Peer corpus store

Check: cards keyed by tooling bucket in SQLite; percentiles appear only once a bucket holds 200;
Quality per thousand lines ranked only within its size band (SC-10).

### 2.7 Job queue and sandbox fleet

Check: a submission runs as one isolated job with a timeout and a cost cap; cost per evaluation is
recorded on every job; the sandbox reaches nothing but GitHub.

### 2.8 Review renderer

Check: the agent's prose and the card into the employer-facing review, each paragraph citing its facts.

### 2.9 Tithe pass: the phase-2 boundary

Check: the metrics hold; `docs/AGENT.md` has the pass.

## Phase 3: the web app

### 3.1 Scaffold `web/`

Check: a Next.js frontend in `web/` talking to the Java API, with the score-card types shared from `rubric/`.

### 3.2 GitHub App and sign-in

Check: applicant and employer roles; an employer verified by company email domain.

### 3.3 Submission flow

Check: pick a repository the account owns, grant read access, enqueue, see status.

### 3.4 Applicant's card page

Check: all seven metrics, the review, the tier, and exactly what the board shows.

### 3.5 Employer board

Check: anonymized cards; filter by language, bucket, category; sort by any metric or ratio.

### 3.6 Unlock flow

Check: Stripe checkout; identity and repository revealed; the response clock started.

### 3.7 Response tracking

Check: advanced, declined or hired marked; an overdue unlock suspends the board; the applicant sees status.

### 3.8 Admin

Check: corpus health per bucket, cost per evaluation, calibration drift.

### 3.9 Tithe pass: the phase-3 boundary

Check: the metrics hold across all three packages.
