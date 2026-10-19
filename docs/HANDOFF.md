# Handoff: what to work on next, and what is waiting on whom

## What to work on next

`TODO.md`, from its first item, in order; what is done is in `docs/DONE.md`. Phase 1 steps 1.1 (the
scaffold) and 1.2 (the rubric's types and ingestion) are done; 1.3 (static analysis adapters) is next.

## For Ghost

- **The image.** `docker build -t evaluator .` has still never run: neither workspace so far had a
  Docker daemon. Gradle itself has now run the build (J-005), so `scripts/prepush.sh` is the one
  untried piece. The first run on your machine proves it.
- **Calibration set.** Phase 2 cannot start until 30 repositories are hand-labeled on Ambition and
  Quality (TODO.md 2.1). Only Ghost can label; the fixture format comes from Opus first.
- **Mutation tool.** E-019 is proposed until a Java mutation tool is vendored (PIT, Apache-2.0, is the
  candidate; its jar and license go under `third_party/` with a MANIFEST.md row). Decide at 1.10.
- **Naming.** "Employee Evaluator" may read as surveillance to applicants (docs/DESIGN.md, open
  questions). No decision needed before phase 3.

## For the next session (1.3)

Read `CLAUDE.md`, then TODO.md 1.3. The TypeScript and Python adapters use tree-sitter (E-028). Before vendoring
anything, prove two things and write each down in the journal: which Java binding works on the
kernel's Java (the official one is believed to need Java 22+; a JNI binding on Java 21, or Java 25, is
Ghost's call), and that its native library loads in the image with the network off. If either fails,
stop and ask Ghost (ANTLR, E-027, is the fallback). Then vendor the jars, the native libraries and the
Python and TypeScript grammars under `third_party/`, each with its license and a MANIFEST.md row.
The Java adapter needs nothing new (the JDK's parser, as `scripts/Scan.java` uses). `analyze/`
reads `ingest/`'s Inventory (`Ingest.read()`); its Complexity facts are built with `Fact.measured()`.
Padding the applicant writes themselves is not set aside (E-025 sets aside only what they did
not write): Ghost, 2026-10-19, "someone just spamming imports is not a good candidate". With the
dependency graph, make imports nothing uses a fact per file (`MAINTAINABILITY.unused_imports`, say),
so padding counts against the applicant; 1.4's duplication catches the copy-paste kind.
Add a fixture beside `test/fixtures/small-app/` when a case needs one, its golden under
`test/goldens/`. Run `gradle kernelMap` before the commit, and write the journal entry.
