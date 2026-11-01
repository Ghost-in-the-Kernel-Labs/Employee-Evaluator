# Escalate to the strong tier, and replay the route (E-060, E-064, E-092)

For a mid-tier or small-tier worker whose one attempt at a fix failed where the failure did not plainly say why (the stop limits
and the tiers are in `docs/how/hand-out-a-task.md`). A Ghost directive: every escalation also checks whether the route the agent
started from gave it what it needed. The file keeps its name so what links it does not break.

## The worker

1. **Stop after one attempt** and hand back to the session that started you, which spawns a **strong-tier agent** (the model for
   that tier is in the table of `docs/how/hand-out-a-task.md`) with: the task as it was given, the failing output, your diff, the
   files you read, and the routing row and instructions file you started from (a file under docs/how/). A small-tier worker's
   escalation goes to the mid tier first; the mid tier's goes to the strong.
2. **Replay when asked.** The strong agent asks you to replay the instructions you followed from the task's start: the
   row, the instructions file, the steps you took in order. Give it from what you can recall, briefly; if you
   cannot recall, say so. That is fine, and not worth more than a few lines.
3. **Apply the answer;** if that fails too, stop and say so to Ghost.
4. **Log it:** one row in `docs/ESCALATIONS.md`, with the issue type the strong agent named. Note the escalation in the
   step's journal entry too.

## The strong-tier agent

1. Ask the worker to replay the instructions it followed, before reading its diff.
2. **Validate the route:** were the instructions it needed for this task in the row and instructions file it
   started from, at the start? Read that file and compare it with what the task needed.
3. Solve the failure, and name one issue type for the log:

| Type | When |
| --- | --- |
| `missing-step` | the needed instruction is in no file the route leads to |
| `not-reached` | the route has it, but the agent did not reach it at the start (a wrong row, a pointer not followed) |
| `wrong-step` | the route's instruction is wrong or stale |
| `hard-problem` | the route was right and was followed; the problem itself is hard |
| `no-replay` | the agent could not replay its steps, so the route could not be checked |
| `conflict` | another agent's work collides with this one's: stop and go to Ghost at once (`docs/how/when-agents-conflict.md`) |

   Name the instructions file and the missing or wrong instruction where the type has one.

## Three of a type go to Ghost

When `docs/ESCALATIONS.md` holds three rows of one type not yet reviewed, the agent who writes the third:
marks those rows `raised` in the Raised column, adds an item to `docs/FOR-GHOST.md` naming the type and the
three rows, and tells Ghost, for review and a possible pass updating the routes. Ghost marks them
`reviewed: <date>` when done. `test/EscalationsTest.java` fails while a third row of a type is unraised.
