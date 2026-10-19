# Third-party code, vendored so a clean clone builds with the network off (E-021)

Each jar is the Maven Central artifact named, recorded here by SHA-256, with its license text beside
it. Nothing under a license that forbids redistribution, and no GPL code, enters this folder. "Ships"
says whether the jar reaches an applicant or an employer: nothing here does.

| Jar | License | Used by | Ships | SHA-256 |
| --- | --- | --- | --- | --- |
| `junit/apiguardian-api-1.1.2.jar` | Apache-2.0 (`junit/LICENSE-apiguardian`) | tests only | no | `b509448ac506d607319f182537f0b35d71007582ec741832a1f111e5b5b70b38` |
| `junit/junit-jupiter-api-5.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `ab83ef9e51ac4597d59d26b4b58812129550e2f579a404c8af7d09f5ce5b4293` |
| `junit/junit-jupiter-engine-5.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `cdf8ac59f3fad774ca738ad03890950eeb91833ef0e8908753177edd26f1581c` |
| `junit/junit-platform-commons-1.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `9edd969b0d0670c54105bc91ae79bd1c6f503e12115faba82073b84c86bbc334` |
| `junit/junit-platform-engine-1.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `b1dd998f64f9acadc15966d9cd3d08074662677b3e390f0a38fcbf0bb4c72330` |
| `junit/junit-platform-launcher-1.11.4.jar` | EPL-2.0 (`junit/LICENSE-junit.md`) | tests only | no | `d7430bd029e7fcced53ee445e4d2d1a8a1e043ea4c4df43b6335a857f79761ae` |
| `junit/opentest4j-1.3.0.jar` | Apache-2.0 (`junit/LICENSE-opentest4j`) | tests only | no | `48e2df636cab6563ced64dcdff8abb2355627cb236ef0bf37598682ddf742f1b` |
| `tree-sitter/tree-sitter-0.26.6.jar` | MIT (`tree-sitter/LICENSE-tree-sitter-ng`; the bundled tree-sitter library: `tree-sitter/LICENSE-tree-sitter`) | `analyze/` (E-028) | no | `81b113176c47c6adef9f6e3f7b8ca9f8141c4a8627f69d4423fc2e130a8ba74e` |
| `tree-sitter/tree-sitter-python-0.25.0.jar` | MIT (`tree-sitter/LICENSE-tree-sitter-python`) | `analyze/` (E-028) | no | `2cf56d951790ca5fbbd3fa64d11cca74e7d7bffc17d445050f9965c80875d640` |
| `tree-sitter/tree-sitter-typescript-0.23.2.jar` | MIT (`tree-sitter/LICENSE-tree-sitter-typescript`) | `analyze/` (E-028) | no | `5c3d1716441fbda55293b8632f9e691733bd3035a582da3936599d2309183bf0` |

The tree-sitter jars are `io.github.bonede` on Maven Central (github.com/bonede/tree-sitter-ng): a JNI
binding built for Java 11, so it runs on the kernel's Java 21, with its native libraries inside each
jar for Linux, macOS and Windows (x86_64, and aarch64 for Linux and macOS). The official binding,
`io.github.tree-sitter:jtreesitter`, is built for Java 23 and ships no native library (J-010).
