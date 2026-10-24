# TODO: the open items, in order

The roadmap as a chain (E-016): items are done in this order, never out of it, and an item that
cannot be done as written stops the chain and goes to Ghost. Each item's plan is its step in
`docs/DESIGN.md`, by the same number; its check is written here. A done item moves to
`docs/DONE.md` with its number kept. `test/TodoTest.java` holds this file: headings in numeric
order, every item with a check, and the first open item's check a real one.

## Phase 1: the rubric and the scorer CLI


## Phase 2: the evaluation engine with agents

### 2.2 Judge harness

Check: a repo summary (inventory, README, sampled files, commit log) and the kernel's facts to a judging
prompt, structured output back (enforced by the API's schema); three Haiku runs averaged, Opus on a
metric they split on by more than 15 (E-055); the judge names the repository's kind and classifies its
folders, and the kernel measures only the folders judged own code (E-057); agreement with the calibration set measured and recorded
with the prompt's hash; no prompt scores live under 80 % agreement.

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

Check: pick a repository the account owns, grant read access, enqueue, see status; the admission gate
(E-056, SC-11): evidence of AI assistance admits, none asks the applicant for their tooling and a judge
decides, hand-written or generated wholesale is declined with its message and no card.

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
