# The score card's shape: a metric, a fact's fields, the card's fields, an invariant

**Start here:**
- `docs/SCORE-CARD.md`
- `rubric/evaluator/rubric/Fact.java`
- `rubric/evaluator/rubric/ScoreCard.java`
- `rubric/evaluator/rubric/Metric.java`

**Contract:** `docs/SCORE-CARD.md` (SC-01 onward).

**Tests:** `test/ScoreCardDocTest.java`, `test/evaluator/rubric/`.

## Steps

- The document and `rubric/` use the same words in the same order: metrics, a fact's fields and states, the card's fields, the tiers. Change both together; `test/ScoreCardDocTest.java` compares them.
- An invariant SC-nn is cited in `rubric/` or `score/` where it is held (`// Invariant: SC-07`), or names the open TODO step that will hold it.
- A new fact or formula has its own instructions: `docs/how/add-a-fact.md`, `docs/how/change-a-formula.md`.
- A field added to the card changes every card golden: `gradle test -PwriteGoldens`, review the diff.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
