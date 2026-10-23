# Read a repository: the inventory, its languages, what is set aside

**Start here:**
- `ingest/evaluator/ingest/Ingest.java` (`Ingest.read()`)
- `ingest/evaluator/ingest/SetAside.java` (vendored and generated code, E-025)
- `ingest/evaluator/ingest/Size.java` (the Size facts, E-026)
- `ingest/evaluator/ingest/Languages.java` (file endings to languages)

**Contract:** E-025, E-026; E-013 (a submission is hostile: read, never run).

**Tests:** `test/evaluator/ingest/`, the golden `test/goldens/small-app/inventory.json`.

## Steps

- A rule about what is set aside goes in `ingest/evaluator/ingest/SetAside.java` with its reason in E-025's terms; a small fixture case in `test/evaluator/ingest/`.
- The inventory golden changes when what is read changes: `gradle test -PwriteGoldens`, review the diff; a refactor changes no golden (E-018).
- A submission is stripped before anything reads it (`ingest/evaluator/ingest/Quarantine.java`, E-033).

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
