# The command line: `evaluator score`, every module's facts in order

**Start here:**
- `cli/evaluator/cli/Main.java` (`Main.run()`, `Main.parse()`)
- `cli/evaluator/cli/Pipeline.java` (the modules in order, E-054)

**Contract:** E-010, E-054; MOD-04: nothing imports `cli/`, and it decides nothing.

**Tests:** `test/evaluator/cli/MainTest.java`, `CardGoldensTest` and `test/goldens/cards/`; `--facts`: `test/evaluator/score/FactsTextTest.java`.

## Steps

- `cli/` only names the work: a rule belongs in the module that does the work, not here.
- Every typed value is checked before use; a wrong one is a usage error on stderr with a non-zero exit, never a stack trace.
- To run it from a build (there is no application task): `gradle compileJava --offline`, then
  `java -cp "build/classes/java/main:third_party/tree-sitter/*" evaluator.cli.Main score <folder> --no-run`
  (without the tree-sitter jars it dies on a missing class). The card carries each score's fact ids, not the
  fact values: `--facts` prints them (below).
- **`--facts`** (E-076) prints, instead of the card, the values behind it: one `name value` line each, one space
  between, sorted by name, a whole number without a decimal point and a fraction in full. The repository-wide facts
  come by their kinds; what is computed from facts comes from `score/evaluator/score/FactsText.java` (the cognitive
  total, the parts and the weighted score, over `Formulas` and `Maintainability`), never from arithmetic in `cli/`.
  It goes with neither `--markdown` nor `--date`; grep a metric with `grep '^SIZE'`. Example, a folder of one
  4-line file with one `if` and no test:

  ```text
  COMPLEXITY.cognitive_total 1
  COMPLEXITY.dependency_depth 0
  COMPLEXITY.functions 1
  MAINTAINABILITY.files 1
  MAINTAINABILITY.part.duplication 1
  MAINTAINABILITY.part.tests 0
  MAINTAINABILITY.weighted 0.75
  SIZE.source_lines 4
  ```

  (the real output has more lines, one for every repository-wide fact and every maintainability part). A baseline is
  recorded, or a golden checked by hand, from this and a fixture's folder: no scratch program.
