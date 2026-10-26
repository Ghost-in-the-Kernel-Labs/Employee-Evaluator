# Handoff: what to work on next, and what it waits on

## Start here: 2.2k, the card's rules for the judged kind (2026-10-26)

2.1 is closed (J-106). 2.2, the judge harness, is sliced into 2.2a to 2.2n (J-109; the plan is `docs/DESIGN.md`, "Step 2.2 sliced"; each part's check is in
`TODO.md`). 2.2a to 2.2j are done (J-110 to J-121; `docs/how/judge-harness.md` lists what each built: the scaffold and import guard, `evaluator brief`, the summary builder, the answer schema and validator, the prompt and its hash, `Judge`/`FakeJudge`/`ReplayJudge`, the `Budget`, `ensemble.py`'s three runs, `agreement.py`'s figures and records, and now `evaluator score --judged`: the kernel counts only the folders the judge calls own code, its file shape and rules are in `docs/how/command-line.md` and E-085).

The first open part is **2.2k**, which needs no Ghost, no model API and no key: a judged fact is built with `Fact.judged` and confidence at most the recorded agreement of its prompt's hash (SC-05), read from `agent-runner/records/`; a prompt with no record for its hash gives no fact; kind plan only withholds Size, Complexity and Maintainability and reads Ambition unverified, kind generated gives the declined outcome with no card (E-056, E-057); each case a fixture with a hand-written judgment and a golden. It reads `rubric/`'s `Fact`, `Calibration` and `Label` only, and builds on 2.2j's `Judgment` (its `kind` is read and checked but not yet acted on). **To confirm for Ghost:** E-085's two rules (own tests make the test-to-code ratio by the judge's class; the wrapper file shape), and 2.2i's margin (E-084).

Agreement against the labels is partly model against model (the labels were drafted by Claude Sonnet agents);
Ghost's review is what keeps it from being circular (J-106).

## Waiting on Ghost

- **The token optional** (proposed, not decided): public repositories read without one, a token only for the
  rate limit, and a rate-limit refusal said plainly.
- The rest is in `docs/FOR-GHOST.md`.

## How the work is done here

Each task goes to a fresh Claude Sonnet agent in its own git worktree (E-068); the session that hands it out
reviews its diff, runs the tests and `scripts/prepush.sh`, then merges and pushes. In a worktree,
`MainTest.thisRepositorysCard` fails by design (`.git` is a file there). The last navigation review was J-118; three commits have passed since (this slice included) and the next is due in about six; a tithe is due in about three (five have passed since the last, this slice included).

## A session's environment

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
