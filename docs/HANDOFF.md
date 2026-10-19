# Handoff: what to work on next, and what is waiting on whom

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. Phase 1 steps 1.1 (the
scaffold) and 1.2 (the rubric's types and ingestion) are done; 1.3 (static analysis adapters) is next.

## For Ghost

- **Review E-025 and E-026.** Built under 1.2 and recorded as in force, but they are judgment calls
  on how Size is counted: which folders, names and headers set code aside, and that Size counts
  non-blank lines and folders (modules wait for 1.3's dependency graph). Change either by a new
  decision; the small-app golden shows what each rule does.
- **The image.** `docker build -t evaluator .` has still never run: neither workspace so far had a
  Docker daemon. Gradle itself has now run the build (J-005), so `scripts/prepush.sh` is the one
  untried piece. The first run on your machine proves it.
- **tree-sitter for 1.3.** TODO.md 1.3 wants tree-sitter vendored for TypeScript and Python. Its Java
  bindings carry a native library per platform (Linux, Windows, macOS), which the offline, vendored
  build (E-021) has not had yet. Before 1.3 starts: approve vendoring it (jars, native libraries,
  licenses, MANIFEST.md rows), or choose another route (a pure-Java parser per language, or the
  adapters in a separate process). Vendoring needs a machine with network access to fetch the files.
- **Calibration set.** Phase 2 cannot start until 30 repositories are hand-labeled on Ambition and
  Quality (TODO.md 2.1). Only Ghost can label; the fixture format comes from Opus first.
- **Mutation tool.** E-019 is proposed until a Java mutation tool is vendored (PIT, Apache-2.0, is the
  candidate; its jar and license go under `third_party/` with a MANIFEST.md row). Decide at 1.10.
- **Naming.** "Employee Evaluator" may read as surveillance to applicants (docs/DESIGN.md, open
  questions). No decision needed before phase 3.

## For the next session (1.3)

Read `CLAUDE.md`, then TODO.md 1.3 and Ghost's answer on tree-sitter above; without one, stop and
ask. The Java adapter needs nothing new (the JDK's parser, as `scripts/Scan.java` uses). `analyze/`
reads `ingest/`'s Inventory (`Ingest.read()`); its Complexity facts are built with `Fact.measured()`.
Add a fixture beside `test/fixtures/small-app/` when a case needs one, its golden under
`test/goldens/`. Run `gradle kernelMap` before the commit, and write the journal entry.
