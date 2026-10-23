# Handoff: what to work on next, and what it waits on

## Start here: 1.7's live check, half done (2026-10-23, J-102)

The first real run of `AuthorshipLiveTest` found a bug in ours, fixed: `HttpGitHub.get` refused `..` anywhere
in a path, and GitHub's compare path is `<sha>...<sha>`; it now refuses a `..` segment only. The test then
passes. **It does not close 1.7:** from a cloud session `api.github.com` answers the private suite-builder 200
with an invalid token and with none, so the pass shows the adapter's reading and parsing, not the token's
access. 1.7 closes on a run outside such a session (Ghost's machine or a runner with only the token), or on
Ghost's word that this pass is enough. Then close it as `docs/how/do-a-todo-step.md` says, the facts it read
recorded in the journal by kind and count, never the token.

Never log, print or write the token, nor paste it into a commit, the journal or a report.

## Then

Nothing else in `TODO.md` is open to an agent: 2.1 waits on Ghost's 30 hand-labelled repositories, and
every later step on 2.1. Done since the last handoff: 1.12 (mutation survivors), 1.13 (baseline scoring,
Suite Builder at 32762a6 reads 100), `--facts`, `-PshowGoldens`, the corpus under rubric 4.

## Waiting on Ghost

- **2.1, the calibration set:** Ghost proposed public repositories from the lab (ghost-in-the-kernel,
  uhneer, CryptoDevving). Open: fixtures keyed by an opaque id with the id-to-repository map kept outside the
  repository (E-014); asking uhneer and CryptoDevving first; CryptoDevving's cloned projects as the "not own
  work" cases for the admission gate (E-056). An agent may list the three accounts' public repositories
  (language, size, kind, domain, no scores) for Ghost to choose from, once Ghost says yes.
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
