# Handoff: what to work on next, and what it waits on

## Start here: 1.7's live check (2026-10-23)

Ghost has made a GitHub token and is adding it to the environment as `EVALUATOR_GITHUB_TOKEN`: a
fine-grained token with read-only access to public repositories and no permissions. **It cannot read the
live test's repository:** Ghost-in-the-Kernel-Labs/suite-builder is private (Ghost, 2026-10-23). A request
that seems to read it unauthenticated from a cloud session goes through the session's proxy, which carries
the session's own GitHub access: no proof of what is public. Before step 2, ask Ghost which way 1.7 goes:
(a) a token scoped to that repository (owner Ghost-in-the-Kernel-Labs, only suite-builder, Contents
read-only), or (b) the public token, with the live test pointed at a public repository of Ghost's with signed
and unsigned commits, which changes 1.7's check and is Ghost's call. With the public token alone, the live
test's 404 is expected, not a bug.

1. Check the session has it, without printing it: `test -n "$EVALUATOR_GITHUB_TOKEN" && echo set`.
   Not set: the session started before Ghost saved it; say so and stop.
2. Run the live test alone, its output to a file: `gradle test --offline --tests
   'evaluator.github.AuthorshipLiveTest' > build/run.log 2>&1` (`mkdir -p build` first), then grep it for
   failures. `--offline` is Gradle's; the test itself reaches GitHub (MOD-05).
3. It passes: 1.7's check holds (TODO.md 1.7). Close 1.7 as `docs/how/do-a-todo-step.md` says, the facts it
   read recorded in the journal by kind and count, never the token. It fails: read the failure, fix what is
   ours, and if GitHub refuses (401, 403, a rate limit) say so to Ghost rather than retry in a loop.

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
