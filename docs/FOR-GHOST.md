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
- **A score past 100 (E-070, TODO.md 1.13):** the baseline makes Suite Builder 100 and lets a repository read
  above it; the card holds every score to 0–100 today (`Score`, SC-07). Either the card's bound goes, or it keeps
  0–100 and shows "× Suite Builder" beside it. Needed before 1.13's first metric moves.
- **A second reference: Anthropic's agent-built C compiler** (Nicholas Carlini, 2026; 16 Claude agents, about
  100,000 lines, compiles Linux 6.9; public on GitHub, link in anthropic.com/engineering/building-c-compiler).
  It is Rust: measuring it needs a Rust adapter, a new grammar to vendor (a dependency, your approval).
- **The judge (E-055):** Haiku three times, averaged, with the kernel's facts and an enforced schema; Opus
  where they split. Recorded in TODO.md 2.2's check; nothing waits on it before 2.2.
