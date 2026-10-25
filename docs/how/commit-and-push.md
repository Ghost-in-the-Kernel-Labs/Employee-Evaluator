# Commit and push

- **Author:** commits are authored as Ghost, so they count on Ghost's profile (E-020). Before the first
  commit of a session: `git config user.name Ghost` and `git config user.email
  118085043+ghost-in-the-kernel@users.noreply.github.com`. An agent keeps its own Co-Authored-By line.
- **The message:** what changed and why; the decision (E-xxx) and journal entry (J-xxx) it goes with. No
  person's real name: the maintainer is "Ghost".
- **One branch, `main`;** a finished step lands on it at once (E-020).
- **Every eighth commit is a tithe** (its subject starts "Tithe", E-015); about every tenth, a navigation
  review (subject "Navigation review", E-068). The pre-push refuses past sixteen and twenty.
- **Push only after `scripts/prepush.sh` passes,** gated, never chained: `if scripts/prepush.sh > build/run.log
  2>&1; then git push origin main; fi` (J-045: a push chained with `;` once sent a red commit to main).
- **A finished task's worktree and branch** are removed by the session that merged it, after the push succeeded and never before: `git worktree remove <folder>` then `git branch -d <task branch>` (`-d` refuses an unmerged one, which is the check). Remove only what you started; a plain clone made for a mutation or tithe run goes the same way once its numbers are recorded.
- **Never force-push** unless every agent working on the project agrees (`docs/how/when-agents-conflict.md`,
  E-067). Never revert, rewrite or push over another agent's commit.
