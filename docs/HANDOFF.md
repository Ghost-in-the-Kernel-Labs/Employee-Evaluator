# Handoff: what to work on next, and what it waits on

## Start here: 2.2j, the kernel counts the folders the judge calls own code (2026-10-25)

2.1 is closed (J-106). 2.2, the judge harness, is sliced into 2.2a to 2.2n (J-109; the plan is `docs/DESIGN.md`, "Step 2.2 sliced"; each part's check is in
`TODO.md`). 2.2a to 2.2h are done (J-110 to J-119; `docs/how/judge-harness.md` lists what each built: the scaffold and import guard, `evaluator brief`, the summary builder, the answer schema and validator, the prompt and its hash, `Judge`/`FakeJudge`/`ReplayJudge`, the `Budget`, and `ensemble.py`'s three runs with Opus on a split). **2.2i is done (J-120):** `agent_runner/agreement.py` computes, per field, the labels matched beside the constant-answer baseline, writes `agent-runner/records/<prompt hash>.json` and answers `is_live`; Ghost's definition (E-084) is both the raw 80 % floor and a margin over the baseline. **Ghost, to confirm:** the margin's size is the agent's proposal, half the gap between the baseline and 100 % (`agreement.MARGIN`), which makes Quality within one need 29 of 30, kind 27, Ambition and Quality exactly 24. Also open to be confirmed: Quality's 1 to 5 rule in `ensemble.py` (the median, Opus above one step apart).

The first open part is **2.2j**, which needs no Ghost, no model API and no key: `evaluator score --judged <judgment.json>` takes a judgment in the 2.2d shape and measures Size, Complexity and Maintainability only over files whose most specific classified folder is own code (E-057); it touches `ingest/` (a filter over the inventory), `score/` and `cli/`, and being Java it needs a mutation run (`gradle mutate -Ptarget=evaluator.<module>.*`). **2.2k** (`Fact.judged` bounded by the record of 2.2i) follows. **2.2l** (the live transport: its loopback tests need no key, its smoke does), **2.2m** (the first run on the calibration set) and **2.2n** (the prompt iterated to 80 %) wait on Ghost: key, budget, model ids and the 30 repositories re-fetched; his questions are in `docs/FOR-GHOST.md`.

Agreement against the labels is partly model against model (the labels were drafted by Claude Sonnet agents);
Ghost's review is what keeps it from being circular (J-106).

## Waiting on Ghost

- **The token optional** (proposed, not decided): public repositories read without one, a token only for the
  rate limit, and a rate-limit refusal said plainly.
- The rest is in `docs/FOR-GHOST.md`.

## How the work is done here

Each task goes to a fresh Claude Sonnet agent in its own git worktree (E-068); the session that hands it out
reviews its diff, runs the tests and `scripts/prepush.sh`, then merges and pushes. In a worktree,
`MainTest.thisRepositorysCard` fails by design (`.git` is a file there). The last navigation review was J-118; two commits have passed since (this slice included) and the next is due in about seven; a tithe is due in about four (four have passed since the last, this slice included).

## A session's environment

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