- **`evaluator brief <path>`** (TODO.md 2.2b, `cli/evaluator/cli/Brief.java`) prints one JSON document for a judge, instead of a
  card: `inventory` (`Inventory.toJson`), `facts` (`--facts`' values as an object, numbers as numbers), `commitsTotal`, `commits`
  (the newest 200: short hash, UTC date, subject cut at 200 characters, files changed), `readme` (root README files) and
  `agentInstructions` (E-044's list). No file's contents, no author name or address (E-014); tests are not run; the same bytes
  each time. It takes no option. Test: `BriefTest`; golden `test/goldens/briefs/` (`gradle test -PwriteGoldens`, review the diff).
- **`--judged <judgment.json>`** (TODO.md 2.2j, E-085; `ingest/evaluator/ingest/Judgment.java`, `ingest/evaluator/ingest/OwnCode.java`, `Pipeline.code`) measures through a
  judge's answer. No model is called (E-003): the file is read as data. The file is **one JSON object of exactly three members**:
  `promptHash` (64 lower-case hex digits, the prompt's hash from `python3 -B -m agent_runner.prompt hash`), `models` (one to eight distinct
  model ids, letters, digits and `. _ : -`) and `judgment` (the answer of `agent-runner/schema/judgment.schema.json`, the 2.2d shape: `kind`,
  `folders` [{`path`, `class`}], `scores`, `ambition`, `quality`, `verdict`, every member and no other). The kernel checks the wrapper, the
  kind, the folders (relative paths with `/`, `.` the root, one trailing slash tolerated, no path twice, one of the eight classes), the
  quality (1 to 5) and the verdict's length, and that `scores` and `ambition` are objects; the full schema check is the harness's
  (`agent_runner.judgment.parse`), which wrote the answer. **Refused with a message and the usage, exit 1:** a bare 2.2d answer (it has
  no prompt or models for the card to name), an ensemble `Result` (`status`, `fields`, `folders` records: take the settled values out of it first,
  folders from `Result.folders[path].value` and `models` from `Result.models`), unreadable or non-JSON text, a wrong hash or model id, a
  member missing or added. Nothing writes this wrapper (2.2m's driver records agreement, not a wrapper): it is written by hand
  (`test/fixtures/judged/judgment.json` is the synthetic example). **What it changes:** a file belongs to its most specific (longest) classified
  folder. Own code and files in no classified folder are counted by Size, Complexity (the cognitive total and the dependency depth) and
  Maintainability's code parts (duplication, cycles, coupling, functions, unused imports), a file in no folder under the kernel's own
  rule (E-085); own tests are not counted as code but are the test files of the test-to-code ratio, whatever their names, while a file in
  an own-code folder is code even when it is named like a test and a file in no folder is a test by its name (E-034); the other classes
  are set aside (`SIZE.set_aside_files` counts them, the reason is `judged: <class> (folder <path>)`). The judge can only narrow: what the kernel
  set aside stays aside. The navigability checklist keeps the whole inventory. The kind is acted on by the card's rules below (2.2k). The card's
  `produced` gains `"judged": {"promptHash", "models"}` and the Markdown a `Judged` line; without `--judged` nothing changes, byte for byte.
  Goes with `--facts` (the facts through the judgment) and `--markdown`. Tests: `OwnCodeTest`, `JudgmentTest`, `MainTest` (the fixture's numbers
  worked by hand), `CardGoldensTest.theJudgedCardIsItsGolden`.
- **The card's rules for the judged kind** (TODO.md 2.2k, E-086; `ingest/evaluator/ingest/JudgedRules.java`, `ingest/evaluator/ingest/AgreementRecord.java`, `score/evaluator/score/Declined.java`) apply when `--judged` is given and its prompt has a record. The record is <folder>/<prompt hash>.json, written by agent_runner/agreement.py (2.2i); the folder is **`--records <folder>`** (it goes with `--judged`) or by default agent-runner/records relative to where the command runs, which holds no record until 2.2m. **With no record** for the hash nothing of the judge's kind, Ambition or Quality is used, stderr says `judged: no agreement record for prompt ...` and the card is the 2.2j card byte for byte; a record that is not whole or not that prompt's is refused (exit 1, its path and the reason). The Java reads only the counts (`matched`, `total`, the baseline's `matched` for each of `ambition`, `quality_exact`, `quality_within_one`, `kind`) and works liveness out exactly as `agreement.is_live` does (E-084): matched/total at least four fifths, and matched at least baseline + ceil(half of (total - baseline)); the stored `live` flag is never read (the two were checked against each other on 12,840 combinations of counts, margins and floors). Per field, a field not live gives a note and no effect:
  - **kind** live: `generated` is **declined** (E-056): **exit status 2** (0 a card or facts, 1 refused), no card; stdout holds the declined document instead, JSON by default (`outcome`, `kind`, `message`, `judged` with the prompt, the models and the note) or with `--markdown` a heading and the same message; `--facts` prints no facts for a declined submission. The message is E-056's: the platform assesses AI-augmented development, a traditional code review would serve the applicant better, nothing is said of their ability. `plan only` withholds every `SIZE`, `COMPLEXITY` and `MAINTAINABILITY` fact (those metrics read not measured) and Ambition reads unverified (no `AMBITION.tier`, a note), E-057. `built`, `mixed`, `own-work` are scored as measured. `clone` is scored as measured with a note (E-082: a clone call alone is no decline; an open question for Ghost, `docs/FOR-GHOST.md`). A kind not live decides nothing, not even a decline.
  - **ambition** live: the fact `AMBITION.tier` (toy, utility, product or research). **quality_within_one** live: the fact `QUALITY.agent_review` (the whole 1 to 5; `quality_exact` gates nothing). Each is `Fact.judged`: confidence the judge's own `confidence` and at most matched/total of its field (SC-05), evidence naming the prompt hash and the models with the sources the judge named. An answer member that is not whole gives no fact and a note. No formula reads these facts yet (2.3, 2.4): the card's Ambition and Quality stay not measured, and `produced.judged.notes` (JSON) or the `Judged note` lines (Markdown) show what the judge said and how far to trust it; `--facts` prints `AMBITION.tier` and `QUALITY.agent_review`.
  - Tests: `AgreementRecordTest`, `JudgedRulesTest`, `DeclinedTest`, `CardRenderingTest`, `MainTest` (exit 2, `--records`, a broken record), goldens `JudgedRulesGoldensTest` over test/fixtures/judged/cases and the synthetic records beside it (never a record from a real run, E-014).
- The cards' goldens are written from the fixtures by `gradle test -PwriteGoldens`; review the diff.

## Always

The fast loop before and after; `gradle kernelMap` before the commit; a changed unit test run by `gradle mutate -Ptarget=evaluator.<module>.*` (E-019); the journal entry. Read further only where the code points.
