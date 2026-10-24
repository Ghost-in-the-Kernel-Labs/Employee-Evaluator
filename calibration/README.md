# The calibration set

Ghost's hand labels (TODO.md 2.1, E-079): 30 repositories, each one file `<id>.json`, the id opaque (`c001` to `c030`).
The id-to-repository map is Ghost's and lives outside this repository: no repository name, owner, URL or person's name
may appear here (E-014). `CalibrationSet` (rubric/) loads the folder and refuses a label that breaks the fields below;
`test/evaluator/rubric/CalibrationSetTest.java` runs it over this folder. Until 30 are here the test reports PENDING
and stays green; at 30 it enforces "30 labelled, at least five domains".

```json
{"id": "c001", "domain": "web-app", "kind": "own-work", "ambition": "product", "quality": 4, "rationale": "optional, one line, 280 characters at most"}
```

| Field | Values |
| --- | --- |
| `id` | `c` and three digits, unique, the file's name |
| `domain` | `web-app`, `cli-tool`, `library`, `data-pipeline`, `game`, `mobile-app`, `infrastructure`, `machine-learning` |
| `kind` | `own-work`, `clone` (another's work, copied), `generated` (a tool's wholesale output); the admission gate's cases (E-056) |
| `ambition` | `toy`, `utility`, `product`, `research` (docs/SCORE-CARD.md) |
| `quality` | a whole number, 1 to 5 (docs/DESIGN.md step 2.1) |
| `rationale` | optional; no URL, address, owner/repo, or two capitalised words in a row (a name) |

The set must span at least five domains. Ambition and Quality are labelled for every kind, as the repository stands.
