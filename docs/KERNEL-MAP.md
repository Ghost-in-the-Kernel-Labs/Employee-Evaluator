# Kernel map: the kernel's own functions, append-only

Generated from the source by `gradle kernelMap` and committed; review its diff, never edit a line by
hand (E-005). One line per function: its module, its name, the decision it cites (`// Decision:`
above it; empty when none), and the date it was first seen. A line is never changed; a function that
goes away gets a later line with `superseded-by:` or `removed:`. `test/KernelMapTest.java` fails on
a function removed or renamed without one. Suite Builder's finding: the append-only map was the
guardrail that tripped when an agent nearly overwrote another area's function.

| Module | Function | Stands on | First seen | Superseded |
| --- | --- | --- | --- | --- |
| cli | `cli/evaluator/cli/Main.java:Main.main` |  | 2026-10-19 |  |
| cli | `cli/evaluator/cli/Main.java:Main.run` |  | 2026-10-19 |  |
