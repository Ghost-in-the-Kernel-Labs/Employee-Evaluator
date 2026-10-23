# The seed corpus

**Phase-1 readings.** These cards are the kernel alone; it counts every source file as the author's, so
Katana's carries its reference and fixture code (below). They are made again once the judge classifies
the folders and the kernel counts only the author's own (E-057); trial 3's Fable scores
(`docs/judges/trial-3-fable.md`) are the reference meanwhile. TODO.md 1.9 closed on Ghost's read, 2026-10-22.

The first cards (TODO.md 1.9, DESIGN.md "Peer corpus"): Ghost's own repositories, scored by the phase-1
kernel. Each is a card as JSON (`<name>.json`, every fact behind each score by key) and as Markdown
(`<name>.md`). Made with `evaluator score <path> --no-run --date 2026-10-22` (and `--markdown` for the `.md`), under rubric `cb62abb8be3c`
(phase-1 rubric 4, scored against the baseline, E-070 to E-075):

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

**Regenerating.** Check out the repository at the commit above in a scratch clone (never the original; the folder is read,
nothing of it is run), build this repository (`gradle compileJava --offline`), and run the java command of
`docs/how/command-line.md` once for the JSON and once with `--markdown`, with `--no-run --date 2026-10-22`; the commit and the
date stay, so the rubric is the only change. The cards were first made under rubric b8043f1115a0 (phase-1 rubric 1: Size,
Complexity and Maintainability on a log scale or a ceiling of 100); they were made again under the current one when
the baseline came (E-070 to E-075). When the rubric moves again the cards do not follow by themselves: `CorpusTest` holds
only that each card says the rubric this file names (E-078). It does not hold them to the current rubric: the test could
know the current hash, but every rubric change would then fail the pre-push until two other repositories (and this one at
an old commit) were scored again in scratch clones, and a clean clone of this repository alone cannot do that. A card made under an older
rubric is still a true record of its day, and its date and hash say which. Anyone who needs the current numbers regenerates, as above.

**Scores can pass 100.** Size, Complexity and Maintainability are counted against Suite Builder at 32762a6 (E-070, E-071) and
have no ceiling. Suite Builder's own card here is at b8f2e8c1, an older commit than the baseline, so it need not read 100 and does
not: Size 96, Complexity 96, Maintainability 100 (99.7 rounded). A repository larger than the baseline reads over 100. Nothing here is
cut to 100.

**What moved when the cards were made again.** Only the rubric hash and the three baselined metrics, the Maintainability at
Complexity ratio that follows from them, and two things that were not the rubric. First, since these cards were made the
JavaScript adapter (E-058) reads `.js` and `.mjs` files, so Suite Builder gained 193 Complexity facts and 252 Maintainability
facts (its build scripts) and this repository 6 Maintainability facts (the JavaScript in its fixtures); Katana's facts are unchanged. Second, this
repository's Provenance reads 72, not 73: the kernel as it was at a7fea50f reads 72 on the same clone, so the old card
did not come from exactly that history (the same kernel reproduces Suite Builder's and Katana's old cards byte for byte).

## Beside the judges

The tools' half of each score (now against the baseline, so over 100 is possible; the judges' half is on the 0 to 100 scale of the trials, so the two columns are no longer on one scale for Size, Complexity and Maintainability), beside the mean of three Opus judges reading the same commit (judge trial 2,
`docs/judges/trial-2.md`; this repository's judges read 17062f1):

| Metric | Employee Evaluator: tool / judges | Suite Builder | Katana |
| --- | --- | --- | --- |
| Complexity | 22 / 64 | 96 / 83 | 55 / 89 |
| Size | 10 / 44 | 96 / 79 | 67 / 72 |
| AI navigability | 99 / 88 | 97 / 87 | 100 / 86 |
| Maintainability | 94 / 79 | 100 / 80 | 63 / 75 |
| Provenance | 72 / 61 | 74 / 82 | 78 / 75 |

## For Ghost's sanity read

- **Katana's Size (67) and Maintainability (63)** count about 19k lines of Java another tool generated, kept
  as reference under `lower/prototypes/` and `oracle/prototypes/`, and 8.7k lines of COBOL test programs, as
  Katana's own source. The generated code is very repetitive: duplication reads 24%, the main drag on
  Maintainability. Which files are the applicant's own code is a judgment (E-055), not a new rule.
- **Katana's `sir/build/`** (15 files of real source) is set aside, because `build/` folders are (E-025, kept
  by Ghost's word); here it is a Java package's name.
- **Complexity** in the tools' half is how much decision logic there is, not how hard the problem is: the
  judges' half (`COMPLEXITY.agent_assessment`) is what will lift Katana's.
- **Navigability** reads high on all three: each keeps the agent docs the checklist asks for. The judges
  scored 86–88, so the checklist's ceiling is the task's (TODO.md 2.5) to lower.
