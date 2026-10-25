# The judge harness: `agent-runner/` (phase 2, TODO.md 2.2)

**Start here:**
- `TODO.md`, the open part of 2.2 and its check
- `docs/DESIGN.md`, "Step 2.2 sliced" (the order, what each part needs, the findings)
- `agent-runner/agent_runner/guard.py` (what a file there may not do)

**Contract:** E-001, E-003, E-013, E-014, E-017, E-055, E-057, E-081, E-082.

**Tests:** `test/AgentRunnerTest.java` (the gate: `gradle test` runs the Python tests); `agent-runner/tests/`.

## Steps

- The harness is Python 3, standard library only (no `pip`, no third-party package: a package is a dependency, `docs/how/add-or-change-a-module.md` does not cover it, ask Ghost). It is not a Java module; `docs/modules.json` does not list it and `ModulesTest` leaves it out of the source.
- Tests run by hand from `agent-runner/`: `python3 -I -B -m unittest discover` (`-I` isolates it from `PYTHON*` variables and the current directory; `-B` writes no bytecode). Without `python3` the gate reports PENDING and stays green; the pinned image has it (E-081).
- The one door to the network and to a key is agent_runner/transport.py (2.2l), the counterpart of MOD-05. Until it exists no file imports `socket`, `http`, `urllib`, `ssl`, `anthropic` (or another network or model-client module in `guard.py`) or reads an environment variable that names a key; `agent-runner/tests/test_guard.py` fails on one.
- The kernel imports no model client (E-003); the harness reaches it only as facts with evidence (E-017). The judge is given no tool and reads a prebuilt summary (E-013). Nothing an applicant submitted enters the repository (E-014).
- The summary builder is `agent-runner/agent_runner/summary.py` (2.2c): the brief JSON and a checkout in, one text out, repository text only inside `~~~data` fences. Its goldens are `test/goldens/summaries/`; after reading `git diff`, rewrite them with `EVALUATOR_WRITE_GOLDENS=1 python3 -I -B -m unittest discover` in `agent-runner/`. Summaries of real repositories go only under `build/` (the CLI refuses elsewhere, E-014).
- The judge's answer is held to `agent-runner/schema/judgment.schema.json` by `agent-runner/agent_runner/judgment.py` (2.2d): `parse(text)` returns the answer or raises `JudgmentError` naming each broken field. There is no `jsonschema` library, so the validator is its own strict checker of that one file, and `check_schema` keeps the file inside the subset a structured-output feature accepts (objects with every property required and `additionalProperties` false, string enums, integer and number ranges, arrays of objects or strings, a description on every node); the module's docstring lists it. The kind has six values (E-082) and its description in the schema is the judge's precedence rule, which the 2.2e prompt quotes: change the wording there, not in a copy.
- The judging prompt is `agent-runner/prompts/judge.instructions.md`, `judge.template.txt` and `judge.hash` (2.2e), with the answer schema above; `agent-runner/agent_runner/prompt.py` renders it (`render(summary)` is the exact text sent) and hashes it. The template's first line is `template-version: N`. The hash line is `sha256 <hex>`: the SHA-256 of the instructions' bytes, the schema's bytes and the template version, not of the summary and not of the template body (the golden holds the body; change it and bump the version). `agent-runner/tests/test_prompt.py` refuses a stale hash line; after changing the instructions, the schema or the version, run `python3 -B -m agent_runner.prompt write-hash` in `agent-runner/` (without `-I`, which would hide the package) and read the diff. The golden `test/goldens/prompts/judge-summary-notes.txt` is rewritten with `EVALUATOR_WRITE_GOLDENS=1` as the summary goldens are. The instructions quote the schema's `kind`, folder-class and tier descriptions through `{{kind_rule}}`, `{{folder_classes}}` and `{{tier_rule}}`: change that wording in the schema, never in the prompt. Their sources in `docs/judges/` are listed in the docstring of `agent-runner/agent_runner/prompt.py`; no real repository, owner or person is named in a prompt file (E-014), and the calibration labels are not prompt material.
- A new Python file needs a test in `agent-runner/tests/`; a new part's check is in `TODO.md`.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; the journal entry. Read further only where the code points.
