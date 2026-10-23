# A submission's own tests: the sandbox, the plans, the reports

**Start here:**
- `analyze/evaluator/analyze/TestRunner.java` (the orchestration; its quarantined copy and cleanup are Workspace.java, the report files found and read under the size cap ReportFiles.java, same folder)
- `analyze/evaluator/analyze/TestPlans.java`
- `analyze/evaluator/analyze/DockerSandbox.java`
- `analyze/evaluator/analyze/Reports.java`

**Contract:** E-037, E-038 (the sandbox), E-033, E-013: a submission's code runs only in the sandbox.

**Tests:** `test/evaluator/analyze/TestRunnerTest.java` (a fake sandbox), `TestPlansTest`, `DockerSandboxTest`, `ReportsTest`, `test/fixtures/reports/`.

## Steps

- Never run a submission's code outside the sandbox, not even to try something. It is stripped of agent and editor configs and git hooks first (`ingest/evaluator/ingest/Quarantine.java`).
- Unit tests use the fake sandbox (`test/evaluator/analyze/RecordedSandbox.java`); the real one runs with `gradle test -Pdocker` after `sandbox/setup.sh`, with a Docker daemon.
- A report format: a fixture under `test/fixtures/reports/` and a case in `ReportsTest`; files are read under a size cap (`Reports.MAX_BYTES`), never through a link.
- The image's pins are held by `test/SupplyChainTest.java`: a new tool in `sandbox/` comes pinned by hash, with Ghost's approval (the supply-chain rules in `CLAUDE.md`).

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
