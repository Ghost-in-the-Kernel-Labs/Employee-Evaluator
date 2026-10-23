# Add a language adapter

What a language needs so its files are measured: functions, cyclomatic and cognitive complexity,
imports, the dependency graph, duplication. JavaScript is the last one added (E-058, commit 4f07111):
`git show --stat 4f07111` lists every file it touched.

## Before you start

- **A grammar is a dependency.** A tree-sitter grammar not under `third_party/` needs a one-line reason
  in `theGhost.md` and Ghost's approval before it is vendored (the supply-chain rules in `CLAUDE.md`, E-033). First
  try a grammar already vendored (`third_party/MANIFEST.md`): JavaScript reads with TypeScript's TSX grammar.
- Read the decisions it stands on: E-030 (what is counted), E-031 (tree-sitter adapters), E-032 (imports).

## Steps

1. **Name the language in the inventory:** its file endings in `ingest/evaluator/ingest/Languages.java`.
   Without this the inventory never says the language and nothing below runs. If the inventory golden
   changes, that is the point: review it (`gradle test -PwriteGoldens`).
2. **Write the adapter** in analyze/evaluator/analyze/*Lang*Adapter.java, implementing
   `analyze/evaluator/analyze/Adapter.java`. A tree-sitter language extends
   `analyze/evaluator/analyze/TreeSitterAdapter.java` and fills its hooks: `grammar`, `functionName`,
   `className`, `decisions`, `cognitive`, `imports`. Copy the closest existing adapter
   (`analyze/evaluator/analyze/PythonAdapter.java`, `analyze/evaluator/analyze/TypeScriptAdapter.java`, or `analyze/evaluator/analyze/JavaScriptAdapter.java` for a dialect of one).
   Its `tool()` names the grammar and its version. Cite the decision above the class (`// Decision:`).
3. **Its imports** in analyze/evaluator/analyze/*Lang*Imports.java (`Import` values; resolved by path,
   never by running anything): see `analyze/evaluator/analyze/PythonImports.java` or `analyze/evaluator/analyze/TypeScriptImports.java`.
4. **Register it** in `Adapters.ALL` (`analyze/evaluator/analyze/Adapters.java`). The order is
   the order facts are made in; append, so no golden moves.
5. **Comments for duplication:** `analyze/evaluator/analyze/Duplication.java` tokenizes with `Tokens.of(text, hashComments)`; a
   language whose comments start with `#` needs its ending added there.
6. **Test files:** if the language names its tests its own way, add the pattern to
   `analyze/evaluator/analyze/TestFiles.java`.
7. **A fixture, worked by hand:** test/fixtures/*lang*-complexity/ with a README saying each function's
   expected numbers and why, plus one file that does not parse. Work every value by hand before any golden.
8. **Its tests and golden:** cases in `test/evaluator/analyze/TreeSitterAdaptersTest.java` (each value
   from the README), the fixture added to `theFixturesComplexityIsItsGolden`, then
   `gradle test -PwriteGoldens` once to write test/goldens/*lang*-complexity/complexity.json; review it.
9. **Record it:** a decision in `theGhost.md` (its next E-number), `gradle kernelMap`, `gradle mutate
   -Ptarget=evaluator.analyze.*` for the adapter's survivors (E-019), and the journal entry.

## Done when

`gradle test` passes; the fixture's golden holds every hand-worked number; a card for a repository in
the language (`evaluator score <path>`) shows its functions counted; no other golden changed unless the
step above says why.
