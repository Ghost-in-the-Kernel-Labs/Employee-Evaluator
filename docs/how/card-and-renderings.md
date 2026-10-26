# Facts into a card: the scores, ratios, tier, bucket; the JSON and Markdown

**Start here:**
- `score/evaluator/score/Cards.java` (`Cards.card()`)
- `score/evaluator/score/Formulas.java`
- `score/evaluator/score/CardJson.java`
- `score/evaluator/score/CardMarkdown.java`

**Contract:** SC-01 to SC-11; MOD-03 (`score/` reads facts only); E-045 onward.

**Tests:** `test/evaluator/score/CardsTest.java` (the card's own rules), `FormulasTest`, `CardRenderingTest`, `MaintainabilityTest`; the cards' goldens, `test/goldens/cards/`.

## Steps

- A formula: `docs/how/change-a-formula.md`.
- The JSON is the same bytes for the same card (SC-09); a change to its form changes every card golden: `gradle test -PwriteGoldens`, review the diff.
- A metric with no facts is "not measured", never 0 (SC-07).
- A judged card (`score --judged`, E-085) names the judgment in `produced` (`"judged": {"promptHash", "models"}`) and on a Markdown `Judged` line, through `CardJson.render(card, judged)` and `CardMarkdown.render(card, judged)` and `score/evaluator/score/Judged.java`; with none the bytes are as ever, so no card golden moves. `rubric/` is untouched.
- The tooling bucket is `score/evaluator/score/Buckets.java` (E-053), from the agent tools `history/` found.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
