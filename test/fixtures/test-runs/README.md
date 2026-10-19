# test-runs

Repositories whose own tests the sandbox runs (TODO.md 1.5, E-037), one per outcome: `py-pass` (three
tests pass), `py-fail` (one of three fails), `py-network` (a test reaches for a name that can never
resolve, `.invalid`), `node-pass` (`node --test`, two pass). Each needs no package, so no install runs.
`test/goldens/test-runs/<name>/` holds the reports a real run of each wrote (pytest 9.1.1 under coverage.py
7.16.2, c8 12.0.0 over Node 22), recorded 2026-10-19 by running each plan's script outside a container,
the fixtures being ours. `TestRunnerFixturesTest` holds the runner to them in the fast loop; with
`gradle test -Pdocker` it runs them in the real sandbox and expects the same facts.
