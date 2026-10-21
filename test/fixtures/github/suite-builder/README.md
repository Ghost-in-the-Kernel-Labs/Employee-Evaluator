# github/suite-builder

GitHub's answers for Suite Builder (Ghost-in-the-Kernel-Labs/suite-builder), for `github/`'s
authorship (TODO.md 1.7, E-043), applicant `ghost-in-the-kernel`. Each file is one API path's answer,
the path's slashes and query as written in `AuthorshipTest`.

What is real and what is made, said plainly:

- **The commits are real:** seven of Suite Builder's, their hashes, dates and the accounts GitHub
  attributes them to (read through the GitHub tools, 2026-10-21): two pull-request merges GitHub
  signed (committer `web-flow`), three of Ghost's unsigned, and two of Claude's (GitHub's `claude`
  account), one of them committed by Ghost.
- **The verification verdicts are read from git, not GitHub:** the GitHub tools do not return them;
  git shows the two merges signed by GitHub's key (`%G?` E, committer noreply@github.com) and the rest
  unsigned (N).
- **The events and the compare are made for the test,** in GitHub's documented shape: a push of
  Ghost's whose compare holds two of his commits, a push by another account that must not count, a
  new branch's push (before all zeros), and an event that is not a push.

The live API's own answer is checked by `AuthorshipLiveTest`, which runs when `EVALUATOR_GITHUB_TOKEN`
is set; until then, TODO.md 1.7's check is not met.
