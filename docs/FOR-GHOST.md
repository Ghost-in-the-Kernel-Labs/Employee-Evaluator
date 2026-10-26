# For Ghost: the open items only Ghost can settle

Not an agent's reading: `CLAUDE.md` routes nowhere here. An agent adds an item when it finds one, and
says so to Ghost; Ghost removes it when settled.

- **Supply chain (E-033), what only you can do:** branch protection on `main` (pull requests required,
  force-pushes blocked; this changes E-020's "a finished step lands on main at once", so decide how an
  agent's work lands), secret scanning and Dependabot alerts, passkey MFA on GitHub, fine-grained tokens
  with an expiry, and `audit-agent-configs.ps1` on this repository as the rollout's acceptance test.
  `osv-scanner` has no lockfile to read here: every dependency is a vendored jar, held by its SHA-256.
- **apt in the kernel's `Dockerfile`:** git is installed with `apt-get` unpinned, the one install not held
  by a hash; pin it (a version, or an archive snapshot date) or record why apt's signatures are enough.
- **Isolation:** a cloud session's container has no seccomp or AppArmor, so isolation there is our flags
  alone; the hosted machine must have both (TODO.md 2.7).
- **Naming:** "Employee Evaluator" may read as surveillance to applicants (docs/DESIGN.md, open
  questions). No decision needed before phase 3.
- **Found in other repositories, reported, not touched:** Suite Builder's
  `docs/HOSTILE-REVIEW-2026-10-20.md` records a critical bypass of the crawler's address guard
  (IPv6-mapped IPv4); a later Fable reading says it is fixed. Suite Builder keeps saved third-party crawls
  in `test/data` and `test/bench` beside a rule against pages saved from others' sites. Katana keeps about
  19k lines of GPL-licensed opensourcecobol4j output under `lower/prototypes/` as reference.
- **A second reference: Anthropic's agent-built C compiler** (Nicholas Carlini, 2026; 16 Claude agents, about
  100,000 lines, compiles Linux 6.9; public on GitHub, link in anthropic.com/engineering/building-c-compiler).
  It is Rust: measuring it needs a Rust adapter, a new grammar to vendor (a dependency, your approval).
- **The judge harness (TODO.md 2.2, sliced into 2.2a to 2.2n; `docs/DESIGN.md`, "Step 2.2 sliced"):** parts a to k need nothing
  from you and an agent can start at 2.2a now. 2.2l to 2.2n wait on you: (1) how an API key reaches a session, and whether
  judges run in a session or CI; (2) the budget per call, evaluation and trial, and the iteration limit; (3) the ids and
  prices of Haiku 5.5 and Opus 5.5; (4) standard-library HTTP or the Anthropic SDK (a dependency); (5) `python3` in the pinned
  image (answered 2026-10-25: yes, E-081); (6) the 30 labelled repositories re-fetched outside the
  repository, by whom and where; (7) what a real run may commit (proposal: ids, scores, kinds, tiers only); (8) what agreement means (answered 2026-10-25: both the raw 80 % floor and a margin over the constant-answer baseline, E-084; **confirm or change the margin's size**: the agent proposed half the gap between the baseline and 100 %, so Quality within one needs 29 of 30, kind 27, Ambition 24, Quality exactly 24; kind is compared exactly, E-082, and the set holds no generated); (9) the rule
  for a three-way tie (answered 2026-10-25: Opus decides, E-083).
- **The card's rules for the judged kind (TODO.md 2.2k, E-086, proposed):** four calls for you to confirm or change. (1) **Clone:** E-056 declines only hand-written and generated work and E-082 says a clone call alone is no decline, so a `clone` is scored as measured with a visible note and nothing more; should a clone be declined, ranked lower, or asked about (E-056's second outcome)? (2) **The declined outcome:** exit status 2, the message and the kind on stdout (JSON, or Markdown with `--markdown`), no card; the web app (3.3) will want to read that shape. (3) **Quality's gate:** the fact `QUALITY.agent_review` (the 1 to 5) is made only when Quality *within one* is live, with its figure as the ceiling on confidence; Quality exactly gates nothing. (4) **A kind that is not live decides nothing:** no decline and no withholding, only a note. One risk to know: the 30 labels hold no generated repository (E-084 records it in `labels_absent`), so a live kind figure does not show that "generated" is recognised; a decline rests on it all the same. Options: add a few generated labels, or require the record to show it before a decline. Also: no formula reads `AMBITION.tier` or `QUALITY.agent_review` yet (2.3, 2.4), so a judged card's Ambition and Quality stay not measured and show the judge's value in a note.
