# The calibration set

Ghost's hand labels (TODO.md 2.1, E-080): 30 repositories, each one file `<id>.json`, the id opaque (`c001` to `c030`).
A label exists only to give ground truth to the judges (Ambition, the agent-review half of Quality) and the admission
gate (kind), so it holds those three and the id. The id-to-repository map, with each repository's domain, is Ghost's and
lives outside this repository: no repository name, owner, URL or person's name may appear here (E-014). `CalibrationSet`
(rubric/) loads the folder and refuses a label that breaks the fields below; `test/evaluator/rubric/CalibrationSetTest.java`
runs it over this folder. Until 30 are here the test reports PENDING and stays green; from 30 it reports MET.

```json
{"id": "c001", "kind": "own-work", "ambition": "product", "quality": 4}
```

| Field | Values |
| --- | --- |
| `id` | `c` and three digits, unique, the file's name |
| `kind` | `own-work`, `clone` (another's work, copied), `generated` (a tool's wholesale output); the admission gate's cases (E-056) |
| `ambition` | `toy`, `utility`, `product`, `research` (docs/SCORE-CARD.md) |
| `quality` | a whole number, 1 to 5 (docs/DESIGN.md step 2.1) |

All four are required and no other field is accepted. Ambition and Quality are labelled for every kind, as the repository stands.
