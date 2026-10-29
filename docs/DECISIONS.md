# Decisions: one line each

<!-- Written by `gradle decisionsDoc` from theGhost.md; edit theGhost.md, never this file. -->

Find the decision here, then read its entry in `theGhost.md` by number
(`grep -n "### E-042" theGhost.md`). The record, its rules and every reason are there.

| Decision | Title | Status | Subject |
| --- | --- | --- | --- |
| E-001 | Java 21 for the kernel, TypeScript for the web, Python for the agent runner | in force | languages |
| E-002 | The website and the GitHub app are one web app | in force | architecture |
| E-003 | No model runs in the kernel; judgment arrives as facts | in force | architecture |
| E-004 | Modules with declared imports, held by a test | in force | architecture |
| E-005 | This file is the decision record; every decision-holding function cites one | in force | intent |
| E-006 | Every document that can drift is held to the code by a test | in force | intent |
| E-007 | One word for one thing: the glossary and its enforced aliases | in force | intent |
| E-008 | The journal: every event, in fixed fields, by whoever was there | in force | intent |
| E-009 | Doc tests are plain Java with no dependency, run under JUnit | in force | tests |
| E-010 | The command line decides nothing | in force | architecture |
| E-011 | Seven metrics, ranked separately; no composite score | in force (seven metrics, no composite); superseded by E-047 (how the two ratios are made) | scoring |
| E-012 | Provenance is a score, not a gate | in force | scoring |
| E-013 | A submitted repository is hostile input | in force | security |
| E-014 | Nothing from an applicant enters this repository | in force | security |
| E-015 | The navigation tithe | in force | process |
| E-016 | The kernel first, then width | in force | process |
| E-017 | The score card is the intent map: facts with evidence, confidence and state | in force | scoring |
| E-018 | Goldens, and one change per commit | in force | tests |
| E-019 | Every new or changed unit test is tried by mutation | in force (E-059 vendored the tool, TODO.md 1.11) | tests |
| E-020 | The maintainer is Ghost; one branch, main | in force | process |
| E-021 | The environment: one pinned image, offline, vendored | in force | environment |
| E-022 | A research findings list | in force | research |
| E-023 | The public pitch attacks the process, never the people | in force | pitch |
| E-024 | GitHub's minutes are spent by hand, never by a push | superseded by E-040 | environment |
| E-025 | Vendored and generated code is set aside by what the repository shows, and says why | in force | ingestion |
| E-026 | What Size counts: non-blank lines, files and folders of the applicant's own source | in force | ingestion |
| E-027 | ANTLR, not tree-sitter, parses TypeScript and Python | superseded by E-028 | environment |
| E-028 | tree-sitter parses TypeScript and Python, after all | in force | environment |
| E-029 | A session works through the steps in order; it does not stop at one | in force | process |
| E-030 | Complexity is counted two ways: McCabe's paths, and SonarSource's cognitive complexity | in force | analysis |
| E-031 | TypeScript and Python are counted by Java's rules, where a function is drawn as each language draws it | in force | analysis |
| E-032 | The dependency graph is the repository's own files, resolved by path; unused imports count against a file | in force | analysis |
| E-033 | Ghost's Supply-Chain Security Baseline, held by a test | in force (part); superseded by E-061 (part: where the directive lives) | security |
| E-034 | A test file is known by its path, as test runners find them | in force | analysis |
| E-035 | Coupling is fan-in, fan-out and import cycles, read from the dependency graph | in force | analysis |
| E-036 | Duplication is shared 60-token runs inside functions, names and literals normalized | in force | analysis |
| E-037 | A submission's tests run in a Docker container per run, in two phases, with pinned runners | in force | security |
| E-038 | A Maven build is installed with exact versions and strict checksums, Maven having no lockfile | in force | security |
| E-039 | Linux only, and every run in the cloud | in force | environment |
| E-040 | The agent starts GitHub runs itself, within the month's minutes | in force | environment |
| E-041 | The sandbox image builds with the network off, from pieces fetched and checked first | in force | environment |
| E-042 | The history's shape is six parts of how it was written, weighted, 0–100 | in force | provenance |
| E-043 | Authorship is read from GitHub's API with a token of its own, read only, never logged | in force | provenance |
| E-044 | The navigability checklist: agent docs, their paths, module cycles, naming, weighted to 0–100 | in force | navigability |
| E-045 | Facts into a card: a formula per metric, none until decided; the ratios as DESIGN.md words them | in force (the card from facts, a formula per metric); superseded by E-047 (the ratios) | scoring |
| E-046 | Provenance is 0.6 × the history's shape + 0.4 × what GitHub backs; the shape alone is capped at 79 | in force | scoring |
| E-047 | The ratios: Quality per thousand lines within a size band; Maintainability at Complexity | in force | scoring |
| E-048 | Size is source lines on a log scale: 100 lines read 0, 100,000 read 100 | in force (Size's facts: source lines, vendored and generated code set aside); superseded by E-070 (Size's formula) | scoring |
| E-049 | A Java class used from its own package is a dependency, though no import line names it | in force | analysis |
| E-050 | Complexity is the total cognitive complexity on a log scale and the dependency depth | in force (Complexity's facts: total cognitive complexity and dependency depth); superseded by E-074 (Complexity's formula) | scoring |
| E-051 | Maintainability is six parts weighed: duplication, tests, cycles, complex functions, coupling, unused imports | in force (its six parts and weights); superseded by E-075 (the scale) | scoring |
| E-052 | Quality is the pass rate and coverage of the submission's own tests; no test at all reads 0 | in force | scoring |
| E-053 | The tooling bucket is the agent tool with more than half the evidence; else mixed | in force | scoring |
| E-054 | `evaluator score <path>`: every module's facts, one card, as JSON or Markdown | in force | architecture |
| E-055 | The judge is Haiku three times, averaged, given the kernel's facts; Opus when the three disagree | in force | judges |
| E-056 | Only AI-assisted work is ranked: evidence admits, its absence asks, hand-written or generated wholesale is declined | in force | admission |
| E-057 | The judge classifies and judges; the kernel counts what the judge calls the author's own, and applies the card's rules | superseded by E-082 (its list of kinds only, the rest stands) | judges |
| E-058 | JavaScript is read as TypeScript is, with the vendored TSX grammar, and `require` counts as an import | in force | analysis |
| E-059 | PIT is the mutation tool, vendored with what its poms declare | in force | tests |
| E-060 | A Sonnet agent tries a hard fix once, then asks Opus | in force | agents |
| E-061 | The supply-chain directive has one copy, theGhost.md's first section, read before any task | superseded by E-073 | security |
| E-062 | `gradle kernelMap` closes a function that is gone | in force | intent |
| E-063 | CLAUDE.md's routing rows are pointers; an action's steps live in its instructions file | superseded by E-065 | agents |
| E-064 | Every escalation replays the route, is logged, and three of a kind go to Ghost | in force | agents |
| E-065 | Every workpath in CLAUDE.md is a pointer to its instructions | in force | agents |
| E-066 | When agents' work conflicts, they stop and tell Ghost; none undoes another's | in force (part); superseded by E-067 (part: force-pushing) | agents |
| E-067 | A force-push only when every active agent agrees, the slow ones waited for | in force | agents |
| E-068 | Fresh Sonnet agents per task; each debriefs; a navigation review about every ten commits | in force | agents |
| E-069 | CLAUDE.md is a memory map: what applies only sometimes is a "When X, read Y" pointer | in force | agents |
| E-070 | Counted aspects score against a baseline repository: Suite Builder at 32762a6 is 100, empty is 0 | in force (Size, Complexity and Maintainability, TODO.md 1.13 built) | scoring |
| E-071 | A baseline-scored metric may pass 100 | in force (Size, Complexity and Maintainability are counted, the rest keep 0–100) | scoring |
| E-072 | The fast loop runs test classes side by side, the tests of a class one after another | in force | tests |
| E-073 | The supply-chain rules are back in CLAUDE.md, their one copy, read on entry | in force | security |
| E-074 | Complexity is scored against the baseline: 0.6 on cognitive complexity, 0.4 on dependency depth, Suite Builder at 32762a6 is 100 | in force | scoring |
| E-075 | Maintainability is scored against the baseline: E-051's weighted score over Suite Builder's at 32762a6 is 100 | in force | scoring |
| E-076 | `evaluator score --facts` prints the values behind a card, one `name value` line each | in force | command line |
| E-077 | `gradle test -PshowGoldens` shows what a golden would change and writes nothing; one helper holds every golden | in force | tests |
| E-078 | The seed corpus is held to the rubric its README names, not to the current rubric | in force | corpus |
| E-079 | A calibration label is one JSON file, an opaque id and five closed fields, held by a loader in rubric/ | superseded by E-080 (its field list only, the rest stands) | calibration |
| E-080 | A calibration label is three fields, kind, ambition and quality, and an opaque id; no domain, no rationale | in force | calibration |
| E-081 | `python3`, standard library only, is in the pinned image for agent-runner/, and no network door exists yet | in force | agent-runner |
| E-082 | The judge's kind is six values, E-057's four plus own-work and clone | in force | judges |
| E-083 | On a category tie among the three Haiku votes, Opus decides and its answer stands | in force | judges |
| E-084 | A judged field is live only if its raw agreement is at least 80 % and it beats the constant answer by a margin | in force | judges |
| E-085 | `score --judged` counts the judge's own code; its own tests make the test ratio, by class | in force | judges |
| E-086 | The card's rules for the judged kind: a judged value counts only where its field is live; generated is declined with exit status 2; clone is scored with a note | proposed | judges |
| E-087 | The live judge is one standard-library door under a dollar budget; the caps, the prices and the model ids are data | in force (the door, the budget, the data); superseded by E-089 (the retry sleeps) | judges |
| E-088 | Every reply starts with "Context Canary" | in force | process |
| E-089 | The live judge retries after 3, 9 and 27 seconds; a key received unencrypted is taken and its sender warned | in force | judges |
| E-090 | A GitHub token is optional | in force | github |
