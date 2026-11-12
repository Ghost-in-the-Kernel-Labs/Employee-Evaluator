# Hand out a task, by tier (E-092)

The cheapest tier whose work the gate can check does the task; the strongest context stays small. The gate (`gradle test`,
`scripts/prepush.sh`) judges the work, so a worker only has to be good enough to pass it. Every other file names a **tier**,
never a model: the one table below is where tiers become models, and `test/TierNamesTest.java` fails if a family name appears
elsewhere in `CLAUDE.md` or `docs/how/` (it is told the exemptions: judge-harness.md and judge-trials.md, whose model ids are
the product's, and update-a-dependency.md, whose reviewer pins are the posture's settings, not worker tiers; the agents folder under .claude
are pinned by hash and keep their own). The journal, the decisions and the history keep the names they were written with.

## The tiers

| Tier | Model (today) | Gets |
| --- | --- | --- |
| small | Haiku | Mechanical work with a mechanical pass line inside one task row's working set: a test for a named mutant, a doc regenerated, a rename, a lint fix, a reputation search. |
| mid | Sonnet | A feature across a few files and one or two modules; the small tier's escalations; the default for a task not placed. |
| strong | Opus | Design, Ghost's questions, security, a failure whose cause is unknown, the mid tier's escalations, coordinating. Read-only questions of the navigation review. |

Give a task the cheapest tier it fits. A choice only Ghost can make is never scored down: it goes to Ghost. To change a model,
change this table and nothing else.

## Stop limits a worker can count

A model cannot see how full its context is, so each worker stops at the first of these it reaches, with its report:

- **One attempt** at a fix the gate rejects for a reason it cannot explain in one reading; a second attempt is the next tier's.
- **A file outside the brief's working set** (other than a test, a doc or a golden it must regenerate).
- **Tool calls:** small 40, mid 120.
- **A choice the brief does not settle:** it stops and asks; it never picks.

## The brief

One page at most (more means the next tier up): the objective and its pass line; the working set (the files it may change);
the stop limits with the tier's tool-call number; the never-rules (no weakened or skipped test, no pinned file, no dependency,
no push, no private data); how to commit (`docs/how/commit-and-push.md`); the report's shape below; and the first word of every
reply (`Context Canary`, CLAUDE.md's fourth rule).

## The report

One page, and all the next tier reads: what it was asked; the commit (hash, subject) and files changed; what it tried; what failed,
with the exact output; the files it read; its tool-call count; the choices left for Ghost. The next tier starts from the report and
the diff, not the transcript. A report that will not fit a page means the task was placed too low.

## The coordinator stays small

- **Sessions end at a natural point,** with a written handoff (`docs/HANDOFF.md`: done, running, waiting on whom, next); the next
  starts from it, not from the conversation.
- **Silence while waiting.** A long run is waited on in the background and wakes the coordinator when it ends; no turn to say it
  still runs.
- **No turn without a decision.** Read a log by grep for the failure, a file in the part needed.
- **The mechanical loop goes to the mid tier:** run the gate, read the failure, fix, run again; the coordinator returns for the
  decision the loop cannot make.
- **Hooks are not edited by an agent;** one that complains of something done on purpose is taught by the person who owns it.
- **Delegate reading:** a question that needs many files goes to a worker that answers in a paragraph.

## Measuring it

Built: each task row's working set in tokens (`gradle metrics`, E-093), and a placement for each open item (`gradle scoreItems`,
E-094): its points from the files and layers it names, a contract and a mechanical pass line, its tier from them, and the strong
tier for anything waiting on Ghost. An item it leaves unplaced (no file or layer named) is scored by a mid-tier agent, who writes
the facts used into the item so the script places it next time. Not built: the worker log (posture step 4). Each worker's report
gives its tokens and tool calls; kept per item beside the placement, they show whether tasks are placed well: an item that
escalated was placed too low, one far under its limits might go a tier down.
