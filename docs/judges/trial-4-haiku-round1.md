# Judge trial 4, round 1: 25 instruction sets for Haiku, measured against Fable

Ghost, 2026-10-22: Haiku can do the job but lacks the right context; try it many times with different
instruction sets and find the context that gives sane results across codebases, weighing the seventh
Fable's cross-evaluation (trial 3) heavily. One workflow: 25 instruction sets × the six repositories of
trial 3, one Claude Haiku 5.5 run each (150 runs, schema-enforced output, about 13M tokens, about $1.30),
scored against the seventh Fable's calibrated scale. Every card is in `trial-4-round1-cards.json`.

## The sets

v01 the trial-1 brief alone; v02 classify folders first; v03 classify from evidence (generator headers,
build excludes, `.gitattributes`, READMEs, lockfiles); v04 the kernel's facts given; v05 v03 + v04; v06 the
gate (built, plan only, generated); v07 ambition split into posed and attempted; v08 navigability by its
mechanisms, paths checked; v09 provenance as evolution apart from who typed it; v10 the seventh Fable's four
insights together (v06–v09); v11 v10 + v03 + v04; v12 a general pitfall list; v13 history first; v14 tests
first; v15 a sceptical auditor; v16 code before docs; v17 calibration anchors (the other five repositories'
Fable scores); v18 v17 + v10; v19 15 tool calls; v20 60; v21 a verdict sentence before each number; v22
archetype anchors (a 100-line script, a CRUD app, a compiler); v23 the hardest problem first; v24 count own
code with commands; v25 everything.

## What it found

- **Telling the kind of repository is easy for Haiku.** Every run (the schema asked every set) called the
  generated atos suite generated and Neon Rain a plan. The question, asked at all, is enough.
- **Knowing is not acting on it.** Having called the atos suite generated, Haiku still scored the generated
  code: navigability +55 over Fable, maintainability +41, quality +39, and still +41, +28, +16 under the best
  sets. Under E-056 a generated repository is declined with no card, so the gate removes the worst error;
  the score must not be asked of the judge at all.
- **A plan needs a rule, not an instruction.** The seventh Fable's insights (v06–v10, v18) made Haiku
  over-discount Neon Rain: ambition 40 below Fable's 70, navigability 20 below. Fable's own advice was to
  withhold Size, Complexity and Maintainability for a plan and mark Ambition unverified: that is a card rule,
  better made by the kernel than asked of a judge.
- **On the four built repositories every set is close.** Mean error 4.2 (v08 navigability by mechanism) to 8.3
  (v15 sceptic), baseline 5.8; pairs put in Fable's order 67–88%. One run per cell, with a single run's spread
  around 8 points (trials 1–2), cannot separate most of the sets: round 2 replicates.
- **The largest errors left are Size (7.9) and Provenance (7.2).** Size comes from what the judge counts as own
  code: Haiku's median own code for Katana is 37k lines, Fable's 20.4k (the GPL reference output kept getting
  counted). A judge that classifies folders, with the kernel counting only the folders judged own, would make
  Size a measurement again.
- The sceptic (v15) and the ambition split (v07) made things worse; counting own code with commands (v24) and
  the kernel's facts (v04) did not help.

## Next (round 2)

The shortlist (v08, v16, v21, v17, v22, v03, and v01 as the control) three times on each repository, with
the folder classification itself in the output, so the hybrid (judge classifies, kernel counts) can be
measured against Fable's folders.
