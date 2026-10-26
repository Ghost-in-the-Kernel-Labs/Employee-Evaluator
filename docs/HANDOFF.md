# Handoff: what to work on next, and what it waits on

## Start here: 2.2l, the live judge, which waits on Ghost (2026-10-26)

2.1 is closed (J-106). 2.2, the judge harness, is sliced into 2.2a to 2.2n (J-109; the plan is `docs/DESIGN.md`, "Step 2.2 sliced"; each part's check is in
`TODO.md`). 2.2a to 2.2k are done (J-110 to J-122; `docs/how/judge-harness.md` lists what each built: the scaffold and import guard, `evaluator brief`, the summary builder, the answer schema and validator, the prompt and its hash, `Judge`/`FakeJudge`/`ReplayJudge`, the `Budget`, `ensemble.py`'s three runs, `agreement.py`'s figures and records, `evaluator score --judged` (E-085), and now the card's rules for the judged kind: `AgreementRecord` and `JudgedRules` in `ingest/`, `Declined` in `score/`, `--records` and exit status 2 in `cli/`, E-086; the rules are in `docs/how/command-line.md`).

Every part that needs no key is built. **The next open part is 2.2l (`agent-runner/transport.py`, the only door to the network), and it waits on Ghost for:** (1) an API key and how it reaches a session (the environment only, never logged); (2) the budget per call, evaluation and trial, and the iteration limit; (3) the model ids and prices of Haiku 5.5 and Opus 5.5 (`claude-haiku-5-5` and `claude-opus-5-5` are unconfirmed defaults); (4) standard-library HTTP or the Anthropic SDK (a dependency he must approve, E-033). Already decided: `python3` is in the image (E-081), Opus decides a category tie (E-083), a field is live at 80 % and half the gap over its baseline (E-084, the half is to confirm), the schema is enforced by the API's structured output, and the build and loopback tests need no key (only the one smoke call does). 2.2m and 2.2n wait on the key, a cap and the 30 labelled repositories re-fetched outside the repository.

**To confirm for Ghost (`docs/FOR-GHOST.md`):** E-086, proposed: a clone is scored with a note, the declined outcome's shape and exit status 2, Quality within one as Quality's gate, a kind that is not live decides nothing; and the risk that the 30 labels hold no generated repository, so a live kind figure does not show "generated" is recognised. Also E-085's two rules (own tests make the test-to-code ratio by the judge's class; the wrapper file shape) and 2.2i's margin (E-084).

Agreement against the labels is partly model against model (the labels were drafted by Claude Sonnet agents);
Ghost's review is what keeps it from being circular (J-106).

## Waiting on Ghost

- **The token optional** (proposed, not decided): public repositories read without one, a token only for the
  rate limit, and a rate-limit refusal said plainly.
- The rest is in `docs/FOR-GHOST.md`.

## How the work is done here

Each task goes to a fresh Claude Sonnet agent in its own git worktree (E-068); the session that hands it out
reviews its diff, runs the tests and `scripts/prepush.sh`, then merges and pushes. In a worktree,
`MainTest.thisRepositorysCard` fails by design (`.git` is a file there). The last navigation review was J-118; four commits have passed since (this slice included) and the next is due in about six; a tithe is due in about two (six have passed since the last, this slice included).

## A session's environment

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
