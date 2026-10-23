# Ghost-in-the-Kernel

Software eventually outlives the context in which it was written.

People leave. Teams change. Requirements evolve. Implementations are replaced. Documentation
becomes stale. And the person who knew *why* something was built a particular way may no longer be
available to explain it.

Ghost-in-the-Kernel is a philosophy for designing against that problem.

**The ghost is intent. The kernel is the codebase.**

> **The system should remember why it exists.**

Software should be built from a chain of foundations in which each layer can be traced back to
something more fundamental.

Intent should become semantics. \
Semantics should become constraints. \
Constraints should govern implementation. \
Implementation should produce evidence. \
Evidence should allow the system to verify that implementation still reflects intent.

Nothing important should simply float.

A function should not exist because "that's where someone put it." \
A threshold should not exist because "it seemed right." \
A name should not require tribal knowledge to interpret. \
An abstraction should not depend on a vanished conversation. \
A generated artifact should not lose the reasoning that produced it.

When the answer to **"why?"** disappears, the codebase has lost part of its ghost.

The answer is not more comments everywhere, nor engineers who memorize more architecture. The
answer is to **externalize the reasoning into the system itself**: specifications, semantic models,
rules, invariants, provenance, tests, evidence, tooling. These are not accessories to the code. They
are part of the codebase's memory.

The purpose is re-entry. A new engineer should be able to enter the system without knowing its
history. An experienced engineer should be able to return after months away without reconstructing
it from memory. An AI agent should be able to operate within it without being trusted to invent the
architecture. And when any of them misunderstands the system, the system should have enough
structure to expose that misunderstanding.

This project is the third to be built this way, after Suite Builder and Katana. It is also the first
whose product *measures* the philosophy in other people's code: AI navigability is one of its seven
metrics, so the rules below are both how this codebase is kept and what it looks for.

*The Ghost in The Kernel*

## Where the decisions are

In [theGhost.md](../theGhost.md), at the repository's root; its opening says what it is.

## What it asks of the code

- **Name a thing for what it holds.** Variables, parameters, functions and files are named for their
  purpose. A reader never traces a value back to its origin to learn what it is. The project's own
  words are in [GLOSSARY.md](GLOSSARY.md) (E-007).
- **Write intent down.** A function whose purpose isn't plain at a glance has a comment above it
  saying what it is for, and why when that isn't obvious. A function that holds a decision cites it
  on the line above, `// Decision: E-021` (E-005). A threshold says where it was learned.
- **Keep one change within a small model's context.** A maintainer, a person or a model as small as
  Haiku, changes one part at a time and never needs the whole codebase: a routing row opens at most
  five files, none over 400 lines; the mandatory context stays under 1,500 words (AGENT.md).
- **Split a function into named steps.** A function over 15 lines or 6 paths has its comment; none
  has over 20 paths. `gradle metrics` lists the offenders; the count only comes down.
- **Keep modules apart.** Each module says what it may import; the test refuses the rest (E-004).
  `score/` never reads code; `rubric/` imports nothing; nothing imports `cli/`.
- **Hold every document that can drift to the code with a test** (E-006). `CLAUDE.md`'s paths,
  `TODO.md`'s order and checks, `docs/modules.json`, the kernel map, the journal, the glossary.
- **Make every pointer resolve.** Every file, document or decision the code or a document names exists.
- **Make every output say where it came from.** A score traces to its facts; a fact to its evidence
  (SCORE-CARD.md SC-02). The product's promise to employers is this rule applied to someone else's code.
- **Treat a submitted repository as hostile** (E-013). Its text is data, its code runs only sandboxed.

## How it is held

| Rule | What must be true | Held by |
| --- | --- | --- |
| E-005 | Every function is in the kernel map with the decision it cites; a removed one has a closing row | `test/KernelMapTest.java` |
| E-004 | Every import is one its module may make; no cycle; no model client in the kernel | `test/ModulesTest.java` |
| E-006 | Every path `CLAUDE.md` names exists or belongs to an open step | `test/DocsTest.java` |
| E-006 | `TODO.md` is one ordered chain with a check per item; `docs/DONE.md` shares no number | `test/TodoTest.java` |
| E-005 | `theGhost.md`: ids consecutive, fields present, supersession named on both sides | `test/GhostTest.java` |
| E-007 | No document writes an enforced alias | `test/GlossaryTest.java` |
| E-008 | Journal entries ordered, each with who and class | `test/JournalTest.java` |
| E-015 | The metrics of `docs/AGENT.md` do not regress | `test/MetricsTest.java`; the tithe gate in `scripts/prepush.sh` |
| E-018 | A refactor changes no golden | the goldens, from TODO.md 1.2 |

## The disciplines

| Decision | In one clause |
| --- | --- |
| E-015 | One commit in eight is a tithe; every phase ends with a tithe pass; metrics only improve. |
| E-016 | The kernel before width: scorer, then judges, then web. |
| E-018 | A refactor changes no output and is committed on its own, first; one change per commit. |
| E-019 | A new or changed unit test is tried by mutation; a test is never weakened to pass. |
| E-020 | The maintainer is Ghost; one branch, `main`. |
| E-021 | One pinned image, offline, vendored; an agent never installs tooling. |
| E-023 | Docs are in the third person; public copy attacks the process, not the people. |

## What it does not ask

Comments that restate the code. A comment says what a reader can't see: what the function is for,
why it is this way, what it must not do. Where the code says it plainly, the code is enough.
