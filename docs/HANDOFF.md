# Handoff: what to work on next, and what it waits on

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. 1.7's live check waits on
Ghost's token (below), so the work is 1.12, the mutation survivors: `rubric/`, `score/`, `history/`, `cli/` and `github/` are done
(docs/MUTATION.md, runs 2 to 4 and 6 to 8); `analyze/` is in six passes by concern: the Java classes (run 9), Python with the tree-sitter base (run 10) and TypeScript/JavaScript with the navigability checklist (run 11) and the dependency graph with duplication (run 12) are done;
next: test runs (`TestRunner`, `TestRuns`, `TestPlans`, `Reports` and the sandboxes), then the rest in order (it is 1,104 of run 1's 1,714 mutants). Phase 2 starts at 2.1, which waits on Ghost's labels.

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
