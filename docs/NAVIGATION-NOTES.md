# Navigation notes: each agent's debrief since the last review

After its task, each agent appends one entry here (`docs/how/debrief-after-a-task.md`, E-068): what was hard
to find, wrong, missing or slow on its way, and what it suggests. About every ten commits a navigation review
reads them for points agents converge on (`docs/how/navigation-review.md`), records its verdicts in the
journal, and wipes the entries below this line; git keeps every past set.

---

### 2026-10-29 · the 2.2m calibration driver · Claude Sonnet 5.5
- **Route:** CLAUDE.md -> do-a-todo-step.md -> judge-harness.md; the module docstrings of agreement.py, budget.py, ensemble.py and transport.py did the rest.
- **Complaints:** how to run the built CLI is only in command-line.md line 15, not in judge-harness.md, where the brief is described. `python3 -I -m unittest tests.test_x` does not work (no cwd on the path); only `discover -p 'test_x.py'` does. Nothing says that the guard forbids copying `os.environ` (a child's environment is cleaned with `env -u`).
- **Suggestions:** one line in judge-harness.md for the `discover -p` form as the way to run one file, and for `env -u` as the guard-safe way to clean a child's environment.
