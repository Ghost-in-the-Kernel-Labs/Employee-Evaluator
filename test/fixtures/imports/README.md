# imports

A fixture for `analyze/`'s dependency graph and unused imports (TODO.md 1.3): a little Java, Python and
TypeScript importing each other, outside the repository, and some names never used; Java's `Checkout`
uses `Cart` from its own package with no import line (E-049). The edges, the two longest chains
(`shop/Checkout.java` → `shop/Cart.java` → `shop/money/Price.java`, and `ts/src/index.ts` →
`ts/src/view.tsx` → `ts/src/types.ts`) and each unused name are
worked by hand in `test/evaluator/analyze/DependenciesTest.java`. `py/app/__init__.py` re-exports, so
its import is not unused.
