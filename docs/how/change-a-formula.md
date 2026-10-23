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
   `Formulas.rubricHash()`, so `theRubricsHash` in `test/evaluator/score/FormulasTest.java` and every card
   golden change with it; that is intended.
4. **Tests:** the worked example as a test in `test/evaluator/score/FormulasTest.java` (Maintainability:
   `test/evaluator/score/MaintainabilityTest.java`), and the edges: a value on each limit, no facts at all
   (the metric is "not measured", never 0, SC-07).
5. **docs/SCORE-CARD.md** if the formula's inputs or the invariants changed.
6. **Goldens:** `gradle test -PshowGoldens > build/show.log 2>&1`, then read each changed golden in the failures it prints
   (`CardGoldensTest` names every changed card and its changed lines; nothing is written, E-077) against the worked example, and only then
   `gradle test -PwriteGoldens` (the two together are refused). The goldens hold scores, not fact values: work each changed score by hand
   from the fixture's facts (`evaluator score <fixture> --no-run --facts`, `docs/how/command-line.md`), and know that a fixture whose
   metric reads 0 on the old and the new scale proves nothing about the formula; the unit test carries it.
7. `gradle kernelMap`, `gradle mutate -Ptarget=evaluator.score.*` (E-019), the journal entry.
