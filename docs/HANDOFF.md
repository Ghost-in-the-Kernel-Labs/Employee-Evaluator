# Handoff: what to work on next, and what it waits on

## Start here: 2.2h, three runs and the vote (2026-10-25)

2.1 is closed (J-106). 2.2, the judge harness, is sliced into 2.2a to 2.2n (J-109; the plan is `docs/DESIGN.md`, "Step 2.2 sliced"; each part's check is in
`TODO.md`). 2.2a is done (J-110): `agent-runner/` exists with its Java gate and its no-network import guard, and Ghost approved `python3` in the pinned image
(E-081). 2.2b is done (J-111): `evaluator brief <path>` prints the kernel's brief as JSON. 2.2c is done (J-112): `agent_runner/summary.py` builds the judge's reading from the brief and a checkout. 2.2d is done (J-113): `agent-runner/schema/judgment.schema.json` and `agent_runner/judgment.py`; the judge's kind has six values (E-082), and the schema's description of `kind` is the precedence rule 2.2e's prompt quotes. 2.2e is done (J-114): `agent-runner/prompts/judge.*`, `agent-runner/agent_runner/prompt.py` (`render`, `prompt_hash`); the recordings of 2.2f are keyed by that hash. 2.2f is done (J-115): `agent_runner/judge.py` has the `Judge` interface, `FakeJudge` and `ReplayJudge` over a JSON-lines recordings file (`docs/how/judge-harness.md`). 2.2g is done (J-117): `agent_runner/budget.py` has `Budget` (caps in tokens per call, evaluation and trial, reserve then charge, the cost records under `build/`) and `BudgetedJudge`, which wraps any `Judge`; the prices file is a format with no real file yet. The first open part is **2.2h** (three runs, the mean, the vote and Opus on a split), which **waits on Ghost for the category tie rule** (E-055 is silent; he is asked in TODO 2.2h and `docs/FOR-GHOST.md`). Before his answer it can be built up to the tie: the three runs under a `BudgetedJudge`, the mean, the 15/16 split boundary, the one retry of an unparsable run, the fewer-than-two refusal and the partial flag on a budget refusal; the three-way tie case is the part that waits. **2.2i** to **2.2k** need no model API and no key; 2.2i needs 2.2h and Ghost's definition of agreement. **2.2l** (the live transport: its loopback
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
`MainTest.thisRepositorysCard` fails by design (`.git` is a file there). The next navigation review is due
in about one commit (nine have passed since the last review, this slice included); a tithe in about seven.

## A session's environment

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
