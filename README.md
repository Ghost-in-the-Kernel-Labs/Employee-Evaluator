# Employee Evaluator

Ranks developers by the AI-assisted repository they actually built. An applicant signs in with
GitHub and submits one repository; the engine scores it on seven metrics, every score traced to
facts with evidence, and verifies that the applicant built it; employers browse anonymized score
cards and pay to unlock the top of the board, under a rule that every unlocked applicant gets an
answer.

- `docs/DESIGN.md` — the pitch, the architecture, provenance, the rubric, the phases.
- `CLAUDE.md` — how to work in this repository; `theGhost.md` — why it is shaped as it is.
- `TODO.md` — what is next, in order.

Built kernel first: a Java 21 scorer (phase 1, in progress), then agent judges (phase 2), then the
web app (phase 3). A clean clone builds and tests with the network off, in the pinned image:

```sh
docker build -t evaluator .
docker run --rm --network none -u "$(id -u)" -v "$PWD":/evaluator evaluator gradle test --offline
```

Ghost in the Kernel Labs. Maintainer: Ghost.
