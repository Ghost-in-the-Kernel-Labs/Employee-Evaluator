# Add a fact

A fact is one thing measured about a submission, with its evidence (docs/SCORE-CARD.md, "A fact"). Facts
are made in the module that reads the thing (`ingest/`, `analyze/`, `history/`, `github/`), never in
`score/` (MOD-03), and a score reads them.

## Steps

1. **Name its kind:** the metric's name, a dot, what it says (`PROVENANCE.history.spread_days`).
   `rubric/evaluator/rubric/Metric.java` (`Metric.ofKind`) refuses any other shape, at run time.
2. **Make it** where the thing is read, with `Fact.measured(kind, subject, value, evidence)`
   (`rubric/evaluator/rubric/Fact.java`): a subject (`Subject.repository()`, or a file or function), and
   evidence naming the module's tool string and the paths read. Follow the module's own fact helper, e.g.
   `fact(...)` in `history/evaluator/history/Shape.java`.
3. **List it** in docs/SCORE-CARD.md, "Where the facts come from", in its module's row (a family built at
   run time as `KIND.*`). `test/ScoreCardDocTest.java` fails until the row and the source agree.
4. **Test it** in the module's tests: the value worked by hand on a small fixture.
5. **If a score should read it,** that is a formula change: `docs/how/change-a-formula.md`.
6. **Goldens:** a new fact shows on every card that has it. Run `gradle test`; if a card golden under
   `test/goldens/cards/` changes, check the change is only the new fact, then `gradle test -PwriteGoldens`.
7. `gradle kernelMap`, `gradle mutate -Ptarget=evaluator.<module>.*` (E-019), the journal entry.
