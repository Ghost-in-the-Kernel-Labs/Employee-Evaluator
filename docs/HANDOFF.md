# Handoff: what to work on next, and what it waits on

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. 1.7's live check waits on
Ghost's token (below), so the work is 1.13, baseline scoring (E-070, E-071): Size (84,971 lines at 32762a6) and Complexity (13,361 total cognitive and depth 28, E-074) are built and counted (`Metric.counted` lifts their ceiling), so Maintainability's parts are next: read each part's measure at the baseline on a clean clone the same way, record it beside the others in `Formulas`, and make Maintainability counted in `Metric` only when its formula moves. 1.12, the mutation survivors, is
done: runs 2 to 14 of docs/MUTATION.md closed `rubric/`, `score/`, `history/`, `cli/`, `github/` and `analyze/` (six passes, the
last the sandbox's network and Docker). Phase 2 starts at 2.1, which waits on Ghost's labels.

## Next steps from the navigation review (J-093)

Larger than an instructions change, so not done in the review: (1) `evaluator score --facts`, printing the
repository-wide fact values (source lines, total cognitive complexity, depth) so a baseline can be recorded
without a scratch program (met by the Size and Complexity agents); (2) `gradle test -PshowGoldens`, printing
each changed golden's diff so `docs/how/change-a-formula.md` step 6 can be followed in its order, reading before writing.

## What waits on whom

- **1.7's live check, on Ghost:** a fine-grained, read-only token for Ghost-in-the-Kernel-Labs/suite-builder
  (Contents and Metadata: read, with an expiry), set in the environment's settings as
  `EVALUATOR_GITHUB_TOKEN`; a new session picks it up and `AuthorshipLiveTest` runs. The session's own
  GitHub credentials are not the product's and are not used.
- **2.1, on Ghost:** 30 repositories hand-labeled on Ambition and Quality.
- **On a run, proven on paper only:** the sandbox's install phase through the registries network and its
  proxy (proven on loopback); the first hosted machine runs `sandbox/setup.sh` and a fixture with a locked
  dependency.

## A session's Docker

`scripts/prepush.sh` needs a Docker daemon. A cloud session starts one with `dockerd` in the background
(it stops at the background time limit; start it again before a push). The image builds on first use.

Ghost's own open items (not an agent's work) are in `docs/FOR-GHOST.md`.
