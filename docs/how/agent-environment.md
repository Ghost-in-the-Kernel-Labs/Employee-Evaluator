# The agent's environment: the metrics, the kernel map, mutation runs

**Start here:**
- `docs/AGENT.md` (the contract AGT-01 onward and the metrics table)
- `docs/KERNEL-MAP.md`
- `scripts/Metrics.java`
- `scripts/KernelMap.java`

**Contract:** E-015, E-021, E-062; E-019 (`docs/MUTATION.md`).

**Tests:** `test/MetricsTest.java`, `test/KernelMapTest.java`.

## Steps

- `gradle metrics` prints every metric; `test/MetricsTest.java` fails on one past its target. A limit is never raised to pass: make room.
- `gradle kernelMap` appends new functions and closes gone ones (E-062); never edit the map by hand.
- A tithe pass records a column in `docs/AGENT.md`'s table.
- A mutation run over a module is recorded in `docs/MUTATION.md`; each survivor gets a test or a harmless row.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
