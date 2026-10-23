# Navigation review: what the agents' debriefs agree on (E-068)

About every ten commits (`gradle metrics` warns past ten; `scripts/prepush.sh` refuses past twenty), an agent
reviews `docs/NAVIGATION-NOTES.md`. The review's commit subject starts "Navigation review".

## Steps

1. **Read the entries.** Group the complaints and suggestions by what they are about (a route, an
   instructions file, a document, a check).
2. **Look for convergence:** a point two or more agents ran into separately is actionable; one agent's alone
   is noted but not acted on, unless it is plainly a fact gone wrong (a stale line, a wrong path).
3. **No actionable items:** record that in the journal (one entry, class `measurement`), wipe the entries,
   and carry on.
4. **Actionable items:** record each in the journal with the entries it came from, then check each is valid
   against the repository (is the complaint true now? would the suggestion help, and break nothing?). A valid
   one is fixed in its instructions file, the routing row, or the document it names: in this commit, or as a
   next step said in docs/HANDOFF.md. An invalid one is recorded as such, with why.
5. **Open questions** (a point whose validity or fix you cannot settle, or one that would change a decision):
   ask a Claude Fable agent (the Agent tool, model `fable`), read only, with the entries, the files they name,
   and the question. Record its answer in the journal; a change to a decision still goes to Ghost.
6. **Wipe the file** back to its header (everything below the `---`) in the review's commit, whether or not
   anything was actionable. git keeps the old set.
