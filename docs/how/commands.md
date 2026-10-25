# Commands, and how to read a long run

- **The fast loop:** `gradle test --offline` (JDK 21, Gradle 8.14.3), doc and unit tests, under 20 s warm.
- **Before a push:** `scripts/prepush.sh` runs the fast loop on a clean clone in the pinned image
  (`Dockerfile`, AGT-01). It refuses a stale kernel map, a push past sixteen commits without a tithe, and one
  past twenty without a navigation review. It needs a Docker daemon: in a cloud session start `dockerd` in
  the background first (it stops at the background time limit; start it again).
- **Mutation:** `gradle mutate -Ptarget=evaluator.<module>.*` runs PIT over one module, in minutes; the whole
  kernel takes long (E-019, `docs/MUTATION.md`).
- **The metrics:** `gradle metrics` prints `docs/AGENT.md`'s numbers, the tithe and review cadences, and every
  `// Unsure:` line.
- **Generated files** (never edited by hand): `gradle kernelMap` (`docs/KERNEL-MAP.md`), `gradle modulesDoc`
  (`docs/MODULES.md`), `gradle decisionsDoc` (`docs/DECISIONS.md`).
- **Goldens:** `gradle test -PshowGoldens` writes nothing and prints, in each failing golden test's message, every changed golden's
  changed lines (`@@ golden line N, new line M @@`, then `- old` and `+ new`); read them, then `gradle test -PwriteGoldens` rewrites the
  goldens. The two properties together are refused. Both go to a file like any test run (E-018, E-077). Every golden test goes through `test/evaluator/Goldens.java`. Python goldens (`agent-runner/`) are not Java's: `EVALUATOR_WRITE_GOLDENS=1 python3 -I -B -m unittest discover` in `agent-runner/` rewrites them (read `git diff` first; `docs/how/judge-harness.md`).
- GitHub's workflow runs only by hand (E-040): never count on it.

## A long run's output

A gradle run here (`test`, `decisionsDoc`, `kernelMap`) can pass the 120 s tool timeout: start it with `run_in_background`, redirected to the file below, and wait for it.

`mkdir -p build` first: a fresh clone or worktree has no `build/`, and the shell fails on the redirect before the run starts.

`gradle mutate`, `scripts/prepush.sh` and `gradle test` always write to a file (> build/run.log 2>&1). You, or
a sub-agent you hand the run to, grep it for failures first ("FAILED", "prepush:"), and read nothing more when
there are none. A failing test's message is in its report under build/test-results/test/, not the console.
