# Handoff: what to work on next, and what it waits on

## Token posture, step 1 adopted (2026-11-01, E-092, J-140); steps 2 to 4 are next

Done: tiers (small, mid, strong), the one tier-to-model table, the countable stop limits (tool calls small 40, mid 120), the brief, the report and the coordinator's rules, all in `docs/how/hand-out-a-task.md`, held by `test/TierNamesTest.java` (no model family name in `CLAUDE.md` or `docs/how/` outside that file and three exempt files). Step 2 done (2026-11-12, E-093, J-141): `gradle metrics` gives each routing row's working set in tokens (`Metrics.workingSets`); the largest, 49,522 for "What to work on", is reported, its budget Ghost's to set. Step 3 done (2026-11-12, E-094, J-142): `gradle scoreItems` places each open item; fifteen of eighteen name no file or layer and are unplaced. Step 4 done (2026-11-12, E-095, J-143): the worker log `docs/WORKERS.jsonl`, read by `gradle scoreItems --args=--tune`; the coordinator adds a line when a worker hands back. The navigation review is done (J-144). **Next, in order:** the tithe (Pass 11, due), the dependency dry run (below), and 2.2n. To confirm for Ghost: the navigation review's open questions go to the strong tier (Opus) where they went to Fable; the exemptions; `.claude/agents/*.md` still say `model: sonnet` and `model: haiku` (pinned, so changed only by Ghost).

## Dependency-update posture adopted (2026-11-01, E-091, J-136 to J-138); the dry run is next, by the orchestrating session

The posture is in: `docs/how/update-a-dependency.md` (the method for npm, PyPI, Maven Central jars, tarballs and images), `third_party/REVIEWED.md` (a row for each of the 90 pins, all "pinned before E-091, not yet reviewed by this posture"), the scan `dependency-review/dependency_scan.py` (`python3 -I -B dependency-review/dependency_scan.py OLD NEW --out OUT`, folders outside the repository), and the two read-only agents `.claude/agents/patch-reviewer.md` and `.claude/agents/reputation-checker.md`, each pinned by SHA-256 in `test/DependencyReviewTest.java`. No pin, hash, lockfile or allow-list was changed, no agent was run and no call was made. The dry run (one real update read by the whole method, started by the orchestrating session with the two agents) has not been done; it is the first test of the how-file's commands, which were written and not yet run on a real artifact. See `docs/FOR-GHOST.md` for what only Ghost can settle.

## Then: 2.2n, the prompt iterated to the floor (2026-10-29, J-134)

2.2m is done: all 30 labelled repositories judged live, the record committed under the prompt's hash (`agent-runner/records/5b05dcac...json`). No field is live yet: Ambition 21 of 30 (needs 24), Quality exactly 23 (needs 24), Quality within one 28 (needs 29), kind 22 (needs 27, under its constant answer of 23). A run cost $0.95; most of it is the few Opus calls. One answer was cut at the 8000-token output limit (the one partial). 2.2n revises the prompt one hashed version at a time, each run under caps.json, at most 3 iterations before Ghost is asked (caps.json); kind is the field furthest from the floor. The run: `gradle compileJava --offline`, then from `agent-runner/`, `python3 -B -m agent_runner.calibrate --corpus <checkouts outside the repository, one per id> --out ../build/<run> --live`. The checkouts are kept outside the repository between iterations and deleted when Ghost says 2.2n is done with them.

## Fixes from the calibration runs (2026-10-29, J-131 to J-133)

The kernel crashed on 5 of 30 repositories on a source file holding an emoji: the tree-sitter binding's string entry indexes modified UTF-8 (6 bytes per emoji) while `Source` slices real UTF-8. `TreeSitterAdapter.parse` now feeds the real bytes (J-131, finding F-001; `EmojiOffsetsTest`; no golden moved). The budget's reserve was `ceil(bytes / 3)` and 13 of 77 live calls used more than their reserve; it is now `ceil(bytes / 2)` (J-132). The reserves are about 1.5 times higher than before, so the per-call and per-evaluation caps hold fewer prompts: 2.2m's third run fitted them (no refusal, J-134). A third fix, J-133: `TypeScriptImports` reads only `import_specifier` children, so a comment inside an import's braces no longer crashes the kernel.

