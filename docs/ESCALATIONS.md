# Escalations: each time a Sonnet agent asked Opus, and whether its route gave it what it needed

One row per escalation (`docs/how/escalate-to-opus.md`, E-064), newest last. Type is one of `missing-step`,
`not-reached`, `wrong-step`, `hard-problem`, `no-replay`. Three rows of a type not yet reviewed go to Ghost:
each marked `raised`, and an item in `docs/FOR-GHOST.md` naming the type; Ghost marks them
`reviewed: <date>`. `test/EscalationsTest.java` holds this table.

| Date | Task | Route | Type | What was missing or wrong | Raised |
| --- | --- | --- | --- | --- | --- |
