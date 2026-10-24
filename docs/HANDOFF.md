# Handoff: what to work on next, and what it waits on

## Start here: 2.2, the judge harness (2026-10-24)

2.1 is closed (J-106): 30 labels in `calibration/`, reviewed by Ghost, the set MET at 30. The first open item in
`TODO.md` is 2.2, the judge harness. It waits on a model API to run the judges (Haiku runs, Opus on a split)
and on a prompt whose agreement with the calibration set is then measured; read its check in `TODO.md` and
`docs/DESIGN.md` step 2.2 before starting, and ask Ghost if the API access is not in place.

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
in about ten commits; a tithe in about five.

## A session's environment

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
