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

### 2026-10-29 · the emoji crash and the reserve estimate · Claude Sonnet 5.5
- **Route:** CLAUDE.md -> add-a-language-adapter.md (nothing on how the parse is fed) -> TreeSitterAdapter.java; judge-harness.md and budget.py's docstring for the estimate.
- **Complaints:** nothing says that the tree-sitter binding's `parse` hands the reader the input array as its buffer, or that a null reader segfaults the JVM; I found both by running a scratch main. `DuplicationTest` was the only thing that caught the first try's corrupted bytes, and its failure text names a golden, not the cause. FINDINGS.md's header says "phase 1 step 1.3" is where findings come from, which hid that a tool bug like this one belongs there. A mutation run in a plain clone needs the changed files copied by hand.
- **Suggestions:** one line in add-a-language-adapter.md: "feed the parse the file's real bytes through `parse(byte[], null, reader, UTF8)`, on a copy" (J-131). A line in agent-environment.md giving the clone-and-copy mutation steps as a command.

### 2026-10-29 · a comment inside TypeScript import braces crashed the kernel · Claude Sonnet 5.5
- **Route:** CLAUDE.md -> the task brief named the file; complexity-and-dependencies.md was not the TypeScript import file's how-file (none is), so I went straight to TypeScriptImports.java and the J-131 commit for the shape.
- **Complaints:** no how-file says what the import readers assume about tree-sitter children (comments are named nodes); the full `gradle test` takes 2m40s and the 120 s tool limit moves it to the background; `DocTests > metrics` fails until `gradle kernelMap` has run, and its message names the artifact, not the command.
- **Suggestions:** one line in add-a-language-adapter.md: "a named child may be a comment: test its type before reading its fields" (J-133).

### 2026-10-29 · the tithe, Pass 10 · Claude Sonnet 5.5
- **Route:** CLAUDE.md -> agent-environment.md ("A tithe pass") -> commands.md; `git show a37b802` and J-125 for the shape.
- **Complaints:** the metrics list names a test's helper and its test methods as "no comment" with no pointer to whether the class's Javadoc counts (it does not). HANDOFF.md keeps old "Start here" sections beside the new one, so after 2.2m two sections disagreed on what is next. Each slice's agent updated its own paragraph and none the neighbouring ones (a repository-wide `git grep "2.2m"` finds the stale ones). The AGENT.md table is edited with a script by hand: a column is added in 13 places.
- **Suggestions:** in agent-environment.md's tithe bullet: "grep the slice numbers just closed (`git grep -n '2.2m'`) for future tense". HANDOFF.md: one "Start here" section only, the older ones under a "Settled" heading.

### 2026-11-01 · adopting the dependency-update posture · Claude Sonnet 5.5
- **Route:** the brief -> CLAUDE.md -> `test/SupplyChainTest.java`, `docs/how/add-or-change-a-module.md`, `docs/how/judge-harness.md` (for where Python may live), `test/AgentRunnerTest.java` for the gate's shape.
- **Complaints:** `DocsTest` flags any backticked word with a slash or a `.md` ending in a how-file (a URL, `old/`, `/etc`, a bare `MANIFEST.md`) and names it only after a full 2 to 3 minute `gradle test`, three runs for me; the routing metric counts a row's file plus every "Start here" bullet, so a fifth bullet made the row six. No page says where a Python tool that is not the judge harness may live, or that there is no list of auto-execution surfaces here (the brief assumed one).
- **Suggestions:** in `docs/how/commands.md`: "DocsTest and Metrics can be run alone in seconds: `gradle test --tests DocTests.docs --offline`" (if true; I did not find a faster loop). In add-or-change-a-module.md: one line that a standard-library Python folder outside the map needs its tests held by a Java gate as `AgentRunnerTest` does, with `DependencyReviewTest` as the second example.
