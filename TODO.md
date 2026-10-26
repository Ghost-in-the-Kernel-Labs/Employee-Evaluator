# TODO: the open items, in order

The roadmap as a chain (E-016): items are done in this order, never out of it, and an item that
cannot be done as written stops the chain and goes to Ghost. Each item's plan is its step in
`docs/DESIGN.md`, by the same number; its check is written here. A done item moves to
`docs/DONE.md` with its number kept. `test/TodoTest.java` holds this file: headings in numeric
order, every item with a check, and the first open item's check a real one. An item too large for one
go is sliced into parts (2.2a, 2.2b, ...), each with its own check, done in order, the plan in its step in
`docs/DESIGN.md`; the item moves to `docs/DONE.md` whole when its last part is done.

## Phase 1: the rubric and the scorer CLI


## Phase 2: the evaluation engine with agents

### 2.2k The card's rules for the judged kind

Check: a judged fact is built with `Fact.judged` and confidence at most the recorded agreement of its prompt's hash (SC-05),
read from `agent-runner/records/`; a prompt with no record for its hash gives no fact; kind plan only withholds Size,
Complexity and Maintainability and reads Ambition unverified, kind generated gives the declined outcome with no card
(E-056, E-057); each case a fixture with a hand-written judgment and a golden. Reads `rubric/`'s `Fact`, `Calibration` and `Label`
only; no model client. No API, no key. Ghost: none. Cost: none.

### 2.2l The live judge: the one door to the API

Check: `agent-runner/transport.py` is the only file that reaches the network: it sends the rendered prompt and the 2.2d
schema to the API's structured-output feature (the schema is enforced there, not asked for in prose), reads the key from the
environment only and never logs or writes it, passes every call through the `Budget`, retries 429 and 5xx a bounded number of
times, and writes each answer in the recordings format; tested without a key against a loopback server (the request carries the
schema and the model id, a failure retries as set, the key appears nowhere in logs or recordings); a smoke test that
calls the real API once on a synthetic summary runs only when the key is set, and says SKIPPED otherwise. Needs a model API
and a key to run the smoke: waits on Ghost: key and budget (and the model ids, and the standard library or the SDK, a
dependency he must approve, E-033). Cost: small (one call).

### 2.2m The first run on the calibration set

Check: the 30 labelled repositories summarised outside the repository by 2.2c (Ghost's private map, never read by an agent),
judged by 2.2h through 2.2l (three Haiku runs each, Opus on a split), the answers recorded and the agreement of 2.2i written
to `agent-runner/records/` with the prompt's hash, committed with ids, scores, kinds and tiers only (never a folder name or
an evidence quote from a real repository, E-014); the run's cost recorded. Needs a model API and a key; needs the 30
repositories re-fetched, which only Ghost can arrange. Waits on Ghost: key, budget and the repositories. Cost: needs a cost
cap (about 90 Haiku runs and the Opus runs; trial 4 spent about $1.30 on 150 Haiku runs).

### 2.2n The prompt iterated to the floor

Check: the prompt revised, one hashed version at a time and each version's record kept, until every field the harness
judges clears 80 % beyond its baseline on the calibration set or the trial's budget is spent (then stop and tell Ghost);
the winning prompt's hash and figures in `docs/judges/` as trial 5; the original check of 2.2 holds in full: the harness
judges a repository from a summary and the kernel's facts to enforced structured output, three Haiku runs and Opus on a
split, the judge names the kind and classifies the folders and the kernel measures the own code (2.2j), agreement
recorded with the prompt's hash, no prompt scoring live under 80 %. Needs a model API and a key. Waits on Ghost: key and
budget. Cost: needs a cost cap (each iteration repeats 2.2m's run).

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