## The judge harness, 2.2a to 2.2m built (2026-10-26, brought up to date 2026-10-29)

2.1 is closed (J-106). 2.2, the judge harness, is sliced into 2.2a to 2.2n (J-109; the plan is `docs/DESIGN.md`, "Step 2.2 sliced"; each part's check is in
`TODO.md`). 2.2a to 2.2m are done (J-110 to J-134; `docs/how/judge-harness.md` lists what each built: the scaffold and import guard, `evaluator brief`, the summary builder, the answer schema and validator, the prompt and its hash, `Judge`/`FakeJudge`/`ReplayJudge`, the `Budget`, `ensemble.py`'s three runs, `agreement.py`'s figures and records, `evaluator score --judged` (E-085), and now the card's rules for the judged kind: `AgreementRecord` and `JudgedRules` in `ingest/`, `Declined` in `score/`, `--records` and exit status 2 in `cli/`, E-086; the rules are in `docs/how/command-line.md`).

2.2l is built (J-124, E-087): `agent-runner/agent_runner/transport.py` is the one door (`live_judge(budget, output_limit, ...)`, a `BudgetedJudge` over a private standard-library judge; the API key only from `ANTHROPIC_KEY`; the schema enforced by the API's structured output; retries; recordings under `build/`), `budget.py` has dollar caps and a per-model prompt-length limit, Ghost's data is in `agent-runner/caps.json` (per trial $5.00, per evaluation $0.50, output limit 8000, 3 prompt iterations in 2.2n) and `agent-runner/prices/2026-10-26.json`, and the model ids `claude-haiku-5-5` and `claude-opus-5-5` are confirmed. **The one real-API smoke test passed (2026-10-26):** `EVALUATOR_LIVE_SMOKE=1 python3 -I -B -m unittest discover -p 'test_transport.py' -k LiveSmoke` in `agent-runner/`, one Haiku call through the proxy, an answer that parses. Its first run was refused with HTTP 400: the API does not accept `minimum`/`maximum` on an integer, so `transport.api_schema` drops the numeric range keywords from the schema it sends; the file and the prompt hash are unchanged, every range is in its description, and `judgment.parse` still enforces it. **The driver for 2.2m is `agent-runner/agent_runner/calibrate.py` (J-130; how to run it is in `docs/how/judge-harness.md`); its first full run is 2.2m's record (J-134). The repositories are fetched by the orchestrating session outside this repository and discarded after judging (Ghost, 2026-10-29).** 2.2n is next (the first section above). Already decided: `python3` is in the image (E-081), Opus decides a category tie (E-083), a field is live at 80 % and half the gap over its baseline (E-084, the half is to confirm).

**To confirm for Ghost (`docs/FOR-GHOST.md`):** E-087's agent-chosen details (the output limit of 8000, the retry numbers) and E-086, proposed: a clone is scored with a note, the declined outcome's shape and exit status 2, Quality within one as Quality's gate, a kind that is not live decides nothing; and the risk that the 30 labels hold no generated repository, so a live kind figure does not show "generated" is recognised. Also E-085's two rules (own tests make the test-to-code ratio by the judge's class; the wrapper file shape) and 2.2i's margin (E-084).

Agreement against the labels is partly model against model (the labels were drafted by Claude Sonnet agents);
Ghost's review is what keeps it from being circular (J-106).

## What the last session settled and left (2026-10-26, J-123)

**The API key.** Ghost has an Anthropic API key and, at the end of the last session, was putting it in the environment as the variable
`ANTHROPIC_KEY` (the cloud environment's settings, Edit, Environment variables; the name is Ghost's). The Network-secrets form would not
save (the Bearer type with header `x-api-key`, no prefix, host `api.anthropic.com`, resolved correctly in its preview but the save failed),
so the variable is the route. Check it without printing it: `test -n "$ANTHROPIC_KEY" && echo set`. Not set means the session started before
Ghost saved it: say so and stop; never ask for the key in the chat, never log, print or write it. The environment's network policy must allow
`api.anthropic.com`; if a call is refused, tell Ghost the host and see the environment network page. `transport.py` sends the key as
`x-api-key` and `anthropic-version: 2023-06-01`, and send no key at all when the variable is unset (so a secret injected by a proxy would also
work). The smoke test runs on an explicit switch, not on detecting a key. Ghost should also set a monthly spend limit on the Console workspace:
the `Budget` only covers calls through the harness.

**Asked and answered (2026-10-26):** the caps, the transport (standard library), the model ids and the prices are Ghost's and now data (E-087); the Haiku 5.5 price is $0.10 / $0.50 per million tokens up to 100,000 prompt tokens and $0.50 / $2.50 above, so a longer prompt is refused (`PromptTooLong`). The first live pass over the 30 labels is about 90 Haiku runs and some Opus runs, a few dollars (trial 4 cost about $1.30 for 150 Haiku runs); the first run is the measurement.

**A cheaper route offered and not decided:** tune the prompt with Claude Code subagents standing in for Haiku and Opus (no key, session usage
instead of API billing), then make one real measurement through the API for the record that counts. It breaks "the judge gets no tools"
(E-013), counts no real tokens, and is still model against model, so such a record must not be stored under the live hash. If Ghost wants it,
it is a new slice before 2.2m.

**Outside the repository (lost with the last session unless Ghost saved them):** the id-to-repository map for `c001` to `c030`, the labelling
sheet, the three evidence files behind each label, the domain of each label (the labels carry none, E-080), and rough sizes of about 80
candidate repositories. Ghost was asked to copy them out of the old session. If they are gone, the labels still stand; only the means to
audit or extend them is lost, and 2.2m's re-fetching of the 30 labelled repositories (question 6) needs the map. Nothing from them may enter
this repository (E-014). Labels were drafted by Claude Sonnet agents under the rubric and confirmed by Ghost.

**Provenance, learnt from one owner's own claims (no names here):** for twelve public repositories whose owner gave human-share claims
(under 5 % AI, human, a known AI rewrite, about 30 %), the kernel's layer-3 history facts did not separate the groups; they track size and
cadence. The two known rewrites stand out by hand (one large commit over most source files, the same day) but barely move the kernel's
facts. Large-commit measures are noise when a vendored file is the big commit. Candidate facts, a design call for Ghost and not a TODO yet:
a burst of changes over most files in one day, and large-commit measures that skip vendored and generated files. Line-level attribution to an
AI is not possible from history or from correct code; do not promise it.

**Environment tips learnt:** if `dockerd` will not start with "process with PID n is still running", the pid file is stale: confirm with
`ps -p n`, then `rm -f /var/run/docker.pid` and start it again. A finished task's worktree is removed by the session that merged it
(`docs/how/commit-and-push.md`).

## Waiting on Ghost

- **The token optional** (decided 2026-10-29, E-090; to build): public repositories read without one, a token only for the
  rate limit, and a rate-limit refusal said plainly; private repositories later through a GitHub App.
- The rest is in `docs/FOR-GHOST.md`.

## How the work is done here

Each task goes to a fresh Claude Sonnet agent in its own git worktree (E-068); the session that hands it out
reviews its diff, runs the tests and `scripts/prepush.sh`, then merges and pushes. In a worktree,
`MainTest.thisRepositorysCard` fails by design (`.git` is a file there). The last navigation review is J-144 (2026-11-12), the next due in about ten commits. The last tithe is Pass 10 (J-135); nine commits have passed since it, so the next tithe (Pass 11) is due now.

## A session's environment

- **An unexplained failure (2026-11-01, J-139):** one `scripts/prepush.sh` run on the posture's commits (6f18f89) failed one test in the image; nine clean-clone runs after it passed, and the report went with the clone, so the test is not known. A test that fails once in ten is a race (E-072). If `prepush.sh` fails again, before rerunning keep the clone's `build/test-results/test/` (the script deletes it) and name the test. A next step for a tithe: have `prepush.sh` print each failing test's name before it deletes the clone.

- `scripts/prepush.sh` needs Docker: start `dockerd` in the background before a push; it stops at the
  background time limit, so start it again when needed. The image builds on first use.
- A cloud checkout may be shallow (`git rev-parse --is-shallow-repository`): `git fetch --unshallow origin
  main` before anything reads the history.
- On a sandbox run, proven on paper only: the install phase through the registries proxy; the first hosted
  machine runs `sandbox/setup.sh` and a fixture with a locked dependency.
