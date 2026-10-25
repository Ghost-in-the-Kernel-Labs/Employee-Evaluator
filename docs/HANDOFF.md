# Handoff: what to work on next, and what it waits on

## Start here: 2.2f, the judge interface, a fake judge and the replay (2026-10-25)

2.1 is closed (J-106). 2.2, the judge harness, is sliced into 2.2a to 2.2n (J-109; the plan is `docs/DESIGN.md`, "Step 2.2 sliced"; each part's check is in
`TODO.md`). 2.2a is done (J-110): `agent-runner/` exists with its Java gate and its no-network import guard, and Ghost approved `python3` in the pinned image
(E-081). 2.2b is done (J-111): `evaluator brief <path>` prints the kernel's brief as JSON. 2.2c is done (J-112): `agent_runner/summary.py` builds the judge's reading from the brief and a checkout. 2.2d is done (J-113): `agent-runner/schema/judgment.schema.json` and `agent_runner/judgment.py`; the judge's kind has six values (E-082), and the schema's description of `kind` is the precedence rule 2.2e's prompt quotes. 2.2e is done (J-114): `agent-runner/prompts/judge.*`, `agent-runner/agent_runner/prompt.py` (`render`, `prompt_hash`); the recordings of 2.2f are keyed by that hash. The first open part is **2.2f**, the judge interface, `FakeJudge` and `ReplayJudge`; it it and every part to **2.2k** need no model API and no key. **2.2l** (the live transport: its loopback
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
in about two commits (eight have passed); a tithe in about eight.

## A session's environment

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
