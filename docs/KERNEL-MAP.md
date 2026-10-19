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
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest.Ingest` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest.read` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest.files` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest..preVisitDirectory` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest..visitFile` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest.gitattributes` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest.bytes` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Ingest.java:Ingest.binary` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Inventory.java:Inventory.Inventory` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Inventory.java:Inventory.toJson` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Inventory.java:Inventory.quote` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Languages.java:Languages.Languages` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Languages.java:Languages.of` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.SetAside` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.reason` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.byFolder` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.byAttributes` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.byName` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.byHeader` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.attributes` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/SetAside.java:SetAside.marks` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Size.java:Size.Size` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Size.java:Size.facts` |  | 2026-10-19 |  |
| ingest | `ingest/evaluator/ingest/Size.java:Size.fact` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Complexity.java:Complexity.Report.Report` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Complexity.java:Complexity.Complexity` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Complexity.java:Complexity.measure` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Complexity.java:Complexity.facts` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Complexity.java:Complexity.toJson` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Complexity.java:Complexity.adapterFor` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Complexity.java:Complexity.read` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter.reads` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter.tool` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter.parse` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter..getCharContent` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter.Methods.Methods` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter.Methods.visitClass` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter.Methods.visitMethod` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaAdapter.java:JavaAdapter.Methods.measure` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.JavaComplexity` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.cyclomatic` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity..scan` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity..visitClass` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.cognitive` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.Cognitive` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitIf` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitConditionalExpression` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitSwitch` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitSwitchExpression` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitForLoop` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitEnhancedForLoop` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitWhileLoop` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitDoWhileLoop` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitCatch` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitBreak` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitContinue` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitLambdaExpression` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitClass` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitMethodInvocation` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.visitBinary` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.logical` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/JavaComplexity.java:JavaComplexity.Cognitive.flatten` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Parsed.java:Parsed.Parsed` |  | 2026-10-19 |  |
| analyze | `analyze/evaluator/analyze/Parsed.java:Parsed.unread` |  | 2026-10-19 |  |
