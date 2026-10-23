# Change a formula: how facts become a score

A metric's score is a formula over the facts on the card (`score/evaluator/score/Formula.java`), one per
metric in `Formulas.phaseOne()` (`score/evaluator/score/Formulas.java`). Every formula is a decision: Ghost
decides a new one or a change, and it is recorded before the code (E-045 onward).

## Steps

1. **The decision first:** a new entry in `theGhost.md`, the formula in its words with an example worked
   by hand; Ghost's go. A change supersedes the old entry, never rewrites it.
2. **The code:** the formula in `score/evaluator/score/Formulas.java` (Maintainability's parts in
   `score/evaluator/score/Maintainability.java`), its constants named, `// Decision: E-xxx` above it. It
   reads only facts (MOD-03).
3. **The rubric's version:** add or change the decision's number in `Formulas.RUBRIC`. That changes
   `Formulas.rubricHash()`, so `theRubricsHash` in `test/evaluator/score/CardsTest.java` and every card
   golden change with it; that is intended.
4. **Tests:** the worked example as a test in `test/evaluator/score/CardsTest.java` (Maintainability:
   `test/evaluator/score/MaintainabilityTest.java`), and the edges: a value on each limit, no facts at all
   (the metric is "not measured", never 0, SC-07).
5. **docs/SCORE-CARD.md** if the formula's inputs or the invariants changed.
6. **Goldens:** `gradle test`, read each changed card golden against the worked example, then
   `gradle test -PwriteGoldens`.
7. `gradle kernelMap`, `gradle mutate -Ptarget=evaluator.score.*` (E-019), the journal entry.
