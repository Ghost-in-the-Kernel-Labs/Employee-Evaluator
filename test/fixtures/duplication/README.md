# duplication

A fixture for `analyze/`'s duplication (TODO.md 1.4): `src/invoices.py` is `src/orders.py`'s
`price_order` with every name and number changed, which normalized tokens still catch;
`src/shipping.py` shares its second to fifth lines (19 tokens, under the 60-token window), which they
must not; `tests/test_orders.py` copies it too, and tests are not counted. Expected values in
`test/evaluator/analyze/DuplicationTest.java`.
