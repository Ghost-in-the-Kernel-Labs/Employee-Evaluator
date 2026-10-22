# The seed corpus

The first cards (TODO.md 1.9, DESIGN.md "Peer corpus"): Ghost's own repositories, scored by the phase-1
kernel. Each is a card as JSON (`<name>.json`, every fact behind each score by key) and as Markdown
(`<name>.md`). Made with `evaluator score <path> --no-run --date 2026-10-22`, rubric `b8043f1115a0`:

| Card | Repository | Commit |
| --- | --- | --- |
| `employee-evaluator` | this repository | a7fea50f |
| `suite-builder` | Ghost-in-the-Kernel-Labs/suite-builder, `main` | b8f2e8c1 |
| `katana` | ghost-in-the-kernel/katana | af9b88d5 |

**Why every Quality reads "not measured":** no test run was possible here. Suite Builder's `npm ci` needs the
install phase's registries, which this cloud container cannot reach (E-041: proven on paper only); Katana and
this repository build with Gradle, which no test plan runs yet. And this repository's Python fixtures would
have been run as its tests (J-039), which is the judges' call (E-055), not a rule's. Ambition waits on its
judge (phase 2). Provenance is the history's shape alone, Partial at most, until a GitHub token reads
authorship (E-046).

## Beside the judges

The tools' half of each score, beside the mean of three Opus judges reading the same commit (judge trial 2,
`docs/judges/trial-2.md`; this repository's judges read 17062f1):

| Metric | Employee Evaluator: tool / judges | Suite Builder | Katana |
| --- | --- | --- | --- |
| Complexity | 41 / 64 | 82 / 83 | 53 / 89 |
| Size | 65 / 44 | 97 / 79 | 92 / 72 |
| AI navigability | 99 / 88 | 97 / 87 | 100 / 86 |
| Maintainability | 84 / 79 | 89 / 80 | 56 / 75 |
| Provenance | 73 / 61 | 74 / 82 | 78 / 75 |

## For Ghost's sanity read

- **Katana's Size (92) and Maintainability (56)** count about 19k lines of Java another tool generated, kept
  as reference under `lower/prototypes/` and `oracle/prototypes/`, and 8.7k lines of COBOL test programs, as
  Katana's own source. The generated code is very repetitive: duplication reads 24%, the main drag on
  Maintainability. Which files are the applicant's own code is a judgment (E-055), not a new rule.
- **Katana's `sir/build/`** (15 files of real source) is set aside, because `build/` folders are (E-025, kept
  by Ghost's word); here it is a Java package's name.
- **Complexity** in the tools' half is how much decision logic there is, not how hard the problem is: the
  judges' half (`COMPLEXITY.agent_assessment`) is what will lift Katana's.
- **Navigability** reads high on all three: each keeps the agent docs the checklist asks for. The judges
  scored 86–88, so the checklist's ceiling is the task's (TODO.md 2.5) to lower.
