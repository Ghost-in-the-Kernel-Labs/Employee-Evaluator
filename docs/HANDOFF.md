# Handoff: what to work on next, and what it waits on

## Start here: 2.2i, agreement with the calibration set (2026-10-25)

2.1 is closed (J-106). 2.2, the judge harness, is sliced into 2.2a to 2.2n (J-109; the plan is `docs/DESIGN.md`, "Step 2.2 sliced"; each part's check is in
`TODO.md`). 2.2a is done (J-110): `agent-runner/` exists with its Java gate and its no-network import guard, and Ghost approved `python3` in the pinned image
(E-081). 2.2b is done (J-111): `evaluator brief <path>` prints the kernel's brief as JSON. 2.2c is done (J-112): `agent_runner/summary.py` builds the judge's reading from the brief and a checkout. 2.2d is done (J-113): `agent-runner/schema/judgment.schema.json` and `agent_runner/judgment.py`; the judge's kind has six values (E-082), and the schema's description of `kind` is the precedence rule 2.2e's prompt quotes. 2.2e is done (J-114): `agent-runner/prompts/judge.*`, `agent-runner/agent_runner/prompt.py` (`render`, `prompt_hash`); the recordings of 2.2f are keyed by that hash. 2.2f is done (J-115): `agent_runner/judge.py` has the `Judge` interface, `FakeJudge` and `ReplayJudge` over a JSON-lines recordings file (`docs/how/judge-harness.md`). 2.2g is done (J-117): `agent_runner/budget.py` has `Budget` (caps in tokens per call, evaluation and trial, reserve then charge, the cost records under `build/`) and `BudgetedJudge`, which wraps any `Judge`; the prices file is a format with no real file yet. 2.2h is done (J-119): `agent_runner/ensemble.py` (`judge_repository`) runs three Haiku runs and Opus once on a split, with the mean, the vote and a per-field record of runs and models; a category tie goes to Opus (E-083, Ghost's answer). The first open part is **2.2i**, which **waits on Ghost for the definition of agreement** (question 8 in `docs/FOR-GHOST.md`). Before his answer an agent can build what does not depend on it: the loader of `calibration/` labels, the baselines (the most common label, always answered), the record file under the prompt's hash and the 80 % marking, with the figure's definition left a parameter; the definition itself is what waits. Also open to be confirmed by Ghost: Quality's 1 to 5 rule in `ensemble.py` (the median, Opus above one step apart). **2.2i** to **2.2k** need no model API and no key; 2.2i needs Ghost's definition of agreement. **2.2l** (the live transport: its loopback
tests need no key, its smoke does), **2.2m** (the first run on the calibration set) and **2.2n** (the prompt iterated to 80 %) wait on Ghost: key, budget, model
ids and the 30 repositories re-fetched; his questions are in `docs/FOR-GHOST.md` (the `python3` one is answered).

Agreement against the labels is partly model against model (the labels were drafted by Claude Sonnet agents);
Ghost's review is what keeps it from being circular (J-106).

## Waiting on Ghost

- **The token optional** (proposed, not decided): public repositories read without one, a token only for the
  rate limit, and a rate-limit refusal said plainly.
- The rest is in `docs/FOR-GHOST.md`.

## How the work is done here

Each task goes to a fresh Claude Sonnet agent in its own git worktree (E-068); the session that hands it out
reviews its diff, runs the tests and `scripts/prepush.sh`, then merges and pushes. In a worktree,
`MainTest.thisRepositorysCard` fails by design (`.git` is a file there). The last navigation review was J-118; one commit has passed since (this slice) and the next is due in about eight; a tithe is due in about five (three have passed since the last, this slice included).

## A session's environment

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
