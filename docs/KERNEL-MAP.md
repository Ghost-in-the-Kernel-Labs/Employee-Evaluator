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
| rubric | `rubric/evaluator/rubric/Calibration.java:Calibration.Calibration` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Evidence.java:Evidence.Evidence` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Evidence.java:Evidence.isEmpty` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Fact.java:Fact.Fact` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Fact.java:Fact.measured` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Fact.java:Fact.judged` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Fact.java:Fact.metric` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Fact.java:Fact.key` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Fact.java:Fact.scores` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Fact.java:Fact.withState` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Metric.java:Metric.Metric` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Metric.java:Metric.ofKind` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/ProvenanceTier.java:ProvenanceTier.ProvenanceTier` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/ProvenanceTier.java:ProvenanceTier.of` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Score.java:Score.Score` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/ScoreCard.java:ScoreCard.ScoreCard` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/ScoreCard.java:ScoreCard.everyMetricAccountedFor` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/ScoreCard.java:ScoreCard.tierAgrees` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Subject.java:Subject.Subject` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Subject.java:Subject.repository` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Subject.java:Subject.file` |  | 2026-10-19 |  |
| rubric | `rubric/evaluator/rubric/Subject.java:Subject.toString` |  | 2026-10-19 |  |
