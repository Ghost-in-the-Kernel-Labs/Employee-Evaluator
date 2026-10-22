# javascript-complexity

A fixture for `analyze/`'s JavaScript adapter (TODO.md 1.10, E-058): the typescript-complexity fixture's
examples without their types (`src/examples.js`, the same values), a Pages Functions style handler with ESM
imports, one never used (`src/handler.mjs`), a CommonJS module with `require` (`src/legacy.cjs`: `path` is
never used, `require("./view")` is run for its effect), and a JSX component (`src/view.jsx`). Every
function's values and every import are worked by hand in `test/evaluator/analyze/TreeSitterAdaptersTest.java`.
`src/broken.js` does not parse.
