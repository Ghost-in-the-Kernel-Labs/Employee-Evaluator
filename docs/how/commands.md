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
- **Goldens:** `gradle test -PwriteGoldens` rewrites them; review every changed value (E-018).
- GitHub's workflow runs only by hand (E-040): never count on it.

## A long run's output

`gradle mutate`, `scripts/prepush.sh` and `gradle test` always write to a file (> build/run.log 2>&1). You, or
a sub-agent you hand the run to, grep it for failures first ("FAILED", "prepush:"), and read nothing more when
there are none. A failing test's message is in its report under build/test-results/test/, not the console.
