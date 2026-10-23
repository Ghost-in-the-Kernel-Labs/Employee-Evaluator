# Commit verification and push attribution: provenance layer 2

**Start here:**
- `github/evaluator/github/Authorship.java` (`Authorship.facts()`, E-043)
- `github/evaluator/github/GitHubApi.java`
- `github/evaluator/github/HttpGitHub.java` (live)

**Contract:** E-043; MOD-05: the only module that reaches the network, and only GitHub.

**Tests:** `test/evaluator/github/AuthorshipTest.java` (recorded answers), `AuthorshipLiveTest` (runs only with `EVALUATOR_GITHUB_TOKEN`).

## Steps

- Tests use recorded answers; a new case is a recorded answer, never a live call in the fast loop.
- The token is read from the environment and never logged or written to a file.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
